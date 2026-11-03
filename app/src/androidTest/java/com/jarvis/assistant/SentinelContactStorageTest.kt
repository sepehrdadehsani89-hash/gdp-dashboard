package com.jarvis.assistant

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.ContactsContract
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.assistant.contacts.*
import com.jarvis.assistant.omnisystem.FeatureVault
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

/** Writes and removes only its own synthetic Android-provider contact in a disposable emulator. */
class SentinelContactStorageTest {
    @Test fun encryptedShortcutRevalidatesActualProviderChangesBeforeDialing(){
        Assume.assumeTrue(Build.HARDWARE in setOf("ranchu","goldfish"))
        val instrumentation=InstrumentationRegistry.getInstrumentation();val context=ApplicationProvider.getApplicationContext<Context>()
        val alias="sentinel_contact_test_"+UUID.randomUUID();val vault=FeatureVault(context,alias);val repo=ContactShortcuts(context,vault)
        val resolver=context.contentResolver;var raw:android.net.Uri?=null
        instrumentation.uiAutomation.adoptShellPermissionIdentity(android.Manifest.permission.READ_CONTACTS,android.Manifest.permission.WRITE_CONTACTS)
        try{
            raw=resolver.insert(ContactsContract.RawContacts.CONTENT_URI,ContentValues().apply{putNull(ContactsContract.RawContacts.ACCOUNT_TYPE);putNull(ContactsContract.RawContacts.ACCOUNT_NAME)})!!
            val data=resolver.insert(ContactsContract.Data.CONTENT_URI,ContentValues().apply{
                put(ContactsContract.Data.RAW_CONTACT_ID,ContentUris.parseId(raw));put(ContactsContract.Data.MIMETYPE,ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                put(ContactsContract.CommonDataKinds.Phone.NUMBER,"+15550123456");put(ContactsContract.CommonDataKinds.Phone.TYPE,ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
            })!!
            repo.save(null,"Synthetic Fixture","+15550123456",listOf("Fixture nickname"),data.toString())
            val saved=repo.all().single();assertEquals(saved,repo.resolve("Fixture nickname"));assertEquals(saved,repo.validateForDial(saved.id,saved.number))
            val encrypted=context.getSharedPreferences(alias,0).getString("contact_shortcuts",null)!!
            assertFalse(encrypted.contains("15550123456"));assertFalse(encrypted.contains("Synthetic Fixture"))
            resolver.update(data,ContentValues().apply{put(ContactsContract.CommonDataKinds.Phone.NUMBER,"+15550987654")},null,null)
            assertThrows(IllegalStateException::class.java){repo.validateForDial(saved.id,saved.number)}
            repo.remove(saved.id);assertTrue(repo.all().isEmpty())
        }finally{raw?.let{resolver.delete(it,null,null)};instrumentation.uiAutomation.dropShellPermissionIdentity();vault.clear();context.deleteSharedPreferences(alias)}
    }
}
