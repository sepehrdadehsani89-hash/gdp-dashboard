package com.jarvis.assistant.neural

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.jarvis.assistant.JarvisApplication
import com.jarvis.assistant.core.AttachmentReader
import com.jarvis.assistant.core.choose
import com.jarvis.assistant.local.ModelDownloadPolicy
import com.jarvis.assistant.local.ModelInstallPolicy
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Ordinary private settings window. No background launch or microphone permission is requested here. */
class NeuralSettingsActivity:ComponentActivity(){
    private fun tr(value:String)=NeuralStrings.text(runtime.store.settings.value.language,value)
    private val runtime get()=(application as JarvisApplication).runtime
    private var revision by mutableIntStateOf(0)
    private var status by mutableStateOf("")
    private var busy by mutableStateOf(false)
    private var operation:Job?=null
    private val calendarPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->runtime.neuralPreferences.save(runtime.neuralPreferences.state.value.copy(briefingCalendar=granted));BriefingScheduler.sync(this);status=if(granted)"Calendar summary authorized." else "Calendar permission denied; calendar briefing is off."}
    private val notification=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->status=if(granted)"Notifications allowed." else "Notifications denied; scheduled briefings cannot display notifications."}
    private val pick=registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris->
        if(uris.isNotEmpty())runOperation{
            val reader=AttachmentReader(this@NeuralSettingsActivity)
            for(uri in uris.take(5)){
                val name=reader.displayName(uri)
                val text=withContext(Dispatchers.IO){reader.read(uri,runtime.store.settings.value.language)}
                withContext(Dispatchers.IO){runtime.knowledge.index(name,text.take(150_000),source="User-selected: $name")}
            };status="Selected documents indexed locally. They are not uploaded.";revision++
        }
    }
    private val export=registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        if(uri!=null)runOperation{
            val payload=withContext(Dispatchers.IO){JSONObject(runtime.knowledge.export()).put("approved_memories",JSONArray().apply{runtime.store.memories.value.forEach{put(JSONObject().put("text",it.text).put("created_at",it.createdAt))}}).put("app_aliases",JSONObject(runtime.appRegistry.aliases())).toString(2)}
            withContext(Dispatchers.IO){contentResolver.openOutputStream(uri,"wt")?.use{it.write(payload.toByteArray())} ?: error("Export destination unavailable")};status="Private memory/document export written. Provider credentials are excluded."
        }
    }
    private val benchmarkExport=registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->if(uri!=null)runOperation{
        withContext(Dispatchers.IO){val source=File(filesDir,"neural-evaluation.json");check(source.isFile){"Run the evaluation first."};contentResolver.openOutputStream(uri,"wt")?.use{out->source.inputStream().use{it.copyTo(out)}} ?: error("Destination unavailable")};status="Synthetic evaluation exported."
    }}
    private fun runOperation(block:suspend ()->Unit){
        if(busy)return
        busy=true
        operation=lifecycleScope.launch{try{block()}catch(e:CancellationException){status="Cancelled. Interrupted model downloads can resume.";throw e}catch(e:Exception){status=e.message?.take(400) ?: "Operation unavailable."}finally{busy=false;revision++}}
        runtime.neuralOperation=operation
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if(getSystemService(android.app.KeyguardManager::class.java).isDeviceLocked){finish();return}
        setContent{MaterialTheme(colorScheme=darkColorScheme()){
            val settings by runtime.neuralPreferences.state.collectAsState()
            var docs by remember{mutableStateOf(emptyList<KnowledgeDocument>())}
            var audit by remember{mutableStateOf(emptyList<String>())}
            var tasks by remember{mutableStateOf(emptyList<Pair<Long,String>>())}
            var apps by remember{mutableStateOf(emptyList<DiscoveredApp>())}
            var aliases by remember{mutableStateOf(emptyMap<String,String>())}
            var briefing by remember{mutableStateOf("")}
            var metric by remember{mutableStateOf("")}
            var alias by remember{mutableStateOf("")};var appQuery by remember{mutableStateOf("")}
            var task by remember{mutableStateOf("")};var search by remember{mutableStateOf("")};var hits by remember{mutableStateOf("")}
            var selected by remember{mutableStateOf(BrainProfiles.get(settings.profile))}
            val assistantSettings by runtime.store.settings.collectAsState()
            val lang=assistantSettings.language
            LaunchedEffect(revision){withContext(Dispatchers.IO){docs=runtime.knowledge.list();audit=runtime.knowledge.auditLines();tasks=runtime.knowledge.tasks();briefing=runtime.briefing.last();metric=File(filesDir,"local-brain/benchmark-last.json").takeIf{it.isFile}?.readText().orEmpty()};apps=runtime.appRegistry.apps();aliases=runtime.appRegistry.aliases()}
            fun save(next:NeuralSettings){runtime.neuralPreferences.save(next);BriefingScheduler.sync(this@NeuralSettingsActivity)}
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides if(lang==com.jarvis.assistant.core.AppLanguage.FA)androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr){Surface(Modifier.fillMaxSize()){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
                TextButton(onClick={finish()}){Text(lang.choose("Back to Jarvis","Volver a Jarvis","بازگشت به جارویس"))}
                Text("JARVIS INFINITY 7.0",style=MaterialTheme.typography.headlineMedium)
                Text(lang.choose("Local intelligence, explicit sources and permissions.","Inteligencia local, fuentes y permisos explícitos.","هوش محلی با منابع و مجوزهای روشن."))
                OutlinedButton(enabled=!busy,onClick={startActivity(Intent(this@NeuralSettingsActivity,com.jarvis.assistant.infinity.InfinityControlsActivity::class.java))}){Text(lang.choose("Infinity · Wi-Fi, Bluetooth, battery & websites","Infinity · Wi-Fi, Bluetooth, batería y sitios","اینفینیتی · وای‌فای، بلوتوث، باتری و وب‌سایت‌ها"))}
                if(status.isNotBlank())Text(status,color=MaterialTheme.colorScheme.primary)
                if(busy)OutlinedButton(onClick={operation?.cancel();runtime.brain.cancel()}){Text(lang.choose("Cancel operation","Cancelar operación","لغو عملیات"))}
                Text(tr("Local brain"),style=MaterialTheme.typography.titleLarge)
                Text(runtime.brain.modelStatus())
                Text(lang.choose("Optional near-1 GB brain: real Qwen2.5 1.5B Q4_K_M model weights, not filler files or a knowledge encyclopedia. Nothing downloads automatically. Lite is retained. Phone speed and EN/ES/FA quality are not yet measured; a larger model does not guarantee better answers.","Cerebro opcional de casi 1 GB: pesos reales Qwen2.5 1.5B Q4_K_M, no archivos de relleno ni una enciclopedia. No se descarga automáticamente. Lite se conserva. Rendimiento y calidad por idioma aún no medidos.","مغز اختیاری نزدیک ۱ گیگابایت: وزن‌های واقعی مدل Qwen2.5 1.5B Q4_K_M، نه فایل پرکننده یا دانشنامه. دانلود خودکار نیست؛ مدل سبک حفظ می‌شود. سرعت و کیفیت زبان‌ها هنوز روی گوشی اندازه‌گیری نشده است."))
                BrainProfiles.all.forEach{profile->Row{RadioButton(selected.id==profile.id,{selected=profile},enabled=profile.available && !busy);Column{Text(profile.title);Text(if(profile.available)"${if(runtime.brain.installed(profile))"Installed: ${ModelInstallPolicy.formatBytes(runtime.brain.installedBytes(profile))}" else "Not installed"}${if(settings.profile==profile.id)" · selected" else ""}\nEstimated working RAM ${profile.workingMiB} MiB; runtime context ${profile.context} tokens." else "No compatible larger model verified.",style=MaterialTheme.typography.bodySmall)}}}
                fun inspect(p:BrainProfile){runOperation{
                    val descriptor=runtime.brain.inspectDownload(p)
                    val error=runtime.brain.hardwareError(p)
                    val free=android.os.StatFs(filesDir.absolutePath).availableBytes
                    val partial=runtime.brain.resumableBytes(p,descriptor)
                    val disk=ModelInstallPolicy.requiredFreeBytes(descriptor.size,partial)
                    AlertDialog.Builder(this@NeuralSettingsActivity).setTitle(tr("Verified optional model download"))
                        .setMessage("${p.title}\n${p.repository}\nDownload: ${ModelInstallPolicy.formatBytes(descriptor.size)}\nResumable: ${ModelInstallPolicy.formatBytes(partial)}\nAdditional free storage needed: ${ModelInstallPolicy.formatBytes(disk)}; retained models stay on disk\nAvailable: ${ModelInstallPolicy.formatBytes(free)}\nEstimated working RAM: ${p.workingMiB} MiB (not a measurement)\nLicense: Apache-2.0 verified from official metadata\nRevision: ${descriptor.revision}\nSHA-256: ${descriptor.sha256}\n${error ?: "Compatibility checks currently pass; RAM-based blocking is off. Phone performance remains unverified."}\nInstallation does not activate this brain or change your AI provider. Use Wi-Fi; mobile data may cost money. Keep this screen open. Cancel retains resumable bytes.\n${if(free<=disk)"Insufficient free storage." else ""}")
                        .setNegativeButton(tr("Cancel"),null).apply{if(free>disk)setPositiveButton(lang.choose("Install weights","Instalar pesos","نصب وزن‌های مدل")){_,_->runtime.stopEverything();runOperation{runtime.brain.download(descriptor,p){progress->status="Downloading real model weights: ${(progress*100).toInt()}% · final SHA-256 verification required"};status="${p.title} verified and installed. Your current brain and AI provider remain selected. Use the activation button when ready."}}}.show()
                }}
                Button(enabled=!busy,onClick={selected=BrainProfiles.balanced;inspect(BrainProfiles.balanced)}){Text(lang.choose("Install optional near-1 GB brain","Instalar cerebro opcional de casi 1 GB","نصب مغز اختیاری نزدیک ۱ گیگابایت"))}
                OutlinedButton(enabled=!busy && selected.available,onClick={inspect(selected)}){Text(tr("Inspect actual size & license before download"))}
                Button(enabled=!busy && runtime.brain.installed(selected),onClick={runtime.stopEverything();runOperation{runtime.brain.select(selected.id);runtime.useLocal();status="${selected.title} activated for on-device AI. Cloud voice is off. Other models retained."}}){Text(tr("Use selected installed profile"))}
                if(selected.available)runtime.brain.hardwareError(selected)?.let{Text("Execution unavailable now: $it Installation is optional and does not override this check.",style=MaterialTheme.typography.bodySmall)}
                OutlinedButton(enabled=!busy && selected.available && selected.id!=settings.profile,onClick={val p=selected;AlertDialog.Builder(this@NeuralSettingsActivity).setTitle("Remove ${p.title} files?").setMessage("This removes this profile’s weights and interrupted download only. Memories, documents and other brains are kept.").setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Remove")){_,_->runOperation{runtime.brain.removeInstalled(p);status="Model files removed. Other brains and memories retained."}}.show()}){Text(lang.choose("Remove unselected brain files","Quitar archivos del cerebro no seleccionado","حذف فایل‌های مغز انتخاب‌نشده"))}
                Toggle(tr("Eco inference · one CPU thread"),settings.eco){next->runOperation{runtime.brain.release();save(settings.copy(eco=next));status="Inference profile updated."}}
                Text(lang.choose("RAM-based blocking and automatic low-memory cancellation are off. Memory estimates are informational. Android manages actual allocation; severe thermal conditions can still stop inference.","El bloqueo por RAM y la cancelación por memoria baja están desactivados. Las estimaciones son informativas. Android gestiona la memoria real; el calor intenso aún puede detener la inferencia.","محدودیت رم و توقف خودکار به‌علت کمبود حافظه حذف شده است. برآورد حافظه فقط برای اطلاع است. تخصیص واقعی را اندروید مدیریت می‌کند؛ گرمای شدید همچنان می‌تواند استنتاج را متوقف کند."))
                Text(tr("Phone evaluation"),style=MaterialTheme.typography.titleLarge)
                Text(tr("27 paired synthetic EN/ES/FA cases (54 answers), using frozen 3.0 and current prompts in the same runtime. This can take a long time; cancel retains partial results. Records actual model bytes, first-token and load time, token counts, sampled app PSS and thermal status. Human review is required for quality. This is not an original-binary, physical baseline or battery-energy benchmark."))
                Button(enabled=!busy && runtime.brain.modelReady(),onClick={runtime.stopEverything();runOperation{EvaluationCorpus.run(runtime.brain,File(filesDir,"neural-evaluation.json")){status=it};revision++}}){Text(tr("Run local evaluation · no microphone/cloud"))}
                OutlinedButton(enabled=!busy,onClick={benchmarkExport.launch("Jarvis-evaluation.json")}){Text(tr("Export synthetic evaluation"))}
                if(metric.isNotBlank())Text(metric,style=MaterialTheme.typography.bodySmall)
                Text(lang.choose("Approved offline library","Biblioteca local autorizada","کتابخانهٔ محلی مجاز"),style=MaterialTheme.typography.titleLarge)
                Text(tr("Only files you explicitly index are retained here. This is full-text retrieval, not a semantic embedding claim. Indexed sources stay local unless you enable cloud sharing below. Removing a source deletes its index copy."))
                Button(enabled=!busy,onClick={pick.launch(arrayOf("text/*","application/pdf","application/json"))}){Text(tr("Choose documents to index locally"))}
                OutlinedTextField(search,{search=it},label={Text(tr("Search approved knowledge"))},modifier=Modifier.fillMaxWidth())
                Button(enabled=!busy,onClick={runOperation{hits=withContext(Dispatchers.IO){RetrievalPolicy.reference(runtime.knowledge.search(search),4000)}.ifBlank{"No relevant indexed source."}}}){Text(tr("Search offline"))}
                if(hits.isNotBlank())Text(hits)
                docs.forEach{d->Text("${d.title} · ${d.kind}\n${d.source}\n${d.version} · ${d.publication} · ${d.license}",style=MaterialTheme.typography.bodySmall);TextButton(enabled=!busy,onClick={AlertDialog.Builder(this@NeuralSettingsActivity).setTitle(tr("Remove indexed copy?")).setMessage(d.title).setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Remove")){_,_->runOperation{withContext(Dispatchers.IO){runtime.knowledge.remove(d.id)};revision++}}.show()}){Text("Remove ${d.title}")}}
                KnowledgePackSection(busy){pack->runOperation{val count=KnowledgePacks.downloadAndInstall(pack,runtime.knowledge);status="$count licensed reference entries installed locally.";revision++}}
                OutlinedButton(enabled=!busy,onClick={
                    val summary=runtime.neural.conversation.summary().ifBlank{runtime.neural.conversation.history().joinToString("\n"){"${it.role}: ${it.text}"}.take(1800)}
                    if(summary.isBlank())status="No current AI conversation to save."
                    else AlertDialog.Builder(this@NeuralSettingsActivity).setTitle(tr("Save these conversation excerpts as local reference?")).setMessage("These are excerpts, not independently verified facts.\n\n$summary").setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Save approved reference")){_,_->runOperation{withContext(Dispatchers.IO){runtime.knowledge.index("Approved conversation excerpts",summary,"CONVERSATION_SUMMARY","Explicitly approved conversation excerpts","1",java.time.LocalDate.now().toString(),"Private user content")};revision++}}.show()
                }){Text(tr("Review & save current conversation reference"))}
                Text(tr("Conversation & privacy"),style=MaterialTheme.typography.titleLarge)
                Toggle(tr("Save conversation history on this phone"),settings.saveHistory){save(settings.copy(saveHistory=it))}
                Text(tr("When off, new turns remain in memory for this process only. Existing saved history is kept until you delete it. Long-term facts still require an explicit remember request."))
                Text(tr("Automatic saved-history retention (memories are excluded)"))
                Row{listOf(0,1,7,30).forEach{days->TextButton(onClick={save(settings.copy(retentionDays=days))}){Text(if(days==0)"No expiry" else "$days d${if(settings.retentionDays==days)" ✓" else ""}")}}}
                Toggle(tr("Allow indexed excerpts in selected cloud AI requests"),settings.cloudKnowledge){enabled->if(!enabled)save(settings.copy(cloudKnowledge=false)) else AlertDialog.Builder(this@NeuralSettingsActivity).setTitle(tr("Allow cloud processing of relevant indexed excerpts?")).setMessage(tr("Relevant private document excerpts may be sent to your selected AI provider for your questions. API keys are never included. Turn this off at any time.")).setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Allow")){_,_->save(settings.copy(cloudKnowledge=true))}.show()}
                Toggle(tr("Allow local fallback if configured cloud AI fails"),settings.cloudFallbackLocal){save(settings.copy(cloudFallbackLocal=it))}
                OutlinedButton(enabled=!busy,onClick={AlertDialog.Builder(this@NeuralSettingsActivity).setTitle(tr("Export private memories and documents?")).setMessage(tr("Choose a private destination. The export includes approved facts, indexed text, app aliases and tasks; provider keys are excluded.")).setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Choose destination")){_,_->export.launch("Jarvis-private-memory.json")}.show()}){Text(tr("Export memories & approved documents"))}
                OutlinedButton(enabled=!busy,onClick={AlertDialog.Builder(this@NeuralSettingsActivity).setTitle(tr("Clear conversation history?")).setMessage(tr("This removes saved and ephemeral conversation, not long-term memories or indexed documents.")).setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Clear")){_,_->runtime.clearConversation();status="Conversation cleared."}.show()}){Text(tr("Delete conversation history"))}
                Text(tr("App aliases"),style=MaterialTheme.typography.titleLarge)
                Text(tr("Only launchable apps visible to Android are listed. Approximate speech matches ask before launching. Aliases are stored only when you press Save or approve a spoken proposal."))
                OutlinedTextField(alias,{alias=it.take(80)},label={Text(tr("Nickname, e.g. my music app"))},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(appQuery,{appQuery=it.take(100)},label={Text(tr("Filter installed app names"))},modifier=Modifier.fillMaxWidth())
                apps.filter{appQuery.isNotBlank() && it.label.contains(appQuery,true)}.take(8).forEach{app->TextButton(enabled=alias.isNotBlank() && !busy,onClick={runtime.appRegistry.saveAlias(alias,app.packageId);alias="";revision++}){Text("Save alias → ${app.label}")}}
                aliases.forEach{(name,pkg)->Text("$name → ${apps.firstOrNull{it.packageId==pkg}?.label ?: "Unavailable app"}");TextButton(onClick={runtime.appRegistry.removeAlias(name);revision++}){Text("Delete alias $name")}}
                Text(tr("Tasks"),style=MaterialTheme.typography.titleLarge)
                OutlinedTextField(task,{task=it.take(300)},label={Text(tr("New local task"))},modifier=Modifier.fillMaxWidth())
                Button(enabled=task.isNotBlank() && !busy,onClick={runOperation{withContext(Dispatchers.IO){runtime.knowledge.addTask(task)};task="";revision++}}){Text(tr("Save task"))}
                tasks.forEach{(id,text)->TextButton(enabled=!busy,onClick={runOperation{withContext(Dispatchers.IO){runtime.knowledge.completeTask(id)};revision++}}){Text("Complete: $text")}}
                Text(lang.choose("Optional daily briefing","Resumen diario opcional","گزارش روزانهٔ اختیاری"),style=MaterialTheme.typography.titleLarge)
                Text(tr("Opt-in Android background work may run late. Quiet hours and notification settings apply. Notifications contain no calendar/task text and never speak automatically. Missing sources are reported as unavailable."))
                Toggle(tr("Enable daily briefing"),settings.briefing){enabled->save(settings.copy(briefing=enabled));if(enabled && android.os.Build.VERSION.SDK_INT>=33)notification.launch(Manifest.permission.POST_NOTIFICATIONS)}
                OutlinedButton(onClick={android.app.TimePickerDialog(this@NeuralSettingsActivity,{_,h,m->save(settings.copy(briefingHour=h,briefingMinute=m))},settings.briefingHour,settings.briefingMinute,true).show()}){Text(tr("Briefing time %02d:%02d").format(settings.briefingHour,settings.briefingMinute))}
                Toggle(tr("Weather from configured city"),settings.briefingWeather){save(settings.copy(briefingWeather=it))}
                Toggle(tr("News headlines · network request"),settings.briefingNews){save(settings.copy(briefingNews=it))}
                Toggle(tr("Calendar · requires existing calendar permission"),settings.briefingCalendar){enabled->if(enabled && androidx.core.content.ContextCompat.checkSelfPermission(this@NeuralSettingsActivity,Manifest.permission.READ_CALENDAR)!=android.content.pm.PackageManager.PERMISSION_GRANTED)calendarPermission.launch(Manifest.permission.READ_CALENDAR) else save(settings.copy(briefingCalendar=enabled))}
                Toggle(tr("Saved task summary"),settings.briefingTasks){save(settings.copy(briefingTasks=it))}
                OutlinedTextField(settings.newsTopic,{save(settings.copy(newsTopic=it))},label={Text(tr("News topic"))},modifier=Modifier.fillMaxWidth())
                Row{OutlinedButton(onClick={android.app.TimePickerDialog(this@NeuralSettingsActivity,{_,h,_->save(settings.copy(quietStart=h))},settings.quietStart,0,true).show()}){Text("Quiet from ${settings.quietStart}:00")};OutlinedButton(onClick={android.app.TimePickerDialog(this@NeuralSettingsActivity,{_,h,_->save(settings.copy(quietEnd=h))},settings.quietEnd,0,true).show()}){Text("to ${settings.quietEnd}:00")}}
                Button(enabled=!busy,onClick={runOperation{briefing=runtime.briefing.compose().text;status="Briefing prepared from available source results."}}){Text(tr("Prepare briefing now"))}
                Text(briefing)
                OutlinedButton(enabled=!busy && briefing.isNotBlank(),onClick={runtime.speakPrivatePreview(briefing)}){Text(tr("Read first part aloud · on device"))}
                Text(tr("Action audit · last 30 events"),style=MaterialTheme.typography.titleLarge)
                Text(tr("Identifiers, status and time only. No request text, private document content or credentials are logged."))
                audit.forEach{Text(it,style=MaterialTheme.typography.bodySmall)}
            }}}
        }}
    }
    override fun onStop(){super.onStop();if(getSystemService(android.app.KeyguardManager::class.java).isDeviceLocked){operation?.cancel();finish()}}
    override fun onDestroy(){operation?.cancel();super.onDestroy()}
}
@Composable private fun Toggle(label:String,value:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth()){Text(label,Modifier.weight(1f));Switch(value,onChange)}}
