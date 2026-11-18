package com.jarvis.assistant.brain

import android.content.Intent
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.StatFs
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.brain.auth.OpenAiAuthException
import com.jarvis.assistant.R
import com.jarvis.assistant.core.ConnectionCheck
import com.jarvis.assistant.data.ChatGptPlanModel
import com.jarvis.assistant.data.OpenAiModelCatalog
import com.jarvis.assistant.data.ProviderException
import com.jarvis.assistant.local.ModelDownloadPolicy
import com.jarvis.assistant.local.ModelInstallPolicy
import com.jarvis.assistant.local.ModelRemovalPolicy
import com.jarvis.assistant.neural.BrainProfile
import com.jarvis.assistant.neural.BrainProfiles
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

/** Private settings around the existing adapters; provider passwords never enter this activity. */
@OptIn(ExperimentalLayoutApi::class)
class BrainManagerActivity:PrivateModuleActivity(){
    private val owner=Any()
    private var resumeEpoch by mutableIntStateOf(0)
    private var revision by mutableIntStateOf(0)
    private var planModels by mutableStateOf<List<ChatGptPlanModel>>(emptyList())
    private var planModel by mutableStateOf("")
    private var pendingProbe by mutableStateOf(false)
    private var signInPause:Long?=null
    private var planConsent by mutableStateOf(false)
    private var consentAccount:String?=null
    private var priorAuth:Pair<String?,Long>?=null
    private var offer by mutableStateOf<DownloadOffer?>(null)
    private var removeProfile by mutableStateOf<String?>(null)
    private var details by mutableStateOf<String?>(null)
    private var downloadProgress by mutableStateOf<Float?>(null)
    private data class DownloadOffer(val profile:BrainProfile,val descriptor:ModelDownloadPolicy.Descriptor,val required:Long,val free:Long,val partial:Long)

    private fun manage(block:suspend ()->Unit)=task{
        check(runtime.beginBrainManagement(owner)){"Finish the current recording or private operation first."}
        try{block()}
        catch(e:CancellationException){throw e}
        catch(e:ProviderException){status=e.message ?: "Provider connection failed."}
        catch(e:OpenAiAuthException){status=e.message ?: "ChatGPT authorization failed."}
        catch(e:IllegalArgumentException){status=e.message ?: "Check the selected model and connection settings."}
        catch(e:IllegalStateException){status=e.message ?: "The requested brain operation is unavailable."}
        finally{downloadProgress=null;runtime.endBrainManagement(owner);revision++}
    }
    private fun browser(url:String){
        val uri=Uri.parse(url)
        require(uri.scheme=="https" && uri.userInfo==null && uri.host in setOf("auth.openai.com","platform.openai.com","developers.openai.com","console.anthropic.com","platform.claude.com","chatgpt.com","aistudio.google.com"))
        startActivity(Intent(Intent.ACTION_VIEW,uri).addCategory(Intent.CATEGORY_BROWSABLE))
    }
    private fun signIn(accountId:String?){
        if(working || locked())return
        signInPause=runtime.pauseForAndroidUi()
        val old=runtime.openAiAuth.credentialDescriptor()
        priorAuth=old?.accountId to (old?.revision ?: -1L)
        try{
            val url=runtime.openAiAuth.beginSignIn(accountId)
            pendingProbe=true;browser(url)
        }catch(_:Exception){pendingProbe=false;runtime.openAiAuth.cancelSignIn();finishSignInPause();status=words("A browser could not start ChatGPT sign-in. Enable a browser and retry.","No se pudo abrir el inicio de ChatGPT. Activa un navegador y reintenta.","مرورگر ورود باز نشد.")}
    }
    private fun requestSignIn(accountId:String?){
        if(runtime.openAiAuth.state.value.accounts.none{it.signedIn}){consentAccount=accountId;planConsent=true}
        else signIn(accountId)
    }
    private fun finishSignInPause(){
        if(!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))return
        signInPause?.let(runtime::finishAndroidUi);signInPause=null
    }
    private suspend fun loadPlanModels(){
        planModels=runtime.chatGptModels()
        if(planModels.none{it.slug==planModel})planModel=planModels.first().slug
        runtime.brainManager.saveConfiguredModel(ProviderConfig("openai",planModel,ProviderCredentialMode.CHATGPT_PLAN))
    }
    private fun result(check:ConnectionCheck){status=words("Actual inference verified","Inferencia real verificada","پاسخ واقعی تأیید شد")+" · ${check.model} · ${check.elapsedMs} ms\n${check.sample}"}

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);if(isFinishing)return
        planModel=runtime.brainManager.configuredModel("openai",ProviderCredentialMode.CHATGPT_PLAN).orEmpty()
        screen(words("ARTIFICIAL INTELLIGENCE · AI BRAIN MANAGER","INTELIGENCIA ARTIFICIAL · GESTOR DE IA","مدیریت مغز هوش مصنوعی")){
            val auth by runtime.openAiAuth.state.collectAsStateWithLifecycle()
            val shared by runtime.state.collectAsStateWithLifecycle()
            Text(words("Choose a verified brain for the existing voice assistant. Speech capture, output voice and phone permissions remain separate.","Elige un cerebro verificado para el asistente de voz existente. La captura, la voz de salida y los permisos del teléfono son independientes.","مغز تأییدشده را انتخاب کنید. میکروفن و صدای خروجی مستقل هستند."))
            Text(words("Saved selection","Selección guardada","انتخاب ذخیره‌شده")+": ${shared.settings.provider} · ${shared.settings.model}")
            LaunchedEffect(auth,resumeEpoch){
                if(pendingProbe && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && !runtime.openAiAuth.state.value.signingIn){
                    pendingProbe=false
                    val current=runtime.openAiAuth.credentialDescriptor()
                    val changed=current!=null && priorAuth!=(current.accountId to current.revision)
                    val view=auth.accounts.firstOrNull{it.accountId==auth.selectedAccountId}
                    if(changed && view?.planAuthorized==true)manage{try{loadPlanModels();result(runtime.brainManager.verify(ProviderConfig("openai",planModel,ProviderCredentialMode.CHATGPT_PLAN)))}finally{finishSignInPause()}}
                    else {status=auth.message ?: words("Sign-in did not authorize AI access. Your previous brain is unchanged.","El inicio no autorizó acceso a IA. El cerebro anterior no cambió.","دسترسی هوش مصنوعی تأیید نشد.");finishSignInPause()}
                }
            }
            planCard()
            apiCard("openai","OpenAI API", "")
            apiCard("anthropic","Claude · Anthropic", "claude-sonnet-4-20250514")
            localCard()
            HorizontalDivider()
            Text(words("Existing integration retained","Integración existente conservada","اتصال قبلی حفظ شده است"),style=MaterialTheme.typography.titleMedium)
            apiCard("gemini","Gemini · Google", "gemini-2.5-flash",collapsed=true)
            TextButton(onClick={browser("https://developers.openai.com/siwc/token-sharing-open-source/sign-in")}){Text(words("Official ChatGPT authorization documentation","Documentación oficial de autorización ChatGPT","مستندات رسمی ورود"))}
            downloadProgress?.let{LinearProgressIndicator(progress={it},modifier=Modifier.fillMaxWidth());Text("${(it*100).toInt()}% · "+words("SHA-256 verification follows download","Se verifica SHA-256 tras descargar","سپس بررسی SHA-256 انجام می‌شود"))}
            if(working)TextButton(onClick={operation?.cancel();runtime.brain.cancel()}){Text(words("Cancel operation · keep resumable model bytes","Cancelar · conservar descarga reanudable","لغو و حفظ دانلود قابل ادامه"))}
            details?.let{text->AlertDialog(onDismissRequest={details=null},title={Text(words("Connection details","Detalles de conexión","جزئیات اتصال"))},text={Text(text)},confirmButton={TextButton(onClick={details=null}){Text("OK")}})}
            offer?.let{downloadDialog(it)}
            removeProfile?.let{id->removeDialog(id)}
            if(planConsent)AlertDialog(onDismissRequest={planConsent=false},title={Text("Continue with ChatGPT")},text={Text(words("You are connecting J.A.R.V.I.S. to your ChatGPT account. If OpenAI grants AI access, model replies and a small connection test use your authorized ChatGPT plan usage and its limits. Identity sign-in alone does not grant inference. Your ChatGPT chat history is not imported. No paid API fallback is automatic. Review the account, workspace and permissions in OpenAI's browser before approving.","Conectarás J.A.R.V.I.S. a tu cuenta ChatGPT. Si OpenAI autoriza IA, las respuestas y una pequeña prueba usan la cuota y límites de tu plan autorizado. Iniciar sesión no concede inferencia por sí solo. No se importan chats ni se cambia automáticamente a API de pago. Revisa cuenta, espacio y permisos en el navegador de OpenAI.","پاسخ‌ها و آزمایش کوچک از سهمیهٔ مجاز ChatGPT استفاده می‌کنند. ورود هویتی کافی نیست؛ تاریخچه وارد نمی‌شود و جایگزینی خودکار با API پولی انجام نمی‌شود. مجوزها و حساب را در مرورگر رسمی بررسی کنید."))},confirmButton={TextButton(onClick={planConsent=false;signIn(consentAccount)}){Text(words("Continue to OpenAI","Continuar a OpenAI","ادامه در OpenAI"))}},dismissButton={TextButton(onClick={planConsent=false}){Text(words("Cancel","Cancelar","لغو"))}})
        }
    }
    override fun onResume(){super.onResume();resumeEpoch++}

    @Composable private fun statusLine(config:ProviderConfig,credentialDraft:Boolean=false):ProviderConnection?{
        val connections by runtime.brainManager.connections.collectAsStateWithLifecycle()
        val shared by runtime.state.collectAsStateWithLifecycle()
        LaunchedEffect(config,revision,credentialDraft){
            while(true){
                if(!credentialDraft && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))runtime.brainManager.refresh(config)
                delay(30_000) // Metadata expiry only; never background inference.
            }
        }
        val connection=connections[config.id]?.takeIf{it.config==config && !credentialDraft}
        val verified=connection?.status==ProviderConnectionStatus.VERIFIED
        val unavailable=connection?.failure in setOf(ProviderFailureKind.CREDITS,ProviderFailureKind.RATE_LIMIT,ProviderFailureKind.USAGE_UNAVAILABLE)
        Text(if(unavailable)words("Usage Unavailable","Uso no disponible","سهمیه در دسترس نیست") else when(connection?.status){
            ProviderConnectionStatus.VERIFIED->words("AI Connected · actual response verified","IA conectada · respuesta real verificada","اتصال با پاسخ واقعی تأیید شد")
            ProviderConnectionStatus.PREVIOUSLY_VERIFIED->words("Previously verified · check current access","Verificada anteriormente · comprueba el acceso actual","قبلاً تأیید شده؛ اکنون آزمایش کنید")
            ProviderConnectionStatus.VERIFYING->words("Testing actual inference…","Probando inferencia real…","در حال آزمایش پاسخ واقعی…")
            ProviderConnectionStatus.FAILED->words("Connection failed","Conexión fallida","اتصال ناموفق")
            else->words("Authorization or connection test required","Se requiere autorización o prueba","ورود یا آزمایش لازم است")
        },color=if(verified)Color(0xFF22C55E) else if(connection?.status==ProviderConnectionStatus.FAILED)Color(0xFFFFB3B3) else MaterialTheme.colorScheme.onSurfaceVariant)
        connection?.message?.takeIf{it.isNotBlank()}?.let{Text(it,style=MaterialTheme.typography.bodySmall)}
        if(verified && shared.settings.provider==config.provider && shared.settings.model==config.model && runtime.brainManager.selectedMode(config.provider)==config.mode)Text(words("PRIMARY BRAIN","CEREBRO PRINCIPAL","مغز اصلی"),color=Color(0xFF22C55E))
        return connection
    }
    @Composable private fun providerBox(title:String,content:@Composable ColumnScope.()->Unit){
        OutlinedCard(modifier=Modifier.fillMaxWidth(),border=BorderStroke(1.dp,Color(0xFF007F91))){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text(title,style=MaterialTheme.typography.titleLarge);content()
        }}
    }
    @Composable private fun planCard(){
        val auth by runtime.openAiAuth.state.collectAsStateWithLifecycle()
        val account=auth.accounts.firstOrNull{it.accountId==auth.selectedAccountId}
        val config=planModel.takeIf{it.isNotBlank()}?.let{ProviderConfig("openai",it,ProviderCredentialMode.CHATGPT_PLAN)}
        providerBox("ChatGPT · OpenAI"){
            Text(words("Connect your ChatGPT account using OpenAI's official browser authorization. Google account choice, when offered, stays inside OpenAI's login.","Conecta tu cuenta ChatGPT mediante la autorización oficial del navegador. Si se ofrece Google, eliges la cuenta dentro del inicio de OpenAI.","ورود در مرورگر رسمی OpenAI انجام می‌شود."))
            if(auth.signingIn){CircularProgressIndicator(Modifier.size(24.dp));Text(words("Signing in · complete the browser flow, then return here","Iniciando sesión · completa el navegador y vuelve aquí","ورود را در مرورگر تکمیل و بازگردید"))}
            account?.let{Text(it.label);Text(if(!it.signedIn)words("Saved account · signed out","Cuenta guardada · sesión cerrada","حساب ذخیره‌شده و خارج‌شده") else if(!it.planAuthorized)words("Connected — Identity Only · AI access unavailable","Conectado — solo identidad · IA no autorizada","فقط هویت تأیید شده؛ دسترسی مدل مجاز نیست") else words("AI authorization granted · verify model response below","Autorización IA concedida · verifica una respuesta abajo","مجوز مدل صادر شده؛ پاسخ را آزمایش کنید"))}
            auth.message?.let{Text(it)}
            FlowRow{
                Button(enabled=!working && !auth.signingIn,onClick={requestSignIn(auth.selectedAccountId)},colors=ButtonDefaults.buttonColors(containerColor=Color.Black,contentColor=Color.White)){
                    Icon(painterResource(R.drawable.ic_chatgpt_official),contentDescription=null,modifier=Modifier.size(21.dp),tint=Color.Unspecified)
                    Spacer(Modifier.width(12.dp));Text("Continue with ChatGPT")
                }
                OutlinedButton(enabled=!working && !auth.signingIn,onClick={requestSignIn(null)}){Text(words("Add account or workspace","Añadir cuenta o espacio","افزودن حساب یا فضای کاری"))}
                if(auth.signingIn)TextButton(onClick={pendingProbe=false;runtime.openAiAuth.cancelSignIn();finishSignInPause()}){Text(words("Cancel sign-in","Cancelar inicio","لغو ورود"))}
            }
            if(auth.accounts.size>1)auth.accounts.forEach{saved->Row{
                RadioButton(saved.accountId==auth.selectedAccountId,{authorize{runtime.openAiAuth.selectAccount(saved.accountId);planModels=emptyList();revision++}},enabled=!working && !auth.signingIn)
                Text(saved.label)
            }}
            Text(words("Uses authorized ChatGPT plan usage. Subscription eligibility, workspace and limits are decided by OpenAI. No automatic paid API fallback or conversation-history import.","Usa la cuota autorizada del plan ChatGPT. OpenAI determina elegibilidad, espacio y límites. No cambia automáticamente a API de pago ni importa chats.","سهمیهٔ مجاز ChatGPT استفاده می‌شود؛ تاریخچه وارد نمی‌شود."))
            TextButton(onClick={browser("https://chatgpt.com/settings/usage")}){Text(words("Manage ChatGPT usage","Gestionar uso de ChatGPT","مدیریت سهمیهٔ ChatGPT"))}
            OutlinedButton(enabled=!working && !auth.signingIn && account?.planAuthorized==true,onClick={manage{loadPlanModels();status=words("Available account models loaded; inference is not yet verified.","Modelos cargados; la inferencia aún no está verificada.","فهرست مدل‌ها دریافت شد؛ پاسخ هنوز تأیید نشده است.")}}){Text(words("Load authorized model choices","Cargar modelos autorizados","دریافت مدل‌های مجاز"))}
            if(planModels.isNotEmpty())modelPicker(planModel,planModels.map{it.slug to it.displayName}){next->planModel=next;runtime.brainManager.saveConfiguredModel(ProviderConfig("openai",next,ProviderCredentialMode.CHATGPT_PLAN));revision++}
            else if(planModel.isNotBlank())Text(words("Previously selected model","Modelo seleccionado anteriormente","مدل انتخابی قبلی")+": $planModel")
            val connection=config?.let{statusLine(it)}
            FlowRow{
                Button(enabled=config!=null && account?.planAuthorized==true && !working && !auth.signingIn,onClick={manage{result(runtime.brainManager.verify(config!!))}}){Text(words("Test Connection","Probar conexión","آزمایش اتصال"))}
                Button(enabled=config!=null && connection?.status==ProviderConnectionStatus.VERIFIED && !working,onClick={authorize{manage{runtime.selectBrain(config!!);status=words("ChatGPT is the selected AI Brain.","ChatGPT es el cerebro seleccionado.","ChatGPT انتخاب شد.")}}}){Text(words("Set as Primary Brain","Elegir cerebro principal","انتخاب به‌عنوان مغز اصلی"))}
                TextButton(onClick={details="Official OAuth public client + PKCE, signed identity validation, separate plan scopes, Responses API.\n"+(account?.email ?: "Email unavailable")+"\nGranted permissions: "+runtime.openAiAuth.credentialDescriptor()?.scopes.orEmpty().sorted().joinToString(" ")+"\nLast verified response: "+(connection?.verifiedAt?.let{java.util.Date(it).toString()} ?: "Not verified")}){Text(words("Connection Details","Detalles de conexión","جزئیات اتصال"))}
                OutlinedButton(enabled=account?.signedIn==true && !working,onClick={authorize{manage{status=if(config!=null)runtime.disconnectBrain(config) else runtime.openAiAuth.disconnect().message;planModels=emptyList()}}}){Text(words("Disconnect ChatGPT","Desconectar ChatGPT","قطع اتصال ChatGPT"))}
            }
        }
    }

    @Composable private fun apiCard(provider:String,title:String,defaultModel:String,collapsed:Boolean=false){
        var expanded by rememberSaveable{mutableStateOf(!collapsed)}
        var model by rememberSaveable{mutableStateOf(runtime.brainManager.configuredModel(provider,ProviderCredentialMode.API_KEY) ?: defaultModel)}
        var key by remember{mutableStateOf("")}
        var models by remember{mutableStateOf<List<String>>(emptyList())}
        var catalogError by remember{mutableStateOf<String?>(null)}
        var internet by remember{mutableStateOf("Unknown")}
        LaunchedEffect(revision,resumeEpoch){
            internet=runCatching{
                val connectivity=getSystemService(ConnectivityManager::class.java)
                val capabilities=connectivity.getNetworkCapabilities(connectivity.activeNetwork)
                when{capabilities==null->"Unavailable";capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)->"Validated";else->"Unvalidated"}
            }.getOrDefault("Unknown")
        }
        val lifecycle=LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle){val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP){key="";models=emptyList();catalogError=null}};lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer);key=""}}
        val config=runCatching{ProviderConfig(provider,model.trim(),ProviderCredentialMode.API_KEY).validated()}.getOrNull()
        val auth by runtime.openAiAuth.state.collectAsStateWithLifecycle()
        providerBox(title){
            Text(if(provider=="anthropic")words("Claude requires supported API access. Signing into claude.ai does not authorize this Android integration. Review your API account and available credits.","Claude requiere acceso API. Iniciar sesión en claude.ai no autoriza esta integración Android. Revisa tu cuenta API y sus créditos.","Claude به دسترسی API نیاز دارد.") else words("Explicit API access is separate from account sign-in and ChatGPT plan usage. It uses the provider API project's quota or billing.","El acceso API es independiente del inicio y del plan ChatGPT. Usa la cuota o facturación del proyecto API.","دسترسی API و سهمیهٔ آن مستقل است."))
            TextButton(enabled=!working,onClick={expanded=!expanded}){Text(words("Configure API Access","Configurar acceso API","تنظیم API"))}
            if(expanded){
                OutlinedTextField(key,{key=it.take(1000);models=emptyList();catalogError=null},label={Text(words("Personal API key · blank keeps saved key","Clave API personal · vacío conserva la guardada","کلید API شخصی"))},visualTransformation=PasswordVisualTransformation(),enabled=!working,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(model,{model=it.take(100)},label={Text(words("Model ID","ID del modelo","شناسهٔ مدل"))},enabled=!working,modifier=Modifier.fillMaxWidth())
                val connection=config?.let{statusLine(it,key.isNotBlank())}
                var retryReady by remember(connection?.retryAt){mutableStateOf(connection?.retryAllowed(System.currentTimeMillis())!=false)}
                LaunchedEffect(connection?.retryAt){
                    val wait=((connection?.retryAt ?: 0L)-System.currentTimeMillis()).coerceAtLeast(0L)
                    if(wait>0)delay(wait)
                    retryReady=true
                }
                Text(words("Test & Save stores this key in Android Keystore-protected storage and sends one small diagnostic request that may consume API credits. Listing models is not a successful inference test.","Probar y guardar almacena la clave protegida por Android Keystore y envía una pequeña petición de prueba. Listar modelos no prueba la inferencia.","آزمایش، کلید را امن ذخیره و یک درخواست کوچک اجرا می‌کند."),style=MaterialTheme.typography.bodySmall)
                FlowRow{
                    Button(enabled=config!=null && !working && !auth.signingIn && retryReady,onClick={val candidate=key;authorize{manage{runtime.configureBrainApi(config!!,candidate);key="";result(runtime.brainManager.verify(config))}}}){Text(words("Test / Retry & Save Connection","Probar / reintentar y guardar","آزمایش دوباره و ذخیرهٔ اتصال"))}
                    OutlinedButton(enabled=!working,onClick={val candidate=key;manage{models=emptyList();catalogError=null;try{models=runtime.brainApiModels(provider,candidate)}catch(e:ProviderException){catalogError="Unable to retrieve available models. ${e.kind}: ${e.message}";throw e};status=words("Account model list loaded; test the chosen model before selecting it.","Lista cargada; prueba el modelo antes de elegirlo.","فهرست دریافت شد؛ مدل را آزمایش کنید.")}}){Text(words("Refresh Models / Retry","Actualizar modelos / reintentar","دریافت دوبارهٔ مدل‌ها"))}
                    Button(enabled=key.isBlank() && connection?.status==ProviderConnectionStatus.VERIFIED && !working,onClick={authorize{manage{runtime.selectBrain(config!!);status="$title · "+words("Primary Brain selected","Cerebro principal elegido","مغز اصلی انتخاب شد")}}}){Text(words("Set as Primary Brain","Elegir cerebro principal","انتخاب مغز اصلی"))}
                }
                catalogError?.let{Text(it,color=Color(0xFFFFB3B3))}
                if(models.isNotEmpty())modelPicker(model,models.map{it to if(provider=="openai")"$it · ${OpenAiModelCatalog.capability(it)}" else it}){model=it}
                if(provider=="openai"){
                    Text("Backend: Not required · this personal key connects directly to OpenAI. Android Keystore protects it at rest; a server-held key provides stronger isolation. ChatGPT subscription and API billing are separate.",style=MaterialTheme.typography.bodySmall)
                    OutlinedButton(enabled=!working && !auth.signingIn,onClick={val candidate=key;authorize{manage{
                        models=emptyList();catalogError=null
                        val selected=OpenAiDefaultSelection.connect(
                            catalog={try{runtime.brainApiModels("openai",candidate).also{models=it}}catch(e:ProviderException){catalogError="Unable to retrieve available models. ${e.kind}: ${e.message}";throw e}},
                            configure={runtime.configureBrainApi(it,candidate);model=it.model;key=""},verify={runtime.brainManager.verify(it)},select={runtime.selectBrain(it)})
                        model=selected.first.model;key="";result(selected.second)
                    }}}){Text("Discover, test & select default")}
                    Text("This explicit action lists your account's models, tests one listed text model using API credits, and selects it only if actual inference succeeds. No alternate model is silently tried.",style=MaterialTheme.typography.bodySmall)
                }
                FlowRow{
                    TextButton(onClick={browser(when(provider){"anthropic"->"https://console.anthropic.com/";"gemini"->"https://aistudio.google.com/";else->"https://platform.openai.com/"})}){Text(words("Open provider console","Abrir consola del proveedor","کنسول ارائه‌دهنده"))}
                    TextButton(onClick={details=if(config==null)"Enter an exact model ID or discover available models. Backend: Not required." else ProviderDiagnostics.describe(config,connection,runtime.store.getApiKey(provider).isNotBlank(),internet,System.currentTimeMillis())}){Text(words("Connection Details","Detalles de conexión","جزئیات اتصال"))}
                    OutlinedButton(enabled=config!=null && !working,onClick={authorize{manage{status=runtime.disconnectBrain(config!!);key=""}}}){Text(words("Disconnect API Access","Desconectar acceso API","قطع اتصال API"))}
                }
            }
        }
    }

    @Composable private fun modelPicker(selected:String,choices:List<Pair<String,String>>,choose:(String)->Unit){
        var menu by remember{mutableStateOf(false)}
        Box{OutlinedButton(enabled=!working,onClick={menu=true}){Text(choices.firstOrNull{it.first==selected}?.second ?: selected.ifBlank{"Choose model"})}
            DropdownMenu(menu,{menu=false},modifier=Modifier.heightIn(max=280.dp)){choices.forEach{item->DropdownMenuItem(text={Text(item.second)},onClick={menu=false;choose(item.first)})}}
        }
    }
    @Composable private fun localCard(){
        var selected by rememberSaveable{mutableStateOf(runtime.brain.profile.id)}
        val profile=BrainProfiles.all.first{it.available && it.id==selected}
        var bytes by remember{mutableLongStateOf(0L)}
        var integrity by remember{mutableStateOf<ModelRemovalPolicy.IntegrityResult?>(null)}
        var storageProblem by remember{mutableStateOf<String?>(null)}
        val config=ProviderConfig("local",profile.id,ProviderCredentialMode.LOCAL)
        LaunchedEffect(selected,revision){
            try{bytes=withContext(Dispatchers.IO){runtime.brain.ownedStorageBytes(profile)};storageProblem=null}
            catch(e:CancellationException){throw e}
            catch(_:Exception){bytes=0;storageProblem=words("Managed model storage could not be inspected safely. No files were removed.","No se pudo inspeccionar el espacio de modelos con seguridad. No se eliminó ningún archivo.","بررسی امن فضای مدل ممکن نشد؛ فایلی حذف نشده است.")}
            integrity=null
        }
        providerBox(words("Local Brain · optional on-device AI","Cerebro local · IA opcional en el dispositivo","مغز محلی اختیاری")){
            Text(words("Cloud brains work without downloaded local LLM weights. Wake, recognition, speech, OCR and core libraries are retained when a local brain is removed.","La nube funciona sin pesos LLM locales. La activación, reconocimiento, voz, OCR y bibliotecas del núcleo se conservan al eliminar un cerebro local.","مدل محلی برای اتصال ابری اجباری نیست؛ اجزای صدا حفظ می‌شوند."))
            modelPicker(selected,BrainProfiles.all.filter{it.available}.map{it.id to it.title}){selected=it}
            Text(words("Managed storage","Almacenamiento gestionado","فضای مدیریت‌شده")+": ${ModelInstallPolicy.formatBytes(bytes)}")
            Text(storageProblem ?: if(bytes==0L)words("Not Installed","No instalado","نصب نشده") else integrity?.message ?: words("Downloaded bytes present · check integrity and test inference","Hay archivos descargados · verifica integridad e inferencia","فایل موجود است؛ صحت و پاسخ را آزمایش کنید"))
            val connection=statusLine(config)
            FlowRow{
                Button(enabled=!working,onClick={manage{
                    val descriptor=runtime.brain.inspectDownload(profile)
                    val partial=runtime.brain.resumableBytes(profile,descriptor)
                    offer=DownloadOffer(profile,descriptor,ModelInstallPolicy.requiredFreeBytes(descriptor.size,partial),StatFs(filesDir.absolutePath).availableBytes,partial)
                }}){Text(words("Install / Repair Local Brain","Instalar / reparar cerebro local","نصب یا تعمیر مغز محلی"))}
                OutlinedButton(enabled=!working,onClick={manage{integrity=runtime.brain.verifyIntegrity(profile);status=integrity!!.message}}){Text(words("Check Model Integrity","Verificar integridad","بررسی صحت مدل"))}
                Button(enabled=bytes>0 && !working,onClick={manage{result(runtime.testLocalBrain(profile.id))}}){Text(words("Test Local Inference","Probar inferencia local","آزمایش پاسخ محلی"))}
                Button(enabled=connection?.status==ProviderConnectionStatus.VERIFIED && !working,onClick={authorize{manage{runtime.selectBrain(config);status=words("Verified Local Brain selected.","Cerebro local verificado seleccionado.","مغز محلی تأییدشده انتخاب شد.")}}}){Text(words("Set as Primary Brain","Elegir cerebro principal","انتخاب مغز اصلی"))}
            }
            Text("${profile.title} · "+words("Estimated working RAM","RAM de trabajo estimada","حافظهٔ تخمینی")+": ${profile.workingMiB} MiB. "+(runtime.brain.hardwareError(profile) ?: words("ARM64 profile; Redmi9 speed and thermal behavior remain unmeasured.","Perfil ARM64; rendimiento y temperatura del Redmi9 aún sin medir.","عملکرد واقعی Redmi9 هنوز اندازه‌گیری نشده است.")))
            FlowRow{
                OutlinedButton(enabled=bytes>0 && !working,onClick={removeProfile=profile.id}){Text(words("Remove This Local Brain","Eliminar este cerebro local","حذف این مغز محلی"))}
                OutlinedButton(enabled=!working,onClick={removeProfile="all"}){Text(words("Remove All Downloaded LLMs","Eliminar todos los LLM descargados","حذف همهٔ مدل‌های زبانی دانلودشده"))}
                TextButton(onClick={details="Only catalog-owned LLM weights, partial downloads and integrity records are removable. Bundled native/runtime/APK bytes cannot be removed separately. Other user files, voice packs, credentials, settings, databases and benchmark records remain.\nOffline inference requires an installed, integrity-checked model with a successful actual inference test; choosing ChatGPT or Claude does not require it."}){Text(words("Storage & Offline Mode Details","Detalles de espacio y modo sin conexión","جزئیات فضای محلی"))}
            }
        }
    }
    @Composable private fun downloadDialog(value:DownloadOffer){
        AlertDialog(onDismissRequest={offer=null},title={Text(words("Review Local Brain download","Revisar descarga del cerebro","بررسی دانلود مغز محلی"))},text={Text("${value.profile.title}\n${value.profile.repository}\n"+words("Download","Descarga","دانلود")+": ${ModelInstallPolicy.formatBytes(value.descriptor.size)}\n"+words("Resumable","Reanudable","قابل ادامه")+": ${ModelInstallPolicy.formatBytes(value.partial)}\n"+words("Additional storage needed / free","Espacio adicional / libre","فضای لازم / آزاد")+": ${ModelInstallPolicy.formatBytes(value.required)} / ${ModelInstallPolicy.formatBytes(value.free)}\nApache-2.0 · ${value.descriptor.revision}\nSHA-256 ${value.descriptor.sha256}\n"+words("Only model files for this profile may be replaced. This does not select it as the primary brain. Cancel preserves a revision-bound partial where possible. Wi-Fi is recommended; mobile data may cost money.","Solo se reemplazan archivos del modelo elegido. No lo elige como cerebro principal. Cancelar conserva una descarga ligada a la revisión cuando sea posible. Se recomienda Wi-Fi.","فقط فایل‌های همین مدل جایگزین می‌شود؛ انتخاب مغز اصلی جداست."))},confirmButton={TextButton(enabled=value.free>value.required && !working,onClick={offer=null;authorize{manage{runtime.brain.download(value.descriptor,value.profile){downloadProgress=it};val checked=runtime.brain.verifyIntegrity(value.profile);check(checked.verified){checked.message};status=words("Weights installed and integrity verified. Run the actual Local Inference test before selecting them.","Pesos instalados e integridad verificada. Prueba la inferencia antes de elegirlos.","مدل نصب و صحت تأیید شد؛ پاسخ را آزمایش کنید.")}}}){Text(words("Download Verified Weights","Descargar pesos verificados","دانلود مدل با صحت‌سنجی"))}},dismissButton={TextButton(onClick={offer=null}){Text(words("Cancel","Cancelar","لغو"))}})
    }
    @Composable private fun removeDialog(id:String){
        AlertDialog(onDismissRequest={removeProfile=null},title={Text(words("Remove downloaded Local Brain?","¿Eliminar cerebro local descargado?","حذف مغز محلی دانلودشده؟"))},text={Text(words("Only confirmed application-owned LLM model files are removed. You can reinstall later. AI accounts, settings, memories, voice components, the core and wallpaper remain. If Local is selected, it will be unavailable until reinstalled and tested.","Solo se eliminan archivos LLM gestionados. Podrás reinstalarlos. Cuentas, ajustes, memoria, voz, núcleo y fondo se conservan. Si Local está elegido, no estará disponible hasta reinstalarlo y probarlo.","فقط فایل‌های مدل زبانی حذف می‌شود؛ حساب‌ها، تنظیمات و اجزای صدا حفظ می‌شوند."))},confirmButton={TextButton(onClick={removeProfile=null;authorize{manage{val freed=runtime.removeLocalBrains(id.takeUnless{it=="all"});status=words("Managed model bytes removed","Bytes de modelos eliminados","فضای مدل حذف‌شده")+": ${ModelInstallPolicy.formatBytes(freed)}"}}}){Text(words("Remove Downloaded Models","Eliminar modelos descargados","حذف مدل‌های دانلودشده"))}},dismissButton={TextButton(onClick={removeProfile=null}){Text(words("Cancel","Cancelar","لغو"))}})
    }
}
