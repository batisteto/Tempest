# Tempest — Realistic Weather for NeoForge 1.21.1

Command-driven, server-authoritative weather: storm cells with a real develop → mature →
dissipate lifecycle, supercells/cyclones with mesocyclone buildup that can drop destructive
tornadoes, ambient wind, a radar/anemometer/siren block set, and large billboard-cloud
rendering tuned to stay visible from far away.

Target: Minecraft 1.21.1, NeoForge **21.1.251**, Java 21, Modrinth-ready.

## Build

```
./gradlew build
```
Output jar: `build/libs/tempest-1.0.0.jar`. Run `./gradlew runClient` / `runServer` to test
locally. This was written and reviewed carefully against the 1.21.1 NeoForge API from
training knowledge, but **it has not been compiled or run** — this sandbox has no network
access to Maven Central / the NeoForged Maven, so `gradlew` can't actually resolve
dependencies here. Treat the first build as the real first compile pass: if `javac` flags
a handful of signature mismatches, they'll almost certainly be in the highest-risk spots
called out below, not scattered randomly through the ~2,000 lines of Java.

## What's actually implemented (not a demo)

- **Simulation**: `WeatherManager` (per-level `SavedData`) ticks storm cells with a real
  lifecycle (intensity/radius/mesocyclone curves), steers them with simplex-noise wobble on
  top of a wind-driven heading, and auto-spawns new cells near players so storms are visible
  approaching from the horizon rather than popping in on top of you.
- **5 storm types**: Rain, Squall, Thunderstorm, Supercell, Cyclone — each with its own size,
  lightning frequency, and (for the last two) mesocyclone growth that can spawn a tornado.
- **Tornadoes**: a real `Entity` (not a particle effect) with EF0–EF5 scaling, AoE suction +
  damage, and block destruction gated by four datapack-editable block tags
  (`tempest:tornado_destructible_tier0-3`) plus the vanilla `mobGriefing` gamerule.
- **Wind**: ambient wind wanders via a slow random walk; storms add local inflow on top
  (`WindUtil`). The Anemometer block reads it live.
- **Blocks**: Anemometer (wind speed/direction), Weather Station (opens a radar screen),
  Tornado Siren (proximity alarm, redstone-style detector). All have real recipes and loot
  tables, not just creative-only items.
- **Radar screen**: top-down NEXRAD-style reflectivity view of tracked storms + a live
  tornado warning banner, reading the same sync packet used for rendering.
- **Rendering**: storm cells draw as clusters of large billboard "puffs" (procedurally
  placed, tinted by storm type/intensity, sized from real storm radius) rather than fixed
  sprites, so a supercell reads as a genuinely large structure at range. Tornadoes render as
  a tapered, twisting mesh built fresh each frame (not a static model). Vanilla's own
  rain/thunder/lightning/sky-darkening is reused as the base layer instead of reimplemented.
- **Lightning**: real vanilla `LightningBolt` entities, frequency driven by storm
  type/intensity — genuinely damaging, sets fires, not cosmetic-only.

## Deliberately deferred (per the earlier project decision to polish block/entity rendering
before the shader pass — see your `cinematic-realism-shader` project for the intended
follow-up)

**This is not a full volumetric/raymarched cloud shader.** A real screen-space raymarcher
against procedural 3D noise (the ProtoManly-style look) is a separate, large piece of work —
a custom post-processing pipeline, its own GLSL, and real in-game tuning that can only be
done by actually running the game. Building one blind, un-tested, in one pass, and calling it
finished would be a worse outcome than telling you plainly: the billboard-cloud system here
is the real, working, honest version of "visible from far away," and the raymarched pass is
the natural next step once this compiles and you've seen it in-game.

## Config (`config/tempest-common.toml`)
`autoSpawn`, `autoSpawnChance`, `maxActiveCells`, `tornadoesEnabled`, `tornadoBlockDamage`,
`tornadoEntityDamage`, `tornadoSpawnChance`, `maxEfScale`, `stormRenderDistance`,
`cloudDensity`, `particleDensity`. All documented inline in the generated file.

## Rendering (v2 rework)
- **Clouds**: vanilla's blocky clouds are switched off (config `hideVanillaClouds`, restored on logout) and replaced by soft, shaded 3D cloud meshes - sun-lit tops, dark undersides, silhouette-fading alpha, drifting with the wind, thickening to full overcast near storms.
- **Storm cells**: dark flat deck, rising towers, a rotating lowered wall cloud, a downwind-sheared anvil and a soft rain shaft.
- **Tornadoes**: smooth funnel whose shape follows EF scale (rope EF0-1, cone EF2-3, wedge EF4-5), swaying, swirling streaks, dust skirt tinted from the block underneath.
- **Rain**: long streaks stretched along their velocity, slanted by the local wind, removed at roofs. Hail uses the same wind slant.
- **Ambience**: localized fog + screen tint while inside a storm (vanilla's dimension-wide weather is locked clear on purpose).

## Commands (op-only)
- `/tempest clear` | `info` | `list`
- `/tempest preset <supercell|ef4_wedge|rope_tornado|outbreak|squall_line|cyclone>`
- `/tempest spawn storm <rain|squall|thunderstorm|supercell|cyclone>`
- `/tempest spawn tornado [0-5]`
- `/tempest wind set <mph> <degrees>`
- `/tempest autospawn <true|false>`

## Known risk areas if something doesn't compile
These are the specific spots where 1.21.1's API could plausibly differ from what's written,
in rough order of likelihood — check these first:
1. `RenderLevelStageEvent` accessors in `StormCloudRenderer` (camera/pose-stack plumbing).
2. `VertexConsumer` fluent chain (`addVertex().setColor()...`) in `StormCloudRenderer` /
   `TornadoRenderer` — this API was mid-rework across 1.20.5–1.21.x.
3. `EntityType#create(Level)` call sites (`StormCell`, `TempestCommand`) — spawn-reason
   parameters have been in flux across recent versions.
4. `LevelTickEvent.Post` in `ServerEvents` and `PacketDistributor.sendToPlayer` in the same
   file — NeoForge's tick/network event names moved during this era.

Everything else (registries, config, the block/entity/networking scaffolding, the actual
simulation logic) is lower-risk, ordinary NeoForge patterns.

## Textures
All textures are procedurally generated (Perlin/simplex noise), original, and not derived
from any other mod's assets.
