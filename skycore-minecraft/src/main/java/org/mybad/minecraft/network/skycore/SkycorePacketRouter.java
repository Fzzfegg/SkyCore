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
                    SkyCoreMod.LOGGER.info("[SkyCore] 服务器请求重新握手（原因：{}）", reason);
                    SkycoreClientHandshake.requestHelloFromServer("server");
                    return;
                case SkycorePacketId.RENDER_SETTINGS:
                    RemoteConfigController.getInstance().handleRenderSettings(SkyCoreProto.RenderSettings.parseFrom(payload));
                    return;
                case SkycorePacketId.CONFIG_INDEX:
                    SkyCoreMod.LOGGER.info("[SkyCore] 收到配置索引。");
                    RemoteConfigController.getInstance().handleConfigIndex(SkyCoreProto.ConfigIndex.parseFrom(payload));
                    return;
                case SkycorePacketId.CONFIG_FILE:
                    SkyCoreMod.LOGGER.info("[SkyCore] 收到配置文件内容。");
                    RemoteConfigController.getInstance().handleMappingFile(SkyCoreProto.MappingFile.parseFrom(payload));
                    return;
                case SkycorePacketId.CONFIG_FILE_REMOVED:
                    SkyCoreMod.LOGGER.info("[SkyCore] 收到配置删除通知。");
                    RemoteConfigController.getInstance().handleFileRemoved(SkyCoreProto.ConfigFileRemoved.parseFrom(payload));
                    return;
                case SkycorePacketId.FORCE_ANIMATION:
                    SkyCoreMod.LOGGER.info("[SkyCore] 收到强制动画指令。");
                    RealtimeCommandExecutor.handleForceAnimation(SkyCoreProto.ForceAnimation.parseFrom(payload));
                    return;
                case SkycorePacketId.CLEAR_FORCE_ANIMATION:
                    SkyCoreMod.LOGGER.info("[SkyCore] 收到清除动画指令。");
                    RealtimeCommandExecutor.handleClearAnimation(SkyCoreProto.ClearForceAnimation.parseFrom(payload));
                    return;
                case SkycorePacketId.SET_MODEL_ATTRIBUTES:
                    SkyCoreMod.LOGGER.info("[SkyCore] 收到模型属性更新。");
                    RealtimeCommandExecutor.handleSetModelAttributes(SkyCoreProto.SetModelAttributes.parseFrom(payload));
                    return;
                case SkycorePacketId.SPAWN_PARTICLE:
                    SkyCoreMod.LOGGER.info("[SkyCore] 收到粒子生成指令。");
                    RealtimeCommandExecutor.handleSpawnParticle(SkyCoreProto.SpawnParticle.parseFrom(payload));
                    return;
                case SkycorePacketId.CLEAR_PARTICLES:
                    SkyCoreMod.LOGGER.info("[SkyCore] 收到粒子清理指令。");
                    RealtimeCommandExecutor.handleClearParticles(SkyCoreProto.ClearParticles.parseFrom(payload));
                    return;
                case SkycorePacketId.DEBUG_MESSAGE:
                    String text = new String(payload, java.nio.charset.StandardCharsets.UTF_8);
                    SkyCoreMod.LOGGER.info("[SkyCore] 调试消息：{}", text);
                    return;
                default:
                    SkyCoreMod.LOGGER.warn("[SkyCore] 收到未知数据包：{}", packetId);
            }
        } catch (InvalidProtocolBufferException ex) {
            SkyCoreMod.LOGGER.error("[SkyCore] 解析数据包 {} 失败", packetId, ex);
        }
    }
}
