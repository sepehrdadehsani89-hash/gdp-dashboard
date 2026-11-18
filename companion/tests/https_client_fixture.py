"""Loopback-only JVM interop fixture. Operator controls use stdin, never an HTTP route.
Creates disposable test TLS keys in the caller's temporary directory, never production state.
"""
import argparse,base64,datetime,hashlib,hmac,json,ssl,sys,threading,time,uuid
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from jarvis_companion import Companion,Server,Handler,Failure,canonical,tls_identity
from test_companion import FakeAdapter

def run():
    parser=argparse.ArgumentParser();parser.add_argument('--state',type=Path,required=True);parser.add_argument('--mode',default='normal');args=parser.parse_args()
    app=Companion(args.state,FakeAdapter());cert,key,pin=tls_identity(args.state,'127.0.0.1')
    if args.mode in ('wrong-host','expired','future'):
        from cryptography import x509
        from cryptography.hazmat.primitives import hashes,serialization
        import ipaddress
        original=x509.load_pem_x509_certificate(cert.read_bytes());private=serialization.load_pem_private_key(key.read_bytes(),None)
        now=datetime.datetime.now(datetime.timezone.utc)
        start=now-datetime.timedelta(minutes=5);end=now+datetime.timedelta(days=1)
        if args.mode=='expired':start=now-datetime.timedelta(days=2);end=now-datetime.timedelta(days=1)
        if args.mode=='future':start=now+datetime.timedelta(days=1);end=now+datetime.timedelta(days=2)
        host='127.0.0.2' if args.mode=='wrong-host' else '127.0.0.1'
        testcert=x509.CertificateBuilder().subject_name(original.subject).issuer_name(original.issuer).public_key(private.public_key()).serial_number(x509.random_serial_number()).not_valid_before(start).not_valid_after(end).add_extension(x509.SubjectAlternativeName([x509.IPAddress(ipaddress.ip_address(host))]),False).sign(private,hashes.SHA256())
        cert.write_bytes(testcert.public_bytes(serialization.Encoding.PEM));pin=testcert.fingerprint(hashes.SHA256()).hex()
    counts={}
    class FixtureHandler(Handler):
        def do_GET(self):
            counts[self.path]=counts.get(self.path,0)+1;super().do_GET()
        def do_POST(self):
            counts[self.path]=counts.get(self.path,0)+1
            if args.mode=='redirect' and self.path=='/v1/pair/request':
                self.send_response(302);self.send_header('Location',url+'/redirect-target');self.send_header('Content-Type','application/json');body=b'{"error":"REQUEST_REJECTED"}';self.send_header('Content-Length',str(len(body)));self.end_headers();self.wfile.write(body);return
            if args.mode=='delay' and self.path=='/v1/pair/request':time.sleep(.3)
            super().do_POST()
        def reply(self,status,value):
            if status==200 and self.path=='/v1/pair/request':
                if args.mode=='oversize':value={'padding':'x'*33000}
                if args.mode=='bad-challenge':value={**value,'ticket':'not-a-ticket'}
                if args.mode=='invalid-json':
                    raw=b'not json';self.send_response(200);self.send_header('Content-Length',str(len(raw)));self.end_headers();self.wfile.write(raw);return
            if status==200 and 'manifest' in value:
                if args.mode=='bad-signature':value={**value,'signature':'0'*64}
                if args.mode in ('wrong-nonce','wrong-device','wrong-session','stale-manifest'):
                    data=json.loads(base64.urlsafe_b64decode(value['manifest']+'=='))
                    field={'wrong-nonce':'request_nonce','wrong-device':'device_id','wrong-session':'session_id','stale-manifest':'sampled_at'}[args.mode]
                    data[field]=0 if field=='sampled_at' else str(uuid.uuid4());raw=canonical(data);row=app.authenticate(self.token())
                    value={'manifest':base64.urlsafe_b64encode(raw).decode().rstrip('='),'signature':hmac.new(bytes.fromhex(row[2]),raw,hashlib.sha256).hexdigest()}
            super().reply(status,value)
    server=Server(('127.0.0.1',0),FixtureHandler);server.app=app;url='https://127.0.0.1:'+str(server.server_port)
    tls=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);tls.minimum_version=ssl.TLSVersion.TLSv1_2;tls.load_cert_chain(cert,key);server.socket=tls.wrap_socket(server.socket,server_side=True,do_handshake_on_connect=False)
    worker=threading.Thread(target=server.serve_forever,daemon=True);worker.start()
    def emit(value):print(json.dumps(value),flush=True)
    def invitation():return 'JARVIS-PAIR-1:'+base64.urlsafe_b64encode(canonical(app.new_invitation(url,pin))).decode().rstrip('=')
    emit({'invitation':invitation()})
    try:
        for line in sys.stdin:
            try:
                q=json.loads(line);op=q['op']
                if op=='stop':emit({'stopped':True});break
                if op=='approve':app.approve(q['ticket']);emit({'approved':True})
                elif op=='deny':app.deny(q['ticket']);emit({'denied':True})
                elif op=='invite':emit({'invitation':invitation()})
                elif op=='stats':emit({'pending':len(app.pending),'pairs':app.db.execute('SELECT COUNT(*) FROM pairs').fetchone()[0],'requests':counts})
                elif op=='revoke-all':app.revoke_all();emit({'revoked':True})
                else:emit({'error':'INVALID_OPERATOR_COMMAND'})
            except Failure as e:emit({'error':e.code})
            except Exception:emit({'error':'FIXTURE_CONTROL_FAILED'})
    finally:server.shutdown();server.server_close();worker.join(2);app.close()
if __name__=='__main__':run()
