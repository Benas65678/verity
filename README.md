# Verity: Darkness & Containment (v1.2.0)

Addon for **The Verity Mod 1.9.8** (Forge 1.20.1). Requires Verity, GeckoLib 4.8.4+ (Verity already needs it).
It never touches Verity's code; it recognises Verity by registry id.

## What's in it
**Sanity** - bar in the top-right. Drains in the dark, near anomalies and near an angry Verity. Below 60% the screen edges darken,
below 40% you hear phantom knocks and whispers, below 25% a heartbeat. Golden bar border = Clarity, flickering purple = Dread.

**Items**
| Item | What it does |
|---|---|
| Sanity Tonic | Drink: +sanity, Clarity, cures Darkness and Dread |
| Splash Sanity Tonic | Throw: area sanity restore, calms Verity, burns anomalies |
| Lingering Sanity Tonic | Throw: leaves a Clarity cloud |
| Comfort Cookie | Eat for sanity, or right-click Verity to feed it (big anger drop) |
| Smile Charm | Carry it: sanity drains 50% slower, Verity a bit calmer |
| Friend Monitor | Right-click: sanity, Verity's mood + stats, anomalies nearby |
| Containment Lantern | Light it: calms Verity, repels anomalies, restores sanity |
| Smile Lamp (block) | Haven: restores sanity, calms Verity, pushes anomalies away |
| Distortion Shard | Dropped by anomalies; crafting material |
| Null Blade | +6 damage vs anomalies. Verity hates it |

**Effects:** Clarity (sanity regen, calms Verity, burns anomalies) and Dread (sanity drain, slower).

**Verity's stats** scale with its anger (0-100): up to +50% speed, +100% damage, +6 armor, +0.6 knockback resistance.
Moods: Calm < 25 < Uneasy < 50 < Agitated < 75 < Furious. Anger rises at night, with low sanity, over the days, when you
hold the Null Blade and when Verity is hurt; it falls in daylight, near lantern / lamp, with the charm, with cookies.

**Anomalies** use your Blockbench model (GeckoLib): idle / walk / jump / scare animations, glowing eyes, custom sounds.
Angrier Verity = more, tougher anomalies.

**Jumpscare finisher:** when an anomaly lands the final hit (the hit would be lethal, or you are below 35% health AND
below 35% sanity) it snaps in front of you, your camera is locked onto it, the scare animation plays with its sting, and
at the lunge (~2.8 s) you die. Kill the anomaly during those seconds, or have a Totem of Undying, to survive.
Creative/spectator players are immune. Tune or disable it in the `[jumpscare]` config section.

## New in 1.2.0
- **Insanity tiers** (Stable >=60%, Uneasy, Shaken <40%, Fractured <25%, Broken <10%): the HUD shows your tier; Fractured gives Weakness
  and darkness pulses, Broken adds Weakness II, slowness, blackouts and the dark sends anomalies after you.
- **Sanity visuals:** camera roll/sway, breathing FOV, tinted edges, tearing glitch bars, static and black flashes, all scaling with the tier.
  Fake sounds: knocks, doors, glass, a creeper hiss and footsteps creeping up behind you.
- **Hallucinations:** from Shaken down, see-through anomaly figures appear that only YOU can see. Harmless; they creep closer when
  you look away and dissolve if you stare at them. Real anomalies are also slightly see-through (config).
- **Stalker:** moves only while unseen (freezes mid-stride when you look), silent, drains sanity if you stare, hits hard.
  Spawns behind you in the dark when sanity is low. PLACEHOLDER MODEL: replace `geo/entity/stalker.geo.json`,
  `animations/entity/stalker.animation.json` (names `animation.stalker.idle` / `.walk`), `textures/entity/stalker.png` (+ `_glowmask.png`).
- **Containment Armor** (4 pieces): each piece shortens the range mobs notice you from (-15%) and cuts anomaly/stalker damage (-7%);
  the full set slowly restores sanity.
- **Anomaly Detector:** hold it; it beeps faster the closer an anomaly/stalker is. Signal meter above the hotbar, no arrow.
- **Verity Journal:** craftable; 15 pages unlock as you discover things (locked pages show ???).
- **Gifts and trust:** right-click Verity with cookies, tonics, cake, golden food, glowstone (good) or shards / rotten flesh (bad).
  Trust tiers: Stranger, Acquaintance (calms itself), Friend (leaves presents), Companion (zaps anomalies), Bonded (shields you).
  Repeating the same gift within 5 minutes gives much less. Hurting Verity costs trust.
- **Containment Facility:** generates in many overworld biomes (lobby + basement with cells, lab, vault, spawners and loot).
  It only appears in NEW chunks. `/locate structure veritydark:containment_facility` finds one.
- **Everything can be switched off:** Mods menu -> Verity: Darkness & Containment -> Config (needs Cloth Config, which Verity requires).
  Master switch + a toggle per feature. Files: `config/veritydark-common.toml`, `config/veritydark-client.toml`.
- New commands: `/veritydark stalker`, `/veritydark trust <0-100>`.

## Recipes
- Sanity Tonic: glass bottle + glowstone dust + golden carrot
- Splash: tonic + gunpowder. Lingering: splash + distortion shard + gunpowder
- Comfort Cookie (x4): 2 wheat + cocoa beans + glowstone dust
- Smile Charm: ` G / GYG / D ` (G gold ingot, Y yellow dye, D distortion shard)
- Friend Monitor: `IPI / IRI / III` (iron, glass pane, redstone)
- Null Blade: 2 distortion shards + stick (vertical)
- Smile Lamp: `GSG / SDS / GLG` (glass, glowstone dust, distortion shard, lantern)
- Containment Lantern: ` I / NLN / G ` (iron ingot, iron nuggets, lantern, glowstone dust)
- Anomaly Detector: ` D / IPI / IRI ` (distortion shard, iron, glass pane, redstone)
- Verity Journal: book + distortion shard + ink sac
- Containment armor (iron + distortion shards): helmet `DID / I I`, chestplate `I I / DID / IDI`, leggings `III / D D / I I`, boots `I I / D D`

## Commands (op)
`/veritydark list` | `status` | `sanity <n>` | `anger <n>` | `spawn` (spawns an anomaly in front of you)

## Editing the anomaly model
`blockbench/anomaly.bbmodel` is your source. After editing it run:
`python3 tools/convert_bbmodel.py blockbench/anomaly.bbmodel out anomaly`
then copy `out/anomaly.geo.json` -> `assets/veritydark/geo/entity/`, `out/anomaly.animation.json` -> `assets/veritydark/animations/entity/`,
`out/anomaly.png` -> `assets/veritydark/textures/entity/`. (`anomaly_glowmask.png` marks the glowing pixels.)

## Building
Push to GitHub; the Action builds it (JDK 17). Download the jar from the run's Artifacts.
Config: `config/veritydark-common.toml` and `config/veritydark-client.toml` (or the in-game Config button).

## Status
Written against the 1.20.1 / Forge 47 / GeckoLib 4.8.4 APIs but not compiled in the authoring sandbox. Expect to fix a typo on first build.
