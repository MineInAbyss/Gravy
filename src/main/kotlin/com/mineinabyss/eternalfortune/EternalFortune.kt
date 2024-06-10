package com.mineinabyss.eternalfortune

import com.mineinabyss.blocky.api.BlockyFurnitures.isBlockyFurniture
import com.mineinabyss.eternalfortune.extensions.EternalMessages
import com.mineinabyss.eternalfortune.extensions.removeGraveTextDisplay
import com.mineinabyss.eternalfortune.extensions.textDisplayIDMap
import com.mineinabyss.eternalfortune.listeners.GraveListener
import com.mineinabyss.eternalfortune.listeners.PlayerListener
import com.mineinabyss.eternalfortune.systems.handleGravePackets
import com.mineinabyss.geary.addons.GearyPhase
import com.mineinabyss.geary.autoscan.autoscan
import com.mineinabyss.geary.modules.geary
import com.mineinabyss.idofront.config.config
import com.mineinabyss.idofront.di.DI
import com.mineinabyss.idofront.messaging.ComponentLogger
import com.mineinabyss.idofront.messaging.logError
import com.mineinabyss.idofront.messaging.observeLogger
import com.mineinabyss.idofront.nms.interceptClientbound
import com.mineinabyss.idofront.plugin.listeners
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class EternalFortune : JavaPlugin() {
    override fun onLoad() {
        geary {
            autoscan(classLoader, "com.mineinabyss.eternalfortune") {
                all()
            }
        }
    }

    override fun onEnable() {
        registerEternalContext()

        EternalCommands.registerCommands()

        listeners(GraveListener(), /*PlayerListener()*/)

        interceptClientbound { packet, player ->
            player?.let { handleGravePackets(packet, it) } ?: packet
        }

        geary.run {
            //graveTextSetter()
        }
        geary {
            on(GearyPhase.ENABLE) {
                if (!eternal.config.graveFurniture.isBlockyFurniture) {
                    eternal.logger.e("The graveFurniture config option must be a BlockyFurniture!")
                    Bukkit.getPluginManager().disablePlugin(this@EternalFortune)
                }
            }
        }
    }

    override fun onDisable() {
        //textDisplayIDMap.values.forEach(::removeGraveTextDisplay)
    }

    fun registerEternalContext() {
        DI.remove<EternalContext>()
        DI.add<EternalContext>(object : EternalContext {
            override val plugin = this@EternalFortune
            override val config: EternalConfig by config("config", dataFolder.toPath(), EternalConfig())
            override val messages: EternalMessages by config("messages", dataFolder.toPath(), EternalMessages())
            override val logger: ComponentLogger by plugin.observeLogger()
        })
    }
}
