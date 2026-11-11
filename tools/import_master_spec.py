"""Import the user's unabridged catalog. No implementation or model-training claims."""
from pathlib import Path
import argparse,hashlib,json,re

def import_spec(source:Path,root:Path):
    raw=source.read_bytes();lines=raw.decode().splitlines();features=[];scenarios=[];system='Front matter'
    for i,line in enumerate(lines):
        if line.startswith(('### ORIGINAL SYSTEM:','### NEW SYSTEM:','## System ','## Supplemental capability domain')):system=line.lstrip('# ')
        original=re.match(r'^\*\*(\d{3})\. (.+?)\.\*\*',line)
        extended=re.match(r'^(?:### Feature |\*\*Feature )(\d{4}) — (.+?)(?:\.\*\*|$)',line)
        m=original or extended
        if m:
            number=int(m[1]);block=[line]
            if extended:
                j=i+1
                while j<len(lines) and lines[j].strip() and not lines[j].startswith('#'):block.append(lines[j]);j+=1
            features.append(dict(id=number,label=f'{number:03}',title=m[2],system=system,requirement='\n'.join(block),source_line=i+1,status='NOT_STARTED',evidence=[],implementation_notes='Not yet audited against this full requirement; earlier Android capabilities are preserved, not assumed complete.',platform_support={},remaining_acceptance='The complete feature requirement and its original validation/done criteria remain binding.'))
        q=re.match(r'^### (JQ-\d{4}) — (.+) \[(train|validation|test)\]',line)
        if q:
            block='\n'.join(lines[i+1:i+8])
            field=lambda pattern: re.search(pattern,block,re.M).group(1)
            scenarios.append(dict(id=q[1],domain=q[2],split=q[3],utterance=field(r'^\*\*User question / command:\*\* (.+)'),intent=field(r'\*\*Intent:\*\* `([^`]+)`'),tool=field(r'\*\*Tool:\*\* `([^`]+)`'),risk=int(field(r'\*\*Risk:\*\* (\d+)')),offline=field(r'\*\*Offline:\*\* (\w+)'),answers=[field(r'^\*\*Answer example A:\*\* (.+)'),field(r'^\*\*Answer example B:\*\* (.+)')],execution_rule=field(r'^\*\*Execution rule:\*\* (.+)'),source_line=i+1))
    assert sorted(f['id'] for f in features)==list(range(1,1501))
    assert len(scenarios)==1000 and len({s['id'] for s in scenarios})==1000
    output=root/'evaluation';output.mkdir(exist_ok=True)
    ledger=dict(schema_version=1,spec_sha256=hashlib.sha256(raw).hexdigest(),policy='All 1500 are planned requirements; status requires release-specific evidence. No model is trained by importing fixtures.',features=features)
    (output/'feature-ledger.json').write_text(json.dumps(ledger,ensure_ascii=False,indent=2)+'\n')
    (output/'scenarios.json').write_text(json.dumps(scenarios,ensure_ascii=False,indent=2)+'\n')
    answers=[dict(id=s['id']+'-'+kind,scenario_id=s['id'],split=s['split'],text=text,kind='PRE_ACTION_EXAMPLE_NOT_EXECUTION_PROOF') for s in scenarios for kind,text in zip(('A','B'),s['answers'])]
    (output/'answer-examples.json').write_text(json.dumps(answers,ensure_ascii=False,indent=2)+'\n')
    (root/'docs/v2/MASTER_SPEC.md').write_bytes(raw)
    print(json.dumps(dict(features=len(features),scenarios=len(scenarios),answer_examples=len(answers),source_sha256=ledger['spec_sha256'])))
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('source',type=Path);args=p.parse_args();import_spec(args.source,Path(__file__).resolve().parents[1])
