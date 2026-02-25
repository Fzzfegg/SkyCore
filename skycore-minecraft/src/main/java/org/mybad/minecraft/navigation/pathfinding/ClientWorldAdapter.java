package org.mybad.minecraft.navigation.pathfinding;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Thin wrapper around the local client world so the pathfinder can query walkability.
 */
public final class ClientWorldAdapter implements WorldAdapter {
    private final World world;

    public ClientWorldAdapter(World world) {
        this.world = world;
    }

    public static ClientWorldAdapter current() {
        World world = Minecraft.getMinecraft().world;
        return world == null ? null : new ClientWorldAdapter(world);
    }

    @Override
    public boolean isWalkable(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockPos head = pos.up();
        BlockPos feet = pos.down();

        return isPassable(pos) && isPassable(head) && hasSolidGround(feet);
    }

    @Override
    public Iterable<PathNode> getNeighbors(PathNode node) {
        List<PathNode> neighbors = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                int nx = node.x + dx;
                int nz = node.z + dz;
                for (int dy = 1; dy >= -3; dy--) {
                    int ny = node.y + dy;
                    if (isWalkable(nx, ny, nz)) {
                        neighbors.add(new PathNode(nx, ny, nz));
                        break;
                    }
                }
            }
        }
        return neighbors;
    }

    @Override
    public boolean isLineClear(double startX, double startY, double startZ,
                               double endX, double endY, double endZ,
                               double width) {
        if (!rayClear(startX, startY, startZ, endX, endY, endZ)) {
            return false;
        }
        double dx = endX - startX;
        double dz = endZ - startZ;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-4) {
            return true;
        }
        double nx = -dz / len * (width / 2.0);
        double nz = dx / len * (width / 2.0);
        return rayClear(startX + nx, startY, startZ + nz, endX + nx, endY, endZ + nz)
                && rayClear(startX - nx, startY, startZ - nz, endX - nx, endY, endZ - nz);
    }

    private boolean rayClear(double sx, double sy, double sz, double ex, double ey, double ez) {
        Vec3d start = new Vec3d(sx, sy, sz);
        Vec3d end = new Vec3d(ex, ey, ez);
        RayTraceResult result = world.rayTraceBlocks(start, end, true, true, false);
        return result == null || result.typeOfHit == RayTraceResult.Type.MISS;
    }

    private boolean isPassable(BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getCollisionBoundingBox(world, pos) == Block.NULL_AABB
                || state.getMaterial().isReplaceable();
    }

    private boolean hasSolidGround(BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getMaterial().isSolid() && !state.getBlock().isLeaves(state, world, pos);
    }
}
