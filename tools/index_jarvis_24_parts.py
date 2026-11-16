import csv,hashlib,io,json,pathlib,re,sqlite3,zipfile
import argparse
parser=argparse.ArgumentParser()
parser.add_argument('archive',type=pathlib.Path)
parser.add_argument('--output',required=True,type=pathlib.Path)
args=parser.parse_args()
root=args.output
root.mkdir(parents=True,exist_ok=True)
archive=args.archive
z=zipfile.ZipFile(archive)
prefix='JARVIS_MASTER_24_PARTS/'
expected={int(r['part']):r for r in csv.DictReader(io.StringIO(z.read(prefix+'PARTS_INDEX_01_TO_24.csv').decode()))}
refs=root/'originals'; refs.mkdir(exist_ok=True)
db=root/'reference_index.sqlite3'
if db.exists(): db.unlink()
c=sqlite3.connect(db)
c.execute('CREATE TABLE passages(id TEXT PRIMARY KEY,part INTEGER,byte_start INTEGER,byte_end INTEGER,line INTEGER,heading TEXT,text TEXT,status TEXT)')
c.execute('CREATE VIRTUAL TABLE search USING fts5(id UNINDEXED,text)')
summary=[]; joined=hashlib.sha256(); total=0; global_offset=0
with (root/'REFERENCES_01_TO_24_INDEX.csv').open('w') as sf,(root/'FEATURE_LEDGER.csv').open('w') as lf:
 sw=csv.writer(sf); sw.writerow(['part','file','bytes','words','sha256','matches_manifest','global_byte_start','passages'])
 lw=csv.writer(lf); lw.writerow(['reference_id','part','byte_start','byte_end','line','heading','status','review_state','excerpt'])
 for part in range(1,25):
  name=expected[part]['filename']; b=z.read(prefix+name); sha=hashlib.sha256(b).hexdigest()
  assert sha==expected[part]['sha256'] and len(b)==int(expected[part]['bytes'])
  dest=refs/name
  if dest.exists(): assert dest.read_bytes()==b
  else: dest.write_bytes(b); dest.chmod(0o444)
  if part<24: joined.update(b); total+=len(b.decode().split())
  pos=0; line=1; heading=''; count=0
  # Bounded line chunks preserve every byte, including part-boundary fragments.
  chunk=[]; start=0; startline=1; size=0
  def emit():
   global count
   if not chunk:return
   text=b''.join(chunk).decode('utf-8'); rid=f'P{part:02d}-B{start:08d}'
   c.execute('INSERT INTO passages VALUES(?,?,?,?,?,?,?,?)',(rid,part,start,start+size,startline,heading,text,'NEEDS_RESEARCH'))
   c.execute('INSERT INTO search VALUES(?,?)',(rid,text))
   lw.writerow([rid,part,start,start+size,startline,heading,'NEEDS_RESEARCH','INDEXED_NOT_CODE_RECONCILED',text[:250].replace('\n',' ')])
   count+=1
  for raw in b.splitlines(keepends=True):
   txt=raw.decode('utf-8').strip()
   isheading=bool(re.match(r'^(#{1,6}\s|(?:SECTION|PART|CHAPTER|FEATURE)\s|\d+(?:\.\d+)*[.) ]\s*\S)',txt,re.I)) and len(txt)<300
   if chunk and (size+len(raw)>3500 or isheading): emit();chunk=[];size=0
   if isheading: heading=txt
   if not chunk:start=pos;startline=line
   chunk.append(raw);size+=len(raw);pos+=len(raw);line+=1
  emit(); assert pos==len(b)
  sw.writerow([part,name,len(b),len(b.decode().split()),sha,True,global_offset,count]);global_offset+=len(b)
  summary.append({'part':part,'passages':count,'hash_verified':True})
  c.commit()
report={'archive_sha256':hashlib.sha256(archive.read_bytes()).hexdigest(),'original_words':total,'original_joined_sha256':joined.hexdigest(),'parts':summary,'interpretation':'Full deterministic sequential indexing; semantic/code reconciliation pending. Passages are provenance candidates, not individually validated feature requirements. Cross-part continuity is recoverable by numeric part and byte offsets.'}
(root/'INDEX_REPORT.json').write_text(json.dumps(report,indent=2))
print(json.dumps(report,indent=2))
