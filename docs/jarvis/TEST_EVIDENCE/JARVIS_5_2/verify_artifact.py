"""SDK/ZIP checks for the Phoenix preview. Does not establish physical installation/audio success."""
import hashlib,json,os,re,struct,subprocess,zipfile,zlib
from pathlib import Path

repo=next(p for p in Path(__file__).resolve().parents if (p/'gradlew').is_file())
out=Path(__file__).resolve().parent
sdk=Path(os.environ['ANDROID_HOME'])/'build-tools/35.0.0'
apk=repo/'app/build/outputs/apk/debug/app-debug.apk'
previous=repo/'downloads/jarvis-4.1-preview/Jarvis-4.1.0-preview.1-debug-arm64.apk'
original=repo/'downloads/polish-2.11.8/Jarvis-2.11.8-arm64-update.apk'
package='com.jarvis.next.phoenix'
expected_signer='f9f13af1f84515b787b89c0f3abc8695f1d0d45e8a647d1192966c4c810964c7'

def command(tool,args,name):
    result=subprocess.run([str(sdk/tool),*map(str,args)],capture_output=True,text=True)
    (out/name).write_text(result.stdout+result.stderr)
    if result.returncode:raise RuntimeError(f'{tool} exited {result.returncode}; see {name}')
    return result.stdout.replace("http://schemas.android.com/apk/res/android:","android:")

def sha(path):
    with path.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()

def attrs(text,name):return set(re.findall(r'android:'+re.escape(name)+r'\([^)]*\)="([^"]+)"',text))

def fingerprint(text):return re.search(r'Signer #1 certificate SHA-256 digest: ([a-f0-9]+)',text).group(1)

def component(text,name):
    lines=text.splitlines()
    for i,line in enumerate(lines):
        if re.search(r'E: (?:activity|service|receiver)\b',line):
            indent=len(line)-len(line.lstrip());end=i+1
            while end<len(lines) and not (lines[end].lstrip().startswith('E: ') and len(lines[end])-len(lines[end].lstrip())<=indent):end+=1
            body='\n'.join(lines[i:end])
            if name in attrs(body,'name'):return body
    return ''

badging=command('aapt2',['dump','badging',apk],'apk-badging.txt')
manifest=command('aapt2',['dump','xmltree',apk,'--file','AndroidManifest.xml'],'apk-manifest.txt')
old_manifest=command('aapt2',['dump','xmltree',previous,'--file','AndroidManifest.xml'],'previous-manifest.txt')
original_manifest=command('aapt2',['dump','xmltree',original,'--file','AndroidManifest.xml'],'original-manifest.txt')
signature=command('apksigner',['verify','--verbose','--print-certs',apk],'apk-signature.txt')
old_signature=command('apksigner',['verify','--verbose','--print-certs',previous],'previous-signature.txt')
original_signature=command('apksigner',['verify','--verbose','--print-certs',original],'original-signature.txt')
alignment=command('zipalign',['-c','-v','4',apk],'apk-alignment.txt')
checks=[]
def check(name,condition,evidence):checks.append({'name':name,'status':'PASS' if condition else 'FAIL','evidence':evidence})
check('Existing Phoenix package identity retained',f"package: name='{package}'" in badging,package)
check('Expected version name',"versionName='5.2.0-preview.1-debug'" in badging,'5.2.0-preview.1-debug')
check('Version code increments known previous preview',"versionCode='33'" in badging,{'current':33,'previous':32})
check('SDK range retained',bool(re.search(r"^(?:sdkVersion|minSdkVersion):'29'$",badging,re.MULTILINE)) and "targetSdkVersion:'35'" in badging,{'minimum':29,'target':35})
check('Display name retained in packaged locales',all(f"application-label{s}:'Jarvis'" in badging for s in ['', '-es','-fa']),'Jarvis')
check('Existing launcher retained',"launchable-activity: name='com.jarvis.assistant.MainActivity'" in badging,'MainActivity')
check('Same signing certificate as known previous APK',fingerprint(signature)==fingerprint(old_signature)==expected_signer,fingerprint(signature))
check('Original production signer difference disclosed',fingerprint(signature)!=fingerprint(original_signature),{'preview':fingerprint(signature),'retained_original':fingerprint(original_signature),'original_update_compatibility_claimed':False})
check('APK signature v2 verification','Verified using v2 scheme (APK Signature Scheme v2): true' in signature,'apksigner exit0')
check('APK ZIP alignment',alignment.rstrip().endswith(('Verification successful','Verification succesful')),'zipalign exit0')
check('No shared UID','sharedUserId' not in manifest,'No sharedUserId')
check('Providers remain isolated from original production package',bool(attrs(manifest,'authorities')) and attrs(manifest,'authorities').isdisjoint(attrs(original_manifest,'authorities')) and all(x.startswith(package+'.') for x in attrs(manifest,'authorities')),sorted(attrs(manifest,'authorities')))
check('Screen capture task affinity retained',attrs(manifest,'taskAffinity')=={package+'.screen'},sorted(attrs(manifest,'taskAffinity')))
check('Spotify callback identity retained',package in attrs(manifest,'scheme') and 'com.jarvis.next' not in attrs(manifest,'scheme'),sorted(attrs(manifest,'scheme')))
check('Original wallpaper service retained','com.jarvis.assistant.sentinel.JarvisWallpaperService' in manifest,'Existing wallpaper service')
new_components=['companion.PairingCaptureActivity','companion.PairingHelpActivity','google.GoogleCloudConfigurationActivity','modes.SystemModesActivity','setup.NexusSetupActivity','studio.WallpaperStudioActivity','controls.AndroidControlsActivity','controls.AndroidControlActionActivity','controls.VoiceCommandLabActivity','google.GoogleAccountsActivity','notifications.NotificationAwarenessActivity','reminders.RemindersActivity','reminders.ReminderTimeReceiver','screen.VideoRecordingActivity','screen.VideoRecordingService']
private_components={name:bool(re.search(r'android:exported\([^)]*\)=(?:false|\(type 0x12\)0x0+)\b',component(manifest,'com.jarvis.assistant.'+name))) for name in new_components}
check('All new owned entry points are nonexported',all(private_components.values()),private_components)
recording=component(manifest,'com.jarvis.assistant.screen.VideoRecordingService')
check('Recording service declares mediaProjection foreground type',bool(re.search(r'android:foregroundServiceType\([^)]*\)=(?:\(type 0x11\))?(?:0x0*20|32)\b',recording)),'VideoRecordingService mediaProjection only')
revocation=component(manifest,'com.google.android.gms.auth.api.signin.internal.RevocationBoundService') or component(manifest,'com.google.android.gms.auth.api.signin.RevocationBoundService')
# Google exports this SDK service specifically for its protected revocation notification.
if not revocation:
    revocation=next((component(manifest,n)for n in attrs(manifest,'name')if n.endswith('.RevocationBoundService')),'')
check('Google SDK revocation entry requires its protected permission','com.google.android.gms.auth.api.signin.permission.REVOCATION_NOTIFICATION' in revocation,'Google SDK permission retained')
permission_pattern=r'E: uses-permission[^\n]*\n\s+A: android:name\([^)]*\)="([^"]+)"'
permissions=set(re.findall(permission_pattern,manifest));old_permissions=set(re.findall(permission_pattern,old_manifest))
check('Manifest permission expansion is reviewed and limited',permissions-old_permissions==set(),sorted(permissions-old_permissions))
check('No new broad-account/app/storage permissions',not {'android.permission.GET_ACCOUNTS','android.permission.QUERY_ALL_PACKAGES','android.permission.MANAGE_EXTERNAL_STORAGE'}&permissions,sorted(permissions))
with zipfile.ZipFile(apk) as current,zipfile.ZipFile(previous)as old:
    names=current.namelist();check('ZIP CRC integrity',current.testzip()is None,'zipfile.testzip')
    check('No duplicate or traversing ZIP entries',len(names)==len(set(names)) and all('..' not in Path(n).parts and not n.startswith('/')for n in names),'ZIP entry names')
    preserved={n for n in old.namelist()if not n.endswith('/')and(n.startswith('assets/')or n.startswith('lib/'))}
    unchanged=preserved.issubset(names)and all(hashlib.sha256(old.read(n)).digest()==hashlib.sha256(current.read(n)).digest()for n in preserved)
    check('All previous native libraries and assets retained byte for byte',unchanged,{'files':len(preserved),'native_libraries':sum(n.startswith('lib/')for n in preserved)})
    libs=[n for n in names if n.startswith('lib/')and n.endswith('.so')];machines={};page_alignments={}
    for n in libs:
        raw=current.read(n);machines[n]=struct.unpack_from('<H',raw,18)[0]if raw[:4]==b'\x7fELF'else None
        if raw[:5]==b'\x7fELF\x02':
            offset=struct.unpack_from('<Q',raw,32)[0];entry_size,count=struct.unpack_from('<HH',raw,54)
            loads=[struct.unpack_from('<Q',raw,offset+i*entry_size+48)[0]for i in range(count)if struct.unpack_from('<I',raw,offset+i*entry_size)[0]==1]
            page_alignments[n]=min(loads)if loads else None
    check('Native architecture is complete ARM64-only',len(libs)>=14 and all(v==183 for v in machines.values())and all(n.startswith('lib/arm64-v8a/')for n in libs),machines)
    dex=[n for n in names if re.fullmatch(r'classes[0-9]*\.dex',n)];dex_status={};combined=[]
    for n in dex:
        raw=current.read(n);dex_status[n]=raw.startswith(b'dex\n')and len(raw)==struct.unpack_from('<I',raw,32)[0]and hashlib.sha1(raw[32:]).digest()==raw[12:32]and zlib.adler32(raw[12:])&0xffffffff==struct.unpack_from('<I',raw,8)[0];combined.append(raw)
    check('All production DEX lengths and checksums valid',bool(dex_status)and all(dex_status.values()),dex_status)
    check('No MockWebServer fixtures packaged',all(b'Lokhttp3/mockwebserver/MockWebServer;'not in raw for raw in combined),{'dex_files':len(dex)})
    key_hits=[n for n in names if(n.endswith('.dex')or n.startswith('assets/'))and re.search(rb'sk-(?:proj-|svcacct-)?[A-Za-z0-9_-]{32,}',current.read(n))]
    check('No hardcoded OpenAI key pattern in executable/assets',not key_hits,{'matching_files':key_hits,'limitation':'Pattern scan supplements source review; not a universal secret detector.'})
    catalogue=current.read('assets/farewells/catalog.tsv').decode('utf-8');rows=[r.split('|',2)for r in catalogue.splitlines()if r and not r.startswith('#')]
    counts={lang:sum(r[0]==lang for r in rows)for lang in ['en','es','fa']}
    check('Actual multilingual farewell catalogue packaged',len(rows)==510 and all(v==170 for v in counts.values())and len({r[2]for r in rows})==510,{'entries':len(rows),'languages':counts})
    check('Official native Google authorization classes packaged',any(b'Lcom/google/android/gms/auth/api/identity/AuthorizationClient;'in raw for raw in combined),'Google AuthorizationClient')

    check('Google QR scanner class is packaged',any(b'Lcom/google/mlkit/vision/codescanner/GmsBarcodeScanning;'in raw for raw in combined),'Actual SDK class, not a static preview')
    check('Bundled on-device barcode scanning JNI is packaged',any('barhopper' in n for n in libs),[n for n in libs if 'barhopper' in n])
    model_files=[n for n in names if n.startswith('assets/') and any(v in n.lower()for v in ['barcode','barhopper'])]
    check('Bundled barcode model assets are packaged',bool(model_files),model_files)
    check('CameraX implementation is packaged',any(b'Landroidx/camera/lifecycle/ProcessCameraProvider;'in raw for raw in combined),'Existing CameraX 1.4.1 reused')
    check('No new native library is silently removed',set(libs).issuperset(n for n in old.namelist()if n.startswith('lib/')and n.endswith('.so')),{'total':len(libs),'new':sorted(set(libs)-set(old.namelist()))})
check('Artifact below GitHub single-file size limit',apk.stat().st_size<100*1024*1024,apk.stat().st_size)
result={'artifact':str(apk.relative_to(repo)),'sha256':sha(apk),'bytes':apk.stat().st_size,'signer_sha256':fingerprint(signature),'previous_preview_sha256':sha(previous),'package':package,'version_code':33,'version_name':'5.2.0-preview.1-debug','minimum_sdk':29,'target_sdk':35,'architectures':['arm64-v8a'],'native_elf_page_alignment_bytes':page_alignments,'environment':'Linux cloud SDK/ZIP inspection; no Android device or emulator','physical_device_tests_executed':0,'checks':checks,'passed':sum(c['status']=='PASS'for c in checks),'failed':sum(c['status']=='FAIL'for c in checks)}
(out/'artifact-verification.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps({k:v for k,v in result.items()if k not in {'checks','native_elf_page_alignment_bytes'}},indent=2))
if result['failed']:
    print(json.dumps([c for c in checks if c['status']=='FAIL'],indent=2));raise SystemExit(1)
