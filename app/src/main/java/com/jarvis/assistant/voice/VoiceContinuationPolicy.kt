package com.jarvis.assistant.voice

/** Capture permission is separate from permission to answer with private data or unlock Android. */
object VoiceContinuationPolicy {
    fun followUp(requested:Boolean,locked:Boolean,allowLockedChat:Boolean,publicPrompt:Boolean=false,privateReply:Boolean=false,systemPrivacy:Boolean=false):Boolean =
        requested && !systemPrivacy && (!locked || !privateReply && (publicPrompt || allowLockedChat))
}
