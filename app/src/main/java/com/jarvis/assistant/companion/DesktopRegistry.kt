package com.jarvis.assistant.companion

import com.jarvis.assistant.neural.AppMatching
import com.jarvis.assistant.omnisystem.FeatureVault
import org.json.JSONArray
import org.json.JSONObject

data class DesktopConnection(val label:String,val pair:DesktopPair,val aliases:Set<String> = emptySet()){override fun toString()="DesktopConnection(device=${pair.deviceId}, label=redacted, credentials=redacted)"}
data class DesktopConnections(val devices:List<DesktopConnection>,val activeId:String?)
/** Up to20 individually approved targets. An empty new registry never falls back to an old grant. */
class DesktopRegistry internal constructor(
    private val read:()->String?,private val exists:()->Boolean,private val write:(String)->Unit,
    private val readLegacy:()->String?,private val legacyExists:()->Boolean,private val deleteLegacy:()->Unit={},
    private val legacyLabel:(DesktopPair)->String?={null}
){
    private val changed=kotlinx.coroutines.flow.MutableStateFlow(0L)
    val changes:kotlinx.coroutines.flow.StateFlow<Long> = changed
    constructor(vault:FeatureVault):this({vault.read("desktop_connections")},{vault.contains("desktop_connections")},{vault.write("desktop_connections",it)},
        {vault.read("desktop_companion")},{vault.contains("desktop_companion")},{vault.remove("desktop_companion")},
        {pair->kotlin.runCatching{JSONObject(vault.read("mission_device_name_v_one") ?: "{}").takeIf{it.optString("device_id")==pair.deviceId}?.optString("name")?.takeIf{it.isNotBlank()}}.getOrNull()})
    private fun label(value:String)=value.trim().also{require(it.length in 1..60 && it.none(Char::isISOControl) && AppMatching.normalize(it).isNotBlank()){"Use a unique computer name of1–60 characters."}}
    private fun save(state:DesktopConnections){require(state.devices.size<=20);require(state.activeId==null || state.devices.any{it.pair.deviceId==state.activeId})
        write(JSONObject().put("version",1).put("active",state.activeId.orEmpty()).put("devices",JSONArray().apply{state.devices.forEach{row->put(JSONObject().put("label",row.label).put("pair",JSONObject(row.pair.json())).put("aliases",JSONArray(row.aliases.toList())))}}).toString());changed.value=changed.value+1}
    @Synchronized fun snapshot():DesktopConnections {
        val raw=read();if(raw==null){check(!exists()){"PRIVATE_PAIRING_UNAVAILABLE"};val legacy=readLegacy();check(legacy!=null || !legacyExists()){"PRIVATE_PAIRING_UNAVAILABLE"}
            if(legacy==null)return DesktopConnections(emptyList(),null)
            val pair=DesktopPair.parse(legacy);return DesktopConnections(listOf(DesktopConnection(label(legacyLabel(pair) ?: "Computer 1"),pair)),pair.deviceId).also(::save)
        }
        val root=JSONObject(raw);require(root.getInt("version")==1);val rows=root.getJSONArray("devices");require(rows.length()<=20)
        val devices=(0 until rows.length()).map{val row=rows.getJSONObject(it);val aliases=row.getJSONArray("aliases");require(aliases.length()<=10)
            DesktopConnection(label(row.getString("label")),DesktopPair.parse(row.getJSONObject("pair").toString()),(0 until aliases.length()).map{index->label(aliases.getString(index))}.toSet())}
        require(devices.map{it.pair.deviceId}.distinct().size==devices.size)
        val owners=mutableMapOf<String,String>()
        devices.forEach{device->(device.aliases+device.label).forEach{name->val key=AppMatching.normalize(name);require(owners[key]==null || owners[key]==device.pair.deviceId);owners[key]=device.pair.deviceId}}
        val active=root.optString("active").takeIf{it.isNotEmpty()};require(active==null || devices.any{it.pair.deviceId==active})
        return DesktopConnections(devices,active)
    }
    @Synchronized fun active():DesktopConnection? {val state=snapshot();return state.devices.firstOrNull{it.pair.deviceId==state.activeId}}
    @Synchronized fun add(pair:DesktopPair,chosenLabel:String?=null):DesktopConnection {
        DesktopPair.parse(pair.json());val state=snapshot();val old=state.devices.firstOrNull{it.pair.deviceId==pair.deviceId};check(old!=null || state.devices.size<20){"Twenty computers are already saved. Remove a target before pairing another."}
        val name=chosenLabel?.let(::label) ?: old?.label ?: generateSequence(1){it+1}.map{"Computer $it"}.first{candidate->state.devices.none{row->(row.aliases+row.label).any{AppMatching.normalize(it)==AppMatching.normalize(candidate)}}}
        require(state.devices.none{it.pair.deviceId!=pair.deviceId && (it.aliases+it.label).any{alias->AppMatching.normalize(alias)==AppMatching.normalize(name)}})
        val row=DesktopConnection(name,pair,old?.aliases.orEmpty());save(DesktopConnections(state.devices.filterNot{it.pair.deviceId==pair.deviceId}+row,pair.deviceId));return row
    }
    @Synchronized fun rename(deviceId:String,name:String,aliases:Set<String> = emptySet()){
        val state=snapshot();require(state.devices.any{it.pair.deviceId==deviceId});val clean=label(name);require(aliases.size<=10);val normalizedAliases=aliases.map(::label).toSet()
        val names=(normalizedAliases+clean).map(AppMatching::normalize).toSet()
        require(state.devices.filterNot{it.pair.deviceId==deviceId}.none{row->(row.aliases+row.label).map(AppMatching::normalize).any{it in names}}){"That computer name or alias is already used. Choose a unique name."}
        save(state.copy(devices=state.devices.map{if(it.pair.deviceId==deviceId)it.copy(label=clean,aliases=normalizedAliases)else it}))
    }
    @Synchronized fun select(query:String):DesktopConnection {
        val state=snapshot();val normalized=AppMatching.normalize(query)
        val matches=state.devices.filter{it.pair.deviceId==query || (it.aliases+it.label).any{label->AppMatching.normalize(label)==normalized}}
        check(matches.size==1){"Select one exact saved computer name or ID. No target was changed."};val row=matches.single();save(state.copy(activeId=row.pair.deviceId));return row
    }
    @Synchronized fun remove(deviceId:String,expectedSessionId:String?=null):Boolean {
        val state=snapshot();val row=state.devices.firstOrNull{it.pair.deviceId==deviceId} ?: return false
        if(expectedSessionId!=null)check(row.pair.sessionId==expectedSessionId){"PAIRED_TARGET_CHANGED_REVIEW_AGAIN"}
        save(state.copy(devices=state.devices.filterNot{it.pair.deviceId==deviceId},activeId=state.activeId.takeUnless{it==deviceId}))
        val old=readLegacy();if(old!=null && runCatching{DesktopPair.parse(old).deviceId==deviceId}.getOrDefault(false))deleteLegacy()
        return true
    }
    companion object {
        fun reviewed(current:DesktopPair,deviceId:String?,sessionId:String?)=deviceId!=null && sessionId!=null && current.deviceId==deviceId && current.sessionId==sessionId
        fun selectionCommand(raw:String):String?=Regex("^(?:jarvis[, ]+)?(?:select|switch to|use) (?:computer|pc|desktop) (.+)$",RegexOption.IGNORE_CASE).matchEntire(raw.trim().trimEnd('.','!'))?.groupValues?.get(1)?.trim()?.takeIf{it.length in 1..60 && it.none(Char::isISOControl)}
    }
}
