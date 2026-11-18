package com.jarvis.assistant.studio

import com.jarvis.assistant.omnisystem.FeatureVault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.*

enum class WallpaperPart { GRID,GLOW,RINGS,CORE,PARTICLES,BATTERY,WAVEFORM,POWER,CAPTION,CLOCK,WEATHER }
enum class WidgetSource { CLOCK,DATE,CITY,WEATHER,TEMPERATURE,FORECAST,SUNRISE,SUNSET,BATTERY,CHARGING,NETWORK,BLUETOOTH,AI,CONNECTION,GMAIL_UNREAD,NOTIFICATIONS,NEXT_REMINDER,CALENDAR,TASKS,COMPUTER,MUSIC,VOLUME,MICROPHONE,SILENT,JARVIS,STORAGE,TELEMETRY }
data class StudioLayer(val id:String,val kind:String,val x:Float=.5f,val y:Float=.5f,val scale:Float=1f,val rotation:Float=0f,val opacity:Float=1f,val order:Int=0,val visible:Boolean=true,val color:Int?=null,val gradientEnd:Int?=null,val glow:Float=.4f,val fontScale:Float=1f,val alignment:String="center"){
    fun validated():StudioLayer {
        require(id.matches(Regex("[A-Za-z0-9_-]{1,60}")))
        require(kind=="BACKGROUND" || kind.startsWith("PART:") && runCatching{WallpaperPart.valueOf(kind.removePrefix("PART:"))}.isSuccess || kind.startsWith("WIDGET:") && runCatching{WidgetSource.valueOf(kind.removePrefix("WIDGET:"))}.isSuccess)
        require(listOf(x,y,scale,rotation,opacity,glow,fontScale).all(Float::isFinite))
        require(x in 0f..1f && y in 0f..1f && scale in .25f..2f && rotation in -180f..180f && opacity in 0f..1f && glow in 0f..1f && fontScale in .5f..2f && order in 0..100 && alignment in setOf("left","center","right"))
        require(gradientEnd==null || color!=null)
        return this
    }
}
data class StudioProfile(val id:String,val name:String,val layers:List<StudioLayer>,val privateWidgets:Boolean=false,val snap:Boolean=true,val quality:String="balanced"){
    fun validated():StudioProfile {
        require(runCatching{UUID.fromString(id).toString()==id}.getOrDefault(false))
        require(name.isNotBlank() && name.length<=60 && name.none(Char::isISOControl))
        require(layers.size in 1..24 && layers.map{it.id}.distinct().size==layers.size && layers.count{it.kind=="BACKGROUND"}<=1)
        require(quality in setOf("saver","balanced","high"));layers.forEach{it.validated()};return this
    }
}
data class StudioLibrary(val profiles:List<StudioProfile> = emptyList(),val active:String?=null){fun selected()=profiles.firstOrNull{it.id==active}}
object StudioPolicy {
    val themes=listOf("Holographic Command Center","Arc Reactor","Deep Space","Cyberpunk Neon","Quantum Violet","Minimal Intelligence","Obsidian","Custom Laboratory")
    fun moved(layer:StudioLayer,x:Float,y:Float,snap:Boolean):StudioLayer {
        require(x.isFinite() && y.isFinite())
        fun value(v:Float):Float = if(snap)round(v.coerceIn(0f,1f)*20)/20 else v.coerceIn(0f,1f)
        return layer.copy(x=value(x),y=value(y)).validated()
    }
    fun original(name:String="Custom Laboratory")=StudioProfile(UUID.randomUUID().toString(),name,buildList{
        add(StudioLayer("background","BACKGROUND",y=.5f,order=0,color=0xff000000.toInt()))
        listOf(WallpaperPart.GRID,WallpaperPart.GLOW,WallpaperPart.RINGS,WallpaperPart.CORE,WallpaperPart.PARTICLES,WallpaperPart.BATTERY,WallpaperPart.WAVEFORM,WallpaperPart.POWER,WallpaperPart.CAPTION).forEachIndexed{i,p->add(StudioLayer(p.name,"PART:${p.name}",order=i+1))}
        add(StudioLayer("clock","WIDGET:CLOCK",x=.25f,y=.83f,order=20));add(StudioLayer("date","WIDGET:DATE",x=.25f,y=.89f,order=21));add(StudioLayer("weather","WIDGET:WEATHER",x=.75f,y=.84f,order=22))
    })
    fun theme(name:String):StudioProfile {
        require(name in themes)
        val (color,bg)=when(name){"Arc Reactor"->0xff3bf0ff.toInt() to 0xff00111a.toInt();"Deep Space"->0xff9ea8ff.toInt() to 0xff030414.toInt();"Cyberpunk Neon"->0xffff37dd.toInt() to 0xff16021c.toInt();"Quantum Violet"->0xffb388ff.toInt() to 0xff0c031a.toInt();"Minimal Intelligence"->0xffe3f2fd.toInt() to 0xff030609.toInt();"Obsidian"->0xff8ca2b5.toInt() to 0xff000000.toInt();else->0xff00e5ff.toInt() to 0xff000811.toInt()}
        val p=original(name);return p.copy(layers=p.layers.map{if(it.kind=="BACKGROUND")it.copy(color=bg,gradientEnd=if(name=="Deep Space")0xff160a32.toInt()else null)else it.copy(color=color,visible=if(name=="Minimal Intelligence")it.kind !in setOf("PART:GRID","PART:PARTICLES")else it.visible)})
    }
    fun visiblePrivate(optedIn:Boolean,unlocked:Boolean,silent:Boolean)=optedIn && unlocked && !silent
}
object StudioColor {
    fun hex(raw:String):Int {val s=raw.trim().removePrefix("#");require(s.matches(Regex("[A-Fa-f0-9]{6}|[A-Fa-f0-9]{8}")));return (if(s.length==6)"FF$s" else s).toLong(16).toInt()}
    fun hex(value:Int)="#%08X".format(java.util.Locale.ROOT,value)
    fun rgb(r:Int,g:Int,b:Int,alpha:Float=1f):Int{require(r in 0..255 && g in 0..255 && b in 0..255 && alpha.isFinite() && alpha in 0f..1f);return ((alpha*255).roundToInt() shl 24)or(r shl 16)or(g shl 8)or b}
    fun hsv(h:Float,s:Float,v:Float,alpha:Float=1f):Int{require(h.isFinite()&&s.isFinite()&&v.isFinite()&&h in 0f..360f&&s in 0f..1f&&v in 0f..1f);val c=v*s;val x=c*(1-abs((h/60)%2-1));val m=v-c;val p=when((h%360/60).toInt()){0->floatArrayOf(c,x,0f);1->floatArrayOf(x,c,0f);2->floatArrayOf(0f,c,x);3->floatArrayOf(0f,x,c);4->floatArrayOf(x,0f,c);else->floatArrayOf(c,0f,x)};return rgb(((p[0]+m)*255).roundToInt().coerceIn(0,255),((p[1]+m)*255).roundToInt().coerceIn(0,255),((p[2]+m)*255).roundToInt().coerceIn(0,255),alpha)}
    fun hsl(h:Float,s:Float,l:Float,alpha:Float=1f):Int{require(l.isFinite()&&l in 0f..1f && s.isFinite()&&s in 0f..1f);val v=l+s*min(l,1-l);return hsv(h,if(v==0f)0f else (2*(1-l/v)).coerceIn(0f,1f),v,alpha)}
}
class WallpaperStudioStore internal constructor(private val read:()->String?,private val exists:()->Boolean,private val write:(String)->Unit){
    constructor(vault:FeatureVault):this({vault.read("wallpaper_studio_profiles")},{vault.contains("wallpaper_studio_profiles")},{vault.write("wallpaper_studio_profiles",it)})
    private val initial=runCatching{load()}
    val storageError=if(initial.isFailure)"Private wallpaper profiles are unavailable; existing data was preserved. Original wallpaper remains active." else null
    private val mutable=MutableStateFlow(initial.getOrDefault(StudioLibrary()));val state:StateFlow<StudioLibrary> = mutable
    private fun encodeLayer(l:StudioLayer)=JSONObject().put("id",l.id).put("kind",l.kind).put("x",l.x).put("y",l.y).put("scale",l.scale).put("rotation",l.rotation).put("opacity",l.opacity).put("order",l.order).put("visible",l.visible).put("color",l.color).put("gradient",l.gradientEnd).put("glow",l.glow).put("font",l.fontScale).put("align",l.alignment)
    private fun layer(j:JSONObject)=StudioLayer(j.getString("id"),j.getString("kind"),j.getDouble("x").toFloat(),j.getDouble("y").toFloat(),j.getDouble("scale").toFloat(),j.getDouble("rotation").toFloat(),j.getDouble("opacity").toFloat(),j.getInt("order"),j.getBoolean("visible"),j.opt("color").takeIf{it is Number}?.let{(it as Number).toInt()},j.opt("gradient").takeIf{it is Number}?.let{(it as Number).toInt()},j.getDouble("glow").toFloat(),j.getDouble("font").toFloat(),j.getString("align")).validated()
    fun load():StudioLibrary{
        val raw=read();if(raw==null){check(!exists()){"Private wallpaper profiles are unavailable; existing data was preserved."};return StudioLibrary()}
        val j=JSONObject(raw);require(j.getInt("version")==1);val ps=j.getJSONArray("profiles");require(ps.length()<=8)
        val profiles=(0 until ps.length()).map{i->val p=ps.getJSONObject(i);val ls=p.getJSONArray("layers");StudioProfile(p.getString("id"),p.getString("name"),(0 until ls.length()).map{layer(ls.getJSONObject(it))},p.getBoolean("private"),p.getBoolean("snap"),p.getString("quality")).validated()}
        require(profiles.map{it.id}.distinct().size==profiles.size);val active=j.optString("active").takeIf{it.isNotBlank()};require(active==null||profiles.any{it.id==active});return StudioLibrary(profiles,active)
    }
    @Synchronized private fun save(library:StudioLibrary){require(library.profiles.size<=8);val encoded=JSONObject().put("version",1).put("active",library.active.orEmpty()).put("profiles",JSONArray().apply{library.profiles.forEach{p->put(JSONObject().put("id",p.id).put("name",p.name).put("private",p.privateWidgets).put("snap",p.snap).put("quality",p.quality).put("layers",JSONArray().apply{p.layers.forEach{put(encodeLayer(it))}}))}});write(encoded.toString());mutable.value=library}
    @Synchronized fun apply(profile:StudioProfile){profile.validated();val current=load();check(current.profiles.size<8||current.profiles.any{it.id==profile.id}){"Eight profiles are already saved. Delete one deliberately before adding another."};save(StudioLibrary(current.profiles.filterNot{it.id==profile.id}+profile,profile.id))}
    @Synchronized fun select(id:String?){val s=load();require(id==null||s.profiles.any{it.id==id});save(s.copy(active=id))}
    @Synchronized fun delete(id:String){val s=load();save(StudioLibrary(s.profiles.filterNot{it.id==id},s.active.takeUnless{it==id}))}
}
