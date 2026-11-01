package com.jarvis.assistant;

public class FolderGrantActivity extends android.app.Activity {
    protected void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        android.net.Uri uri = android.provider.DocumentsContract.buildTreeDocumentUri("com.jarvis.next.test.folders", "root");
        grantUriPermission("com.jarvis.next", uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION | android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | android.content.Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        sendBroadcast(new android.content.Intent("com.jarvis.next.test.FOLDER_GRANTED").setPackage("com.jarvis.next"));
        finish();
    }
}
