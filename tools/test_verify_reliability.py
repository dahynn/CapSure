import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location("verify", Path(__file__).with_name("verify-reliability.py"))
verify = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verify)


class EvidenceTest(unittest.TestCase):
    def test_missing_xml_is_not_success(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(RuntimeError):
                verify.junit_counts(Path(directory))

    def test_junit_counts_preserve_failures_and_skips(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)
            (path / "TEST-synthetic.xml").write_text(
                '<testsuite tests="5" failures="1" errors="1" skipped="2"/>')
            self.assertEqual(verify.junit_counts(path), dict(tests=5, failures=1, errors=1, skipped=2))

    def test_summary_excludes_warmup_and_keeps_each_configuration(self):
        samples = []
        for workers in (1, 2):
            for repetition, elapsed in enumerate((999, 10, 20, 30, 40, 50)):
                samples.append(dict(scenario="synthetic", workers=workers, warmup=repetition == 0,
                                    elapsedMs=elapsed, recoveryAfterResumeMs=0, injectedDowntimeMs=0,
                                    duplicates=0, controlTotalMatched=True))
        groups = verify.summarize(dict(samples=samples))["groups"]
        self.assertEqual(len(groups), 2)
        for group in groups:
            self.assertEqual(group["repetitions"], 5)
            self.assertEqual(group["elapsedMs"], dict(median=30, minimum=10, maximum=50))


if __name__ == "__main__":
    unittest.main()
