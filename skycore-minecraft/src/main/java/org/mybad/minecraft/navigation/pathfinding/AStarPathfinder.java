package org.mybad.minecraft.navigation.pathfinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Lightweight A* implementation based on the original PathFindering code.
 */
public final class AStarPathfinder {
    public List<PathNode> findPath(
            PathNode start,
            PathNode goal,
            WorldAdapter world,
            Heuristic heuristic,
            CostStack costs,
            PersonalityProfile profile,
            CustomPersonalityProfile customProfile,
            boolean useCustomProfile
    ) {
        if (start == null || goal == null || world == null || heuristic == null) {
            return Collections.emptyList();
        }

        Map<PathNode, PathNode> cameFrom = new HashMap<>();
        Map<PathNode, Double> gScore = new HashMap<>();
        Map<PathNode, Double> fScore = new HashMap<>();

        Comparator<PathNode> comparator = (a, b) -> {
            double fa = fScore.getOrDefault(a, Double.POSITIVE_INFINITY);
            double fb = fScore.getOrDefault(b, Double.POSITIVE_INFINITY);
            if (fa == fb) {
                return Double.compare(heuristic.estimate(a, goal), heuristic.estimate(b, goal));
            }
            return Double.compare(fa, fb);
        };
        PriorityQueue<PathNode> open = new PriorityQueue<>(comparator);
        Set<PathNode> closed = new HashSet<>();

        gScore.put(start, 0.0);
        fScore.put(start, heuristic.estimate(start, goal));
        open.add(start);

        while (!open.isEmpty()) {
            PathNode current = open.poll();
            if (current.equals(goal)) {
                return reconstructPath(cameFrom, current);
            }
            closed.add(current);

            for (PathNode neighbor : world.getNeighbors(current)) {
                if (neighbor == null || closed.contains(neighbor)) {
                    continue;
                }
                double edgeCost = useCustomProfile && customProfile != null
                        ? costs.compute(current, neighbor, customProfile)
                        : costs.compute(current, neighbor, profile);
                double tentativeG = gScore.getOrDefault(current, Double.POSITIVE_INFINITY) + edgeCost;
                double existing = gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY);
                if (tentativeG >= existing) {
                    continue;
                }
                cameFrom.put(neighbor, current);
                gScore.put(neighbor, tentativeG);
                fScore.put(neighbor, tentativeG + heuristic.estimate(neighbor, goal));
                open.remove(neighbor);
                open.add(neighbor);
            }
        }
        return Collections.emptyList();
    }

    private List<PathNode> reconstructPath(Map<PathNode, PathNode> cameFrom, PathNode current) {
        List<PathNode> path = new ArrayList<>();
        while (current != null) {
            path.add(current);
            current = cameFrom.get(current);
        }
        Collections.reverse(path);
        return path;
    }
}
