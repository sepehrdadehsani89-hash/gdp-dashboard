"""Structural validation is not ASR/intent accuracy or model training."""
from pathlib import Path
import hashlib,json,unittest
ROOT=Path(__file__).resolve().parents[1]
class CatalogIntegrityTest(unittest.TestCase):
    def test_all_1500_original_requirements_are_preserved_once(self):
        ledger=json.loads((ROOT/'evaluation/feature-ledger.json').read_text());rows=ledger['features'];self.assertEqual(list(range(1,1501)),sorted(r['id'] for r in rows));self.assertTrue(all(r['requirement'] and len(r['requirement'])>50 for r in rows));self.assertEqual(ledger['spec_sha256'],hashlib.sha256((ROOT/'docs/v2/MASTER_SPEC.md').read_bytes()).hexdigest())
    def test_statuses_do_not_claim_unverified_completion(self):
        rows=json.loads((ROOT/'evaluation/feature-ledger.json').read_text())['features'];allowed={'IMPLEMENTED/VERIFIED','IMPLEMENTED/UNVERIFIED','PARTIAL','BLOCKED_BY_PLATFORM','BLOCKED_BY_PERMISSION','BLOCKED_BY_PROVIDER','NOT_STARTED','EXPERIMENTAL'}
        for r in rows:
            self.assertIn(r['status'],allowed)
            if r['status']!='NOT_STARTED':self.assertTrue(r['evidence'])
    def test_scenarios_have_valid_splits_intents_and_execution_rules(self):
        rows=json.loads((ROOT/'evaluation/scenarios.json').read_text());self.assertEqual(1000,len(rows));self.assertEqual(1000,len({r['id'] for r in rows}))
        for r in rows:self.assertIn(r['split'],('train','validation','test'));self.assertTrue(r['utterance'] and r['intent'] and r['tool'] and r['execution_rule']);self.assertIn(r['risk'],range(6))
    def test_two_answer_examples_per_case_are_not_execution_receipts(self):
        rows=json.loads((ROOT/'evaluation/answer-examples.json').read_text());self.assertEqual(2000,len(rows));counts={}
        for r in rows:counts[r['scenario_id']]=counts.get(r['scenario_id'],0)+1;self.assertEqual('PRE_ACTION_EXAMPLE_NOT_EXECUTION_PROOF',r['kind']);self.assertTrue(r['text'])
        self.assertEqual(1000,len(counts));self.assertTrue(all(n==2 for n in counts.values()))
if __name__=='__main__':unittest.main(verbosity=2)
