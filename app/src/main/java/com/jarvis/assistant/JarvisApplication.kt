package com.jarvis.assistant

import android.app.Application
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class JarvisApplication : Application() {
    val runtime: AssistantRuntime by lazy { AssistantRuntime(this) }
    override fun onCreate() { super.onCreate(); runtime.initialize() }
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) runtime.trimMemory()
    }
}

class PanicTileService : TileService() {
    override fun onStartListening() {
        qsTile?.apply { label = "Jarvis • Stop"; state = Tile.STATE_ACTIVE; updateTile() }
    }
    override fun onClick() {
        (application as JarvisApplication).runtime.stopEverything()
        qsTile?.apply { state = Tile.STATE_INACTIVE; updateTile() }
    }
}
