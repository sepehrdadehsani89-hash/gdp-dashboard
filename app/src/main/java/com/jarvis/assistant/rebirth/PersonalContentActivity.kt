package com.jarvis.assistant.rebirth

import android.content.*
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.documentfile.provider.DocumentFile
import com.jarvis.assistant.core.*
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import kotlinx.coroutines.*

data class SelectedContent(val uri:Uri,val name:String,val mime:String)
class PersonalContentActivity:PrivateModuleActivity(){
    private val folders by lazy{ConnectedFolders(this)}
    private val reader by lazy{AttachmentReader(this)}
    private var selected by mutableStateOf(emptyList<SelectedContent>())
    private var listing by mutableStateOf(emptyList<SelectedContent>())
    private var directory:DocumentFile?=null
    private var search by mutableStateOf("")
    private var excerpt by mutableStateOf("")
    private var cloudConsent by mutableStateOf(false)
    private val documents=registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris->loadSelection(uris)}
    private val photos=registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(8)){uris->loadSelection(uris)}
    private val tree=registerForActivityResult(ActivityResultContracts.OpenDocumentTree()){uri->if(uri!=null)task{withContext(Dispatchers.IO){folders.connect(uri)};browseRoot()}}
    private fun loadSelection(uris:List<Uri>){if(uris.isEmpty() || locked())return;task{
        selected=withContext(Dispatchers.IO){uris.take(8).map{SelectedContent(it,reader.displayName(it),contentResolver.getType(it).orEmpty())}}
        excerpt="";cloudConsent=false;status="Selected items are temporary. Nothing was uploaded or copied into chat history."
    }}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(isFinishing)return
        screen("JARVIS PERSONAL CONTENT"){
            Button(enabled=!working,onClick={documents.launch(arrayOf("text/*","application/pdf","application/json","image/*","video/*"))}){Text("Choose documents / media with Android")}
            Button(enabled=!working,onClick={photos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))}){Text("Open Android Photo Picker")}
            OutlinedButton(enabled=!working,onClick={tree.launch(null)}){Text("Connect read-only folder")}
            OutlinedButton(enabled=!working,onClick={task{browseRoot()}}){Text("Browse authorized folder")}
            OutlinedTextField(search,{search=it.take(100)},label={Text("Search authorized folder names")})
            OutlinedButton(enabled=!working && search.isNotBlank(),onClick={task{
                listing=withContext(Dispatchers.IO){
                    val queue=ArrayDeque<DocumentFile>();queue.add(folders.openRoot());val seen=mutableSetOf<String>();val found=mutableListOf<SelectedContent>();var visited=0
                    while(queue.isNotEmpty() && visited++<8 && found.size<40){
                        currentCoroutineContext().ensureActive();val d=queue.removeFirst();if(!seen.add(d.uri.toString()))continue
                        for(item in folders.children(d)){
                            currentCoroutineContext().ensureActive();val name=item.name.orEmpty()
                            if(name.contains(search,true) && found.size<40)found+=SelectedContent(item.uri,name,item.type.orEmpty())
                            if(item.isDirectory && queue.size<8)queue.add(item)
                        }
                    };found
                };status="Search inspected at most eight directories, 100 children each; up to 40 matching names. It is not a full-device search."
            }}){Text("Search selected tree")}
            TextButton(onClick={folders.disconnect();listing=emptyList();directory=null;status="Folder grant disconnected. No file was deleted."}){Text("Disconnect folder access")}
            listing.forEach{item->OutlinedButton(enabled=!working,onClick={task{
                val file=DocumentFile.fromSingleUri(this@PersonalContentActivity,item.uri)
                if(withContext(Dispatchers.IO){file?.isDirectory==true}){directory=file;listing=children(checkNotNull(file))}
                else{selected=listOf(item);excerpt="";cloudConsent=false}
            }}){Text(item.name)}}
            Text("Current user-selected items · ${selected.size}/8")
            selected.forEach{item->
                Text("${item.name} · ${item.mime.ifBlank{"type unavailable"}}")
                OutlinedButton(enabled=!working,onClick={open(item)}){Text("Open / preview with Android")}
                OutlinedButton(enabled=!working,onClick={task{
                    excerpt=reader.read(item.uri,runtime.store.settings.value.language).take(6000)
                    status="Local extraction only. Photos use printed-text OCR; this is not object/scene identification. Videos and unsupported types need a supported viewer."
                }}){Text("Read text / photo OCR locally")}
                OutlinedButton(enabled=!working,onClick={share(item)}){Text("Share using Android chooser")}
                OutlinedButton(enabled=!working,onClick={runtime.addAttachments(listOf(item.uri),cloudConsent);runtime.submit("Summarize the selected document");finish()}){Text("Summarize selected content")}
                OutlinedButton(enabled=!working,onClick={runtime.addAttachments(listOf(item.uri),cloudConsent);runtime.submit(if(runtime.store.settings.value.language==AppLanguage.ES)"Translate the selected document into English" else "Translate the selected document into Spanish");finish()}){Text("Translate selected content")}
                TextButton(onClick={runCatching{contentResolver.releasePersistableUriPermission(item.uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)};selected=selected.filterNot{it.uri==item.uri};excerpt=""}){Text("Forget selection / release saved URI grant")}
            }
            Switch(cloudConsent,{cloudConsent=it})
            Text("Allow extracted text from this selection to my configured cloud AI for the next question. Off uses the optional local model. No photo bitmap or video is uploaded by these actions. Fish private speech has separate consent in Output voice.")
            if(excerpt.isNotBlank())Text(excerpt)
            TextButton(onClick={selected=emptyList();excerpt="";listing=emptyList();cloudConsent=false}){Text("Clear temporary selections and text")}
        }
        if(intent.getBooleanExtra("pick_photos",false) && savedInstanceState==null)photos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }
    private suspend fun children(d:DocumentFile)=withContext(Dispatchers.IO){folders.children(d).map{SelectedContent(it.uri,it.name.orEmpty(),it.type.orEmpty())}}
    private suspend fun browseRoot(){directory=withContext(Dispatchers.IO){folders.openRoot()};listing=children(checkNotNull(directory));status="Showing at most 100 children of the user-authorized directory."}
    private fun open(item:SelectedContent){runCatching{startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(item.uri,item.mime.ifBlank{"*/*"}).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))}.onFailure{status="No supported viewer or URI access was revoked. Select the item again."}}
    private fun share(item:SelectedContent){runCatching{val intent=Intent(Intent.ACTION_SEND).setType(item.mime.ifBlank{"*/*"}).putExtra(Intent.EXTRA_STREAM,item.uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);intent.clipData=ClipData.newUri(contentResolver,"Selected item",item.uri);startActivity(Intent.createChooser(intent,"Share selected item"))}.onFailure{status="Sharing unavailable or access revoked. Nothing was sent automatically."}}
    override fun onDestroy(){selected=emptyList();listing=emptyList();excerpt="";super.onDestroy()}
}
