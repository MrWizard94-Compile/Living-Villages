# Living Villages Codex v2
### Design Bible & Implementation Guide
**Author:** Rob (MrWizard94) + Corwin  
**Version:** 2.0 — Fresh Start  
**Date:** March 2026

---

## The Soul of the Mod

**One sentence:** Villages that feel like real communities full of people who remember you.

Vanilla Minecraft villagers are vending machines with legs. Every mod that tries to fix this either goes full city-builder (losing the Minecraft feel) or adds shallow cosmetics. Living Villages occupies the sweet spot — making the player *care* about villagers through emergent social behavior, organic village evolution, and a reputation system communicated entirely through the world itself, with zero UI dependency.

### Design Principles

1. **Show, don't tell.** No HUD elements, no stat screens. The player learns everything through villager behavior, environmental cues, and overheard gossip.
2. **Work with vanilla, not against it.** Hook into Minecraft's existing POI, villager AI, and structure systems. Extend them, don't replace them.
3. **Emergence over scripting.** Simple rules producing complex, surprising behavior. The player should see things we didn't explicitly program.
4. **Data-driven everything.** Names, traits, dialogue, building templates — all JSON/data packs. The mod is moddable itself.
5. **Modular architecture.** Each module is self-contained with clean interfaces. Modules can be developed, tested, and released independently.

---

## Architecture Overview

```
Module 0: Core Framework
    ├── Registration, config, data loading, event bus
    └── Foundation for all other modules

Module 1: Villager Identity
    ├── Names, personality axes, memory system
    ├── Gossip propagation
    └── The heart of the mod

Module 1.5: Villager Social Dynamics
    └── Villager-to-villager gossip and relationships (extends Module 1)

Module 2: Village Awareness
    ├── POI-based village detection
    ├── Village state (emergent from villager states)
    ├── Environmental visual cues (path tiers, decorations)
    └── Villager maintenance behavior

Module 3: Player Reputation
    ├── Action tracking (witnessed vs unwitnessed)
    ├── Reputation tiers with behavioral thresholds
    ├── Organic quest generation
    └── Depends on Modules 1 and 2

Future Modules (post-v1):
    - Village Expansion (structural growth, new buildings)
    - Economy (shops, trade networks between villages)
    - Custom Structures (data-driven building templates)
    - Inter-Village Relations (trade routes, rivalries, alliances)
```

### Technical Stack

- **Minecraft Version:** Target current stable at time of development
- **Modloader:** NeoForge (superior entity/AI hook support)
- **Language:** Kotlin (via KotlinForForge)
- **Data Format:** JSON data packs for all configurable content
- **Build System:** Gradle with NeoForge MDK

---

## Module 0: Core Framework

### Purpose
The mod skeleton. Registration, configuration, data loading infrastructure, and the internal event bus. Everything else plugs into this. Built and tested alone before any gameplay features exist.

### Components

#### 0.1 — Mod Entry Point
- Mod ID: `livingvillages`
- Standard NeoForge mod initialization
- Config loading
- Event bus registration
- Data pack loading pipeline

#### 0.2 — Configuration System
- Server-side config (gameplay tuning values)
- Client-side config (visual options, chat bubble display)
- All gameplay-affecting values exposed for server operators

#### 0.3 — Data Pack Loader
- JSON schema definitions for all data types
- Validation on load with clear error messages
- Hot-reload support for development
- Data types loaded:
  - Name pools (by culture/biome)
  - Personality trait definitions
  - Gossip dialogue templates
  - Path block progressions
  - Decoration block palettes

#### 0.4 — Internal Event Bus
- Lightweight event system for cross-module communication
- Events: `VillagerInteraction`, `VillageStateChange`, `ReputationChange`, `GossipEvent`, `MaintenanceTick`
- Modules subscribe to events they care about — no direct dependencies between gameplay modules

#### 0.5 — Saved Data Manager
- Per-world persistent data using NeoForge's saved data system
- Handles serialization/deserialization of:
  - Village registry
  - Villager identity data
  - Player reputation data
  - Memory buffers
- Backup-safe: atomic writes, corruption recovery

---

## Module 1: Villager Identity

### Purpose
The core of Living Villages. Every villager becomes a person with a name, personality, memories, and opinions. This module alone should make villages feel meaningfully different from vanilla.

### Components

#### 1.1 — Name System
- Names assigned on first encounter (not on spawn — avoids naming villagers the player never meets)
- Name pools loaded from data packs, organized by biome/culture:
  - Plains villages → English/pastoral names
  - Desert villages → Arabic/Semitic-inspired names
  - Taiga villages → Nordic-inspired names
  - Savanna villages → African-inspired names
  - Snowy villages → Slavic-inspired names
- Names displayed above villager heads (replaces "Villager" label)
- Names persist through zombification/curing cycle
- Naming system supports community-contributed name packs

#### 1.2 — Personality System

Four axes, each on a spectrum from -1.0 to 1.0:

| Axis | Low End (-1.0) | High End (1.0) | Behavioral Impact |
|------|---------------|----------------|-------------------|
| **Warmth** | Cold, distant | Friendly, welcoming | Greeting behavior, willingness to interact, trade generosity |
| **Energy** | Passive, sedentary | Active, bustling | Movement speed during free time, frequency of social interactions, work enthusiasm |
| **Courage** | Timid, avoidant | Brave, confrontational | Raid behavior, reaction to threats, willingness to explore |
| **Honesty** | Sneaky, unreliable | Straightforward, trustworthy | Gossip accuracy, trade fairness, theft reporting |

**Personality Assignment:**
- Randomly generated on villager identity creation with normal distribution (most villagers are moderate, extremes are rare)
- Slight biome bias (desert villagers trend slightly higher courage, snowy villagers trend slightly higher warmth — subtle, not stereotypical)
- Personality is fixed for life — people don't fundamentally change

**Personality Combinations Produce Archetypes:**
- High warmth + high courage = the village protector (runs toward danger, checks on neighbors)
- High warmth + low courage = the worrier (hides during raids but checks on you after)
- Low warmth + high honesty = the blunt critic (tells you exactly what they think)
- Low warmth + low honesty = the schemer (unreliable gossip, might not report theft)
- High energy + high warmth = the social butterfly (constantly visiting neighbors)
- Low energy + low warmth = the hermit (stays home, minimal interaction)

These archetypes aren't coded — they emerge naturally from the personality axes influencing behavior.

#### 1.3 — Memory System

Each villager maintains a **rolling buffer of 15 interaction records** per relationship (player and other villagers).

**Interaction Record Structure:**
```
{
  type: InteractionType,      // TRADE, GIFT, THEFT, RAID_DEFENSE, ATTACK, TALK, CURE, VISIT, etc.
  timestamp: Long,            // In-game tick
  sentimentWeight: Float,     // -1.0 (very negative) to 1.0 (very positive)
  magnitude: Float,           // 0.0 to 1.0 — how significant was this event
  witnessed: Boolean          // Did this villager see it firsthand?
}
```

**Opinion Derivation:**
- Opinion is NEVER stored as a separate value
- Calculated on demand from the memory buffer: weighted average of (sentimentWeight × magnitude), with recency bias (newer memories weighted more heavily)
- This means opinion naturally shifts as old memories roll off the buffer
- A villager you wronged 14 interactions ago is almost over it; wrong them again and it's fresh

**Memory Overflow:**
- When buffer is full, oldest memory is dropped
- Exception: memories with magnitude > 0.8 (major events like killing a villager, curing from zombie) get a "sticky" flag and are the LAST to be dropped, even if older
- This means truly impactful moments persist longer in memory

#### 1.4 — Gossip System

Villagers who have relationships share impressions of others, including the player.

**Gossip Triggers:**
- Two villagers with an established relationship (proximity-based, built over time)
- Both in downtime state (not working, not sleeping, not fleeing)
- Within 8 blocks of each other
- Maximum one gossip exchange per villager pair per in-game day

**Gossip Mechanics:**
- Speaker selects their strongest recent memory about a subject (player or other villager)
- The memory is transmitted to the listener, BUT filtered through the speaker's personality:
  - **Honesty axis** affects accuracy: high honesty → sentiment transmitted faithfully; low honesty → sentiment may be exaggerated or downplayed (random drift applied)
  - **Warmth axis** affects spin: high warmth → negative sentiment softened slightly; low warmth → negative sentiment amplified slightly
- The listener receives this as a new interaction record in their own memory buffer:
  - `type: GOSSIP`
  - `witnessed: false`
  - `magnitude` reduced by 30% (secondhand information carries less weight)
  - `sentimentWeight` as filtered by speaker's personality
- Gossip chains naturally degrade — by the time an impression travels through 3-4 villagers, it may barely resemble the original event

**Player Overhearing:**
- When gossip occurs within 6 blocks of the player, display a chat bubble above the speaking villager
- Bubble text drawn from dialogue templates in data packs
- Examples:
  - Positive: "{speaker} told {listener}: 'That traveler helped us during the last raid.'"
  - Negative: "{speaker} whispered to {listener}: 'I saw the stranger taking crops last night...'"
  - Neutral: "{speaker} mentioned to {listener}: 'The outsider has been around a lot lately.'"
- Dialogue templates support personality variation (warm villagers gossip differently than cold ones)

---

## Module 1.5: Villager Social Dynamics

### Purpose
Extends Module 1's gossip system to villager-to-villager relationships. Villagers form opinions of each other, creating an internal social fabric.

**Note:** This module is developed AFTER Module 1 is stable. It uses the same memory and gossip systems but adds villager-to-villager interaction tracking.

### Components

#### 1.5.1 — Villager Relationships
- Villagers who share workspace proximity or housing proximity build familiarity
- Familiarity unlocks gossip (see Module 1.4)
- High mutual warmth + high familiarity = friendship (visible: they seek each other out during downtime)
- Low mutual warmth + high familiarity = rivalry (visible: they avoid each other, occasional negative gossip)

#### 1.5.2 — Social Events
- Villagers with friendships occasionally gift items to each other
- Villager disputes: two low-warmth villagers near each other may argue (particle effects, sound cues, nearby villagers react)
- Celebration: when village state is thriving, villagers gather at the bell in the evening

---

## Module 2: Village Awareness

### Purpose
The village itself becomes a living entity whose state emerges from its inhabitants. Environmental visual cues communicate village health without any UI.

### Components

#### 2.1 — Village Detection (POI-Based)

**No block scanning.** We hook into Minecraft's Point of Interest (POI) system.

**Detection Logic:**
- Listen for POI registration events (bed placement, job site placement, bell placement)
- When a cluster is detected (3+ beds + 1 bell + 2+ associated villagers within a radius), register as a Living Village
- Assign a unique village ID and begin tracking
- Village boundary = convex hull of all associated POIs + buffer radius

**Initial World Load:**
- One-time POI query for all loaded chunks
- Cluster registered POIs using spatial proximity
- Register qualifying clusters as villages

**Dynamic Updates:**
- New POIs within village boundary + buffer → village grows
- POIs destroyed → village shrinks
- All POIs lost → village dissolved (archived in save data for potential re-detection)
- Player-built settlements qualify automatically when threshold is met (beds + bell + villagers)

**Performance:**
- Event-driven, not polling-based
- Village boundary recalculated only when POIs change
- Villager association cached and updated on villager AI tick

#### 2.2 — Village State

Village state is **emergent**, not tracked as a separate stat. Calculated periodically (every ~1000 ticks / ~50 seconds) from:

**Inputs:**
- Average villager opinion/mood (derived from their memory buffers)
- Villager count vs bed count (overcrowding or empty beds)
- Recent raid history (raids within last 3 in-game days)
- Food availability (farmers producing, composters active)
- Villager death count (recent deaths tank morale)

**Output — Village Vitality Score:**
A single floating-point value from 0.0 to 1.0, mapped to named states:

| Range | State | Description |
|-------|-------|-------------|
| 0.0 — 0.2 | Declining | Village is dying. Villagers may leave. |
| 0.2 — 0.4 | Struggling | Problems evident. Morale low. |
| 0.4 — 0.6 | Stable | Normal vanilla-equivalent state. |
| 0.6 — 0.8 | Thriving | Village is growing and happy. |
| 0.8 — 1.0 | Flourishing | Peak state. Unlocks advanced features. |

#### 2.3 — Environmental Visual Cues

Village vitality directly drives environmental appearance. Changes are applied by villager maintenance behavior (see 2.4), not by a global world-edit system.

**Path Progression Tiers:**

| Village State | Path Material | Description |
|---------------|---------------|-------------|
| Declining | Coarse dirt, moss, grass encroachment | Overgrown, neglected |
| Struggling | Patchy dirt paths, some wear | Functional but worn |
| Stable | Clean dirt paths | Standard vanilla appearance |
| Thriving | Gravel paths, cobblestone edges | Upgraded, well-maintained |
| Flourishing | Stone brick / brick paths, lanterns along walkways | Proper town infrastructure |

**Decorative Progression:**
- **Declining:** Broken fences, missing doors, cobwebs accumulate
- **Struggling:** Minimal decoration, functional only
- **Stable:** Vanilla-equivalent decorations
- **Thriving:** Flower pots outside doors, extra lighting, hay bales organized neatly
- **Flourishing:** Banners, fountain or well in town square, consistent aesthetic palette

**Block Palettes:**
- Defined in data packs per biome
- Each vitality tier has a palette mapping (e.g., "path_block" → coarse_dirt at Declining, gravel at Thriving, stone_bricks at Flourishing)
- Biome-specific: desert villages might use sandstone variants, taiga uses spruce-themed decorations

#### 2.4 — Villager Maintenance Behavior

Villagers are the agents of environmental change. This grounds the visual cues in believable behavior.

**Maintenance AI Task:**
- Added to villager schedule during downtime (after work, before sleep)
- Villager looks for nearby blocks that don't match current vitality tier palette
- If village is improving: villager "upgrades" blocks (replaces dirt path with gravel, places flower pots, repairs fences)
- If village is declining: maintenance skipped; a separate **decay tick** slowly degrades blocks (coarse dirt spreads, moss appears, decorations break)

**Constraints:**
- Villagers only maintain within ~16 blocks of their home bed
- One block change per villager per day (gradual, not instant)
- Requires appropriate materials in village storage (future: tied to economy module)
- For v1: materials assumed available (handwave it — the village "has resources")

**Decay System:**
- Runs independently of villager AI
- When vitality is below 0.4, random blocks in path/decoration positions have a small daily chance to downgrade one tier
- Decay rate scales inversely with vitality (lower vitality = faster decay)
- Decay only affects placed decorations and path blocks, never vanilla structures

---

## Module 3: Player Reputation

### Purpose
The player is a participant in the village, not a god. Their actions have consequences that propagate organically through the social systems established in Modules 1 and 2.

### Components

#### 3.1 — Action Tracking

Every player action near a village is evaluated for reputation impact.

**Positive Actions:**

| Action | Sentiment | Magnitude | Notes |
|--------|-----------|-----------|-------|
| Trading | +0.3 | 0.2 | Small but frequent |
| Gifting items | +0.5 | 0.4 | Higher if villager needed the item |
| Defending raid | +0.8 | 0.8 | Witnessed by all nearby villagers |
| Curing zombie villager | +1.0 | 0.9 | Near-permanent positive memory |
| Bringing requested resources | +0.7 | 0.6 | Tied to village improvement requests |
| Regular presence | +0.1 | 0.1 | Passive — "familiar face" effect |

**Negative Actions:**

| Action | Sentiment | Magnitude | Notes |
|--------|-----------|-----------|-------|
| Hitting a villager | -0.8 | 0.7 | Spreads fast through gossip |
| Stealing crops | -0.5 | 0.4 | Only if witnessed |
| Breaking village blocks | -0.4 | 0.3 | Only if witnessed |
| Leading hostile mobs in | -0.7 | 0.6 | Detected by proximity + mob source |
| Killing a villager | -1.0 | 1.0 | Sticky memory — witnesses never forget |
| Prolonged absence after trust | -0.2 | 0.2 | Slow passive decay — "they left us" |

#### 3.2 — Witnessed vs Unwitnessed

**Critical mechanic:** Actions only create memories in villagers who witness them.

- **Line of sight + proximity (16 blocks)** determines witnessing
- Nighttime actions with all villagers sleeping = unwitnessed
- A dishonest villager who witnesses theft may not gossip about it (personality-dependent)
- An honest villager who witnesses anything will spread it reliably

This creates genuine gameplay decisions. Do you steal resources at night? Trade fairly in public? The social system responds dynamically to player strategy.

#### 3.3 — Reputation Tiers

Reputation is per-villager, but we define behavioral thresholds for how individual villagers treat the player based on their derived opinion.

| Tier | Opinion Range | Villager Behavior |
|------|---------------|-------------------|
| **Hostile** | < -0.7 | Flees from player, iron golems aggro, no trades, doors close on approach |
| **Unwelcome** | -0.7 to -0.3 | Avoidance behavior, iron golems stare/follow, trade prices inflated heavily |
| **Stranger** | -0.3 to 0.1 | Default state. Neutral behavior, standard trades |
| **Acquaintance** | 0.1 to 0.4 | Greets player by name, slightly better trade prices |
| **Trusted** | 0.4 to 0.7 | Approaches with requests, unlocks better trades, warns about dangers, shares village news |
| **Family** | > 0.7 | Best prices, actively helps player (toss food during raids, follow if asked), mourns if player dies nearby |

**Village-Level Aggregate:**
- The "village reputation" is the median opinion across all villagers
- This affects village-level responses (iron golem behavior, whether the bell rings alarm when you approach)
- But individual villagers can differ — you might be Trusted by the farmer you always trade with but a Stranger to the librarian you've never spoken to

#### 3.4 — Recovery and Forgiveness

- **Hostile is recoverable, but hard.** Requires sustained positive actions over many in-game days. Gossip works against you — negative impressions keep circulating.
- **Killing a villager creates a sticky memory** in witnesses. It never falls off naturally. But it can eventually be outweighed by enough positive memories filling the buffer around it.
- **Village-wide hostility** can be recovered from by helping in a raid (large positive, witnessed by many) or curing zombie villagers.
- **Different villages are independent.** Burn your reputation in one village, start fresh in another — unless a villager migrates and carries gossip with them.

#### 3.5 — Organic Quests

Not a quest system. No quest log. No UI. Just villagers asking for things.

**Request System:**
- Trusted+ reputation villagers may approach the player during the day
- Requests are generated from village needs:
  - Village improving but lacks materials → "We've been wanting to upgrade our paths, but we need stone."
  - Recent raid damage → "Our fences were destroyed. Could you bring some wood?"
  - Farmer lost crops → "Something got into my fields. Could you bring some seeds?"
- Delivering the requested items completes the request (positive reputation, village improvement applied)
- Ignoring requests has no penalty (they just stop asking after a while)
- Completing requests can trigger village improvements that wouldn't happen otherwise (path upgrades, new decorations)

---

## Future Modules (Post-v1 Vision)

These are documented for design continuity but explicitly out of scope for initial release.

### Module 4: Village Expansion
- Thriving/Flourishing villages grow by placing new structures
- Structure templates defined in data packs
- Villagers "build" structures over multiple days (scaffolding → construction → complete)
- New structures attract new villagers
- Village layout follows organic growth patterns, not grid placement

### Module 5: Economy
- Shops with inventory and supply/demand pricing
- Villagers produce and consume resources
- Trade networks between nearby villages (caravans?)
- Player can participate in or disrupt trade

### Module 6: Custom Structures
- Full data-driven structure template system
- Community-created building packs
- Biome-appropriate architecture
- Structure requirements (population threshold, vitality level, materials available)

### Module 7: Inter-Village Relations
- Villages aware of neighboring villages
- Trade routes (visible — villagers traveling between villages)
- Rivalries and alliances
- Player actions in one village affecting relations with others
- Migration (unhappy villagers leave for better villages)

---

## Technical Notes

### Performance Budget
- Gossip checks: one pass per day per village, not per tick
- Opinion calculation: cached, invalidated when memory buffer changes
- Village vitality: recalculated every ~1000 ticks
- Maintenance behavior: one block per villager per day
- Environmental decay: one random check per village per day
- POI detection: event-driven, no polling

### Save Data Structure
```
world/data/livingvillages/
  ├── villages.dat          # Village registry (IDs, boundaries, vitality history)
  ├── villagers.dat         # Identity data (names, personalities, memory buffers)
  ├── reputation.dat        # Player reputation data (per-player, per-villager)
  └── maintenance.dat       # Block change queue and history
```

### Compatibility Considerations
- No mixin into vanilla villager AI — use NeoForge event hooks and capability system
- No vanilla behavior replacement — only additions
- Compatible with other village mods that don't override villager AI
- Data pack system allows resource pack-like community distribution

### Testing Strategy
- Module 0 tested standalone (config loads, data packs parse, events fire)
- Module 1 tested with command-driven simulation (`/lv test personality`, `/lv test gossip`)
- Module 2 tested with manual village state overrides (`/lv test vitality 0.8`)
- Module 3 tested with action simulation commands (`/lv test reputation trade 10`)
- Integration testing: full lifecycle from village detection → identity assignment → player interaction → gossip propagation → visual change

### Debug Commands
All under `/livingvillages` or `/lv`:
- `/lv info` — show nearby village data
- `/lv villager <name>` — show villager personality, memories, opinion of player
- `/lv vitality` — show village vitality score and contributing factors
- `/lv gossip trigger` — force a gossip round (testing)
- `/lv reputation` — show player's reputation tier with nearest village
- `/lv test <module> <scenario>` — run predefined test scenarios

---

## Build Order

1. **Module 0** — Get the mod loading, config parsing, data packs reading, events firing. No gameplay yet.
2. **Module 1 (Identity only)** — Villagers get names and personalities. Visible immediately in-game.
3. **Module 1 (Memory + Gossip)** — Add memory buffers and gossip. The social fabric comes alive.
4. **Module 2 (Detection + State)** — Villages are recognized and tracked. Vitality score calculated.
5. **Module 2 (Visual Cues)** — Environmental changes based on vitality. The world reflects the systems.
6. **Module 3 (Action Tracking)** — Player actions create memories. The player enters the social web.
7. **Module 3 (Reputation Tiers + Requests)** — Behavioral responses to reputation. Organic quests emerge.
8. **Module 1.5** — Villager-to-villager dynamics. The village becomes a true community.

Each step is playable and testable before the next begins.

---

*"The goal isn't to simulate a village. It's to make you care about one."*
