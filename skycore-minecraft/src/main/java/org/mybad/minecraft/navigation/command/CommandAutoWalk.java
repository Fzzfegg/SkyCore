package org.mybad.minecraft.navigation.command;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.minecraft.navigation.PlayerAutoNavigator;

@SideOnly(Side.CLIENT)
public class CommandAutoWalk extends CommandBase {

    @Override
    public String getName() {
        return "skywalk";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/skywalk <x> <y> <z>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 3) {
            sender.sendMessage(new TextComponentString("用法: " + getUsage(sender)));
            return;
        }
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }
        try {
            double x = Double.parseDouble(args[0]);
            double y = Double.parseDouble(args[1]);
            double z = Double.parseDouble(args[2]);
            boolean ok = PlayerAutoNavigator.getInstance().navigateTo(new Vec3d(x, y, z));
            if (!ok) {
                sender.sendMessage(new TextComponentString("无法规划到指定坐标。"));
            }
        } catch (NumberFormatException ex) {
            sender.sendMessage(new TextComponentString("请输入合法的坐标，例如 /skywalk 100 65 -200"));
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
