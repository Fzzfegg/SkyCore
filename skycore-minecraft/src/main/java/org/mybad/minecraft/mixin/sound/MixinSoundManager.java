package org.mybad.minecraft.mixin.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.event.EntityRenderEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;

@Mixin(SoundManager.class)
public abstract class MixinSoundManager {

    @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
    private void skycore$muteSounds(ISound sound, CallbackInfo ci) {
        if (sound == null) {
            return;
        }
        EntityRenderEventHandler handler = SkyCoreMod.getEntityRenderEventHandler();
        if (handler == null) {
            return;
        }
        Entity entity = resolveEntity(sound);
        if (entity instanceof EntityLivingBase && handler.isSkyCoreEntity(entity)) {
            ci.cancel();
            return;
        }
        if (shouldMuteByPosition(sound, handler)) {
            ci.cancel();
        }
    }

    private boolean shouldMuteByPosition(ISound sound, EntityRenderEventHandler handler) {
        WorldClient world = Minecraft.getMinecraft().world;
        if (world == null) {
            return false;
        }
        float x = sound.getXPosF();
        float y = sound.getYPosF();
        float z = sound.getZPosF();
        double radius = 0.6;
        AxisAlignedBB box = new AxisAlignedBB(
            x - radius, y - radius, z - radius,
            x + radius, y + radius, z + radius
        );
        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class, box);
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        for (EntityLivingBase living : entities) {
            if (living != null && handler.isSkyCoreEntity(living)) {
                return true;
            }
        }
        return false;
    }

    private Entity resolveEntity(ISound sound) {
        Class<?> type = sound.getClass();
        while (type != null && type != Object.class) {
            Field[] fields = type.getDeclaredFields();
            for (Field field : fields) {
                if (!Entity.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(sound);
                    if (value instanceof Entity) {
                        return (Entity) value;
                    }
                } catch (Throwable ignored) {}
            }
            type = type.getSuperclass();
        }
        return null;
    }
}
