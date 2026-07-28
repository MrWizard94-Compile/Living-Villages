package com.livingvillages.memory

import com.livingvillages.event.InteractionType
import com.livingvillages.identity.VillagerIdentity
import net.minecraft.world.entity.npc.Villager
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent
import org.slf4j.LoggerFactory

/**
 * Server-side event handlers that record player interactions into villager memory buffers.
 *
 * Each handler:
 * 1. Gets the villager's identity (skipping if uninitialized)
 * 2. Creates an InteractionRecord with appropriate type/sentiment/magnitude
 * 3. Adds it to the villager's memory buffer for that player
 * 4. Calls setData() to persist the change (attachment won't auto-persist mutations)
 */
object MemoryEvents {
    private val logger = LoggerFactory.getLogger("LivingVillages/MemoryEvents")

    /**
     * Player completes a trade with a villager.
     * Positive sentiment, moderate magnitude.
     */
    @SubscribeEvent
    fun onTradeWithVillager(event: TradeWithVillagerEvent) {
        val villager = event.abstractVillager
        if (villager !is Villager) return

        val identity = villager.getData(VillagerIdentity.VILLAGER_IDENTITY.get())
        if (!identity.initialized) return

        val record = InteractionRecord(
            type = InteractionType.TRADE,
            timestamp = villager.level().gameTime,
            sentimentWeight = 0.4f,
            magnitude = 0.3f,
            witnessed = true,
        )

        identity.memory.addMemory(event.entity.uuid, record)
        // setData() to trigger persistence (mutating getData() in-place doesn't persist)
        villager.setData(VillagerIdentity.VILLAGER_IDENTITY.get(), identity)

        logger.trace("Recorded TRADE memory: {} remembers player {}", identity.name, event.entity.name.string)
    }

    /**
     * Player right-clicks (visits/talks to) a villager.
     * Mild positive — just showing up counts for something.
     */
    @SubscribeEvent
    fun onPlayerInteractVillager(event: PlayerInteractEvent.EntityInteract) {
        val entity = event.target
        if (entity !is Villager) return
        if (entity.level().isClientSide) return

        val identity = entity.getData(VillagerIdentity.VILLAGER_IDENTITY.get())
        if (!identity.initialized) return

        val record = InteractionRecord(
            type = InteractionType.VISIT,
            timestamp = entity.level().gameTime,
            sentimentWeight = 0.1f,
            magnitude = 0.1f,
            witnessed = true,
        )

        identity.memory.addMemory(event.entity.uuid, record)
        entity.setData(VillagerIdentity.VILLAGER_IDENTITY.get(), identity)

        logger.trace("Recorded VISIT memory: {} remembers player {}", identity.name, event.entity.name.string)
    }
}
