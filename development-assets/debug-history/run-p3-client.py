from pathlib import Path
import json, subprocess, sys
base=Path('/Volumes/仕事/开发/2026-10-02/referenced-chatgpt-conversation-this-is-an')
repo=base/'outputs/wildcraft'
metadata=repo/'src/gametest/resources/fabric.mod.json'
original=metadata.read_bytes()
try:
    data=json.loads(original)
    data['entrypoints']['fabric-client-gametest']=['dev.wildcraft.test.GlidingClientSmokeTest']
    metadata.write_text(json.dumps(data,indent=2)+'\n')
    with (base/'work/p3-client-focused.log').open('w') as log:
        result=subprocess.run([str(repo/'dev.sh'),'runClientGameTest','-PacceptMinecraftEula=true'],cwd=repo,stdout=log,stderr=subprocess.STDOUT)
finally:
    metadata.write_bytes(original)
sys.exit(result.returncode)
