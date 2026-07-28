package com.livingvillages.identity

import com.livingvillages.LVConstants
import com.livingvillages.memory.MemoryBuffer
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.attachment.IAttachmentHolder
import net.neoforged.neoforge.attachment.IAttachmentSerializer
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.function.Supplier

/**
 * Data attachment that gives each villager a persistent identity:
 * a name, a personality, a memory buffer, and an initialization flag.
 *
 * Attached to Villager entities via NeoForge's attachment system.
 * Serialized to NBT for persistence, synced to client for name rendering.
 * Memory is server-only — NOT included in the sync codec.
 */
class VillagerIdentity(
    var name: String = "",
    var personality: Personality = Personality(),
    var memory: MemoryBuffer = MemoryBuffer(),
    var initialized: Boolean = false,
) {
    companion object {
        val ATTACHMENT_TYPES: DeferredRegister<AttachmentType<*>> =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, LVConstants.MOD_ID)

        val VILLAGER_IDENTITY: Supplier<AttachmentType<VillagerIdentity>> =
            ATTACHMENT_TYPES.register("villager_identity") { ->
                AttachmentType.builder(Supplier { VillagerIdentity() })
                    .serialize(Serializer)
                    .copyOnDeath()
                    .sync(SyncCodec)
                    .build()
            }

        private object Serializer : IAttachmentSerializer<CompoundTag, VillagerIdentity> {
            override fun read(
                holder: IAttachmentHolder,
                tag: CompoundTag,
                provider: HolderLookup.Provider,
            ): VillagerIdentity {
                val identity = VillagerIdentity(
                    name = tag.getString("name"),
                    personality = Personality.deserialize(tag.getCompound("personality")),
                    initialized = tag.getBoolean("initialized"),
                )
                if (tag.contains("memory")) {
                    identity.memory = MemoryBuffer.deserialize(tag.getCompound("memory"))
                }
                return identity
            }

            override fun write(
                attachment: VillagerIdentity,
                provider: HolderLookup.Provider,
            ): CompoundTag {
                val tag = CompoundTag()
                tag.putString("name", attachment.name)
                tag.put("personality", attachment.personality.serialize())
                tag.putBoolean("initialized", attachment.initialized)
                tag.put("memory", attachment.memory.serialize())
                return tag
            }
        }

        /** Client sync: name + personality + initialized only. Memory is server-only. */
        private object SyncCodec : StreamCodec<RegistryFriendlyByteBuf, VillagerIdentity> {
            override fun decode(buf: RegistryFriendlyByteBuf): VillagerIdentity {
                return VillagerIdentity(
                    name = buf.readUtf(),
                    personality = Personality(
                        warmth = buf.readFloat(),
                        energy = buf.readFloat(),
                        courage = buf.readFloat(),
                        honesty = buf.readFloat(),
                    ),
                    initialized = buf.readBoolean(),
                )
            }

            override fun encode(buf: RegistryFriendlyByteBuf, value: VillagerIdentity) {
                buf.writeUtf(value.name)
                buf.writeFloat(value.personality.warmth)
                buf.writeFloat(value.personality.energy)
                buf.writeFloat(value.personality.courage)
                buf.writeFloat(value.personality.honesty)
                buf.writeBoolean(value.initialized)
            }
        }
    }
}
