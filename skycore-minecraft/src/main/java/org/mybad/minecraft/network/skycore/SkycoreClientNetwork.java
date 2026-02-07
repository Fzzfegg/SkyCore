package org.mybad.minecraft.network.skycore;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import org.mybad.minecraft.SkyCoreMod;

import java.nio.charset.StandardCharsets;

/**
 * 客户端主动/被动发送 PluginMessage（HELLO 握手等）
 */
public final class SkycoreClientNetwork {
    private static final String CHANNEL = "skycore:main";
    private static final byte HEADER = 0x40;

    private SkycoreClientNetwork() {}

    public static void sendHello(String reason) {
        sendPacket(SkycorePacketId.HELLO, buildHelloPayload(reason), "握手数据");
    }

    public static void sendBinaryKeyAck(String detail) {
        String text = detail == null ? "ok" : detail.trim();
        if (text.isEmpty()) {
            text = "ok";
        }
        sendPacket(SkycorePacketId.BINARY_KEY_ACK, text.getBytes(StandardCharsets.UTF_8), "密钥确认");
    }

    private static byte[] buildHelloPayload(String reason) {
        String body = SkyCoreMod.VERSION + "|" + (reason == null ? "" : reason);
        return body.getBytes(StandardCharsets.UTF_8);
    }

    private static void sendPacket(int packetId, byte[] payload, String label) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.getConnection() == null) {
            SkyCoreMod.LOGGER.warn("[SkyCore] 无法发送{}：连接尚未建立。", label);
            return;
        }
        byte[] body = payload == null ? new byte[0] : payload;
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer(9 + body.length));
        buffer.writeByte(HEADER);
        buffer.writeInt(packetId);
        buffer.writeInt(1); // finish flag
        buffer.writeBytes(body);
        try {
            minecraft.getConnection().sendPacket(new CPacketCustomPayload(CHANNEL, buffer));
        } catch (Exception ex) {
            SkyCoreMod.LOGGER.error("[SkyCore] 发送{}失败", label, ex);
        }
    }
}
