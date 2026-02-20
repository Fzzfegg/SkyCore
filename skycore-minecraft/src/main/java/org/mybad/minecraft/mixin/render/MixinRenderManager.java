package org.mybad.minecraft.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.minecraft.render.entity.EntityMappingResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(RenderManager.class)
public class MixinRenderManager {

    @Shadow private boolean debugBoundingBox;

    @Inject(method = "shouldRender(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;DDD)Z",
        at = @At("HEAD"), cancellable = true)
    private void skycore$forceRender(Entity entity, net.minecraft.client.renderer.culling.ICamera camera,
                                     double camX, double camY, double camZ,
                                     CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof EntityLivingBase)) {
            return;
        }
        EntityMappingResolver.MappingResult mapping = EntityMappingResolver.resolve((EntityLivingBase) entity);
        if (mapping != null) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "setDebugBoundingBox", at = @At("HEAD"), cancellable = true)
    private void skycore$restrictDebugBoundingBox(boolean enabled, CallbackInfo ci) {
        if (enabled && !skycore$canShowHitboxes()) {
            this.debugBoundingBox = false;
            ci.cancel();
        }
    }
    

    private boolean skycore$canShowHitboxes() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.player.capabilities == null) {
            return false;
        }
        return mc.player.capabilities.isCreativeMode;
    }
}
