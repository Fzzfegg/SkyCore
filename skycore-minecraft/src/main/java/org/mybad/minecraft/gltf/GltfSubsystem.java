package org.mybad.minecraft.gltf;

import net.minecraft.tileentity.TileEntitySkull;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.mybad.minecraft.gltf.client.CustomEntityEventHandler;
import org.mybad.minecraft.gltf.client.decoration.DecorationSkullRenderer;

import org.mybad.minecraft.gltf.resource.GltfResourceAccess;
import org.mybad.minecraft.gltf.resource.GltfResourceLoader;
import org.mybad.minecraft.resource.ResourceCacheManager;

/**
 * Thin bootstrap that wires GLTF-specific event handlers and network listeners
 * into the SkyCore runtime instead of relying on a standalone @Mod entrypoint.
 */
public final class GltfSubsystem {


    private final CustomEntityEventHandler entityHandler = new CustomEntityEventHandler();
    private final GltfResourceLoader resourceLoader;


    public GltfSubsystem(ResourceCacheManager cacheManager) {
        this.resourceLoader = new GltfResourceLoader(cacheManager);
    }

    public void install() {
        GltfResourceAccess.install(resourceLoader);
        MinecraftForge.EVENT_BUS.register(entityHandler);
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntitySkull.class, new DecorationSkullRenderer());

    }

    public void uninstall() {
        MinecraftForge.EVENT_BUS.unregister(entityHandler);
        GltfResourceAccess.clear();
    }
}
