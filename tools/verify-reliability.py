#!/usr/bin/env python3
"""Local, synthetic verification; all heavy work runs under the shared lock."""
import argparse
from collections import defaultdict
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import statistics
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]


def capture(*command):
    return subprocess.check_output(command, cwd=ROOT, text=True).strip()


def write_json(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def junit_counts(directory):
    totals = dict(tests=0, failures=0, errors=0, skipped=0)
    files = list(directory.glob("TEST-*.xml"))
    if not files:
        raise RuntimeError(f"No fresh JUnit XML: {directory}")
    for path in files:
        suite = ET.parse(path).getroot()
        for key in totals:
            totals[key] += int(suite.get(key, "0"))
    return totals


def summarize(raw):
    groups = defaultdict(list)
    for sample in raw["samples"]:
        if not sample["warmup"]:
            groups[(sample["scenario"], sample["workers"])].append(sample)
    result = []
    for (scenario, workers), samples in sorted(groups.items()):
        row = dict(scenario=scenario, workers=workers, repetitions=len(samples))
        for field in ("elapsedMs", "recoveryAfterResumeMs", "injectedDowntimeMs"):
            values = [sample[field] for sample in samples]
            row[field] = dict(median=statistics.median(values), minimum=min(values), maximum=max(values))
        row["ordersPerRepetition"] = 40
        row["duplicates"] = sum(sample["duplicates"] for sample in samples)
        row["allControlTotalsMatched"] = all(sample["controlTotalMatched"] for sample in samples)
        result.append(row)
    return dict(groups=result, limitations=[
        "Same code, workers 1 vs 2: configuration comparison, not code before/after or production throughput.",
        "Synthetic PG inquiry only; FAILED is not a successful payment. No real PG requests.",
        "Controlled interruption after durable chunks, not a process kill or database outage.",
        "Five measured repetitions per group; median/range are descriptive, not an SLA or statistical guarantee.",
        "Shared lock only coordinates participating projects; other host load can remain.",
    ])


def run_locked(mode):
    # No reuse of existing artifacts, and no environment files or variables are dumped.
    docker_version = capture("docker", "info", "--format", "{{.ServerVersion}}")
    evidence_root = ROOT / "docs/evidence"
    evidence_root.mkdir(parents=True, exist_ok=True)
    output = Path(tempfile.mkdtemp(prefix="reliability-", dir=evidence_root))
    diff = subprocess.check_output(["git", "diff", "HEAD", "--binary"], cwd=ROOT)
    untracked = capture("git", "ls-files", "--others", "--exclude-standard").splitlines()
    source_hashes = {name: hashlib.sha256((ROOT / name).read_bytes()).hexdigest()
                     for name in untracked if Path(name).suffix in (".java", ".py", ".md", ".gradle")}
    metadata = dict(startedAt=datetime.now(timezone.utc).isoformat(), mode=mode,
                    revision=capture("git", "rev-parse", "HEAD"),
                    branch=capture("git", "branch", "--show-current"),
                    trackedDiffSha256=hashlib.sha256(diff).hexdigest(), untrackedSourceSha256=source_hashes,
                    dockerVersion=docker_version, javaHome=os.environ.get("JAVA_HOME"), runs=[])
    write_json(output / "metadata.json", metadata)
    print(f"Evidence: {output}", flush=True)
    tasks = (["test", "bootJar"] if mode in ("regression", "all") else [])
    if mode in ("measure", "all"):
        tasks.append("reliabilityMeasurement")
    try:
        for task in tasks:
            command = ["bash", "./gradlew", task, "--no-daemon", "--rerun-tasks",
                       f"-PevidenceDir={output}", f"-PmeasurementOutput={output / 'measurement.json'}"]
            with (output / f"{task}.log").open("w", encoding="utf-8") as log:
                process = subprocess.Popen(command, cwd=ROOT / "Backend", stdout=subprocess.PIPE,
                                           stderr=subprocess.STDOUT, text=True)
                for line in process.stdout:
                    log.write(line)
                    print(line, end="", flush=True)
                code = process.wait()
            run = dict(task=task, exitCode=code)
            metadata["runs"].append(run)
            if (output / f"{task}-xml").exists():
                run["junit"] = junit_counts(output / f"{task}-xml")
            if code:
                raise RuntimeError(f"{task} exited {code}; see raw log")
            if task in ("test", "reliabilityMeasurement"):
                counts = run.get("junit")
                if not counts or not counts["tests"] or any(counts[key] for key in ("failures", "errors", "skipped")):
                    raise RuntimeError(f"Missing, failed or skipped tests: {counts}")
            if task == "reliabilityMeasurement":
                raw = json.loads((output / "measurement.json").read_text())
                if len(raw["samples"]) != 24:
                    raise RuntimeError("Incomplete measurement: expected 4 warmup + 20 measured samples")
                write_json(output / "summary.json", summarize(raw))
        metadata["status"] = "PASS"
        return 0
    except Exception as error:
        metadata["status"] = "FAIL"
        metadata["failure"] = str(error)
        print(str(error), file=sys.stderr)
        return 1
    finally:
        metadata["finishedAt"] = datetime.now(timezone.utc).isoformat()
        write_json(output / "metadata.json", metadata)
        print(f"Evidence retained: {output}", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("regression", "measure", "all"))
    parser.add_argument("--lock", default=os.environ.get("CAPSURE_MEASUREMENT_LOCK"),
                        help="Path to the common measurement_lock.py; required outside the lock")
    parser.add_argument("--timeout", type=float, default=0)
    parser.add_argument("--inside-lock", action="store_true", help=argparse.SUPPRESS)
    args = parser.parse_args()
    if args.inside_lock:
        return run_locked(args.mode)
    if not args.lock or not Path(args.lock).is_file():
        parser.error("Provide --lock /absolute/path/to/measurement_lock.py; never bypass shared coordination")
    return subprocess.call([sys.executable, args.lock, "--timeout", str(args.timeout), "--",
                            sys.executable, str(Path(__file__).resolve()), args.mode, "--inside-lock"])


if __name__ == "__main__":
    raise SystemExit(main())
