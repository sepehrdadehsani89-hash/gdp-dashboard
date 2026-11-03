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
    fun resolve(target:String)=all().singleOrNull{AppMatching.normalize(target) in ContactShortcutPolicy.keys(it)}
    fun validateForDial(id:String,expected:String):ContactShortcut {
        val c=all().singleOrNull{it.id==id} ?: error("Shortcut was removed. Ask again.")
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
