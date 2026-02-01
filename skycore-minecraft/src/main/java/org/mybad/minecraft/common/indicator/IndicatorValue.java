package org.mybad.minecraft.common.indicator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

public class IndicatorValue {
    private static final Map<UUID, Entity> ENTITY_CACHE = new HashMap<>();
    private static WorldClient CACHED_WORLD;

    private UUID entityA;
    private UUID entityB;
    private final double fallbackValue;
    private final BiFunction<Entity, Entity, Double> valueSupplier;

    public IndicatorValue(double fallbackValue, BiFunction<Entity, Entity, Double> supplier) {
        this.fallbackValue = fallbackValue;
        this.valueSupplier = supplier;
    }

    public IndicatorValue(PacketBuffer buffer, BiFunction<Entity, Entity, Double> supplier) {
        this.valueSupplier = supplier;
        this.fallbackValue = buffer.readDouble();
        byte flag = buffer.readByte();
        if (flag == 0) {
            this.entityA = buffer.readUniqueId();
        } else if (flag == 1) {
            this.entityA = buffer.readUniqueId();
            this.entityB = buffer.readUniqueId();
        }
    }

    public void setEntityA(UUID uuid) {
        this.entityA = uuid;
    }

    public void setEntityB(UUID uuid) {
        this.entityB = uuid;
    }

    public double computeValue() {
        if (valueSupplier == null) {
            return fallbackValue;
        }
        if (this.entityB != null) {
            Entity first = resolveEntity(this.entityA);
            Entity second = resolveEntity(this.entityB);
            if (first == null || second == null) {
                return fallbackValue;
            }
            Double dynamic = valueSupplier.apply(first, second);
            return (dynamic != null ? dynamic : 0.0d) + fallbackValue;
        }
        if (this.entityA != null) {
            Entity entity = resolveEntity(this.entityA);
            if (entity == null) {
                return fallbackValue;
            }
            Double dynamic = valueSupplier.apply(entity, null);
            return (dynamic != null ? dynamic : 0.0d) + fallbackValue;
        }
        Double dynamic = valueSupplier.apply(null, null);
        return dynamic != null ? dynamic + fallbackValue : fallbackValue;
    }

    private static Entity resolveEntity(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return null;
        }
        WorldClient world = mc.world;
        if (world == null) {
            ENTITY_CACHE.clear();
            CACHED_WORLD = null;
            return null;
        }
        if (CACHED_WORLD != world) {
            ENTITY_CACHE.clear();
            CACHED_WORLD = world;
        }
        Entity cached = ENTITY_CACHE.get(uuid);
        if (cached != null && !cached.isDead && cached.world == world && uuid.equals(cached.getUniqueID())) {
            return cached;
        }
        for (Entity entity : world.getLoadedEntityList()) {
            if (uuid.equals(entity.getUniqueID())) {
                ENTITY_CACHE.put(uuid, entity);
                return entity;
            }
        }
        ENTITY_CACHE.remove(uuid);
        return null;
    }
}
