package org.mybad.minecraft.navigation.pathfinding;

@FunctionalInterface
public interface Heuristic {
    double estimate(PathNode current, PathNode goal);
}
