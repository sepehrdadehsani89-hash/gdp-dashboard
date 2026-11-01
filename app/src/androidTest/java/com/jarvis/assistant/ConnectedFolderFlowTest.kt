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
        instrumentation.context.grantUriPermission(target.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        val folders = ConnectedFolders(target)
        try {
            folders.connect(uri)
            val restored = ConnectedFolders(target)
            assertEquals(uri, restored.root)
            assertTrue(target.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission && !it.isWritePermission })
            val entries = restored.children(restored.openRoot())
            assertEquals(listOf("nested", "note.txt"), entries.map { it.name })
            val nested = restored.children(entries.first())
            assertEquals("other.txt", nested.single().name)
            val content = target.contentResolver.openInputStream(nested.single().uri)!!.bufferedReader().use { it.readText() }
            assertEquals("Folder fixture other.txt", content)
            restored.disconnect()
            assertNull(ConnectedFolders(target).root)
            assertFalse(target.contentResolver.persistedUriPermissions.any { it.uri == uri })
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
