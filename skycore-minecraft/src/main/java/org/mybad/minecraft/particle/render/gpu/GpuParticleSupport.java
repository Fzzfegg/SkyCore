package org.mybad.minecraft.particle.render.gpu;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.mybad.minecraft.SkyCoreMod;

/**
 * GPU 粒子渲染能力检测（SSBO + instancing + memory barrier）。
 */
public final class GpuParticleSupport {
    private static boolean detectionComplete = false;
    private static boolean gpuParticleSupported = false;
    private static boolean forceDisabled = false;

    private GpuParticleSupport() {
    }

    public static boolean isGpuParticleAvailable() {
        if (forceDisabled) {
            return false;
        }
        if (!detectionComplete) {
            ContextCapabilities caps = currentCapabilities();
            if (caps == null) {
                return false;
            }

            gpuParticleSupported = detectSupport(caps);
            detectionComplete = true;

            if (gpuParticleSupported) {
                String version = "unknown";
                try {
                    version = GL11.glGetString(GL11.GL_VERSION);
                } catch (Throwable ignored) {
                }
                SkyCoreMod.LOGGER.info("[SkyCore] GPU particle rendering enabled (OpenGL {}).", version);
            } else {
                SkyCoreMod.LOGGER.warn("[SkyCore] GPU particle rendering disabled (SSBO not available)." );
            }
        }
        return gpuParticleSupported;
    }

    public static void disable(String reason) {
        if (!forceDisabled) {
            forceDisabled = true;
            SkyCoreMod.LOGGER.warn("[SkyCore] Disabling GPU particle rendering: {}", reason);
        }
    }

    private static ContextCapabilities currentCapabilities() {
        try {
            return GLContext.getCapabilities();
        } catch (Throwable t) {
            SkyCoreMod.LOGGER.warn("[SkyCore] Unable to query OpenGL capabilities, disabling GPU particles.");
            return null;
        }
    }

    private static boolean detectSupport(ContextCapabilities caps) {
        boolean hasGl43 = caps.OpenGL43;
        boolean hasSsboExt = caps.GL_ARB_shader_storage_buffer_object;
        boolean hasMemoryBarrierExt = caps.GL_ARB_shader_image_load_store || caps.OpenGL42 || caps.OpenGL43;
        boolean hasInstancing = caps.OpenGL31;
        return (hasGl43 || (hasSsboExt && hasMemoryBarrierExt)) && hasInstancing;
    }
}
