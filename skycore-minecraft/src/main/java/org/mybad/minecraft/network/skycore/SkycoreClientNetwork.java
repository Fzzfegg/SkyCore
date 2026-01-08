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
        if (minecraft == null || minecraft.getConnection() == null) {
            SkyCoreMod.LOGGER.warn("[SkyCore] Cannot send HELLO, connection unavailable.");
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
            SkyCoreMod.LOGGER.info("[SkyCore] Sent HELLO to server (reason={})", reason);
        } catch (Exception ex) {
            SkyCoreMod.LOGGER.error("[SkyCore] Failed to send HELLO", ex);
        }
    }

    private static byte[] buildHelloPayload(String reason) {
        String body = SkyCoreMod.VERSION + "|" + (reason == null ? "" : reason);
        return body.getBytes(StandardCharsets.UTF_8);
    }
}
