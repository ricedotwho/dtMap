package com.ricedotwho.dtmap.mixin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.odtheking.odin.config.ModuleConfig;
import com.odtheking.odin.features.Module;
import com.ricedotwho.dtmap.utils.settings.Saving;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(ModuleConfig.class)
public class MixinModuleConfig {
    @Final
    @Shadow
    private static Gson gson;

    @ModifyExpressionValue(method = "load()V", at = @At(value = "INVOKE", target = "Ljava/util/LinkedHashMap;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object onLoad(Object original, @Local(name = "value") JsonElement value) {
        if (original instanceof Saving saving) {
            saving.read(value, gson);
        }
        return original;
    }

    @ModifyExpressionValue(method = "save", at = @At(value = "INVOKE", target = "Ljava/util/Map$Entry;getValue()Ljava/lang/Object;"))
    private Object onSave(Object original, @Local(ordinal = 1) String name, @Local(ordinal = 2) JsonObject obj) {
        if (original instanceof Saving saving) {
            obj.add(name, saving.write(gson));
        }
        return original;
    }
}
