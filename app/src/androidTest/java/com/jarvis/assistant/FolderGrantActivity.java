package com.jarvis.assistant;

public class FolderGrantActivity extends android.app.Activity {
    protected void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        String target = getIntent().getStringExtra("jarvis_test_target");
        String authority = getIntent().getStringExtra("jarvis_test_authority");
        if (target == null || !getPackageName().equals(target + ".test") || authority == null) { finish(); return; }
        boolean ownsProvider = false;
        try {
            android.content.pm.ProviderInfo[] providers = getPackageManager().getPackageInfo(getPackageName(), android.content.pm.PackageManager.GET_PROVIDERS).providers;
            if (providers != null) for (android.content.pm.ProviderInfo provider : providers) {
                if (provider.name.equals(FolderFixtureProvider.class.getName()) && authority.equals(provider.authority)) ownsProvider = true;
            }
        } catch (android.content.pm.PackageManager.NameNotFoundException ignored) { }
        if (!ownsProvider) { finish(); return; }
        android.net.Uri uri = android.provider.DocumentsContract.buildTreeDocumentUri(authority, "root");
        grantUriPermission(target, uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION | android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | android.content.Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        sendBroadcast(new android.content.Intent(getPackageName() + ".FOLDER_GRANTED").setPackage(target));
        finish();
    }
}
