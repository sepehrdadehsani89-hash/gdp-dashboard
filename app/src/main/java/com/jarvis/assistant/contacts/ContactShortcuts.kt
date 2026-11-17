package com.jarvis.assistant.contacts

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.jarvis.assistant.neural.AppMatching
import com.jarvis.assistant.omnisystem.FeatureVault
import com.jarvis.assistant.tools.UtilityIntentParser
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ContactShortcut(val id:String,val nickname:String,val number:String,val variants:List<String>,val reference:String?)
object ContactShortcutPolicy {
    fun number(input:String):String {
        val result=UtilityIntentParser.digits(input).replace(Regex("[\\s().-]"),"")
        require(result.matches(Regex("\\+?[0-9]{7,15}"))){"Use an ordinary 7–15 digit number; short/emergency numbers and USSD are not supported."}
        return result
    }
    fun target(text:String):String? {
        val t=AppMatching.normalize(text).removePrefix("hey jarvis ").removePrefix("jarvis ")
        return Regex("^(?:call |dial |llama a |llamar a |تماس با )(.{1,80})$").matchEntire(t)?.groupValues?.get(1)?.removePrefix("my ")?.trim()?.takeIf{it.isNotBlank()}
    }
    fun decline(text:String)=AppMatching.normalize(text) in setOf("no","no thanks","its okay","it s okay","never mind","cancel","no gracias","no importa","cancelar","نه","نه ممنون","مهم نیست","لغو")
    fun keys(contact:ContactShortcut)=(listOf(contact.nickname)+contact.variants).map(AppMatching::normalize).toSet()
    fun validate(items:List<ContactShortcut>){
        require(items.size<=15 && items.map{it.id}.distinct().size==items.size)
        val aliases=mutableSetOf<String>()
        items.forEach{c->
            require(c.id.length in 1..100 && c.nickname.length in 1..80 && c.nickname.none(Char::isISOControl))
            require(c.number==number(c.number) && c.variants.size<=5)
            require(c.variants.all{it.length in 1..80 && it.none(Char::isISOControl)})
            val uri=c.reference?.let{java.net.URI(it)}
            require(uri==null || uri.scheme=="content" && uri.authority=="com.android.contacts" && uri.query==null && uri.fragment==null)
            keys(c).forEach{require(it.isNotBlank() && aliases.add(it)){"A spoken nickname belongs to more than one shortcut."}}
        }
    }
    fun encode(items:List<ContactShortcut>):String {validate(items);return JSONArray().apply{items.forEach{c->put(JSONObject().put("id",c.id).put("nickname",c.nickname).put("number",c.number).put("variants",JSONArray(c.variants)).put("reference",c.reference ?: JSONObject.NULL))}}.toString()}
    fun parse(raw:String):List<ContactShortcut>{
        require(raw.length<=100_000);val array=JSONArray(raw);require(array.length()<=15)
        return (0 until array.length()).map{i->val c=array.getJSONObject(i);val variants=c.getJSONArray("variants");require(variants.length()<=5)
            ContactShortcut(c.getString("id"),c.getString("nickname"),c.getString("number"),(0 until variants.length()).map{variants.getString(it)},if(c.isNull("reference"))null else c.getString("reference"))
        }.also(::validate)
    }
}
/** Encrypted user-approved snapshots. A linked provider number is revalidated before opening the dialer. */
class ContactShortcuts(private val context:Context,private val vault:FeatureVault){
    private fun unlocked()=context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked==false
    @Synchronized fun all():List<ContactShortcut>{
        check(unlocked())
        val raw=vault.read("contact_shortcuts") ?: run{check(!vault.contains("contact_shortcuts")){"Encrypted contacts unavailable."};return emptyList()}
        return ContactShortcutPolicy.parse(raw)
    }
    @Synchronized fun save(id:String?,nickname:String,number:String,variants:List<String>,reference:String?){
        val before=all();require(id==null || before.any{it.id==id})
        val c=ContactShortcut(id ?: UUID.randomUUID().toString(),nickname.trim(),ContactShortcutPolicy.number(number),variants.map(String::trim).filter(String::isNotEmpty).distinct(),reference)
        val next=before.filterNot{it.id==id}+c
        vault.write("contact_shortcuts",ContactShortcutPolicy.encode(next))
    }
    @Synchronized fun remove(id:String){vault.write("contact_shortcuts",ContactShortcutPolicy.encode(all().filterNot{it.id==id}))}
    fun permissionGranted()=ContextCompat.checkSelfPermission(context,Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED
    fun lookupCandidates(target:String):List<PhoneContactCandidate>{
        check(unlocked() && permissionGranted()){"CONTACTS_PERMISSION_REQUIRED"}
        require(target.length in 1..80 && target.none(Char::isISOControl))
        val uri=ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val result=mutableListOf<PhoneContactCandidate>()
        context.contentResolver.query(Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,Uri.encode(SpokenContactPolicy.targetName(target))),arrayOf(ContactsContract.CommonDataKinds.Phone._ID,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER),null,null,null)?.use{cursor->
            while(cursor.moveToNext() && result.size<31){val id=cursor.getLong(0);result+=PhoneContactCandidate(id,cursor.getString(1).orEmpty().take(80),cursor.getString(2).orEmpty(),android.content.ContentUris.withAppendedId(uri,id).toString())}
        }
        check(result.size<=30){"Too many matching contact records. Use the Android contact picker."}
        return result
    }
    fun resolve(target:String):ContactShortcut? {
        val entries=all()
        val spelled=SpokenContactPolicy.savedMatches(target,entries)
        if(spelled.isNotEmpty())return spelled.singleOrNull()
        if(!permissionGranted())return null
        val candidate=SpokenContactPolicy.unique(target,lookupCandidates(target)) ?: return null
        return ContactShortcut("android:${candidate.id}",candidate.name,candidate.number,emptyList(),candidate.reference)
    }
    private fun currentCandidate(reference:String):PhoneContactCandidate {
        check(unlocked()){"Unlock Android before reading a linked contact"}
        val uri=Uri.parse(reference)
        require(uri.scheme=="content" && uri.authority=="com.android.contacts" && uri.query==null && uri.fragment==null && uri.lastPathSegment?.toLongOrNull()!=null)
        return context.contentResolver.query(uri,arrayOf(ContactsContract.CommonDataKinds.Phone._ID,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER),null,null,null).use{cursor->
            check(cursor!=null && cursor.moveToFirst()){"The contact is no longer available"}
            PhoneContactCandidate(cursor.getLong(0),cursor.getString(1).orEmpty().take(80),ContactShortcutPolicy.number(cursor.getString(2)),reference)
        }
    }
    private fun fingerprint()=java.security.MessageDigest.getInstance("SHA-256").digest(ContactShortcutPolicy.encode(all()).toByteArray()).joinToString(""){"%02x".format(it)}
    fun spokenAliasProposal(request:SpokenContactAlias):com.jarvis.assistant.core.AssistantReply {
        val matches=SpokenContactPolicy.savedMatches(request.target,all())
        if(matches.size>1)return com.jarvis.assistant.core.AssistantReply("That spoken nickname matches multiple saved shortcuts. Use the exact contact name or choose a number in Contact shortcuts. Nothing was saved or called.",allowCloudSpeech=true)
        val saved=matches.singleOrNull()
        val manualNumber=runCatching{ContactShortcutPolicy.number(request.target)}.getOrNull()
        val candidate=if(saved!=null)PhoneContactCandidate(-1,saved.nickname,saved.number,saved.reference.orEmpty()) else if(manualNumber!=null)PhoneContactCandidate(-1,"your requested phone number",manualNumber,"") else SpokenContactPolicy.unique(request.target,lookupCandidates(request.target))
            ?: return com.jarvis.assistant.core.AssistantReply("No unique exact contact match was found. Say the contact's exact saved name or use the contact picker. Nothing was saved or called.",allowCloudSpeech=true)
        val conflicting=SpokenContactPolicy.savedMatches(request.alias,all()).any{it.number!=candidate.number}
        if(conflicting)return com.jarvis.assistant.core.AssistantReply("That nickname already belongs to a different saved number. Review the existing contact shortcut before reassigning it. Nothing changed.",allowCloudSpeech=true)
        val actualProviderName=if(candidate.reference.isNotBlank()){val current=currentCandidate(candidate.reference);check(current.number==candidate.number){"The contact number changed. Ask again."};current.name}else ""
        val payload=JSONObject().put("provider_name",actualProviderName).put("alias",request.alias).put("number",candidate.number).put("reference",candidate.reference).put("name",candidate.name).put("fingerprint",fingerprint()).toString()
        val details="Save the requested nickname for ${candidate.name}, number ending ${candidate.number.takeLast(4)}? Android's contact record is not changed."
        return com.jarvis.assistant.core.AssistantReply("I found a unique exact contact match. Say confirm to save your requested nickname, or cancel. No call has been placed.",action=com.jarvis.assistant.core.PendingAction(description=details,kind="spoken_contact_alias",payload=payload),allowCloudSpeech=true)
    }
    @Synchronized fun confirmSpokenAlias(action:com.jarvis.assistant.core.PendingAction):String {
        check(unlocked());require(action.kind=="spoken_contact_alias" && System.currentTimeMillis()-action.createdAt in 0..120000)
        val j=JSONObject(action.payload);check(j.getString("fingerprint")==fingerprint()){"Contact shortcuts changed. Ask again."}
        val number=ContactShortcutPolicy.number(j.getString("number"));val alias=j.getString("alias");require(alias.length in 1..80 && alias.none(Char::isISOControl))
        check(SpokenContactPolicy.savedMatches(alias,all()).all{it.number==number}){"Nickname conflicts with another saved number"}
        val reference=j.getString("reference").takeIf{it.isNotBlank()}
        reference?.let{val current=currentCandidate(it);check(current.number==number && AppMatching.normalize(current.name)==AppMatching.normalize(j.getString("provider_name"))){"The Android contact changed. Ask again."}}
        val sameNumber=all().firstOrNull{it.number==number}
        if(sameNumber!=null){
            val variants=(sameNumber.variants+alias+SpokenContactPolicy.variants(alias)).filter{AppMatching.normalize(it)!=AppMatching.normalize(sameNumber.nickname)}.distinct()
            require(variants.size<=5){"This contact already has five aliases. Review Contact shortcuts."}
            save(sameNumber.id,sameNumber.nickname,number,variants,sameNumber.reference ?: reference)
        }else save(null,alias,number,SpokenContactPolicy.variants(alias),reference)
        return "Your contact nickname was saved locally. You can now ask Jarvis to call that nickname. Android's dialer still owns the final Call action."
    }
    fun validateForDial(id:String,expected:String):ContactShortcut {
        val c=all().singleOrNull{it.id==id} ?: if(id.startsWith("android:")){
            val phoneId=id.removePrefix("android:").toLongOrNull() ?: error("Invalid contact reference")
            val uri=android.content.ContentUris.withAppendedId(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,phoneId).toString()
            val current=currentCandidate(uri)
            ContactShortcut(id,current.name,current.number,emptyList(),uri)
        }else error("Shortcut was removed. Ask again.")
        check(c.number==ContactShortcutPolicy.number(expected)){"Saved number changed. Review again."}
        c.reference?.let{reference->
            try{
                context.contentResolver.query(Uri.parse(reference),arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),null,null,null).use{cursor->
                    check(cursor!=null && cursor.moveToFirst()){"Linked contact is no longer available. Choose it again."}
                    check(ContactShortcutPolicy.number(cursor.getString(0))==c.number){"Android contact number changed. Choose it again before calling."}
                }
            }catch(_:SecurityException){error("Contact access expired. Choose the contact again or grant Contacts permission in the shortcut screen.")}
        }
        return c
    }
}
