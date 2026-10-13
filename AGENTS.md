# AGENTS — Living Villages project entry

**Status:** Level 4 project instructions
**Canonical Constitution:** https://github.com/MrWizard94-Compile/Agents-Constitution
**Accepted source commit:** `ded36646c11d61bcfa7e2420b81a278dc98f47e6`

The accepted GitHub repository is the only editable WPAI Constitution source.
Load its `SKILL.md`, `AGENTS.md`, `SOP.md`, `references/always-load.md`, and
applicable `law/*` files from this verified commit. A generated mirror is
usable only when its manifest matches that SHA. `.claude/AGENTS.md` points
here for Claude compatibility; it is not another source of law.

## Project mission and stack

- [`LIVING_VILLAGES_CODEX_V2.md`](LIVING_VILLAGES_CODEX_V2.md) is the product
  design bible. Its "Soul of the Mod" is a gameplay vision, not a governance
  source. Preserve its show-don't-tell, vanilla-coexistence, emergent behavior,
  data-driven, and modular goals when implementing features.
- Use the versions actually declared in `gradle.properties` and
  `build.gradle.kts`: Minecraft 1.21.1, NeoForge 21.1.219, Kotlin 2.2.20,
  KotlinForForge 5.11.0, and Java 21. Treat the design bible's broad stack
  description as intent, not a replacement for build pins.
- Do not remove or rewrite worlds, saves, recipes, tags, data packs, or user
  configs as part of a governance migration. A clean source build is not a
  fresh game launch, world-lifecycle test, or compatibility acceptance.

## Verification and provenance

For a governance-only edit, validate the pinned Constitution source and tests,
run its `tools/check-adoption.py` against this Git root, and run
`gradlew.bat build --warning-mode all` on Windows. Reproduce gameplay changes
in a fresh NeoForge launch and relevant world lifecycle before claiming runtime
acceptance. Record the exact instance and logs used for that claim.

The retired `.claude/SOUL.md` pointer and 188-line Claude-era variant are
recoverable from Git commit `278168a64079b958d87867a3c3bc9db9af07d95f`.
The variant's SHA-256 is
`AD3137994B46B5581584B9A43049C4FB31173361D49EB1D872CCDBBCB1E2539E`;
it matches the previously reviewed Research-Crawler and Claude-Playground
variants recorded in the canonical source inventory.
