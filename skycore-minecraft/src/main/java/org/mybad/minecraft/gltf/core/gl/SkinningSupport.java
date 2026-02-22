package org.mybad.minecraft.gltf.core.gl;

import org.mybad.minecraft.gltf.GltfLog;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

/**
 * Centralizes OpenGL capability checks for the GPU skinning path so we can
 * gracefully fall back on CPUs when SSBOs or modern barriers are missing.
 */
public final class SkinningSupport {
    private static boolean detectionComplete = false;
    private static boolean gpuSkinningSupported = false;
    private static boolean contextMissingLogged = false;
    private static boolean forceDisabled = false;

    private SkinningSupport() {
    }

    /**
     * @return true if the current OpenGL context exposes the minimum feature set
     *         required for the SSBO-based skinning shader.
     */
    public static boolean isGpuSkinningAvailable() {
        if (forceDisabled) {
            return false;
        }
        if (!detectionComplete) {
            ContextCapabilities caps = currentCapabilities();
            if (caps == null) {
                if (!contextMissingLogged) {
                    GltfLog.LOGGER.info("OpenGL context not ready yet, deferring GPU skinning detection.");
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
                GltfLog.LOGGER.info("GPU skinning enabled (OpenGL {} detected).", version);
            } else {
                GltfLog.LOGGER.warn(
                    "GPU skinning disabled: OpenGL 4.3+ / ARB_shader_storage_buffer_object not available.");
            }
        }
        return gpuSkinningSupported;
    }

    public static void disableGpuSkinning(String reason) {
        if (!forceDisabled) {
            forceDisabled = true;
            GltfLog.LOGGER.warn("Disabling GPU skinning: {}", reason);
        }
    }

    private static ContextCapabilities currentCapabilities() {
        try {
            return GLContext.getCapabilities();
        } catch (Throwable t) {
            GltfLog.LOGGER.warn("Unable to query OpenGL capabilities, disabling GPU skinning.", t);
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
