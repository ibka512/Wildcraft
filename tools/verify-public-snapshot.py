#!/usr/bin/env python3
"""Fail a public snapshot if private animation data or private generated archives are included."""
from pathlib import Path
import argparse
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PRIVATE = (
    'art/player-animation-v1/', 'art/player-rig-v1/', 'art/animation-research/',
    'src/main/resources/assets/wildcraft/animation/',
    'src/gametest/resources/assets/wildcraft-test/animation/',
    'development-assets/local-only/',
    'art/review-batches/', 'art/production-v2/mechanical-bottom-additions/',
)

PRIVATE_FILES = ('src/main/resources/assets/wildcraft/bottom-additions-provenance.json', 'src/main/resources/assets/wildcraft/geometry/battery_bottom.json', 'src/main/resources/assets/wildcraft/geometry/machine_bottom_battery.json', 'src/main/resources/assets/wildcraft/geometry/machine_bottom_rocket.json', 'src/main/resources/assets/wildcraft/geometry/machine_bottom_stabilizer.json', 'src/main/resources/assets/wildcraft/geometry/machine_bottom_wheel.json', 'src/main/resources/assets/wildcraft/geometry/rocket_bottom.json', 'src/main/resources/assets/wildcraft/geometry/spent_rocket_bottom.json', 'src/main/resources/assets/wildcraft/geometry/stabilizer_bottom.json', 'src/main/resources/assets/wildcraft/geometry/wheel_bottom.json')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--artifacts', action='store_true')
    args = parser.parse_args()
    tracked = subprocess.check_output(['git', 'ls-files', '-z'], cwd=ROOT).decode().split('\0')
    bad = [p for p in tracked if (p.startswith(PRIVATE) or p in PRIVATE_FILES) or any(x.startswith('._') for x in Path(p).parts)]
    for directory in PRIVATE:
        path = ROOT / directory
        if path.exists() and any(p.is_file() for p in path.rglob('*')):
            bad.append(directory)
    if bad:
        raise SystemExit('Private data in public snapshot: ' + ', '.join(sorted(set(bad))))
    for name in PRIVATE_FILES:
        if (ROOT / name).exists():
            raise SystemExit("Private local-only derived art: " + name)
    checked = []
    if args.artifacts:
        builds = [ROOT / 'build/libs', ROOT / 'dist']
        # dev.sh uses a project-dependent build root on macOS; dist is the exported main JAR.
        for folder in builds:
            for file in folder.glob('*.jar'):
                with zipfile.ZipFile(file) as jar:
                    if jar.testzip() is not None:
                        raise SystemExit('Corrupt JAR: ' + file.name)
                    forbidden = [n for n in jar.namelist() if n.startswith(('assets/wildcraft/animation/', 'assets/wildcraft-test/animation/')) or n in [p.removeprefix('src/main/resources/') for p in PRIVATE_FILES] or any(p.startswith('._') for p in Path(n).parts)]
                    if forbidden:
                        raise SystemExit('Private/metadata entries in JAR: ' + file.name)
                checked.append(file.name)
        if not checked:
            raise SystemExit('No public build JAR found; build first')
    print(f'PASS: private paths excluded; {len(checked)} public JAR files checked')

if __name__ == '__main__':
    main()
