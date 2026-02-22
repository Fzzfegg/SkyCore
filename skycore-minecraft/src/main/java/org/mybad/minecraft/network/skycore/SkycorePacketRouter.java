package org.mybad.minecraft.network.skycore;

import com.google.protobuf.InvalidProtocolBufferException;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.debug.DebugRenderController;
import org.mybad.minecraft.network.skycore.config.RemoteConfigController;
import org.mybad.minecraft.network.skycore.runtime.RealtimeCommandExecutor;
import org.mybad.minecraft.resource.preload.PreloadManager;
import org.mybad.minecraft.resource.BinaryKeyManager;
import org.mybad.skycoreproto.SkyCoreProto;

import java.nio.charset.StandardCharsets;

public final class SkycorePacketRouter {
    private SkycorePacketRouter() {}

    public static void handle(int packetId, byte[] payload) {
        try {
            switch (packetId) {
                case SkycorePacketId.HELLO:
                    String reason = new String(payload, StandardCharsets.UTF_8);
//                    SkyCoreMod.LOGGER.info("[SkyCore] 服务器请求重新握手（原因：{}）", reason);
                    SkycoreClientHandshake.requestHelloFromServer("server");
                    return;
                case SkycorePacketId.CONFIG_INDEX:
//                    SkyCoreMod.LOGGER.info("[SkyCore] 收到配置索引。");
                    RemoteConfigController.getInstance().handleConfigIndex(SkyCoreProto.ConfigIndex.parseFrom(payload));
                    return;
                case SkycorePacketId.CONFIG_FILE:
//                    SkyCoreMod.LOGGER.info("[SkyCore] 收到配置文件内容。");
                    RemoteConfigController.getInstance().handleMappingFile(SkyCoreProto.MappingFile.parseFrom(payload));
                    return;
                case SkycorePacketId.CONFIG_FILE_REMOVED:
//                    SkyCoreMod.LOGGER.info("[SkyCore] 收到配置删除通知。");
                    RemoteConfigController.getInstance().handleFileRemoved(SkyCoreProto.ConfigFileRemoved.parseFrom(payload));
                    return;
                case SkycorePacketId.FORCE_ANIMATION:
//                    SkyCoreMod.LOGGER.info("[SkyCore] 收到强制动画指令。");
                    RealtimeCommandExecutor.handleForceAnimation(SkyCoreProto.ForceAnimation.parseFrom(payload));
                    return;
                case SkycorePacketId.FORCE_SKULL_ANIMATION:
                    RealtimeCommandExecutor.handleForceSkullAnimation(SkyCoreProto.ForceSkullAnimation.parseFrom(payload));
                    return;
                case SkycorePacketId.SET_MODEL_ATTRIBUTES:
//                    SkyCoreMod.LOGGER.info("[SkyCore] 收到模型属性更新。");
                    RealtimeCommandExecutor.handleSetModelAttributes(SkyCoreProto.SetModelAttributes.parseFrom(payload));
                    return;
                case SkycorePacketId.SPAWN_PARTICLE:
//                    SkyCoreMod.LOGGER.info("[SkyCore] 收到粒子生成指令。");
                    RealtimeCommandExecutor.handleSpawnParticle(SkyCoreProto.SpawnParticle.parseFrom(payload));
                    return;
                case SkycorePacketId.PRELOAD_HINT:
//                    SkyCoreMod.LOGGER.info("[SkyCore] 收到预热指令。");
                    PreloadManager manager = SkyCoreMod.getPreloadManager();
                    if (manager != null) {
                        manager.enqueue(SkyCoreProto.PreloadHint.parseFrom(payload));
                    }
                    return;
                case SkycorePacketId.DEBUG_MESSAGE:
                    String text = new String(payload, java.nio.charset.StandardCharsets.UTF_8);
//                    SkyCoreMod.LOGGER.info("[SkyCore] 调试消息：{}", text);
                    return;
                case SkycorePacketId.DEBUG_FLAGS:
//                    SkyCoreMod.LOGGER.info("[SkyCore] 收到调试渲染开关。");
                    DebugRenderController.apply(SkyCoreProto.DebugRenderFlags.parseFrom(payload));
                    return;
                case SkycorePacketId.BINARY_KEY:
//                    SkyCoreMod.LOGGER.info("[SkyCore] 收到资源包密钥。");
                    BinaryKeyManager.applyBinaryKey(SkyCoreProto.BinaryKey.parseFrom(payload));
                    return;
                case SkycorePacketId.ENTITY_ATTACHMENT:
                    RealtimeCommandExecutor.handleEntityAttachment(SkyCoreProto.EntityAttachment.parseFrom(payload));
                    return;
                case SkycorePacketId.REMOVE_ENTITY_ATTACHMENT:
                    RealtimeCommandExecutor.handleRemoveAttachment(SkyCoreProto.RemoveEntityAttachment.parseFrom(payload));
                    return;
                case SkycorePacketId.INDICATOR_COMMAND:
//                    SkyCoreMod.LOGGER.info("[SkyCore] 收到地面指示器指令。");
                    RealtimeCommandExecutor.handleIndicatorCommand(SkyCoreProto.IndicatorCommand.parseFrom(payload));
                    return;
                case SkycorePacketId.WORLD_ACTOR_COMMAND:
//                    SkyCoreMod.LOGGER.info("[SkyCore] 收到世界实体指令。");
                    RealtimeCommandExecutor.handleWorldActorCommand(SkyCoreProto.WorldActorCommand.parseFrom(payload));
                    return;
                case SkycorePacketId.GLTF_PROFILE:
                    org.mybad.minecraft.gltf.client.network.RemoteProfileRegistry.handleProfileDefinition(
                        SkyCoreProto.GltfProfile.parseFrom(payload));
                    return;
                case SkycorePacketId.GLTF_ASSIGN:
                    org.mybad.minecraft.gltf.client.network.RemoteProfileRegistry.handleProfileAssignment(
                        SkyCoreProto.GltfProfileAssignment.parseFrom(payload));
                    return;
                default:
                    SkyCoreMod.LOGGER.warn("[SkyCore] 收到未知数据包：{}", packetId);
            }
        } catch (InvalidProtocolBufferException ex) {
//            SkyCoreMod.LOGGER.error("[SkyCore] 解析数据包 {} 失败", packetId, ex);
        }
    }
}
