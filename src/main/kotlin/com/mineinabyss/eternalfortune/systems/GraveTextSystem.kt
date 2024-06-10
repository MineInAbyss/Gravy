package com.mineinabyss.eternalfortune.systems

import com.github.shynixn.mccoroutine.bukkit.launch
import com.mineinabyss.blocky.components.core.BlockyFurniture
import com.mineinabyss.blocky.helpers.GenericHelpers
import com.mineinabyss.eternalfortune.components.Grave
import com.mineinabyss.eternalfortune.eternal
import com.mineinabyss.eternalfortune.extensions.*
import com.mineinabyss.geary.modules.GearyModule
import com.mineinabyss.geary.observers.events.OnEntityRemoved
import com.mineinabyss.geary.observers.events.OnSet
import com.mineinabyss.geary.papermc.tracking.entities.toGeary
import com.mineinabyss.geary.systems.builders.observe
import com.mineinabyss.geary.systems.query.query
import kotlinx.coroutines.delay
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.server.packs.repository.Pack
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player

//fun GearyModule.graveTextSetter() = observe<OnSet>()
//    .involving(query<ItemDisplay, BlockyFurniture, Grave>())
//    .exec { (itemDisplay, _, grave) ->
//        eternal.plugin.launch {
//            delay(1)
//            itemDisplay.world.getNearbyPlayers(itemDisplay.location, GenericHelpers.simulationDistance).filterNotNull().forEach { player ->
//                player.sendGraveTextDisplay(itemDisplay, grave)
//            }
//        }
//    }

fun handleGravePackets(packet: Packet<*>, player: Player): Packet<*> {
    return when (packet) {
        is ClientboundBundlePacket -> ClientboundBundlePacket(packet.subPackets().map { handleGravePackets(it, player) as Packet<in ClientGamePacketListener> })
        is ClientboundAddEntityPacket -> {
            eternal.plugin.launch {
                delay(1) // Delay so the entity exists in the world
                val entity = (player.world as CraftWorld).handle.getEntity(packet.id)?.bukkitEntity
                val grave = (entity as? ItemDisplay)?.toGeary()?.get<Grave>() ?: return@launch
                player.sendGraveTextDisplay(entity, grave)
            }
            packet
        }
        // This doesnt handle removed entities as they are gone by the time the client gets this packet
        // This is for entities being unloaded from the client, removed entities are handled elsewhere
        is ClientboundRemoveEntitiesPacket -> {
            packet.entityIds.forEach { entityId ->
                eternal.plugin.launch {
                    delay(1)
                    val entity = ((player.world as CraftWorld).handle.getEntity(entityId)?.bukkitEntity as? ItemDisplay) ?: return@launch
                    player.removeGraveTextDisplay(entity)
                }
            }
            packet
        }
        else -> packet
    }
}