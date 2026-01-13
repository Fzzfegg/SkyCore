package org.mybad.minecraft.mixin.render;

import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntitySkull;
import org.mybad.minecraft.render.skull.SkullModelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntitySkullRenderer.class)
public abstract class MixinTileEntitySkullRenderer extends TileEntitySpecialRenderer<TileEntitySkull> {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void skycore$renderSkull(TileEntitySkull tile,
                                     double x,
                                     double y,
                                     double z,
                                     float partialTicks,
                                     int destroyStage,
                                     float alpha,
                                     CallbackInfo ci) {
        if (destroyStage >= 0) {
            return;
        }
        if (SkullModelManager.render(tile, x, y, z, partialTicks)) {
            ci.cancel();
        }
    }

    @Override
    public boolean isGlobalRenderer(TileEntitySkull tile) {
        return SkullModelManager.isGlobalRenderer(tile) || super.isGlobalRenderer(tile);
    }
}
