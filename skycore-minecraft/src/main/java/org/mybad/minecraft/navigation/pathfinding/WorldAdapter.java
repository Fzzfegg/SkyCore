package org.mybad.minecraft.navigation.pathfinding;

public interface WorldAdapter {
    boolean isWalkable(int x, int y, int z);

    Iterable<PathNode> getNeighbors(PathNode node);

    boolean isLineClear(double startX, double startY, double startZ,
                        double endX, double endY, double endZ,
                        double width);
}
