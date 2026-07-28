package com.livingvillages.command

import com.livingvillages.LVConstants
import com.livingvillages.data.NamePoolLoader
import com.livingvillages.event.ReputationTier
import com.livingvillages.identity.Personality
import com.livingvillages.identity.VillagerIdentity
import com.livingvillages.memory.GossipService
import com.livingvillages.save.LVSavedData
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.phys.AABB
import org.slf4j.LoggerFactory

/**
 * Debug and info commands under /livingvillages (alias /lv).
 * These are operator-only commands for debugging and testing.
 */
object LVCommands {
    private val logger = LoggerFactory.getLogger("LivingVillages/Commands")

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val root = Commands.literal(LVConstants.MOD_ID)
            .requires { it.hasPermission(2) } // op-only
            .then(Commands.literal("info").executes(::cmdInfo))
            .then(Commands.literal("villager").executes(::cmdVillager))
            .then(Commands.literal("vitality").executes(::cmdVitality))
            .then(Commands.literal("names").executes(::cmdNames))
            .then(
                Commands.literal("gossip")
                    .then(Commands.literal("trigger").executes(::cmdGossipTrigger))
            )
            .then(
                Commands.literal("test")
                    .then(Commands.literal("personality").executes(::cmdTestPersonality))
                    .then(Commands.literal("gossip").executes(::cmdTestGossip))
            )

        dispatcher.register(root)

        // Alias: /lv
        dispatcher.register(Commands.literal("lv").redirect(dispatcher.getRoot().getChild(LVConstants.MOD_ID)))

        logger.info("Registered commands")
    }

    /**
     * /lv villager — Show identity info for the nearest villager within 8 blocks.
     * Includes personality, memory count, and opinion of the commanding player.
     */
    private fun cmdVillager(ctx: CommandContext<CommandSourceStack>): Int {
        val source = ctx.source
        val player = source.playerOrException
        val level = player.serverLevel()

        val searchBox = AABB.ofSize(player.position(), 16.0, 8.0, 16.0)
        val nearbyVillagers = level.getEntitiesOfClass(Villager::class.java, searchBox)

        if (nearbyVillagers.isEmpty()) {
            source.sendFailure(Component.literal("[LV] No villagers within 8 blocks."))
            return 0
        }

        val nearest = nearbyVillagers.minByOrNull { it.distanceToSqr(player) }!!
        val identity = nearest.getData(VillagerIdentity.VILLAGER_IDENTITY.get())

        if (!identity.initialized) {
            source.sendSuccess({
                Component.literal("[LV] Nearest villager is not yet initialized (approach to initialize).")
            }, false)
            return 1
        }

        val p = identity.personality
        val mem = identity.memory
        val profession = nearest.villagerData.profession.name()
        val dist = String.format("%.1f", nearest.distanceTo(player))

        // Calculate opinion of the commanding player
        val opinion = mem.calculateOpinion(player.uuid, level.gameTime)
        val tier = ReputationTier.fromOpinion(opinion)
        val memoriesOfPlayer = mem.getMemories(player.uuid).size

        source.sendSuccess({
            Component.literal(
                """
                |§6--- ${identity.name} ---§r
                |§7Profession:§r $profession
                |§7Distance:§r ${dist}m
                |§7Warmth:§r ${formatAxis(p.warmth, Personality.Labels::warmth)}
                |§7Energy:§r ${formatAxis(p.energy, Personality.Labels::energy)}
                |§7Courage:§r ${formatAxis(p.courage, Personality.Labels::courage)}
                |§7Honesty:§r ${formatAxis(p.honesty, Personality.Labels::honesty)}
                |§7Relationships:§r ${mem.relationshipCount()} subjects, ${mem.totalMemoryCount()} total memories
                |§7Opinion of you:§r ${String.format("%+.2f", opinion)} (${tier.name}) — $memoriesOfPlayer memories
                """.trimMargin()
            )
        }, false)

        return 1
    }

    private fun formatAxis(value: Float, labelFn: (Float) -> String): String {
        val bar = buildAxisBar(value)
        return "$bar §e${String.format("%+.2f", value)}§r (${labelFn(value)})"
    }

    private fun buildAxisBar(value: Float): String {
        val steps = 10
        val filled = ((value + 1f) / 2f * steps).toInt().coerceIn(0, steps)
        val full = "§a${"█".repeat(filled)}§7${"░".repeat(steps - filled)}§r"
        return "[$full]"
    }

    /**
     * /lv names — Show loaded name pool statistics.
     */
    private fun cmdNames(ctx: CommandContext<CommandSourceStack>): Int {
        val pools = NamePoolLoader.getAll()
        if (pools.isEmpty()) {
            ctx.source.sendFailure(Component.literal("[LV] No name pools loaded!"))
            return 0
        }

        val lines = mutableListOf("[LV] §6Loaded Name Pools:§r")
        for ((id, pool) in pools) {
            val total = pool.allNames().size
            lines.add("  §7${pool.culture}§r: $total names (${pool.maleNames.size}m / ${pool.femaleNames.size}f / ${pool.neutralNames.size}n) [${id}]")
        }

        ctx.source.sendSuccess({
            Component.literal(lines.joinToString("\n"))
        }, false)
        return 1
    }

    private fun cmdInfo(ctx: CommandContext<CommandSourceStack>): Int {
        val server = ctx.source.server
        val data = LVSavedData.get(server)
        val villages = data.getAllVillages()
        ctx.source.sendSuccess({
            Component.literal("[LV] ${villages.size} villages tracked")
        }, false)
        return 1
    }

    private fun cmdVitality(ctx: CommandContext<CommandSourceStack>): Int {
        ctx.source.sendSuccess({
            Component.literal("[LV] Vitality: not yet implemented (Module 2)")
        }, false)
        return 1
    }

    /**
     * /lv gossip trigger — Force a gossip round in the player's current level.
     */
    private fun cmdGossipTrigger(ctx: CommandContext<CommandSourceStack>): Int {
        val source = ctx.source
        val level = source.level
        val count = GossipService.runGossipRound(level)
        source.sendSuccess({
            Component.literal("[LV] Gossip round complete: $count exchanges in ${level.dimension().location()}")
        }, false)
        return 1
    }

    private fun cmdTestPersonality(ctx: CommandContext<CommandSourceStack>): Int {
        ctx.source.sendSuccess({
            Component.literal("[LV] Test personality: not yet implemented")
        }, false)
        return 1
    }

    private fun cmdTestGossip(ctx: CommandContext<CommandSourceStack>): Int {
        ctx.source.sendSuccess({
            Component.literal("[LV] Test gossip: not yet implemented")
        }, false)
        return 1
    }
}
