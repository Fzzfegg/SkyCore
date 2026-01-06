package org.mybad.minecraft.mixin.render;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.mybad.minecraft.render.entity.EntityMappingResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Render.class)
public class MixinRender {

    @Inject(method = "doRenderShadowAndFire", at = @At("HEAD"), cancellable = true)
    private void skycore$skipShadow(Entity entity, double x, double y, double z, float yaw, float partialTicks, CallbackInfo ci) {
        if (!(entity instanceof EntityLivingBase)) {
            return;
        }
        EntityMappingResolver.MappingResult mapping = EntityMappingResolver.resolve((EntityLivingBase) entity);
        if (mapping != null && mapping.mapping != null && !mapping.mapping.isRenderShadow()) {
            ci.cancel();
        }
    }
}
