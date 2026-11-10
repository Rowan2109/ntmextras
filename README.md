# MC Warfare NTM Extras

An add-on for NTM NEXT 1.0.3 on Minecraft 26.2 (NeoForge). It adds content from HBM's Nuclear Tech Mod for 1.7.10 (up to
build X5863) that NTM NEXT does not have yet. It is not made by, or affiliated with, the NTM NEXT team.

Adds the Lambda-98 rocket and its launch pad, exhaust and pneumatic pipe anchors, the network separator, ilmenite and titanium
tetrachloride, aviation grade sheeting, the Redstone-over-Radio designator, the HIMRAAL launcher and micro rockets, and four
structures. Also holds a small fix so a new world can start with Distant Horizons and Chunky both installed, and lower top
speeds for the Vectored Jetpack and the Boostpack (2026-11-09).

## Licence and credits

LGPL-3.0-only, see LICENSE and LICENSE.LESSER. The content, models and textures come from HBM's Nuclear Tech Mod by HbmMods
(The Bobcat) and its contributors. The add-on builds on NTM NEXT by movblock (https://github.com/Warfactory-Official/ntm-next)
and follows the layout of NTM NEXT: Space.

Changes from upstream 1.7.10, 2026-11-02: ported to Minecraft 26.2 and the NTM NEXT APIs; the changes that affect play are noted
in the code.

## Build

Requires JDK 25: `./gradlew jar`. `tools/` holds the structure converter and the mesh compiler with its model sources.

When NTM NEXT updates: bump `hbm_version`, re-sync `src/main/resources/data/hbm/worldgen/structure_set/structures.json` and
`data/hbm/recipe/centrifuge/ore_titanium*.json` with NTM's own files, and drop anything NTM NEXT now has itself.