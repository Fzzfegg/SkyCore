package org.mybad.minecraft.event;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.resource.ResourceLoader;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
final class DebugStackManager {
    private final ResourceLoader resourceLoader;
    private final List<DebugStack> debugStacks;

    DebugStackManager(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.debugStacks = new ArrayList<>();
    }

    void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getRenderManager() == null) {
            return;
        }
        double camX = mc.getRenderManager().viewerPosX;
        double camY = mc.getRenderManager().viewerPosY;
        double camZ = mc.getRenderManager().viewerPosZ;
        float partialTicks = event.getPartialTicks();

        if (debugStacks.isEmpty()) {
            return;
        }

        synchronized (debugStacks) {
            for (DebugStack stack : debugStacks) {
                for (int i = 0; i < stack.count; i++) {
                    double y = stack.y + i * stack.spacing;
                    stack.wrapper.render(null,
                        stack.x - camX,
                        y - camY,
                        stack.z - camZ,
                        stack.yaw,
                        partialTicks
                    );
                }
            }
        }
    }

    void clearDebugStacks() {
        synchronized (debugStacks) {
            for (DebugStack stack : debugStacks) {
                stack.wrapper.dispose();
            }
            debugStacks.clear();
        }
    }

    boolean addDebugStack(String mappingName, double x, double y, double z, float yaw, int count, double spacing) {
        EntityModelMapping mapping = SkyCoreConfig.getInstance().getMapping(mappingName);
        if (mapping == null) {
            return false;
        }

        BedrockModelHandle wrapper = ModelHandleFactory.create(resourceLoader, mapping, "debug stack");
        if (wrapper == null) {
            return false;
        }
        wrapper.setEmissiveStrength(mapping.getEmissiveStrength());
        wrapper.setModelScale(mapping.getModelScale());

        synchronized (debugStacks) {
            debugStacks.add(new DebugStack(wrapper, x, y, z, yaw, count, spacing));
        }
        return true;
    }

    private static final class DebugStack {
        private final BedrockModelHandle wrapper;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final int count;
        private final double spacing;

        private DebugStack(BedrockModelHandle wrapper, double x, double y, double z, float yaw, int count, double spacing) {
            this.wrapper = wrapper;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.count = count;
            this.spacing = spacing;
        }
    }
}
