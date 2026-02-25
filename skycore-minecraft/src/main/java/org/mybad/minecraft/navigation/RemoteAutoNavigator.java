package org.mybad.minecraft.navigation;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.skycoreproto.SkyCoreProto;

@SideOnly(Side.CLIENT)
public final class RemoteAutoNavigator {
    private RemoteAutoNavigator() {}

    public static void handleCommand(SkyCoreProto.AutoNavigateCommand command) {
        if (command == null) {
            return;
        }
        PlayerAutoNavigator navigator = PlayerAutoNavigator.getInstance();
        switch (command.getAction()) {
            case START:
                Vec3d target = new Vec3d(command.getX(), command.getY(), command.getZ());
                boolean ok = navigator.navigateTo(target);
                if (!ok && net.minecraft.client.Minecraft.getMinecraft().player != null) {
                    net.minecraft.client.Minecraft.getMinecraft().player.sendMessage(
                        new TextComponentString("[SkyCore] 无法规划到指定坐标。"));
                }
                return;
            case STOP:
                navigator.cancelNavigation("服务器请求停止自动行走。");
                return;
            default:
                SkyCoreMod.LOGGER.warn("[SkyCore] 收到未知自动行走指令：{}", command.getAction());
        }
    }
}
