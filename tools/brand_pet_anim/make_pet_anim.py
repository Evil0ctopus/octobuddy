#!/usr/bin/env python3
"""
OctoBuddy brand-pet animation — Blender headless.

Imports the exact Evil0ctopus brand PNG as an emission/alpha textured plane
(preserves aspect), then animates the SAME pixels via lattice + armature
deforms (breathe, bob, tentacle sway, squash). Renders PNG RGBA frame loops.

Usage (via render.sh):
  blender -b -P make_pet_anim.py -- --png PATH --out DIR [--clips idle,feed,...]
"""

from __future__ import annotations

import argparse
import math
import os
import sys


def parse_args():
    # Blender passes script args after "--"
    argv = sys.argv
    if "--" in argv:
        argv = argv[argv.index("--") + 1 :]
    else:
        argv = []
    p = argparse.ArgumentParser()
    p.add_argument(
        "--png",
        required=True,
        help="Path to brand PNG (octobuddy_pet.png)",
    )
    p.add_argument(
        "--out",
        required=True,
        help="Output root; writes <clip>/frame_XX.png",
    )
    p.add_argument(
        "--size",
        type=int,
        default=512,
        help="Longest side in pixels (default 512)",
    )
    p.add_argument(
        "--clips",
        default="idle,feed,play,rest,tap",
        help="Comma-separated clip names",
    )
    p.add_argument(
        "--engine",
        default="BLENDER_EEVEE_NEXT",
        choices=("BLENDER_EEVEE_NEXT", "BLENDER_EEVEE", "CYCLES"),
    )
    return p.parse_args(argv)


def clear_scene():
    import bpy

    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    # orphan data
    for block in (bpy.data.meshes, bpy.data.materials, bpy.data.images, bpy.data.lattices, bpy.data.armatures):
        for b in list(block):
            block.remove(b)


def load_image(path: str):
    import bpy

    img = bpy.data.images.load(path, check_existing=True)
    img.alpha_mode = "STRAIGHT"
    # Brand source is RGB; treat as opaque so warps keep brand pixels.
    return img


def make_textured_plane(img, name: str = "BrandPet"):
    import bpy
    from mathutils import Vector

    w, h = img.size[0], img.size[1]
    aspect = w / float(h) if h else 1.0
    # Plane in XY, camera looks -Z. Height = 2, width = 2*aspect
    plane_h = 2.0
    plane_w = 2.0 * aspect

    bpy.ops.mesh.primitive_plane_add(size=1, location=(0, 0, 0))
    obj = bpy.context.active_object
    obj.name = name
    obj.scale = (plane_w / 2.0, plane_h / 2.0, 1.0)
    bpy.ops.object.transform_apply(scale=True)

    # Dense mesh for soft deform (subdivide)
    bpy.ops.object.mode_set(mode="EDIT")
    bpy.ops.mesh.subdivide(number_cuts=24)
    bpy.ops.object.mode_set(mode="OBJECT")

    # Material: Emission + Alpha (Principled with emission for EEVEE glow feel)
    mat = bpy.data.materials.new(name="BrandPetMat")
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    links = mat.node_tree.links
    nodes.clear()

    out = nodes.new("ShaderNodeOutputMaterial")
    out.location = (400, 0)
    bsdf = nodes.new("ShaderNodeBsdfPrincipled")
    bsdf.location = (100, 0)
    tex = nodes.new("ShaderNodeTexImage")
    tex.location = (-300, 0)
    tex.image = img
    tex.interpolation = "Linear"

    # Brand art: full emission so colors stay brand-true under lights-off
    links.new(tex.outputs["Color"], bsdf.inputs["Base Color"])
    if "Emission Color" in bsdf.inputs:
        links.new(tex.outputs["Color"], bsdf.inputs["Emission Color"])
    elif "Emission" in bsdf.inputs:
        links.new(tex.outputs["Color"], bsdf.inputs["Emission"])
    if "Emission Strength" in bsdf.inputs:
        bsdf.inputs["Emission Strength"].default_value = 1.0
    bsdf.inputs["Roughness"].default_value = 1.0
    if "Specular IOR Level" in bsdf.inputs:
        bsdf.inputs["Specular IOR Level"].default_value = 0.0
    elif "Specular" in bsdf.inputs:
        bsdf.inputs["Specular"].default_value = 0.0
    # RGB source → alpha 1; if PNG gains alpha later, wire it
    if img.channels >= 4 and "Alpha" in tex.outputs:
        links.new(tex.outputs["Alpha"], bsdf.inputs["Alpha"])
        mat.blend_method = "BLEND"
        if hasattr(mat, "shadow_method"):
            mat.shadow_method = "NONE"
    else:
        bsdf.inputs["Alpha"].default_value = 1.0
        mat.blend_method = "OPAQUE"

    links.new(bsdf.outputs["BSDF"], out.inputs["Surface"])
    obj.data.materials.append(mat)

    # Ensure UVs cover full image
    if not obj.data.uv_layers:
        obj.data.uv_layers.new(name="UVMap")

    return obj, plane_w, plane_h


def add_lattice(obj, name: str = "PetLattice"):
    import bpy

    bpy.ops.object.add(type="LATTICE", location=obj.location)
    lat_obj = bpy.context.active_object
    lat_obj.name = name
    lat = lat_obj.data
    lat.points_u = 5
    lat.points_v = 5
    lat.points_w = 1
    # Match plane bounds (plane is ~2x2 around origin in XY; lattice default is 1)
    # Scale lattice to cover plane with a little margin
    dim = obj.dimensions
    lat_obj.scale = (dim.x * 1.05, dim.y * 1.05, 0.2)

    # Parent-ish: add lattice modifier
    mod = obj.modifiers.new(name="Lattice", type="LATTICE")
    mod.object = lat_obj
    return lat_obj


def add_armature(obj):
    """Simple 3-bone armature: root (head), mid, tip (tentacles) for sway."""
    import bpy
    from mathutils import Vector

    bpy.ops.object.armature_add(enter_editmode=True, location=(0, 0, 0))
    arm_obj = bpy.context.active_object
    arm_obj.name = "PetArmature"
    arm = arm_obj.data
    arm.name = "PetArmatureData"

    # Clear default bone and build chain along -Y (tentacles toward bottom of image)
    # In Blender plane default: +Y is up in local after we kept XY plane facing camera?
    # Default plane is XY; image V=0 is bottom. Brand: head top (+Y), tentacles bottom (-Y).
    edit = arm.edit_bones
    for b in list(edit):
        edit.remove(b)

    root = edit.new("head")
    root.head = (0, 0.55, 0)
    root.tail = (0, 0.15, 0)

    mid = edit.new("body")
    mid.head = root.tail
    mid.tail = (0, -0.25, 0)
    mid.parent = root

    tip = edit.new("tentacles")
    tip.head = mid.tail
    tip.tail = (0, -0.95, 0)
    tip.parent = mid

    bpy.ops.object.mode_set(mode="OBJECT")

    # Parent mesh with automatic weights
    obj.select_set(True)
    arm_obj.select_set(True)
    bpy.context.view_layer.objects.active = arm_obj
    bpy.ops.object.parent_set(type="ARMATURE_AUTO")
    return arm_obj


def key_loc(obj, frame, loc):
    obj.location = loc
    obj.keyframe_insert(data_path="location", frame=frame)


def key_scale(obj, frame, scale):
    obj.scale = scale
    obj.keyframe_insert(data_path="scale", frame=frame)


def key_rot(obj, frame, euler):
    obj.rotation_euler = euler
    obj.keyframe_insert(data_path="rotation_euler", frame=frame)


def key_bone(pose_bone, frame, *, loc=None, rot=None, scale=None):
    if loc is not None:
        pose_bone.location = loc
        pose_bone.keyframe_insert(data_path="location", frame=frame)
    if rot is not None:
        pose_bone.rotation_euler = rot
        pose_bone.keyframe_insert(data_path="rotation_euler", frame=frame)
    if scale is not None:
        pose_bone.scale = scale
        pose_bone.keyframe_insert(data_path="scale", frame=frame)


def clear_animation(obj):
    if obj.animation_data:
        obj.animation_data_clear()


def deform_lattice_point(lat_obj, u, v, w, delta_xy):
    """Move lattice control point by delta in object space (u,v,w indices)."""
    lat = lat_obj.data
    # points indexed: for w, for v, for u
    idx = w * (lat.points_u * lat.points_v) + v * lat.points_u + u
    # Lattice points use normalized coords; deform via .co which is in [-0.5,0.5] local
    pt = lat.points[idx]
    pt.co_deform.x += delta_xy[0]
    pt.co_deform.y += delta_xy[1]
    # z unused for flat


def animate_idle(arm_obj, lat_obj, mesh_obj, frames: int = 32):
    """Subtle breathe (scale Y) + Z bob + tentacle sway. Loop-safe."""
    import bpy

    clear_animation(arm_obj)
    clear_animation(lat_obj)
    clear_animation(mesh_obj)

    pose = arm_obj.pose.bones
    for b in pose:
        b.rotation_mode = "XYZ"

    # Loop: frame 1 == frame frames+1 conceptually; key at 1 and frames+1 same
    for f in range(1, frames + 2):
        t = (f - 1) / float(frames) * 2.0 * math.pi
        breathe = 1.0 + 0.075 * math.sin(t)
        bob = 0.065 * math.sin(t)
        sway = 0.16 * math.sin(t * 1.0)
        sway2 = 0.11 * math.sin(t * 1.0 + 0.8)

        key_bone(pose["head"], f, scale=(1.0 / math.sqrt(breathe), breathe, 1.0), loc=(0, bob * 0.3, 0))
        key_bone(pose["body"], f, rot=(0, 0, sway * 0.25), loc=(sway * 0.02, bob * 0.5, 0))
        key_bone(
            pose["tentacles"],
            f,
            rot=(0, 0, sway),
            loc=(sway2 * 0.04, 0, 0),
            scale=(1.0 + abs(sway) * 0.05, 1.0 - abs(sway) * 0.03, 1.0),
        )
        # Whole-object gentle float
        key_loc(mesh_obj, f, (0, bob, 0))

    # Make cyclic
    for obj in (arm_obj, mesh_obj):
        if obj.animation_data and obj.animation_data.action:
            for fc in obj.animation_data.action.fcurves:
                mod = fc.modifiers.new(type="CYCLES")
                mod.mode_before = "REPEAT"
                mod.mode_after = "REPEAT"


def animate_feed(arm_obj, mesh_obj, frames: int = 16):
    """Happy nom: squash + bounce up + slight wiggle."""
    clear_animation(arm_obj)
    clear_animation(mesh_obj)
    pose = arm_obj.pose.bones
    for b in pose:
        b.rotation_mode = "XYZ"

    # Reset pose at start
    key_bone(pose["head"], 1, scale=(1, 1, 1), loc=(0, 0, 0), rot=(0, 0, 0))
    key_bone(pose["body"], 1, scale=(1, 1, 1), loc=(0, 0, 0), rot=(0, 0, 0))
    key_bone(pose["tentacles"], 1, scale=(1, 1, 1), loc=(0, 0, 0), rot=(0, 0, 0))
    key_loc(mesh_obj, 1, (0, 0, 0))
    key_scale(mesh_obj, 1, (1, 1, 1))

    # Squash
    mid = max(3, frames // 4)
    key_scale(mesh_obj, mid, (1.12, 0.82, 1))
    key_loc(mesh_obj, mid, (0, -0.06, 0))
    key_bone(pose["head"], mid, scale=(1.08, 0.9, 1))

    # Bounce up
    up = mid + max(2, frames // 5)
    key_scale(mesh_obj, up, (0.94, 1.14, 1))
    key_loc(mesh_obj, up, (0, 0.18, 0))
    key_bone(pose["tentacles"], up, rot=(0, 0, 0.15), scale=(1.05, 0.95, 1))

    # Wiggle settle
    wig = up + max(2, frames // 4)
    key_loc(mesh_obj, wig, (0.04, 0.04, 0))
    key_bone(pose["body"], wig, rot=(0, 0, -0.12))
    key_scale(mesh_obj, wig, (1.04, 0.97, 1))

    end = frames
    key_scale(mesh_obj, end, (1, 1, 1))
    key_loc(mesh_obj, end, (0, 0, 0))
    key_bone(pose["head"], end, scale=(1, 1, 1), loc=(0, 0, 0), rot=(0, 0, 0))
    key_bone(pose["body"], end, scale=(1, 1, 1), loc=(0, 0, 0), rot=(0, 0, 0))
    key_bone(pose["tentacles"], end, scale=(1, 1, 1), loc=(0, 0, 0), rot=(0, 0, 0))


def animate_play(arm_obj, mesh_obj, frames: int = 20):
    """Energetic spin-jig: rotate + hop + tentacle flourish."""
    clear_animation(arm_obj)
    clear_animation(mesh_obj)
    pose = arm_obj.pose.bones
    for b in pose:
        b.rotation_mode = "XYZ"

    key_rot(mesh_obj, 1, (0, 0, 0))
    key_loc(mesh_obj, 1, (0, 0, 0))
    key_scale(mesh_obj, 1, (1, 1, 1))
    for name in ("head", "body", "tentacles"):
        key_bone(pose[name], 1, scale=(1, 1, 1), loc=(0, 0, 0), rot=(0, 0, 0))

    # Spin almost full turn while hopping
    for i, f in enumerate([1, frames // 4, frames // 2, (3 * frames) // 4, frames]):
        ang = (i / 4.0) * math.radians(360)
        hop = 0.16 * math.sin(i / 4.0 * math.pi)
        key_rot(mesh_obj, f, (0, 0, ang if i < 4 else 0))
        key_loc(mesh_obj, f, (0, hop, 0))
        key_scale(mesh_obj, f, (1.0 + 0.06 * math.sin(i * 1.2), 1.0 - 0.04 * math.sin(i * 1.2), 1))
        key_bone(pose["tentacles"], f, rot=(0, 0, 0.25 * math.sin(i * 1.5)), scale=(1.08, 0.92, 1))
        key_bone(pose["head"], f, scale=(1.0, 1.0 + 0.05 * math.sin(i), 1))


def animate_rest(arm_obj, mesh_obj, frames: int = 18):
    """Sleepy droop: settle down, slow breathe, soft tentacle curl."""
    clear_animation(arm_obj)
    clear_animation(mesh_obj)
    pose = arm_obj.pose.bones
    for b in pose:
        b.rotation_mode = "XYZ"

    key_loc(mesh_obj, 1, (0, 0, 0))
    key_scale(mesh_obj, 1, (1, 1, 1))
    for name in ("head", "body", "tentacles"):
        key_bone(pose[name], 1, scale=(1, 1, 1), loc=(0, 0, 0), rot=(0, 0, 0))

    settle = max(4, frames // 3)
    key_loc(mesh_obj, settle, (0, -0.12, 0))
    key_scale(mesh_obj, settle, (1.06, 0.90, 1))
    key_bone(pose["head"], settle, scale=(1.02, 0.94, 1), rot=(0.08, 0, 0))
    key_bone(pose["tentacles"], settle, rot=(0.05, 0, 0.08), scale=(0.96, 1.04, 1))

    # Soft pulse while down
    mid = (settle + frames) // 2
    key_scale(mesh_obj, mid, (1.04, 0.88, 1))
    key_bone(pose["head"], mid, scale=(1.0, 0.92, 1))

    key_loc(mesh_obj, frames, (0, -0.04, 0))
    key_scale(mesh_obj, frames, (1.02, 0.96, 1))
    key_bone(pose["head"], frames, scale=(1, 0.98, 1), rot=(0.03, 0, 0), loc=(0, 0, 0))
    key_bone(pose["body"], frames, rot=(0, 0, 0), loc=(0, 0, 0), scale=(1, 1, 1))
    key_bone(pose["tentacles"], frames, rot=(0.02, 0, 0.04), scale=(0.98, 1.02, 1), loc=(0, 0, 0))


def animate_tap(arm_obj, mesh_obj, frames: int = 12):
    """Quick punch / react."""
    clear_animation(arm_obj)
    clear_animation(mesh_obj)
    pose = arm_obj.pose.bones
    for b in pose:
        b.rotation_mode = "XYZ"

    key_scale(mesh_obj, 1, (1, 1, 1))
    key_loc(mesh_obj, 1, (0, 0, 0))
    key_rot(mesh_obj, 1, (0, 0, 0))
    for name in ("head", "body", "tentacles"):
        key_bone(pose[name], 1, scale=(1, 1, 1), loc=(0, 0, 0), rot=(0, 0, 0))

    hit = max(2, frames // 4)
    key_scale(mesh_obj, hit, (1.14, 0.88, 1))
    key_rot(mesh_obj, hit, (0, 0, math.radians(18)))
    key_bone(pose["tentacles"], hit, rot=(0, 0, -0.2), scale=(1.1, 0.9, 1))
    key_bone(pose["head"], hit, scale=(1.06, 0.95, 1))

    mid = hit + max(2, frames // 3)
    key_scale(mesh_obj, mid, (0.96, 1.08, 1))
    key_rot(mesh_obj, mid, (0, 0, math.radians(-10)))
    key_loc(mesh_obj, mid, (0, 0.06, 0))

    key_scale(mesh_obj, frames, (1, 1, 1))
    key_rot(mesh_obj, frames, (0, 0, 0))
    key_loc(mesh_obj, frames, (0, 0, 0))
    for name in ("head", "body", "tentacles"):
        key_bone(pose[name], frames, scale=(1, 1, 1), loc=(0, 0, 0), rot=(0, 0, 0))


CLIP_FRAMES = {
    "idle": 32,
    "feed": 16,
    "play": 20,
    "rest": 18,
    "tap": 12,
}


def setup_camera(plane_w, plane_h, size_px: int):
    import bpy

    bpy.ops.object.camera_add(location=(0, 0, 5))
    cam = bpy.context.active_object
    cam.name = "OrthoCam"
    cam.data.type = "ORTHO"
    # Fit plane with small margin
    cam.data.ortho_scale = max(plane_w, plane_h) * 1.02
    cam.rotation_euler = (0, 0, 0)
    bpy.context.scene.camera = cam

    scene = bpy.context.scene
    # Square render matching brand
    scene.render.resolution_x = size_px
    scene.render.resolution_y = size_px
    scene.render.resolution_percentage = 100
    scene.render.film_transparent = True
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"
    scene.render.image_settings.compression = 15
    scene.render.fps = 24
    return cam


def setup_engine(engine: str):
    import bpy

    scene = bpy.context.scene
    # Prefer EEVEE Next on 4.2+
    available = {e.identifier for e in bpy.types.RenderSettings.bl_rna.properties["engine"].enum_items}
    if engine not in available:
        if "BLENDER_EEVEE_NEXT" in available:
            engine = "BLENDER_EEVEE_NEXT"
        elif "BLENDER_EEVEE" in available:
            engine = "BLENDER_EEVEE"
        else:
            engine = "CYCLES"
    scene.render.engine = engine
    print(f"[brand_pet_anim] render engine: {engine}")

    if engine.startswith("BLENDER_EEVEE"):
        ee = scene.eevee
        # Fast settings
        if hasattr(ee, "taa_render_samples"):
            ee.taa_render_samples = 16
        if hasattr(ee, "use_gtao"):
            ee.use_gtao = False
        if hasattr(ee, "use_bloom"):
            ee.use_bloom = False
    elif engine == "CYCLES":
        scene.cycles.samples = 32
        scene.cycles.use_denoising = False
        scene.cycles.device = "CPU"


def render_clip(out_dir: str, clip: str, frame_count: int):
    import bpy

    os.makedirs(out_dir, exist_ok=True)
    scene = bpy.context.scene
    scene.frame_start = 1
    scene.frame_end = frame_count
    # Clear previous frames in this folder
    for name in os.listdir(out_dir):
        if name.startswith("frame_") and name.endswith(".png"):
            os.remove(os.path.join(out_dir, name))

    for f in range(1, frame_count + 1):
        scene.frame_set(f)
        out_path = os.path.join(out_dir, f"frame_{f:02d}.png")
        scene.render.filepath = out_path
        bpy.ops.render.render(write_still=True)
        print(f"[brand_pet_anim] wrote {out_path}")


def main():
    import bpy

    args = parse_args()
    png = os.path.abspath(args.png)
    out_root = os.path.abspath(args.out)
    if not os.path.isfile(png):
        raise SystemExit(f"PNG not found: {png}")

    clips = [c.strip() for c in args.clips.split(",") if c.strip()]
    for c in clips:
        if c not in CLIP_FRAMES:
            raise SystemExit(f"Unknown clip '{c}'. Choose from {list(CLIP_FRAMES)}")

    clear_scene()
    img = load_image(png)
    mesh_obj, plane_w, plane_h = make_textured_plane(img)
    lat_obj = add_lattice(mesh_obj)
    arm_obj = add_armature(mesh_obj)
    setup_camera(plane_w, plane_h, args.size)
    setup_engine(args.engine)

    # Mild world so emission reads cleanly
    world = bpy.data.worlds.new("DarkWorld")
    bpy.context.scene.world = world
    world.use_nodes = True
    bg = world.node_tree.nodes.get("Background")
    if bg:
        bg.inputs[0].default_value = (0, 0, 0, 1)
        bg.inputs[1].default_value = 0.0

    animators = {
        "idle": lambda: animate_idle(arm_obj, lat_obj, mesh_obj, CLIP_FRAMES["idle"]),
        "feed": lambda: animate_feed(arm_obj, mesh_obj, CLIP_FRAMES["feed"]),
        "play": lambda: animate_play(arm_obj, mesh_obj, CLIP_FRAMES["play"]),
        "rest": lambda: animate_rest(arm_obj, mesh_obj, CLIP_FRAMES["rest"]),
        "tap": lambda: animate_tap(arm_obj, mesh_obj, CLIP_FRAMES["tap"]),
    }

    os.makedirs(out_root, exist_ok=True)
    for clip in clips:
        print(f"[brand_pet_anim] animating clip={clip} frames={CLIP_FRAMES[clip]}")
        animators[clip]()
        render_clip(os.path.join(out_root, clip), clip, CLIP_FRAMES[clip])

    # Manifest for the app / humans
    manifest = os.path.join(out_root, "manifest.json")
    import json

    meta = {
        "source": os.path.basename(png),
        "size": args.size,
        "engine": bpy.context.scene.render.engine,
        "clips": {c: {"frames": CLIP_FRAMES[c], "fps": 24} for c in clips},
        "technique": "brand PNG textured plane + armature/lattice deform (pixels warped, not re-meshed)",
    }
    with open(manifest, "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2)
        f.write("\n")
    print(f"[brand_pet_anim] done → {out_root}")


if __name__ == "__main__":
    main()
