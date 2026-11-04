package com.jarvis.assistant.titan

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import com.jarvis.assistant.core.MemoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.*

class MemoryCardsActivity:PrivateModuleActivity(){
    private var query by mutableStateOf("")
    private var editId by mutableStateOf<Long?>(null)
    private var editText by mutableStateOf("")
    private var selectedCategory by mutableStateOf("Notes")
    private var revision by mutableIntStateOf(0)
    private var exportPayload:ByteArray?=null
    private val pins by lazy{getSharedPreferences("jarvis_titan_memory_cards_v1",MODE_PRIVATE)}
    private val export=registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        val payload=exportPayload;exportPayload=null
        if(uri==null || payload==null){payload?.fill(0);return@registerForActivityResult}
        task{try{withContext(Dispatchers.IO){contentResolver.openOutputStream(uri,"wt")!!.use{it.write(payload)}};status=words("Selected private memory-card export saved","Exportación privada de tarjetas guardada","Saved")}finally{payload.fill(0)}}
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);if(isFinishing)return
        screen(words("JARVIS · INTELLIGENT MEMORY CARDS","JARVIS · TARJETAS DE MEMORIA","Memory")){
            val memories by runtime.store.memories.collectAsStateWithLifecycle()
            val state by runtime.state.collectAsStateWithLifecycle()
            @Suppress("UNUSED_VARIABLE")val observed=revision
            Text(words("Only user-approved memories. Editing updates the original record; no duplicate database. Search is lexical relevance, not a newly verified semantic embedding engine.","Solo recuerdos aprobados. Editar actualiza el registro original, sin base duplicada. La búsqueda es léxica; no es un motor nuevo de embeddings verificado.","Approved memories only"))
            OutlinedTextField(query,{query=it.take(200)},label={Text(words("Search approved memories","Buscar recuerdos aprobados","Search"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(editText,{editText=it.take(4000)},label={Text(if(editId==null)words("New approved memory","Recuerdo nuevo aprobado","New") else words("Edit original memory","Editar recuerdo original","Edit"))},modifier=Modifier.fillMaxWidth())
            MemoryCardPolicy.categories.forEach{category->FilterChip(selectedCategory==category,{selectedCategory=category},label={Text(category)})}
            Button(enabled=editText.isNotBlank() && !working,onClick={val id=editId;val text=editText;val category=selectedCategory;authorize{task{
                val saved=withContext(Dispatchers.IO){if(id==null)runtime.store.remember(text) else {runtime.store.editMemory(id,text);id}}
                runtime.invalidateMemoryContext();runtime.setCategory(saved,category);editId=null;editText="";status=words("Approved memory saved","Recuerdo aprobado guardado","Saved")
            }}}){Text(words("Approve and save card","Aprobar y guardar tarjeta","Save"))}
            TextButton(onClick={editId=null;editText=""}){Text(words("Cancel editor / new card","Cancelar edición / nueva tarjeta","Cancel"))}
            memories.filter{it.text.contains(query,true)}.sortedWith(compareByDescending<MemoryItem>{pins.getBoolean("pin:${it.id}",false)}.thenByDescending{it.createdAt}).forEach{m->
                HorizontalDivider();Text((if(pins.getBoolean("pin:${m.id}",false))"★ " else "")+(state.memoryCategories[m.id] ?: "Notes"),style=MaterialTheme.typography.labelLarge)
                Text(m.text)
                TextButton(onClick={editId=m.id;editText=m.text;selectedCategory=state.memoryCategories[m.id] ?: "Notes"}){Text(words("Edit","Editar","Edit"))}
                TextButton(onClick={pins.edit().putBoolean("pin:${m.id}",!pins.getBoolean("pin:${m.id}",false)).apply();revision++}){Text(words("Pin / unpin","Fijar / desfijar","Pin"))}
                TextButton(onClick={authorize{runtime.deleteMemory(m.id);pins.edit().remove("pin:${m.id}").apply();revision++}}){Text(words("Delete approved memory","Eliminar recuerdo aprobado","Delete"))}
            }
            OutlinedButton(enabled=exportPayload==null,onClick={authorize{
                val items=runtime.store.memories.value
                exportPayload=JSONObject().put("format","jarvis-approved-memory-cards-v1").put("memories",JSONArray().apply{items.forEach{m->put(JSONObject().put("id",m.id).put("text",m.text).put("created_at",m.createdAt).put("category",runtime.state.value.memoryCategories[m.id] ?: "Notes").put("pinned",pins.getBoolean("pin:${m.id}",false)))}}).toString(2).toByteArray()
                export.launch("Jarvis-approved-memory-cards.json")
            }}){Text(words("Export approved cards privately · choose destination","Exportar tarjetas en privado · elegir destino","Export"))}
        }
    }
    override fun onDestroy(){exportPayload?.fill(0);exportPayload=null;super.onDestroy()}
}
object MemoryCardPolicy {val categories=listOf("Notes","Personal","Work","Tasks","Preferences","Projects","Favorite apps","Destinations","Websites")}
