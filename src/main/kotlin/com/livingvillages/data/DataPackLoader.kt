package com.livingvillages.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.livingvillages.LVConstants
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import org.slf4j.LoggerFactory

/**
 * Base class for Living Villages data pack loaders.
 *
 * Each data type (name pools, personality traits, dialogue templates, path palettes, etc.)
 * gets its own loader subclass that plugs into Minecraft's reload listener system.
 *
 * Data lives under: data/<namespace>/livingvillages/<data_type>/<file>.json
 */
abstract class LVDataLoader<T>(
    private val dataType: String,
) : SimpleJsonResourceReloadListener(GSON, LVConstants.MOD_ID + "/" + dataType) {

    private val logger = LoggerFactory.getLogger("LivingVillages/Data/$dataType")
    private val loadedData = mutableMapOf<ResourceLocation, T>()

    companion object {
        val GSON: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    }

    override fun apply(
        entries: Map<ResourceLocation, JsonElement>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller,
    ) {
        loadedData.clear()
        var loaded = 0
        var failed = 0

        for ((id, json) in entries) {
            try {
                val parsed = parse(id, json)
                if (parsed != null) {
                    loadedData[id] = parsed
                    loaded++
                } else {
                    logger.warn("Skipped invalid entry: {}", id)
                    failed++
                }
            } catch (e: Exception) {
                logger.error("Failed to load {}: {}", id, e.message)
                failed++
            }
        }

        logger.info("Loaded {} {} entries ({} failed)", loaded, dataType, failed)
        onLoadComplete(loadedData)
    }

    /**
     * Parse a single JSON entry into typed data. Return null to skip invalid entries.
     */
    protected abstract fun parse(id: ResourceLocation, json: JsonElement): T?

    /**
     * Called after all entries are loaded. Override to perform post-processing.
     */
    protected open fun onLoadComplete(data: Map<ResourceLocation, T>) {}

    fun getAll(): Map<ResourceLocation, T> = loadedData
    fun get(id: ResourceLocation): T? = loadedData[id]
    fun size(): Int = loadedData.size
}

// ---- Concrete loaders (skeletons — parsing logic comes with Module 1+) ----

/**
 * Loads name pool data packs.
 * Path: data/<namespace>/livingvillages/names/<culture>.json
 */
object NamePoolLoader : LVDataLoader<NamePoolData>("names") {
    override fun parse(id: ResourceLocation, json: JsonElement): NamePoolData? {
        val obj = json.asJsonObject ?: return null
        val culture = obj.get("culture")?.asString ?: return null
        val maleNames = obj.getAsJsonArray("male")?.map { it.asString } ?: emptyList()
        val femaleNames = obj.getAsJsonArray("female")?.map { it.asString } ?: emptyList()
        val neutralNames = obj.getAsJsonArray("neutral")?.map { it.asString } ?: emptyList()

        if (maleNames.isEmpty() && femaleNames.isEmpty() && neutralNames.isEmpty()) {
            return null
        }

        return NamePoolData(culture, maleNames, femaleNames, neutralNames)
    }
}

data class NamePoolData(
    val culture: String,
    val maleNames: List<String>,
    val femaleNames: List<String>,
    val neutralNames: List<String>,
) {
    fun allNames(): List<String> = maleNames + femaleNames + neutralNames
}

/**
 * Loads gossip dialogue templates.
 * Path: data/<namespace>/livingvillages/dialogue/<category>.json
 */
object DialogueLoader : LVDataLoader<DialogueTemplateData>("dialogue") {
    override fun parse(id: ResourceLocation, json: JsonElement): DialogueTemplateData? {
        val obj = json.asJsonObject ?: return null
        val category = obj.get("category")?.asString ?: return null
        val templates = obj.getAsJsonArray("templates")?.map { it.asString } ?: return null

        if (templates.isEmpty()) return null
        return DialogueTemplateData(category, templates)
    }
}

data class DialogueTemplateData(
    val category: String, // "positive", "negative", "neutral"
    val templates: List<String>, // e.g., "{speaker} told {listener}: 'That traveler helped us.'"
)

/**
 * Loads block palette definitions for village visual cues.
 * Path: data/<namespace>/livingvillages/palettes/<biome>.json
 */
object BlockPaletteLoader : LVDataLoader<BlockPaletteData>("palettes") {
    override fun parse(id: ResourceLocation, json: JsonElement): BlockPaletteData? {
        val obj = json.asJsonObject ?: return null
        val biome = obj.get("biome")?.asString ?: return null
        val tiers = mutableMapOf<String, Map<String, String>>()

        for (tierName in listOf("declining", "struggling", "stable", "thriving", "flourishing")) {
            val tierObj = obj.getAsJsonObject(tierName) ?: continue
            val mapping = mutableMapOf<String, String>()
            for ((key, value) in tierObj.entrySet()) {
                mapping[key] = value.asString
            }
            tiers[tierName] = mapping
        }

        return BlockPaletteData(biome, tiers)
    }
}

data class BlockPaletteData(
    val biome: String,
    val tiers: Map<String, Map<String, String>>, // tier -> (slot -> block_id)
)
