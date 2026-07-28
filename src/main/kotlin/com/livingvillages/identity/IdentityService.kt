package com.livingvillages.identity

import com.livingvillages.data.NamePoolData
import com.livingvillages.data.NamePoolLoader
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Biomes
import org.slf4j.LoggerFactory
import java.util.Random

/**
 * Service that handles villager identity initialization:
 * - Assigns a biome-appropriate name from data packs
 * - Generates personality on a normal distribution with optional biome bias
 *
 * Called once per villager, on first player proximity (within 16 blocks) or direct interaction.
 */
object IdentityService {
    private val logger = LoggerFactory.getLogger("LivingVillages/Identity")
    private val random = Random()

    /**
     * Maps biome resource keys to culture strings used in name pool JSON files.
     * Falls back to "plains" if no specific mapping exists.
     */
    private val BIOME_CULTURE_MAP: Map<ResourceKey<Biome>, String> = mapOf(
        // Plains / generic
        Biomes.PLAINS to "plains",
        Biomes.SUNFLOWER_PLAINS to "plains",
        Biomes.MEADOW to "plains",
        Biomes.FLOWER_FOREST to "plains",
        Biomes.FOREST to "plains",
        Biomes.BIRCH_FOREST to "plains",
        Biomes.DARK_FOREST to "plains",
        Biomes.OLD_GROWTH_BIRCH_FOREST to "plains",
        Biomes.SWAMP to "plains",
        Biomes.MANGROVE_SWAMP to "plains",

        // Desert
        Biomes.DESERT to "desert",
        Biomes.BADLANDS to "desert",
        Biomes.ERODED_BADLANDS to "desert",
        Biomes.WOODED_BADLANDS to "desert",

        // Taiga / Nordic
        Biomes.TAIGA to "taiga",
        Biomes.OLD_GROWTH_SPRUCE_TAIGA to "taiga",
        Biomes.OLD_GROWTH_PINE_TAIGA to "taiga",
        Biomes.WINDSWEPT_HILLS to "taiga",
        Biomes.WINDSWEPT_FOREST to "taiga",
        Biomes.WINDSWEPT_GRAVELLY_HILLS to "taiga",
        Biomes.GROVE to "taiga",
        Biomes.STONY_PEAKS to "taiga",

        // Savanna / African
        Biomes.SAVANNA to "savanna",
        Biomes.SAVANNA_PLATEAU to "savanna",
        Biomes.WINDSWEPT_SAVANNA to "savanna",
        Biomes.JUNGLE to "savanna",
        Biomes.SPARSE_JUNGLE to "savanna",
        Biomes.BAMBOO_JUNGLE to "savanna",

        // Snowy / Slavic
        Biomes.SNOWY_PLAINS to "snowy",
        Biomes.SNOWY_TAIGA to "snowy",
        Biomes.SNOWY_BEACH to "snowy",
        Biomes.SNOWY_SLOPES to "snowy",
        Biomes.FROZEN_PEAKS to "snowy",
        Biomes.JAGGED_PEAKS to "snowy",
        Biomes.ICE_SPIKES to "snowy",
        Biomes.FROZEN_RIVER to "snowy",
        Biomes.FROZEN_OCEAN to "snowy",
        Biomes.DEEP_FROZEN_OCEAN to "snowy",
    )

    private const val DEFAULT_CULTURE = "plains"

    /**
     * Initialize a villager's identity: assign name and generate personality.
     * Returns true if initialization was performed, false if already initialized.
     */
    fun initializeIdentity(villager: Villager): Boolean {
        val existing = villager.getData(VillagerIdentity.VILLAGER_IDENTITY.get())
        if (existing.initialized) return false

        val culture = getCulture(villager)
        val identity = VillagerIdentity(
            name = pickName(culture),
            personality = generatePersonality(culture),
            initialized = true,
        )
        // setData() triggers attachment sync to tracking clients
        villager.setData(VillagerIdentity.VILLAGER_IDENTITY.get(), identity)

        logger.debug(
            "Initialized villager {} as '{}' (culture={}, warmth={}, energy={}, courage={}, honesty={})",
            villager.uuid, identity.name, culture,
            String.format("%.2f", identity.personality.warmth),
            String.format("%.2f", identity.personality.energy),
            String.format("%.2f", identity.personality.courage),
            String.format("%.2f", identity.personality.honesty),
        )

        return true
    }

    /**
     * Determine the culture for a villager based on its current biome.
     */
    fun getCulture(villager: Villager): String {
        val level = villager.level()
        val biomeHolder = level.getBiome(villager.blockPosition())
        val biomeKey = biomeHolder.unwrapKey().orElse(null)
        return if (biomeKey != null) BIOME_CULTURE_MAP[biomeKey] ?: DEFAULT_CULTURE else DEFAULT_CULTURE
    }

    /**
     * Pick a random name from the culture's name pool.
     * Falls back to plains if the culture has no loaded names.
     */
    fun pickName(culture: String): String {
        val pool = findNamePool(culture) ?: findNamePool(DEFAULT_CULTURE)
        if (pool == null) {
            logger.warn("No name pools loaded at all — using fallback name")
            return "Villager"
        }

        val allNames = pool.allNames()
        return if (allNames.isNotEmpty()) {
            allNames[random.nextInt(allNames.size)]
        } else {
            "Villager"
        }
    }

    private fun findNamePool(culture: String): NamePoolData? {
        return NamePoolLoader.getAll().values.firstOrNull { it.culture == culture }
    }

    /**
     * Generate personality values using a normal distribution.
     * Mean 0.0, stddev 0.4, clamped to [-1.0, 1.0].
     * Subtle biome biases applied per the codex.
     */
    fun generatePersonality(culture: String): Personality {
        val stddev = 0.4f

        var warmth = gaussianAxis(stddev)
        var energy = gaussianAxis(stddev)
        var courage = gaussianAxis(stddev)
        var honesty = gaussianAxis(stddev)

        // Subtle biome biases (codex section 1.2)
        when (culture) {
            "desert" -> courage += 0.1f   // desert villagers trend slightly higher courage
            "snowy" -> warmth += 0.1f     // snowy villagers trend slightly higher warmth
            "taiga" -> courage += 0.05f   // nordic influence — slight courage bump
            "savanna" -> energy += 0.05f  // warmer climate — slightly more active
        }

        return Personality(
            warmth = Personality.clamp(warmth),
            energy = Personality.clamp(energy),
            courage = Personality.clamp(courage),
            honesty = Personality.clamp(honesty),
        )
    }

    private fun gaussianAxis(stddev: Float): Float {
        return Personality.clamp((random.nextGaussian() * stddev).toFloat())
    }
}
