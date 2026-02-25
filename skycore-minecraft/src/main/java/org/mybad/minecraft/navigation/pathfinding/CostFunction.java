package org.mybad.minecraft.navigation.pathfinding;

@FunctionalInterface
public interface CostFunction {
    double cost(PathNode from, PathNode to);
}
