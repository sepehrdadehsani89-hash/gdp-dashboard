package com.jarvis.assistant.sentinel

import android.app.NotificationManager
import android.content.Context
import android.media.*
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.jarvis.assistant.R
import com.jarvis.assistant.core.AssistantPhase
import com.jarvis.assistant.hud.AudioSignals
import com.jarvis.assistant.hud.HudSettings
import java.util.concurrent.ConcurrentHashMap

/** Short, original, pre-rendered assets; no repeated decoding or endless ambient loops. */
enum class SentinelSound(val resource:Int,val priority:Int=1,val duration:Long=250) {
    WAKE(R.raw.sentinel_wake,2),REACTOR(R.raw.sentinel_reactor,2,450),READY(R.raw.sentinel_ready,2),
    LISTEN(R.raw.sentinel_listen,2),RECOGNIZED(R.raw.sentinel_recognized,2),PROCESSING(R.raw.sentinel_processing),
    RESPONSE(R.raw.sentinel_response,2),SPEAK_START(R.raw.sentinel_speak_start),SPEAK_STOP(R.raw.sentinel_speak_stop),
    BUTTON(R.raw.sentinel_button,1,70),TAB(R.raw.sentinel_tab),PANEL_OPEN(R.raw.sentinel_panel_open),PANEL_CLOSE(R.raw.sentinel_panel_close),
    SUCCESS(R.raw.sentinel_success,2),PERMISSION(R.raw.sentinel_permission,3),CODEX(R.raw.sentinel_codex,3),IDENTITY(R.raw.sentinel_identity,3),
    AUTH_SUCCESS(R.raw.sentinel_auth_success,3),AUTH_DENIED(R.raw.sentinel_auth_denied,3),NETWORK_LOST(R.raw.sentinel_network_lost,2),NETWORK_RESTORED(R.raw.sentinel_network_restored),
    LOW_BATTERY(R.raw.sentinel_low_battery,2),CHARGE(R.raw.sentinel_charge),UNPLUG(R.raw.sentinel_unplug),ERROR(R.raw.sentinel_error,3),SLEEP(R.raw.sentinel_sleep,2,400)
}
object SoundMixPolicy {
    fun volume(enabled:Boolean,volume:Float,quiet:Boolean,inCall:Boolean,mic:Boolean,micLevel:Float,speaking:Boolean,event:SentinelSound):Float {
        if(!enabled || quiet || inCall || !volume.isFinite())return 0f
        if(mic && (event !in listOf(SentinelSound.WAKE,SentinelSound.LISTEN) || micLevel>.02f))return 0f
        return volume.coerceIn(0f,1f)*(if(mic).12f else if(speaking).15f else 1f)
    }
}
class JarvisSoundEffects(private val context:Context,private val settings:()->HudSettings,private val phase:()->AssistantPhase) {
    private val main=Handler(Looper.getMainLooper())
    private val audio=context.getSystemService(AudioManager::class.java)
    private val notifications=context.getSystemService(NotificationManager::class.java)
    private var pool:SoundPool?=null
    private val ids=ConcurrentHashMap<SentinelSound,Int>()
    private val ready=ConcurrentHashMap.newKeySet<Int>()
    private val streams=ArrayDeque<Pair<Int,Int>>()
    private var focus:AudioFocusRequest?=null
    private var lastAt=-1000L
    private var quietUntil=0L
    @Volatile var lastQueued:SentinelSound?=null;private set
    val loadedCount:Int get()=ready.size
    fun warm(){if(Looper.myLooper()!=Looper.getMainLooper()){main.post{warm()};return};if(pool!=null)return
        val created=SoundPool.Builder().setMaxStreams(2).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
        pool=created
        created.setOnLoadCompleteListener{_,id,status->if(status==0)ready.add(id)}
        SentinelSound.entries.forEach { ids[it]=created.load(context,it.resource,1) }
    }
    fun userPlay(event:SentinelSound){quietUntil=0;play(event)}
    fun hush(milliseconds:Long){silence();quietUntil=SystemClock.elapsedRealtime()+milliseconds}
    fun play(request:SentinelSound){if(Looper.myLooper()!=Looper.getMainLooper()){main.post{play(request)};return}
        if(SystemClock.elapsedRealtime()<quietUntil)return
        val s=settings();val w=s.sentinel
        val event=when {
            request==SentinelSound.WAKE && w.activationSound=="soft"->SentinelSound.READY
            request==SentinelSound.WAKE && w.activationSound=="deep"->SentinelSound.REACTOR
            request==SentinelSound.SLEEP && w.shutdownSound=="silent"->return
            request==SentinelSound.SLEEP && w.shutdownSound=="soft"->SentinelSound.PANEL_CLOSE
            else->request
        }
        val quiet=audio.ringerMode!=AudioManager.RINGER_MODE_NORMAL || notifications.currentInterruptionFilter in listOf(NotificationManager.INTERRUPTION_FILTER_NONE,NotificationManager.INTERRUPTION_FILTER_ALARMS,NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        val mic=AudioSignals.state.value
        val speaking=phase()==AssistantPhase.SPEAKING
        val commandMic=mic.recording && (phase()==AssistantPhase.LISTENING || com.jarvis.assistant.voice.WakeListenerService.state.value.awaitingCommand || !mic.source.startsWith("Wake microphone"))
        val volume=SoundMixPolicy.volume(s.soundEffects,w.effectsVolume,quiet,audio.mode!=AudioManager.MODE_NORMAL,commandMic,mic.level,speaking,event)*(if(mic.recording && !commandMic).25f else 1f)
        if(volume==0f)return
        val now=SystemClock.elapsedRealtime();if(now-lastAt<80 && event.priority<3)return
        warm();val target=pool ?: return;val id=ids[event] ?: return;if(id !in ready)return
        // Never steal speech focus or call focus to play a decorative cue.
        if(!mic.recording && !speaking && event.priority>1){
            if(focus==null){val requestFocus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build())
                .setOnAudioFocusChangeListener{change->if(change<=AudioManager.AUDIOFOCUS_LOSS)main.post{silence()}}.build()
                if(audio.requestAudioFocus(requestFocus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED)return
                focus=requestFocus
            }
        } else if(audio.isMusicActive && !speaking && event.priority==1)return
        while(streams.size>=2){val previous=streams.removeFirst();target.stop(previous.first)}
        val stream=target.play(id,volume,volume,event.priority,0,1f)
        if(stream==0)return
        lastAt=now;lastQueued=event;streams.addLast(stream to event.priority)
        main.postDelayed({target.stop(stream);streams.removeAll{it.first==stream};if(streams.isEmpty()){focus?.let{audio.abandonAudioFocusRequest(it)};focus=null}},event.duration)
    }
    fun silence(){pool?.autoPause();streams.clear();focus?.let{audio.abandonAudioFocusRequest(it)};focus=null}
    fun close(){main.removeCallbacksAndMessages(null);silence();pool?.release();pool=null;ids.clear();ready.clear()}
}
