package com.jarvis.assistant.sentinel

import android.graphics.*
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.choose
import com.jarvis.assistant.hud.*
import kotlin.math.*
import java.util.Locale

/** Captions are ephemeral, explicitly opted in, and stripped at render time when locked. */
data class WallpaperFrame(val hud:HudState=HudState(),val audio:AudioReading=AudioReading(),val visual:SentinelVisualState=SentinelVisualState(),val language:AppLanguage=AppLanguage.EN,val playback:PlaybackReading=PlaybackReading(),val followUpArming:Boolean=false,val commandDeadlineElapsed:Long=0,val caption:com.jarvis.assistant.phantom.LiveCaption=com.jarvis.assistant.phantom.LiveCaption(),val captionLines:Int=3)

/** Native dynamic artwork. Geometry/paints are reused; no video, full-screen blur or random sensor values. */
class WallpaperRenderer {
    private val line=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;strokeCap=Paint.Cap.BUTT}
    private val fill=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.FILL}
    private val text=Paint(Paint.ANTI_ALIAS_FLAG).apply{typeface=Typeface.create("sans-serif-light",Typeface.NORMAL);textAlign=Paint.Align.CENTER}
    private val ring=RectF()
    private val pointer=Path()
    private var w=0;private var h=0;private var cx=0f;private var cy=0f;private var radius=0f
    private var lastScale=-1f
    private var glow:RadialGradient?=null
    private var glowColor=0
    private val labelCache=object:LinkedHashMap<String,android.text.StaticLayout>(12,.75f,true){override fun removeEldestEntry(eldest:MutableMap.MutableEntry<String,android.text.StaticLayout>?)=size>12}
    private var clockKey="";private var clock="";private var date=""
    private var captionKey=""
    private var captionLayout:android.text.StaticLayout?=null
    fun clearCaption(){captionKey="";captionLayout=null}
    fun draw(canvas:Canvas,frame:WallpaperFrame,now:Long) {
        val s=frame.hud.settings;val v=s.sentinel;val battery=frame.hud.telemetry.battery
        val mode=frame.visual.mode
        val motion=frame.hud.telemetry.systemMotion && !s.reducedMotion && v.animationSpeed>0
        val dormant=mode==SentinelMode.DORMANT
        val returning=if(mode==SentinelMode.RETURNING_TO_WALLPAPER)((now-frame.visual.returningAt)/600f).coerceIn(0f,1f) else 0f
        val activation=if(mode==SentinelMode.ACTIVATING && motion)ReactorGeometry.awakening(now-frame.visual.activatedAt) else Awakening(1f,1f,1f,0f,true)
        val energy=if(frame.audio.recording && mode in listOf(SentinelMode.LISTENING,SentinelMode.ACTIVATING))frame.audio.level else if(frame.playback.active)frame.playback.level else 0f
        val active=if(dormant){if(v.cinematicWallpaper).35f else .2f} else (1-returning*.8f)
        val color=ReactorPalette.core(mode,s.accent)
        val scale=v.reactorScale
        if(w!=canvas.width || h!=canvas.height || scale!=lastScale || color!=glowColor){
            w=canvas.width;h=canvas.height;cx=w*.5f;cy=h*.46f
            radius=min(w*.43f,h*.32f)*scale;lastScale=scale;glowColor=color
            glow=RadialGradient(cx,cy,radius*1.1f,intArrayOf(Color.TRANSPARENT,colorWithAlpha(color,70),Color.TRANSPARENT),floatArrayOf(0f,.55f,1f),Shader.TileMode.CLAMP)
            pointer.reset();pointer.moveTo(cx,cy-radius*.74f);pointer.lineTo(cx-radius*.025f,cy-radius*.81f);pointer.lineTo(cx+radius*.025f,cy-radius*.81f);pointer.close()
        }
        canvas.drawColor(Color.BLACK)
        val brightness=v.brightness*(if((battery.percent ?:100)<20).7f else 1f)
        // Faint static grid remains behind normal launcher icons.
        line.color=colorWithAlpha(color,(9*brightness).toInt());line.strokeWidth=1f
        val grid=max(32f,w/12f)
        var x=0f;while(x<w){canvas.drawLine(x,0f,x,h.toFloat(),line);x+=grid}
        var y=0f;while(y<h){canvas.drawLine(0f,y,w.toFloat(),y,line);y+=grid}
        if(v.ambientGlow>0 && active>.2f){fill.shader=glow;fill.alpha=(v.ambientGlow*brightness*active*255).toInt();canvas.drawCircle(cx,cy,radius*1.1f,fill);fill.shader=null;fill.alpha=255}
        val charging=(battery.charging || battery.full && battery.source!="Battery") && v.chargingEffects
        val detail=when(v.detail){"minimal"->1;"cinematic"->3;else->2}
        val ringAlpha=(brightness*(45+active*150)*activation.rings).toInt()
        line.color=colorWithAlpha(color,ringAlpha);line.strokeWidth=max(1f,w/600f)
        ReactorGeometry.ticks.forEachIndexed { i,p ->
            if(detail==1 && i%3!=0)return@forEachIndexed
            val outer=radius;val inner=outer-radius*(if(i%5==0).035f else .014f)
            canvas.drawLine(cx+p.x*inner,cy+p.y*inner,cx+p.x*outer,cy+p.y*outer,line)
        }
        ReactorGeometry.ringScales.forEach { factor ->
            line.color=colorWithAlpha(color,(brightness*(30+active*100)*activation.rings).toInt());canvas.drawCircle(cx,cy,radius*factor,line)
        }
        val speed=ReactorGeometry.speed(s,mode)
        val time=if(motion)now else 0
        ReactorGeometry.arcScales.forEachIndexed { index,factor ->
            val r=radius*factor
            ring.set(cx-r,cy-r,cx+r,cy+r)
            line.strokeWidth=radius*(if(index==0).018f else .008f)
            val angle=ReactorGeometry.angle(time,speed,index)
            repeat(if(detail==1)2 else 3){segment ->
                line.color=colorWithAlpha(if(segment==1)Color.rgb(170,124,255) else color,(ringAlpha*(if(dormant).65f else 1f)).toInt())
                canvas.drawArc(ring,angle+segment*120+12,(if(index==0)40f else 64f)*activation.rings,false,line)
            }
        }
        val core=radius*.48f*(.98f+activation.core*.02f)
        fill.color=colorWithAlpha(color,(brightness*(8+active*17+energy*15)*activation.core).toInt());canvas.drawCircle(cx,cy,core,fill)
        line.color=colorWithAlpha(color,(brightness*(90+active*150)*activation.core).toInt());line.strokeWidth=max(1.2f,radius*.007f)
        canvas.drawCircle(cx,cy,core,line)
        if(energy>0){line.strokeWidth=radius*(.018f+energy*.03f);line.color=colorWithAlpha(color,(energy*100).toInt());canvas.drawCircle(cx,cy,core*(1+energy*.07f),line)}
        if(!dormant && detail>1){
            line.color=colorWithAlpha(color,(brightness*20).toInt());line.strokeWidth=1f
            for(i in -3..3){val yy=i*core/4;val dx=sqrt(max(0f,core*core-yy*yy));canvas.drawLine(cx-dx,cy+yy,cx+dx,cy+yy,line)}
        }
        if(detail>1 && v.particleDensity>0){
            val count=(v.particleDensity*(if(dormant){if(v.cinematicWallpaper)24 else 12} else 28)).toInt()
            repeat(count){i->
                val a=(i*2*PI/max(1,count)+ReactorGeometry.angle(time,speed,0)*PI/180)
                val rr=radius*(.56f+(i%4)*.09f)*(if(charging && motion)1f-((now%2400)/2400f)*.08f else 1f)
                fill.color=colorWithAlpha(color,(brightness*(if(dormant)35 else 130)*activation.rings).toInt())
                canvas.drawCircle(cx+cos(a).toFloat()*rr,cy+sin(a).toFloat()*rr,max(1f,radius*.004f),fill)
            }
        }
        battery.percent?.let{percent->
            val tone=com.jarvis.assistant.titan.BatteryPolicy.band(percent).argb.toInt()
            val pulse=if(motion && percent<=10)(.65+.2*sin(now/1400.0)).toFloat() else .8f
            line.color=colorWithAlpha(tone,(pulse*255).toInt());line.strokeWidth=radius*.012f
            val rr=radius*.55f;ring.set(cx-rr,cy-rr,cx+rr,cy+rr)
            canvas.drawArc(ring,-90f,percent*3.6f,false,line)
        }
        if(charging){
            line.color=colorWithAlpha(ReactorPalette.green,(brightness*210).toInt());line.strokeWidth=radius*.014f
            ring.set(cx-radius*1.025f,cy-radius*1.025f,cx+radius*1.025f,cy+radius*1.025f)
            canvas.drawArc(ring,ReactorGeometry.angle(time,v.animationSpeed*1.3f,0),36f,false,line)
        }
        if(activation.pulse>0){line.color=colorWithAlpha(color,(activation.pulse*110*brightness).toInt());line.strokeWidth=radius*.015f;canvas.drawCircle(cx,cy,radius*(.5f+activation.pulse*.55f),line)}
        fill.color=colorWithAlpha(color,ringAlpha);canvas.drawPath(pointer,fill)
        val font=radius*.24f
        text.textLocale=Locale.forLanguageTag(frame.language.tag)
        text.color=com.jarvis.assistant.titan.BatteryPolicy.band(battery.percent).argb.toInt();text.textSize=font
        if(v.wallpaperBattery)drawText(canvas,battery.percent?.let{"$it%"} ?: "—",cx,cy+font*.3f,text,frame.language)
        text.textSize=radius*.045f
        val state=when(mode){
            SentinelMode.DORMANT->frame.language.choose("SENTINEL · STANDBY","SENTINEL · EN ESPERA","سنتینل · آماده‌باش")
            SentinelMode.ACTIVATING->frame.language.choose("J.A.R.V.I.S. · AWAKENING","J.A.R.V.I.S. · ACTIVANDO","جارویس · فعال‌سازی")
            SentinelMode.LISTENING->if(frame.followUpArming)frame.language.choose("MIC REARMING","PREPARANDO MICRÓFONO","آماده‌سازی میکروفن") else frame.language.choose("LISTENING · ADAPTIVE","ESCUCHANDO · ADAPTATIVO","در حال شنیدن · فرصت ۱۰ ثانیه")
            SentinelMode.PROCESSING->frame.language.choose("PROCESSING","PROCESANDO","در حال پردازش")
            SentinelMode.SPEAKING->if(frame.playback.active)"SPEAKING · LIVE PCM PLAYBACK" else frame.language.choose("SPEAKING · DECORATIVE MOTION","HABLANDO · MOVIMIENTO DECORATIVO","در حال گفتن · حرکت تزئینی")
            SentinelMode.AUTHORIZATION_REQUIRED->"CODEX · IDENTITY REQUIRED"
            SentinelMode.ERROR->frame.language.choose("ATTENTION · OPEN JARVIS","ATENCIÓN · ABRE JARVIS","توجه · جارویس را باز کنید")
            SentinelMode.RETURNING_TO_WALLPAPER->frame.language.choose("RETURNING TO STANDBY","VOLVIENDO A ESPERA","بازگشت به آماده‌باش")
        }
        drawText(canvas,state,cx,cy-core*.44f,text,frame.language)
        text.textSize=radius*.048f;text.color=colorWithAlpha(color,(brightness*220).toInt())
        drawText(canvas,if(battery.full)"FULL" else if(battery.charging)"CHARGING · ${battery.source}" else "BATTERY",cx,cy+core*.55f,text,frame.language)
        if(frame.audio.recording && mode in listOf(SentinelMode.LISTENING,SentinelMode.ACTIVATING) || frame.playback.active){
            val values=if(frame.playback.active)frame.playback.history else frame.audio.history;val step=core*1.25f/32
            line.color=colorWithAlpha(Color.rgb(170,124,255),200);line.strokeWidth=max(1f,radius*.007f)
            values.forEachIndexed{i,value->val xx=cx-core*.625f+i*step;val amplitude=value*core*.14f;canvas.drawLine(xx,cy+core*.75f-amplitude,xx,cy+core*.75f+amplitude,line)}
        }
        drawCaption(canvas,frame,now)
        if(v.wallpaperClock){
            val t=frame.hud.telemetry
            val key="${t.epochMillis/60000}|${t.timezone}|${s.clockFormat}|${t.system24Hour}|${frame.language.code}"
            if(key!=clockKey){clockKey=key;val locale=Locale.forLanguageTag(frame.language.tag);clock=ClockDisplay.time(t.epochMillis,t.timezone,locale,s.clockFormat,t.system24Hour);date=ClockDisplay.date(t.epochMillis,t.timezone,locale)}
            val short=min(w,h).toFloat();val landscape=w>h
            val clockX=if(landscape)w*.17f else if(v.wallpaperWeather)w*.26f else cx
            val clockY=if(landscape)h*.30f else cy-radius*1.32f
            val dateY=if(landscape)clockY+short*.055f else cy-radius*1.16f
            text.color=colorWithAlpha(Color.rgb(221,248,255),(brightness*200).toInt());text.textSize=short*.058f
            val clockWidth=if(!landscape && v.wallpaperWeather).43f else if(landscape).28f else .92f
            drawText(canvas,clock,clockX,clockY,text,frame.language,clockWidth);text.textSize=short*.023f;drawText(canvas,date,clockX,dateY,text,frame.language,clockWidth)
        }
        if(v.wallpaperWeather){
            val weather=frame.hud.weather;val r=weather.reading
            val label=if(r==null)frame.language.choose("Weather unavailable · configure Jarvis","Clima no disponible · configura Jarvis","هوا در دسترس نیست · تنظیم در جارویس") else {
                val temp=r.temperatureC?.let{if(s.fahrenheit)(it*9/5+32).roundToInt() else it.roundToInt()}
                "${r.place} · ${temp?.let{"$it°${if(s.fahrenheit)"F" else "C"}"} ?: "—"}${if(weather.stale)" · STALE" else ""}"
            }
            val short=min(w,h).toFloat();val landscape=w>h
            val weatherX=if(landscape)w*.83f else if(v.wallpaperClock)w*.75f else cx;val weatherY=if(landscape)h*.30f else cy-radius*1.32f
            val weatherWidth=if(landscape).28f else if(v.wallpaperClock).43f else .92f
            text.textSize=short*.023f;text.color=colorWithAlpha(color,(brightness*180).toInt());drawText(canvas,label,weatherX,weatherY,text,frame.language,weatherWidth)
            text.textSize=short*.019f;drawText(canvas,"Open-Meteo.com · ${r?.observationTime?.replace('T',' ') ?: "no reading"}",weatherX,weatherY+short*.07f,text,frame.language,weatherWidth)
        }
    }
    private fun drawCaption(canvas:Canvas,frame:WallpaperFrame,now:Long){
        val caption=frame.caption
        if(!caption.visible(now)){captionLayout=null;captionKey="";return}
        val value=(if(caption.user.isNotBlank())"${if(caption.provisional)"Heard (provisional)" else "You"} · ${caption.user.takeLast(240)}" else "")+(if(caption.assistant.isNotBlank())"\nJarvis · ${caption.assistant.takeLast(600)}" else "")
        val width=(w*.88f).toInt().coerceAtLeast(1)
        val size=(min(w,h)*.031f).coerceAtLeast(16f)
        val key="$value|${caption.entity}|$width|$size|${frame.captionLines}"
        if(captionKey!=key){
            captionKey=key
            val spans=android.text.SpannableString(value)
            caption.entity?.takeIf{it.isNotBlank()}?.let{entity->val index=value.indexOf(entity,ignoreCase=true);if(index>=0)spans.setSpan(android.text.style.ForegroundColorSpan(Color.rgb(34,197,94)),index,index+entity.length,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)}
            val paint=android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE;textSize=size;typeface=Typeface.create("sans-serif",Typeface.NORMAL)}
            captionLayout=android.text.StaticLayout.Builder.obtain(spans,0,spans.length,paint,width).setAlignment(android.text.Layout.Alignment.ALIGN_CENTER).setIncludePad(false).setMaxLines(frame.captionLines.coerceIn(2,4)).setEllipsize(android.text.TextUtils.TruncateAt.END).build()
        }
        val layout=captionLayout ?: return
        val top=(cy+radius*1.12f).coerceAtMost(h-layout.height-min(w,h)*.045f).coerceAtLeast(cy+radius*.6f)
        val alpha=caption.alpha(now,frame.hud.settings.reducedMotion || !frame.hud.telemetry.systemMotion)
        fill.color=Color.argb((210*alpha).toInt(),0,0,0);canvas.drawRoundRect(w*.06f-10,top-8,w*.94f+10,top+layout.height+8,12f,12f,fill)
        layout.paint.alpha=(255*alpha).toInt()
        canvas.save();canvas.translate((w-width)/2f,top);layout.draw(canvas);canvas.restore()
    }
    private fun drawText(canvas:Canvas,value:String,x:Float,baseline:Float,paint:Paint,language:AppLanguage,widthFraction:Float=if(w>h).28f else .92f){
        // StaticLayout handles native shaping and bidirectional runs; cache bounded labels.
        val width=(w*widthFraction).toInt().coerceAtLeast(1)
        val key="$value|${paint.textSize}|$width|${language.code}"
        val layout=labelCache.getOrPut(key){
            val copy=android.text.TextPaint(paint).apply{textAlign=Paint.Align.LEFT}
            android.text.StaticLayout.Builder.obtain(value,0,value.length,copy,width)
                .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER).setIncludePad(false).setMaxLines(2)
                .setEllipsize(android.text.TextUtils.TruncateAt.END)
                .setTextDirection(if(language==AppLanguage.FA)android.text.TextDirectionHeuristics.FIRSTSTRONG_RTL else android.text.TextDirectionHeuristics.FIRSTSTRONG_LTR).build()
        }
        layout.paint.color=paint.color
        canvas.save();canvas.translate(x-width/2f,baseline-layout.getLineBaseline(0));layout.draw(canvas);canvas.restore()
    }
    private fun colorWithAlpha(color:Int,alpha:Int)=(color and 0x00ffffff) or (alpha.coerceIn(0,255) shl 24)
}
