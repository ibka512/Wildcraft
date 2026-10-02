from pathlib import Path
import hashlib
import json
import shutil
import subprocess
import xml.etree.ElementTree as ET
import zipfile

base = Path('/Volumes/仕事/开发/2026-10-02/referenced-chatgpt-conversation-this-is-an')
out = base / 'outputs'
repo = out / 'wildcraft'
runtime = Path('/Users/zhou/Library/Caches/Wildcraft/builds/260a5f2b3758')
evidence = out / 'verification/p3'

def summary(path):
    return {'file': str(path.relative_to(out)), 'bytes': path.stat().st_size,
            'sha256': hashlib.sha256(path.read_bytes()).hexdigest()}

previous = json.loads((out / 'manifest.json').read_text())
assert previous['phase'] == 'P2'
def verify_previous(record):
    for file in record.get('artifacts', []) + record.get('verification', {}).get('files', []):
        assert summary(out / file['file']) == file, file['file']
    for record in record.get('history', []):
        verify_previous(record)
verify_previous(previous)

logs = {
    'datagen.log': 'p3-datagen-final.log',
    'build-final.log': 'p3-build-final.log',
    'client-dev-test.log': 'p3-client-dev-final.log',
    'packaged-client-test.log': 'p3-packaged-client.log',
    'packaged-server.log': 'p3-packaged-server.log',
}
for target, source in logs.items():
    contents = (base / 'work' / source).read_text()
    assert 'BUILD SUCCESSFUL' in contents, source
    if 'client' in target:
        for phase in ['R0', 'P1', 'P2', 'P3']:
            assert 'WILDCRAFT ' + phase in contents, (source, phase)
        assert 'moved wrongly' not in contents and 'moved too quickly' not in contents
server_log = (base / 'work/p3-packaged-server.log').read_text()
for text in ['Env=SERVER', 'wildcraft 0.1.0-dev.4', 'Done (',
             'There are 0 of a max of 2 players online', 'Stopping server', 'Saving worlds']:
    assert text in server_log, text

xml = runtime / 'run/gameTest/reports/gametest-results.xml'
results = ET.parse(xml)
assert len(results.findall('.//testcase')) == 20
assert not results.findall('.//failure') and not results.findall('.//error')
assert len([case for case in results.findall('.//testcase') if case.get('name').startswith('wildcraft-test:')]) == 19

evidence.mkdir(parents=True, exist_ok=True)
for target, source in logs.items():
    shutil.copyfile(base / 'work' / source, evidence / target)
shutil.copyfile(xml, evidence / 'gametest-results.xml')
screens = runtime / 'run/packaged-client-test/screenshots'
for name in ['paraglider-equipment-slot-zh', 'paraglider-creative-slot-zh',
             'paraglider-gliding-zh', 'climb-to-glide', 'gliding-tcp-en']:
    matches = [p for p in screens.glob('*_p3-' + name + '.png') if not p.name.startswith('._')]
    assert len(matches) == 1, name
    shutil.copyfile(matches[0], evidence / (name + '.png'))

assert subprocess.check_output(['git', 'status', '--porcelain'], cwd=repo) == b''
commit = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=repo, text=True).strip()
tag = 'v0.1.0-dev.4+mc26.3'
assert subprocess.check_output(['git', 'rev-list', '-n', '1', tag], cwd=repo, text=True).strip() == commit
jar = out / 'wildcraft-0.1.0-dev.4+mc26.3.jar'
shutil.copyfile(repo / 'dist/wildcraft-0.1.0-dev.4.jar', jar)
with zipfile.ZipFile(jar) as archive:
    assert json.loads(archive.read('fabric.mod.json'))['version'] == '0.1.0-dev.4'
    assert not any('/test/' in name or '/research/' in name or '/._' in name for name in archive.namelist())

source_zip = out / 'Wildcraft-P3-source.zip'
tracked = subprocess.check_output(['git', 'ls-files', '--stage', '-z'], cwd=repo).split(b'\0')
with zipfile.ZipFile(source_zip, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
    for entry in tracked:
        if not entry:
            continue
        header, relative = entry.split(b'\t', 1)
        path = relative.decode()
        assert not any(part.startswith('._') for part in Path(path).parts)
        assert not any(part in ['build', '.gradle', 'dist', 'run', '.git', '.idea'] for part in Path(path).parts)
        info = zipfile.ZipInfo('wildcraft/' + path, date_time=(2026, 10, 2, 0, 0, 0))
        info.create_system = 3
        info.external_attr = int(header.split()[0], 8) << 16
        info.compress_type = zipfile.ZIP_DEFLATED
        archive.writestr(info, (repo / path).read_bytes(), compresslevel=9)
with zipfile.ZipFile(source_zip) as archive:
    assert archive.testzip() is None
    for executable in ['wildcraft/gradlew', 'wildcraft/dev.sh']:
        assert archive.getinfo(executable).external_attr >> 16 & 0o111

files = [summary(p) for p in sorted(evidence.iterdir()) if p.is_file() and not p.name.startswith('._')]
assert len(files) == 11
manifest = {
    'project': 'Wildcraft', 'phase': 'P3', 'mod_version': '0.1.0-dev.4', 'minecraft': '26.3',
    'git_commit': commit, 'git_tag': tag, 'publication': 'local only',
    'artifacts': [summary(jar), summary(source_zip)],
    'verification': {
        'date': '2026-10-02', 'directory': 'verification/p3',
        'numerical_examples': 13, 'numerical_bound_monotonicity_cases': 1000,
        'project_gametests_passed': 19, 'builtin_gametests_passed': 1,
        'development_client_passed': True, 'final_packaged_client_passed': True,
        'separate_packaged_server_passed': True, 'loopback_tcp_allow_flight': False,
        'separate_packaged_server_allow_flight': False, 'remote_ci_run': False,
        'saved_stamina_schema_version': 1, 'saved_glider_equipment_schema_version': 1,
        'production_dependencies_added': [], 'files': files
    },
    'history': [previous]
}
(out / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
checksums = out / 'SHA256SUMS.txt'
old_checksums = checksums.read_text()
for line in old_checksums.splitlines():
    checksum, file = line.split('  ', 1)
    assert hashlib.sha256((out / file).read_bytes()).hexdigest() == checksum
checksums.write_text(old_checksums.rstrip() + '\n' + ''.join(
    file['sha256'] + '  ' + file['file'] + '\n' for file in manifest['artifacts']))
print(json.dumps({'commit': commit, 'tag': tag, 'artifacts': manifest['artifacts'],
                  'evidence_files': len(files), 'source_files': len([e for e in tracked if e])}, ensure_ascii=False))
