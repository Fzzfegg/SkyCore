package org.mybad.minecraft.gltf.client.decoration;

import org.mybad.minecraft.gltf.GltfLog;
import org.mybad.minecraft.gltf.client.GltfProfile;
import org.mybad.minecraft.gltf.client.network.RemoteProfileRegistry;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Central controller that scans skull tile entities for decoration tags and renders bound GLTF models.
 */
public final class DecorationManager {

    private static final String NBT_CLIP = "GltfDecorationClip";

    private static final Map<DecorationKey, DecorationInstance> INSTANCES = new HashMap<>();
    private static final Map<DecorationKey, PendingClip> PENDING_CLIPS = new HashMap<>();
    private static boolean DEBUG_BOUNDING_BOX = false;

    private DecorationManager() {
    }

    public static void render(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.world == null || mc.getRenderViewEntity() == null) {
            return;
        }
        World world = mc.world;
        RenderManager renderManager = mc.getRenderManager();
        double cameraX = renderManager.viewerPosX;
        double cameraY = renderManager.viewerPosY;
        double cameraZ = renderManager.viewerPosZ;

        int dimension = world.provider.getDimension();
        Set<DecorationKey> visibleKeys = new HashSet<>();

        for (TileEntity tileEntity : world.loadedTileEntityList) {
            if (!(tileEntity instanceof TileEntitySkull)) {
                continue;
            }
            TileEntitySkull skull = (TileEntitySkull) tileEntity;
            DecorationKey key = new DecorationKey(dimension, skull.getPos());
            if (renderSkullInternal(skull, partialTicks, cameraX, cameraY, cameraZ)) {
                visibleKeys.add(key);
            }
        }

        INSTANCES.entrySet().removeIf(entry -> entry.getKey().dimension == dimension && !visibleKeys.contains(entry.getKey()));
    }

    public static void clear() {
        INSTANCES.values().forEach(DecorationInstance::unbind);
        INSTANCES.clear();
    }

    public static boolean renderTesr(TileEntitySkull skull, double x, double y, double z, float partialTicks) {
        if (skull == null) {
            return false;
        }
        double cameraX = TileEntityRendererDispatcher.staticPlayerX;
        double cameraY = TileEntityRendererDispatcher.staticPlayerY;
        double cameraZ = TileEntityRendererDispatcher.staticPlayerZ;
        return renderSkullInternal(skull, partialTicks, cameraX, cameraY, cameraZ);
    }

    private static boolean renderSkullInternal(TileEntitySkull skull, float partialTicks,
                                               double cameraX, double cameraY, double cameraZ) {
        World world = skull.getWorld();
        if (world == null) {
            return false;
        }

        // 1) 从 SkullOwner 里的 gltf_profile 读取 profileId
        GameProfile skullProfile = skull.getPlayerProfile();
        if (skullProfile == null) {
            return false;
        }
        PropertyMap properties = skullProfile.getProperties();
        if (properties == null) {
            return false;
        }
        String profileId = extractProfileId(properties);
        if (profileId == null) {
            return false;
        }
        GltfProfile config = RemoteProfileRegistry.getProfile(profileId);
        if (config == null) {
            return false;
        }

        DecorationKey key = new DecorationKey(world.provider.getDimension(), skull.getPos());
        DecorationInstance instance = INSTANCES.computeIfAbsent(key, unused -> new DecorationInstance());
        if (!instance.isBoundTo(config)) {
            instance.bindConfiguration(config);
        }

        BlockPos pos = skull.getPos();
        double worldX = pos.getX() + 0.5;
        double worldY = pos.getY();
        double worldZ = pos.getZ() + 0.5;

        double relX = worldX - cameraX;
        double relY = worldY - cameraY;
        double relZ = worldZ - cameraZ;

        float skullYaw = skull.getSkullRotation() * 360.0f / 16.0f;
        float yaw = skullYaw;

        double distanceSq = relX * relX + relY * relY + relZ * relZ;
        if (distanceSq > 256.0 * 256.0) {
            return true; // keep instance cached but skip rendering far away
        }

        PendingClip override = PENDING_CLIPS.get(key);

        // 可选：从方块 NBT 读取要播放的动画片段
        net.minecraft.nbt.NBTTagCompound data = skull.getTileData();
        String clip = data != null && data.hasKey(NBT_CLIP, net.minecraftforge.common.util.Constants.NBT.TAG_STRING)
            ? data.getString(NBT_CLIP)
            : null;

        String requestedClip = override != null ? override.clipId : clip;

        boolean rendered = instance.render(worldX, worldY, worldZ,
            relX, relY, relZ, yaw, 0.0f, 1.0f, partialTicks, requestedClip);
        if (!rendered) {
            GltfLog.LOGGER.debug("Failed to render decoration at {}", pos);
        }

        if (override != null) {
            override.consumeOnce();
            if (!override.shouldKeep()) {
                PENDING_CLIPS.remove(key);
            }
        }

        if (DEBUG_BOUNDING_BOX) {
            drawBoundingBox(relX, relY, relZ);
        }
        return rendered;
    }

    public static void enqueueClip(int x, int y, int z, String clipId, float speed, boolean loop) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.world == null || mc.world.provider == null) {
            return;
        }
        int dimension = mc.world.provider.getDimension();
        DecorationKey key = new DecorationKey(dimension, new BlockPos(x, y, z));
        PENDING_CLIPS.put(key, new PendingClip(clipId, speed, loop));
    }

    private static final class DecorationKey {
        private final int dimension;
        private final long posLong;

        private DecorationKey(int dimension, BlockPos pos) {
            this.dimension = dimension;
            this.posLong = pos.toLong();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof DecorationKey)) {
                return false;
            }
            DecorationKey other = (DecorationKey) obj;
            return this.dimension == other.dimension && this.posLong == other.posLong;
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(dimension);
            result = 31 * result + Long.hashCode(posLong);
            return result;
        }
    }

    private static final class PendingClip {
        private final String clipId;
        private final boolean loop;
        private int ticksRemaining;

        private PendingClip(String clipId, float speed, boolean loop) {
            this.clipId = clipId;
            this.loop = loop;
            // 粗略根据 speed 调整播放保持时间，默认 40tick 约2秒
            this.ticksRemaining = Math.max(1, (int) (40 / Math.max(speed, 0.01f)));
        }

        void consumeOnce() {
            if (!loop && ticksRemaining > 0) {
                ticksRemaining--;
            }
        }

        boolean shouldKeep() {
            return loop || ticksRemaining > 0;
        }
    }

    public static void setDebugBoundingBox(boolean enabled) {
        DEBUG_BOUNDING_BOX = enabled;
    }

    public static boolean isDebugBoundingBoxEnabled() {
        return DEBUG_BOUNDING_BOX;
    }

    public static String extractProfileId(PropertyMap properties) {
        java.util.Collection<Property> custom = properties.get("gltf_profile");
        if (custom != null && !custom.isEmpty()) {
            return custom.iterator().next().getValue();
        }
        return null;
    }

    private static void drawBoundingBox(double relX, double relY, double relZ) {
        org.lwjgl.opengl.GL11.glPushMatrix();
        org.lwjgl.opengl.GL11.glTranslated(relX - 0.5, relY, relZ - 0.5);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        org.lwjgl.opengl.GL11.glLineWidth(2.0f);
        org.lwjgl.opengl.GL11.glColor4f(0.0f, 1.0f, 1.0f, 0.7f);

        org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_LINES);
        // bottom square
        org.lwjgl.opengl.GL11.glVertex3d(0, 0, 0); org.lwjgl.opengl.GL11.glVertex3d(1, 0, 0);
        org.lwjgl.opengl.GL11.glVertex3d(1, 0, 0); org.lwjgl.opengl.GL11.glVertex3d(1, 0, 1);
        org.lwjgl.opengl.GL11.glVertex3d(1, 0, 1); org.lwjgl.opengl.GL11.glVertex3d(0, 0, 1);
        org.lwjgl.opengl.GL11.glVertex3d(0, 0, 1); org.lwjgl.opengl.GL11.glVertex3d(0, 0, 0);
        // top square
        org.lwjgl.opengl.GL11.glVertex3d(0, 1, 0); org.lwjgl.opengl.GL11.glVertex3d(1, 1, 0);
        org.lwjgl.opengl.GL11.glVertex3d(1, 1, 0); org.lwjgl.opengl.GL11.glVertex3d(1, 1, 1);
        org.lwjgl.opengl.GL11.glVertex3d(1, 1, 1); org.lwjgl.opengl.GL11.glVertex3d(0, 1, 1);
        org.lwjgl.opengl.GL11.glVertex3d(0, 1, 1); org.lwjgl.opengl.GL11.glVertex3d(0, 1, 0);
        // verticals
        org.lwjgl.opengl.GL11.glVertex3d(0, 0, 0); org.lwjgl.opengl.GL11.glVertex3d(0, 1, 0);
        org.lwjgl.opengl.GL11.glVertex3d(1, 0, 0); org.lwjgl.opengl.GL11.glVertex3d(1, 1, 0);
        org.lwjgl.opengl.GL11.glVertex3d(1, 0, 1); org.lwjgl.opengl.GL11.glVertex3d(1, 1, 1);
        org.lwjgl.opengl.GL11.glVertex3d(0, 0, 1); org.lwjgl.opengl.GL11.glVertex3d(0, 1, 1);
        org.lwjgl.opengl.GL11.glEnd();

        // restore state
        org.lwjgl.opengl.GL11.glLineWidth(1.0f);
        org.lwjgl.opengl.GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
        org.lwjgl.opengl.GL11.glPopMatrix();
    }
}
