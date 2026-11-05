package com.jarvis.assistant.widgets

import android.app.Activity
import android.appwidget.*
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jarvis.assistant.MainActivity
import com.jarvis.assistant.omnisystem.*
import com.jarvis.assistant.security.SystemCredentialPrivacy

data class WidgetProviderChoice(val info:AppWidgetProviderInfo,val label:String)

/** User-approved widget copies in Jarvis; it cannot adopt a launcher's private widget instances. */
class WidgetCenterActivity:PrivateModuleActivity(){
    private val repository by lazy{WidgetRepository(this)}
    private val host by lazy{AppWidgetHost(this,WidgetHostPolicy.HOST_ID)}
    private var owner=Any()
    private var setupLifetime:Job?=null
    private var widgets by mutableStateOf(emptyList<Int>())
    private var providers by mutableStateOf(emptyList<WidgetProviderChoice>())
    private var query by mutableStateOf("")
    private var widgetsVisible by mutableStateOf(false)
    private var pending by mutableStateOf<Int?>(null)
    private var external by mutableStateOf(false)
    private var refreshJob:Job?=null
    private var refreshToken=0L
    private var expectedResult=-1
    private var configuringResult=false
    private var privacyOwned=false
    private var handoffGeneration=-1L
    private var revision by mutableIntStateOf(0)
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(isFinishing)return
        pending=savedInstanceState?.getInt("pending",-1)?.takeIf{it>0}
        external=savedInstanceState?.getBoolean("external",false)==true
        configuringExisting=savedInstanceState?.getBoolean("existing",false)==true
        expectedResult=savedInstanceState?.getInt("result_code",-1) ?: -1
        configuringResult=savedInstanceState?.getBoolean("configuring_result",false)==true
        privacyOwned=savedInstanceState?.getBoolean("privacy_owned",false)==true
        handoffGeneration=savedInstanceState?.getLong("handoff_generation",-1) ?: -1
        runCatching{repository.removeOrphans(host,pending)}
        // A recreated request has no continuing microphone authority. Keep Android UI quiet
        // until its result; an explicit recovery button handles an abandoned result.
        refresh()
        screen(words("JARVIS · WIDGETS & PANELS","JARVIS · WIDGETS Y PANELES","ابزارک‌ها و پنل‌ها")){
            Text(words("Add widgets exposed by your installed apps. Android asks you to approve each binding and any provider configuration. Existing launcher widgets stay with your launcher. Widget content is not read into chat or sent to AI.","Añade widgets de tus apps instaladas. Android pide aprobar cada enlace y la configuración del proveedor. El contenido no se lee en el chat ni se envía a IA.","ابزارک‌ها فقط با تأیید اندروید اضافه می‌شوند؛ محتوای آن‌ها به هوش مصنوعی فرستاده نمی‌شود."))
            Text(words("Jarvis panels","Paneles de Jarvis","پنل‌های جارویس"))
            HudModule.entries.forEach{module->
                OutlinedButton(onClick={runtime.selectModule(module);startActivity(Intent(this@WidgetCenterActivity,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("phantom_expand",true))}){Text(module.name.lowercase().replaceFirstChar(Char::uppercaseChar))}
            }
            HorizontalDivider()
            Text("${widgets.size}/${WidgetHostPolicy.MAX_WIDGETS} · "+words("approved widgets","widgets aprobados","ابزارک تأییدشده"))
            if(pending!=null)Text(words("Android widget setup is pending. Complete or cancel it before adding another.","Configuración pendiente. Termina o cancela antes de añadir otro.","تنظیم ابزارک در انتظار نتیجهٔ اندروید است."))
            if(external || SystemCredentialPrivacy.pending(this@WidgetCenterActivity))TextButton(onClick={
                if(window.decorView.hasWindowFocus() && !locked()){
                    runtime.stopEverything();discardPending();external=false;privacyOwned=false;expectedResult=-1;runCatching{SystemCredentialPrivacy.end(this@WidgetCenterActivity)};runtime.abandonExclusiveCapture(owner)
                    status=words("Interrupted setup cleared. Listening stays stopped; start it explicitly when ready.","Configuración interrumpida cancelada. La escucha queda detenida.","تنظیم متوقف شد؛ شنود خاموش می‌ماند.")
                }
            }){Text(words("Recover interrupted widget setup","Cancelar configuración interrumpida","بازیابی تنظیم متوقف"))}
            @Suppress("UNUSED_VARIABLE")val observed=revision
            if(widgetsVisible)widgets.forEach{id->key(id,revision){
                val info=repository.manager.getAppWidgetInfo(id)
                if(info!=null){
                    Text(info.loadLabel(packageManager).orEmpty().take(100))
                    val width=(LocalConfiguration.current.screenWidthDp-40).coerceAtLeast(120)
                    val height=(info.minHeight/resources.displayMetrics.density).toInt().coerceIn(96,600)
                    AndroidView(factory={runCatching{host.createView(this@WidgetCenterActivity,id,info).apply{
                        setAppWidget(id,info);updateAppWidgetSize(Bundle(),width,height,width,height)
                    }}.getOrElse{android.widget.TextView(this@WidgetCenterActivity).apply{text="Widget unavailable. Refresh or remove this slot."}}},modifier=Modifier.fillMaxWidth().height(height.dp))
                    if(info.configure!=null)TextButton(enabled=pending==null,onClick={beginExistingConfiguration(id)}){Text(words("Configure widget","Configurar widget","تنظیم ابزارک"))}
                    TextButton(enabled=pending==null,onClick={authorize{host.deleteAppWidgetId(id);repository.forget(id);refresh()}}){Text(words("Remove this Jarvis widget","Quitar este widget de Jarvis","حذف این ابزارک جارویس"))}
                }
            }}
            OutlinedTextField(query,{query=it.take(100)},label={Text(words("Search available widget providers","Buscar proveedores de widgets","جست‌وجوی ابزارک"))})
            OutlinedButton(onClick={refresh()}){Text(words("Refresh installed widget providers","Actualizar proveedores instalados","تازه‌سازی ابزارک‌ها"))}
            val matching=providers.filter{it.label.contains(query,true) || it.info.provider.packageName.contains(query,true)}
            Text("${matching.size} · "+words("matching providers; showing up to100","proveedores; se muestran hasta100","ارائه‌دهنده؛ نمایش حداکثر ۱۰۰"))
            matching.take(100).forEach{choice->val info=choice.info;OutlinedButton(enabled=pending==null && !external && widgets.size<WidgetHostPolicy.MAX_WIDGETS,onClick={authorize{beginAdd(info)}}){Text(choice.label+" · "+info.provider.packageName)}}
            if(providers.isEmpty())Text(words("Android exposed no widget providers. Install/enable a widget-capable app or refresh; hidden/work-profile widgets may be unavailable.","Android no expuso proveedores. Instala o activa una app con widgets. Los perfiles de trabajo pueden tener restricciones.","اندروید ابزارکی ارائه نکرد؛ محدودیت نمایه یا برنامه را بررسی کنید."))
        }
    }
    private fun refresh(){
        val token=++refreshToken;refreshJob?.cancel()
        refreshJob=lifecycleScope.launch{
            try{
                val result=withContext(Dispatchers.IO){
                    repository.approved(host) to repository.manager.getInstalledProvidersForProfile(Process.myUserHandle()).map{WidgetProviderChoice(it,it.loadLabel(packageManager).orEmpty().take(100))}.sortedBy{it.label}
                }
                if(token==refreshToken && !locked() && !isFinishing){widgets=result.first;providers=result.second;revision++}
            }catch(e:CancellationException){throw e}catch(_:Exception){status=words("Android widget service unavailable. Reopen and retry; no access is assumed.","Servicio de widgets no disponible. Vuelve a abrirlo.","سرویس ابزارک در دسترس نیست.")}
        }
    }
    private fun beginSetupPause():Boolean {
        val token=Any()
        if(!runtime.beginExclusiveCapture(token))return false
        owner=token
        val lifetime=lifecycleScope.launch(start=CoroutineStart.UNDISPATCHED){
            try{awaitCancellation()}finally{runtime.abandonExclusiveCapture(token)}
        }
        setupLifetime=lifetime;runtime.trackModuleOperation(token,lifetime)
        return true
    }
    private fun beginAdd(info:AppWidgetProviderInfo){
        if(locked() || pending!=null || !beginSetupPause())return
        try{
            val id=host.allocateAppWidgetId();pending=id
            if(repository.manager.bindAppWidgetIdIfAllowed(id,info.profile,info.provider,null))configure(id)
            else{
                beginExternal()
                expectedResult=repository.nextResultCode();configuringResult=false
                startActivityForResult(Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,info.provider).putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE,info.profile),expectedResult)
            }
        }catch(_:Exception){discardPending();finishExternal();status=words("Android did not allow this widget. Try another provider.","Android no permitió este widget. Prueba otro.","اندروید اجازهٔ افزودن ابزارک را نداد.")}
    }
    private var configuringExisting=false
    private fun beginExistingConfiguration(id:Int){
        if(locked() || pending!=null || !beginSetupPause())return
        configuringExisting=true;pending=id
        try{launchConfiguration(id)}catch(_:Exception){pending=null;configuringExisting=false;finishExternal();status="Provider configuration is unavailable."}
    }
    private fun configure(id:Int){
        val info=repository.manager.getAppWidgetInfo(id)
        if(info==null){discardPending();finishExternal();return}
        if(info.configure!=null)launchConfiguration(id)
        else complete(id)
    }
    private fun launchConfiguration(id:Int){
        beginExternal();expectedResult=repository.nextResultCode();configuringResult=true
        host.startAppWidgetConfigureActivityForResult(this,id,0,expectedResult,null)
    }
    private fun complete(id:Int){
        if(!configuringExisting)repository.add(id,host)
        pending=null;configuringExisting=false;finishExternal();refresh();status=words("Widget binding verified by Android.","Enlace del widget verificado por Android.","اتصال ابزارک توسط اندروید تأیید شد.")
    }
    private fun beginExternal(){if(!privacyOwned){SystemCredentialPrivacy.begin(this);privacyOwned=true;handoffGeneration=runtime.currentCommandSerial()};external=true;runtime.phantomOverlay.hide()}
    private fun finishExternal(){expectedResult=-1;external=false;if(privacyOwned){if(handoffGeneration==runtime.currentCommandSerial())runCatching{SystemCredentialPrivacy.end(this)};privacyOwned=false};runtime.endExclusiveCapture(owner);setupLifetime?.cancel();setupLifetime=null}
    private fun discardPending(){pending?.let{if(!configuringExisting && it.toString() !in repository.saved())runCatching{host.deleteAppWidgetId(it)}};pending=null;configuringExisting=false}
    @Deprecated("Android widget host configuration uses the platform activity-result protocol")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data)
        if(requestCode!=expectedResult || expectedResult<0)return
        if(!WidgetHostPolicy.currentResult(expectedResult,requestCode,handoffGeneration,runtime.currentCommandSerial())){
            expectedResult=-1;external=false;privacyOwned=false;discardPending();runtime.abandonExclusiveCapture(owner)
            status="Widget setup expired after Stop or a newer session. Recover the interrupted setup before retrying."
            return
        }
        val wasConfiguration=configuringResult
        expectedResult=-1
        external=false
        val id=pending
        if(id==null || locked()){discardPending();finishExternal();return}
        try{
            if(resultCode!=Activity.RESULT_OK){discardPending();finishExternal();status="Widget setup cancelled. No new widget was approved."}
            else if(!wasConfiguration)configure(id) else complete(id)
        }catch(_:Exception){discardPending();finishExternal();status="Widget provider could not complete setup. Existing widgets were preserved."}
    }
    override fun onSaveInstanceState(out:Bundle){pending?.let{out.putInt("pending",it)};out.putBoolean("external",external);out.putBoolean("existing",configuringExisting);out.putInt("result_code",expectedResult);out.putBoolean("configuring_result",configuringResult);out.putBoolean("privacy_owned",privacyOwned);out.putLong("handoff_generation",handoffGeneration);super.onSaveInstanceState(out)}
    override fun onStart(){super.onStart();if(!isFinishing){widgetsVisible=true;runCatching{host.startListening()};refresh()}}
    override fun onStop(){widgetsVisible=false;refreshToken++;refreshJob?.cancel();runCatching{host.stopListening()};super.onStop()}
    override fun onDestroy(){if(!isChangingConfigurations && !external){discardPending();finishExternal()};runtime.abandonExclusiveCapture(owner);super.onDestroy()}
}
