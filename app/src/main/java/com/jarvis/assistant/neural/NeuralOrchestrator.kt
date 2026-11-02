package com.jarvis.assistant.neural

import android.content.Context
import com.jarvis.assistant.core.*
import com.jarvis.assistant.hud.HudRepository
import com.jarvis.assistant.tools.DeviceTools
import com.jarvis.assistant.local.LocalBrain
import kotlinx.coroutines.*

/** Only validated requests from the current user's input enter tools. Indexed text never does. */
class NeuralOrchestrator(private val context:Context,private val hud:HudRepository,private val tools:DeviceTools,private val index:KnowledgeIndex,private val brain:LocalBrain,private val preferences:NeuralPreferences,private val briefing:BriefingService){
    val conversation=ConversationState()
    private val forecast=ForecastService(hud::acceptWeatherReading)
    private val research=WebResearch()
    private fun locked()=context.getSystemService(android.app.KeyguardManager::class.java).isDeviceLocked
    suspend fun route(text:String,lang:AppLanguage):AssistantReply?{
        if(locked())return null
        if(ContextPrivacyPolicy.localOnly(text) && (ForecastPolicy.request(text) || Regex("news|headline|noticias|اخبار|research|investiga|خواندن منبع",RegexOption.IGNORE_CASE).containsMatchIn(text)))return AssistantReply(lang.choose("This request needs a live source. Local-only mode makes no network request; ask again with network access if you want fresh information.","La información actual requiere una fuente en línea. No hice una solicitud de red en modo local.","اطلاعات زنده به منبع آنلاین نیاز دارد؛ در حالت فقط محلی هیچ درخواست شبکه‌ای انجام نشد."))
        val researchUrl=Regex("^(?:research|read source|investiga|خواندن منبع) +(?<url>https://[^\\s]+)$",RegexOption.IGNORE_CASE).matchEntire(text.trim())?.groups?.get("url")?.value
        if(researchUrl!=null)return research.read(researchUrl,lang)
        val alias=Regex("^(?:jarvis[, ]+)?remember that when I say (.{2,80}?),? I mean (.{2,80})[.!]?$",RegexOption.IGNORE_CASE).matchEntire(text.trim())
        if(alias!=null)return tools.requestAlias(alias.groupValues[1],alias.groupValues[2].trimEnd('.','!'),lang)
        ToolRegistry.directPlan(text)?.let{return executePlan(text,it,lang)}
        if(text.startsWith("plan:",true)){
            val input=text.substringAfter(':').trim()
            return try{executePlan(input,proposedPlan(input,lang),lang)}catch(e:CancellationException){throw e}catch(_:Exception){AssistantReply(lang.choose("I couldn't validate a safe local plan. Try a direct request; no proposed action was executed.","No pude validar un plan seguro. Prueba una petición directa.","طرح محلی امن تأیید نشد؛ یک درخواست مستقیم بدهید. اقدامی اجرا نشد."))}
        }
        val t=AppMatching.normalize(text)
        if(t in setOf("daily briefing","my briefing","what should i know about today","resumen del dia","گزارش روزانه","امروز چه خبر"))return briefing.compose()
        val task=Regex("^(?:add task|remember task|agrega tarea|کار اضافه کن)[: ]+(.+)$",RegexOption.IGNORE_CASE).matchEntire(text.trim())
        if(task!=null){withContext(Dispatchers.IO){index.addTask(task.groupValues[1])};return AssistantReply(lang.choose("Task saved in Neural Core. It is not a scheduled reminder.","Tarea guardada en Neural Core. No es una alarma.","کار در هستهٔ عصبی ذخیره شد؛ یادآور زمان‌بندی‌شده نیست."))}
        val follow=conversation.weatherFollowUp(text,android.os.SystemClock.elapsedRealtime())
        if(follow!=null || ForecastPolicy.request(text)){
            val city=ForecastPolicy.city(text) ?: if(follow!=null)conversation.weatherCity else hud.state.value.settings.weatherCity
            if(city.isNullOrBlank())return AssistantReply(lang.choose("Which city? Set one in Settings or say weather in Quito.","¿Qué ciudad? Configúrala o di clima en Quito.","کدام شهر؟ در تنظیمات شهر را مشخص کنید یا بگویید هوای تهران."))
            val day=follow ?: ForecastPolicy.day(text)
            NeuralEvents.emit(NeuralEvent.TOOL_RUNNING,"Checking forecast")
            val result=try{withTimeout(30_000){forecast.reply(city,day,lang,Regex("umbrella|paraguas|چتر",RegexOption.IGNORE_CASE).containsMatchIn(text))}}catch(e:CancellationException){throw e}catch(e:Exception){return AssistantReply(e.message?.take(240) ?: "Weather unavailable.")}
            NeuralEvents.emit(NeuralEvent.TOOL_COMPLETED,"Forecast available")
            conversation.weather(city,day,android.os.SystemClock.elapsedRealtime());withContext(Dispatchers.IO){index.audit("weather","verified_source_fields")}
            return result
        }
        if(Regex("^(search my documents|search offline|busca en mis documentos|جستجو در اسناد)[: ]+",RegexOption.IGNORE_CASE).containsMatchIn(text)){
            val hits=withContext(Dispatchers.IO){index.search(text.substringAfter(':',text))}
            return AssistantReply(if(hits.isEmpty())lang.choose("No relevant indexed source found. Add an approved document or knowledge pack.","No hay fuentes pertinentes indexadas.","منبع مرتبطی در نمایه یافت نشد.") else RetrievalPolicy.reference(hits,4000))
        }
        // Deterministic app resolution runs before model-based classification.
        val app=AppMatching.commandTarget(text)
        if(app!=null && tools.appRegistry.apps().let{AppMatching.match(app,it).isNotEmpty()})return tools.requestApp(app,lang)
        return null
    }
    /** Explicit opt-in local classification, isolated from documents/history and bounded to 3 steps. */
    suspend fun proposedPlan(input:String,lang:AppLanguage):List<ToolRequest>{
        check(!locked())
        val output=brain.answer(ToolRegistry.modelPrompt(input),lang,emptyList(),{},concise=true)
        val plan=ToolRegistry.parse(output)
        require(plan.all{ToolRegistry.authorizedByInput(it,input)}){"The proposed tools do not match your explicit request."}
        return plan
    }
    suspend fun executePlan(input:String,plan:List<ToolRequest>,lang:AppLanguage):AssistantReply{
        check(!locked());require(plan.size in 1..3)
        require(plan.all{ToolRegistry.authorizedByInput(it,input) && ToolRegistry.publicArgumentsBound(it,input,hud.state.value.settings.weatherCity)}){"A proposed argument did not match your request."}
        val results=mutableListOf<AssistantReply>()
        for(step in plan){
            require(ToolRegistry.authorizedByInput(step,input));val definition=ToolRegistry.definitions.getValue(step.id)
            NeuralEvents.emit(NeuralEvent.TOOL_RUNNING,"Checking ${definition.id}")
            val result=withTimeout(definition.timeoutMs){when(step.id){
                "time"->tools.route("what time is it",lang)!!
                "battery"->tools.route("battery",lang)!!
                "weather"->{val (city,day)=ToolRegistry.weatherArguments(step,input,hud.state.value.settings.weatherCity);forecast.reply(city,day,lang)}
                "news"->tools.news("news about ${step.arguments["topic"].orEmpty()}",lang)
                "calendar"->PhoneContext(context).route("my calendar today",lang)!!
                "open_app"->{val target=step.arguments["name"] ?: error("Explicit app target required");tools.requestApp(target,lang,alwaysConfirm=true)}
                "knowledge"->{val hits=withContext(Dispatchers.IO){index.search(input)};AssistantReply(if(hits.isEmpty())"No relevant approved offline source." else RetrievalPolicy.reference(hits))}
                else->error("Unsupported tool")
            }}
            check(!locked()){"Unlock before viewing private results."}
            withContext(Dispatchers.IO){index.audit(step.id,if(result.action!=null)"awaiting_confirmation" else "result_returned")}
            NeuralEvents.emit(NeuralEvent.TOOL_COMPLETED,"${definition.id} result available")
            results+=result
            if(result.action!=null)break // Never execute later steps behind a pending approval.
        }
        return AssistantReply(results.joinToString("\n\n"){it.text},results.flatMap{it.sources},results.lastOrNull()?.action)
    }
}
