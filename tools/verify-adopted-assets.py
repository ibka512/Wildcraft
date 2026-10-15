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
    if len(ids) != len(assets) or len(assets) != registry["counts"]["current_asset_ids"]:
        raise ValueError("Duplicate or inconsistent current adopted asset IDs")
    if sum(a["batch"] == "core-v1" for a in assets) != 14:
        raise ValueError("Expected 14 core assets")
    if sum(a["batch"] == "second-v2" for a in assets) != 29:
        raise ValueError("Expected 29 second-batch assets")
    wind_ids = {a["id"] for a in assets if a["batch"] == "wind-v1"}
    if wind_ids not in (set(), {"F07-01", "F07-02"}):
        raise ValueError("Wind runtime registry must contain both adopted F07 purposes")
    if len(assets) != 43 + len(wind_ids):
        raise ValueError("Unrecognized new runtime asset batch")
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
    variants = []
    for asset in assets:
        for variant in asset.get("runtime_variants", []):
            check_file(variant["source_manifest"], variant["source_manifest_sha256"])
            check_file(variant["integration_document"]); check_file(variant["verification_document"])
            approval = json.loads((ROOT / variant["approval"]).read_text())
            if not approval.get("adopted"): raise ValueError("Bottom variant lacks adopted source approval")
            variants.append(variant)
    count = 0
    source_manifests = {"art/approved-v1/SOURCE-MANIFEST.json"}
    source_manifests.update(a["source_manifest"] for a in assets)
    source_manifests.update(v["source_manifest"] for v in variants)
    for name in sorted(source_manifests):
        data = json.loads((ROOT / name).read_text())
        if name.endswith("/docs/DELIVERY-MANIFEST.json"):
            for item in data["files"]:
                check_file(str((Path(name).parent.parent / item["path"])), item["sha256"])
                count += 1
            continue
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
    wind_copies = []
    if wind_ids:
        wind_copies = json.loads((ROOT / "src/main/resources/assets/wildcraft/wind-art-provenance.json").read_text())
        for item in wind_copies:
            check_file(item["source"], item["sha256"])
            check_file(item["target"], item["sha256"])
    bottom_copies = 0
    bottom_provenance = ROOT / "src/main/resources/assets/wildcraft/bottom-art-provenance.json"
    if variants:
        records = json.loads(bottom_provenance.read_text())
        if len(records) != 4 or len(variants) != 4: raise ValueError("Expected four approved bottom variants")
        for record in records:
            check_file(record["source"], record["sha256"])
            for output in record["outputs"]:
                check_file(output["target"], output["sha256"]); bottom_copies += 1
    print(f"PASS: {len(assets)} current IDs; {len(manifests)} second-batch versions; "
          f"{count} source file records; {len(provenance) + len(wind_copies) + bottom_copies} runtime copies; {len(variants)} bottom variants")


if __name__ == "__main__":
    main()
