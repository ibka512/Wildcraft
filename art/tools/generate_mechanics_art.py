#!/usr/bin/env python3
"""Export original mechanics prototype pixel grids; standard library only."""
from pathlib import Path
import json
import struct
import zlib

root = Path(__file__).resolve().parents[2]

def chunk(tag, data):
    return struct.pack('!I', len(data)) + tag + data + struct.pack('!I', zlib.crc32(tag + data) & 0xffffffff)

for name, folder in [('fan', 'item'), ('machine_body', 'item'), ('machine', 'entity'), ('wing', 'item'), ('wheel', 'item'), ('rocket', 'item'), ('spent_rocket', 'item'), ('spring', 'item'), ('stabilizer', 'item'), ('buoyancy', 'item')]:
    grid = json.loads((root / 'art/mechanics-v1' / (name + '.pixels.json')).read_text())
    width, height = grid['width'], grid['height']
    assert len(grid['rows']) == height and all(len(row) == width for row in grid['rows'])
    raw = b''.join(b'\0' + b''.join(bytes.fromhex(grid['palette'][p]) for p in row) for row in grid['rows'])
    png = (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', width, height, 8, 6, 0, 0, 0))
           + chunk(b'IDAT', zlib.compress(raw)) + chunk(b'IEND', b''))
    (root / 'src/main/resources/assets/wildcraft/textures' / folder / (name + '.png')).write_bytes(png)
print('Mechanics prototype grids exported')
