package com.livingvillages.client

import com.livingvillages.config.ClientConfig
import com.livingvillages.identity.VillagerIdentity
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.npc.Villager
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.RenderNameTagEvent
import net.neoforged.neoforge.common.util.TriState

/**
 * Client-side event handlers for Living Villages rendering.
 */
object ClientEvents {

    /**
     * Replace the default "Villager" name tag with the assigned Living Villages name.
     * Only fires for Villager entities with an initialized identity.
     */
    @SubscribeEvent
    fun onRenderNameTag(event: RenderNameTagEvent) {
        val entity = event.entity
        if (entity !is Villager) return

        if (!ClientConfig.showVillagerNames.get()) return

        val identity = entity.getData(VillagerIdentity.VILLAGER_IDENTITY.get())
        if (!identity.initialized) return

        event.setContent(Component.literal(identity.name))
        event.setCanRender(TriState.TRUE)
    }
}
