package com.livingvillages

import net.minecraft.resources.ResourceLocation

object LVConstants {
    const val MOD_ID = "livingvillages"
    const val MOD_NAME = "Living Villages"

    fun modRL(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)

    // Config defaults
    const val DEFAULT_GOSSIP_RANGE = 8.0
    const val DEFAULT_WITNESS_RANGE = 16.0
    const val DEFAULT_MEMORY_BUFFER_SIZE = 15
    const val DEFAULT_STICKY_MAGNITUDE_THRESHOLD = 0.8f
    const val DEFAULT_GOSSIP_MAGNITUDE_DECAY = 0.7f // secondhand info = 70% weight
    const val DEFAULT_VITALITY_TICK_INTERVAL = 1000 // ~50 seconds
    const val DEFAULT_MAINTENANCE_RANGE = 16 // blocks from home bed
    const val DEFAULT_VILLAGE_MIN_BEDS = 3
    const val DEFAULT_VILLAGE_MIN_VILLAGERS = 2
}
