package com.mineinabyss.eternalfortune.extensions

import com.mineinabyss.blocky.api.BlockyFurnitures
import com.mineinabyss.blocky.api.BlockyFurnitures.blockyFurniture
import com.mineinabyss.blocky.helpers.FurnitureHelpers
import com.mineinabyss.eternalfortune.components.Grave
import com.mineinabyss.eternalfortune.components.GraveOfflineNotice
import com.mineinabyss.eternalfortune.components.PlayerGraves
import com.mineinabyss.eternalfortune.eternal
import com.mineinabyss.eternalfortune.extensions.EternalHelpers.openGraveInventory
import com.mineinabyss.geary.papermc.datastore.encode
import com.mineinabyss.geary.papermc.tracking.entities.toGearyOrNull
import com.mineinabyss.geary.papermc.tracking.items.gearyItems
import com.mineinabyss.idofront.entities.toOfflinePlayer
import com.mineinabyss.idofront.entities.toPlayer
import com.mineinabyss.idofront.messaging.error
import com.mineinabyss.idofront.messaging.logError
import com.mineinabyss.idofront.messaging.success
import com.mineinabyss.idofront.nms.nbt.WrappedPDC
import com.mineinabyss.idofront.textcomponents.miniMsg
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.StorageGui
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.craftbukkit.v1_20_R1.CraftServer
import org.bukkit.craftbukkit.v1_20_R1.persistence.CraftPersistentDataContainer
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

object EternalHelpers {
    fun Player.spawnGrave() {
        val graveLocation = location.findNearestSpawnableBlock() ?: return this.error(EternalMessages.NO_SPACE_FOR_GRAVE)
        val grave = BlockyFurnitures.placeFurniture(eternal.config.graveFurniture, graveLocation) ?: return this.error(EternalMessages.NO_SPACE_FOR_GRAVE)
        val expirationDate = LocalDateTime.now().plusSeconds(eternal.config.expirationTime.inWholeSeconds).toEpochSecond(ZoneOffset.UTC)
        val protectionDate = LocalDateTime.now().plusSeconds(eternal.config.protectionTime.inWholeSeconds).toEpochSecond(ZoneOffset.UTC)
        grave.toGearyOrNull()?.setPersisting(Grave(uniqueId, inventory.contents.filterNotNull(), protectionDate, expirationDate)) ?: this.error("Could not fill grave with items")

        this.success("Grave spawned at ${graveLocation.blockX} ${graveLocation.blockY} ${graveLocation.blockZ}!")
    }

    private fun Location.findNearestSpawnableBlock(): Location? {
        return when {
            FurnitureHelpers.hasEnoughSpace(eternal.config.graveFurniture.blockyFurniture!!, this, 0f) -> this
            else -> {
                val spawnRadius = eternal.config.spawnRadiusCheck
                //TODO Make this go from center and outwards
                for (x in -spawnRadius..spawnRadius) for (y in -spawnRadius..spawnRadius) for (z in -spawnRadius..spawnRadius)
                    if (FurnitureHelpers.hasEnoughSpace(eternal.config.graveFurniture.blockyFurniture!!, this.add(x.toDouble(),y.toDouble(),z.toDouble()), 0f))
                        this.add(x.toDouble(),y.toDouble(),z.toDouble())
                null
            }
        }
    }

    private val graveInvMap = mutableMapOf<UUID, StorageGui>()
    fun ItemDisplay.openGraveInventory(player: Player) {
        val graveContent = grave?.graveContent ?: return
        val graveInv = graveInvMap.getOrPut(uniqueId) {
            Gui.storage().title("Grave".miniMsg()).rows(3).create().apply {
                this.addItem(graveContent)
                setCloseGuiAction { close ->
                    when {
                        close.inventory.isEmpty -> {
                            val owner = grave!!.graveOwner.toOfflinePlayer()
                            when {
                                owner.isOnline -> owner.player!!.success(EternalMessages.GRAVE_EMPTIED)
                                else -> {
                                    val pdc = owner.getOfflinePDC() ?: return@setCloseGuiAction logError("Could not get PDC for ${owner.name}")
                                    pdc.encode(GraveOfflineNotice(EternalMessages.GRAVE_EMPTIED))
                                    owner.saveOfflinePDC(pdc)
                                }
                            }
                            BlockyFurnitures.removeFurniture(this@openGraveInventory)
                        }
                        else -> this@openGraveInventory.toGearyOrNull()?.setPersisting(grave!!.copy(graveContent = close.inventory.contents.filterNotNull()))
                    }
                }
            }
        }
        graveInv.open(player)
    }
}

val ItemDisplay.isGrave get() = toGearyOrNull()?.has<Grave>() == true
val ItemDisplay.grave get() = toGearyOrNull()?.get<Grave>()
fun Grave.isExpired() = expirationTime < currentTime()


fun currentTime() = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)

val Player.hasGraves get() = toGearyOrNull()?.get<PlayerGraves>()?.let { it.graveLocations.isNotEmpty() && it.graveUuids.isNotEmpty() } ?: false
val Player.playerGraves get() = toGearyOrNull()?.get<PlayerGraves>()
fun PlayerGraves.size() = graveUuids.size
/**
 * Gets the PlayerData from file for this UUID.
 */
internal fun UUID.getOfflinePlayerData(): CompoundTag? = (Bukkit.getServer() as CraftServer).handle.playerIo.getPlayerData(this.toString())

/**
 * Gets a copy of the WrappedPDC for this OfflinePlayer.
 * Care should be taken to ensure that the player is not online when this is called.
 */
fun OfflinePlayer.getOfflinePDC() : WrappedPDC? {
    if (isOnline) return WrappedPDC((player!!.persistentDataContainer as CraftPersistentDataContainer).toTagCompound())
    val baseTag = uniqueId.getOfflinePlayerData()?.getCompound("BukkitValues") ?: return null
    return WrappedPDC(baseTag)
}

/**
 * Saves the given WrappedPDC to the OfflinePlayer's PlayerData file.
 * Care should be taken to ensure that the player is not online when this is called.
 * @return true if successful, false otherwise.
 */
fun OfflinePlayer.saveOfflinePDC(pdc: WrappedPDC): Boolean {
    if (isOnline) return false
    val worldNBTStorage = (Bukkit.getServer() as CraftServer).server.playerDataStorage
    val tempFile = File(worldNBTStorage.playerDir, "$uniqueId.dat.tmp")
    val playerFile = File(worldNBTStorage.playerDir, "$uniqueId.dat")

    val mainPDc = uniqueId.getOfflinePlayerData() ?: return false
    mainPDc.put("BukkitValues", pdc.compoundTag) ?: return false
    runCatching {
        Files.newOutputStream(tempFile.toPath()).use { outStream ->
            NbtIo.writeCompressed(mainPDc, outStream)
            if (playerFile.exists() && !playerFile.delete()) logError("Failed to delete player file $uniqueId")
            if (!tempFile.renameTo(playerFile)) logError("Failed to rename player file $uniqueId")
        }
    }.onFailure {
        logError("Failed to save player file $uniqueId")
        it.printStackTrace()
        return false
    }
    return true
}
