from pathlib import Path
import json
from PIL import Image
R=Path(__file__).resolve().parents[1];s=json.loads((R/'source/pixel-spec.json').read_text());e=['#00000000']*4096
for x,y,c in s['composite']:e[y*64+x]=c
im=Image.open(R/'exports/wind-weather-atlas.png').convert('RGBA');a=['#'+bytes(p).hex()for p in im.get_flattened_data()];assert a==e
for n in ['aseprite-pixels.json','reopened-source-pixels.json','reopened-png-pixels.json']:assert json.loads((R/'review'/n).read_text())['colors']==e,n
for t in s['entries']:
 crop=im.crop((t['x'],t['y'],t['x']+16,t['y']+16));crop.save(R/'exports'/(t['id']+'.png'));assert crop.getbbox()
assert len(set(a))==8 and all(int(v[-2:],16)in[0,255]for v in a)
final=json.loads((R/'review/aseprite-final-state.json').read_text());doc=next(d for d in final['documents']if d['active']);assert doc['layerCount']==3 and doc['frameCount']==1 and doc['width']==64 and doc['height']==64
report={'allPassed':True,'comparedPixels':4096,'roundtripComparisons':3,'nativeSize':16,'iconCount':13,'atlas':[64,64],'visibleColors':7,'alphaValues':[0,255],'editableLayers':3,'savedSourceAndPNGReopenedInAseprite':True,'atlasEqualsAuthoredPixelSpec':True,'exportRecovery':'Used fresh typed preview export after state inspection and verified every pixel. Earlier named export returned stale file or failed.'}
(R/'review/pixel-verification.json').write_text(json.dumps(report,indent=2)+'\n');print(report)
