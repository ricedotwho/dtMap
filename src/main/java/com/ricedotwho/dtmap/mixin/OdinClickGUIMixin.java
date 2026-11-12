package com.ricedotwho.dtmap.mixin;

import com.odtheking.odin.OdinMod;
import com.odtheking.odin.clickgui.ClickGUI;
import com.odtheking.odin.utils.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClickGUI.class)
public class OdinClickGUIMixin {
    @Unique
    private final int colour = new Color(255, 255, 255, 0.15f).getRgba();

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    public void oding(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        var mc = OdinMod.getMc();
        graphics.pose().pushMatrix();
        var window = mc.getWindow();
        graphics.pose().scale(1F / window.getGuiScale());
        graphics.pose().translate((window.getWidth() - mc.font.width("thanks oding") * 10) / 2f, (window.getHeight() * 0.8f));
        graphics.pose().scale(10f);
        graphics.text(mc.font, "thanks oding", 0, 0, colour, false);
        graphics.pose().popMatrix();
    }
}
