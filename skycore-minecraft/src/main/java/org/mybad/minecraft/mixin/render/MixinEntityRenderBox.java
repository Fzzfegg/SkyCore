package org.mybad.minecraft.mixin.render;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.render.entity.EntityMappingResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntityRenderBox {

    @Inject(method = "getRenderBoundingBox", at = @At("HEAD"), cancellable = true)
    private void skycore$expandRenderBoundingBox(CallbackInfoReturnable<AxisAlignedBB> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof EntityLivingBase)) {
            return;
        }
        EntityMappingResolver.MappingResult mappingResult = EntityMappingResolver.resolve((EntityLivingBase) self);
        if (mappingResult == null) {
            return;
        }
        EntityModelMapping mapping = mappingResult.mapping;
        if (mapping == null || !mapping.hasCustomRenderBox()) {
            return;
        }
        AxisAlignedBB custom = buildBoundingBox(self, mapping);
        if (custom == null) {
            return;
        }
        AxisAlignedBB union = self.getEntityBoundingBox().union(custom);
        cir.setReturnValue(union);
    }

    private static AxisAlignedBB buildBoundingBox(Entity entity, EntityModelMapping mapping) {
        double width = mapping.getRenderBoxWidth();
        double height = mapping.getRenderBoxHeight();
        double depth = mapping.getRenderBoxDepth();
        if (width <= 0.0 || height <= 0.0 || depth <= 0.0) {
            return null;
        }
        double halfWidth = width * 0.5;
        double halfDepth = depth * 0.5;
        double minX = entity.posX - halfWidth;
        double maxX = entity.posX + halfWidth;
        double minZ = entity.posZ - halfDepth;
        double maxZ = entity.posZ + halfDepth;
        double minY = entity.posY;
        double maxY = entity.posY + height;
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
