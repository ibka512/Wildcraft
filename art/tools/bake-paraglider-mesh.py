"""Rebuild the approved mesh table, leaving the native renderer intact (no packages needed)."""
import json
from pathlib import Path
repo = Path(__file__).resolve().parents[2]
source = repo / "art/approved-v1/A03-A04-展开滑翔伞-v1/source/geometry-and-uv.json"
target = repo / "src/client/java/dev/wildcraft/client/render/ParagliderMesh.java"
data = json.loads(source.read_text())
rows = []
for part in data["parts"]:
    assert not any(part["rotation"]) and part["type"] == "mesh"
    for face in part["faces"].values():
        values = []
        for key in reversed(face["vertices"]):
            x, y, z = part["vertices"][key]
            u, v = face["uv"][key]
            values.extend((x/16, (24-y)/16, z/16, u/128, v/64))
        assert len(values) == 20
        rows.append("        {" + ", ".join(f"{value:.8f}F" for value in values) + "},")
rows[-1] = rows[-1].removesuffix(",")
s = target.read_text()
a = s.index("    private static final float[][] QUADS = {")
b = s.index("    };", a)
s = s[:a] + "    private static final float[][] QUADS = {\n" + "\n".join(rows) + "\n" + s[b:]
target.write_text(s)
print(f"Baked {len(data['parts'])} parts / {len(rows)} quads")
