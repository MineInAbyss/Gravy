package com.mineinabyss.eternalfortune

import com.mineinabyss.eternalfortune.extensions.EternalMessages
import com.mineinabyss.idofront.di.DI
import com.mineinabyss.idofront.messaging.ComponentLogger

val eternal by DI.observe<EternalContext>()
interface EternalContext {
    val plugin: EternalFortune
    val config: EternalConfig
    val messages: EternalMessages
    val logger: ComponentLogger
}
