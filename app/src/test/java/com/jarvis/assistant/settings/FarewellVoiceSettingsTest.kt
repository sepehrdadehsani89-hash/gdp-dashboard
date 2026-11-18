package com.jarvis.assistant.settings

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class FarewellVoiceSettingsTest(private val text:String,private val id:String?,private val value:String?){
    @Test fun voiceSettingsStayDeterministicAndCredentialSafe(){val request=SpokenSettings.parse(text);if(id==null){assertTrue(request==null || request is SpokenSettingRequest.PrivateCredential)}else{assertTrue(request is SpokenSettingRequest.Edit);request as SpokenSettingRequest.Edit;assertEquals(id,request.setting.id);assertEquals(value,request.value);if(id=="farewell_context")assertTrue(request.setting.confirmation)}}
    companion object{@JvmStatic @Parameterized.Parameters(name="{0}") fun cases()=listOf(
        arrayOf("set farewell style to formal","farewell_style","formal"),arrayOf("set farewell style to short","farewell_style","short"),
        arrayOf("change goodbye style to friendly","farewell_style","friendly"),arrayOf("set farewell style to relaxed","farewell_style","relaxed"),
        arrayOf("set farewell style to focus","farewell_style","focus"),arrayOf("set farewell style to general","farewell_style","general"),
        arrayOf("set farewell style to auto","farewell_style","auto"),arrayOf("turn farewell courtesy off","farewell_courtesy","false"),
        arrayOf("turn farewell courtesy on","farewell_courtesy","true"),arrayOf("disable sir preference","farewell_courtesy","false"),
        arrayOf("enable sir preference","farewell_courtesy","true"),arrayOf("enable farewell private context","farewell_context","true"),
        arrayOf("disable farewell private context","farewell_context","false"),arrayOf("turn farewell reminder context on","farewell_context","true"),
        arrayOf("turn farewell reminder context off","farewell_context","false"),arrayOf("use perplexity as my ai brain","brain","perplexity"),
        arrayOf("set ai brain to perplexity","brain","perplexity"),arrayOf("Do not enable farewell private context",null,null),
        arrayOf("What does farewell mean?",null,null),arrayOf("save perplexity api key secret-value",null,null),
        arrayOf("remember google access token private-token",null,null),arrayOf("How do I change farewell style?",null,null)
    )}
}
