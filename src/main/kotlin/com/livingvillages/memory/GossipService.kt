package com.livingvillages.memory

import com.livingvillages.config.ServerConfig
import com.livingvillages.data.DialogueLoader
import com.livingvillages.event.GossipEvent
import com.livingvillages.event.InteractionType
import com.livingvillages.event.LVEventBus
import com.livingvillages.identity.VillagerIdentity
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.phys.AABB
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Handles gossip propagation between villagers.
 *
 * Gossip rules (from codex section 1.4):
 * - Two villagers must both be initialized and within gossip range
 * - Maximum one gossip exchange per pair per in-game day
 * - Speaker selects their strongest recent memory about a subject
 * - Honesty axis affects accuracy, warmth axis affects spin
 * - Listener receives as GOSSIP type with 30% magnitude reduction
 * - Player overhears if within 6 blocks
 */
object GossipService {
    private val logger = LoggerFactory.getLogger("LivingVillages/Gossip")

    // Track which pairs have gossiped today (reset on new day)
    // Key: "uuid1:uuid2" where uuid1 < uuid2 lexicographically
    private val gossipedToday = mutableSetOf<String>()
    private var lastDayTracked = -1L

    /**
     * Run a gossip round for all initialized villagers in a level.
     * Called periodically or via /lv gossip trigger.
     */
    fun runGossipRound(level: ServerLevel): Int {
        val currentDay = level.dayTime / 24000L
        if (currentDay != lastDayTracked) {
            gossipedToday.clear()
            lastDayTracked = currentDay
        }

        val gossipRange = ServerConfig.gossipRange.get()
        var gossipCount = 0

        // Collect all initialized villagers in the level
        val villagers = mutableListOf<Villager>()
        for (entity in level.allEntities) {
            if (entity is Villager && entity.getData(VillagerIdentity.VILLAGER_IDENTITY.get()).initialized) {
                villagers.add(entity)
            }
        }

        for (speaker in villagers) {
            val nearbyBox = speaker.boundingBox.inflate(gossipRange)
            val nearbyVillagers = level.getEntitiesOfClass(Villager::class.java, nearbyBox)
                .filter { it !== speaker && it.getData(VillagerIdentity.VILLAGER_IDENTITY.get()).initialized }

            for (listener in nearbyVillagers) {
                if (tryGossip(level, speaker, listener)) {
                    gossipCount++
                }
            }
        }

        if (gossipCount > 0) {
            logger.debug("Gossip round: {} exchanges in {}", gossipCount, level.dimension().location())
        }
        return gossipCount
    }

    /**
     * Attempt a gossip exchange between speaker and listener.
     * Returns true if gossip occurred.
     */
    private fun tryGossip(level: ServerLevel, speaker: Villager, listener: Villager): Boolean {
        val pairKey = makePairKey(speaker.uuid, listener.uuid)
        if (pairKey in gossipedToday) return false

        val speakerIdentity = speaker.getData(VillagerIdentity.VILLAGER_IDENTITY.get())
        val listenerIdentity = listener.getData(VillagerIdentity.VILLAGER_IDENTITY.get())

        // Speaker needs something to gossip about
        val speakerMemory = speakerIdentity.memory
        val subjects = speakerMemory.getSubjects()
        if (subjects.isEmpty()) return false

        // Pick a subject to gossip about (strongest memory)
        var bestSubject: UUID? = null
        var bestRecord: InteractionRecord? = null
        for (subject in subjects) {
            if (subject == listener.uuid) continue // don't gossip about the listener to them
            val record = speakerMemory.getStrongestRecentMemory(subject) ?: continue
            if (bestRecord == null || kotlin.math.abs(record.sentimentWeight * record.magnitude) >
                kotlin.math.abs(bestRecord.sentimentWeight * bestRecord.magnitude)
            ) {
                bestSubject = subject
                bestRecord = record
            }
        }

        if (bestSubject == null || bestRecord == null) return false

        // Apply personality filter
        val filteredSentiment = filterSentiment(
            originalSentiment = bestRecord.sentimentWeight,
            speakerHonesty = speakerIdentity.personality.honesty,
            speakerWarmth = speakerIdentity.personality.warmth,
        )

        val magnitudeDecay = ServerConfig.gossipMagnitudeDecay.get().toFloat()
        val gossipRecord = InteractionRecord(
            type = InteractionType.GOSSIP,
            timestamp = level.gameTime,
            sentimentWeight = filteredSentiment,
            magnitude = bestRecord.magnitude * magnitudeDecay,
            witnessed = false,
        )

        // Add to listener's memory — need to create new identity with updated memory
        val updatedListenerMemory = listenerIdentity.memory
        updatedListenerMemory.addMemory(bestSubject, gossipRecord)
        // We mutate the memory in place (it's server-only, no sync needed for memory data)

        gossipedToday.add(pairKey)

        // Fire internal event
        LVEventBus.post(GossipEvent(
            speakerUUID = speaker.uuid,
            listenerUUID = listener.uuid,
            subjectUUID = bestSubject,
            originalSentiment = bestRecord.sentimentWeight,
            transmittedSentiment = filteredSentiment,
        ))

        // Player overhearing
        notifyNearbyPlayers(level, speaker, listener, bestSubject, filteredSentiment)

        logger.trace(
            "Gossip: {} told {} about {} (original={}, transmitted={})",
            speakerIdentity.name, listenerIdentity.name, bestSubject,
            String.format("%.2f", bestRecord.sentimentWeight),
            String.format("%.2f", filteredSentiment),
        )

        return true
    }

    /**
     * Filter sentiment through speaker's personality.
     * Honesty affects accuracy (low = random drift), warmth affects spin (high = softens negative).
     */
    private fun filterSentiment(originalSentiment: Float, speakerHonesty: Float, speakerWarmth: Float): Float {
        var sentiment = originalSentiment

        // Honesty: low honesty adds random drift
        // At honesty = -1.0, drift up to ±0.3; at honesty = 1.0, drift ≈ 0
        val maxDrift = 0.3f * ((1f - speakerHonesty) / 2f)
        if (maxDrift > 0.01f) {
            sentiment += (Math.random().toFloat() * 2f - 1f) * maxDrift
        }

        // Warmth: high warmth softens negative sentiment, low warmth amplifies it
        if (sentiment < 0f) {
            // warmth = 1.0 → multiply negative by 0.7 (soften)
            // warmth = -1.0 → multiply negative by 1.3 (amplify)
            val warmthFactor = 1f - speakerWarmth * 0.3f
            sentiment *= warmthFactor
        }

        return sentiment.coerceIn(-1f, 1f)
    }

    /**
     * If a player is within overhear range, send them a chat message with the gossip.
     */
    private fun notifyNearbyPlayers(
        level: ServerLevel,
        speaker: Villager,
        listener: Villager,
        subjectUUID: UUID,
        sentiment: Float,
    ) {
        val overhearRange = ServerConfig.playerOverhearRange.get()
        val speakerPos = speaker.position()
        val overhearBox = AABB.ofSize(speakerPos, overhearRange * 2, overhearRange * 2, overhearRange * 2)
        val nearbyPlayers = level.getEntitiesOfClass(ServerPlayer::class.java, overhearBox)

        if (nearbyPlayers.isEmpty()) return

        val speakerName = speaker.getData(VillagerIdentity.VILLAGER_IDENTITY.get()).name
        val listenerName = listener.getData(VillagerIdentity.VILLAGER_IDENTITY.get()).name

        // Pick a dialogue template
        val category = when {
            sentiment > 0.2f -> "positive"
            sentiment < -0.2f -> "negative"
            else -> "neutral"
        }

        val message = pickDialogueTemplate(speakerName, listenerName, category)

        for (player in nearbyPlayers) {
            player.sendSystemMessage(Component.literal("§7§o$message§r"))
        }
    }

    private fun pickDialogueTemplate(speaker: String, listener: String, sentimentHint: String): String {
        val allTemplates = DialogueLoader.getAll().values
        // Filter templates by sentiment category, fall back to all if no match
        val categoryMatched = allTemplates.filter { it.category == sentimentHint }.flatMap { it.templates }
        val pool = categoryMatched.ifEmpty { allTemplates.flatMap { it.templates } }
        if (pool.isEmpty()) {
            return "$speaker whispered something to $listener."
        }
        val template = pool.random()
        return template
            .replace("{speaker}", speaker)
            .replace("{listener}", listener)
            .replace("{subject}", "the outsider")
    }

    private fun makePairKey(a: UUID, b: UUID): String {
        val sa = a.toString()
        val sb = b.toString()
        return if (sa < sb) "$sa:$sb" else "$sb:$sa"
    }

    /** Clear daily gossip tracking. Used in testing. */
    fun resetDaily() {
        gossipedToday.clear()
        lastDayTracked = -1L
    }
}
