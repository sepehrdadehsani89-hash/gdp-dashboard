"""Visible, local-LAN Jarvis companion. Only Chrome/open and Google search are allowed.
No shell endpoint, desktop audio requirement, cloud relay, account impersonation or file access.
"""
from __future__ import annotations
import argparse,base64,collections,hashlib,hmac,http.server,ipaddress,json,os,secrets,shutil,socket,sqlite3,ssl,subprocess,sys,threading,time,uuid
from pathlib import Path
from urllib.parse import quote,urlparse,parse_qs

PROTOCOL=1
CAPABILITIES=['desktop.open_app','desktop.search_web']
PRIVATE_NETS=tuple(ipaddress.ip_network(s) for s in ('10.0.0.0/8','172.16.0.0/12','192.168.0.0/16','127.0.0.0/8'))
def canonical(value):return json.dumps(value,ensure_ascii=False,separators=(',',':'),sort_keys=True).encode()
def digest(value:bytes):return hashlib.sha256(value).hexdigest()
def local_address(host):
    try:return any(ipaddress.ip_address(host) in n for n in PRIVATE_NETS)
    except ValueError:return False
def uid(value):
    try:return str(uuid.UUID(value))==value
    except (ValueError,TypeError,AttributeError):return False
class Failure(Exception):
    def __init__(self,code):self.code=code;super().__init__(code)
def require(condition,code='INVALID_REQUEST'):
    if not condition:raise Failure(code)
def search_url(query):return 'https://www.google.com/search?q='+quote(query,safe='')
def same_search_page(actual,expected):
    if actual==expected:return True
    a=urlparse(actual);e=urlparse(expected)
    return (e.scheme=='https' and e.hostname in ('www.google.com','google.com') and e.path=='/search' and
            a.scheme=='https' and a.hostname in ('www.google.com','google.com') and a.port in (None,443) and not a.username and not a.password and
            a.path=='/search' and parse_qs(a.query).get('q')==parse_qs(e.query).get('q') and len(parse_qs(a.query).get('q',[]))==1)

def validate_command(request,device_id,client_id,now):
    fields={'schema_version','command_id','requester_user_id','target_device_id','session_id','issued_at','expires_at','action_type','normalized_parameters','required_capabilities','risk_level','idempotency_key','correlation_id','nonce','confirmed'}
    require(isinstance(request,dict) and set(request)==fields)
    require(request['schema_version']==1 and type(request['schema_version']) is int,'PROTOCOL_MISMATCH')
    require(all(uid(request[k]) for k in ('command_id','requester_user_id','target_device_id','session_id','correlation_id','nonce')))
    require(request['target_device_id']==device_id and request['requester_user_id']==client_id,'WRONG_DEVICE')
    require(type(request['issued_at']) is int and type(request['expires_at']) is int)
    require(now-90000<=request['issued_at']<=now+30000 and now<request['expires_at']<=request['issued_at']+30000,'COMMAND_EXPIRED')
    require(request['confirmed'] is True,'CONFIRMATION_REQUIRED')
    action=request['action_type'];params=request['normalized_parameters']
    require(action in CAPABILITIES and request['required_capabilities']==[action] and request['risk_level']==1 and type(request['risk_level']) is int,'UNSUPPORTED_ACTION')
    require(request['idempotency_key']==request['command_id'] and isinstance(params,dict))
    if action=='desktop.open_app':require(params=={'app':'chrome'},'UNSUPPORTED_APP')
    else:
        require(set(params)=={'browser','query'} and params['browser']=='chrome')
        q=params['query'];require(isinstance(q,str) and 1<=len(q)<=300 and len(q.encode())<=1000 and all(ord(c)>=32 and ord(c)!=127 for c in q),'INVALID_SEARCH')
    return action,params

class ChromeAdapter:
    def __init__(self,state:Path,executable=None,extra_args=(),url_builder=search_url):
        self.state=state;self.executable=executable;self.extra_args=tuple(extra_args);self.url_builder=url_builder
        self.process=None;self.port=None;self.lock=threading.Lock();self.profile_owned=False
    def locate(self):
        paths=[self.executable] if self.executable else []
        if sys.platform=='win32':
            paths += [str(Path(os.environ.get(k,''))/'Google/Chrome/Application/chrome.exe') for k in ('PROGRAMFILES','PROGRAMFILES(X86)','LOCALAPPDATA')]
        elif sys.platform=='darwin':paths += ['/Applications/Google Chrome.app/Contents/MacOS/Google Chrome']
        paths += [shutil.which(n) for n in ('google-chrome','chromium','chromium-browser','chrome')]
        return next((str(Path(p).resolve()) for p in paths if p and Path(p).is_file()),None)
    def _start(self,cancel):
        if self.process is not None and self.process.poll() is None:return
        exe=self.locate();require(exe is not None,'CHROME_NOT_INSTALLED')
        profile=self.state/'chrome-profile';profile.mkdir(parents=True,exist_ok=True)
        portfile=profile/'DevToolsActivePort'
        require(not portfile.exists(),'JARVIS_BROWSER_PROFILE_BUSY')
        self.process=subprocess.Popen([exe,'--user-data-dir='+str(profile),'--remote-debugging-address=127.0.0.1','--remote-debugging-port=0','--no-first-run','--no-default-browser-check','--disable-extensions','--disable-sync',*self.extra_args,'about:blank'],stdin=subprocess.DEVNULL,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,shell=False)
        self.profile_owned=True
        deadline=time.monotonic()+8
        while time.monotonic()<deadline:
            require(not cancel.is_set(),'CANCELLED')
            require(self.process.poll() is None,'BROWSER_LAUNCH_FAILED')
            if portfile.is_file():
                try:self.port=int(portfile.read_text().splitlines()[0]);require(1024<=self.port<=65535);return
                except (ValueError,IndexError):pass
            time.sleep(.05)
        raise Failure('BROWSER_LAUNCH_TIMEOUT')
    def execute(self,action,params,cancel):
        require(self.lock.acquire(timeout=2),'DESKTOP_BUSY')
        try:
            self._start(cancel)
            import urllib.request,websocket
            opener=urllib.request.build_opener(urllib.request.ProxyHandler({})) # loopback Chrome only
            with opener.open(f'http://127.0.0.1:{self.port}/json/list',timeout=3) as r:targets=json.loads(r.read(65536))
            target=next((t for t in targets if t.get('type')=='page'),None);require(target is not None,'BROWSER_UNVERIFIED')
            endpoint=target['webSocketDebuggerUrl'];require(endpoint.startswith(f'ws://127.0.0.1:{self.port}/'),'BROWSER_UNVERIFIED')
            ws=websocket.create_connection(endpoint,timeout=.25,suppress_origin=True,http_no_proxy=['127.0.0.1','localhost'])
            try:
                seq=0
                def call(method,arguments=None):
                    nonlocal seq;seq+=1;ws.send(json.dumps({'id':seq,'method':method,'params':arguments or {}}));return seq
                call('Page.enable');call('Network.enable')
                url=self.url_builder(params['query']) if action=='desktop.search_web' else 'about:blank'
                navigate=call('Page.navigate',{'url':url});deadline=time.monotonic()+12;http_status=None;loaded=False
                while time.monotonic()<deadline:
                    require(not cancel.is_set(),'CANCELLED')
                    try:message=json.loads(ws.recv())
                    except websocket.WebSocketTimeoutException:continue
                    if message.get('id')==navigate:require(not message.get('result',{}).get('errorText') and not message.get('error'),'PAGE_LOAD_FAILED')
                    if message.get('method')=='Network.responseReceived':
                        p=message.get('params',{});response=p.get('response',{})
                        if p.get('type')=='Document' and same_search_page(response.get('url',''),url):http_status=int(response.get('status',0))
                    if message.get('method')=='Page.loadEventFired':loaded=True;break
                require(loaded,'PAGE_LOAD_TIMEOUT')
                probe=call('Runtime.evaluate',{'expression':'JSON.stringify({url:location.href,document:document.documentURI,ready:document.readyState})','returnByValue':True})
                observed=None
                while time.monotonic()<deadline:
                    require(not cancel.is_set(),'CANCELLED')
                    try:message=json.loads(ws.recv())
                    except websocket.WebSocketTimeoutException:continue
                    if message.get('id')==probe:
                        observed=json.loads(message.get('result',{}).get('result',{}).get('value','{}'));break
                require(observed and same_search_page(observed.get('url',''),url) and observed.get('ready')=='complete' and observed.get('document')==observed.get('url'),'POSTCONDITION_UNVERIFIED')
                if action=='desktop.search_web':require(http_status is not None and 200<=http_status<300,'PAGE_HTTP_ERROR')
                return {'kind':'CHROME_DOCUMENT_LOADED','url_sha256':digest(url.encode()),'actual_url_sha256':digest(observed['url'].encode()),'document_ready':'complete','http_status':http_status}
            finally:ws.close()
        finally:self.lock.release()
    def close(self):
        if not self.profile_owned:return
        if self.process is not None and self.process.poll() is None:
            try:
                import websocket
                endpoint=(self.state/'chrome-profile/DevToolsActivePort').read_text().splitlines()[1]
                if endpoint.startswith('/devtools/browser/'):
                    ws=websocket.create_connection(f'ws://127.0.0.1:{self.port}'+endpoint,timeout=1,suppress_origin=True,http_no_proxy=['127.0.0.1','localhost'])
                    ws.send('{"id":1,"method":"Browser.close"}');ws.close()
                    self.process.wait(timeout=2)
            except Exception:pass
        if self.process is not None and self.process.poll() is None:
            self.process.terminate()
            try:self.process.wait(timeout=3)
            except subprocess.TimeoutExpired:self.process.kill();self.process.wait(timeout=3)
        self.process=None;self.profile_owned=False
        (self.state/'chrome-profile/DevToolsActivePort').unlink(missing_ok=True)

class Companion:
    def __init__(self,state:Path,adapter,now=lambda:int(time.time()*1000)):
        self.state=state;state.mkdir(parents=True,exist_ok=True);state.chmod(0o700)
        self.db=sqlite3.connect(state/'companion.sqlite3',check_same_thread=False);(state/'companion.sqlite3').chmod(0o600)
        self.db.executescript('CREATE TABLE IF NOT EXISTS metadata(k TEXT PRIMARY KEY,v TEXT); CREATE TABLE IF NOT EXISTS pairs(client TEXT PRIMARY KEY,session TEXT,token_hash TEXT,revoked INTEGER DEFAULT 0); CREATE TABLE IF NOT EXISTS commands(id TEXT PRIMARY KEY,client TEXT,nonce TEXT,body_hash TEXT,receipt TEXT,UNIQUE(client,nonce));')
        row=self.db.execute("SELECT v FROM metadata WHERE k='device_id'").fetchone()
        self.device_id=row[0] if row else str(uuid.uuid4())
        if not row:self.db.execute("INSERT INTO metadata VALUES ('device_id',?)",(self.device_id,));self.db.commit()
        self.lock=threading.RLock();self.adapter=adapter;self.now=now;self.invite=None;self.pending={};self.cancels={};self.rates=collections.defaultdict(collections.deque)
    def new_invitation(self,url,pin):
        p=urlparse(url);require(p.scheme=='https' and local_address(p.hostname) and p.port and not p.username and not p.query and p.path in ('','/'))
        with self.lock:
            self.invite={'schema_version':1,'url':url.rstrip('/'),'certificate_sha256':pin,'device_id':self.device_id,'pairing_id':str(uuid.uuid4()),'code':secrets.token_urlsafe(32),'expires_at':self.now()+120000,'device_name':'Jarvis computer'}
            return dict(self.invite)
    def request_pair(self,request):
        with self.lock:
            invite=self.invite;require(invite is not None and self.now()<invite['expires_at'],'PAIRING_EXPIRED')
            require(set(request)=={'pairing_id','client_id','nonce','proof'} and uid(request['client_id']) and uid(request['nonce']))
            queue=self.rates['pair'];self._rate(queue,5)
            expected=hmac.new(invite['code'].encode(),(request['pairing_id']+'\n'+request['client_id']+'\n'+request['nonce']).encode(),hashlib.sha256).hexdigest()
            require(request['pairing_id']==invite['pairing_id'] and hmac.compare_digest(expected,str(request['proof'])),'PAIRING_PROOF_INVALID')
            ticket=str(uuid.uuid4());code=secrets.token_hex(3).upper()
            self.pending[ticket]={'client':request['client_id'],'nonce':request['nonce'],'proof':request['proof'],'expires':invite['expires_at'],'approved':False,'compare_code':code}
            self.invite=None
            return {'ticket':ticket,'compare_code':code,'expires_at':self.pending[ticket]['expires']}
    def approve(self,ticket):
        with self.lock:
            p=self.pending.get(ticket);require(p is not None and self.now()<p['expires'],'PAIRING_EXPIRED');p['approved']=True
    def finish_pair(self,request):
        with self.lock:
            require(set(request)=={'ticket','client_id','nonce','proof'})
            p=self.pending.get(request['ticket']);require(p is not None and self.now()<p['expires'],'PAIRING_EXPIRED')
            require(p['client']==request['client_id'] and p['nonce']==request['nonce'] and hmac.compare_digest(p['proof'],str(request['proof'])),'PAIRING_PROOF_INVALID')
            require(p['approved'],'DESKTOP_APPROVAL_REQUIRED')
            for c in self.db.execute('SELECT id FROM commands WHERE client=?',(p['client'],)).fetchall():
                if c[0] in self.cancels:self.cancels[c[0]].set()
            token=secrets.token_urlsafe(32);session=str(uuid.uuid4())
            self.db.execute('INSERT OR REPLACE INTO pairs VALUES (?,?,?,0)',(p['client'],session,digest(token.encode())));self.db.commit();del self.pending[request['ticket']]
            return {'schema_version':1,'device_id':self.device_id,'client_id':p['client'],'session_id':session,'token':token,'capabilities':CAPABILITIES}
    def authenticate(self,token):
        require(isinstance(token,str) and 30<=len(token)<=100,'UNAUTHORIZED')
        with self.lock:
            row=self.db.execute('SELECT client,session,token_hash FROM pairs WHERE token_hash=? AND revoked=0',(digest(token.encode()),)).fetchone()
        require(row is not None,'UNAUTHORIZED');return row
    def _rate(self,queue,limit):
        while queue and queue[0]<self.now()-60000:queue.popleft()
        require(len(queue)<limit,'RATE_LIMITED');queue.append(self.now())
    def signed(self,row,request,body_hash,state,evidence=None,error=None):
        require(state!='COMPLETED' or evidence is not None,'POSTCONDITION_UNVERIFIED')
        receipt={'schema_version':1,'command_id':request['command_id'],'target_device_id':self.device_id,'requester_user_id':row[0],'session_id':row[1],'request_sha256':body_hash,'action_type':request['action_type'],'accepted_at':self.now(),'completed_at':self.now() if state!='PROCESSING' else None,'state':state,'verified_postcondition':evidence,'error_category':error,'retryable':error in ('DESKTOP_BUSY','PAGE_LOAD_TIMEOUT'),'user_action_needed':None}
        raw=canonical(receipt);return {'receipt':base64.urlsafe_b64encode(raw).decode().rstrip('='),'signature':hmac.new(bytes.fromhex(row[2]),raw,hashlib.sha256).hexdigest()}
    def command(self,token,body):
        row=self.authenticate(token);request=json.loads(body);require(isinstance(request,dict));body_hash=digest(body)
        with self.lock:
            existing=self.db.execute('SELECT client,body_hash,receipt FROM commands WHERE id=?',(request.get('command_id'),)).fetchone()
            if existing:
                require(existing[0]==row[0] and existing[1]==body_hash,'IDEMPOTENCY_CONFLICT')
                return json.loads(existing[2]) if existing[2] else self.signed(row,request,body_hash,'PROCESSING')
            action,params=validate_command(request,self.device_id,row[0],self.now());require(request['session_id']==row[1],'WRONG_SESSION')
            self._rate(self.rates[row[0]],10)
            require(self.db.execute('SELECT COUNT(*) FROM commands').fetchone()[0]<10000,'AUDIT_STORAGE_FULL')
            try:self.db.execute('INSERT INTO commands VALUES (?,?,?,?,NULL)',(request['command_id'],row[0],request['nonce'],body_hash));self.db.commit()
            except sqlite3.IntegrityError:raise Failure('REPLAY_REJECTED')
            cancel=threading.Event();self.cancels[request['command_id']]=cancel
        try:
            evidence=self.adapter.execute(action,params,cancel)
            require(not cancel.is_set(),'CANCELLED')
            result=self.signed(row,request,body_hash,'COMPLETED',evidence)
        except Failure as e:result=self.signed(row,request,body_hash,'CANCELLED' if e.code=='CANCELLED' else 'FAILED',error=e.code)
        except Exception:result=self.signed(row,request,body_hash,'FAILED',error='DESKTOP_EXECUTION_FAILED')
        with self.lock:
            self.db.execute('UPDATE commands SET receipt=? WHERE id=?',(json.dumps(result),request['command_id']));self.db.commit();self.cancels.pop(request['command_id'],None)
        return result
    def capabilities(self,token,request):
        row=self.authenticate(token)
        require(isinstance(request,dict) and set(request)=={'nonce'} and uid(request['nonce']))
        with self.lock:self._rate(self.rates['capabilities:'+row[0]],20)
        locate=getattr(self.adapter,'locate',None)
        try:chrome=bool(locate()) if locate else None
        except Exception:chrome=None
        status='REQUIRES_CONFIRMATION' if chrome else 'UNAVAILABLE'
        reason=None if chrome else 'CHROME_NOT_INSTALLED' if chrome is False else 'ADAPTER_READINESS_UNAVAILABLE'
        platform='windows' if sys.platform=='win32' else 'macos' if sys.platform=='darwin' else 'linux' if sys.platform.startswith('linux') else 'unsupported'
        capabilities=[{'action_id':action,'status':status,'reason':reason,'app':'chrome','verification':'CHROME_DOCUMENT_LOADED'} for action in CAPABILITIES]
        capabilities += [{'action_id':action,'status':'UNAVAILABLE','reason':'NOT_IMPLEMENTED','verification':None} for action in ('desktop.open_document','desktop.transfer_file','desktop.arbitrary_app','desktop.screen_capture','desktop.remote_shell')]
        value={'schema_version':1,'agent_version':'1.1','service':'jarvis-companion','device_id':self.device_id,'client_id':row[0],'session_id':row[1],'request_nonce':request['nonce'],'sampled_at':self.now(),'platform':platform,'capabilities':capabilities}
        raw=canonical(value)
        return {'manifest':base64.urlsafe_b64encode(raw).decode().rstrip('='),'signature':hmac.new(bytes.fromhex(row[2]),raw,hashlib.sha256).hexdigest()}
    def receipt(self,token,command_id):
        row=self.authenticate(token)
        with self.lock:r=self.db.execute('SELECT receipt FROM commands WHERE id=? AND client=?',(command_id,row[0])).fetchone()
        require(r is not None,'RECEIPT_NOT_FOUND');require(r[0] is not None,'STILL_PROCESSING');return json.loads(r[0])
    def cancel(self,token,command_id):
        row=self.authenticate(token)
        with self.lock:
            require(self.db.execute('SELECT id FROM commands WHERE id=? AND client=?',(command_id,row[0])).fetchone() is not None,'RECEIPT_NOT_FOUND')
            event=self.cancels.get(command_id)
            if event:event.set()
        return {'state':'CANCELLATION_REQUESTED','undo_guaranteed':False}
    def revoke(self,token):
        row=self.authenticate(token)
        with self.lock:
            self.db.execute('UPDATE pairs SET revoked=1 WHERE client=?',(row[0],));self.db.commit()
            for c in self.db.execute('SELECT id FROM commands WHERE client=?',(row[0],)).fetchall():
                if c[0] in self.cancels:self.cancels[c[0]].set()
        return {'revoked':True}
    def revoke_all(self):
        with self.lock:
            self.db.execute('UPDATE pairs SET revoked=1');self.db.commit()
            for event in self.cancels.values():event.set()
    def close(self):
        for event in self.cancels.values():event.set()
        self.adapter.close();self.db.close()

class Handler(http.server.BaseHTTPRequestHandler):
    server_version='JarvisCompanion/1';sys_version=''
    def log_message(self,*args):pass # No URLs, credentials, pairing invitations or user queries in logs.
    def reply(self,status,value):
        data=canonical(value);self.send_response(status);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(data)));self.send_header('Cache-Control','no-store');self.send_header('X-Content-Type-Options','nosniff');self.end_headers();self.wfile.write(data)
    def do_GET(self):
        try:
            if self.path=='/v1/health':self.reply(200,{'service':'jarvis-companion','schema_version':1,'device_id':self.server.app.device_id,'capabilities':CAPABILITIES});return
            if self.path.startswith('/v1/receipts/'):self.reply(200,self.server.app.receipt(self.token(),self.path.removeprefix('/v1/receipts/')));return
            self.reply(404,{'error':'ROUTE_NOT_FOUND'})
        except Failure as e:self.reply(401 if e.code=='UNAUTHORIZED' else 409,{'error':e.code})
    def token(self):return self.headers.get('Authorization','').removeprefix('Bearer ')
    def do_POST(self):
        try:
            require(self.headers.get('Origin') is None,'ORIGIN_REJECTED')
            require(self.headers.get('Content-Type','').split(';')[0]=='application/json','CONTENT_TYPE_REQUIRED')
            size=int(self.headers.get('Content-Length','0'));require(0<size<=8192,'REQUEST_TOO_LARGE')
            body=self.rfile.read(size);require(len(body)==size)
            app=self.server.app
            if self.path=='/v1/pair/request':value=app.request_pair(json.loads(body))
            elif self.path=='/v1/pair/complete':value=app.finish_pair(json.loads(body))
            elif self.path=='/v1/commands':value=app.command(self.token(),body)
            elif self.path=='/v1/capabilities':value=app.capabilities(self.token(),json.loads(body))
            elif self.path=='/v1/revoke':value=app.revoke(self.token())
            elif self.path.startswith('/v1/cancel/'):value=app.cancel(self.token(),self.path.removeprefix('/v1/cancel/'))
            else:self.reply(404,{'error':'ROUTE_NOT_FOUND'});return
            self.reply(200,value)
        except Failure as e:self.reply(401 if e.code=='UNAUTHORIZED' else 429 if e.code=='RATE_LIMITED' else 409 if e.code in ('DESKTOP_APPROVAL_REQUIRED','STILL_PROCESSING') else 400,{'error':e.code})
        except (ValueError,TypeError,KeyError,json.JSONDecodeError):self.reply(400,{'error':'INVALID_REQUEST'})
        except (BrokenPipeError,ConnectionResetError):pass
        except Exception:self.reply(500,{'error':'COMPANION_INTERNAL_ERROR'})
    def setup(self):
        super().setup();self.connection.settimeout(3)
        if isinstance(self.connection,ssl.SSLSocket):self.connection.do_handshake()
        self.connection.settimeout(20)

class Server(http.server.ThreadingHTTPServer):
    daemon_threads=True;block_on_close=False
    def __init__(self,*args,**kwargs):self.slots=threading.BoundedSemaphore(8);super().__init__(*args,**kwargs)
    def process_request(self,request,client_address):
        if not self.slots.acquire(blocking=False):request.close();return
        try:super().process_request(request,client_address)
        except Exception:self.slots.release();raise
    def process_request_thread(self,request,client_address):
        try:super().process_request_thread(request,client_address)
        finally:self.slots.release()
    def handle_error(self,request,client_address):pass # no request data or exception payload logging

def tls_identity(state:Path,host):
    from cryptography import x509
    from cryptography.hazmat.primitives import hashes,serialization
    from cryptography.hazmat.primitives.asymmetric import ec
    from cryptography.x509.oid import NameOID
    import datetime
    certificate=state/'tls-certificate.pem';keyfile=state/'tls-private.pem'
    require(certificate.exists()==keyfile.exists(),'TLS_IDENTITY_INCOMPLETE')
    marker=state/'tls-identity-created'
    if not certificate.exists():
        require(not marker.exists(),'TLS_IDENTITY_MISSING')
        key=ec.generate_private_key(ec.SECP256R1());name=x509.Name([x509.NameAttribute(NameOID.COMMON_NAME,'Jarvis local companion')]);now=datetime.datetime.now(datetime.timezone.utc)
        cert=x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(key.public_key()).serial_number(x509.random_serial_number()).not_valid_before(now-datetime.timedelta(minutes=5)).not_valid_after(now+datetime.timedelta(days=730)).add_extension(x509.SubjectAlternativeName([x509.IPAddress(ipaddress.ip_address(host))]),critical=False).sign(key,hashes.SHA256())
        fd=os.open(keyfile,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
        with os.fdopen(fd,'wb') as f:f.write(key.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()))
        certificate.write_bytes(cert.public_bytes(serialization.Encoding.PEM));marker.write_text('Existing identity must be restored, not silently rotated.\n')
    cert=x509.load_pem_x509_certificate(certificate.read_bytes())
    require(ipaddress.ip_address(host) in cert.extensions.get_extension_for_class(x509.SubjectAlternativeName).value.get_values_for_type(x509.IPAddress),'TLS_HOST_CHANGED')
    return certificate,keyfile,cert.fingerprint(hashes.SHA256()).hex()
def save_invitation(state,invitation):
    import qrcode,qrcode.image.svg
    payload='JARVIS-PAIR-1:'+base64.urlsafe_b64encode(canonical(invitation)).decode().rstrip('=')
    text=state/'pairing-invitation.txt';text.write_text(payload);text.chmod(0o600)
    qr=qrcode.make(payload,image_factory=qrcode.image.svg.SvgPathImage);path=state/'pairing-qr.svg';qr.save(path);path.chmod(0o600)
    return path

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--bind',default='127.0.0.1');parser.add_argument('--port',type=int,default=8765);parser.add_argument('--state',type=Path,default=Path.home()/'.jarvis-companion');parser.add_argument('--chrome-path');args=parser.parse_args()
    require(local_address(args.bind) and 1024<=args.port<=65535,'PRIVATE_LAN_ADDRESS_REQUIRED')
    args.state=args.state.expanduser().resolve();args.state.mkdir(parents=True,exist_ok=True);args.state.chmod(0o700)
    adapter=ChromeAdapter(args.state,args.chrome_path);app=Companion(args.state,adapter);cert,key,pin=tls_identity(args.state,args.bind)
    server=Server((args.bind,args.port),Handler);server.app=app
    context=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);context.minimum_version=ssl.TLSVersion.TLSv1_2;context.load_cert_chain(cert,key);server.socket=context.wrap_socket(server.socket,server_side=True,do_handshake_on_connect=False)
    def invite():
        path=save_invitation(args.state,app.new_invitation(f'https://{args.bind}:{args.port}',pin));print(f'New private QR: {path}\nValid for 120 seconds. Scan in Jarvis, compare the code, then approve the phone here. Do not share the invitation publicly.')
    invite();thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
    print('Visible companion running. Only Chrome open/search are enabled. Commands: pending | approve TICKET | invite | revoke-all | stop. No mic or speakers are used.')
    try:
        while True:
            command=input('Jarvis companion> ').strip()
            try:
                if command=='stop':break
                if command=='pending':
                    with app.lock:
                        for ticket,p in app.pending.items():print(f'Ticket {ticket} | phone {p["client"][-8:]} | compare code {p["compare_code"]} | expires in {max(0,(p["expires"]-app.now())//1000)} seconds')
                elif command.startswith('approve '):app.approve(command.split(' ',1)[1]);print('Approved. Finish pairing on the phone.')
                elif command=='invite':invite()
                elif command=='revoke-all':app.revoke_all();print('All phone sessions revoked; in-flight cancellation requested.')
                else:print('Use pending, approve TICKET, invite, revoke-all or stop.')
            except Failure as e:print(e.code)
    except (EOFError,KeyboardInterrupt):pass
    finally:server.shutdown();server.server_close();app.close();(args.state/'pairing-invitation.txt').unlink(missing_ok=True);(args.state/'pairing-qr.svg').unlink(missing_ok=True)
if __name__=='__main__':
    try:main()
    except Failure as e:print('Companion could not start: '+e.code,file=sys.stderr);sys.exit(1)
