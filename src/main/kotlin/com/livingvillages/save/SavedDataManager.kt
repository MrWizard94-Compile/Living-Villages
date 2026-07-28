package com.livingvillages.save

import com.livingvillages.LVConstants
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Per-world persistent data for Living Villages.
 *
 * Stores village-level data only: village registry, vitality scores, etc.
 * Per-villager identity/memory data lives in the VillagerIdentity attachment.
 *
 * Data is stored under: world/data/livingvillages_data.dat
 */
class LVSavedData private constructor() : SavedData() {
    private val logger = LoggerFactory.getLogger("LivingVillages/SavedData")

    // ---- Village Registry ----
    private val villages = mutableMapOf<UUID, VillageRecord>()

    // ---- Village accessors ----

    fun getVillage(id: UUID): VillageRecord? = villages[id]
    fun getAllVillages(): Map<UUID, VillageRecord> = villages
    fun putVillage(id: UUID, record: VillageRecord) { villages[id] = record; setDirty() }
    fun removeVillage(id: UUID) { villages.remove(id); setDirty() }

    // ---- Serialization ----

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        val villagesTag = CompoundTag()
        for ((id, record) in villages) {
            villagesTag.put(id.toString(), record.serialize())
        }
        tag.put("villages", villagesTag)

        logger.debug("Saved: {} villages", villages.size)
        return tag
    }

    private fun load(tag: CompoundTag) {
        val villagesTag = tag.getCompound("villages")
        for (key in villagesTag.allKeys) {
            try {
                villages[UUID.fromString(key)] = VillageRecord.deserialize(villagesTag.getCompound(key))
            } catch (e: Exception) {
                logger.error("Failed to load village {}: {}", key, e.message)
            }
        }

        logger.info("Loaded: {} villages", villages.size)
    }

    companion object {
        private const val DATA_NAME = "${LVConstants.MOD_ID}_data"

        private val factory = Factory(
            { LVSavedData() },
            { tag, _ ->
                LVSavedData().apply { load(tag) }
            },
            null
        )

        fun get(server: MinecraftServer): LVSavedData {
            return server.overworld().dataStorage.computeIfAbsent(factory, DATA_NAME)
        }
    }
}

data class VillageRecord(
    val id: UUID,
    val name: String = "",
    val vitality: Float = 0.5f,
) {
    fun serialize(): CompoundTag = CompoundTag().apply {
        putString("id", id.toString())
        putString("name", name)
        putFloat("vitality", vitality)
    }

    companion object {
        fun deserialize(tag: CompoundTag) = VillageRecord(
            id = UUID.fromString(tag.getString("id")),
            name = tag.getString("name"),
            vitality = tag.getFloat("vitality"),
        )
    }
}
