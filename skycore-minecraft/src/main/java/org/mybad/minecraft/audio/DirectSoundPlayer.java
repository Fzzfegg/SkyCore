package org.mybad.minecraft.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import paulscode.sound.SoundSystem;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Path;
import java.util.UUID;

public final class DirectSoundPlayer {
    private static final String[] SOUND_MANAGER_FIELDS = {"sndManager", "field_147694_f"};
    private static final String[] SOUND_SYSTEM_FIELDS = {"sndSystem", "field_148620_e"};
    private DirectSoundPlayer() {}

    public static void play(ResourceLocation soundId,
                            SoundCategory category,
                            float volume,
                            float pitch,
                            double x,
                            double y,
                            double z) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getSoundHandler() == null) {
            return;
        }
        SoundManager manager = getSoundManager(mc.getSoundHandler());
        if (manager == null) {
            return;
        }
        SoundSystem system = getSoundSystem(manager);
        if (system == null) {
            return;
        }
        if (soundId == null) {
            return;
        }
        if (!SoundExistenceCache.isReady()) {
            return;
        }
        if (!SoundExistenceCache.exists(soundId)) {
            SoundExistenceCache.warnMissing(soundId);
            return;
        }

        ResourceLocation fileLoc = toSoundFileLocation(soundId);
        URL url = getURLForFilePath(fileLoc);
        if (url == null) {
            return;
        }

        float clampedPitch = MathHelper.clamp(pitch, 0.5F, 2.0F);
        float clampedVolume = MathHelper.clamp(volume * getCategoryVolume(mc.gameSettings, category), 0.0F, 1.0F);
        if (clampedVolume <= 0.0F) {
            return;
        }
        float range = 16.0F;
        if (clampedVolume > 1.0F) {
            range *= clampedVolume;
        }
        int attenuation = (x == 0.0 && y == 0.0 && z == 0.0)
            ? ISound.AttenuationType.NONE.getTypeInt()
            : ISound.AttenuationType.LINEAR.getTypeInt();

        String channel = UUID.randomUUID().toString();
        system.newSource(false, channel, url, fileLoc.toString(), false, (float) x, (float) y, (float) z, attenuation, range);
        system.setPitch(channel, clampedPitch);
        system.setVolume(channel, clampedVolume);
        system.play(channel);
    }


    private static SoundManager getSoundManager(SoundHandler handler) {
        try {
            return ReflectionHelper.getPrivateValue(SoundHandler.class, handler, SOUND_MANAGER_FIELDS);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static SoundSystem getSoundSystem(SoundManager manager) {
        try {
            return ReflectionHelper.getPrivateValue(SoundManager.class, manager, SOUND_SYSTEM_FIELDS);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static float getCategoryVolume(GameSettings settings, SoundCategory category) {
        if (settings == null) {
            return 1.0F;
        }
        if (category != null && category != SoundCategory.MASTER) {
            return settings.getSoundLevel(category);
        }
        return 1.0F;
    }

    private static ResourceLocation toSoundFileLocation(ResourceLocation soundId) {
        String path = soundId.getPath();
        if (path.endsWith(".ogg")) {
            path = path.substring(0, path.length() - 4);
        }
        if (!path.startsWith("sounds/")) {
            path = "sounds/" + path;
        }
        path = path + ".ogg";
        return new ResourceLocation(soundId.getNamespace(), path);
    }

    private static URL getURLForFilePath(ResourceLocation location) {
        if (location == null) {
            return null;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameDir == null) {
            return null;
        }
        Path file = mc.gameDir.toPath()
            .resolve("resourcepacks")
            .resolve("SkyCore")
            .resolve(location.getNamespace())
            .resolve(location.getPath().replace('/', java.io.File.separatorChar));
        if (!java.nio.file.Files.isRegularFile(file)) {
            return null;
        }
        try {
            return file.toUri().toURL();
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
