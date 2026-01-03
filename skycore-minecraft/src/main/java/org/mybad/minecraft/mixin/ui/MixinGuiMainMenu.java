package org.mybad.minecraft.mixin.ui;

import net.minecraft.client.gui.GuiMainMenu;
import org.mybad.minecraft.SkyCoreMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu {
    private static boolean logged;

    @Inject(method = "initGui", at = @At("HEAD"))
    private void skycore$initGui(CallbackInfo ci) {
        if (!logged) {
            logged = true;
            SkyCoreMod.LOGGER.info("[SkyCore][Mixin] GuiMainMenu initGui hook ok.");
        }
    }
}
