#!/bin/sh
# Resource-size exports only; visual cleanup is preserved in the imagegen master.
set -eu
task_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
task_brand="$task_root/art/brand-v2/A14/v02"
for task_size in 20 32 64 128; do
    sips -z "$task_size" "$task_size" "$task_brand/source/wildcraft-brand-simple-cleaned.png" --out "$task_brand/exports/wildcraft-brand-simple-$task_size.png" >/dev/null
done
sips -z 512 512 "$task_brand/source/wildcraft-brand-detailed-original.png" --out "$task_brand/exports/wildcraft-brand-detailed-512.png" >/dev/null
cp "$task_brand/exports/wildcraft-brand-simple-128.png" "$task_root/src/main/resources/assets/wildcraft/icon.png"
cp "$task_brand/exports/wildcraft-brand-simple-20.png" "$task_root/src/main/resources/assets/wildcraft/icon-20.png"
