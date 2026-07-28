package com.livingvillages.memory

import com.livingvillages.LVConstants
import com.livingvillages.event.InteractionType
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import java.util.UUID
import kotlin.math.exp
import kotlin.math.max

/**
 * A villager's memory of all their relationships.
 *
 * Maps subject UUID (player or other villager) to a rolling list of InteractionRecords.
 * Buffer size per relationship is capped at [LVConstants.DEFAULT_MEMORY_BUFFER_SIZE].
 *
 * Opinion of a subject is NEVER stored — it's derived on demand from the memory buffer
 * using a weighted average with recency bias.
 */
class MemoryBuffer {
    private val memories: MutableMap<UUID, MutableList<InteractionRecord>> = mutableMapOf()

    /**
     * Add an interaction record to this villager's memory of the given subject.
     * Evicts the oldest non-sticky record if the buffer is full.
     */
    fun addMemory(subjectUUID: UUID, record: InteractionRecord) {
        val buffer = memories.getOrPut(subjectUUID) { mutableListOf() }
        if (buffer.size >= LVConstants.DEFAULT_MEMORY_BUFFER_SIZE) {
            evictOldest(buffer)
        }
        buffer.add(record)
    }

    /**
     * Get all memories about a specific subject.
     */
    fun getMemories(subjectUUID: UUID): List<InteractionRecord> =
        memories[subjectUUID] ?: emptyList()

    /**
     * Get all subjects this villager has memories about.
     */
    fun getSubjects(): Set<UUID> = memories.keys

    /**
     * Get the strongest recent memory about a subject (highest |sentiment * magnitude|).
     * Used by gossip system to select what to talk about.
     */
    fun getStrongestRecentMemory(subjectUUID: UUID): InteractionRecord? {
        val buffer = memories[subjectUUID] ?: return null
        return buffer.maxByOrNull {
            kotlin.math.abs(it.sentimentWeight * it.magnitude)
        }
    }

    /**
     * Calculate opinion of a subject from their memory buffer.
     *
     * Opinion = weighted average of (sentimentWeight × magnitude), with recency bias.
     * Newer memories are weighted more heavily using exponential decay.
     * Returns 0.0 if no memories exist.
     */
    fun calculateOpinion(subjectUUID: UUID, currentTick: Long): Float {
        val buffer = memories[subjectUUID] ?: return 0f
        if (buffer.isEmpty()) return 0f

        // Half-life of ~6000 ticks (~5 minutes) for recency weighting
        val halfLife = 6000.0
        var weightedSum = 0.0
        var totalWeight = 0.0

        for (record in buffer) {
            val age = max(0L, currentTick - record.timestamp).toDouble()
            val recencyWeight = exp(-age * 0.693 / halfLife) // ln(2) ≈ 0.693
            val weight = recencyWeight * record.magnitude
            weightedSum += record.sentimentWeight * weight
            totalWeight += weight
        }

        return if (totalWeight > 0.0) {
            (weightedSum / totalWeight).toFloat().coerceIn(-1f, 1f)
        } else {
            0f
        }
    }

    /**
     * Count of total memories across all relationships.
     */
    fun totalMemoryCount(): Int = memories.values.sumOf { it.size }

    /**
     * Count of relationships (subjects remembered).
     */
    fun relationshipCount(): Int = memories.size

    // ---- Eviction ----

    private fun evictOldest(buffer: MutableList<InteractionRecord>) {
        // Find oldest non-sticky record to evict
        val nonSticky = buffer.filter { !it.sticky }
        if (nonSticky.isNotEmpty()) {
            val oldest = nonSticky.minByOrNull { it.timestamp }!!
            buffer.remove(oldest)
        } else {
            // All sticky — evict the oldest sticky record
            val oldest = buffer.minByOrNull { it.timestamp }!!
            buffer.remove(oldest)
        }
    }

    // ---- Serialization ----

    fun serialize(): CompoundTag {
        val tag = CompoundTag()
        for ((uuid, records) in memories) {
            val list = ListTag()
            for (record in records) {
                list.add(record.serialize())
            }
            tag.put(uuid.toString(), list)
        }
        return tag
    }

    companion object {
        fun deserialize(tag: CompoundTag): MemoryBuffer {
            val buffer = MemoryBuffer()
            for (key in tag.allKeys) {
                try {
                    val uuid = UUID.fromString(key)
                    val list = tag.getList(key, 10) // 10 = CompoundTag type ID
                    val records = mutableListOf<InteractionRecord>()
                    for (i in 0 until list.size) {
                        records.add(InteractionRecord.deserialize(list.getCompound(i)))
                    }
                    if (records.isNotEmpty()) {
                        buffer.memories[uuid] = records
                    }
                } catch (_: Exception) {
                    // skip corrupted entries
                }
            }
            return buffer
        }
    }
}
