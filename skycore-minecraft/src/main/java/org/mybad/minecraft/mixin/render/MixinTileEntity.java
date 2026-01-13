package org.mybad.minecraft.mixin.render;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import org.mybad.minecraft.render.skull.SkullModelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntity.class)
public abstract class MixinTileEntity {

    @Inject(method = "invalidate", at = @At("HEAD"), remap = false)
    private void skycore$invalidate(CallbackInfo ci) {
        if ((Object) this instanceof TileEntitySkull) {
            SkullModelManager.remove((TileEntitySkull) (Object) this);
        }
    }

    @Inject(method = "onChunkUnload", at = @At("HEAD"), remap = false)
    private void skycore$chunkUnload(CallbackInfo ci) {
        if ((Object) this instanceof TileEntitySkull) {
            SkullModelManager.remove((TileEntitySkull) (Object) this);
        }
    }
}
