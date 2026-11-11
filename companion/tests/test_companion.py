import base64,copy,hashlib,hmac,json,ssl,tempfile,threading,time,unittest,urllib.request,uuid
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from jarvis_companion import *

class FakeAdapter:
    def __init__(self):self.calls=0;self.failure=None;self.wait=False;self.entered=threading.Event()
    def execute(self,action,params,cancel):
        self.calls+=1;self.entered.set()
        if self.failure:raise Failure(self.failure)
        if self.wait:
            while not cancel.wait(.01):pass
            raise Failure('CANCELLED')
        url=search_url(params['query']) if action=='desktop.search_web' else 'about:blank'
        return {'kind':'CHROME_DOCUMENT_LOADED','url_sha256':digest(url.encode()),'document_ready':'complete','http_status':200 if action=='desktop.search_web' else None}
    def close(self):pass

class CompanionTest(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.now=1700000000000;self.adapter=FakeAdapter();self.app=Companion(Path(self.tmp.name),self.adapter,lambda:self.now)
        self.client=str(uuid.uuid4());self.nonce=str(uuid.uuid4());self.invite=self.app.new_invitation('https://192.168.1.8:8765','a'*64)
    def tearDown(self):self.app.close();self.tmp.cleanup()
    def pair(self,client=None):
        client=client or self.client;i=self.invite
        request={'pairing_id':i['pairing_id'],'client_id':client,'nonce':self.nonce,'proof':hmac.new(i['code'].encode(),(i['pairing_id']+'\n'+client+'\n'+self.nonce).encode(),hashlib.sha256).hexdigest()}
        pending=self.app.request_pair(request);self.app.approve(pending['ticket']);return self.app.finish_pair({'ticket':pending['ticket'],'client_id':client,'nonce':self.nonce,'proof':request['proof']})
    def command(self,pair,query='ChatGPT'):
        command_id=str(uuid.uuid4());return {'schema_version':1,'command_id':command_id,'requester_user_id':pair['client_id'],'target_device_id':self.app.device_id,'session_id':pair['session_id'],'issued_at':self.now,'expires_at':self.now+30000,'action_type':'desktop.search_web','normalized_parameters':{'browser':'chrome','query':query},'required_capabilities':['desktop.search_web'],'risk_level':1,'idempotency_key':command_id,'correlation_id':str(uuid.uuid4()),'nonce':str(uuid.uuid4()),'confirmed':True}
    def receipt(self,envelope,token):
        raw=base64.urlsafe_b64decode(envelope['receipt']+'==');expected=hmac.new(hashlib.sha256(token.encode()).digest(),raw,hashlib.sha256).hexdigest();self.assertTrue(hmac.compare_digest(expected,envelope['signature']));return json.loads(raw)
    def assertFailure(self,code,fn):
        with self.assertRaises(Failure) as c:fn()
        self.assertEqual(code,c.exception.code)
    def test_pairing_requires_desktop_approval(self):
        i=self.invite;proof=hmac.new(i['code'].encode(),(i['pairing_id']+'\n'+self.client+'\n'+self.nonce).encode(),hashlib.sha256).hexdigest();p=self.app.request_pair(dict(pairing_id=i['pairing_id'],client_id=self.client,nonce=self.nonce,proof=proof));self.assertFailure('DESKTOP_APPROVAL_REQUIRED',lambda:self.app.finish_pair(dict(ticket=p['ticket'],client_id=self.client,nonce=self.nonce,proof=proof)))
    def test_invalid_proof_never_pairs(self):self.assertFailure('PAIRING_PROOF_INVALID',lambda:self.app.request_pair(dict(pairing_id=self.invite['pairing_id'],client_id=self.client,nonce=self.nonce,proof='0'*64)))
    def test_expired_invitation_never_pairs(self):self.now+=120001;self.assertFailure('PAIRING_EXPIRED',self.pair)
    def test_invitation_is_one_time(self):self.pair();self.assertFailure('PAIRING_EXPIRED',self.pair)
    def test_pairing_rate_limit(self):
        bad=dict(pairing_id=self.invite['pairing_id'],client_id=self.client,nonce=self.nonce,proof='0'*64)
        for _ in range(5):self.assertFailure('PAIRING_PROOF_INVALID',lambda:self.app.request_pair(bad))
        self.assertFailure('RATE_LIMITED',lambda:self.app.request_pair(bad))
    def test_command_receipt_bound_to_exact_bytes_and_identity(self):
        p=self.pair();r=self.command(p);body=canonical(r);e=self.app.command(p['token'],body);v=self.receipt(e,p['token']);self.assertEqual('COMPLETED',v['state']);self.assertEqual(digest(body),v['request_sha256']);self.assertEqual(p['client_id'],v['requester_user_id']);self.assertEqual(self.app.device_id,v['target_device_id']);self.assertEqual(1,self.adapter.calls)
    def test_idempotence_does_not_launch_twice(self):
        p=self.pair();r=canonical(self.command(p));a=self.app.command(p['token'],r);b=self.app.command(p['token'],r);self.assertEqual(a,b);self.assertEqual(1,self.adapter.calls)
    def test_changed_body_cannot_reuse_command_id(self):
        p=self.pair();r=self.command(p);self.app.command(p['token'],canonical(r));r['normalized_parameters']['query']='different';self.assertFailure('IDEMPOTENCY_CONFLICT',lambda:self.app.command(p['token'],canonical(r)));self.assertEqual(1,self.adapter.calls)
    def test_nonce_replay_rejected_for_new_command(self):
        p=self.pair();r=self.command(p);self.app.command(p['token'],canonical(r));s=self.command(p);s['nonce']=r['nonce'];self.assertFailure('REPLAY_REJECTED',lambda:self.app.command(p['token'],canonical(s)))
    def test_expired_command_never_executes(self):
        p=self.pair();r=self.command(p);self.now+=30001;self.assertFailure('COMMAND_EXPIRED',lambda:self.app.command(p['token'],canonical(r)));self.assertEqual(0,self.adapter.calls)
    def test_unconfirmed_command_never_executes(self):
        p=self.pair();r=self.command(p);r['confirmed']=False;self.assertFailure('CONFIRMATION_REQUIRED',lambda:self.app.command(p['token'],canonical(r)));self.assertEqual(0,self.adapter.calls)
    def test_wrong_device_or_user_never_executes(self):
        p=self.pair()
        for field in ('target_device_id','requester_user_id'):
            r=self.command(p);r[field]=str(uuid.uuid4());self.assertFailure('WRONG_DEVICE',lambda:self.app.command(p['token'],canonical(r)))
        self.assertEqual(0,self.adapter.calls)
    def test_wrong_session_never_executes(self):
        p=self.pair();r=self.command(p);r['session_id']=str(uuid.uuid4());self.assertFailure('WRONG_SESSION',lambda:self.app.command(p['token'],canonical(r)))
    def test_shell_actions_are_not_supported(self):
        p=self.pair();r=self.command(p);r['action_type']='desktop.shell';r['required_capabilities']=['desktop.shell'];r['normalized_parameters']={'command':'echo forbidden'};self.assertFailure('UNSUPPORTED_ACTION',lambda:self.app.command(p['token'],canonical(r)));self.assertEqual(0,self.adapter.calls)
    def test_arbitrary_app_or_url_cannot_be_injected(self):
        p=self.pair()
        r=self.command(p);r['action_type']='desktop.open_app';r['required_capabilities']=['desktop.open_app'];r['normalized_parameters']={'app':'powershell'};self.assertFailure('UNSUPPORTED_APP',lambda:self.app.command(p['token'],canonical(r)))
        r=self.command(p);r['normalized_parameters']['url']='file:///private';self.assertFailure('INVALID_REQUEST',lambda:self.app.command(p['token'],canonical(r)));self.assertEqual(0,self.adapter.calls)
    def test_search_is_urlencoded_data_not_execution(self):self.assertEqual('https://www.google.com/search?q=%24%28danger%29%3B%20%22%2A',search_url('$(danger); "*'))
    def test_control_characters_and_large_queries_rejected(self):
        p=self.pair()
        for q in ('a\nb','a'*301,''):
            r=self.command(p,q);self.assertFailure('INVALID_SEARCH',lambda:self.app.command(p['token'],canonical(r)))
    def test_revoke_blocks_all_future_requests(self):
        p=self.pair();self.app.revoke(p['token']);self.assertFailure('UNAUTHORIZED',lambda:self.app.command(p['token'],canonical(self.command(p))))
    def test_receipt_access_is_client_scoped(self):
        p=self.pair();r=self.command(p);self.app.command(p['token'],canonical(r));self.invite=self.app.new_invitation('https://192.168.1.8:8765','a'*64);q=self.pair(str(uuid.uuid4()));self.assertFailure('RECEIPT_NOT_FOUND',lambda:self.app.receipt(q['token'],r['command_id']))
    def test_missing_chrome_is_truthful_failure(self):
        p=self.pair();self.adapter.failure='CHROME_NOT_INSTALLED';v=self.receipt(self.app.command(p['token'],canonical(self.command(p))),p['token']);self.assertEqual('FAILED',v['state']);self.assertIsNone(v['verified_postcondition']);self.assertEqual('CHROME_NOT_INSTALLED',v['error_category'])
    def test_completion_requires_postcondition(self):
        p=self.pair();r=self.command(p);row=self.app.authenticate(p['token']);self.assertFailure('POSTCONDITION_UNVERIFIED',lambda:self.app.signed(row,r,'a'*64,'COMPLETED'))
    def test_request_throttling(self):
        p=self.pair()
        for _ in range(10):self.app.command(p['token'],canonical(self.command(p)))
        self.assertFailure('RATE_LIMITED',lambda:self.app.command(p['token'],canonical(self.command(p))))
    def test_local_only_addresses(self):
        for host in ('127.0.0.1','192.168.1.2','10.0.0.1','172.16.0.5'):self.assertTrue(local_address(host))
        for host in ('8.8.8.8','0.0.0.0','example.com','169.254.169.254','::1'):self.assertFalse(local_address(host))
    def test_secrets_and_queries_are_not_stored_in_audit(self):
        p=self.pair();self.app.command(p['token'],canonical(self.command(p,'a-private-query-unique')));raw=(Path(self.tmp.name)/'companion.sqlite3').read_bytes();self.assertNotIn(p['token'].encode(),raw);self.assertNotIn(b'a-private-query-unique',raw)
    def test_pending_cancellation_never_reports_success(self):
        p=self.pair();r=self.command(p);self.adapter.wait=True;result=[];worker=threading.Thread(target=lambda:result.append(self.app.command(p['token'],canonical(r))));worker.start();self.assertTrue(self.adapter.entered.wait(2));self.app.cancel(p['token'],r['command_id']);worker.join(3);self.assertFalse(worker.is_alive());self.assertEqual('CANCELLED',self.receipt(result[0],p['token'])['state'])
    def test_busy_profile_is_preserved(self):
        path=Path(self.tmp.name);profile=path/'chrome-profile';profile.mkdir();f=profile/'DevToolsActivePort';f.write_text('keep');adapter=ChromeAdapter(path);adapter.close();self.assertEqual('keep',f.read_text())

class IdentityAndUrlTest(unittest.TestCase):
    def test_missing_tls_identity_is_not_silently_replaced(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);cert,key,pin=tls_identity(root,'127.0.0.1');cert.unlink();key.unlink()
            with self.assertRaises(Failure) as e:tls_identity(root,'127.0.0.1')
            self.assertEqual('TLS_IDENTITY_MISSING',e.exception.code)
    def test_google_tracking_redirects_match_only_the_same_requested_search(self):
        expected=search_url('ChatGPT')
        self.assertTrue(same_search_page('https://www.google.com/search?q=ChatGPT&sourceid=chrome',expected))
        for actual in ('https://evil.example/search?q=ChatGPT','https://www.google.com/search?q=different','https://www.google.com/search?q=ChatGPT&q=other','http://www.google.com/search?q=ChatGPT','https://user@www.google.com/search?q=ChatGPT','https://www.google.com/accounts?q=ChatGPT'):
            self.assertFalse(same_search_page(actual,expected),actual)

class ActualTlsTest(unittest.TestCase):
    def test_https_pairing_action_receipt_and_revocation(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);cert,key,pin=tls_identity(root,'127.0.0.1');app=Companion(root,FakeAdapter());server=Server(('127.0.0.1',0),Handler);server.app=app
            context=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);context.load_cert_chain(cert,key);server.socket=context.wrap_socket(server.socket,server_side=True,do_handshake_on_connect=False)
            thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start();url=f'https://127.0.0.1:{server.server_port}';opener=urllib.request.build_opener(urllib.request.ProxyHandler({}),urllib.request.HTTPSHandler(context=ssl.create_default_context(cafile=cert)))
            def post(path,data,token=None):
                req=urllib.request.Request(url+path,data=canonical(data),headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})});return json.loads(opener.open(req,timeout=5).read())
            try:
                health=json.loads(opener.open(url+'/v1/health').read());self.assertEqual('jarvis-companion',health['service'])
                i=app.new_invitation(url,pin);client=str(uuid.uuid4());nonce=str(uuid.uuid4());proof=hmac.new(i['code'].encode(),(i['pairing_id']+'\n'+client+'\n'+nonce).encode(),hashlib.sha256).hexdigest();p=post('/v1/pair/request',dict(pairing_id=i['pairing_id'],client_id=client,nonce=nonce,proof=proof));app.approve(p['ticket']);paired=post('/v1/pair/complete',dict(ticket=p['ticket'],client_id=client,nonce=nonce,proof=proof));self.assertEqual(app.device_id,paired['device_id']);self.assertTrue(post('/v1/revoke',{},paired['token'])['revoked'])
            finally:server.shutdown();server.server_close();app.close()
if __name__=='__main__':unittest.main()
