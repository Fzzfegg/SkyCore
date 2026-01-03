package org.mybad.minecraft.resource;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import org.mybad.minecraft.SkyCoreMod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Resolves resource paths and loads raw content from Minecraft's resource manager.
 */
final class ResourcePathResolver {

    ResourceLocation resolveResourceLocation(String path) {
        int colonIndex = path.indexOf(':');
        if (colonIndex > 0) {
            String namespace = path.substring(0, colonIndex);
            String resourcePath = path.substring(colonIndex + 1);
            return new ResourceLocation(namespace, resourcePath);
        }
        return new ResourceLocation(SkyCoreMod.MOD_ID, path);
    }

    String readResourceAsString(String path) {
        ResourceLocation location = resolveResourceLocation(path);
        try {
            IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
            try (InputStream is = resource.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch (IOException e) {
            return null;
        }
    }
}
