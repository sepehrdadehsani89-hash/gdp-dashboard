package com.jarvis.assistant.studio

import android.graphics.*
import com.jarvis.assistant.sentinel.WallpaperFrame
import com.jarvis.assistant.sentinel.WallpaperRenderer

/** Optional user-owned composition; the inactive path calls the original renderer unchanged. */
class StudioWallpaperRenderer {
    private val original=WallpaperRenderer()
    private val fill=Paint(Paint.ANTI_ALIAS_FLAG)
    private val alphaPaint=Paint()
    private val masks=WallpaperPart.entries.associateWith{setOf(it)}
    private var lastProfile:StudioProfile?=null
    private var ordered=emptyList<StudioLayer>()
    private val text=Paint(Paint.ANTI_ALIAS_FLAG).apply{typeface=Typeface.create("sans-serif-light",Typeface.NORMAL)}
    fun pause()=original.pause()
    fun clearCaption()=original.clearCaption()
    fun draw(canvas:Canvas,frame:WallpaperFrame,now:Long,profile:StudioProfile?,widgets:Map<WidgetSource,String>,privateAllowed:Boolean){
        if(profile==null){original.draw(canvas,frame,now);return}
        val allowPrivate=privateAllowed && profile.privateWidgets
        val width=canvas.width.toFloat();val height=canvas.height.toFloat()
        canvas.drawColor(Color.BLACK)
        if(lastProfile!==profile){lastProfile=profile;ordered=profile.layers.sortedBy{it.order}}
        for(layer in ordered){
            if(!layer.visible || layer.opacity<=0f)continue
            alphaPaint.alpha=(layer.opacity*255).toInt()
            val offscreen=layer.opacity<1f || layer.kind.startsWith("PART:") && layer.color!=null
            val checkpoint=if(offscreen)canvas.saveLayer(0f,0f,width,height,alphaPaint)else canvas.save()
            try{
                val part=layer.kind.removePrefix("PART:").takeIf{layer.kind.startsWith("PART:")}?.let(WallpaperPart::valueOf)
                val widget=layer.kind.removePrefix("WIDGET:").takeIf{layer.kind.startsWith("WIDGET:")}?.let(WidgetSource::valueOf)
                if(part!=null){
                    if(part==WallpaperPart.CAPTION && !allowPrivate)continue
                    val pivotY=if(part in setOf(WallpaperPart.GRID,WallpaperPart.POWER,WallpaperPart.CAPTION))height*.5f else height*.46f
                    canvas.translate((layer.x-.5f)*width,(layer.y-.5f)*height);canvas.rotate(layer.rotation,width*.5f,pivotY);canvas.scale(layer.scale,layer.scale,width*.5f,pivotY)
                    val selected=if(part==WallpaperPart.GLOW)frame.copy(hud=frame.hud.copy(settings=frame.hud.settings.copy(sentinel=frame.hud.settings.sentinel.copy(ambientGlow=layer.glow))))else frame
                    original.draw(canvas,selected,now,requireNotNull(masks[part]),clearBackground=false,forcedColor=if(layer.color!=null)Color.WHITE else null,fontScale=layer.fontScale)
                    if(layer.color!=null){setFill(layer,width,height);fill.blendMode=BlendMode.SRC_IN;canvas.drawRect(0f,0f,width,height,fill);fill.blendMode=null;fill.shader=null}
                }else if(layer.kind=="BACKGROUND"){
                    canvas.translate((layer.x-.5f)*width,(layer.y-.5f)*height)
                    canvas.rotate(layer.rotation,width*.5f,height*.5f);canvas.scale(layer.scale,layer.scale,width*.5f,height*.5f)
                    setFill(layer,width,height);canvas.drawRect(0f,0f,width,height,fill);fill.shader=null
                }else if(widget!=null){
                    val sensitive=widget in setOf(WidgetSource.GMAIL_UNREAD,WidgetSource.NOTIFICATIONS,WidgetSource.NEXT_REMINDER,WidgetSource.CALENDAR,WidgetSource.TASKS)
                    val value=if(sensitive && !allowPrivate)"Private data hidden" else widgets[widget] ?: "$widget · unavailable"
                    val x=layer.x*width;val y=layer.y*height
                    canvas.rotate(layer.rotation,x,y);canvas.scale(layer.scale,layer.scale,x,y)
                    text.textSize=minOf(width,height)*.035f*layer.fontScale;text.color=layer.color ?: Color.rgb(221,248,255)
                    text.textAlign=when(layer.alignment){"left"->Paint.Align.LEFT;"right"->Paint.Align.RIGHT;else->Paint.Align.CENTER}
                    text.shader=layer.gradientEnd?.let{LinearGradient(0f,0f,width,0f,layer.color ?: Color.WHITE,it,Shader.TileMode.CLAMP)}
                    if(layer.glow>0)text.setShadowLayer(layer.glow*8,0f,0f,text.color)else text.clearShadowLayer()
                    val rows=value.lines().take(4)
                    for((i,row)in rows.withIndex()){
                        val clean=row.take(180);val allowed=width*.85f
                        val line=if(text.measureText(clean)<=allowed)clean else clean.take((clean.length*allowed/text.measureText(clean)).toInt().coerceAtLeast(0))+"…"
                        canvas.drawText(line,x,y+i*text.textSize*1.2f,text)
                    }
                    text.shader=null;text.clearShadowLayer()
                }
            }finally{canvas.restoreToCount(checkpoint)}
        }
    }
    private fun setFill(layer:StudioLayer,width:Float,height:Float){fill.color=layer.color ?: Color.BLACK;fill.shader=layer.gradientEnd?.let{LinearGradient(0f,0f,width,height,layer.color ?: Color.BLACK,it,Shader.TileMode.CLAMP)}}
}
