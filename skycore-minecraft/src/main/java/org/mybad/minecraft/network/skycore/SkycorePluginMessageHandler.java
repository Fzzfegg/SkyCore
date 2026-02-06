package org.mybad.minecraft.network.skycore;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.minecraft.SkyCoreMod;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 监听 PluginMessage 并交给 SkycorePacketRouter 处理
 */
@SideOnly(Side.CLIENT)
public class SkycorePluginMessageHandler {

    private static final byte HEADER = 0x40;
    private static final String CHANNEL = "skycore:main";

    private final Map<Integer, ByteArrayOutputStream> pendingChunks = new HashMap<>();

    @SubscribeEvent
    public void onClientCustomPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        if (!(event.getPacket() instanceof FMLProxyPacket)) {
            return;
        }
        FMLProxyPacket proxy = (FMLProxyPacket) event.getPacket();
        if (!CHANNEL.equals(proxy.channel())) {
            return;
        }
        ByteBuf payload = proxy.payload();
        PacketBuffer buffer = new PacketBuffer(payload.copy());
        try {
            processBuffer(buffer);
        } finally {
            buffer.release();
        }
    }

    private void processBuffer(PacketBuffer buffer) {
        if (!buffer.isReadable()) {
            return;
        }
        byte header = buffer.readByte();
        if (header != HEADER) {
            return;
        }
        try {
            int packetId = buffer.readInt();
            int finishFlag = buffer.readInt();
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);

            ByteArrayOutputStream stream = pendingChunks.computeIfAbsent(packetId, key -> new ByteArrayOutputStream());
            stream.write(bytes);
            if (finishFlag == 1) {
                byte[] data = stream.toByteArray();
                pendingChunks.remove(packetId);
//                SkyCoreMod.LOGGER.info("[SkyCore] 接收到数据包 {}（{} 字节）", packetId, data.length);
                SkycorePacketRouter.handle(packetId, data);
            }
        } catch (IOException ex) {
            pendingChunks.clear();
//            SkyCoreMod.LOGGER.error("[SkyCore] 读取插件消息失败", ex);
        }
    }
}
