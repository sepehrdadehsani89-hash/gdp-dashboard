import hashlib
import json
import os
from pathlib import Path
import re
import struct
import subprocess
import zipfile

repo = Path('/workspace/gdp-dashboard')
out = Path('/workspace/work/jarvis-brain-manager')
sdk = Path(os.environ['ANDROID_HOME']) / 'build-tools/35.0.0'
apk = repo / 'app/build/outputs/apk/debug/app-debug.apk'
original = repo / 'downloads/polish-2.11.8/Jarvis-2.11.8-arm64-update.apk'
previous = repo / 'downloads/voice-intelligence-2.11.9-debug/Jarvis-2.11.9-voice.1-debug-arm64.apk'
expected = 'com.jarvis.next.phoenix'

def command(tool, args, filename):
    result = subprocess.run([str(sdk/tool), *map(str, args)], capture_output=True, text=True)
    (out/filename).write_text(result.stdout + result.stderr)
    if result.returncode:
        raise RuntimeError(f'{tool} failed ({result.returncode}): {result.stdout}{result.stderr}')
    return result.stdout

badging = command('aapt2', ['dump', 'badging', apk], 'apk-badging.txt')
manifest = command('aapt2', ['dump', 'xmltree', apk, '--file', 'AndroidManifest.xml'], 'apk-manifest.txt')
signature = command('apksigner', ['verify', '--verbose', '--print-certs', apk], 'apk-signature.txt')
alignment = command('zipalign', ['-c', '-v', '4', apk], 'apk-alignment.txt')
original_manifest = command('aapt2', ['dump', 'xmltree', original, '--file', 'AndroidManifest.xml'], 'original-manifest.txt')
original_signature = command('apksigner', ['verify', '--verbose', '--print-certs', original], 'original-signature.txt')
previous_signature = command('apksigner', ['verify', '--verbose', '--print-certs', previous], 'previous-signature.txt')

def attrs(text, name):
    return set(re.findall(r'android:' + re.escape(name) + r'\([^)]*\)="([^"]+)"', text))

def declarations(text):
    return set(re.findall(r'E: permission[^\n]*\n\s+A: [^\n]*android:name\([^)]*\)="([^"]+)"', text))

def digest(path):
    with path.open('rb') as source:
        return hashlib.file_digest(source, 'sha256').hexdigest()

checks = []
def check(name, condition, evidence):
    checks.append({'name': name, 'status': 'PASS' if condition else 'FAIL', 'evidence': evidence})

check('Distinct preview application ID', f"package: name='{expected}'" in badging, expected)
check('Version name', "versionName='2.11.9-brain.1-debug'" in badging, '2.11.9-brain.1-debug')
check('Version code', "versionCode='29'" in badging, 29)
check('SDK minimum and target', bool(re.search(r"^(?:minSdkVersion|sdkVersion):'29'$", badging, re.MULTILINE)) and "targetSdkVersion:'35'" in badging, 'min 29 / target 35')
check('Distinct app labels in English Spanish Persian', all(f"application-label{suffix}:'Jarvis Phoenix Test'" in badging for suffix in ['', '-es', '-fa']), 'Jarvis Phoenix Test')
check('Existing launch activity retained', "launchable-activity: name='com.jarvis.assistant.MainActivity'" in badging, 'com.jarvis.assistant.MainActivity')
providers = attrs(manifest, 'authorities')
original_providers = attrs(original_manifest, 'authorities')
check('No provider collision with release', bool(providers) and bool(original_providers) and providers.isdisjoint(original_providers) and all(x.startswith(expected+'.') for x in providers), sorted(providers))
permissions = declarations(manifest)
original_permissions = declarations(original_manifest)
check('No declared permission collision with release', bool(permissions) and bool(original_permissions) and permissions.isdisjoint(original_permissions) and all(x.startswith(expected+'.') for x in permissions), sorted(permissions))
check('No shared Android UID', 'sharedUserId' not in manifest, 'No sharedUserId declaration')
check('Isolated screen capture task affinity', attrs(manifest, 'taskAffinity') == {expected+'.screen'}, sorted(attrs(manifest, 'taskAffinity')))
schemes = attrs(manifest, 'scheme')
check('Isolated Spotify callback', expected in schemes and 'com.jarvis.next' not in schemes, sorted(schemes))
check('Wallpaper service retained', 'com.jarvis.assistant.sentinel.JarvisWallpaperService' in manifest, 'Existing service class present')
check('Signature v2 verified', 'Verified using v2 scheme (APK Signature Scheme v2): true' in signature, 'apksigner exit 0')
cert = re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]+)', signature).group(1)
original_cert = re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]+)', original_signature).group(1)
previous_cert = re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]+)', previous_signature).group(1)
check('Same debug certificate as previous Phoenix preview', cert == previous_cert, cert)
check('Release certificate difference recorded honestly', cert != original_cert, {'preview': cert, 'retained_release': original_cert})
check('APK alignment verified', alignment.rstrip().endswith(('Verification successful', 'Verification succesful')), 'zipalign exit 0')
with zipfile.ZipFile(apk) as current, zipfile.ZipFile(previous) as old:
    check('ZIP entries pass CRC validation', current.testzip() is None, 'zipfile.testzip')
    libs = [n for n in current.namelist() if n.startswith('lib/') and n.endswith('.so')]
    elf = {}
    for name in libs:
        with current.open(name) as source:
            header = source.read(20)
        elf[name] = struct.unpack('<H', header[18:20])[0] if header[:4] == b'\x7fELF' else None
    check('ARM64 native architecture', bool(elf) and all(v == 183 for v in elf.values()) and all(n.startswith('lib/arm64-v8a/') for n in elf), elf)
    preserved = {n for n in old.namelist() if not n.endswith('/') and (n.startswith('assets/') or n.startswith('lib/'))}
    retained = any(n.startswith('assets/') for n in preserved) and any(n.startswith('lib/') for n in preserved) and preserved.issubset(current.namelist()) and all(hashlib.sha256(old.read(n)).digest() == hashlib.sha256(current.read(n)).digest() for n in preserved)
    check('Previous Phoenix native libraries and assets preserved byte for byte', retained, {'files_compared': len(preserved)})
check('Artifact within GitHub file size limit', apk.stat().st_size < 100*1024*1024, apk.stat().st_size)

result = {'artifact': str(apk.relative_to(repo)), 'sha256': digest(apk), 'bytes': apk.stat().st_size,
          'retained_release_sha256': digest(original), 'previous_phoenix_sha256': digest(previous),
          'environment': 'Linux cloud Android SDK artifact inspection; no Android device or emulator',
          'physical_device_tests_executed': 0, 'checks': checks,
          'passed': sum(c['status']=='PASS' for c in checks), 'failed': sum(c['status']=='FAIL' for c in checks)}
(out/'artifact-verification.json').write_text(json.dumps(result, indent=2)+'\n')
print(json.dumps({k:v for k,v in result.items() if k != 'checks'}, indent=2))
if result['failed']:
    print(json.dumps([c for c in checks if c['status']=='FAIL'], indent=2))
    raise SystemExit(1)
