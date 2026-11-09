package com.jarvis.assistant.phantom

import android.app.*
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import com.jarvis.assistant.AssistantRuntime
import com.jarvis.assistant.core.AssistantPhase
import com.jarvis.assistant.security.SystemCredentialPrivacy
import com.jarvis.assistant.sentinel.JarvisWallpaperService
import kotlinx.coroutines.*
import kotlin.math.*

/** Process-owned windows over ordinary apps. Reuses the existing runtime; owns no mic or AI. */
class PhantomOverlay(private val context:Context,private val runtime:AssistantRuntime,private val scope:CoroutineScope) {
    private val manager=context.getSystemService(WindowManager::class.java)
    private val activities=java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Activity,Boolean>())
    private var capsule:LinearLayout?=null
    private var card:LinearLayout?=null
    private var label:TextView?=null
    private var body:TextView?=null
    private var reactor:MiniReactor?=null
    val isShowing:Boolean get()=capsule!=null
    private var minimized=false
    @Volatile private var dismissed:Long?=null
    @Volatile private var suppressed=false
    private var started=false
    private var dragging=false
    private var lastLayout=""
    fun start(){
        if(started)return;started=true
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(object:Application.ActivityLifecycleCallbacks{
            override fun onActivityStarted(a:Activity){activities.add(a);hide()}
            override fun onActivityStopped(a:Activity){activities.remove(a)}
            override fun onActivityDestroyed(a:Activity){activities.remove(a)}
            override fun onActivityCreated(a:Activity,b:Bundle?){}
            override fun onActivityResumed(a:Activity){}
            override fun onActivityPaused(a:Activity){}
            override fun onActivitySaveInstanceState(a:Activity,b:Bundle){}
        })
        scope.launch{while(isActive){refresh();delay(350)}}
    }
    fun minimize(){minimized=true;lastLayout=""}
    fun reveal(){suppressed=false;dismissed=null}
    fun dismissCurrent(){suppressed=true;dismissed=runtime.phantom.state.value.trace?.id;hide()}
    fun hide(){val wasShowing=capsule!=null || card!=null;capsule?.let{runCatching{manager.removeViewImmediate(it)}};card?.let{runCatching{manager.removeViewImmediate(it)}};capsule=null;card=null;label=null;body=null;reactor=null;dragging=false;lastLayout="";if(wasShowing)runtime.hud.stopVisible(this)}
    private fun refresh(){
        val state=runtime.state.value;val p=runtime.phantomPreferences.state.value;val now=SystemClock.elapsedRealtime()
        val locked=context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked!=false || !context.getSystemService(PowerManager::class.java).isInteractive
        val mode=PhantomPolicy.presentation(locked,SystemCredentialPrivacy.pending(context),state.phase==AssistantPhase.PAUSED,activities.isNotEmpty(),JarvisWallpaperService.homeVisible,p.enabled,Settings.canDrawOverlays(context))
        val current=runtime.phantom.state.value
        val active=current.execution !in setOf(ExecutionState.IDLE,ExecutionState.CANCELLED,ExecutionState.COMPLETED,ExecutionState.FAILED,ExecutionState.WAITING_USER)
        if(suppressed || mode!=Presentation.OVERLAY || current.trace!=null && dismissed==current.trace.id || p.autoHide && !active && !current.caption.visible(now)){hide();return}
        try{
            if(capsule==null)create()
            label?.text=when(current.execution){ExecutionState.GENERATING_AUDIO->"VOICE…";ExecutionState.SPEAKING->"SPEAKING";ExecutionState.LISTENING,ExecutionState.TRANSCRIBING,ExecutionState.WAKE->"LISTENING";ExecutionState.FAILED->"RETRY";ExecutionState.COMPLETED,ExecutionState.IDLE->"JARVIS";else->current.execution.name.replace('_',' ')}
            reactor?.set(current.execution,state.audio.level,state.hud.settings.reducedMotion || !state.hud.telemetry.systemMotion || p.reducedMotion || state.performanceRestricted,p.speed)
            body?.textSize=p.captionSp
            val caption=current.caption.takeIf{it.visible(now)}
            val powerLine=if(state.activeTask=="BATTERY")com.jarvis.assistant.power.ChargingText.strip(state.hud.power,now,state.settings.language==com.jarvis.assistant.core.AppLanguage.ES)+"\n"else ""
            body?.text=(state.activeTask+"\n"+powerLine+(caption?.assistant?.ifBlank{caption.user} ?: "Tap Expand for the current Jarvis panel.")).take(1800)
            body?.maxLines=p.lines+1
            body?.setTextColor(if(state.activeTask=="BATTERY")com.jarvis.assistant.titan.BatteryPolicy.band(state.hud.telemetry.battery.percent).argb.toInt() else Color.WHITE)
            card?.visibility=if(minimized || caption==null)View.GONE else View.VISIBLE
            capsule?.alpha=p.opacity;card?.alpha=p.opacity
            if(!dragging)layout()
        }catch(_:Exception){hide()} // Revocation / display replacement must not crash or leave a ghost window.
    }
    private fun background()=GradientDrawable().apply{setColor(Color.rgb(5,9,22));cornerRadius=dp(16).toFloat();setStroke(dp(1),Color.argb((255*runtime.phantomPreferences.state.value.glow).toInt(),124,77,255))}
    private fun text(value:String,size:Float=11f)=TextView(context).apply{text=value;textSize=size;setTextColor(Color.WHITE);gravity=Gravity.CENTER_VERTICAL;setPadding(dp(5),dp(4),dp(5),dp(4));importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_YES}
    private fun create(){
        val root=LinearLayout(context).apply{orientation=LinearLayout.HORIZONTAL;background=background();gravity=Gravity.CENTER_VERTICAL;contentDescription="Jarvis floating assistant. Tap to minimize or restore; enable Edit position to drag."}
        reactor=MiniReactor(context);root.addView(reactor,LinearLayout.LayoutParams(dp(32),dp(32)))
        label=text("JARVIS",10f);root.addView(label,LinearLayout.LayoutParams(0,-1,1f))
        root.addView(text("×",22f).apply{contentDescription="Close floating Jarvis until next request";setOnClickListener{dismissCurrent()}},LinearLayout.LayoutParams(dp(36),-1))
        root.setOnClickListener{minimized=!minimized;lastLayout=""}
        drag(root)
        capsule=root;manager.addView(root,params(dp(156),dp(52)))
        val task=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;background=background();setPadding(dp(8),dp(3),dp(8),dp(3))}
        body=text("",12f);task.addView(body,LinearLayout.LayoutParams(-1,-2))
        task.addView(text("Expand ↗",12f).apply{contentDescription="Open current full Jarvis interface";setOnClickListener{runtime.expandPhantom(true)}})
        card=task;manager.addView(task,params(dp(210),-2));runtime.hud.startVisible(this,includeWeather=false);layout()
    }
    private fun params(w:Int,h:Int)=WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.LEFT;title="Jarvis Phantom"}
    private fun bounds():Rect {
        if(Build.VERSION.SDK_INT>=30){val m=manager.currentWindowMetrics;val i=m.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout());return Rect(m.bounds.left+i.left,m.bounds.top+i.top,m.bounds.right-i.right,m.bounds.bottom-i.bottom)}
        val metrics=context.resources.displayMetrics
        // API29 safe fallback; overlays remain below system windows. Conservative margins avoid cutouts.
        val insets=capsule?.rootWindowInsets;val cutout=insets?.displayCutout
        return Rect(max(dp(12),cutout?.safeInsetLeft ?: 0),max(dp(64),max(insets?.stableInsetTop ?: 0,cutout?.safeInsetTop ?: 0)),metrics.widthPixels-max(dp(12),cutout?.safeInsetRight ?: 0),metrics.heightPixels-max(dp(64),max(insets?.stableInsetBottom ?: 0,cutout?.safeInsetBottom ?: 0)))
    }
    private fun layout(){
        val root=capsule ?: return;val task=card ?: return;val p=runtime.phantomPreferences.state.value;val b=bounds();val gap=dp(8)
        val width=(dp(156)*p.scale*context.resources.configuration.fontScale.coerceIn(1f,1.35f)).toInt().coerceAtMost(b.width().coerceAtLeast(1))
        val height=(dp(52)*context.resources.configuration.fontScale.coerceIn(1f,1.8f)).toInt().coerceAtMost(b.height().coerceAtLeast(1))
        val x=b.left+PhantomPolicy.clampPosition(p.x,b.width()-width);val y=b.top+gap+PhantomPolicy.clampPosition(p.y,b.height()-height-gap)
        val cardWidth=(dp(p.cardWidth.toInt())*p.scale).toInt().coerceAtMost(b.width().coerceAtLeast(1))
        val requestedHeight=(dp(70+20*p.lines)*context.resources.configuration.fontScale.coerceIn(1f,2f)).toInt()
        val cardX=b.right-cardWidth
        var cardY=b.top+gap
        var availableHeight=b.height()-gap
        if(x+width+gap>cardX){
            val below=b.bottom-(y+height+gap);val above=y-b.top-gap
            if(below>=dp(70)){cardY=y+height+gap;availableHeight=below}
            else if(above>=dp(70)){availableHeight=above;cardY=y-gap-min(requestedHeight,above)}
            else{availableHeight=0;task.visibility=View.GONE}
        }
        val cardHeight=requestedHeight.coerceAtMost(availableHeight.coerceAtLeast(1))
        val key="$b|$width|$height|$x|$y|$cardWidth|$cardY|$cardHeight"
        if(lastLayout==key)return;lastLayout=key
        (root.layoutParams as WindowManager.LayoutParams).apply{this.width=width;this.height=height;this.x=x;this.y=y;manager.updateViewLayout(root,this)}
        (task.layoutParams as WindowManager.LayoutParams).apply{this.width=cardWidth;this.height=cardHeight;this.x=cardX;this.y=cardY;manager.updateViewLayout(task,this)}
    }
    private fun drag(view:View){var downX=0f;var downY=0f;var startX=0;var startY=0;var moved=false
        view.setOnTouchListener{_,e->
            if(!runtime.phantomPreferences.state.value.editMode)return@setOnTouchListener false
            val lp=view.layoutParams as WindowManager.LayoutParams
            when(e.actionMasked){MotionEvent.ACTION_DOWN->{downX=e.rawX;downY=e.rawY;startX=lp.x;startY=lp.y;moved=false;dragging=true;true}
                MotionEvent.ACTION_MOVE->{moved=moved || abs(e.rawX-downX)+abs(e.rawY-downY)>dp(6);val b=bounds();lp.x=(startX+e.rawX-downX).toInt().coerceIn(b.left,max(b.left,b.right-lp.width));lp.y=(startY+e.rawY-downY).toInt().coerceIn(b.top,max(b.top,b.bottom-lp.height));runCatching{manager.updateViewLayout(view,lp)};true}
                MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{dragging=false;val b=bounds();val p=runtime.phantomPreferences.state.value;runtime.phantomPreferences.save(p.copy(x=(lp.x-b.left).toFloat()/max(1,b.width()-lp.width),y=(lp.y-b.top-dp(8)).toFloat()/max(1,b.height()-lp.height-dp(8))));lastLayout="";if(!moved && e.actionMasked==MotionEvent.ACTION_UP)view.performClick();true}
                else->false}
        }
    }
    private fun dp(value:Int)=(context.resources.displayMetrics.density*value).roundToInt()
    private class MiniReactor(context:Context):View(context){
        private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;strokeWidth=3f};private var execution=ExecutionState.IDLE;private var level=0f;private var angle=0f
        fun set(s:ExecutionState,audio:Float,reduced:Boolean,speed:Float){if(s!=execution || !reduced){execution=s;level=if(reduced || speed==0f)0f else audio.coerceIn(0f,1f);angle=if(reduced)0f else (android.os.SystemClock.elapsedRealtime()%10000)*.036f*speed;invalidate()}}
        override fun onDraw(c:Canvas){super.onDraw(c);paint.color=when(execution){ExecutionState.SPEAKING->0xff00e5ff.toInt();ExecutionState.IDLE,ExecutionState.COMPLETED,ExecutionState.CANCELLED->0xff3278ff.toInt();ExecutionState.LISTENING,ExecutionState.TRANSCRIBING,ExecutionState.WAKE->0xffa855f7.toInt();else->0xff7c4dff.toInt()};val r=min(width,height)*.34f;c.drawCircle(width/2f,height/2f,r,paint);if(execution in setOf(ExecutionState.GENERATING_RESPONSE,ExecutionState.GENERATING_AUDIO,ExecutionState.EXECUTING)){c.drawArc(width/2f-r*.75f,height/2f-r*.75f,width/2f+r*.75f,height/2f+r*.75f,angle,70f,false,paint)};c.drawCircle(width/2f,height/2f,r*(.5f+if(execution in setOf(ExecutionState.LISTENING,ExecutionState.TRANSCRIBING))level*.25f else 0f),paint)}
    }
}
