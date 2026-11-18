"""Actual protocol/TLS fixtures; no physical Redmi or native Windows claim."""
import base64,hashlib,hmac,json,ssl,tempfile,threading,unittest,urllib.request,urllib.error,uuid
from pathlib import Path
from jarvis_companion import Companion,Failure,Server,Handler,tls_identity,canonical
from test_companion import FakeAdapter

class PairingRecoveryTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.now=1700000000000
        self.app=Companion(Path(self.temp.name),FakeAdapter(),lambda:self.now)
        self.invite=self.app.new_invitation('https://192.168.1.8:8765','a'*64)
        self.request=self.make_request()
    def tearDown(self):self.app.close();self.temp.cleanup()
    def make_request(self):
        i=self.invite;client=str(uuid.uuid4());nonce=str(uuid.uuid4())
        return {'pairing_id':i['pairing_id'],'client_id':client,'nonce':nonce,'proof':hmac.new(i['code'].encode(),(i['pairing_id']+'\n'+client+'\n'+nonce).encode(),hashlib.sha256).hexdigest()}
    def request_ticket(self):
        c=self.app.request_pair(self.request)
        self.complete={'ticket':c['ticket'],**{k:self.request[k]for k in ('client_id','nonce','proof')}}
        return c
    def fail(self,code,fn):
        with self.assertRaises(Failure)as e:fn()
        self.assertEqual(code,e.exception.code)
    def test_empty_pending_has_a_useful_message(self):self.assertIn('No pending pairing requests.',self.app.pending_lines()[0])
    def test_request_appears_as_a_real_pending_ticket(self):
        c=self.request_ticket();line=self.app.pending_lines()[0];self.assertIn(c['ticket'],line);self.assertIn(c['compare_code'],line);self.assertIn('age 0 seconds',line)
    def test_pending_output_excludes_invitation_secret(self):self.request_ticket();self.assertNotIn(self.invite['code'],'\n'.join(self.app.pending_lines()))
    def test_pending_output_excludes_the_full_phone_identity(self):self.request_ticket();self.assertNotIn(self.request['client_id'],'\n'.join(self.app.pending_lines()))
    def test_identical_request_reconciles_lost_response(self):c=self.request_ticket();self.assertEqual(c,self.app.request_pair(self.request));self.assertEqual(1,len(self.app.pending))
    def test_different_nonce_cannot_reuse_consumed_invitation(self):self.request_ticket();self.fail('PAIRING_EXPIRED',lambda:self.app.request_pair(self.make_request()))
    def test_different_proof_cannot_retrieve_ticket(self):self.request_ticket();bad={**self.request,'proof':'0'*64};self.fail('PAIRING_EXPIRED',lambda:self.app.request_pair(bad))
    def test_expired_retry_is_rejected(self):self.request_ticket();self.now+=120000;self.fail('PAIRING_EXPIRED',lambda:self.app.request_pair(self.request))
    def test_expired_pending_is_not_listed_as_active(self):self.request_ticket();self.now+=120000;self.assertIn('No pending',self.app.pending_lines()[0])
    def test_pending_status_is_not_authorization(self):self.request_ticket();self.assertEqual('PENDING',self.app.pairing_status(self.complete)['state']);self.fail('DESKTOP_APPROVAL_REQUIRED',lambda:self.app.finish_pair(self.complete))
    def test_status_is_bound_to_the_same_phone(self):self.request_ticket();self.fail('PAIRING_PROOF_INVALID',lambda:self.app.pairing_status({**self.complete,'client_id':str(uuid.uuid4())}))
    def test_status_is_bound_to_the_same_nonce(self):self.request_ticket();self.fail('PAIRING_PROOF_INVALID',lambda:self.app.pairing_status({**self.complete,'nonce':str(uuid.uuid4())}))
    def test_status_requires_the_possession_proof(self):self.request_ticket();self.fail('PAIRING_PROOF_INVALID',lambda:self.app.pairing_status({**self.complete,'proof':'0'*64}))
    def test_operator_approval_changes_real_status(self):c=self.request_ticket();self.app.approve(c['ticket']);self.assertEqual('APPROVED',self.app.pairing_status(self.complete)['state'])
    def test_url_is_not_an_approval_ticket(self):self.fail('INVALID_TICKET',lambda:self.app.approve(self.invite['url']))
    def test_unknown_ticket_is_explained(self):self.fail('TICKET_NOT_FOUND',lambda:self.app.approve(str(uuid.uuid4())))
    def test_already_approved_ticket_is_explained(self):c=self.request_ticket();self.app.approve(c['ticket']);self.fail('ALREADY_APPROVED',lambda:self.app.approve(c['ticket']))
    def test_expired_ticket_cannot_be_approved(self):c=self.request_ticket();self.now+=120000;self.fail('PAIRING_EXPIRED',lambda:self.app.approve(c['ticket']))
    def test_denial_cannot_create_a_session(self):c=self.request_ticket();self.app.deny(c['ticket']);self.assertEqual('DENIED',self.app.pairing_status(self.complete)['state']);self.fail('PAIRING_DENIED',lambda:self.app.finish_pair(self.complete));self.assertEqual(0,self.app.db.execute('SELECT COUNT(*) FROM pairs').fetchone()[0])
    def test_denied_request_cannot_retrieve_its_ticket_again(self):c=self.request_ticket();self.app.deny(c['ticket']);self.fail('PAIRING_DENIED',lambda:self.app.request_pair(self.request))
    def test_complete_response_loss_reconciles_the_same_session(self):c=self.request_ticket();self.app.approve(c['ticket']);first=self.app.finish_pair(self.complete);second=self.app.finish_pair(self.complete);self.assertEqual(first,second);self.assertEqual(1,self.app.db.execute('SELECT COUNT(*) FROM pairs').fetchone()[0])
    def test_finished_ticket_is_not_an_outstanding_request(self):c=self.request_ticket();self.app.approve(c['ticket']);self.app.finish_pair(self.complete);self.assertIn('No pending',self.app.pending_lines()[0])
    def test_finished_ticket_cannot_be_approved_again(self):c=self.request_ticket();self.app.approve(c['ticket']);self.app.finish_pair(self.complete);self.fail('PAIRING_ALREADY_COMPLETED',lambda:self.app.approve(c['ticket']))
    def test_revoked_session_is_not_resurrected_by_retry(self):c=self.request_ticket();self.app.approve(c['ticket']);pair=self.app.finish_pair(self.complete);self.app.revoke(pair['token']);self.fail('UNAUTHORIZED',lambda:self.app.finish_pair(self.complete))
    def test_response_reconciliation_expires_with_original_challenge(self):c=self.request_ticket();self.app.approve(c['ticket']);self.app.finish_pair(self.complete);self.now+=120000;self.fail('PAIRING_EXPIRED',lambda:self.app.finish_pair(self.complete))
    def test_revoke_all_invalidates_pending_approvals(self):self.request_ticket();self.app.revoke_all();self.fail('PAIRING_DENIED',lambda:self.app.finish_pair(self.complete))
    def test_revoke_all_invalidates_the_old_invitation(self):self.app.revoke_all();self.fail('PAIRING_EXPIRED',lambda:self.app.request_pair(self.request))
    def test_no_plaintext_token_is_persisted(self):c=self.request_ticket();self.app.approve(c['ticket']);p=self.app.finish_pair(self.complete);self.assertNotIn(p['token'].encode(),(Path(self.temp.name)/'companion.sqlite3').read_bytes())
    def test_restarting_preserves_computer_and_saved_session_identity(self):c=self.request_ticket();self.app.approve(c['ticket']);p=self.app.finish_pair(self.complete);identity=self.app.device_id;self.app.close();self.app=Companion(Path(self.temp.name),FakeAdapter(),lambda:self.now);self.assertEqual(identity,self.app.device_id);self.assertEqual(p['client_id'],self.app.authenticate(p['token'])[0]);self.assertIn('No pending',self.app.pending_lines()[0])

class PairingHttpsTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();root=Path(self.temp.name)
        self.app=Companion(root,FakeAdapter());cert,key,self.pin=tls_identity(root,'127.0.0.1')
        self.server=Server(('127.0.0.1',0),Handler);self.server.app=self.app
        ctx=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);ctx.minimum_version=ssl.TLSVersion.TLSv1_2;ctx.load_cert_chain(cert,key)
        self.server.socket=ctx.wrap_socket(self.server.socket,server_side=True,do_handshake_on_connect=False)
        self.worker=threading.Thread(target=self.server.serve_forever,daemon=True);self.worker.start()
        self.url='https://127.0.0.1:'+str(self.server.server_port)
        self.context=ssl.create_default_context(cafile=str(cert));self.opener=urllib.request.build_opener(urllib.request.ProxyHandler({}),urllib.request.HTTPSHandler(context=self.context))
        self.invite=self.app.new_invitation(self.url,self.pin)
        client=str(uuid.uuid4());nonce=str(uuid.uuid4());i=self.invite
        self.request={'pairing_id':i['pairing_id'],'client_id':client,'nonce':nonce,'proof':hmac.new(i['code'].encode(),(i['pairing_id']+'\n'+client+'\n'+nonce).encode(),hashlib.sha256).hexdigest()}
    def tearDown(self):self.server.shutdown();self.server.server_close();self.worker.join(2);self.app.close();self.temp.cleanup()
    def post(self,path,value,token=None):
        headers={'Content-Type':'application/json'}
        if token:headers['Authorization']='Bearer '+token
        req=urllib.request.Request(self.url+path,data=canonical(value),headers=headers)
        try:
            with self.opener.open(req,timeout=4)as r:return r.status,json.loads(r.read())
        except urllib.error.HTTPError as e:return e.code,json.loads(e.read())
    def ticket(self):
        status,c=self.post('/v1/pair/request',self.request);self.assertEqual(200,status)
        return c,{'ticket':c['ticket'],**{k:self.request[k]for k in ('client_id','nonce','proof')}}
    def test_real_https_request_creates_operator_ticket(self):c,_=self.ticket();self.assertIn(c['ticket'],self.app.pending_lines()[0])
    def test_real_https_request_retry_returns_identical_challenge(self):c,_=self.ticket();status,d=self.post('/v1/pair/request',self.request);self.assertEqual((200,c),(status,d))
    def test_real_https_status_needs_the_correct_proof(self):_,request=self.ticket();status,j=self.post('/v1/pair/status',{**request,'proof':'0'*64});self.assertEqual(400,status);self.assertEqual('PAIRING_PROOF_INVALID',j['error'])
    def test_real_https_never_pairs_before_operator_approval(self):_,request=self.ticket();status,j=self.post('/v1/pair/complete',request);self.assertEqual(409,status);self.assertEqual('DESKTOP_APPROVAL_REQUIRED',j['error'])
    def test_real_https_approval_finish_and_signed_capabilities(self):
        c,request=self.ticket();self.app.approve(c['ticket']);status,p=self.post('/v1/pair/complete',request);self.assertEqual(200,status)
        nonce=str(uuid.uuid4());status,j=self.post('/v1/capabilities',{'nonce':nonce},p['token']);self.assertEqual(200,status)
        raw=base64.urlsafe_b64decode(j['manifest']+'==');key=hashlib.sha256(p['token'].encode()).digest();self.assertTrue(hmac.compare_digest(j['signature'],hmac.new(key,raw,hashlib.sha256).hexdigest()));self.assertEqual(nonce,json.loads(raw)['request_nonce'])
    def test_real_https_denial_is_visible(self):c,request=self.ticket();self.app.deny(c['ticket']);status,j=self.post('/v1/pair/status',request);self.assertEqual(200,status);self.assertEqual('DENIED',j['state'])
    def test_real_https_wrong_token_cannot_read_capabilities(self):status,j=self.post('/v1/capabilities',{'nonce':str(uuid.uuid4())},'wrong');self.assertEqual(401,status);self.assertEqual('UNAUTHORIZED',j['error'])
    def test_real_https_untrusted_certificate_is_rejected(self):
        opener=urllib.request.build_opener(urllib.request.ProxyHandler({}),urllib.request.HTTPSHandler(context=ssl.create_default_context()))
        with self.assertRaises(urllib.error.URLError)as e:opener.open(self.url+'/v1/health',timeout=4)
        self.assertIsInstance(e.exception.reason,ssl.SSLCertVerificationError)
