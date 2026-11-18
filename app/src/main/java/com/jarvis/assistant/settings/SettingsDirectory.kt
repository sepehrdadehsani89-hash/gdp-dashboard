package com.jarvis.assistant.settings

data class SettingsSection(val id:String,val title:String,val detail:String,val keywords:String)
object SettingsDirectory {
    val sections=listOf(
        SettingsSection("account","Account & Identity","Name and saved personal preferences","profile owner"),
        SettingsSection("ai","AI Brain & Providers","Verified model access and local models","chatgpt claude gemini perplexity local inference authentication"),
        SettingsSection("voice","Voice & Microphone","Speech, wake listening and audio diagnostics","cooldown captions echo noise stop speaking"),
        SettingsSection("device","Device Control","Supported Android actions and contact nicknames","flashlight volume brightness wifi bluetooth battery timers alarm contacts ma mom"),
        SettingsSection("security","Security & Permissions","Android authentication and access controls","privacy screen lock pin unlock permission"),
        SettingsSection("appearance","Appearance & Wallpaper","Existing holographic core, motion and wallpaper","reduced motion particles charging glow color rings"),
        SettingsSection("automation","Notifications & Automation","Existing briefings and scheduled actions","routine neural reminders notification"),
        SettingsSection("language","Language & Accessibility","Supported languages and motion preference","english spanish idioma subtitles accessibility"),
        SettingsSection("diagnostics","Diagnostics","Real voice, inference and system status","error microphone test retry repair health"),
        SettingsSection("about","About Jarvis","Version, setup and data handling","setup wizard onboarding version release privacy"))
    fun search(query:String):List<SettingsSection>{
        val words=com.jarvis.assistant.neural.AppMatching.normalize(query.take(160)).split(' ').filter{it.isNotBlank()}
        return sections.filter{section->val text=com.jarvis.assistant.neural.AppMatching.normalize("${section.title} ${section.detail} ${section.keywords}");words.all{text.contains(it)}}
    }
}
