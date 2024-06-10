package com.mineinabyss.eternalfortune

import com.mineinabyss.eternalfortune.components.PlayerGraves
import com.mineinabyss.eternalfortune.extensions.*
import com.mineinabyss.eternalfortune.extensions.EternalHelpers.spawnGrave
import com.mineinabyss.geary.papermc.datastore.decode
import com.mineinabyss.idofront.commands.brigadier.commands
import com.mineinabyss.idofront.messaging.error
import com.mineinabyss.idofront.messaging.success
import com.mineinabyss.idofront.nms.nbt.getOfflinePDC
import com.mojang.brigadier.arguments.StringArgumentType
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import org.bukkit.Bukkit
import org.bukkit.entity.ItemDisplay

object EternalCommands {
    fun registerCommands() {
        eternal.plugin.commands {
            ("eternalfortune" / "ef") {
                "reload" {
                    executes {
                        eternal.plugin.registerEternalContext()
                        sender.success("EternalFortune configs have been reloaded!")
                    }
                }
                "graves" {
                    "place" {
                        playerExecutes {
                            player.spawnGrave(player.inventory.storageContents.toList().filterNotNull(), 0)
                        }
                        val player by ArgumentTypes.player().suggests { suggest(Bukkit.getOnlinePlayers().map { it.name }) }
                        executes {
                            val player = player()!!.resolve(context.source).first()!!
                            player.spawnGrave(player.inventory.storageContents.toList().filterNotNull(), 0)
                        }
                    }
                    "text" {
                        playerExecutes {
                            player.getNearbyEntities(10.0, 10.0, 10.0).filterIsInstance<ItemDisplay>().filter { it.isGrave }.forEach {
                                player.sendGraveTextDisplay(it)
                            }
                        }
                        val player by ArgumentTypes.player().suggests { suggest(Bukkit.getOnlinePlayers().map { it.name }) }
                        executes {
                            val player = player()!!.resolve(context.source).first()!!
                            player.getNearbyEntities(10.0, 10.0, 10.0).filterIsInstance<ItemDisplay>().filter { it.isGrave }.forEach {
                                player.sendGraveTextDisplay(it)
                            }
                        }
                    }
                    "remove" {
                        val player by StringArgumentType.word()
                        executes {
                            val offlinePlayer = Bukkit.getOfflinePlayer(player())!!
                            val playerGraves = when {
                                offlinePlayer.isOnline -> offlinePlayer.player!!.playerGraves
                                else -> offlinePlayer.getOfflinePDC()?.decode<PlayerGraves>()
                            } ?: return@executes sender.error("${offlinePlayer.name} has no graves")

                            playerGraves.graveUuids.zip(playerGraves.graveLocations).toSet().forEach { (uuid, loc) ->
                                loc.ensureWorldIsLoaded()
                                loc.world.getChunkAtAsync(loc).thenAccept { c ->
                                    val graveEntity = c.entities.find { it.uniqueId == uuid } as? ItemDisplay
                                    when {
                                        graveEntity == null ->
                                            if (executor?.uniqueId == offlinePlayer.uniqueId) sender.error("Could not find grave entity")
                                            else sender.error("Could not find grave entity for ${offlinePlayer.name}")
                                        graveEntity.grave?.graveContent?.isNotEmpty() == true ->
                                            for (item in graveEntity.grave!!.graveContent)
                                                graveEntity.world.dropItemNaturally(graveEntity.location, item)
                                    }

                                    graveEntity?.remove()

                                    // Remove the grave from the player's data
                                    offlinePlayer.removeGraveFromPlayerGraves(uuid, loc)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
