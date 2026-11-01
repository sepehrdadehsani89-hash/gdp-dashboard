package com.jarvis.assistant

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsProvider
import java.io.File

/** Fixture ships only in the test APK, never the phone APK. */
class FolderFixtureProvider : DocumentsProvider() {
    override fun onCreate() = true
    override fun queryRoots(projection: Array<out String>?): Cursor = MatrixCursor(arrayOf("root_id"))
    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor = rows(listOf(documentId))
    override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?): Cursor = rows(when(parentDocumentId) { "root" -> listOf("nested", "note.txt"); "nested" -> listOf("other.txt"); else -> emptyList() })
    private fun rows(ids: List<String>): Cursor = MatrixCursor(arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE, Document.COLUMN_FLAGS, Document.COLUMN_SIZE)).apply {
        ids.forEach { id -> addRow(arrayOf(id, id, if (id in listOf("root", "nested")) Document.MIME_TYPE_DIR else "text/plain", 0, 24)) }
    }
    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        check(mode == "r")
        val file = File(context!!.cacheDir, documentId).apply { writeText("Folder fixture $documentId") }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }
    override fun isChildDocument(parentDocumentId: String, documentId: String) = parentDocumentId == "root" || parentDocumentId == "nested" && documentId == "other.txt"
}
