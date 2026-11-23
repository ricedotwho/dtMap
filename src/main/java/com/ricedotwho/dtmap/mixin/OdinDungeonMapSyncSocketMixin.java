package com.ricedotwho.dtmap.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.ricedotwho.dtmap.config.MapConfig;
import com.ricedotwho.dtmap.features.map.Scan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.Reader;

@Pseudo
@Mixin(targets = "com.odtheking.odin.features.impl.dungeon.map.DungeonMap$syncSocket$1$1")
public class OdinDungeonMapSyncSocketMixin {
    @Inject(method = "invoke(Ljava/lang/String;)V", at = @At("HEAD"))
    private void dtMap$onOdinWSMessage(String message, CallbackInfo ci) {
        if (!MapConfig.INSTANCE.getOdinWS()) return;
        JsonElement object = JsonParser.parseReader(Reader.of(message));
        if (object == null || !object.isJsonObject()) return;
        Scan.INSTANCE.parseOdinMessage(object.getAsJsonObject());
    }
}
