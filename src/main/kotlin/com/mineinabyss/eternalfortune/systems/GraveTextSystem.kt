package com.mineinabyss.eternalfortune.systems

import com.github.shynixn.mccoroutine.bukkit.launch
import com.mineinabyss.blocky.components.core.BlockyFurniture
import com.mineinabyss.blocky.helpers.GenericHelpers
import com.mineinabyss.eternalfortune.components.Grave
import com.mineinabyss.eternalfortune.eternal
import com.mineinabyss.eternalfortune.extensions.sendGraveText
import com.mineinabyss.geary.modules.GearyModule
import com.mineinabyss.geary.observers.events.OnEntityRemoved
import com.mineinabyss.geary.observers.events.OnSet
import com.mineinabyss.geary.systems.builders.observe
import com.mineinabyss.geary.systems.query.query
import kotlinx.coroutines.delay
import org.bukkit.entity.ItemDisplay

fun GearyModule.graveTextSetter() = observe<OnSet>()
    .involving(query<ItemDisplay, BlockyFurniture, Grave>())
    .exec { (itemDisplay, _, grave) ->
        eternal.plugin.launch {
            delay(1)
            itemDisplay.world.getNearbyPlayers(itemDisplay.location, GenericHelpers.simulationDistance).filterNotNull().forEach { player ->
                player.sendGraveText(itemDisplay, grave)
            }
        }
    }

fun GearyModule.graveTextRemover() = observe<OnEntityRemoved>()