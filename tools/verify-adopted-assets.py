#!/usr/bin/env python3
"""Check current adoption registry and original delivery hashes without editing assets."""
import csv
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def check_file(path, expected=None):
    file = ROOT / path
    if not file.is_file():
        raise ValueError(f"Missing asset: {path}")
    if expected and hashlib.sha256(file.read_bytes()).hexdigest() != expected:
        raise ValueError(f"SHA-256 mismatch: {path}")


def main():
    registry = json.loads((ROOT / "art/ADOPTED-ASSETS.json").read_text())
    assets = registry["assets"]
    ids = {a["id"] for a in assets}
    if len(ids) != len(assets) or len(assets) != 43:
        raise ValueError("Expected 43 unique adopted asset IDs")
    if sum(a["batch"] == "core-v1" for a in assets) != 14:
        raise ValueError("Expected 14 core assets")
    if sum(a["batch"] == "second-v2" for a in assets) != 29:
        raise ValueError("Expected 29 second-batch assets")
    with (ROOT / "art/ADOPTED-ASSETS.csv").open(newline="") as file:
        rows = list(csv.DictReader(file))
    if rows != [{k: str(a[k]) for k in rows[0]} for a in assets]:
        raise ValueError("CSV and JSON registry differ")
    for asset in assets:
        if asset["adopted"] is not True or asset["integrated"] is not True:
            raise ValueError(f"Incorrect current status: {asset['id']}")
        check_file(asset["source_manifest"], asset["source_manifest_sha256"])
        check_file(asset["integration_document"])
        check_file(asset["verification_document"])
        for item in asset["runtime_files"]:
            check_file(item["path"], item["sha256"])
    count = 0
    source_manifests = {"art/approved-v1/SOURCE-MANIFEST.json"}
    source_manifests.update(a["source_manifest"] for a in assets)
    for name in sorted(source_manifests):
        data = json.loads((ROOT / name).read_text())
        source_files = data.get("sourceFiles", [])
        if not isinstance(source_files, list):
            continue  # Some historical manifests use this key for a file count.
        for item in source_files:
            check_file(item["file"], item["sha256"])
            count += 1
    manifests = sorted((ROOT / "art/production-v2").glob("*/v*/manifest.json"))
    if len(manifests) != 30:
        raise ValueError("Expected 30 preserved second-batch delivery manifests")
    for manifest in manifests:
        data = json.loads(manifest.read_text())
        for item in data["files"]:
            relative = item.get("path") or item["file"]
            check_file(str((manifest.parent / relative).relative_to(ROOT)), item["sha256"])
            count += 1
    provenance = json.loads((ROOT / "src/main/resources/assets/wildcraft/second-art-provenance.json").read_text())
    for item in provenance:
        check_file(item["source"], item["sha256"])
        check_file(item["target"], item["sha256"])
    print(f"PASS: {len(assets)} current IDs; {len(manifests)} second-batch versions; "
          f"{count} original file records; {len(provenance)} runtime copies")


if __name__ == "__main__":
    main()
