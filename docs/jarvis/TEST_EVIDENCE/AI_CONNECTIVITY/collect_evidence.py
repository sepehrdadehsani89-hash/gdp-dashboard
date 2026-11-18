import csv
import re
import hashlib
import json
import shutil
from pathlib import Path
import xml.etree.ElementTree as ET

repo = next(p for p in Path(__file__).resolve().parents if (p/'gradlew').is_file() and (p/'app').is_dir())
out = Path(__file__).resolve().parent
results = repo / 'app/build/test-results/testDebugUnitTest'
frozen = json.loads((out / 'frozen-application-source.json').read_text())
changed = [name for name, digest in frozen.items() if hashlib.sha256((repo/name).read_bytes()).hexdigest() != digest]
if changed:
    raise SystemExit('Application source changed during verification: '+repr(changed))
shutil.copytree(results, out/'jvm', dirs_exist_ok=True)
rows=[]
for xml in sorted(results.glob('TEST-*.xml')):
    root=ET.parse(xml).getroot()
    for case in root.findall('testcase'):
        state='FAIL' if case.find('failure') is not None or case.find('error') is not None else 'SKIP' if case.find('skipped') is not None else 'PASS'
        rows.append({'class':case.attrib.get('classname',root.attrib.get('name','')),'test':case.attrib['name'],'result':state,'seconds':case.attrib.get('time','')})
summary={'tests':len(rows),'passed':sum(x['result']=='PASS' for x in rows),'failed':sum(x['result']=='FAIL' for x in rows),'skipped':sum(x['result']=='SKIP' for x in rows),'environment':'Linux cloud JVM tests. Mock provider responses are simulated. No provider credentials or physical audio are involved.','application_source_unchanged_during_verification':True,'skipped_cases':[r for r in rows if r['result']=='SKIP']}
(out/'jvm-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
with (out/'jvm-cases.csv').open('w',newline='') as f:
    writer=csv.DictWriter(f,fieldnames=['class','test','result','seconds'],lineterminator='\n');writer.writeheader();writer.writerows(rows)
for name in ['lint-results-debug.xml','lint-results-debug.txt','lint-results-debug.html']:
    shutil.copy2(repo/'app/build/reports'/name,out/name)
lint=ET.parse(out/'lint-results-debug.xml').getroot()
counts={level:sum(x.attrib.get('severity')==level for x in lint.findall('issue')) for level in ['Error','Fatal','Warning','Information']}
(out/'lint-summary.json').write_text(json.dumps(counts,indent=2)+'\n')
features=json.loads((out/'feature-test-map.json').read_text())
for name, feature in features.items():
    feature['declared_test_methods']=sum(len(re.findall(r'@Test\b',(repo/'app/src/test/java/com/jarvis/assistant'/f).read_text())) for f in feature['test_files'])
    names={p.rsplit('/',1)[-1].removesuffix('.kt') for p in feature['test_files']}
    selected=[r for r in rows if r['class'].rsplit('.',1)[-1] in names]
    feature['executed_test_cases']=len(selected)
    feature['passed']=sum(x['result']=='PASS' for x in selected)
    feature['failed']=sum(x['result']=='FAIL' for x in selected)
    feature['skipped']=sum(x['result']=='SKIP' for x in selected)
    feature['status']='PASS' if feature['failed']==0 and feature['passed']>=20 else 'REVIEW'
(out/'feature-test-map.json').write_text(json.dumps(features,indent=2)+'\n')
print(json.dumps({'jvm':summary,'lint':counts,'feature_groups':{k:{x:v[x] for x in ['executed_test_cases','passed','failed','status']} for k,v in features.items()}},indent=2))
if not rows or summary['failed'] or counts['Error'] or counts['Fatal']:
    raise SystemExit(1)
