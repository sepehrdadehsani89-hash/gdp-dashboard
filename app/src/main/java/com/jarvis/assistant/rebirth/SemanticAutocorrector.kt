package com.jarvis.assistant.rebirth

import com.jarvis.assistant.neural.*

data class RecognitionHypothesis(val text:String,val confidence:Float?=null)
enum class CorrectionKind { UNCHANGED, EXACT_ALIAS, SUPPORTED_VARIANT, ALTERNATIVE, PHONETIC, CLARIFY }
data class CorrectedRecognition(val heard:String,val understood:String,val targetPackage:String?=null,val kind:CorrectionKind=CorrectionKind.UNCHANGED,val candidates:List<String> = emptyList())

/** Entity-aware command correction. It cannot create packages, alter numbers or authorize an action. */
object SemanticAutocorrector {
    private val variants=mapOf("instant gram" to "instagram","insta gram" to "instagram","what s app" to "whatsapp","whats app" to "whatsapp","what s up" to "whatsapp","you tube" to "youtube","spot if i" to "spotify","spotty fi" to "spotify","cloud" to "claude")
    fun correct(raw:String,alternatives:List<RecognitionHypothesis>,apps:List<DiscoveredApp>,approvedAliases:Map<String,String> = emptyMap()):CorrectedRecognition {
        val original=raw.trim().take(8000)
        val target=AppMatching.commandTarget(original) ?: return CorrectedRecognition(original,original)
        // Security, recipients, phone numbers and addresses never pass through fuzzy replacement.
        if(target.any(Char::isDigit) || '@' in original || target.length>100)return CorrectedRecognition(original,original)
        val pinned=approvedAliases.entries.firstOrNull{AppMatching.normalize(it.key)==AppMatching.normalize(target)}?.value
        if(pinned!=null){
            val app=apps.firstOrNull{it.packageId==pinned} ?: return CorrectedRecognition(original,original,kind=CorrectionKind.CLARIFY)
            return CorrectedRecognition(original,"open ${app.label}",app.packageId,CorrectionKind.EXACT_ALIAS)
        }
        val exact=AppMatching.match(target,apps).filter{it.exact}
        if(exact.size==1){val app=exact.single().app;return CorrectedRecognition(original,"open ${app.label}",app.packageId,CorrectionKind.EXACT_ALIAS)}
        if(exact.size>1)return CorrectedRecognition(original,original,kind=CorrectionKind.CLARIFY,candidates=exact.map{it.app.label})
        val replacement=variants[AppMatching.normalize(target)] ?: AppMatching.normalize(target).removePrefix("my ").takeIf{it!=AppMatching.normalize(target)}
        if(replacement!=null){
            val matches=apps.filter{AppMatching.normalize(it.label)==replacement || it.aliases.any{alias->AppMatching.normalize(alias)==replacement}}
            if(matches.size==1){val app=matches.single();return CorrectedRecognition(original,"open ${app.label}",app.packageId,CorrectionKind.SUPPORTED_VARIANT)}
        }
        // Alternative hypotheses must preserve the opening intent and resolve an actual app exactly.
        val corroborated=alternatives.take(5).filter{it.confidence==null || it.confidence>=.65f}.mapNotNull{alternative->
            val candidate=AppMatching.commandTarget(alternative.text) ?: return@mapNotNull null
            val match=AppMatching.match(candidate,apps).singleOrNull{it.exact} ?: return@mapNotNull null
            if(AppMatching.match(target,listOf(match.app)).firstOrNull()?.score?.let{it>=.70}!=true)return@mapNotNull null
            match.app
        }.distinctBy{it.packageId}
        if(corroborated.size==1){val app=corroborated.single();return CorrectedRecognition(original,"open ${app.label}",app.packageId,CorrectionKind.ALTERNATIVE)}
        val ranked=AppMatching.match(target,apps)
        if(ranked.isEmpty())return CorrectedRecognition(original,original)
        val best=ranked.first()
        if(best.score>=.90 && (ranked.size==1 || best.score-ranked[1].score>=.14))return CorrectedRecognition(original,"open ${best.app.label}",best.app.packageId,CorrectionKind.PHONETIC)
        return CorrectedRecognition(original,original,kind=CorrectionKind.CLARIFY,candidates=ranked.take(3).map{it.app.label})
    }
}

object RebirthFastCommands {
    private fun normalized(text:String)=AppMatching.normalize(text).removePrefix("hey jarvis ").removePrefix("oye jarvis ").removePrefix("jarvis ")
    fun openSelf(text:String)=normalized(text) in setOf("let s cook","lets cook","open yourself","open jarvis","abre jarvis","abrete")
    fun homeScreen(text:String)=normalized(text) in setOf("go to the home screen","go to home screen","open home screen","show home screen","ve a la pantalla de inicio")
    fun socialReply(text:String,spanish:Boolean,confidentOwner:Boolean):com.jarvis.assistant.core.AssistantReply?=
        social(text,spanish,confidentOwner)?.let{com.jarvis.assistant.core.AssistantReply(it,allowCloudSpeech=true)}
    fun social(text:String,spanish:Boolean,confidentOwner:Boolean):String? {
        val sir=if(confidentOwner)if(spanish)", señor" else ", sir" else ""
        return when(normalized(text)){
            "thank you","thanks","thank you jarvis","thanks jarvis","gracias","gracias jarvis"->if(spanish)"De nada$sir." else "You're welcome$sir."
            "are you ready","ready","estas listo"->if(spanish)"Listo$sir." else "Ready$sir."
            "never mind","nevermind","forget it","no importa","olvidalo"->if(spanish)"Entendido$sir." else "Understood$sir."
            else->null
        }
    }
}
