package com.ricedotwho.dtmap.mixin;

import com.ricedotwho.dtmap.features.RoomSecrets;
import com.ricedotwho.dtmap.features.SoloClear;
import com.ricedotwho.dtmap.features.map.DungeonMap;
import com.ricedotwho.dtmap.features.map.Mimic;
import com.ricedotwho.dtmap.features.map.Scoreboard;
import com.ricedotwho.dtmap.utils.Location;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "handleMapItemData", at = @At("TAIL"))
    void handleMapItemData(ClientboundMapItemDataPacket packet, CallbackInfo ci) {
        DungeonMap.INSTANCE.rescanMapItem(packet);
    }

    @Inject(method = "handleOpenScreen(Lnet/minecraft/network/protocol/game/ClientboundOpenScreenPacket;)V", at = @At("HEAD"), cancellable = true)
    void handleOpenScreen(ClientboundOpenScreenPacket clientboundOpenScreenPacket, CallbackInfo ci) {
        if (Location.INSTANCE.getIsland() != Location.Island.Dungeon) return;
        SoloClear.INSTANCE.openedWindow(clientboundOpenScreenPacket, ci);
    }

    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    void handleEntityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        Mimic.INSTANCE.entityEvent(packet);
    }

    @Inject(method = "handleRemoveEntities(Lnet/minecraft/network/protocol/game/ClientboundRemoveEntitiesPacket;)V", at = @At("TAIL"))
    void handleRemoveEntities(ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) {
        Scoreboard.INSTANCE.removedEntities(packet);
    }

    @Inject(method = "handleSetPlayerTeamPacket", at = @At("TAIL"))
    void handleSetPlayerTeam(ClientboundSetPlayerTeamPacket packet, CallbackInfo ci) {
        Scoreboard.INSTANCE.handleSetPlayerTeam(packet);
    }

    @Inject(method = "handlePlayerInfoUpdate", at = @At("TAIL"))
    void handleInfoUpdate(ClientboundPlayerInfoUpdatePacket packet, CallbackInfo ci) {
        Scoreboard.INSTANCE.handlePlayerInfoUpdate(packet);
    }
}
