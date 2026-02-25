package org.mybad.minecraft.navigation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.minecraft.navigation.pathfinding.AStarPathfinder;
import org.mybad.minecraft.navigation.pathfinding.ClientWorldAdapter;
import org.mybad.minecraft.navigation.pathfinding.CostFunction;
import org.mybad.minecraft.navigation.pathfinding.CostStack;
import org.mybad.minecraft.navigation.pathfinding.CustomPersonalityProfile;
import org.mybad.minecraft.navigation.pathfinding.Heuristic;
import org.mybad.minecraft.navigation.pathfinding.PathNode;
import org.mybad.minecraft.navigation.pathfinding.PersonalityProfile;

import java.util.Collections;
import java.util.List;

@SideOnly(Side.CLIENT)
public final class PlayerAutoNavigator {

    private static final PlayerAutoNavigator INSTANCE = new PlayerAutoNavigator();

    public static PlayerAutoNavigator getInstance() {
        return INSTANCE;
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private final AStarPathfinder pathfinder = new AStarPathfinder();
    private final Heuristic heuristic = (a, b) -> Math.sqrt(
            Math.pow(a.x - b.x, 2) +
                    Math.pow(a.y - b.y, 2) +
                    Math.pow(a.z - b.z, 2)
    );
    private final CostStack costStack = new CostStack();
    private final PersonalityProfile profile = new PersonalityProfile();
    private final CustomPersonalityProfile customProfile = new CustomPersonalityProfile();

    private List<PathNode> currentPath = Collections.emptyList();
    private int currentIndex = 0;
    private Vec3d currentTarget;
    private boolean navigating = false;
    private boolean autoJumpOverridden = false;
    private boolean previousAutoJump = false;

    private PlayerAutoNavigator() {
        costStack.add(distanceCost());
        customProfile.addWeight(0.0);
        customProfile.addWeight(0.0);
        customProfile.addWeight(0.0);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public boolean navigateTo(Vec3d target) {
        if (target == null) {
            return false;
        }
        EntityPlayerSP player = mc.player;
        ClientWorldAdapter adapter = ClientWorldAdapter.current();
        if (player == null || adapter == null) {
            return false;
        }
        PathNode start = toNode(player.getPositionVector());
        PathNode goal = toNode(target);
        List<PathNode> path = pathfinder.findPath(start, goal, adapter, heuristic, costStack, profile, customProfile, false);
        if (path.isEmpty()) {
            return false;
        }
        this.currentPath = path;
        this.currentIndex = 0;
        this.currentTarget = target;
        this.navigating = true;
        applyAutoJumpOverride();
        notifyPlayer("自动行走：已规划路径，共 " + path.size() + " 个节点。");
        return true;
    }

    public void cancelNavigation(String reason) {
        if (!navigating) {
            return;
        }
        stopInternal(false);
        if (reason != null && !reason.isEmpty()) {
            notifyPlayer(reason);
        }
    }

    public boolean isNavigating() {
        return navigating;
    }

    private PathNode toNode(Vec3d vec) {
        return new PathNode(MathHelper.floor(vec.x), MathHelper.floor(vec.y), MathHelper.floor(vec.z));
    }

    @SubscribeEvent
    public void onInputUpdate(InputUpdateEvent event) {
        if (!navigating) {
            return;
        }
        GameSettings settings = mc.gameSettings;
        boolean manual =
                settings.keyBindForward.isKeyDown() ||
                        settings.keyBindBack.isKeyDown() ||
                        settings.keyBindLeft.isKeyDown() ||
                        settings.keyBindRight.isKeyDown();
        if (manual) {
            cancelNavigation("检测到手动移动，自动行走已取消。");
            return;
        }
        if (!autoJumpOverridden) {
            applyAutoJumpOverride();
        }
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null) {
            stopInternal(false);
            return;
        }
        if (currentIndex >= currentPath.size()) {
            stopInternal(true);
            return;
        }
        Vec3d playerPos = player.getPositionVector();
        PathNode targetNode = currentPath.get(currentIndex);
        Vec3d targetPos = nodeCenter(targetNode);
        double distance = targetPos.distanceTo(playerPos);
        double reachDistance = computeReachDistance(player);
        boolean advance = distance < reachDistance;
        if (!advance && currentIndex > 0) {
            Vec3d previousPos = nodeCenter(currentPath.get(currentIndex - 1));
            Vec3d pathDir = targetPos.subtract(previousPos);
            Vec3d playerAhead = playerPos.subtract(targetPos);
            if (pathDir.lengthSquared() > 1.0E-4 && playerAhead.lengthSquared() > 1.0E-4) {
                advance = pathDir.dotProduct(playerAhead) > 0.0;
            }
        }
        if (advance) {
            currentIndex++;
            if (currentIndex >= currentPath.size()) {
                stopInternal(true);
                return;
            }
            targetNode = currentPath.get(currentIndex);
            targetPos = nodeCenter(targetNode);
        }

        double angle = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(
                targetPos.z - playerPos.z,
                targetPos.x - playerPos.x)) - 90.0);
        player.rotationYaw = (float) angle;
        player.rotationYawHead = (float) angle;
        player.rotationPitch = (float) MathHelper.clamp(targetPos.y - playerPos.y, -45.0, 45.0);

        event.getMovementInput().moveForward = 1.0f;
        event.getMovementInput().moveStrafe = 0.0f;
        event.getMovementInput().jump = false;
        double dy = targetPos.y - playerPos.y;
        if (dy > 0.6 && player.onGround && !player.isRiding()) {
            player.jump();
        }
        player.moveForward = event.getMovementInput().moveForward;
        player.moveStrafing = event.getMovementInput().moveStrafe;
        player.movementInput.jump = false;
        if (!player.isSprinting()) {
            player.setSprinting(true);
        }
    }

    private void stopInternal(boolean reached) {
        navigating = false;
        currentPath = Collections.emptyList();
        currentIndex = 0;
        currentTarget = null;
        EntityPlayerSP player = mc.player;
        if (player != null && player.movementInput != null) {
            player.movementInput.moveForward = 0.0f;
            player.movementInput.moveStrafe = 0.0f;
            player.moveForward = 0.0f;
            player.moveStrafing = 0.0f;
            player.setSprinting(false);
        }
        restoreAutoJumpOverride();
        if (reached) {
            notifyPlayer("自动行走：已到达目标点。");
        }
    }

    private void notifyPlayer(String message) {
        EntityPlayerSP player = mc.player;
        if (player != null && message != null && !message.isEmpty()) {
            player.sendMessage(new TextComponentString("[SkyCore] " + message));
        }
    }

    private CostFunction distanceCost() {
        return (a, b) -> {
            int dx = a.x - b.x;
            int dy = a.y - b.y;
            int dz = a.z - b.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        };
    }

    private Vec3d nodeCenter(PathNode node) {
        return new Vec3d(node.x + 0.5, node.y, node.z + 0.5);
    }

    private double computeReachDistance(EntityPlayerSP player) {
        if (player == null) {
            return 0.4;
        }
        if (!player.isRiding()) {
            return 0.4;
        }
        Entity mount = player.getRidingEntity();
        if (mount == null) {
            return 0.4;
        }
        double width = Math.max(mount.width, 0.6F);
        double reach = 0.4 + width * 0.6;
        return MathHelper.clamp(reach, 0.5, 1.6);
    }

    private void applyAutoJumpOverride() {
        GameSettings settings = mc.gameSettings;
        if (settings != null && !autoJumpOverridden) {
            previousAutoJump = settings.autoJump;
            settings.autoJump = false;
            settings.saveOptions();
            autoJumpOverridden = true;
        }
    }

    private void restoreAutoJumpOverride() {
        GameSettings settings = mc.gameSettings;
        if (settings != null && autoJumpOverridden) {
            settings.autoJump = previousAutoJump;
            settings.saveOptions();
            autoJumpOverridden = false;
        }
    }
}
