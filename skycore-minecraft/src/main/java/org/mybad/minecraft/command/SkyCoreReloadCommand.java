package org.mybad.minecraft.command;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.resource.ResourceCacheManager;
import org.mybad.minecraft.particle.runtime.BedrockParticleSystem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SideOnly(Side.CLIENT)
public class SkyCoreReloadCommand extends CommandBase {

    private static final List<String> ROOT_ALIASES = Collections.singletonList("skyreload");

    @Override
    public String getName() {
        return "skyreload";
    }

    @Override
    public List<String> getAliases() {
        return ROOT_ALIASES;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/skyreload <all|model|animation|particle|texture> [path]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(getUsage(sender));
        }
        String sub = args[0];
        if ("all".equalsIgnoreCase(sub)) {
            scheduleFullReload(sender);
            return;
        }
        if (args.length < 2) {
            throw new WrongUsageException(getUsage(sender));
        }
        ReloadTarget target = ReloadTarget.from(sub);
        if (target == null) {
            throw new WrongUsageException(getUsage(sender));
        }
        String path = args[1];
        scheduleTargetedReload(sender, target, path);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("all", "model", "animation", "particle", "texture"));
        }
        return Collections.emptyList();
    }

    private void scheduleFullReload(ICommandSender sender) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (SkyCoreMod.instance == null) {
                notifyFailure(sender, "SkyCore reload 失败：Mod 实例不存在。");
                return;
            }
            SkyCoreMod.instance.reload();
            notifySuccess(sender, "SkyCore 全局资源已重新加载。");
        });
    }

    private void scheduleTargetedReload(ICommandSender sender, ReloadTarget target, String rawPath) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            ResourceCacheManager cacheManager = SkyCoreMod.instance != null ? SkyCoreMod.instance.getResourceCacheManager() : null;
            if (cacheManager == null) {
                notifyFailure(sender, "SkyCore reload 失败：资源管理器不可用。");
                return;
            }
            boolean success = false;
            switch (target) {
                case MODEL:
                    cacheManager.invalidateModel(rawPath);
                    success = true;
                    break;
                case ANIMATION:
                    cacheManager.invalidateAnimation(rawPath);
                    success = true;
                    break;
                case PARTICLE:
                    cacheManager.invalidateParticle(rawPath);
                    success = true;
                    BedrockParticleSystem system = SkyCoreMod.getParticleSystem();
                    if (system != null) {
                        system.clear();
                    }
                    break;
                case TEXTURE:
                    ResourceLocation location = cacheManager.resolveResourceLocation(rawPath);
                    if (location != null) {
                        mc.getTextureManager().deleteTexture(location);
                        success = true;
                    }
                    break;
            }
            if (success) {
                notifySuccess(sender, "已清除缓存：" + target.display + " -> " + rawPath);
            } else {
                notifyFailure(sender, "无法清除缓存：" + rawPath);
            }
        });
    }

    public  static void notifySuccess(ICommandSender sender, String message) {
        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + message));
    }

    public static void notifyFailure(ICommandSender sender, String message) {
        sender.sendMessage(new TextComponentString(TextFormatting.RED + message));
    }

    private enum ReloadTarget {
        MODEL("模型"),
        ANIMATION("动画"),
        PARTICLE("粒子"),
        TEXTURE("纹理");

        final String display;

        ReloadTarget(String display) {
            this.display = display;
        }

        static ReloadTarget from(String value) {
            for (ReloadTarget target : values()) {
                if (target.name().equalsIgnoreCase(value)) {
                    return target;
                }
            }
            return null;
        }
    }
}
