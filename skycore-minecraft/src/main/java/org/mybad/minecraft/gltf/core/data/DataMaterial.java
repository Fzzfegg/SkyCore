package org.mybad.minecraft.gltf.core.data;

import org.mybad.minecraft.gltf.GltfLog;
import net.minecraft.util.ResourceLocation;
import org.joml.Vector4f;

import java.util.Objects;

public class DataMaterial {
    public String name;
    public boolean isTranslucent;

    public float depthOffset = 0;

    public Vector4f baseColorFactor = new Vector4f(1f, 1f, 1f, 1f);
    public float metallicFactor = 1.0f;
    public float roughnessFactor = 1.0f;

    public String baseColorTexture;
    public String metallicRoughnessTexture;
    public String normalTexture;
    public String occlusionTexture;

    private transient ResourceLocation baseColorResource;
    private transient String cachedBaseColorPath;

    public enum AlphaMode {
        OPAQUE,
        MASK,
        BLEND
    }

    public AlphaMode alphaMode = AlphaMode.MASK;
    public float alphaCutoff = 0.5f;

    public void updateTranslucencyFromAlphaMode() {
        this.isTranslucent = (this.alphaMode == AlphaMode.BLEND);
    }

    public void setBaseColorTexturePath(String path) {
        this.baseColorTexture = path;
        this.cachedBaseColorPath = path;
        this.baseColorResource = buildResource(path);
    }

    public ResourceLocation getBaseColorTextureResource() {
        ensureBaseColorCached();
        return baseColorResource;
    }

    private void ensureBaseColorCached() {
        if (Objects.equals(cachedBaseColorPath, baseColorTexture)) {
            return;
        }
        this.cachedBaseColorPath = baseColorTexture;
        this.baseColorResource = buildResource(baseColorTexture);
    }

    private ResourceLocation buildResource(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        try {
            return new ResourceLocation(path);
        } catch (Exception e) {
            if (GltfLog.LOGGER.isDebugEnabled()) {
                GltfLog.LOGGER.debug("Invalid texture path '{}' for material '{}'", path, name);
            }
            return null;
        }
    }
}
