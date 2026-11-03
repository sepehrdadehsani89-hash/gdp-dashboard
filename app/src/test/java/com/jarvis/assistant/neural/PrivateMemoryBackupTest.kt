package com.jarvis.assistant.neural

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class PrivateMemoryBackupTest {
    private fun document(id:String="one")=JSONObject().put("id",id).put("title","Private reference").put("kind","OFFLINE_KNOWLEDGE").put("source","local").put("version","1").put("publication","2026").put("license","Private").put("text","Example knowledge")
    private fun backup()=JSONObject().put("format","jarvis-neural-export-v1").put("documents",JSONArray().put(document())).put("tasks",JSONArray().put("Example task")).put("approved_memories",JSONArray().put(JSONObject().put("text","Example fact").put("created_at",123L))).put("app_aliases",JSONObject().put("my music","org.example.music"))
    private fun rejects(value:JSONObject){assertThrows(IllegalArgumentException::class.java){PrivateMemoryBackup.parse(value.toString())}}
    @Test fun readsAllExportedCollectionsWithoutRestoringCredentials(){
        val source=backup().put("api_key","must not restore")
        val result=PrivateMemoryBackup.read(ByteArrayInputStream(source.toString().toByteArray()))
        assertEquals(1,result.documents.size);assertEquals(123L,result.memories.single().createdAt)
        assertEquals(listOf("Example task"),result.tasks);assertEquals("org.example.music",result.aliases["my music"])
        assertTrue(result.documents.single().id.startsWith("import:"))
    }
    @Test fun sameExportHasStableIdsButChangedBodyCannotReplacePreviousImport(){
        val source=backup();val first=PrivateMemoryBackup.parse(source.toString())
        assertEquals(first,PrivateMemoryBackup.parse(source.toString()))
        source.getJSONArray("documents").getJSONObject(0).put("text","Different contents")
        assertNotEquals(first.documents.single().id,PrivateMemoryBackup.parse(source.toString()).documents.single().id)
    }
    @Test fun rejectsUnsupportedAndMistypedFormats(){rejects(backup().put("format","other"));rejects(backup().put("tasks","not an array"))}
    @Test fun rejectsDuplicateDocumentIdsBeforeAnyStorage(){rejects(backup().put("documents",JSONArray().put(document()).put(document())))}
    @Test fun rejectsOversizedOrEmptyDocumentText(){
        rejects(backup().put("documents",JSONArray().put(document().put("text","x".repeat(150_221)))))
        rejects(backup().put("documents",JSONArray().put(document().put("text"," "))))
    }
    @Test fun rejectsInvalidKindsAndTimestampTypes(){
        rejects(backup().put("documents",JSONArray().put(document().put("kind","TRUSTED_SYSTEM"))))
        rejects(backup().put("approved_memories",JSONArray().put(JSONObject().put("text","Example").put("created_at",1.5))))
    }
    @Test fun rejectsInvalidPackageAndNormalizedAliasCollision(){
        rejects(backup().put("app_aliases",JSONObject().put("my music","../../bad")))
        rejects(backup().put("app_aliases",JSONObject().put("My Music","org.example.one").put("my music","org.example.two")))
    }
    @Test fun deduplicatesTasksAndMemories(){
        val source=backup();source.getJSONArray("tasks").put("Example task");source.getJSONArray("approved_memories").put(source.getJSONArray("approved_memories").getJSONObject(0))
        val result=PrivateMemoryBackup.parse(source.toString());assertEquals(1,result.tasks.size);assertEquals(1,result.memories.size)
    }
    @Test fun readsLegacyExportWithPassageNewlinesWithoutTruncation(){
        val text="x".repeat(150_000).chunked(700).joinToString("\n",postfix="\n")
        val result=PrivateMemoryBackup.parse(backup().put("documents",JSONArray().put(document().put("text",text))).toString())
        assertEquals(text,result.documents.single().text)
    }
    @Test fun rejectsStreamOverLimit(){
        val stream=object:java.io.InputStream(){var remaining=PrivateMemoryBackup.MAX_BYTES+1;override fun read():Int=if(remaining-->0)120 else -1;override fun read(b:ByteArray,off:Int,len:Int):Int {if(remaining<=0)return -1;val n=minOf(len,remaining);b.fill(120.toByte(),off,off+n);remaining-=n;return n}}
        assertThrows(IllegalArgumentException::class.java){PrivateMemoryBackup.read(stream)}
    }
    @Test fun validatesUserExportPrivatelyWhenProvided(){
        val path=System.getenv("JARVIS_PRIVATE_EXPORT_TEST")
        org.junit.Assume.assumeNotNull(path)
        val value=java.io.File(path!!).inputStream().use{PrivateMemoryBackup.read(it)}
        assertEquals(11,value.documents.size);assertEquals(1,value.aliases.size)
        assertEquals(0,value.memories.size);assertEquals(0,value.tasks.size)
    }
}
