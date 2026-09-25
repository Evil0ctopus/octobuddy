#!/usr/bin/env python3
"""
Generate OctoBuddy's SceneView pet GLB to match the Evil0ctopus brand mark.

Original procedural work (CC0 1.0). Colors sampled from
app/src/main/res/drawable-nodpi/octobuddy_pet.png (navy / cyan / copper).

Usage:
  python3 tools/generate_octobuddy_glb.py
  # writes app/src/main/assets/models/octobuddy.glb

Silhouette goals (vs 2D icon):
  - Bulbous upright mantle, slightly peaked
  - Head-on face with slanted glowing cyan eyes + subtle smirk
  - Eight radiating tentacles with curled tips (spread, not pillars)
  - Cyan ring spots on mantle / outer tentacles
  - Copper undersides with sucker discs
  - Slightly oversized head/eyes so Hatchling→Adult scale still reads cute
"""
from __future__ import annotations

import json
import math
import struct
from pathlib import Path

import numpy as np

# Brand palette sampled from octobuddy_pet.png (navy/cyan/copper).
# Lifted slightly from raw sample means so the low-poly mesh reads on-device.
NAVY = np.array([0.18, 0.32, 0.46], dtype=np.float32)
NAVY_HI = np.array([0.32, 0.48, 0.62], dtype=np.float32)
NAVY_LO = np.array([0.08, 0.14, 0.20], dtype=np.float32)
CYAN = np.array([0.40, 0.95, 1.00], dtype=np.float32)
CYAN_RING = np.array([0.30, 0.88, 0.96], dtype=np.float32)
CYAN_CORE = np.array([0.04, 0.10, 0.14], dtype=np.float32)
COPPER = np.array([0.72, 0.55, 0.38], dtype=np.float32)
COPPER_LO = np.array([0.50, 0.36, 0.24], dtype=np.float32)
SUCKER = np.array([0.82, 0.65, 0.46], dtype=np.float32)
SMIRK = np.array([0.06, 0.10, 0.14], dtype=np.float32)

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app/src/main/assets/models/octobuddy.glb"


def ico_sphere(subdiv: int = 2, radius: float = 1.0):
    t = (1.0 + math.sqrt(5.0)) / 2.0
    raw = [
        (-1, t, 0), (1, t, 0), (-1, -t, 0), (1, -t, 0),
        (0, -1, t), (0, 1, t), (0, -1, -t), (0, 1, -t),
        (t, 0, -1), (t, 0, 1), (-t, 0, -1), (-t, 0, 1),
    ]
    verts = [np.array(v, dtype=np.float64) for v in raw]
    faces = [
        (0, 11, 5), (0, 5, 1), (0, 1, 7), (0, 7, 10), (0, 10, 11),
        (1, 5, 9), (5, 11, 4), (11, 10, 2), (10, 7, 6), (7, 1, 8),
        (3, 9, 4), (3, 4, 2), (3, 2, 6), (3, 6, 8), (3, 8, 9),
        (4, 9, 5), (2, 4, 11), (6, 2, 10), (8, 6, 7), (9, 8, 1),
    ]
    faces = [list(f) for f in faces]

    def mid(i, j, cache):
        key = tuple(sorted((i, j)))
        if key in cache:
            return cache[key]
        verts.append((verts[i] + verts[j]) * 0.5)
        cache[key] = len(verts) - 1
        return cache[key]

    for _ in range(subdiv):
        cache = {}
        new_faces = []
        for i, j, k in faces:
            a, b, c = mid(i, j, cache), mid(j, k, cache), mid(k, i, cache)
            new_faces += [[i, a, c], [j, b, a], [k, c, b], [a, b, c]]
        faces = new_faces

    positions, normals = [], []
    for v in verts:
        n = v / np.linalg.norm(v)
        positions.append(n * radius)
        normals.append(n)
    return (
        np.array(positions, dtype=np.float32),
        np.array(normals, dtype=np.float32),
        np.array(faces, dtype=np.uint32),
    )


def torus_ring(major=0.08, minor=0.018, major_seg=16, minor_seg=8):
    positions, normals, faces = [], [], []
    for i in range(major_seg):
        u = 2 * math.pi * i / major_seg
        for j in range(minor_seg):
            v = 2 * math.pi * j / minor_seg
            cx = math.cos(u) * major
            cz = math.sin(u) * major
            nx = math.cos(u) * math.cos(v)
            ny = math.sin(v)
            nz = math.sin(u) * math.cos(v)
            positions.append([cx + minor * nx, minor * ny, cz + minor * nz])
            normals.append([nx, ny, nz])
    for i in range(major_seg):
        i2 = (i + 1) % major_seg
        for j in range(minor_seg):
            j2 = (j + 1) % minor_seg
            a = i * minor_seg + j
            b = i * minor_seg + j2
            c = i2 * minor_seg + j
            d = i2 * minor_seg + j2
            faces.append([a, c, b])
            faces.append([b, c, d])
    return (
        np.array(positions, dtype=np.float32),
        np.array(normals, dtype=np.float32),
        np.array(faces, dtype=np.uint32),
    )


def disc(radius=0.05, segments=12):
    positions = [[0.0, 0.0, 0.0]]
    normals = [[0.0, 0.0, 1.0]]
    faces = []
    for s in range(segments):
        ang = 2 * math.pi * s / segments
        positions.append([math.cos(ang) * radius, math.sin(ang) * radius, 0.0])
        normals.append([0.0, 0.0, 1.0])
    for s in range(segments):
        faces.append([0, 1 + s, 1 + (s + 1) % segments])
    return (
        np.array(positions, dtype=np.float32),
        np.array(normals, dtype=np.float32),
        np.array(faces, dtype=np.uint32),
    )


def rotation_matrix(axis, angle):
    axis = np.array(axis, dtype=np.float64)
    axis = axis / (np.linalg.norm(axis) + 1e-12)
    x, y, z = axis
    c, s = math.cos(angle), math.sin(angle)
    C = 1 - c
    return np.array([
        [c + x * x * C, x * y * C - z * s, x * z * C + y * s],
        [y * x * C + z * s, c + y * y * C, y * z * C - x * s],
        [z * x * C - y * s, z * y * C + x * s, c + z * z * C],
    ], dtype=np.float64)


def xform(pos, normals, R, translate):
    p = (pos.astype(np.float64) @ R.T) + np.asarray(translate, dtype=np.float64)
    n = normals.astype(np.float64) @ R.T
    norms = np.linalg.norm(n, axis=1, keepdims=True)
    norms[norms < 1e-8] = 1
    return p.astype(np.float32), (n / norms).astype(np.float32)


def align_y_to(direction):
    """Rotation that maps +Y to unit direction."""
    d = np.asarray(direction, dtype=np.float64)
    d = d / (np.linalg.norm(d) + 1e-12)
    default = np.array([0.0, 1.0, 0.0])
    axis = np.cross(default, d)
    if np.linalg.norm(axis) < 1e-8:
        return np.eye(3) if d[1] > 0 else rotation_matrix([1, 0, 0], math.pi)
    ang = math.acos(float(np.clip(np.dot(default, d), -1, 1)))
    return rotation_matrix(axis, ang)


def align_z_to(direction):
    d = np.asarray(direction, dtype=np.float64)
    d = d / (np.linalg.norm(d) + 1e-12)
    default = np.array([0.0, 0.0, 1.0])
    axis = np.cross(default, d)
    if np.linalg.norm(axis) < 1e-8:
        return np.eye(3) if d[2] > 0 else rotation_matrix([0, 1, 0], math.pi)
    ang = math.acos(float(np.clip(np.dot(default, d), -1, 1)))
    return rotation_matrix(axis, ang)


def tube_along_path(points, radii, segments=12):
    points = np.asarray(points, dtype=np.float64)
    n_pts = len(points)
    tangents = np.zeros_like(points)
    for i in range(n_pts):
        if i == 0:
            t = points[1] - points[0]
        elif i == n_pts - 1:
            t = points[-1] - points[-2]
        else:
            t = points[i + 1] - points[i - 1]
        tangents[i] = t / (np.linalg.norm(t) + 1e-12)

    up = np.array([0.0, 1.0, 0.0])
    if abs(np.dot(tangents[0], up)) > 0.9:
        up = np.array([1.0, 0.0, 0.0])
    normals_f, binormals = [], []
    n0 = np.cross(tangents[0], up)
    n0 /= np.linalg.norm(n0) + 1e-12
    b0 = np.cross(tangents[0], n0)
    normals_f.append(n0)
    binormals.append(b0)
    for i in range(1, n_pts):
        v = np.cross(tangents[i - 1], tangents[i])
        c = np.dot(tangents[i - 1], tangents[i])
        if np.linalg.norm(v) < 1e-8:
            n = normals_f[-1]
            b = binormals[-1]
        else:
            axis = v / np.linalg.norm(v)
            ang = math.atan2(np.linalg.norm(v), c)
            R = rotation_matrix(axis, ang)
            n = R @ normals_f[-1]
            b = np.cross(tangents[i], n)
            b /= np.linalg.norm(b) + 1e-12
            n = np.cross(b, tangents[i])
        normals_f.append(n)
        binormals.append(b)

    positions, normals = [], []
    for i in range(n_pts):
        rr = float(radii[i])
        for s in range(segments):
            ang = 2 * math.pi * s / segments
            offset = math.cos(ang) * normals_f[i] * rr + math.sin(ang) * binormals[i] * rr
            positions.append(points[i] + offset)
            normals.append(offset / (np.linalg.norm(offset) + 1e-12))

    faces = []
    for i in range(n_pts - 1):
        for s in range(segments):
            i0 = i * segments + s
            i1 = i * segments + (s + 1) % segments
            i2 = (i + 1) * segments + s
            i3 = (i + 1) * segments + (s + 1) % segments
            faces.append([i0, i2, i1])
            faces.append([i1, i2, i3])

    tip = len(positions)
    positions.append(points[-1])
    normals.append(tangents[-1])
    last = (n_pts - 1) * segments
    for s in range(segments):
        faces.append([tip, last + s, last + (s + 1) % segments])

    return (
        np.array(positions, dtype=np.float32),
        np.array(normals, dtype=np.float32),
        np.array(faces, dtype=np.uint32),
        np.array(normals_f),
        np.array(binormals),
        tangents,
        segments,
    )


def pad4(b: bytes) -> bytes:
    return b + b"\x00" * ((4 - (len(b) % 4)) % 4)


class MeshAccum:
    def __init__(self):
        self.buckets = {k: [] for k in ("body", "copper", "glow", "dark")}

    def add(self, bucket, pos, normals, faces, color):
        cols = np.tile(np.asarray(color, dtype=np.float32), (len(pos), 1))
        self.buckets[bucket].append((pos, normals, cols, faces.astype(np.uint32)))

    def add_graded(self, bucket, pos, normals, faces, color_fn):
        cols = np.zeros((len(pos), 3), dtype=np.float32)
        for i, p in enumerate(pos):
            cols[i] = color_fn(p)
        self.buckets[bucket].append((pos, normals, cols, faces.astype(np.uint32)))

    def add_colored(self, bucket, pos, normals, faces, colors):
        self.buckets[bucket].append(
            (pos, normals, np.asarray(colors, dtype=np.float32), faces.astype(np.uint32))
        )


def tentacle_path(i: int, n_seg: int = 22):
    """
    Icon-matched tentacle layout (head-on).

    Angles are ordered around the mantle so a front view shows the brand
    "flower" of curls: upper L/R rise beside the head, lower ones hang and
    spiral, sides sweep out. Control points are authored in a flat-ish
    icon plane (Z compressed) so SceneView spin still has depth without
    hiding limbs behind the mantle.
    """
    # (yaw_deg, y_mid, y_tip, reach, hook, hook_y)
    # hook > 0 curls tip counter-clockwise in the front view
    specs = [
        # 0 upper-right (+X, slight +Z)
        ( 28, 0.58, 0.90, 1.08,  0.50,  0.10),
        # 1 mid-right
        ( 55, 0.22, 0.00, 1.02,  0.40, -0.02),
        # 2 lower-right (toward camera-right)
        ( 72, -0.02, -0.58, 0.98,  0.22, -0.12),
        # 3 lower-left (toward camera-left)
        (108, -0.02, -0.58, 0.98, -0.22, -0.12),
        # 4 mid-left
        (125, 0.22, 0.00, 1.02, -0.40, -0.02),
        # 5 upper-left
        (152, 0.58, 0.90, 1.08, -0.50,  0.10),
        # 6 back-left
        (210, 0.35, 0.45, 0.82, -0.30,  0.04),
        # 7 back-right
        (330, 0.35, 0.45, 0.82,  0.30,  0.04),
    ]
    yaw_deg, y_mid, y_tip, reach, hook, hook_y = specs[i % 8]
    ang0 = math.radians(yaw_deg)
    perp = ang0 + math.pi / 2.0

    pts, radii = [], []
    for k in range(n_seg):
        t = k / (n_seg - 1)
        ease = t * t * (3 - 2 * t)
        tip = max(0.0, (t - 0.45) / 0.55)
        tip_e = tip * tip * (3 - 2 * tip)

        r = 0.36 + reach * ease * (1.0 - 0.30 * tip_e)
        h = hook * tip_e
        # Compress depth so front view shows the flower; keep some Z for spin
        depth = 0.55

        if t < 0.45:
            u = t / 0.45
            y = 0.12 + (y_mid - 0.12) * math.sin(u * math.pi * 0.5)
        else:
            u = (t - 0.45) / 0.55
            y = y_mid + (y_tip - y_mid) * (u * u * (3 - 2 * u))
        y += hook_y * tip_e

        x = math.cos(ang0) * r + math.cos(perp) * h
        z = (math.sin(ang0) * r + math.sin(perp) * h) * depth
        pts.append([x, y, z])
        radii.append(0.135 * (1.0 - 0.80 * ease) + 0.016)

    return np.array(pts, dtype=np.float64), radii


def build_octopus() -> MeshAccum:
    m = MeshAccum()

    # --- Mantle ---
    hp, hn, hf = ico_sphere(subdiv=3, radius=0.62)
    scale = np.diag([1.18, 1.35, 1.05]).astype(np.float64)
    hp64 = hp.astype(np.float64) @ scale
    for i, p in enumerate(hp64):
        if p[1] > 0:
            peak = (p[1] / hp64[:, 1].max()) ** 1.35
            hp64[i, 1] += 0.07 * peak
            hp64[i, 0] *= 1.0 - 0.10 * peak
            hp64[i, 2] *= 1.0 - 0.10 * peak
    hn64 = hn.astype(np.float64) @ scale
    hn64 /= np.linalg.norm(hn64, axis=1, keepdims=True).clip(1e-8)
    hp = hp64.astype(np.float32)
    hn = hn64.astype(np.float32)
    hp[:, 1] += 0.38

    def mantle_color(p):
        t = float(np.clip((p[1] - 0.05) / 1.15, 0, 1))
        z = float(np.clip((p[2] + 0.35) / 0.9, 0, 1))
        base = NAVY_LO * (1 - t) + NAVY * t
        return base * (0.85 + 0.15 * z) + NAVY_HI * (0.25 * t + 0.15 * z)

    m.add_graded("body", hp, hn, hf, mantle_color)

    # --- Eyes (slanted almond cyan) ---
    for side in (-1.0, 1.0):
        ep, en, ef = ico_sphere(subdiv=2, radius=0.17)
        ep = ep * np.array([1.65, 0.72, 0.42], dtype=np.float32)
        slant = math.radians(-38 if side < 0 else 38)
        R = rotation_matrix([0, 1, 0], side * math.radians(-6)) @ rotation_matrix([0, 0, 1], slant)
        translate = np.array([side * 0.26, 0.56, 0.52], dtype=np.float64)
        ep, en = xform(ep, en, R, translate)
        m.add("glow", ep, en, ef, CYAN)

        # Brow ridge
        bp, bn, bf = ico_sphere(subdiv=1, radius=0.065)
        bp = bp * np.array([1.7, 0.32, 0.55], dtype=np.float32)
        bp, bn = xform(bp, bn, R, translate + np.array([side * 0.02, 0.095, -0.01]))
        m.add("dark", bp, bn, bf, NAVY_LO)

    # Smirk
    for ang in np.linspace(-0.25, 0.55, 4):
        mp, mn, mf = ico_sphere(subdiv=0, radius=0.022)
        mp[:, 0] += math.sin(float(ang)) * 0.15
        mp[:, 1] += 0.32 - float(ang) * 0.045
        mp[:, 2] += 0.50
        m.add("dark", mp, mn, mf, SMIRK)

    # Beak shadow
    beak_p, beak_n, beak_f = ico_sphere(subdiv=1, radius=0.07)
    beak_p = beak_p * np.array([0.75, 0.45, 0.55], dtype=np.float32)
    beak_p[:, 1] += 0.16
    beak_p[:, 2] += 0.32
    m.add("dark", beak_p, beak_n, beak_f, NAVY_LO)

    # Cyan rings on mantle
    ring_placements = [
        (0.00, 0.98, 0.12, 0.095),
        (-0.30, 0.88, 0.00, 0.075),
        (0.30, 0.88, 0.00, 0.075),
        (-0.18, 0.72, 0.38, 0.065),
        (0.18, 0.72, 0.38, 0.065),
        (-0.38, 0.58, 0.22, 0.060),
        (0.38, 0.58, 0.22, 0.060),
        (0.00, 0.82, -0.38, 0.085),
        (-0.25, 0.65, -0.32, 0.065),
        (0.25, 0.65, -0.32, 0.065),
        (-0.12, 0.50, 0.42, 0.050),
        (0.12, 0.50, 0.42, 0.050),
    ]
    mantle_center = np.array([0.0, 0.38, 0.0])
    for x, y, z, maj in ring_placements:
        pos = np.array([x, y, z], dtype=np.float64)
        outward = pos - mantle_center
        outward /= np.linalg.norm(outward) + 1e-12
        R = align_y_to(outward)
        rp, rn, rf = torus_ring(major=maj, minor=maj * 0.24, major_seg=14, minor_seg=6)
        rp, rn = xform(rp, rn, R, pos)
        m.add("glow", rp, rn, rf, CYAN_RING)
        dp, dn, df = disc(radius=maj * 0.50, segments=10)
        dp, dn = xform(dp, dn, R, pos + outward * 0.012)
        m.add("dark", dp, dn, df, CYAN_CORE)

    # --- Eight tentacles ---
    for i in range(8):
        pts, radii = tentacle_path(i)
        tp, tn, tf, frame_n, frame_b, tangents, segs = tube_along_path(pts, radii, segments=12)

        # Split body/copper by ring angle: lower half of each ring = copper
        body_pos, body_n, body_f = [], [], []
        cop_pos, cop_n, cop_f = [], [], []
        # Rebuild per-ring: duplicate verts into two meshes is messy; color by angle instead
        colors = np.zeros((len(tp), 3), dtype=np.float32)
        n_rings = len(pts)
        for ri in range(n_rings):
            for s in range(segs):
                vi = ri * segs + s
                # angle 0 = +normal_f, pi/2 = +binormal
                # Underside ≈ where offset has negative world Y contribution
                ang = 2 * math.pi * s / segs
                # Use actual normal Y
                if tn[vi, 1] < -0.05:
                    colors[vi] = COPPER if (ri + s) % 3 else COPPER_LO
                else:
                    t = ri / max(1, n_rings - 1)
                    colors[vi] = NAVY * (1 - 0.25 * t) + NAVY_HI * (0.25 * t)
        # tip vertex
        if len(tp) > n_rings * segs:
            colors[-1] = COPPER_LO

        m.add_colored("body", tp, tn, tf, colors)

        # Explicit copper sucker discs on underside
        for k in range(2, len(pts) - 1, 1):
            # underside direction: prefer -world Y in the normal/binormal plane
            candidates = [frame_n[k], -frame_n[k], frame_b[k], -frame_b[k]]
            under = min(candidates, key=lambda v: v[1])
            under = under / (np.linalg.norm(under) + 1e-12)
            rr = float(radii[k])
            world = pts[k] + under * rr * 0.92

            if k % 2 == 1:
                dp, dn, df = disc(radius=rr * 0.62, segments=9)
                R = align_z_to(under)
                dp, dn = xform(dp, dn, R, world + under * 0.008)
                m.add("copper", dp, dn, df, SUCKER)
                # inner darker ring
                dp2, dn2, df2 = disc(radius=rr * 0.32, segments=8)
                dp2, dn2 = xform(dp2, dn2, R, world + under * 0.014)
                m.add("copper", dp2, dn2, df2, COPPER_LO)

        # Cyan rings on outer surface
        for k in (3, 7, 11):
            if k >= len(pts):
                continue
            candidates = [frame_n[k], -frame_n[k], frame_b[k], -frame_b[k]]
            outward = max(candidates, key=lambda v: v[1])  # prefer upward-ish outer
            # Prefer away from center in XZ
            radial = np.array([pts[k][0], 0.0, pts[k][2]])
            if np.linalg.norm(radial) > 1e-6:
                radial /= np.linalg.norm(radial)
                outward = max(candidates, key=lambda v: float(np.dot(v, radial)))
            outward = outward / (np.linalg.norm(outward) + 1e-12)
            maj = float(radii[k]) * 0.75
            rp, rn, rf = torus_ring(major=maj, minor=maj * 0.30, major_seg=12, minor_seg=5)
            R = align_y_to(outward)
            pos = pts[k] + outward * float(radii[k]) * 0.9
            rp, rn = xform(rp, rn, R, pos)
            m.add("glow", rp, rn, rf, CYAN_RING)

    return m


def merge_bucket(parts):
    if not parts:
        return None
    base = 0
    pos, norm, col, idx = [], [], [], []
    for p, n, c, f in parts:
        pos.append(p)
        norm.append(n)
        col.append(c)
        idx.append(f + base)
        base += len(p)
    return (
        np.concatenate(pos),
        np.concatenate(norm),
        np.concatenate(col),
        np.concatenate(idx).reshape(-1),
    )


def write_glb(mesh: MeshAccum, path: Path):
    order = ["body", "copper", "glow", "dark"]
    materials = {
        "body": {
            "name": "OctoNavy",
            "pbrMetallicRoughness": {
                "baseColorFactor": [1, 1, 1, 1],
                "metallicFactor": 0.04,
                "roughnessFactor": 0.50,
            },
            "doubleSided": True,
        },
        "copper": {
            "name": "OctoCopper",
            "pbrMetallicRoughness": {
                "baseColorFactor": [1, 1, 1, 1],
                "metallicFactor": 0.30,
                "roughnessFactor": 0.40,
            },
            "doubleSided": True,
        },
        "glow": {
            "name": "OctoCyanGlow",
            "pbrMetallicRoughness": {
                "baseColorFactor": [1, 1, 1, 1],
                "metallicFactor": 0.0,
                "roughnessFactor": 0.30,
            },
            "emissiveFactor": [0.20, 0.85, 0.95],
            "doubleSided": True,
        },
        "dark": {
            "name": "OctoDark",
            "pbrMetallicRoughness": {
                "baseColorFactor": [1, 1, 1, 1],
                "metallicFactor": 0.0,
                "roughnessFactor": 0.70,
            },
            "doubleSided": True,
        },
    }

    merged = {}
    for name in order:
        data = merge_bucket(mesh.buckets[name])
        if data is not None:
            merged[name] = data

    all_pos, all_norm, all_col, all_idx = [], [], [], []
    primitives = []
    mat_list = []
    mat_index = {}
    vert_base = 0

    for name in order:
        if name not in merged:
            continue
        pos, norm, col, indices = merged[name]
        nlen = np.linalg.norm(norm, axis=1, keepdims=True)
        nlen[nlen < 1e-8] = 1
        norm = (norm / nlen).astype(np.float32)
        if name not in mat_index:
            mat_index[name] = len(mat_list)
            mat_list.append(materials[name])
        primitives.append({
            "icount": len(indices),
            "material": mat_index[name],
        })
        all_pos.append(pos)
        all_norm.append(norm)
        all_col.append(col.astype(np.float32))
        all_idx.append(indices.astype(np.uint32) + vert_base)
        vert_base += len(pos)

    positions = np.concatenate(all_pos, axis=0)
    normals = np.concatenate(all_norm, axis=0)
    colors = np.concatenate(all_col, axis=0)
    indices = np.concatenate(all_idx, axis=0)

    mins = positions.min(axis=0)
    maxs = positions.max(axis=0)
    center = (mins + maxs) * 0.5
    positions = positions - center
    extent = float((positions.max(axis=0) - positions.min(axis=0)).max())
    positions = (positions / extent).astype(np.float32)
    # Sit slightly above origin so circular crop + bob look balanced
    positions[:, 1] -= float(positions[:, 1].min()) * 0.12
    positions[:, 0] -= float(positions[:, 0].mean())
    positions[:, 2] -= float(positions[:, 2].mean())
    positions = positions.astype(np.float32)

    pos_bytes = positions.tobytes()
    norm_bytes = normals.tobytes()
    col_bytes = colors.tobytes()
    idx_bytes = indices.tobytes()
    bin_blob = pad4(pos_bytes) + pad4(norm_bytes) + pad4(col_bytes) + pad4(idx_bytes)
    o_pos = 0
    o_norm = len(pad4(pos_bytes))
    o_col = o_norm + len(pad4(norm_bytes))
    o_idx = o_col + len(pad4(col_bytes))

    buffer_views = [
        {"buffer": 0, "byteOffset": o_pos, "byteLength": len(pos_bytes), "target": 34962},
        {"buffer": 0, "byteOffset": o_norm, "byteLength": len(norm_bytes), "target": 34962},
        {"buffer": 0, "byteOffset": o_col, "byteLength": len(col_bytes), "target": 34962},
        {"buffer": 0, "byteOffset": o_idx, "byteLength": len(idx_bytes), "target": 34963},
    ]
    accessors = [
        {
            "bufferView": 0, "componentType": 5126, "count": len(positions), "type": "VEC3",
            "max": positions.max(axis=0).tolist(), "min": positions.min(axis=0).tolist(),
        },
        {"bufferView": 1, "componentType": 5126, "count": len(normals), "type": "VEC3"},
        {"bufferView": 2, "componentType": 5126, "count": len(colors), "type": "VEC3"},
    ]
    prims_gltf = []
    running = 0
    for prim in primitives:
        accessors.append({
            "bufferView": 3,
            "byteOffset": running * 4,
            "componentType": 5125,
            "count": prim["icount"],
            "type": "SCALAR",
        })
        prims_gltf.append({
            "attributes": {"POSITION": 0, "NORMAL": 1, "COLOR_0": 2},
            "indices": len(accessors) - 1,
            "mode": 4,
            "material": prim["material"],
        })
        running += prim["icount"]

    gltf = {
        "asset": {
            "version": "2.0",
            "generator": "OctoBuddy tools/generate_octobuddy_glb.py (brand-matched, CC0)",
        },
        "scenes": [{"nodes": [0]}],
        "scene": 0,
        "nodes": [{"mesh": 0, "name": "OctoBuddy"}],
        "meshes": [{"name": "OctoBuddyMesh", "primitives": prims_gltf}],
        "materials": mat_list,
        "accessors": accessors,
        "bufferViews": buffer_views,
        "buffers": [{"byteLength": len(bin_blob)}],
    }

    json_bytes = pad4(json.dumps(gltf, separators=(",", ":")).encode("utf-8"))

    def chunk(ctype: bytes, data: bytes) -> bytes:
        return struct.pack("<I", len(data)) + ctype + data

    json_chunk = chunk(b"JSON", json_bytes)
    bin_chunk = chunk(b"BIN\x00", bin_blob)
    total = 12 + len(json_chunk) + len(bin_chunk)
    header = struct.pack("<4sII", b"glTF", 2, total)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(header + json_chunk + bin_chunk)
    print(
        f"Wrote {path} ({path.stat().st_size} bytes), "
        f"verts={len(positions)}, tris={len(indices)//3}, "
        f"materials={list(mat_index.keys())}, "
        f"extent_units~1, y=[{positions[:,1].min():.3f},{positions[:,1].max():.3f}]"
    )


def main():
    write_glb(build_octopus(), OUT)


if __name__ == "__main__":
    main()
