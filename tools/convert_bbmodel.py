"""Blockbench (.bbmodel, modded_entity) -> GeckoLib geo.json + animation.json
Applies the same coordinate conventions Blockbench's own Bedrock/GeckoLib export uses:
  bone pivot / cube pivot: x negated      cube origin x = -to.x
  rotation: [-x, -y, z]                   position keyframes: [-x, y, z]     scale: unchanged
"""
import json, sys, base64, re, os

src, outdir, ident = sys.argv[1], sys.argv[2], sys.argv[3]
d = json.load(open(src))
os.makedirs(outdir, exist_ok=True)

def num(v):
    f = float(v)
    f = round(f, 5)
    return 0.0 if f == 0 else f  # kill -0.0

def numlist(a):
    return [num(x) for x in a]

els = {e["uuid"]: e for e in d["elements"]}
groups = {g["uuid"]: g for g in d["groups"]}
res = d["resolution"]

bones = []
for node in d["outliner"]:
    g = groups[node["uuid"]]
    px, py, pz = g["origin"]
    bone = {"name": g["name"], "pivot": numlist([-px, py, pz])}
    rot = g.get("rotation") or [0, 0, 0]
    if any(abs(r) > 1e-9 for r in rot):
        bone["rotation"] = numlist([-rot[0], -rot[1], rot[2]])
    cubes = []
    for child in node["children"]:
        e = els[child]
        fx, fy, fz = e["from"]; tx, ty, tz = e["to"]
        size = [tx - fx, ty - fy, tz - fz]
        cube = {"origin": numlist([-tx, fy, fz]), "size": numlist(size), "uv": numlist(e.get("uv_offset", [0, 0]))}
        r = e.get("rotation") or [0, 0, 0]
        if any(abs(x) > 1e-9 for x in r):
            ox, oy, oz = e["origin"]
            cube["pivot"] = numlist([-ox, oy, oz])
            cube["rotation"] = numlist([-r[0], -r[1], r[2]])
        if e.get("inflate"):
            cube["inflate"] = num(e["inflate"])
        if e.get("mirror_uv"):
            cube["mirror"] = True
        cubes.append(cube)
    bone["cubes"] = cubes
    bones.append(bone)

geo = {
    "format_version": "1.12.0",
    "minecraft:geometry": [{
        "description": {
            "identifier": "geometry." + ident,
            "texture_width": res["width"], "texture_height": res["height"],
            "visible_bounds_width": 4, "visible_bounds_height": 5, "visible_bounds_offset": [0, 2, 0],
        },
        "bones": bones,
    }],
}
json.dump(geo, open(os.path.join(outdir, ident + ".geo.json"), "w"), indent=1)

# ---------------- animations ----------------
def conv(channel, p):
    x, y, z = (float(p["x"]), float(p["y"]), float(p["z"]))
    if channel == "rotation": return numlist([-x, -y, z])
    if channel == "position": return numlist([-x, y, z])
    return numlist([x, y, z])

def tkey(t):
    s = ("%.5f" % t).rstrip("0").rstrip(".")
    return s if s else "0"

anims = {}
for a in d["animations"]:
    loop = a.get("loop", "once")
    out = {"animation_length": num(a["length"])}
    if loop == "loop": out["loop"] = True
    elif loop == "hold": out["loop"] = "hold_on_last_frame"
    bones_out = {}
    for an in a["animators"].values():
        name = an["name"]
        chans = {}
        for k in an.get("keyframes", []):
            chans.setdefault(k["channel"], []).append(k)
        bone_out = {}
        for ch, kfs in chans.items():
            kfs.sort(key=lambda k: k["time"])
            if len(kfs) == 1 and abs(kfs[0]["time"]) < 1e-9:
                bone_out[ch] = conv(ch, kfs[0]["data_points"][0])
                continue
            seq = {}
            for i, k in enumerate(kfs):
                t = float(k["time"]); val = conv(ch, k["data_points"][0])
                if i == 0:
                    seq[tkey(t)] = val
                    continue
                prev = kfs[i - 1]
                pi, ci = prev.get("interpolation", "linear"), k.get("interpolation", "linear")
                if pi == "step":
                    # hold the previous value right up to this keyframe
                    seq[tkey(max(t - 0.001, float(prev["time"]) + 0.0005))] = conv(ch, prev["data_points"][0])
                    seq[tkey(t)] = val
                elif pi == "bezier" or ci == "bezier":
                    seq[tkey(t)] = {"vector": val, "easing": "easeinoutsine"}
                elif pi == "catmullrom" or ci == "catmullrom":
                    seq[tkey(t)] = {"vector": val, "easing": "catmullrom"}
                else:
                    seq[tkey(t)] = val
            bone_out[ch] = seq
        if bone_out:
            bones_out[name] = bone_out
    out["bones"] = bones_out
    anims["animation.%s.%s" % (ident, a["name"])] = out

json.dump({"format_version": "1.8.0", "animations": anims}, open(os.path.join(outdir, ident + ".animation.json"), "w"), indent=1)

# texture
t = d["textures"][0]
m = re.match(r"data:image/png;base64,(.*)", t["source"])
open(os.path.join(outdir, ident + ".png"), "wb").write(base64.b64decode(m.group(1)))
print("bones:", [b["name"] + ":" + str(len(b["cubes"])) for b in bones])
print("animations:", list(anims.keys()))
