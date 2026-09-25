# Brand pet animation (Blender)

Headless Blender pipeline that warps the **exact** Evil0ctopus brand PNG
(`app/src/main/res/drawable-nodpi/octobuddy_pet.png`) — textured plane +
armature / lattice deform — then renders PNG RGBA frame loops for in-app playback.

## Requirements

- Blender 4.2+ (`blender` on `PATH`)
- Optional: `pngquant` for APK-friendly quantization

## Render

```bash
./tools/brand_pet_anim/render.sh
# or
SIZE=512 CLIPS=idle,feed,play,rest,tap ./tools/brand_pet_anim/render.sh
```

Writes `app/src/main/assets/pet_anim/{idle,feed,play,rest,tap}/frame_XX.png`
plus `manifest.json`.

## Clips

| Clip | Frames | Motion |
|------|--------|--------|
| idle | 32 | Breathe + bob + tentacle sway (loop) |
| feed | 16 | Squash-bounce nom |
| play | 20 | Spin jig + hop |
| rest | 18 | Sleepy droop |
| tap  | 12 | Quick punch |

Do **not** replace the brand art with a different 3D octopus mesh.
