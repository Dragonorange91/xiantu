"""Emit Forge's Jar-in-Jar resources with the accepted GFBS bytes intact."""
import argparse
import json
import shutil
from pathlib import Path

from verify_gfbs_runtime import EXPECTED, OPTIMIZED, PROJECT, verify_payload

NESTED_PATH = "META-INF/jarjar/gfbs_gltf-1.5.1-xiantu-optimized.jar"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    payload = OPTIMIZED.read_bytes()
    verify_payload(payload, str(OPTIMIZED))
    target = args.output / NESTED_PATH
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(OPTIMIZED, target)
    metadata = {"jars": [{
        "identifier": {"group": "org.lytharalab.gfbs", "artifact": "gfbs_gltf"},
        "version": {"range": "[1.5.1]", "artifactVersion": "1.5.1"},
        "path": NESTED_PATH,
        "isObfuscated": False,
    }]}
    (target.parent / "metadata.json").write_text(
        json.dumps(metadata, indent=2) + "\n", encoding="utf8")
    license_text = (PROJECT / "libs/GFBS-glTF-LICENSE.txt").read_text("utf8")
    (args.output / "META-INF/LICENSE_GFBS").write_text(license_text, encoding="utf8")
    (args.output / "META-INF/NOTICE_GFBS.txt").write_text(
        "GFBS : glTF 1.5.1, Copyright (c) 2026 LytharaLab.\n"
        "MIT License; see LICENSE_GFBS and the license inside the nested JAR.\n"
        "This distribution includes Xiantu's locally modified performance build.\n"
        "Upstream: https://github.com/LytharaLab\n"
        f"Runtime SHA-256: {EXPECTED}\n", encoding="utf8")
    print(f"Prepared optimized GFBS and its MIT notice: {target}")


if __name__ == "__main__":
    main()
