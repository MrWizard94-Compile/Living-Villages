package com.livingvillages.config

import com.livingvillages.LVConstants
import net.neoforged.neoforge.common.ModConfigSpec

/**
 * Server-side config — gameplay tuning values.
 * All gameplay-affecting values are exposed so server operators can tweak them.
 */
object ServerConfig {
    private val BUILDER = ModConfigSpec.Builder()

    // -- Memory System --
    val memoryBufferSize: ModConfigSpec.IntValue = BUILDER
        .comment("Number of interaction records each villager keeps per relationship")
        .defineInRange("memory.bufferSize", LVConstants.DEFAULT_MEMORY_BUFFER_SIZE, 5, 50)

    val stickyMagnitudeThreshold: ModConfigSpec.DoubleValue = BUILDER
        .comment("Minimum magnitude for a memory to be 'sticky' (last to be evicted)")
        .defineInRange("memory.stickyThreshold", LVConstants.DEFAULT_STICKY_MAGNITUDE_THRESHOLD.toDouble(), 0.5, 1.0)

    // -- Gossip System --
    val gossipRange: ModConfigSpec.DoubleValue = BUILDER
        .comment("Maximum block distance for gossip to occur between villagers")
        .defineInRange("gossip.range", LVConstants.DEFAULT_GOSSIP_RANGE, 4.0, 32.0)

    val gossipMagnitudeDecay: ModConfigSpec.DoubleValue = BUILDER
        .comment("Multiplier applied to magnitude when information is passed via gossip (secondhand = less weight)")
        .defineInRange("gossip.magnitudeDecay", LVConstants.DEFAULT_GOSSIP_MAGNITUDE_DECAY.toDouble(), 0.1, 1.0)

    val playerOverhearRange: ModConfigSpec.DoubleValue = BUILDER
        .comment("Maximum block distance for player to overhear villager gossip")
        .defineInRange("gossip.playerOverhearRange", 6.0, 2.0, 16.0)

    // -- Village Detection --
    val villageMinBeds: ModConfigSpec.IntValue = BUILDER
        .comment("Minimum number of beds required to register a cluster as a village")
        .defineInRange("village.minBeds", LVConstants.DEFAULT_VILLAGE_MIN_BEDS, 1, 20)

    val villageMinVillagers: ModConfigSpec.IntValue = BUILDER
        .comment("Minimum associated villagers required to register a cluster as a village")
        .defineInRange("village.minVillagers", LVConstants.DEFAULT_VILLAGE_MIN_VILLAGERS, 1, 10)

    val vitalityTickInterval: ModConfigSpec.IntValue = BUILDER
        .comment("How often (in ticks) village vitality is recalculated. 1000 = ~50 seconds")
        .defineInRange("village.vitalityTickInterval", LVConstants.DEFAULT_VITALITY_TICK_INTERVAL, 200, 6000)

    // -- Reputation --
    val witnessRange: ModConfigSpec.DoubleValue = BUILDER
        .comment("Maximum block distance + line of sight for a villager to 'witness' a player action")
        .defineInRange("reputation.witnessRange", LVConstants.DEFAULT_WITNESS_RANGE, 8.0, 48.0)

    // -- Maintenance --
    val maintenanceRange: ModConfigSpec.IntValue = BUILDER
        .comment("How far (in blocks) from their home bed a villager will perform maintenance tasks")
        .defineInRange("maintenance.range", LVConstants.DEFAULT_MAINTENANCE_RANGE, 8, 32)

    val SPEC: ModConfigSpec = BUILDER.build()
}

/**
 * Client-side config — visual/display options.
 */
object ClientConfig {
    private val BUILDER = ModConfigSpec.Builder()

    val showChatBubbles: ModConfigSpec.BooleanValue = BUILDER
        .comment("Show chat bubbles above villagers when they gossip nearby")
        .define("display.chatBubbles", true)

    val showVillagerNames: ModConfigSpec.BooleanValue = BUILDER
        .comment("Show villager names above their heads")
        .define("display.villagerNames", true)

    val chatBubbleDuration: ModConfigSpec.IntValue = BUILDER
        .comment("How long (in ticks) gossip chat bubbles remain visible. 60 = 3 seconds")
        .defineInRange("display.chatBubbleDuration", 80, 20, 200)

    val SPEC: ModConfigSpec = BUILDER.build()
}
