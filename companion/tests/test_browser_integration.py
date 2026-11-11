"""Actual Chromium + actual HTTPS companion + local controlled search fixture.
This does not claim a real Google search, Windows GUI, Android microphone or phone TTS test.
"""
import base64,hashlib,hmac,http.server,json,ssl,sys,tempfile,threading,unittest,urllib.request,uuid
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from jarvis_companion import *

class Fixture(http.server.BaseHTTPRequestHandler):
    def log_message(self,*args):pass
    def do_GET(self):
        data=b'<!doctype html><html><head><title>Jarvis controlled search fixture</title></head><body>Actual local browser test. Not Google.</body></html>'
        self.send_response(404 if 'Missing' in self.path else 200);self.send_header('Content-Type','text/html');self.send_header('Content-Length',str(len(data)));self.end_headers();self.wfile.write(data)

@unittest.skipUnless(Path("/usr/bin/chromium").is_file(),"Linux Chromium fixture only; native platform execution is not implied")
class ActualBrowserTest(unittest.TestCase):
    def test_https_command_reaches_real_browser_and_returns_verified_receipt(self):
        fixture=http.server.ThreadingHTTPServer(('127.0.0.1',0),Fixture);threading.Thread(target=fixture.serve_forever,daemon=True).start()
        with tempfile.TemporaryDirectory() as tmp:
            state=Path(tmp);url_builder=lambda q:f'http://127.0.0.1:{fixture.server_port}/search?q='+quote(q,safe='')
            adapter=ChromeAdapter(state,'/usr/bin/chromium',('--headless=new','--no-sandbox','--disable-dev-shm-usage','--disable-background-networking','--host-resolver-rules=MAP * ~NOTFOUND, EXCLUDE localhost, EXCLUDE 127.0.0.1'),url_builder)
            app=Companion(state,adapter);cert,key,pin=tls_identity(state,'127.0.0.1');server=Server(('127.0.0.1',0),Handler);server.app=app
            tls=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);tls.load_cert_chain(cert,key);server.socket=tls.wrap_socket(server.socket,server_side=True,do_handshake_on_connect=False);threading.Thread(target=server.serve_forever,daemon=True).start()
            endpoint=f'https://127.0.0.1:{server.server_port}';opener=urllib.request.build_opener(urllib.request.ProxyHandler({}),urllib.request.HTTPSHandler(context=ssl.create_default_context(cafile=cert)))
            def post(path,data,token=None):
                r=urllib.request.Request(endpoint+path,data=canonical(data),headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})});return json.loads(opener.open(r,timeout=25).read())
            try:
                i=app.new_invitation(endpoint,pin);client=str(uuid.uuid4());nonce=str(uuid.uuid4());proof=hmac.new(i['code'].encode(),(i['pairing_id']+'\n'+client+'\n'+nonce).encode(),hashlib.sha256).hexdigest()
                p=post('/v1/pair/request',dict(pairing_id=i['pairing_id'],client_id=client,nonce=nonce,proof=proof));app.approve(p['ticket']);pair=post('/v1/pair/complete',dict(ticket=p['ticket'],client_id=client,nonce=nonce,proof=proof))
                def request(q):
                    cid=str(uuid.uuid4());return dict(schema_version=1,command_id=cid,requester_user_id=client,target_device_id=app.device_id,session_id=pair['session_id'],issued_at=app.now(),expires_at=app.now()+30000,action_type='desktop.search_web',normalized_parameters={'browser':'chrome','query':q},required_capabilities=['desktop.search_web'],risk_level=1,idempotency_key=cid,correlation_id=str(uuid.uuid4()),nonce=str(uuid.uuid4()),confirmed=True)
                command=request('ChatGPT');result=post('/v1/commands',command,pair['token']);raw=base64.urlsafe_b64decode(result['receipt']+'==')
                self.assertTrue(hmac.compare_digest(result['signature'],hmac.new(hashlib.sha256(pair['token'].encode()).digest(),raw,hashlib.sha256).hexdigest()))
                receipt=json.loads(raw);self.assertEqual('COMPLETED',receipt['state'],receipt.get('error_category'));self.assertEqual(digest(url_builder('ChatGPT').encode()),receipt['verified_postcondition']['url_sha256']);self.assertEqual(200,receipt['verified_postcondition']['http_status'])
                duplicate=post('/v1/commands',command,pair['token']);self.assertEqual(result,duplicate)
                failed=post('/v1/commands',request('Missing'),pair['token']);v=json.loads(base64.urlsafe_b64decode(failed['receipt']+'=='));self.assertEqual('FAILED',v['state']);self.assertEqual('PAGE_HTTP_ERROR',v['error_category']);self.assertIsNone(v['verified_postcondition'])
                Path(os.environ.get('JARVIS_COMPANION_BROWSER_REPORT',str(state/'browser-result.json'))).write_text(json.dumps({'kind':'ACTUAL_CHROMIUM_PLUS_HTTPS_ON_LOCAL_FIXTURE','completed_receipt_verified':True,'duplicate_idempotent':True,'actual_404_failed_truthfully':True,'google_search_tested':False,'windows_tested':False,'android_voice_tested':False,'sandbox_disabled_only_for_cloud_headless_fixture':True},indent=2)+'\n')
            finally:server.shutdown();server.server_close();app.close();fixture.shutdown();fixture.server_close()
if __name__=='__main__':unittest.main()
