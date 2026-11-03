package com.jarvis.assistant.omnisystem

import android.app.AlertDialog
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.jarvis.assistant.core.AttachmentReader
import com.jarvis.assistant.data.LlmClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

/** User-selected stills only. Provider output is text, never executable tool authority. */
class VisionHudActivity:PrivateModuleActivity(){
    private var images by mutableStateOf(emptyList<SelectedVisionImage>())
    private var output by mutableStateOf("")
    private val generation=AtomicLong()
    private val picker=registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris->
        if(uris.size>2){status=words("Select at most two images.","Selecciona como máximo dos imágenes.","حداکثر دو تصویر انتخاب کنید.");return@registerForActivityResult}
        if(uris.isNotEmpty())load(uris)
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);if(isFinishing)return
        screen(words("JARVIS · IMAGE OBSERVATIONS","JARVIS · OBSERVACIONES DE IMAGEN","جارویس · مشاهدهٔ تصویر")){
            Text(words("Nothing uploads when this screen opens or when images are selected. Local OCR reads text; general descriptions/comparisons require an explicitly approved image-capable cloud model.","Abrir o seleccionar imágenes no las sube. El OCR local lee texto; describir o comparar requiere aprobar un modelo de imágenes en la nube.","باز کردن صفحه یا انتخاب تصاویر چیزی ارسال نمی‌کند. OCR محلی متن را می‌خواند؛ توضیح و مقایسهٔ عمومی به تأیید مدل تصویری ابری نیاز دارد."))
            Button(enabled=!working,onClick={picker.launch(arrayOf("image/*"))}){Text(words("Choose one or two images","Elegir una o dos imágenes","انتخاب یک یا دو تصویر"))}
            images.forEachIndexed{i,image->
                Text(words("Image","Imagen","تصویر")+" ${i+1}")
                Image(image.preview.asImageBitmap(),words("Explicitly selected image","Imagen seleccionada explícitamente","تصویر انتخاب‌شدهٔ صریح"),Modifier.fillMaxWidth().height(180.dp))
            }
            if(images.isNotEmpty()){
                OutlinedButton(enabled=!working,onClick={task{
                    output="";val reader=AttachmentReader(this@VisionHudActivity)
                    val results=images.mapIndexed{i,image->
                        val text=withContext(Dispatchers.IO){reader.read(image.uri,runtime.store.settings.value.language)}
                        "${words("Image","Imagen","تصویر")} ${i+1}\n$text"
                    }
                    output=results.joinToString("\n\n")
                    status=words("Local extracted text only; no cloud image request was made.","Solo texto extraído localmente; no se enviaron imágenes a la nube.","فقط متن استخراج‌شدهٔ محلی؛ درخواست تصویر ابری انجام نشد.")
                }}){Text(words("Read image text locally · OCR","Leer texto localmente · OCR","خواندن متن تصویر روی گوشی · OCR"))}
                Button(enabled=!working,onClick={ask(false)}){Text(words("Describe first image · review sharing","Describir primera imagen · revisar envío","توضیح تصویر اول · تأیید ارسال"))}
                if(images.size==2)Button(enabled=!working,onClick={ask(true)}){Text(words("Compare both images · review sharing","Comparar ambas imágenes · revisar envío","مقایسهٔ دو تصویر · تأیید ارسال"))}
                OutlinedButton(enabled=!working,onClick={clearImages();output=""}){Text(words("Clear selected images","Quitar imágenes seleccionadas","پاک کردن تصاویر انتخابی"))}
            }
            if(output.isNotBlank()){
                Text(output)
                Text(words("AI interpretations can be wrong. Visible observations are not a diagnosis or identification of a person.","La interpretación puede ser incorrecta. Las observaciones no son diagnóstico ni identificación personal.","تفسیر هوش مصنوعی ممکن است نادرست باشد. مشاهدهٔ تصویر، تشخیص پزشکی یا شناسایی هویت فرد نیست."))
                OutlinedButton(enabled=!working,onClick={runtime.speakPrivatePreview(output)}){Text(words("Read preview on device","Leer vista previa en el dispositivo","خواندن پیش‌نمایش روی گوشی"))}
            }
        }
        intent.data?.takeIf{it.scheme=="content" && it.authority=="$packageName.camera-files"}?.let{load(listOf(it))}
    }
    private fun load(uris:List<android.net.Uri>){task{
        clearImages();output=""
        val prepared=uris.map{SelectedImageReader.read(this@VisionHudActivity,it)}
        images=prepared
        status=words("Preview prepared locally; EXIF metadata stripped from cloud copies.","Vista previa preparada localmente; se quitó EXIF de las copias para la nube.","پیش‌نمایش محلی آماده شد؛ فرادادهٔ EXIF از نسخهٔ ارسالی حذف شد.")
    }}
    private fun ask(compare:Boolean){
        val provider=runtime.store.settings.value.provider
        if(provider !in setOf("gemini","openai","anthropic")){
            status=words("Select an image-capable cloud provider in Settings. Local OCR remains available.","Selecciona un proveedor con imágenes en Ajustes. El OCR local sigue disponible.","در تنظیمات، سرویس ابری دارای قابلیت تصویر انتخاب کنید. OCR محلی در دسترس است.");return
        }
        AlertDialog.Builder(this).setTitle(words("Send these resized images to $provider?","¿Enviar estas imágenes reducidas a $provider?","تصاویر کوچک‌شده برای $provider ارسال شوند؟"))
            .setMessage(words("Only the selected JPEG copies and a fixed description/comparison request are sent. EXIF, conversation history, saved memories and phone data are excluded. Your selected model must support images; provider fees may apply.","Solo se envían copias JPEG y una petición fija. Se excluyen EXIF, conversaciones, recuerdos y datos del teléfono. El modelo debe admitir imágenes y puede tener cargos.","فقط نسخه‌های JPEG انتخاب‌شده و درخواست ثابت توضیح یا مقایسه ارسال می‌شود. EXIF، سابقه، حافظه و داده‌های گوشی ارسال نمی‌شوند. مدل باید تصویر را پشتیبانی کند و ممکن است هزینه داشته باشد."))
            .setNegativeButton(words("Cancel","Cancelar","لغو"),null).setPositiveButton(words("Analyze selected copies","Analizar copias seleccionadas","تحلیل نسخه‌های انتخابی")){_,_->task{
                val epoch=generation.incrementAndGet();output=""
                val selected=if(compare)images else images.take(1)
                val result=LlmClient(runtime.store).vision(compare,runtime.store.settings.value.language,selected.map{it.jpeg}){chunk->
                    lifecycleScope.launch(Dispatchers.Main.immediate){if(epoch==generation.get() && operation?.isActive==true)output+=chunk}
                }
                if(epoch==generation.get())output=result
                status=words("Selected provider response; no phone action was executed.","Respuesta del proveedor; no se ejecutó ninguna acción del teléfono.","پاسخ سرویس انتخاب‌شده؛ اقدامی روی گوشی اجرا نشد.")
            }}.show()
    }
    private fun clearImages(){generation.incrementAndGet();images.forEach{it.jpeg.fill(0)};images=emptyList()}
    override fun onStop(){generation.incrementAndGet();operation?.cancel();clearImages();output="";super.onStop()}
}
