package com.ricedotwho.dtmap.utils

import com.odtheking.odin.OdinMod.mc
import com.ricedotwho.dtmap.events.ChatEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import java.util.regex.Pattern

object DungeonMessages {
    val witherKeyClaimPattern = Pattern.compile("(?:\\[[A-Za-z+]+] )?([A-Za-z0-9_]+) has obtained Wither Key!")
    const val witherKeyPickedUpString = "A Wither Key was picked up!"
    val witherDoorOpenPattern = Pattern.compile("([A-Za-z0-9_]+) opened a WITHER door!")
    val bloodKeyClaimPattern = Pattern.compile("(?:\\[[A-Za-z+]+] )?([A-Za-z0-9_]+) has obtained Blood Key!")
    const val bloodKeyPickedUpString = "A Blood Key was picked up!"
    const val bloodDoorOpenMessage = "The BLOOD DOOR has been opened!"

    val bossEntryMessages = arrayOf(
        "[BOSS] Bonzo: Gratz for making it this far, but I'm basically unbeatable.",
        "[BOSS] Scarf: This is where the journey ends for you, Adventurers.",
        "[BOSS] The Professor: I was burdened with terrible news recently...",
        "[BOSS] Thorn: Welcome Adventurers! I am Thorn, the Spirit! And host of the Vegan Trials!",
        "[BOSS] Livid: Welcome, you've arrived right on time. I am Livid, the Master of Shadows.",
        "[BOSS] Sadan: So you made it all the way here... Now you wish to defy me? Sadan?!",
        "[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!"
    )

    val dungeonEndString = listOf(
        "                        The Catacombs - Entrance",
        "                         The Catacombs - Floor I",
        "                         The Catacombs - Floor II",
        "                        The Catacombs - Floor III",
        "                        The Catacombs - Floor IV",
        "                         The Catacombs - Floor V",
        "                        The Catacombs - Floor VI",
        "                        The Catacombs - Floor VII",
        "                 Master Mode The Catacombs - Floor I",
        "                Master Mode The Catacombs - Floor II",
        "                Master Mode The Catacombs - Floor III",
        "                Master Mode The Catacombs - Floor IV",
        "                 Master Mode The Catacombs - Floor V",
        "                Master Mode The Catacombs - Floor VI",
        "                Master Mode The Catacombs - Floor VII"
    )

    const val dungeonStartMessage = "[NPC] Mort: Here, I found this map when I first entered the dungeon."

    var inBoss = false
    var dungeonEnded = false
    val isDead: Boolean
        get() {
            return mc.player?.inventory?.getItem(0)?.customName?.string == "Haunt"
        }
    var seenDungeonStart = false

    fun register() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ ->
            inBoss = false
            dungeonEnded = false
            seenDungeonStart = false
        }
        ChatEvents.SYSTEM_MESSAGE_CLIENT_EVENT.register(AddMessage)
    }

    val AddMessage = ChatEvents.SystemMessageEvent { (component, message, unformatted) ->
        if (bossEntryMessages.contains(unformatted)) inBoss = true
        else if (dungeonEndString.contains(unformatted)) dungeonEnded = true
        else if (unformatted == dungeonStartMessage) seenDungeonStart = true
    }
}