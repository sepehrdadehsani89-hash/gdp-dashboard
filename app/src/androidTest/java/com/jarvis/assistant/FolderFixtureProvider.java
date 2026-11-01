package com.jarvis.assistant;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/** Pure Java: separate fixture process must not depend on target-only Kotlin runtime. */
public class FolderFixtureProvider extends DocumentsProvider {
    public boolean onCreate() { return true; }
    public Cursor queryRoots(String[] projection) { return new MatrixCursor(new String[]{"root_id"}); }
    public Cursor queryDocument(String id, String[] projection) { return rows(new String[]{id}, projection); }
    public Cursor queryChildDocuments(String id, String[] projection, String order) {
        return rows(id.equals("root") ? new String[]{"nested", "note.txt"} : id.equals("nested") ? new String[]{"other.txt"} : new String[]{}, projection);
    }
    private Cursor rows(String[] ids, String[] projection) {
        String[] columns = projection != null ? projection : new String[]{Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE, Document.COLUMN_FLAGS, Document.COLUMN_SIZE};
        MatrixCursor result = new MatrixCursor(columns);
        for (String id : ids) {
            Object[] row = new Object[columns.length];
            for (int index = 0; index < columns.length; index++) {
                switch (columns[index]) {
                    case Document.COLUMN_DOCUMENT_ID: case Document.COLUMN_DISPLAY_NAME: row[index] = id; break;
                    case Document.COLUMN_MIME_TYPE: row[index] = id.equals("root") || id.equals("nested") ? Document.MIME_TYPE_DIR : "text/plain"; break;
                    case Document.COLUMN_FLAGS: row[index] = 0; break;
                    case Document.COLUMN_SIZE: row[index] = 24; break;
                    default: row[index] = null;
                }
            }
            result.addRow(row);
        }
        return result;
    }
    public ParcelFileDescriptor openDocument(String id, String mode, CancellationSignal signal) throws java.io.FileNotFoundException {
        if (!mode.equals("r") || !(id.equals("note.txt") || id.equals("other.txt"))) throw new java.io.FileNotFoundException();
        File file = new File(getContext().getCacheDir(), id);
        try (FileOutputStream out = new FileOutputStream(file)) { out.write(("Folder fixture " + id).getBytes(StandardCharsets.UTF_8)); }
        catch (java.io.IOException e) { throw new java.io.FileNotFoundException(e.getMessage()); }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }
    public boolean isChildDocument(String parent, String child) { return parent.equals("root") && (child.equals("nested") || child.equals("note.txt") || child.equals("other.txt")) || parent.equals("nested") && child.equals("other.txt"); }
}
