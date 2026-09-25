#!/usr/bin/env bash
# Render OctoBuddy brand-pet frame loops with Blender (headless).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SCRIPT="$(cd "$(dirname "$0")" && pwd)/make_pet_anim.py"
PNG="${PNG:-$ROOT/app/src/main/res/drawable-nodpi/octobuddy_pet.png}"
OUT="${OUT:-$ROOT/app/src/main/assets/pet_anim}"
SIZE="${SIZE:-512}"
CLIPS="${CLIPS:-idle,feed,play,rest,tap}"
ENGINE="${ENGINE:-BLENDER_EEVEE_NEXT}"

if ! command -v blender >/dev/null 2>&1; then
  echo "error: blender not on PATH" >&2
  exit 1
fi

echo "==> Blender $(blender --version | head -1)"
echo "==> PNG  $PNG"
echo "==> OUT  $OUT"
echo "==> SIZE $SIZE  CLIPS $CLIPS"

mkdir -p "$OUT"
blender -b -P "$SCRIPT" -- --png "$PNG" --out "$OUT" --size "$SIZE" --clips "$CLIPS" --engine "$ENGINE"

# Quantize RGBA PNGs to keep APK growth reasonable (lossless-ish 8-bit)
if command -v pngquant >/dev/null 2>&1; then
  echo "==> pngquant optimize"
  find "$OUT" -type f -name 'frame_*.png' -print0 | while IFS= read -r -d '' f; do
    pngquant --force --quality=70-95 --skip-if-larger --output "$f" -- "$f" || true
  done
fi

# Summary
python3 - << PY
import os, json
out = "$OUT"
total = 0
bytes_ = 0
for clip in sorted(os.listdir(out)):
    d = os.path.join(out, clip)
    if not os.path.isdir(d):
        continue
    frames = sorted(f for f in os.listdir(d) if f.startswith("frame_") and f.endswith(".png"))
    sz = sum(os.path.getsize(os.path.join(d, f)) for f in frames)
    total += len(frames)
    bytes_ += sz
    print(f"  {clip}: {len(frames)} frames, {sz/1024:.1f} KiB")
print(f"TOTAL {total} frames, {bytes_/1024/1024:.2f} MiB")
man = os.path.join(out, "manifest.json")
if os.path.isfile(man):
    print("manifest:", open(man).read().strip())
PY

echo "==> render complete"
