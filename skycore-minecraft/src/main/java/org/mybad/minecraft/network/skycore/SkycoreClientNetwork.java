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
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.getConnection() == null) {
            SkyCoreMod.LOGGER.warn("[SkyCore] 无法发送握手数据：连接尚未建立。");
            return;
        }
        byte[] payload = buildHelloPayload(reason);
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer(9 + payload.length));
        buffer.writeByte(HEADER);
        buffer.writeInt(SkycorePacketId.HELLO);
        buffer.writeInt(1); // finish flag
        buffer.writeBytes(payload);
        try {
            minecraft.getConnection().sendPacket(new CPacketCustomPayload(CHANNEL, buffer));
            SkyCoreMod.LOGGER.info("[SkyCore] 已向服务器发送握手数据（原因：{}）。", reason);
        } catch (Exception ex) {
            SkyCoreMod.LOGGER.error("[SkyCore] 发送握手数据失败", ex);
        }
    }

    private static byte[] buildHelloPayload(String reason) {
        String body = SkyCoreMod.VERSION + "|" + (reason == null ? "" : reason);
        return body.getBytes(StandardCharsets.UTF_8);
    }
}
