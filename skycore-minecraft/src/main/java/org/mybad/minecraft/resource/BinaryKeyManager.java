package org.mybad.minecraft.resource;

import org.mybad.core.binary.AesCtrBinaryCipher;
import org.mybad.core.binary.AesGcmBinaryCipher;
import org.mybad.core.binary.BinaryKeyDeriver;
import org.mybad.core.binary.BinaryPayloadCipher;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.event.EntityRenderEventHandler;
import org.mybad.minecraft.render.skull.SkullModelManager;
import org.mybad.skycoreproto.SkyCoreProto;

public final class BinaryKeyManager {
    private BinaryKeyManager() {}

    public static void applyBinaryKey(SkyCoreProto.BinaryKey message) {
        ResourceCacheManager cacheManager = SkyCoreMod.getResourceCacheManagerInstance();
        if (cacheManager == null) {
            return;
        }
        // Key updates must invalidate any binary-decoded caches; otherwise models can keep stale parse state.
        boolean changed = false;
        byte[] seed = message.getSeed().toByteArray();
        if (seed.length == 0) {
            cacheManager.installBinaryCipher(null);
            changed = true;
        } else {
            String algorithm = message.getAlgorithm().toLowerCase();
            int keySize = sanitizeKeySize(message.getKeySize());
            byte[] derived = BinaryKeyDeriver.derive(seed, keySize);
            BinaryPayloadCipher cipher;
            switch (algorithm) {
                case "aes-gcm":
                    cipher = new AesGcmBinaryCipher(derived);
                    break;
                case "aes-ctr":
                default:
                    cipher = new AesCtrBinaryCipher(derived);
                    break;
            }
            cacheManager.installBinaryCipher(cipher);
            changed = true;
            SkyCoreMod.LOGGER.info("[SkyCore] Binary resource key applied (algo={}, size={}).", algorithm, keySize);
        }

        if (changed) {
            cacheManager.clearCache();
            EntityRenderEventHandler handler = SkyCoreMod.getEntityRenderEventHandler();
            if (handler != null) {
                handler.clearCache();
            }
            SkullModelManager.clear();
        }
    }

    private static int sanitizeKeySize(int size) {
        switch (size) {
            case 24:
            case 32:
                return size;
            case 16:
            default:
                return 16;
        }
    }

}
