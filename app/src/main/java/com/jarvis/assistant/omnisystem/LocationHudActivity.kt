package com.jarvis.assistant.omnisystem

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.webkit.*
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.ByteArrayInputStream
import java.util.Locale

/** Real fix and real OSM map only after explicit permission/network consent. */
class LocationHudActivity:PrivateModuleActivity(){
    private var mapUrl by mutableStateOf<String?>(null)
    private var mapLabel by mutableStateOf("")
    private var saved by mutableStateOf(emptyList<SavedDestination>())
    private var destinationName by mutableStateOf("")
    private var web:WebView?=null
    private val permission=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){result->
        status=if(result[Manifest.permission.ACCESS_COARSE_LOCATION]==true)words("Location permission granted. Request a fresh fix when ready.","Permiso concedido. Solicita una ubicación reciente.","مجوز موقعیت داده شد. برای دریافت موقعیت تازه درخواست دهید.") else words("Location permission denied.","Permiso de ubicación denegado.","مجوز موقعیت رد شد.")
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);if(isFinishing)return
        loadSaved()
        screen(words("JARVIS · LOCATION INTELLIGENCE","JARVIS · INTELIGENCIA DE UBICACIÓN","جارویس · موقعیت مکانی")){
            val snapshot by runtime.locations.state.collectAsStateWithLifecycle()
            Text(words("Current fixes are temporary. Destinations are saved only when you explicitly name and save one. There is no automatic location timeline.","Las ubicaciones actuales son temporales. Solo se guarda un destino cuando lo nombras y guardas. No hay historial automático.","موقعیت فعلی موقت است. مقصد فقط با نام‌گذاری و ذخیرهٔ صریح شما ثبت می‌شود. سابقهٔ خودکار موقعیت وجود ندارد."))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedButton(onClick={permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))}){Text(words("Approximate permission","Permiso aproximado","مجوز تقریبی"))}
                OutlinedButton(onClick={permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION))}){Text(words("Precise permission","Permiso preciso","مجوز دقیق"))}
            }
            OutlinedButton(onClick={open(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))}){Text(words("Android location/provider settings","Ubicación y proveedores de Android","تنظیمات موقعیت و ارائه‌دهندهٔ اندروید"))}
            Button(enabled=!working,onClick={task{runtime.locations.current();status=words("Fresh Android fix received","Ubicación reciente de Android recibida","موقعیت تازه از اندروید دریافت شد")}}){Text(words("Request current location","Solicitar ubicación actual","درخواست موقعیت فعلی"))}
            snapshot.point?.let{p->
                Text(String.format(Locale.ROOT,"%.6f, %.6f",p.latitude,p.longitude),style=MaterialTheme.typography.headlineSmall)
                Text(words("Android-reported accuracy","Precisión indicada por Android","دقت گزارش‌شدهٔ اندروید")+String.format(Locale.ROOT,": ±%.0f m",p.accuracy))
                Text(words("Observed","Observada","زمان دریافت")+": "+java.time.Instant.ofEpochMilli(p.observedAt))
                snapshot.address?.let{Text(it)}
                OutlinedButton(enabled=!working,onClick={AlertDialog.Builder(this@LocationHudActivity)
                    .setTitle(words("Ask Android's address provider?","¿Consultar la dirección a Android?","نشانی از سرویس اندروید دریافت شود؟"))
                    .setMessage(words("The Android geocoder may send these coordinates to its configured provider. Availability varies by device; no address is invented.","El geocodificador puede enviar las coordenadas a su proveedor. La disponibilidad depende del dispositivo; no se inventa una dirección.","ژئوکدر اندروید ممکن است مختصات را برای سرویس تنظیم‌شده ارسال کند. دسترسی به دستگاه بستگی دارد؛ نشانی ساخته نمی‌شود."))
                    .setNegativeButton(words("Cancel","Cancelar","لغو"),null).setPositiveButton(words("Look up address","Consultar dirección","دریافت نشانی")){_,_->task{runtime.locations.address(Locale.forLanguageTag(runtime.store.settings.value.language.tag));status=words("Address returned by Android","Android devolvió la dirección","نشانی توسط اندروید ارائه شد")}}.show()
                }){Text(words("Resolve address · may use network","Obtener dirección · puede usar red","دریافت نشانی · ممکن است از شبکه استفاده کند"))}
                Button(enabled=!working,onClick={confirmMap(null)}){Text(words("Load real map · share selected area with OSM","Cargar mapa real · compartir zona con OSM","دریافت نقشهٔ واقعی · ارسال محدوده به OSM"))}
                OutlinedButton(onClick={task{val fresh=runtime.locations.usablePoint();open(Intent(Intent.ACTION_VIEW,Uri.parse("geo:${fresh.latitude},${fresh.longitude}?q=${fresh.latitude},${fresh.longitude}")))}}){Text(words("Open installed map app","Abrir app de mapas instalada","باز کردن برنامهٔ نقشه"))}
                OutlinedTextField(destinationName,{destinationName=it.take(80)},label={Text(words("Destination name","Nombre del destino","نام مقصد"))},modifier=Modifier.fillMaxWidth(),enabled=!working)
                Button(enabled=destinationName.isNotBlank() && !working,onClick={val name=destinationName;authorize{
                    runtime.locations.save(name);destinationName="";loadSaved();status=words("Destination saved privately","Destino guardado en privado","مقصد به‌صورت خصوصی ذخیره شد")
                }}){Text(words("Save current fix as destination","Guardar ubicación como destino","ذخیرهٔ موقعیت فعلی به‌عنوان مقصد"))}
            }
            mapUrl?.let{url->
                Text(mapLabel);Text(words("Map is north-up; this is not a measured device compass heading.","El mapa apunta al norte; no es una brújula medida del dispositivo.","بالای نقشه شمال است؛ این نمایش، قطب‌نمای اندازه‌گیری‌شدهٔ گوشی نیست."))
                key(url){AndroidView(factory={ctx->
                    try {
                        check(WebView.getCurrentWebViewPackage()!=null)
                        WebView(ctx).apply{
                            web=this
                            settings.javaScriptEnabled=true // OSM's documented interactive embed, no app JS bridge.
                            settings.allowFileAccess=false;settings.allowContentAccess=false;settings.domStorageEnabled=false;settings.databaseEnabled=false
                            settings.setGeolocationEnabled(false);settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW;settings.cacheMode=WebSettings.LOAD_NO_CACHE
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this,false);CookieManager.getInstance().setAcceptCookie(false)
                            webViewClient=object:WebViewClient(){
                                override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean {
                                    val u=request.url
                                    if(u.host=="www.openstreetmap.org" && u.scheme=="https" && u.path=="/export/embed.html")return false
                                    if(request.hasGesture() && u.scheme=="https" && u.host=="www.openstreetmap.org")open(Intent(Intent.ACTION_VIEW,u))
                                    return true
                                }
                                override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse? =
                                    if(LocationMapPolicy.permittedResource(request.url.toString()))null else WebResourceResponse("text/plain","UTF-8",ByteArrayInputStream(ByteArray(0)))
                                override fun onReceivedError(view:WebView,request:WebResourceRequest,error:WebResourceError){if(request.isForMainFrame)status=words("Map unavailable. Check network/provider access.","Mapa no disponible. Revisa conexión y proveedor.","نقشه در دسترس نیست. اتصال و سرویس را بررسی کنید.")}
                            }
                            loadUrl(url)
                        }
                    } catch(_:Exception){TextView(ctx).apply{text=words("Android WebView unavailable. Use an installed map app.","WebView no disponible. Usa una app de mapas.","WebView در دسترس نیست. از برنامهٔ نقشه استفاده کنید.")}}
                },modifier=Modifier.fillMaxWidth().height(320.dp),onRelease={view->if(view is WebView){view.stopLoading();view.clearHistory();view.clearCache(true);view.destroy();if(web===view)web=null}},update={})}
                Text("© OpenStreetMap contributors · openstreetmap.org/copyright")
                OutlinedButton(onClick={mapUrl=null}){Text(words("Close map and clear web cache","Cerrar mapa y borrar caché web","بستن نقشه و پاک کردن حافظهٔ وب"))}
            }
            HorizontalDivider();Text(words("Saved destinations","Destinos guardados","مقصدهای ذخیره‌شده"),style=MaterialTheme.typography.titleLarge)
            saved.forEach{d->
                Text(d.name);Text(String.format(Locale.ROOT,"%.6f, %.6f",d.latitude,d.longitude))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    OutlinedButton(onClick={confirmMap(d)}){Text(words("Map","Mapa","نقشه"))}
                    OutlinedButton(onClick={open(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${d.latitude},${d.longitude}")))}){Text(words("Directions · Google Maps","Ruta · Google Maps","مسیریابی · گوگل‌مپ"))}
                    TextButton(onClick={authorize{runtime.locations.remove(d.id);loadSaved()}}){Text(words("Delete","Eliminar","حذف"))}
                }
            }
            OutlinedButton(onClick={runtime.locations.clearTransient();mapUrl=null;status=words("Temporary location cleared","Ubicación temporal borrada","موقعیت موقت پاک شد")}){Text(words("Clear current fix","Borrar ubicación actual","پاک کردن موقعیت فعلی"))}
            if(saved.isNotEmpty())OutlinedButton(onClick={AlertDialog.Builder(this@LocationHudActivity).setTitle(words("Delete every saved destination?","¿Eliminar todos los destinos?","همهٔ مقصدهای ذخیره‌شده حذف شوند؟")).setNegativeButton(words("Cancel","Cancelar","لغو"),null).setPositiveButton(words("Delete","Eliminar","حذف")){_,_->authorize{runtime.locations.removeAll();loadSaved()}}.show()}){Text(words("Delete all saved destinations","Eliminar todos los destinos","حذف همهٔ مقصدها"))}
        }
    }
    private fun loadSaved(){runCatching{runtime.locations.destinations()}.onSuccess{saved=it}.onFailure{status=words("Saved destinations are unavailable; existing encrypted data was not overwritten.","Destinos no disponibles; no se sobrescribieron los datos cifrados.","مقصدها در دسترس نیستند؛ داده‌های رمز‌شده بازنویسی نشدند.")}}
    private fun confirmMap(destination:SavedDestination?){
        AlertDialog.Builder(this).setTitle(words("Load OpenStreetMap?","¿Cargar OpenStreetMap?","نقشهٔ OpenStreetMap دریافت شود؟"))
            .setMessage(words("The selected area/marker and your IP reach OpenStreetMap and its tile servers. Interactive tiles use data. The map is not continuous location tracking.","La zona, marcador e IP llegan a OpenStreetMap y sus servidores. Los mapas consumen datos. No hay seguimiento continuo.","محدوده، نشانگر و IP شما به OpenStreetMap و سرورهای نقشه می‌رسد. نقشه از اینترنت استفاده می‌کند و ردیابی پیوسته نیست."))
            .setNegativeButton(words("Cancel","Cancelar","لغو"),null).setPositiveButton(words("Load map","Cargar mapa","دریافت نقشه")){_,_->task{
                if(destination==null){val point=runtime.locations.usablePoint();mapUrl=LocationMapPolicy.embed(point.latitude,point.longitude,point.accuracy);mapLabel=words("Reported current fix","Ubicación actual reportada","موقعیت فعلی گزارش‌شده")}
                else{mapUrl=LocationMapPolicy.embed(destination.latitude,destination.longitude);mapLabel=words("Saved destination","Destino guardado","مقصد ذخیره‌شده")+": "+destination.name}
            }}.show()
    }
    private fun open(intent:Intent){runCatching{startActivity(intent)}.onFailure{status=words("No app can handle this Android action.","Ninguna app puede realizar esta acción.","برنامه‌ای برای این اقدام اندروید وجود ندارد.")}}
    override fun onResume(){super.onResume();if(isFinishing)return
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED)runtime.locations.clearTransient()
    }
    override fun onStop(){mapUrl=null;web?.apply{stopLoading();clearHistory();clearCache(true)};super.onStop()}
}
