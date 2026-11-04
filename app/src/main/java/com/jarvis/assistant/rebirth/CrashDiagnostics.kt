package com.jarvis.assistant.rebirth

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import org.json.*

/** Operational metadata only: no exception messages, raw stacks, file paths, email or transcript. */
class CrashDiagnostics(context:Context){
    private val app=context.applicationContext
    private val prefs=app.getSharedPreferences("jarvis_rebirth_crash_metadata",Context.MODE_PRIVATE)
    fun install(){
        val previous=Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler{thread,error->
            runCatching{prefs.edit().putString("java_type",error.javaClass.name.take(160)).putLong("java_at",System.currentTimeMillis()).commit()}
            if(previous!=null)previous.uncaughtException(thread,error) else {android.os.Process.killProcess(android.os.Process.myPid());kotlin.system.exitProcess(10)}
        }
    }
    fun snapshot():String {
        val manager=app.getSystemService(ActivityManager::class.java)
        val memory=ActivityManager.MemoryInfo();manager.getMemoryInfo(memory)
        val exits=if(Build.VERSION.SDK_INT>=30)runCatching{manager.getHistoricalProcessExitReasons(app.packageName,0,6).joinToString("\n"){"OS process exit · reason ${it.reason}, status ${it.status}, time ${it.timestamp}, PSS ${it.pss} KiB, RSS ${it.rss} KiB"}}.getOrDefault("OS process-exit metadata unavailable.") else "OS process-exit metadata requires Android 11+."
        return "Actual Android memory now: available ${memory.availMem/1048576} MiB / total ${memory.totalMem/1048576} MiB; lowMemory=${memory.lowMemory}.\nLast Java failure type: ${prefs.getString("java_type","none observed")}; time ${prefs.getLong("java_at",0)}.\n$exits\nNo raw exception message, stack contents or conversation was collected. These observations do not establish a crash root cause."
    }
    fun clear(){prefs.edit().clear().apply()}
}
