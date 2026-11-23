package com.ricedotwho.dtmap

import com.mojang.blaze3d.platform.InputConstants
import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.ModuleManager
import com.ricedotwho.dtmap.config.EspConfig
import com.ricedotwho.dtmap.config.MapConfig
import com.ricedotwho.dtmap.config.RoomSecrets
import com.ricedotwho.dtmap.config.SoloClearConfig
import com.ricedotwho.dtmap.features.*
import com.ricedotwho.dtmap.features.map.DoorEsp
import com.ricedotwho.dtmap.features.map.DungeonMap
import com.ricedotwho.dtmap.features.map.Mimic
import com.ricedotwho.dtmap.features.map.RoomData
import com.ricedotwho.dtmap.utils.DungeonMessages
import com.ricedotwho.dtmap.utils.MapImageLoader
import com.ricedotwho.dtmap.utils.Scheduler
import com.ricedotwho.dtmap.utils.TickCounters
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object DtMap : ClientModInitializer {
    val LOGGER = LoggerFactory.getLogger("dtmap")
    val zero = 0 to 0
    val dtMapCat = Category.custom("dtMap", 860, 10)

    lateinit var keybindShowHud: KeyMapping

    override fun onInitializeClient() {
        MapImageLoader
        ClientLifecycleEvents.CLIENT_STARTED.register {
            RoomData
        }

        val keybindCategory = KeyMapping.Category(Identifier.fromNamespaceAndPath("dtmap", "dungeons"))

        keybindShowHud = KeyMappingHelper.registerKeyMapping(KeyMapping(
            "key.dtmap.hud",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_LALT,
            keybindCategory
        ))

        DungeonMap.register()
        DungeonMessages.register()
        Key.register()
        Scheduler.register()
        SoloClear.register()
        BloodRushInstaClear.register()
        DoorEsp.register()
        TickCounters.register()
        BloodRush.register()

        Mimic
        RedstoneKeySkullHighlight

        Leap.register()

        ModuleManager.registerModules(ModuleConfig("dtMap.json"), MapConfig, RoomSecrets, EspConfig, SoloClearConfig)
    }
}
