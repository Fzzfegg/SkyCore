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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
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
        if (!SoundExistenceCache.canPlay(soundId)) {
            return;
        }

        ResourceLocation fileLoc = toSoundFileLocation(soundId);
        URL url = getURLForSoundResource(fileLoc);
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

    private static URL getURLForSoundResource(final ResourceLocation location) {
        String spec = String.format("%s:%s:%s", "mcsounddomain", location.getNamespace(), location.getPath());
        URLStreamHandler handler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) throws IOException {
                return new URLConnection(url) {
                    @Override
                    public void connect() throws IOException {
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        return Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream();
                    }
                };
            }
        };
        try {
            return new URL(null, spec, handler);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    // resource existence is handled by SoundExistenceCache
}
