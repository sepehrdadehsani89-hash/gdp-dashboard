package com.jarvis.assistant.core

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/** Read-only SAF grants, independent of the old Jarvis installation. No filesystem paths. */
class ConnectedFolders(private val context: Context) {
    private val prefs = context.getSharedPreferences("connected-folders", Context.MODE_PRIVATE)
    val root: Uri? get() = prefs.getString("root", null)?.let(Uri::parse)
    private fun unlocked() = check(context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked != true) { "Unlock the phone to access connected folders." }
    fun connect(uri: Uri) {
        unlocked()
        check(uri.scheme == "content" && android.provider.DocumentsContract.isTreeUri(uri))
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val old = root
        prefs.edit().putString("root", uri.toString()).commit()
        if (old != null && old != uri) runCatching { context.contentResolver.releasePersistableUriPermission(old, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    fun disconnect() {
        val old = root
        prefs.edit().remove("root").commit()
        old?.let { runCatching { context.contentResolver.releasePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
    }
    fun openRoot(): DocumentFile {
        unlocked()
        val uri = root ?: error("Connect a folder first.")
        check(context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }) { "Folder access was revoked. Connect it again." }
        return DocumentFile.fromTreeUri(context, uri)?.takeIf { it.isDirectory && it.canRead() } ?: error("Folder unavailable. Connect it again.")
    }
    fun children(directory: DocumentFile): List<DocumentFile> {
        unlocked()
        openRoot() // recheck persisted grant on every navigation
        check(directory.isDirectory && directory.canRead()) { "Folder unavailable." }
        return directory.listFiles().take(100).sortedWith(compareBy({ !it.isDirectory }, { it.name.orEmpty().lowercase() }))
    }
}
