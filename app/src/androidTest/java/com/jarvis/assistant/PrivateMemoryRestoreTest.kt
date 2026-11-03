package com.jarvis.assistant

import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.assistant.neural.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class PrivateMemoryRestoreTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun document(id:String="import:fixture")=RestoredDocument(id,"Example reference","Example joules energy reference.","local","1","2026","Private")
    private fun backup(docs:List<RestoredDocument> = listOf(document()))=PrivateMemoryBackup(docs,listOf("Fixture task"),emptyList(),emptyMap())
    private fun database(block:(KnowledgeIndex)->Unit){
        val name="restore-fixture-${UUID.randomUUID()}.db";val index=KnowledgeIndex(context,name)
        try{block(index)}finally{index.close();context.deleteDatabase(name)}
    }
    @Test fun knowledgeRestoreIsAdditiveRetrySafeAndPreservesAllText(){database{index->
        index.index("Existing","Existing document content",id="keep")
        val body="x".repeat(150_000).chunked(700).joinToString("\n",postfix="\n")
        val source=backup(listOf(document().copy(text=body)))
        assertEquals(1 to 1,index.restoreBackup(source));assertEquals(0 to 0,index.restoreBackup(source))
        assertEquals(2,index.list().size);assertTrue(index.list().any{it.id=="keep"})
        assertEquals("USER_DOCUMENT",index.list().first{it.id=="import:fixture"}.kind)
        val actual=index.readableDatabase.rawQuery("SELECT body FROM passages WHERE doc=? ORDER BY rowid",arrayOf("import:fixture")).use{c->buildString{while(c.moveToNext())append(c.getString(0))}}
        assertEquals(body,actual);assertEquals(1,index.tasks().size)
    }}
    @Test fun capacityFailureRollsBackKnowledgeAndTasks(){database{index->
        index.index("Existing","Existing reference",id="keep")
        val source=backup((0 until 200).map{document("import:$it")})
        assertTrue(runCatching{index.restoreBackup(source)}.isFailure)
        assertEquals(listOf("keep"),index.list().map{it.id});assertTrue(index.tasks().isEmpty())
    }}
    @Test fun aliasRestoreSkipsMissingAppsAndNeverOverwritesExistingAlias(){
        val registry=AppRegistry(context);val pkg=registry.apps().first{it.packageId==context.packageName}.packageId
        val name="restore fixture ${UUID.randomUUID().toString().take(8)}"
        try{
            assertEquals(1 to 1,registry.restoreAliases(mapOf(name to pkg,"missing fixture" to "org.example.nonexistent.fixture")))
            assertEquals(0 to 1,registry.restoreAliases(mapOf(name to "org.example.nonexistent.fixture")))
            assertEquals(pkg,registry.aliases()[name])
        } finally {registry.removeAlias(name)}
    }
    @Test fun memoryRestoreRetainsTimestampAndDoesNotDuplicateOnRetry()=runBlocking{
        val store=(context.applicationContext as JarvisApplication).runtime.store
        val text="Restore fixture ${UUID.randomUUID()}";val entries=listOf(RestoredMemory(text,123456L))
        try{
            assertEquals(1,store.restoreMemories(entries));assertEquals(0,store.restoreMemories(entries))
            val items=withTimeout(20_000){store.memories.first{list->list.any{it.text==text}}}.filter{it.text==text}
            assertEquals(1,items.size);assertEquals(123456L,items.single().createdAt)
        } finally {store.memories.value.filter{it.text==text}.forEach{store.forget(it.id)}}
    }
}
