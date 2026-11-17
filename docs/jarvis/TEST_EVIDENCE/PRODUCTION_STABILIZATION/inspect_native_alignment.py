"""ELF LOAD alignment inspection, not Android 16 KB runtime acceptance."""
import hashlib,json,struct,sys,zipfile
from pathlib import Path
apk=Path(sys.argv[1]);output=Path(sys.argv[2]);rows=[]
with zipfile.ZipFile(apk) as z:
    for name in z.namelist():
        if not (name.startswith('lib/') and name.endswith('.so')):continue
        with z.open(name) as source:
            header=source.read(64)
            assert header[:6]==b'\x7fELF\x02\x01', 'Expected little-endian ELF64'
            offset=struct.unpack_from('<Q',header,32)[0]
            size,count=struct.unpack_from('<HH',header,54)
            assert size>=56 and count<128
            source.seek(offset);programs=source.read(size*count)
        loads=[]
        for i in range(count):
            p=struct.unpack_from('<IIQQQQQQ',programs,i*size)
            if p[0]==1:loads.append({'offset':p[2],'vaddr':p[3],'alignment':p[7],'compatible_16k':p[7]>=16384 and (p[3]-p[2])%16384==0})
        rows.append({'library':name,'load_segments':loads,'compatible_16k':bool(loads) and all(p['compatible_16k'] for p in loads)})
with apk.open('rb') as source:sha=hashlib.file_digest(source,'sha256').hexdigest()
result={'artifact':str(apk),'sha256':sha,'scope':'ELF program headers only. Zero Android 16 KB device tests executed.','libraries':rows,'compatible':sum(r['compatible_16k'] for r in rows),'incompatible':sum(not r['compatible_16k'] for r in rows)}
output.write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps({'libraries':len(rows),'compatible':result['compatible'],'incompatible':[r['library'] for r in rows if not r['compatible_16k']]},indent=2))
