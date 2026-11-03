package com.jarvis.assistant.contacts

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jarvis.assistant.omnisystem.PrivateModuleActivity

class ContactShortcutsActivity:PrivateModuleActivity(){
    private var entries by mutableStateOf(emptyList<ContactShortcut>())
    private var nickname by mutableStateOf("")
    private var number by mutableStateOf("")
    private var variants by mutableStateOf("")
    private var reference:String?=null
    private var editing:String?=null
    private var returned by mutableStateOf(false)
    private var pendingReturn=false
    private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->status=if(granted)words("Contacts permission granted. Review the saved number before dialing.","Permiso concedido. Revisa el número antes de marcar.","مجوز مخاطبین داده شد. پیش از شماره‌گیری، شماره را بررسی کنید.") else words("Contacts permission denied. You can select a contact again or approve a manual number.","Permiso denegado. Puedes elegir un contacto de nuevo o aprobar un número manual.","مجوز رد شد. می‌توانید مخاطب را دوباره انتخاب کنید یا شماره‌ای را دستی تأیید کنید.")}
    private val picker=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        val uri=result.data?.data ?: return@registerForActivityResult
        if(result.resultCode!=RESULT_OK || uri.scheme!="content" || uri.authority!="com.android.contacts")return@registerForActivityResult
        task{
            val chosen=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){contentResolver.query(uri,arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER),null,null,null).use{cursor->
                check(cursor!=null && cursor.moveToFirst());cursor.getString(0).take(80) to ContactShortcutPolicy.number(cursor.getString(1))
            }}
            nickname=chosen.first;number=chosen.second;reference=uri.toString();status=words("Selected number loaded locally. Review and save explicitly.","Número cargado localmente. Revisa y guarda.","شمارهٔ انتخاب‌شده روی گوشی دریافت شد. بررسی و صریحاً ذخیره کنید.")
        }
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);if(isFinishing)return;reload()
        screen(words("JARVIS · FIFTEEN CONTACT SHORTCUTS","JARVIS · QUINCE ATAJOS DE CONTACTOS","جارویس · پانزده میانبر مخاطب")){
            Text(words("Numbers and nicknames are encrypted privately. Dialing opens Android's dialer for your final Call action; Jarvis does not claim a call connected or went unanswered.","Números y alias se cifran en privado. Se abre el marcador Android para que pulses Llamar; Jarvis no confirma conexión ni llamada sin respuesta.","شماره و نام مستعار به‌صورت خصوصی رمز می‌شوند. شماره‌گیر اندروید برای تأیید نهایی تماس شما باز می‌شود؛ جارویس ادعای وصل شدن یا بی‌پاسخ بودن تماس ندارد."))
            Text("${entries.size}/15")
            OutlinedButton(onClick={permission.launch(Manifest.permission.READ_CONTACTS)}){Text(words("Optional Contacts permission for linked-number rechecks","Permiso opcional de Contactos para verificar vínculos","مجوز اختیاری مخاطبین برای بررسی دوبارهٔ شمارهٔ پیوندشده"))}
            OutlinedButton(enabled=!working,onClick={runCatching{picker.launch(Intent(Intent.ACTION_PICK,ContactsContract.CommonDataKinds.Phone.CONTENT_URI))}.onFailure{status=words("No Android contact picker available.","No hay selector de contactos.","انتخاب‌گر مخاطب اندروید در دسترس نیست.")}}){Text(words("Choose one Android contact number","Elegir un número de contacto Android","انتخاب یک شماره از مخاطبین اندروید"))}
            OutlinedTextField(nickname,{nickname=it.take(80)},label={Text(words("Spoken nickname","Alias hablado","نام مستعار گفتاری"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(number,{number=it.take(40);reference=null},label={Text(words("Approved number · editing makes a manual shortcut","Número aprobado · editar crea un atajo manual","شمارهٔ تأییدشده · ویرایش، میانبر را دستی می‌کند"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(variants,{variants=it.take(400)},label={Text(words("Up to five spoken variants · comma separated","Hasta cinco variantes · separadas por coma","حداکثر پنج شکل گفتاری · جداشده با ویرگول"))},modifier=Modifier.fillMaxWidth())
            Button(enabled=!working && nickname.isNotBlank() && number.isNotBlank() && (entries.size<15 || editing!=null),onClick={authorize{
                runtime.contacts.save(editing,nickname,number,variants.split(',', '،'),reference)
                clearEditor();reload();status=words("Contact shortcut saved privately","Atajo guardado en privado","میانبر مخاطب خصوصی ذخیره شد")
            }}){Text(words("Approve and save shortcut","Aprobar y guardar atajo","تأیید و ذخیرهٔ میانبر"))}
            TextButton(onClick=::clearEditor){Text(words("Clear editor / new shortcut","Limpiar / nuevo atajo","پاک کردن ویرایشگر / میانبر جدید"))}
            entries.forEach{c->
                Text(c.nickname+" · "+c.number,style=MaterialTheme.typography.titleMedium)
                Text(c.variants.joinToString(" · "))
                if(c.reference!=null)Text(words("Linked number is checked again before dialing. Expired access or changed numbers require reselection.","El número vinculado se verifica antes de marcar. Acceso vencido o cambios requieren elegirlo de nuevo.","شمارهٔ پیوندشده پیش از شماره‌گیری دوباره بررسی می‌شود. دسترسی منقضی یا شمارهٔ تغییریافته به انتخاب دوباره نیاز دارد."))
                Row{
                    TextButton(onClick={editing=c.id;nickname=c.nickname;number=c.number;variants=c.variants.joinToString(", ");reference=c.reference}){Text(words("Edit","Editar","ویرایش"))}
                    TextButton(onClick={askDial(c)}){Text(words("Test / open dialer","Probar / abrir marcador","آزمایش / باز کردن شماره‌گیر"))}
                    TextButton(onClick={authorize{runtime.contacts.remove(c.id);reload()}}){Text(words("Delete","Eliminar","حذف"))}
                }
            }
            OutlinedButton(onClick={startActivity(Intent(this@ContactShortcutsActivity,PersonalMemoActivity::class.java))}){Text(words("New personal voice memo · separate consent","Nota personal nueva · consentimiento separado","پیام صوتی شخصی جدید · تأیید جداگانه"))}
            if(returned){
                HorizontalDivider()
                Text(words("Returned from the dialer. Android has not supplied a trusted call outcome. Fifteen seconds alone never proves an unanswered call.","Volviste del marcador. Android no dio un resultado fiable. Quince segundos no prueban una llamada sin respuesta.","از شماره‌گیر برگشتید. اندروید نتیجهٔ قابل‌اعتماد تماس ارائه نکرده است. پانزده ثانیه به‌تنهایی بی‌پاسخ بودن تماس را ثابت نمی‌کند."))
                OutlinedButton(onClick={startActivity(Intent(this@ContactShortcutsActivity,PersonalMemoActivity::class.java))}){Text(words("Review recording a new personal voice memo","Revisar grabación de nota de voz personal","بررسی ضبط پیام صوتی شخصی جدید"))}
                TextButton(onClick={returned=false}){Text(words("No thanks · never mind · cancel","No gracias · déjalo · cancelar","نه ممنون · مهم نیست · لغو"))}
            }
        }
    }
    private fun askDial(c:ContactShortcut){AlertDialog.Builder(this).setTitle(words("Open Android dialer for ${c.nickname}?","¿Abrir marcador para ${c.nickname}?","شماره‌گیر اندروید برای ${c.nickname} باز شود؟"))
        .setMessage(c.number+"\n"+words("Review and press Call in Android. No call is placed automatically.","Revisa y pulsa Llamar en Android. No se llama automáticamente.","شماره را بررسی و در اندروید دکمهٔ تماس را بزنید. تماس خودکار برقرار نمی‌شود."))
        .setNegativeButton(words("Cancel","Cancelar","لغو"),null).setPositiveButton(words("Open dialer","Abrir marcador","باز کردن شماره‌گیر")){_,_->task{
            val verified=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){runtime.contacts.validateForDial(c.id,c.number)}
            startActivity(Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",verified.number,null)));pendingReturn=true
            status=words("Dialer opening requested; call outcome unverified.","Marcador solicitado; resultado no verificado.","باز شدن شماره‌گیر درخواست شد؛ نتیجهٔ تماس تأیید نشده است.")
        }}.show()}
    private fun clearEditor(){editing=null;nickname="";number="";variants="";reference=null}
    private fun reload(){runCatching{runtime.contacts.all()}.onSuccess{entries=it}.onFailure{status=words("Encrypted shortcuts unavailable; existing data was not overwritten.","Atajos cifrados no disponibles; no se sobrescribieron datos.","میانبرهای رمز‌شده در دسترس نیستند؛ داده‌های موجود بازنویسی نشدند.")}}
    override fun onResume(){super.onResume();if(!isFinishing){reload();if(pendingReturn){pendingReturn=false;returned=true}}}
}
