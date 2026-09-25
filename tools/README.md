# Tools

| Script | Purpose |
|--------|---------|
| `generate_octobuddy_glb.py` | Rebuild `app/src/main/assets/models/octobuddy.glb` (CC0 procedural octopus matched to the Evil0ctopus brand PNG) |

```bash
python3 tools/generate_octobuddy_glb.py
```

Needs `numpy`. Output is Y-up, origin-centered, unit-normalized for SceneView `scaleToUnits`.
