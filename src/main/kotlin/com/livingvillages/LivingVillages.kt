package com.livingvillages

import com.livingvillages.client.ClientEvents
import com.livingvillages.command.LVCommands
import com.livingvillages.config.ClientConfig
import com.livingvillages.config.ServerConfig
import com.livingvillages.data.BlockPaletteLoader
import com.livingvillages.data.DialogueLoader
import com.livingvillages.data.NamePoolLoader
import com.livingvillages.identity.IdentityEvents
import com.livingvillages.identity.VillagerIdentity
import com.livingvillages.memory.GossipService
import com.livingvillages.memory.MemoryEvents
import net.minecraft.world.level.Level
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.AddReloadListenerEvent
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import org.slf4j.LoggerFactory

@Mod(LVConstants.MOD_ID)
class LivingVillages(
    modBus: IEventBus,
    modContainer: ModContainer,
) {
    private val logger = LoggerFactory.getLogger(LVConstants.MOD_NAME)

    init {
        logger.info("Initializing {} v{}", LVConstants.MOD_NAME, modContainer.modInfo.version)

        // Register configs
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC)
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC)

        // Register attachment types on the mod bus
        VillagerIdentity.ATTACHMENT_TYPES.register(modBus)

        // Register game event listeners on the NeoForge bus
        val forgeBus = NeoForge.EVENT_BUS
        forgeBus.addListener(::onRegisterCommands)
        forgeBus.addListener(::onAddReloadListeners)
        forgeBus.addListener(::onServerStarting)
        forgeBus.addListener(::onServerTick)

        // Register identity event handlers (server-side)
        forgeBus.register(IdentityEvents)
        forgeBus.register(MemoryEvents)

        // Register client-side event handlers
        if (FMLEnvironment.dist.isClient) {
            forgeBus.register(ClientEvents)
        }

        logger.info("{} initialization complete", LVConstants.MOD_NAME)
    }

    private fun onRegisterCommands(event: RegisterCommandsEvent) {
        LVCommands.register(event.dispatcher)
    }

    private fun onAddReloadListeners(event: AddReloadListenerEvent) {
        event.addListener(NamePoolLoader)
        event.addListener(DialogueLoader)
        event.addListener(BlockPaletteLoader)
        logger.debug("Data pack loaders registered")
    }

    private fun onServerStarting(event: ServerStartingEvent) {
        val nameStats = NamePoolLoader.getAll().values.joinToString(", ") {
            "${it.culture}: ${it.allNames().size}"
        }
        logger.info("{} server starting — Name pools: [{}] | {} dialogue sets | {} palettes",
            LVConstants.MOD_NAME,
            nameStats,
            DialogueLoader.size(),
            BlockPaletteLoader.size(),
        )
    }

    /**
     * Periodic gossip tick: run a gossip round in the overworld every ~2 minutes (2400 ticks).
     */
    private fun onServerTick(event: ServerTickEvent.Post) {
        val server = event.server
        if (server.tickCount % GOSSIP_TICK_INTERVAL != 0L) return

        val overworld = server.getLevel(Level.OVERWORLD) ?: return
        GossipService.runGossipRound(overworld)
    }

    companion object {
        private const val GOSSIP_TICK_INTERVAL = 2400L // ~2 minutes
    }
}
