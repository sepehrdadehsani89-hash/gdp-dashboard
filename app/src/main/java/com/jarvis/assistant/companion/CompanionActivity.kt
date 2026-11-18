package com.jarvis.assistant.companion

import android.Manifest
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Explicit two-device approval; secrets stay in the retained, RAM-only coordinator. */
class CompanionActivity:PrivateModuleActivity(){
    private var revision by mutableIntStateOf(0)
    private var diagnostics by mutableStateOf(false)
    private val nearby by lazy{NearbyCompanions(this){runtime.companion.registry.snapshot().devices.map{it.pair}}}
    private val model by lazy{ViewModelProvider(this,object:ViewModelProvider.Factory{
        override fun <T:ViewModel> create(modelClass:Class<T>):T {
            val owner=Any()
            val value=PairingViewModel(runtime.companion){runtime.trackModuleOperation(owner,it)}
            if(runCatching{runtime.companion.paired()!=null}.getOrDefault(false))value.coordinator.selected()
            @Suppress("UNCHECKED_CAST") return value as T
        }
    })[PairingViewModel::class.java]}
    private val session get()=model.coordinator
    private val scanner=registerForActivityResult(ScanContract()){result->
        val current=runtime.captureIsCurrent(session.scannerOwner);model.handoff=false;runtime.endExclusiveCapture(session.scannerOwner)
        if(!current || locked()){session.cancel();return@registerForActivityResult}
        val scannerError=result.originalIntent?.getStringExtra("jarvis_scan_error")
        if(result.contents!=null){session.setInvitation(result.contents,true);session.scannerInfo(QrScannerEngine.CAMERAX_MLKIT,QrScannerOutcome.RESULT_RECEIVED)}
        else if(scannerError!=null){session.scanFailure(scannerError);session.scannerInfo(QrScannerEngine.CAMERAX_MLKIT,QrScannerOutcome.FAILED)}
        else if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)session.scanFailure("CAMERA_PERMISSION_DENIED")
        else session.scanCancelled()
    }
    private val cameraPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->val current=runtime.captureIsCurrent(session.scannerOwner);model.handoff=false;runtime.endExclusiveCapture(session.scannerOwner);if(!current || locked())session.cancel()else if(granted){session.scanCancelled();openBundled()}else session.scanFailure("CAMERA_PERMISSION_DENIED")}
    private val gallery=registerForActivityResult(ActivityResultContracts.GetContent()){uri->
        val current=runtime.captureIsCurrent(session.scannerOwner);model.handoff=false;runtime.endExclusiveCapture(session.scannerOwner)
        if(!current || locked()){session.cancel();return@registerForActivityResult}
        if(uri==null){session.scanCancelled();return@registerForActivityResult}
        task{try{val raw=withContext(Dispatchers.IO){decodeImage(uri)};session.setInvitation(raw,true);session.scannerInfo(QrScannerEngine.GALLERY_LOCAL,QrScannerOutcome.RESULT_RECEIVED)}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){session.scanFailure((e as? CompanionFailure)?.code ?: "NO_QR_IN_IMAGE");session.scannerInfo(QrScannerEngine.GALLERY_LOCAL,QrScannerOutcome.FAILED)}}
    }
    private val export=registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")){uri->model.handoff=false;if(uri!=null && !locked())task{withContext(Dispatchers.IO){contentResolver.openOutputStream(uri,"wt")?.use{it.write(session.diagnostics().toByteArray())} ?: error("No output stream")};status="Only redacted diagnostic state was exported; no invitation, token, ticket or comparison code."}}
    private fun decodeImage(uri:Uri):String {
        val bytes=contentResolver.openInputStream(uri)?.use{input->
            val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
            while(true){val n=input.read(buffer);if(n<0)break;if(out.size()+n>16*1024*1024)throw CompanionFailure("QR_IMAGE_TOO_LARGE");out.write(buffer,0,n)};out.toByteArray()
        } ?: throw CompanionFailure("NO_QR_IN_IMAGE")
        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
        if(bounds.outWidth<=0 || bounds.outHeight<=0 || bounds.outWidth.toLong()*bounds.outHeight>100_000_000)throw CompanionFailure("NO_QR_IN_IMAGE")
        var sample=1;while(bounds.outWidth/sample>2048 || bounds.outHeight/sample>2048)sample*=2
        val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply{inSampleSize=sample;inPreferredConfig=android.graphics.Bitmap.Config.ARGB_8888}) ?: throw CompanionFailure("NO_QR_IN_IMAGE")
        return try{val pixels=IntArray(bitmap.width*bitmap.height);bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height);QrImageDecoder.decode(bitmap.width,bitmap.height,pixels)}finally{bitmap.recycle();bytes.fill(0)}
    }
    private fun openGoogle(){
        if(!session.scannerOpening())return
        if(!runtime.beginExclusiveCapture(session.scannerOwner)){session.scanFailure("CAPTURE_BUSY");return}
        val playStatus=GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if(playStatus!=ConnectionResult.SUCCESS){session.scannerInfo(QrScannerEngine.GOOGLE,QrScannerOutcome.FAILED,playStatus);runtime.endExclusiveCapture(session.scannerOwner);session.scanFailure("SCANNER_UNAVAILABLE");status="Google scanner is unavailable. Opening the bundled local camera scanner with normal Android permission.";openBundled();return}
        try{
            model.handoff=true;session.scanning();session.scannerInfo(QrScannerEngine.GOOGLE,QrScannerOutcome.OPENING)
            val options=GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).enableAutoZoom().build()
            GmsBarcodeScanning.getClient(this,options).startScan()
                .addOnSuccessListener{barcode->val current=runtime.captureIsCurrent(session.scannerOwner);model.handoff=false;runtime.endExclusiveCapture(session.scannerOwner);if(current && !locked()){session.setInvitation(barcode.rawValue.orEmpty(),true);session.scannerInfo(QrScannerEngine.GOOGLE,QrScannerOutcome.RESULT_RECEIVED)}else session.cancel()}
                .addOnCanceledListener{session.scannerInfo(QrScannerEngine.GOOGLE,QrScannerOutcome.CANCELLED);model.handoff=false;runtime.endExclusiveCapture(session.scannerOwner);session.scanCancelled()}
                .addOnFailureListener{e->session.scannerInfo(QrScannerEngine.GOOGLE,QrScannerOutcome.FAILED,(e as? ApiException)?.statusCode);val current=runtime.captureIsCurrent(session.scannerOwner);model.handoff=false;runtime.endExclusiveCapture(session.scannerOwner);if(e is ApiException && e.statusCode==CommonStatusCodes.CANCELED)session.scanCancelled()else{session.scanFailure("SCANNER_FAILED");if(current && !locked() && !isFinishing && !isDestroyed){status="Google scanner initialization failed. Using the bundled local camera scanner; no pairing has been assumed.";openBundled()}}}
        }catch(_:Exception){model.handoff=false;runtime.endExclusiveCapture(session.scannerOwner);session.scanFailure("SCANNER_UNAVAILABLE");if(!locked() && !isFinishing && !isDestroyed){status="Google scanner is unavailable. Opening the bundled on-device CameraX / ML Kit scanner.";openBundled()}}
    }
    private fun openBundled(){
        if(!session.scannerOpening())return
        if(!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){session.scanFailure("CAMERA_UNAVAILABLE");return}
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){if(!runtime.beginExclusiveCapture(session.scannerOwner)){session.scanFailure("CAPTURE_BUSY");return};model.handoff=true;try{cameraPermission.launch(Manifest.permission.CAMERA)}catch(_:Exception){model.handoff=false;runtime.endExclusiveCapture(session.scannerOwner);session.scanFailure("CAMERA_PERMISSION_DENIED")};return}
        if(!runtime.beginExclusiveCapture(session.scannerOwner)){session.scanFailure("CAPTURE_BUSY");return}
        try{model.handoff=true;session.scanning();session.scannerInfo(QrScannerEngine.CAMERAX_MLKIT,QrScannerOutcome.OPENING);scanner.launch(ScanOptions().setCaptureActivity(PairingCaptureActivity::class.java).setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt("Scan the private Jarvis invitation · Cancel or Flashlight below").setBeepEnabled(false).setOrientationLocked(false))}
        catch(_:Exception){model.handoff=false;runtime.endExclusiveCapture(session.scannerOwner);session.scanFailure("SCANNER_FAILED")}
    }
    private fun confirmed(action:()->Unit){model.handoff=true;authorize{model.handoff=false;action()}}
    private fun showInstructions(){startActivity(Intent(this,PairingHelpActivity::class.java))}
    override fun onCreate(state:Bundle?){super.onCreate(state);if(isFinishing)return
        screen("JARVIS · PAIRED COMPUTERS"){
            val flow by session.state.collectAsStateWithLifecycle()
            val storageRevision by runtime.companion.registry.changes.collectAsStateWithLifecycle()
            val discovered by nearby.state.collectAsStateWithLifecycle()
            LaunchedEffect(Unit){while(true){kotlinx.coroutines.delay(1000);session.tick()}}
            LaunchedEffect(flow.stage){if(flow.stage in setOf(PairingStage.PAIRED_UNVERIFIED,PairingStage.PAIRED_VERIFIED))revision++}
            val saved=remember(revision,storageRevision){runCatching{runtime.companion.registry.snapshot()}}
            val connections=saved.getOrNull();val selected=connections?.devices?.firstOrNull{it.pair.deviceId==connections.activeId}
            val idle=!working && !flow.busy
            saved.exceptionOrNull()?.let{Text("Private pairing storage is unavailable. Existing data was preserved; no computer is assumed connected.")}
            Text("Saved computers: ${connections?.devices?.size ?: 0} /20. Selected: ${selected?.label ?: "None"}. Saved pairing is separate from a live connection.")
            Text(discovered.message)
            OutlinedButton(enabled=idle && !discovered.running,onClick={nearby.start()}){Text("Discover nearby computers · no automatic trust")}
            discovered.results.forEach{row->
                Text("Nearby ${row.deviceId.takeLast(8)} · ${row.address} · ${if(row.known)"matches a saved device ID; unverified" else "unpaired; scan its QR first"}")
                val known=connections?.devices?.firstOrNull{NearbyCompanionPolicy.canUseSavedEndpoint(row,it.pair)}
                if(known!=null && known.pair.deviceId==selected?.pair?.deviceId)TextButton(enabled=idle,onClick={session.check(known.pair)}){Text("Authenticate saved nearby target")}
                if(row.known && known==null)Text("Address changed or does not match the saved endpoint. Discovery cannot replace its certificate or address; review the original identity before reconfiguration.")
            }
            Text("Pairing state: ${flow.stage}");Text(flow.message)
            if(flow.busy){LinearProgressIndicator();OutlinedButton(onClick={session.cancel();runtime.abandonExclusiveCapture(session.scannerOwner)}){Text("Cancel pairing")}}
            connections?.devices?.forEach{row->
                Text(row.label+" · "+row.pair.deviceId)
                OutlinedButton(enabled=idle,onClick={confirmed{runtime.mission.cancel();runtime.companion.registry.select(row.pair.deviceId);session.selected();revision++;status="Target selected. Existing command reviews remain bound to their original target."}}){Text("Select this computer")}
            }
            if(selected!=null){
                var name by remember(selected.pair.deviceId,revision){mutableStateOf(selected.label)}
                var aliases by remember(selected.pair.deviceId,revision){mutableStateOf(selected.aliases.joinToString(", "))}
                OutlinedTextField(name,{name=it.take(60)},label={Text("Selected computer name")})
                OutlinedTextField(aliases,{aliases=it.take(600)},label={Text("Unique voice aliases · comma separated")})
                OutlinedButton(enabled=idle,onClick={confirmed{runtime.companion.registry.rename(selected.pair.deviceId,name,aliases.split(',').map(String::trim).filter(String::isNotEmpty).toSet());revision++;status="Name and aliases saved."}}){Text("Save this computer's name / aliases")}
            }
            Text("Your phone handles voice. Only approved Chrome open/search commands are currently implemented; no computer microphone or speakers are needed.")
            Button(enabled=idle,onClick=::openGoogle){Text("Scan computer's one-time QR")}
            OutlinedButton(enabled=idle,onClick=::openBundled){Text("Use bundled camera scanner · offline fallback")}
            TextButton(enabled=idle,onClick={startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")))}){Text("Camera permission assistance · Android app settings")}
            OutlinedButton(enabled=idle,onClick={if(session.scannerOpening()){if(runtime.beginExclusiveCapture(session.scannerOwner)){model.handoff=true;try{gallery.launch("image/*")}catch(_:Exception){model.handoff=false;runtime.endExclusiveCapture(session.scannerOwner);session.scanFailure("SCANNER_FAILED")}}else session.scanFailure("CAPTURE_BUSY")}}){Text("Select QR image from gallery · decode locally")}
            OutlinedTextField(session.input,{session.setInvitation(it)},label={Text("Full private JARVIS-PAIR-1 invitation")},enabled=idle,maxLines=4)
            OutlinedButton(enabled=idle,onClick={val clip=getSystemService(ClipboardManager::class.java)?.primaryClip;val text=if(clip!=null && clip.itemCount>0)clip.getItemAt(0).text?.toString().orEmpty() else "";session.setInvitation(text)}){Text("Paste invitation")}
            TextButton(enabled=idle,onClick={session.setInvitation("")}){Text("Clear previous invitation")}
            if(flow.address.isNotBlank())Text("Computer: ${flow.name}\nAddress: ${flow.address}\nCertificate SHA-256: ${flow.pin}\nInvitation expires: ${if(flow.expires>0)java.util.Date(flow.expires)else "not active"}\nPair only with your trusted computer. Keep the invitation private.")
            Button(enabled=idle && flow.stage==PairingStage.INVITATION_READY,onClick={confirmed{session.request()}}){Text("Request pairing · approve on both devices")}
            if(flow.stage==PairingStage.FAILED && session.input.isNotBlank())OutlinedButton(enabled=idle,onClick={confirmed{session.request()}}){Text("Retry same request · reconcile a lost response")}
            if(flow.ticket.isNotBlank()){
                Text("Comparison code: ${flow.comparison}\nActual ticket: ${flow.ticket}\nUse approve followed by this ticket on your own computer after comparing the code.")
                OutlinedButton(enabled=idle,onClick={session.approval()}){Text("Check desktop approval")}
                Button(enabled=idle && flow.stage!=PairingStage.EXPIRED,onClick={confirmed{session.finish()}}){Text("Finish after approving on the computer")}
            }
            if(selected!=null){
                Button(enabled=idle,onClick={session.check(selected.pair)}){Text("Check actual authenticated connection")}
                Button(enabled=idle,onClick={runtime.submit("open Chrome on my computer and search for ChatGPT");finish()}){Text("Review a Chrome search test")}
                OutlinedButton(enabled=idle,onClick={confirmed{task{runtime.companion.revoke();session.revoked();revision++;status="Computer session revoked remotely and pairing removed locally."}}}){Text("Unpair on computer and forget locally")}
                TextButton(enabled=idle,onClick={confirmed{runtime.companion.forget();session.revoked();revision++;status="Forgot locally. If offline, use revoke-all on the computer to revoke that old session."}}){Text("Forget locally when computer is offline")}
            }
            OutlinedButton(enabled=idle,onClick={diagnostics=true;if(selected!=null)session.check(selected.pair)else if(flow.address.isNotBlank())session.inspect()else status="Scan or paste a valid invitation to check its pinned endpoint. Scanner/camera availability is shown below. No pairing request was sent."}){Text("Run Pairing Diagnostics")}
            if(diagnostics){Text(session.diagnostics());Text("Google Play services status: ${GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this@CompanionActivity)} (0 means available; scanner module initialization is verified only by opening the real scanner). Camera permission: ${ContextCompat.checkSelfPermission(this@CompanionActivity,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED}. Camera hardware: ${packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)}. No unauthenticated health check is presented as successful pairing.")}
            TextButton(enabled=idle,onClick={model.handoff=true;export.launch("Jarvis-pairing-redacted.txt")}){Text("Export Redacted Logs")}
            TextButton(onClick=::showInstructions){Text("Generate New Invitation on Computer · setup instructions")}
            TextButton(onClick=::showInstructions){Text("View Setup Instructions")}
            Text("Retry scanning after a failure, or type invite in the visible companion to generate a fresh QR. Expired invitations cannot be revived. A browser certificate warning is not proof of phone pairing. No TLS bypass is provided.")
        }
    }
    override fun onResume(){super.onResume();if(!isFinishing){model.handoff=false;nearby.start()}}
    override fun onStop(){nearby.stop();if(!isChangingConfigurations && !model.handoff){session.cancel();runtime.abandonExclusiveCapture(session.scannerOwner)};super.onStop()}
    override fun onDestroy(){if(!isChangingConfigurations)runtime.abandonExclusiveCapture(session.scannerOwner);super.onDestroy()}
}
