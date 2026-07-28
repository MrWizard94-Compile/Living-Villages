package com.livingvillages.identity

import net.minecraft.nbt.CompoundTag
import kotlin.math.max
import kotlin.math.min

/**
 * Four-axis personality system. Each axis ranges from -1.0 to 1.0.
 *
 * Personality is generated once at identity initialization and fixed for life.
 * Combinations of axes produce emergent archetypes (see codex section 1.2).
 */
data class Personality(
    val warmth: Float = 0f,   // Cold/distant (-1) to Friendly/welcoming (+1)
    val energy: Float = 0f,   // Passive/sedentary (-1) to Active/bustling (+1)
    val courage: Float = 0f,  // Timid/avoidant (-1) to Brave/confrontational (+1)
    val honesty: Float = 0f,  // Sneaky/unreliable (-1) to Straightforward/trustworthy (+1)
) {
    fun serialize(): CompoundTag {
        val tag = CompoundTag()
        tag.putFloat("warmth", warmth)
        tag.putFloat("energy", energy)
        tag.putFloat("courage", courage)
        tag.putFloat("honesty", honesty)
        return tag
    }

    companion object {
        fun deserialize(tag: CompoundTag) = Personality(
            warmth = tag.getFloat("warmth"),
            energy = tag.getFloat("energy"),
            courage = tag.getFloat("courage"),
            honesty = tag.getFloat("honesty"),
        )

        fun clamp(value: Float): Float = max(-1f, min(1f, value))
    }

    /** Descriptive label for a given axis value. */
    object Labels {
        fun warmth(v: Float): String = when {
            v <= -0.6f -> "Cold"
            v <= -0.2f -> "Reserved"
            v <= 0.2f -> "Neutral"
            v <= 0.6f -> "Warm"
            else -> "Friendly"
        }

        fun energy(v: Float): String = when {
            v <= -0.6f -> "Passive"
            v <= -0.2f -> "Calm"
            v <= 0.2f -> "Moderate"
            v <= 0.6f -> "Active"
            else -> "Bustling"
        }

        fun courage(v: Float): String = when {
            v <= -0.6f -> "Timid"
            v <= -0.2f -> "Cautious"
            v <= 0.2f -> "Steady"
            v <= 0.6f -> "Bold"
            else -> "Brave"
        }

        fun honesty(v: Float): String = when {
            v <= -0.6f -> "Sneaky"
            v <= -0.2f -> "Evasive"
            v <= 0.2f -> "Average"
            v <= 0.6f -> "Honest"
            else -> "Forthright"
        }
    }
}
