package com.jarvis.assistant

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.assistant.core.ConnectedFolders
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectedFolderFlowTest {
    @Test fun persistedReadGrantBrowseReadReconnectAndRevoke() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val target = instrumentation.targetContext
        assertEquals("com.jarvis.next", target.packageName)
        val uri = DocumentsContract.buildTreeDocumentUri("com.jarvis.next.test.folders", "root")
        fun grantFromOwner() {
            val ready = java.util.concurrent.CountDownLatch(1)
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: android.content.Context?, intent: Intent?) { ready.countDown() }
            }
            androidx.core.content.ContextCompat.registerReceiver(target, receiver, android.content.IntentFilter("com.jarvis.next.test.FOLDER_GRANTED"), androidx.core.content.ContextCompat.RECEIVER_EXPORTED)
            try {
                instrumentation.context.startActivity(Intent().setClassName(instrumentation.context.packageName, FolderGrantActivity::class.java.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                assertTrue("Provider owner must issue a fresh grant", ready.await(30, java.util.concurrent.TimeUnit.SECONDS))
            } finally { target.unregisterReceiver(receiver) }
        }
        grantFromOwner()
        val folders = ConnectedFolders(target)
        try {
            folders.connect(uri)
            val restored = ConnectedFolders(target)
            assertEquals(uri, restored.root)
            assertTrue(target.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission && !it.isWritePermission })
            val document = androidx.documentfile.provider.DocumentFile.fromTreeUri(target, uri)!!
            target.contentResolver.query(document.uri, arrayOf(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)!!.use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(android.provider.DocumentsContract.Document.MIME_TYPE_DIR, cursor.getString(0))
            }
            assertTrue("Directory flag for ${document.uri}", document.isDirectory)
            assertTrue("Read permission for ${document.uri}: " + target.checkUriPermission(document.uri, android.os.Process.myPid(), android.os.Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION), document.canRead())
            val entries = restored.children(restored.openRoot())
            assertEquals(listOf("nested", "note.txt"), entries.map { it.name })
            val nested = restored.children(entries.first())
            assertEquals("other.txt", nested.single().name)
            val content = target.contentResolver.openInputStream(nested.single().uri)!!.bufferedReader().use { it.readText() }
            assertEquals("Folder fixture other.txt", content)
            restored.disconnect()
            assertNull(ConnectedFolders(target).root)
            assertFalse(target.contentResolver.persistedUriPermissions.any { it.uri == uri })
            grantFromOwner()
            restored.connect(uri)
            target.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            assertTrue(runCatching { restored.openRoot() }.isFailure)
        } finally { folders.disconnect() }
    }
    @Test fun rejectsNonTreeUris() {
        val folders = ConnectedFolders(InstrumentationRegistry.getInstrumentation().targetContext)
        assertTrue(runCatching { folders.connect(Uri.parse("file:///sdcard")) }.isFailure)
    }
}
