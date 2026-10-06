"""Read local Minecraft colors for review-only geometry; does not copy any native PNG."""
from pathlib import Path
import json,zipfile,hashlib,io
from PIL import Image
R=Path(__file__).resolve().parents[1]
JAR=Path('/Volumes/仕事/Wildcraft/开发环境/cache/gradle/caches/fabric-loom/26.3/minecraft-client.jar')
entries={}
with zipfile.ZipFile(JAR) as z:
 for name in ['assets/minecraft/textures/item/arrow.png','assets/minecraft/textures/item/diamond.png','assets/minecraft/textures/item/feather.png','assets/minecraft/textures/block/stone.png','assets/minecraft/textures/entity/projectiles/arrow.png']:
  raw=z.read(name);im=Image.open(io.BytesIO(raw)).convert('RGBA')
  entries[name]={'sha256':hashlib.sha256(raw).hexdigest(),'size':list(im.size),'pixels':list(im.getdata())}
(R/'references/native-color-reference.json').write_text(json.dumps({'referenceOnly':True,'archiveSha256':hashlib.sha256(JAR.read_bytes()).hexdigest(),'entries':entries},separators=(',',':'))+'\n')
