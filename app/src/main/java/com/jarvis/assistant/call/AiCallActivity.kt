package com.jarvis.assistant.call

import android.Manifest
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.JarvisApplication
import com.jarvis.assistant.core.choose

/** Public AI caller identity over lock screen; private captions stay hidden until Android unlock. */
class AiCallActivity:ComponentActivity(){
    private val runtime get()=(application as JarvisApplication).runtime
    private var expectedId by mutableStateOf("")
    private var status by mutableStateOf("")
    private var unlocked by mutableStateOf(false)
    private var pendingAnswer=false
    private var pendingAnswerId=""
    private val microphone=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->
        val requested=pendingAnswer;pendingAnswer=false
        if(granted && requested)runtime.answerAiCall(pendingAnswerId) else if(!granted)status="Allow the microphone to answer this AI voice call. You can decline without granting it."
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);setShowWhenLocked(true)
        expectedId=intent.getStringExtra("call_id").orEmpty();pendingAnswer=savedInstanceState?.getBoolean("pending_answer")==true;pendingAnswerId=savedInstanceState?.getString("pending_answer_id").orEmpty()
        setContent{
            val c by runtime.aiCall.state.collectAsStateWithLifecycle();val shared by runtime.state.collectAsStateWithLifecycle()
            MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF00E5FF),background=Color(0xFF050C14))){
                Surface(Modifier.fillMaxSize()){
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
                        Text("J.A.R.V.I.S.",style=MaterialTheme.typography.headlineLarge)
                        Text(shared.settings.language.choose("AI VOICE CALL","LLAMADA DE VOZ CON IA","تماس صوتی با هوش مصنوعی"))
                        Spacer(Modifier.height(12.dp))
                        Text(if(c.id!=expectedId)"This call is no longer active" else when(c.phase){
                            AiCallPhase.RINGING->"Incoming AI call · ringing"
                            AiCallPhase.CONNECTING->"Connecting audio / preparing microphone…"
                            AiCallPhase.LISTENING->"Connected · listening to you"
                            AiCallPhase.THINKING->"Connected · preparing a response"
                            AiCallPhase.SPEAKING->"Connected · Jarvis is speaking"
                            AiCallPhase.ERROR->"Call ended · ${c.reason}"
                            AiCallPhase.ENDED->"Call ended · ${c.reason}"
                            else->"No active call"
                        },style=MaterialTheme.typography.titleLarge)
                        com.jarvis.assistant.ui.HudCore(shared.copy(authorization=null),Modifier.fillMaxWidth().height(230.dp))
                        if(status.isNotBlank())Text(status)
                        if(c.id==expectedId && c.phase==AiCallPhase.RINGING){
                            Button(onClick={answer()}){Text(shared.settings.language.choose("Answer","Contestar","پاسخ"))}
                            OutlinedButton(onClick={runtime.endAiCall(expectedId,"DECLINED")}){Text(shared.settings.language.choose("Decline","Rechazar","رد تماس"))}
                        }else if(c.id==expectedId && c.active){
                            if(unlocked){
                                shared.transcript.takeIf{it.isNotBlank()}?.let{Text(it.take(500),maxLines=3)}
                                shared.liveReply.takeIf{it.isNotBlank()}?.let{Text(it.take(700),maxLines=4)}
                                shared.error?.let{Text(it)}
                            }else Text("Unlock normally for private answers and protected actions.")
                            Button(onClick={runtime.endAiCall(expectedId,"ENDED_BY_USER")}){Text(shared.settings.language.choose("End call","Terminar llamada","پایان تماس"))}
                        }else {
                            if(unlocked && c.phase==AiCallPhase.ERROR){
                                shared.error?.let{Text(it,maxLines=5)}
                                TextButton(onClick={startActivity(Intent(this@AiCallActivity,com.jarvis.assistant.voice.VoiceSettingsActivity::class.java))}){Text("Output voice settings")}
                                TextButton(onClick={startActivity(Intent(this@AiCallActivity,com.jarvis.assistant.MainActivity::class.java).putExtra("open_settings",true))}){Text("Speech pack / app settings")}
                            }
                            Button(onClick={runtime.requestAiCall(showUi=true)}){Text(shared.settings.language.choose("Call Jarvis again","Volver a llamar a Jarvis","تماس دوباره"))}
                        }
                        TextButton(onClick={finish()}){Text(if(c.id==expectedId && c.active)"Back · call continues in the notification" else "Back to Jarvis")}
                        Text(shared.settings.language.choose("An in-app AI conversation. Use your phone’s media volume for Jarvis speech. No telephone number, carrier call or call-log access is used.","Conversación con IA dentro de la app. Usa el volumen multimedia para la voz. No usa un número telefónico ni llamadas del operador.","گفت‌وگوی صوتی داخل برنامه با هوش مصنوعی."),style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        handleAnswerIntent()
    }
    override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);expectedId=intent.getStringExtra("call_id").orEmpty();status="";handleAnswerIntent()}
    private fun handleAnswerIntent(){if(intent.getBooleanExtra("answer",false)){intent.removeExtra("answer");answer()}}
    private fun answer(){
        val c=runtime.aiCall.state.value
        if(c.id!=expectedId || c.phase!=AiCallPhase.RINGING)return
        val keyguard=getSystemService(KeyguardManager::class.java)
        if(keyguard.isDeviceLocked && !com.jarvis.assistant.voice.WakeListenerService.state.value.running){
            keyguard.requestDismissKeyguard(this,object:KeyguardManager.KeyguardDismissCallback(){override fun onDismissSucceeded(){unlocked=true;if(expectedId==c.id && runtime.aiCall.state.value.id==c.id)answer()};override fun onDismissCancelled(){status="Unlock to start a new microphone session, or decline the call."}});return
        }
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){pendingAnswer=true;pendingAnswerId=c.id;microphone.launch(Manifest.permission.RECORD_AUDIO);return}
        if(!runtime.answerAiCall(expectedId))status="This call is no longer ringing. Start a new call when ready."
    }
    override fun onResume(){super.onResume();unlocked=!getSystemService(KeyguardManager::class.java).isDeviceLocked}
    override fun onStart(){super.onStart();runtime.hud.startVisible(this)}
    override fun onStop(){runtime.hud.stopVisible(this);super.onStop()}
    override fun onSaveInstanceState(outState:Bundle){outState.putBoolean("pending_answer",pendingAnswer);outState.putString("pending_answer_id",pendingAnswerId);super.onSaveInstanceState(outState)}
}
