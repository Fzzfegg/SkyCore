package org.mybad.minecraft.gltf;

import net.minecraft.tileentity.TileEntitySkull;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.mybad.minecraft.gltf.client.CustomEntityEventHandler;
import org.mybad.minecraft.gltf.client.CustomPlayerEventHandler;
import org.mybad.minecraft.gltf.client.decoration.DecorationRenderHandler;
import org.mybad.minecraft.gltf.client.decoration.DecorationSkullRenderer;

import org.mybad.minecraft.gltf.resource.GltfResourceAccess;
import org.mybad.minecraft.gltf.resource.GltfResourceLoader;
import org.mybad.minecraft.resource.ResourceCacheManager;

/**
 * Thin bootstrap that wires GLTF-specific event handlers and network listeners
 * into the SkyCore runtime instead of relying on a standalone @Mod entrypoint.
 */
public final class GltfSubsystem {


    private final CustomPlayerEventHandler playerHandler = new CustomPlayerEventHandler();
    private final CustomEntityEventHandler entityHandler = new CustomEntityEventHandler();
    private final DecorationRenderHandler decorationRenderHandler = new DecorationRenderHandler();
    private final GltfResourceLoader resourceLoader;


    public GltfSubsystem(ResourceCacheManager cacheManager) {
        this.resourceLoader = new GltfResourceLoader(cacheManager.getResolver());
    }

    public void install() {
        GltfResourceAccess.install(resourceLoader);
        MinecraftForge.EVENT_BUS.register(playerHandler);
        MinecraftForge.EVENT_BUS.register(entityHandler);
        MinecraftForge.EVENT_BUS.register(decorationRenderHandler);

        ClientRegistry.bindTileEntitySpecialRenderer(TileEntitySkull.class, new DecorationSkullRenderer());

    }

    public void uninstall() {
        MinecraftForge.EVENT_BUS.unregister(playerHandler);
        MinecraftForge.EVENT_BUS.unregister(entityHandler);
        MinecraftForge.EVENT_BUS.unregister(decorationRenderHandler);
        GltfResourceAccess.clear();
    }
}
