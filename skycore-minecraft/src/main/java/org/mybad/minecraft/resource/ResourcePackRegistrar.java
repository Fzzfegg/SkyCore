package org.mybad.minecraft.resource;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.binary.BinaryPayloadCipherRegistry;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SideOnly(Side.CLIENT)
public final class ResourcePackRegistrar {
    private static final String[] DEFAULT_PACKS_FIELDS = {"defaultResourcePacks", "field_110449_ao"};
    private static boolean registered;

    private ResourcePackRegistrar() {}

    public static Path getPackRoot(File gameDir) {
        if (gameDir == null) {
            return null;
        }
        return gameDir.toPath().resolve("resourcepacks").resolve("SkyCore");
    }

    public static void registerConfigPack(Path packRoot, BinaryPayloadCipherRegistry cipherRegistry) {
        if (registered) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameDir == null) {
            return;
        }
        if (packRoot == null) {
            packRoot = mc.gameDir.toPath().resolve("resourcepacks").resolve("SkyCore");
        }
        try {
            Files.createDirectories(packRoot);
        } catch (Exception ignored) {
        }
        List<IResourcePack> packs = ReflectionHelper.getPrivateValue(Minecraft.class, mc, DEFAULT_PACKS_FIELDS);
        if (packs == null) {
            return;
        }
        ConfigResourcePack pack = new ConfigResourcePack(packRoot, cipherRegistry);
        packs.add(pack);
        mc.refreshResources();
        registered = true;
    }
}
