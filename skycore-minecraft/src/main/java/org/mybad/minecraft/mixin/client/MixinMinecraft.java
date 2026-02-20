package org.mybad.minecraft.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Shadow public EntityPlayerSP player;

    @Inject(method = "processKeyF3", at = @At("HEAD"), cancellable = true)
    private void skycore$onlyAllowHitboxKeyInCreative(int key, CallbackInfoReturnable<Boolean> cir) {
        if (key != Keyboard.KEY_B) {
            return;
        }
        if (skycore$canShowHitboxes()) {
            return;
        }
        cir.setReturnValue(true);
    }

    private boolean skycore$canShowHitboxes() {
        return player != null
            && player.capabilities != null
            && player.capabilities.isCreativeMode;
    }
}
