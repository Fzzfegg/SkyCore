package org.mybad.minecraft.network.skycore;

import com.google.protobuf.InvalidProtocolBufferException;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.network.skycore.config.RemoteConfigController;
import org.mybad.minecraft.network.skycore.runtime.RealtimeCommandExecutor;
import org.mybad.skycoreproto.SkyCoreProto;

import java.nio.charset.StandardCharsets;

public final class SkycorePacketRouter {
    private SkycorePacketRouter() {}

    public static void handle(int packetId, byte[] payload) {
        try {
            switch (packetId) {
                case SkycorePacketId.HELLO:
                    String reason = new String(payload, StandardCharsets.UTF_8);
                    SkyCoreMod.LOGGER.info("[SkyCore] Server requested HELLO (reason={})", reason);
                    SkycoreClientHandshake.requestHelloFromServer("server");
                    return;
                case SkycorePacketId.CONFIG_INDEX:
                    SkyCoreMod.LOGGER.info("[SkyCore] Received CONFIG_INDEX");
                    RemoteConfigController.getInstance().handleConfigIndex(SkyCoreProto.ConfigIndex.parseFrom(payload));
                    return;
                case SkycorePacketId.CONFIG_FILE:
                    SkyCoreMod.LOGGER.info("[SkyCore] Received CONFIG_FILE");
                    RemoteConfigController.getInstance().handleMappingFile(SkyCoreProto.MappingFile.parseFrom(payload));
                    return;
                case SkycorePacketId.CONFIG_FILE_REMOVED:
                    SkyCoreMod.LOGGER.info("[SkyCore] Received CONFIG_FILE_REMOVED");
                    RemoteConfigController.getInstance().handleFileRemoved(SkyCoreProto.ConfigFileRemoved.parseFrom(payload));
                    return;
                case SkycorePacketId.FORCE_ANIMATION:
                    SkyCoreMod.LOGGER.info("[SkyCore] Received FORCE_ANIMATION");
                    RealtimeCommandExecutor.handleForceAnimation(SkyCoreProto.ForceAnimation.parseFrom(payload));
                    return;
                case SkycorePacketId.CLEAR_FORCE_ANIMATION:
                    SkyCoreMod.LOGGER.info("[SkyCore] Received CLEAR_FORCE_ANIMATION");
                    RealtimeCommandExecutor.handleClearAnimation(SkyCoreProto.ClearForceAnimation.parseFrom(payload));
                    return;
                case SkycorePacketId.SET_MODEL_ATTRIBUTES:
                    SkyCoreMod.LOGGER.info("[SkyCore] Received SET_MODEL_ATTRIBUTES");
                    RealtimeCommandExecutor.handleSetModelAttributes(SkyCoreProto.SetModelAttributes.parseFrom(payload));
                    return;
                case SkycorePacketId.SPAWN_PARTICLE:
                    SkyCoreMod.LOGGER.info("[SkyCore] Received SPAWN_PARTICLE");
                    RealtimeCommandExecutor.handleSpawnParticle(SkyCoreProto.SpawnParticle.parseFrom(payload));
                    return;
                case SkycorePacketId.CLEAR_PARTICLES:
                    SkyCoreMod.LOGGER.info("[SkyCore] Received CLEAR_PARTICLES");
                    RealtimeCommandExecutor.handleClearParticles(SkyCoreProto.ClearParticles.parseFrom(payload));
                    return;
                case SkycorePacketId.DEBUG_MESSAGE:
                    String text = new String(payload, java.nio.charset.StandardCharsets.UTF_8);
                    SkyCoreMod.LOGGER.info("[SkyCore] Debug message: {}", text);
                    return;
                default:
                    SkyCoreMod.LOGGER.warn("[SkyCore] Unknown packet id {}", packetId);
            }
        } catch (InvalidProtocolBufferException ex) {
            SkyCoreMod.LOGGER.error("[SkyCore] Failed to decode packet {}", packetId, ex);
        }
    }
}
