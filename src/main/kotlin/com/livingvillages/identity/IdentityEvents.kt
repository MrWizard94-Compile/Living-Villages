package com.livingvillages.identity

import com.livingvillages.LVConstants
import com.livingvillages.config.ServerConfig
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import org.slf4j.LoggerFactory

/**
 * Server-side event handlers for villager identity initialization.
 *
 * Two triggers:
 * 1. Direct interaction (right-click on villager) — immediate
 * 2. Proximity check (player within 16 blocks) — checked periodically
 */
object IdentityEvents {
    private val logger = LoggerFactory.getLogger("LivingVillages/IdentityEvents")

    // Check proximity every 40 ticks (2 seconds) to avoid per-tick overhead
    private const val PROXIMITY_CHECK_INTERVAL = 40L
    private const val PROXIMITY_RANGE_SQ = 16.0 * 16.0 // 16 blocks squared

    /**
     * On direct right-click interaction with a villager, initialize their identity immediately.
     */
    @SubscribeEvent
    fun onPlayerInteractEntity(event: PlayerInteractEvent.EntityInteract) {
        val entity = event.target
        if (entity !is Villager) return
        if (entity.level().isClientSide) return

        IdentityService.initializeIdentity(entity)
    }

    /**
     * Periodic proximity scan: check all villagers near each player.
     * Runs server-side every PROXIMITY_CHECK_INTERVAL ticks.
     */
    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Post) {
        val server = event.server
        if (server.tickCount % PROXIMITY_CHECK_INTERVAL != 0L) return

        for (level in server.allLevels) {
            // Villagers only naturally exist in the overworld
            if (level.dimension() != Level.OVERWORLD) continue
            for (player in level.players()) {
                initializeNearbyVillagers(level, player)
            }
        }
    }

    private fun initializeNearbyVillagers(level: ServerLevel, player: Player) {
        val playerPos = player.position()
        // Use the entity lookup to find nearby villagers efficiently
        val aabb = player.boundingBox.inflate(16.0)
        val nearbyVillagers = level.getEntitiesOfClass(Villager::class.java, aabb)

        for (villager in nearbyVillagers) {
            val identity = villager.getData(VillagerIdentity.VILLAGER_IDENTITY.get())
            if (!identity.initialized) {
                IdentityService.initializeIdentity(villager)
            }
        }
    }
}
