package com.jarvis.assistant.neural

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.ln

data class KnowledgeDocument(val id:String,val title:String,val kind:String,val source:String,val version:String,val publication:String,val license:String)
data class KnowledgeHit(val document:KnowledgeDocument,val text:String,val score:Double)
object RetrievalPolicy {
    fun terms(text:String)=Regex("[\\p{L}\\p{N}]+").findAll(AppMatching.normalize(text)).map{it.value}.filter{it.length>1 && it !in setOf("the","what","this","that","with","from","please","and","que","los","las","una","por","para","این","است","را","برای","بگو")}.distinct().take(24).toList()
    fun chunks(text:String):List<String>{val clean=text.replace('\u0000',' ').take(150_000);return clean.chunked(700).take(220)}
    fun relevantMemories(query:String,memories:List<com.jarvis.assistant.core.MemoryItem>):List<com.jarvis.assistant.core.MemoryItem>{
        val q=terms(query).toSet();if(q.isEmpty())return emptyList()
        return memories.map{memory->memory to Regex("[\\p{L}\\p{N}]+").findAll(AppMatching.normalize(memory.text)).map{it.value}.toSet().count{it in q}}
            .filter{it.second>0}.sortedByDescending{it.second}.take(5).map{it.first}
    }
    fun rank(query:String,candidates:List<Pair<KnowledgeDocument,String>>):List<KnowledgeHit>{
        val q=terms(query);if(q.isEmpty())return emptyList()
        val counts=candidates.map{(_,text)->Regex("[\\p{L}\\p{N}]+").findAll(AppMatching.normalize(text)).map{it.value}.groupingBy{it}.eachCount()}
        val frequencies=q.associateWith{term->counts.count{term in it}}
        return candidates.mapIndexed{index,(d,t)->
            val words=counts[index];val size=words.values.sum();val title=AppMatching.normalize(d.title)
            val score=q.sumOf{term->val frequency=words[term] ?: 0;val df=frequencies.getValue(term);if(frequency==0)0.0 else ln(1+(candidates.size-df+.5)/(df+.5))*frequency/(frequency+.7+size/100.0)}+q.count{title.contains(it)}*.7
            KnowledgeHit(d,t,score)
        }.filter{it.score>0}.sortedByDescending{it.score}.take(5)
    }
    fun reference(hits:List<KnowledgeHit>,maximum:Int=2400)=hits.joinToString("\n\n"){"[${it.document.kind}: ${it.document.title}; ${it.document.source}; published ${it.document.publication}]\n${it.text}"}.take(maximum)
}
/** Separate additive SQLite database: existing Room history/memories are not migrated. */
class KnowledgeIndex(context:Context,name:String="jarvis_neural_v4.db"):SQLiteOpenHelper(context,name,null,1){
    override fun onCreate(db:SQLiteDatabase){
        db.execSQL("CREATE TABLE documents(id TEXT PRIMARY KEY,title TEXT,kind TEXT,source TEXT,version TEXT,publication TEXT,license TEXT)")
        db.execSQL("CREATE VIRTUAL TABLE passages USING fts4(doc TEXT, body TEXT, terms TEXT, tokenize=unicode61)")
        db.execSQL("CREATE TABLE audit(id INTEGER PRIMARY KEY,time INTEGER,tool TEXT,status TEXT)")
        db.execSQL("CREATE TABLE tasks(id INTEGER PRIMARY KEY AUTOINCREMENT,text TEXT,done INTEGER DEFAULT 0)")
    }
    override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int)=Unit
    @Synchronized fun index(title:String,text:String,kind:String="USER_DOCUMENT",source:String="User-selected file",version:String="1",publication:String="user supplied",license:String="Private user content",id:String=UUID.randomUUID().toString()):String{
        require(text.isNotBlank() && text.length<=150_000);require(kind in setOf("USER_DOCUMENT","USER_NOTE","OFFLINE_KNOWLEDGE","CONVERSATION_SUMMARY"))
        val db=writableDatabase;require(list().size<200 || list().any{it.id==id}){"Index limit reached. Remove a document first."}
        db.beginTransaction();try{
            db.delete("passages","doc=?",arrayOf(id));db.delete("documents","id=?",arrayOf(id))
            db.insertOrThrow("documents",null,ContentValues().apply{put("id",id);put("title",title.take(160));put("kind",kind);put("source",source.take(500));put("version",version.take(40));put("publication",publication.take(80));put("license",license.take(100))})
            RetrievalPolicy.chunks(text).forEach{chunk->db.insertOrThrow("passages",null,ContentValues().apply{put("doc",id);put("body",chunk);put("terms",AppMatching.normalize(chunk))})}
            db.setTransactionSuccessful()
        }finally{db.endTransaction()};return id
    }
    @Synchronized fun list():List<KnowledgeDocument> = readableDatabase.rawQuery("SELECT id,title,kind,source,version,publication,license FROM documents ORDER BY title",null).use{c->buildList{while(c.moveToNext())add(KnowledgeDocument(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6)))}}
    @Synchronized fun search(query:String):List<KnowledgeHit>{
        val words=RetrievalPolicy.terms(query);if(words.isEmpty())return emptyList()
        val match=words.joinToString(" OR "){"\"$it\""}
        val candidates=readableDatabase.rawQuery("SELECT d.id,d.title,d.kind,d.source,d.version,d.publication,d.license,passages.body FROM passages JOIN documents d ON d.id=passages.doc WHERE passages MATCH ? LIMIT 80",arrayOf(match)).use{c->buildList{while(c.moveToNext())add(KnowledgeDocument(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6)) to c.getString(7))}}
        return RetrievalPolicy.rank(query,candidates)
    }
    @Synchronized fun installPack(pack:KnowledgePack,entries:List<Pair<String,String>>){
        val db=writableDatabase;db.beginTransaction();try{
            list().filter{it.id.startsWith("pack:${pack.id}:")}.forEach{remove(it.id)}
            entries.forEachIndexed{i,(title,text)->index(title,text,"OFFLINE_KNOWLEDGE","https://github.com/sepehrdadehsani89-hash/gdp-dashboard/blob/jarvis-next/downloads/neural-packs/${pack.file}",pack.version,pack.publication,"CC0-1.0","pack:${pack.id}:$i")}
            db.setTransactionSuccessful()
        }finally{db.endTransaction()}
    }
    @Synchronized fun remove(id:String){writableDatabase.beginTransaction();try{writableDatabase.delete("passages","doc=?",arrayOf(id));writableDatabase.delete("documents","id=?",arrayOf(id));writableDatabase.setTransactionSuccessful()}finally{writableDatabase.endTransaction()}}
    @Synchronized fun audit(tool:String,status:String){require(tool.matches(Regex("[a-z_]{1,40}")));writableDatabase.insert("audit",null,ContentValues().apply{put("time",System.currentTimeMillis());put("tool",tool);put("status",status.take(40))});writableDatabase.execSQL("DELETE FROM audit WHERE id NOT IN (SELECT id FROM audit ORDER BY id DESC LIMIT 100)")}
    @Synchronized fun auditLines():List<String> = readableDatabase.rawQuery("SELECT time,tool,status FROM audit ORDER BY id DESC LIMIT 30",null).use{c->buildList{while(c.moveToNext())add("${java.time.Instant.ofEpochMilli(c.getLong(0))} · ${c.getString(1)} · ${c.getString(2)}")}}
    @Synchronized fun addTask(text:String){require(text.isNotBlank());writableDatabase.insertOrThrow("tasks",null,ContentValues().apply{put("text",text.trim().take(300));put("done",0)})}
    @Synchronized fun tasks():List<Pair<Long,String>> = readableDatabase.rawQuery("SELECT id,text FROM tasks WHERE done=0 ORDER BY id DESC LIMIT 30",null).use{c->buildList{while(c.moveToNext())add(c.getLong(0) to c.getString(1))}}
    @Synchronized fun completeTask(id:Long){writableDatabase.delete("tasks","id=?",arrayOf(id.toString()))}
    @Synchronized fun export():String=JSONObject().put("format","jarvis-neural-export-v1").put("documents",JSONArray().apply{list().forEach{d->put(JSONObject().put("id",d.id).put("title",d.title).put("kind",d.kind).put("source",d.source).put("license",d.license).put("version",d.version).put("publication",d.publication).put("text",readableDatabase.rawQuery("SELECT body FROM passages WHERE doc=? ORDER BY rowid",arrayOf(d.id)).use{c->buildString{while(c.moveToNext()){append(c.getString(0));append('\n')}}}))}}).put("tasks",JSONArray().apply{tasks().forEach{put(it.second)}}).toString(2)
    /** Additive restore: imported data never replaces a document or grants pack provenance. */
    @Synchronized fun restoreBackup(backup:PrivateMemoryBackup):Pair<Int,Int>{
        val db=writableDatabase
        db.beginTransaction()
        try {
            val existing=list().map{it.id}.toSet()
            val pending=backup.documents.filter{it.id !in existing}
            require(existing.size+pending.size<=200){"Not enough document slots. Nothing was imported into the knowledge index."}
            pending.forEach{d->
                db.insertOrThrow("documents",null,ContentValues().apply{put("id",d.id);put("title",d.title);put("kind","USER_DOCUMENT");put("source",d.source);put("version",d.version);put("publication",d.publication);put("license",d.license)})
                d.text.chunked(700).forEach{chunk->db.insertOrThrow("passages",null,ContentValues().apply{put("doc",d.id);put("body",chunk);put("terms",AppMatching.normalize(chunk))})}
            }
            val knownTasks=db.rawQuery("SELECT text FROM tasks",null).use{c->buildSet{while(c.moveToNext())add(c.getString(0))}}
            val pendingTasks=backup.tasks.filter{it !in knownTasks}
            pendingTasks.forEach{addTask(it)}
            db.setTransactionSuccessful()
            return pending.size to pendingTasks.size
        } finally { db.endTransaction() }
    }
    @Synchronized fun clear(){writableDatabase.execSQL("DELETE FROM passages");writableDatabase.execSQL("DELETE FROM documents");writableDatabase.execSQL("DELETE FROM tasks");writableDatabase.execSQL("DELETE FROM audit")}
}
