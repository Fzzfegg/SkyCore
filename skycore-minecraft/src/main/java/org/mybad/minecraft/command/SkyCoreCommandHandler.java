package org.mybad.minecraft.command;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;
import org.mybad.minecraft.event.EntityRenderEventHandler;
import org.mybad.minecraft.debug.BedrockParticleDebugSystem;
import org.mybad.minecraft.render.GeometryCache;
import org.mybad.minecraft.resource.ResourceLoader;

/**
 * Reload/Debug chat command handler.
 * Listens to client chat and handles /skycore commands.
 */
@SideOnly(Side.CLIENT)
public class SkyCoreCommandHandler {

    @SubscribeEvent
    public void onClientChat(ClientChatEvent event) {
        String message = event.getMessage();
        if (message.equalsIgnoreCase("/skycore reload")) {
            if (SkyCoreMod.instance != null) {
                SkyCoreMod.instance.reload();
                Minecraft.getMinecraft().player.sendMessage(
                    new TextComponentString("\u00a7a[SkyCore] \u914d\u7f6e\u5df2\u91cd\u65b0\u52a0\u8f7d")
                );
            }
            return;
        }

        if (message.equalsIgnoreCase("/skycore geomstats")) {
            if (SkyCoreMod.instance != null) {
                ResourceLoader loader = SkyCoreMod.instance.getResourceLoader();
                if (loader != null) {
                    GeometryCache.Stats stats = loader.getGeometryCache().getStats();
                    Minecraft.getMinecraft().player.sendMessage(
                        new TextComponentString("[SkyCore] \u51e0\u4f55\u7f13\u5b58\u7edf\u8ba1: " + stats.toString())
                    );
                }
            }
            return;
        }

        if (message.equalsIgnoreCase("/skycore particlestats")) {
            if (SkyCoreMod.instance != null) {
                ResourceLoader loader = SkyCoreMod.instance.getResourceLoader();
                BedrockParticleDebugSystem particleDebug = SkyCoreMod.getParticleDebugSystem();
                if (loader != null && particleDebug != null) {
                    int cached = loader.getCachedParticleCount();
                    int active = particleDebug.getActiveCount();
                    Minecraft.getMinecraft().player.sendMessage(
                        new TextComponentString("[SkyCore] \u7c92\u5b50\u7f13\u5b58: " + cached + " / \u6d3b\u52a8\u7c92\u5b50: " + active)
                    );
                }
            }
            return;
        }

        if (message.equalsIgnoreCase("/skycore particle_clear")) {
            BedrockParticleDebugSystem particleDebug = SkyCoreMod.getParticleDebugSystem();
            if (particleDebug != null) {
                particleDebug.clear();
                Minecraft.getMinecraft().player.sendMessage(
                    new TextComponentString("[SkyCore] \u5df2\u6e05\u7a7a\u8c03\u8bd5\u7c92\u5b50")
                );
            }
            return;
        }

        if (message.equalsIgnoreCase("/skycore debug_clear")) {
            EntityRenderEventHandler handler = SkyCoreMod.getEntityRenderEventHandler();
            if (handler != null) {
                handler.clearDebugStacks();
                Minecraft.getMinecraft().player.sendMessage(
                    new TextComponentString("[SkyCore] \u8c03\u8bd5\u5806\u53e0\u5df2\u6e05\u7a7a")
                );
            }
            return;
        }

        if (message.startsWith("/skycore debug_stack")) {
            EntityRenderEventHandler handler = SkyCoreMod.getEntityRenderEventHandler();
            if (handler == null) {
                return;
            }
            String[] parts = message.trim().split("\\s+");
            if (parts.length < 3) {
                Minecraft.getMinecraft().player.sendMessage(
                    new TextComponentString("[SkyCore] \u7528\u6cd5: /skycore debug_stack <\u540d\u5b57> [\u6570\u91cf] [\u95f4\u8ddd]")
                );
                return;
            }
            String mappingName = parts[2];
            int count = 20;
            double spacing = 1.0;
            if (parts.length >= 4) {
                try { count = Integer.parseInt(parts[3]); } catch (NumberFormatException ignored) {}
            }
            if (parts.length >= 5) {
                try { spacing = Double.parseDouble(parts[4]); } catch (NumberFormatException ignored) {}
            }
            if (count < 1) { count = 1; } else if (count > 200) { count = 200; }
            if (spacing <= 0.0) { spacing = 1.0; }

            Minecraft mc = Minecraft.getMinecraft();
            if (mc.player == null) {
                return;
            }
            double x = mc.player.posX;
            double y = mc.player.posY;
            double z = mc.player.posZ;
            float yaw = mc.player.rotationYaw;

            boolean ok = handler.addDebugStack(mappingName, x, y, z, yaw, count, spacing);
            if (ok) {
                mc.player.sendMessage(new TextComponentString("[SkyCore] \u5df2\u521b\u5efa\u8c03\u8bd5\u5806\u53e0: " + mappingName + "\uff0c\u6570\u91cf=" + count + "\uff0c\u95f4\u8ddd=" + spacing));
            } else {
                mc.player.sendMessage(new TextComponentString("[SkyCore] \u521b\u5efa\u8c03\u8bd5\u5806\u53e0\u5931\u8d25\uff1a\u672a\u627e\u5230\u6620\u5c04\u6216\u6a21\u578b\u52a0\u8f7d\u5931\u8d25"));
            }
            return;
        }

        if (message.startsWith("/skycore play_anim_clear")) {
            EntityRenderEventHandler handler = SkyCoreMod.getEntityRenderEventHandler();
            if (handler == null) {
                return;
            }
            String[] parts = message.trim().split("\\s+");
            if (parts.length < 3) {
                handler.clearAllForcedAnimations();
                Minecraft.getMinecraft().player.sendMessage(
                    new TextComponentString("[SkyCore] \u5df2\u6e05\u7a7a\u6240\u6709\u5f3a\u5236\u52a8\u753b")
                );
                return;
            }
            String mappingName = parts[2];
            handler.clearForcedAnimation(mappingName);
            Minecraft.getMinecraft().player.sendMessage(
                new TextComponentString("[SkyCore] \u5df2\u6e05\u9664\u5f3a\u5236\u52a8\u753b: " + mappingName)
            );
            return;
        }

        if (message.startsWith("/skycore play_anim")) {
            if (SkyCoreMod.instance == null) {
                return;
            }
            EntityRenderEventHandler handler = SkyCoreMod.getEntityRenderEventHandler();
            ResourceLoader loader = SkyCoreMod.instance.getResourceLoader();
            if (handler == null || loader == null) {
                return;
            }
            String[] parts = message.trim().split("\\s+");
            if (parts.length < 4) {
                Minecraft.getMinecraft().player.sendMessage(
                    new TextComponentString("[SkyCore] \u7528\u6cd5: /skycore play_anim <\u540d\u5b57> <\u52a8\u753b\u7247\u6bb5\u540d>")
                );
                return;
            }
            String mappingName = parts[2];
            String clipName = parts[3];
            EntityModelMapping mapping = SkyCoreConfig.getInstance().getMapping(mappingName);
            if (mapping == null) {
                Minecraft.getMinecraft().player.sendMessage(
                    new TextComponentString("[SkyCore] \u672a\u627e\u5230\u6620\u5c04: " + mappingName)
                );
                return;
            }
            String animPath = mapping.getAnimation();
            if (animPath == null || animPath.isEmpty()) {
                Minecraft.getMinecraft().player.sendMessage(
                    new TextComponentString("[SkyCore] \u8be5\u6620\u5c04\u6ca1\u6709\u52a8\u753b\u6587\u4ef6: " + mappingName)
                );
                return;
            }
            Animation animation = loader.loadAnimation(animPath, clipName);
            if (animation == null) {
                Minecraft.getMinecraft().player.sendMessage(
                    new TextComponentString("[SkyCore] \u52a8\u753b\u7247\u6bb5\u4e0d\u5b58\u5728: " + clipName)
                );
                return;
            }
            boolean ok = handler.setForcedAnimation(mappingName, animation);
            if (ok) {
                Minecraft.getMinecraft().player.sendMessage(
                    new TextComponentString("[SkyCore] \u5df2\u5f3a\u5236\u64ad\u653e\u52a8\u753b: " + mappingName + " -> " + clipName)
                );
            } else {
                Minecraft.getMinecraft().player.sendMessage(
                    new TextComponentString("[SkyCore] \u5f3a\u5236\u64ad\u653e\u5931\u8d25")
                );
            }
            return;
        }

        if (message.startsWith("/skycore particle ")) {
            BedrockParticleDebugSystem particleDebug = SkyCoreMod.getParticleDebugSystem();
            if (particleDebug == null) {
                return;
            }
            String[] parts = message.trim().split("\\s+");
            if (parts.length < 3) {
                Minecraft.getMinecraft().player.sendMessage(
                    new TextComponentString("[SkyCore] \u7528\u6cd5: /skycore particle <\u7c92\u5b50\u6587\u4ef6\u8def\u5f84> [\u6570\u91cf]")
                );
                return;
            }
            String path = parts[2];
            int count = 0;
            if (parts.length >= 4) {
                try { count = Integer.parseInt(parts[3]); } catch (NumberFormatException ignored) {}
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.player == null) {
                return;
            }
            boolean ok = particleDebug.spawn(path, mc.player.posX, mc.player.posY + 1.0, mc.player.posZ, count);
            if (ok) {
                mc.player.sendMessage(new TextComponentString("[SkyCore] \u5df2\u751f\u6210\u8c03\u8bd5\u7c92\u5b50: " + path));
            } else {
                mc.player.sendMessage(new TextComponentString("[SkyCore] \u751f\u6210\u7c92\u5b50\u5931\u8d25: " + path));
            }
        }
    }
}
