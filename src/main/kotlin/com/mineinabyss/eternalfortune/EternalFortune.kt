package com.mineinabyss.eternalfortune

import com.mineinabyss.eternalfortune.listeners.GraveListener
import com.mineinabyss.eternalfortune.listeners.PlayerListener
import com.mineinabyss.geary.autoscan.autoscan
import com.mineinabyss.geary.modules.geary
import com.mineinabyss.idofront.config.config
import com.mineinabyss.idofront.di.DI
import com.mineinabyss.idofront.platforms.Platforms
import com.mineinabyss.idofront.plugin.listeners
import org.bukkit.plugin.java.JavaPlugin

class EternalFortune : JavaPlugin() {
    override fun onLoad() {
        Platforms.load(this, "mineinabyss")
    }

    override fun onEnable() {
        registerEternalContext()

        EternalCommands()

        listeners(GraveListener(), PlayerListener())

        geary {
            autoscan(classLoader, "com.mineinabyss.eternalfortune") {
                all()
            }
        }
    }

    fun registerEternalContext() {
        DI.remove<EternalContext>()
        DI.add<EternalContext>(object : EternalContext {
            override val plugin = this@EternalFortune
            override val config: EternalConfig by config("config") { fromPluginPath(loadDefault = true) }
        })
    }
}