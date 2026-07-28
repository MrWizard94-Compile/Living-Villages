package com.livingvillages.event

import java.util.UUID

/**
 * Internal mod events for cross-module communication.
 * These are NOT NeoForge events — they use our lightweight internal bus.
 */
sealed interface LVEvent

/** Fired when a player interacts with a villager in a reputation-relevant way. */
data class VillagerInteractionEvent(
    val playerUUID: UUID,
    val villagerUUID: UUID,
    val type: InteractionType,
    val sentimentWeight: Float,
    val magnitude: Float,
    val witnessed: Boolean,
    val tick: Long,
) : LVEvent

enum class InteractionType {
    TRADE, GIFT, THEFT, RAID_DEFENSE, ATTACK, TALK, CURE, VISIT,
    KILL, CROP_STEAL, BLOCK_BREAK, MOB_LURE, PRESENCE, GOSSIP
}

/** Fired when a village's vitality score changes enough to cross a tier boundary. */
data class VillageStateChangeEvent(
    val villageId: UUID,
    val oldVitality: Float,
    val newVitality: Float,
) : LVEvent

/** Fired when a player's reputation tier changes with a specific villager. */
data class ReputationChangeEvent(
    val playerUUID: UUID,
    val villagerUUID: UUID,
    val oldTier: ReputationTier,
    val newTier: ReputationTier,
) : LVEvent

enum class ReputationTier(val minOpinion: Float, val maxOpinion: Float) {
    HOSTILE(-1.0f, -0.7f),
    UNWELCOME(-0.7f, -0.3f),
    STRANGER(-0.3f, 0.1f),
    ACQUAINTANCE(0.1f, 0.4f),
    TRUSTED(0.4f, 0.7f),
    FAMILY(0.7f, 1.0f);

    companion object {
        fun fromOpinion(opinion: Float): ReputationTier =
            entries.lastOrNull { opinion >= it.minOpinion } ?: HOSTILE
    }
}

/** Fired when gossip propagates between two villagers. */
data class GossipEvent(
    val speakerUUID: UUID,
    val listenerUUID: UUID,
    val subjectUUID: UUID, // player or villager being gossiped about
    val originalSentiment: Float,
    val transmittedSentiment: Float,
) : LVEvent

/** Fired on the periodic maintenance tick for a village. */
data class MaintenanceTickEvent(
    val villageId: UUID,
    val vitality: Float,
) : LVEvent

/** Village vitality tiers as defined in the codex. */
enum class VitalityState(val minVitality: Float, val maxVitality: Float) {
    DECLINING(0.0f, 0.2f),
    STRUGGLING(0.2f, 0.4f),
    STABLE(0.4f, 0.6f),
    THRIVING(0.6f, 0.8f),
    FLOURISHING(0.8f, 1.0f);

    companion object {
        fun fromVitality(vitality: Float): VitalityState =
            entries.lastOrNull { vitality >= it.minVitality } ?: DECLINING
    }
}
