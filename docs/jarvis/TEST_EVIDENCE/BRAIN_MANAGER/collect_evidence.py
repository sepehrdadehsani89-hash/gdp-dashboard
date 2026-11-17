import csv,hashlib,json,shutil,xml.etree.ElementTree as ET
from pathlib import Path
from collections import Counter
repo=Path('/workspace/gdp-dashboard')
work=Path('/workspace/work/jarvis-brain-manager')
log=work/'integrated-build-5.log'
assert 'BUILD SUCCESSFUL' in log.read_text(),'Final build has not passed; no success evidence collected.'
base=repo/'docs/jarvis/TEST_EVIDENCE/BRAIN_MANAGER'
xmlout=base/'jvm';xmlout.mkdir(parents=True,exist_ok=True)
suites=[];rows=[]
for path in sorted((repo/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml')):
    root=ET.parse(path).getroot()
    suites.append({k:root.get(k) for k in ['name','tests','failures','errors','skipped','time']})
    shutil.copy2(path,xmlout/path.name)
    for case in root.findall('testcase'):
        status='FAIL' if case.find('failure') is not None or case.find('error') is not None else 'SKIPPED' if case.find('skipped') is not None else 'PASS'
        rows.append([case.get('classname'),case.get('name'),status,case.get('time')])
assert rows and all(r[2]!='FAIL' for r in rows),'A failing test prevents final evidence.'
with (base/'JVM_RESULTS.csv').open('w',newline='') as output:
    writer=csv.writer(output,lineterminator='\n');writer.writerow(['suite','case','status','seconds']);writer.writerows(rows)
summary={'environment':'Linux JVM; mock provider transports, generated JWT signing keys, actual numeric-loopback sockets and temporary model fixtures', 'counts':dict(Counter(r[2] for r in rows)),'total':len(rows),'suites':suites,'live_authenticated_provider_requests':0,'physical_device_tests_executed':0,'native_model_inference_tests_executed':0}
(base/'jvm-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
for name in ['integrated-build-5.log']:
    (base/name.replace('.log','.txt')).write_text('\n'.join(line.rstrip() for line in (work/name).read_text().splitlines())+'\n')
for suffix in ['xml','txt']:
    source=repo/f'app/build/reports/lint-results-debug.{suffix}'
    shutil.copy2(source,base/source.name)
lint=Counter(issue.get('severity') for issue in ET.parse(base/'lint-results-debug.xml').getroot().findall('issue'))
assert not lint.get('Error') and not lint.get('Fatal'),dict(lint)
(base/'lint-summary.json').write_text(json.dumps(dict(lint),indent=2)+'\n')
snapshot=json.loads((work/'final-source-snapshot.json').read_text())
assert all(hashlib.sha256((repo/name).read_bytes()).hexdigest()==expected for name,expected in snapshot.items()),'Source changed during final verification.'
(base/'verified-source-sha256.json').write_text(json.dumps(snapshot,indent=2,sort_keys=True)+'\n')
print(json.dumps({'JVM':summary['counts'],'total':len(rows),'lint':dict(lint),'unchanged_source_files':len(snapshot)},indent=2))
