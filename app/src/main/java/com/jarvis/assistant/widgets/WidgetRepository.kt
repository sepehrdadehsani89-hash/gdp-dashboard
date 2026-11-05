package com.jarvis.assistant.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context

/** Stores approved IDs only. No RemoteViews content, screenshots, conversations or AI sharing. */
class WidgetRepository(context:Context){
    private val app=context.applicationContext
    private val preferences=app.getSharedPreferences("jarvis_approved_widgets_v1",Context.MODE_PRIVATE)
    val manager:AppWidgetManager=AppWidgetManager.getInstance(app)
    fun saved()=preferences.getStringSet("approved",emptySet()).orEmpty().toSet()
    fun nextResultCode():Int {
        val code=preferences.getInt("next_result",32000)
        check(code in 32000..64999){"Widget setup request limit reached."}
        check(preferences.edit().putInt("next_result",code+1).commit());return code
    }
    fun approved(host:AppWidgetHost)=WidgetHostPolicy.restored(saved(),host.appWidgetIds.toSet()){manager.getAppWidgetInfo(it)!=null}
    fun add(id:Int,host:AppWidgetHost){
        check(id in host.appWidgetIds && manager.getAppWidgetInfo(id)!=null)
        val ids=approved(host);require(ids.size<WidgetHostPolicy.MAX_WIDGETS || id in ids)
        check(preferences.edit().putStringSet("approved",(ids+id).map(Int::toString).toSet()).commit())
    }
    fun removeOrphans(host:AppWidgetHost,pending:Int?){
        val retained=saved().mapNotNull(String::toIntOrNull).toSet()
        host.appWidgetIds.filter{it !in retained && it!=pending}.forEach{host.deleteAppWidgetId(it)}
    }
    fun forget(id:Int){check(preferences.edit().putStringSet("approved",saved()-id.toString()).commit())}
    fun clear(){runCatching{AppWidgetHost(app,WidgetHostPolicy.HOST_ID).deleteHost()};preferences.edit().clear().apply()}
}
