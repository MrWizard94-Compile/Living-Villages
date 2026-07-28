package com.livingvillages.memory

import com.livingvillages.LVConstants
import com.livingvillages.event.InteractionType
import net.minecraft.nbt.CompoundTag

/**
 * A single interaction record stored in a villager's memory buffer.
 * This is the unified interaction record used throughout the mod.
 */
data class InteractionRecord(
    val type: InteractionType,
    val timestamp: Long,            // in-game tick
    val sentimentWeight: Float,     // -1.0 (very negative) to 1.0 (very positive)
    val magnitude: Float,           // 0.0 to 1.0 — how significant
    val witnessed: Boolean,         // firsthand or via gossip?
) {
    val sticky: Boolean get() = magnitude >= LVConstants.DEFAULT_STICKY_MAGNITUDE_THRESHOLD

    fun serialize(): CompoundTag {
        val tag = CompoundTag()
        tag.putString("type", type.name)
        tag.putLong("timestamp", timestamp)
        tag.putFloat("sentiment", sentimentWeight)
        tag.putFloat("magnitude", magnitude)
        tag.putBoolean("witnessed", witnessed)
        return tag
    }

    companion object {
        fun deserialize(tag: CompoundTag): InteractionRecord {
            val typeName = tag.getString("type")
            val type = try {
                InteractionType.valueOf(typeName)
            } catch (_: IllegalArgumentException) {
                InteractionType.TALK // safe fallback for corrupted data
            }
            return InteractionRecord(
                type = type,
                timestamp = tag.getLong("timestamp"),
                sentimentWeight = tag.getFloat("sentiment"),
                magnitude = tag.getFloat("magnitude"),
                witnessed = tag.getBoolean("witnessed"),
            )
        }
    }
}
