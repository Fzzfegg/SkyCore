package org.mybad.minecraft.network.skycore;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.minecraft.SkyCoreMod;

/**
 * 客户端握手：登录后自动发送 HELLO，并响应服务器的重发请求
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = SkyCoreMod.MOD_ID, value = Side.CLIENT)
public final class SkycoreClientHandshake {

    private static boolean autoHelloPending;
    private static int ticksUntilHello;

    private SkycoreClientHandshake() {}

    @SubscribeEvent
    public static void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        autoHelloPending = true;
        ticksUntilHello = 20; // 1s
        org.mybad.minecraft.resource.BinaryKeyManager.markKeyPending();
    }

    @SubscribeEvent
    public static void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        autoHelloPending = false;
        org.mybad.minecraft.resource.ResourceCacheManager manager = org.mybad.minecraft.SkyCoreMod.getResourceCacheManagerInstance();
        if (manager != null) {
            manager.installBinaryCipher(null);
            manager.clearCache();
        }
        org.mybad.minecraft.resource.BinaryKeyManager.markKeyPending();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!autoHelloPending || event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.player == null || minecraft.getConnection() == null) {
            return;
        }
        if (ticksUntilHello > 0) {
            ticksUntilHello--;
            return;
        }
        SkycoreClientNetwork.sendHello("auto");
        autoHelloPending = false;
    }

    public static void requestHelloFromServer(String reason) {
        SkycoreClientNetwork.sendHello(reason);
    }
}
