package org.mybad.minecraft.render;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

/**
 * GPU 蒙皮能力检测（SSBO + memory barrier）。
 */
public final class GpuSkinningSupport {
    private static boolean detectionComplete = false;
    private static boolean gpuSkinningSupported = false;
    private static boolean contextMissingLogged = false;
    private static boolean forceDisabled = false;

    private GpuSkinningSupport() {
    }

    public static boolean isGpuSkinningAvailable() {
        if (forceDisabled) {
            return false;
        }
        if (!detectionComplete) {
            ContextCapabilities caps = currentCapabilities();
            if (caps == null) {
                if (!contextMissingLogged) {
                    System.out.println("[SkyCore] OpenGL context not ready, skip GPU skinning detection.");
                    contextMissingLogged = true;
                }
                return false;
            }

            contextMissingLogged = false;
            gpuSkinningSupported = detectSupport(caps);
            detectionComplete = true;

            if (gpuSkinningSupported) {
                String version = "unknown";
                try {
                    version = GL11.glGetString(GL11.GL_VERSION);
                } catch (Throwable ignored) {
                }
                System.out.println("[SkyCore] GPU skinning enabled (OpenGL " + version + ").");
            } else {
                System.out.println("[SkyCore] GPU skinning disabled (SSBO not available).");
            }
        }
        return gpuSkinningSupported;
    }

    public static void disableGpuSkinning(String reason) {
        if (!forceDisabled) {
            forceDisabled = true;
            System.out.println("[SkyCore] Disabling GPU skinning: " + reason);
        }
    }

    private static ContextCapabilities currentCapabilities() {
        try {
            return GLContext.getCapabilities();
        } catch (Throwable t) {
            System.out.println("[SkyCore] Unable to query OpenGL capabilities, disabling GPU skinning.");
            return null;
        }
    }

    private static boolean detectSupport(ContextCapabilities caps) {
        boolean hasGl43 = caps.OpenGL43;
        boolean hasSsboExt = caps.GL_ARB_shader_storage_buffer_object;
        boolean hasMemoryBarrierExt = caps.GL_ARB_shader_image_load_store || caps.OpenGL42 || caps.OpenGL43;
        return hasGl43 || (hasSsboExt && hasMemoryBarrierExt);
    }
}
