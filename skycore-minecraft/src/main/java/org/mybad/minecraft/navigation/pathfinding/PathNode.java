package org.mybad.minecraft.navigation.pathfinding;

import java.util.Objects;

/**
 * Simple 3D node used by the pathfinding algorithms.
 */
public final class PathNode {
    public final int x;
    public final int y;
    public final int z;

    public PathNode(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PathNode)) {
            return false;
        }
        PathNode node = (PathNode) o;
        return x == node.x && y == node.y && z == node.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
