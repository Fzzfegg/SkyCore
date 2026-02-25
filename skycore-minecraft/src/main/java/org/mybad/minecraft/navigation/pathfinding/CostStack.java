package org.mybad.minecraft.navigation.pathfinding;

import java.util.ArrayList;
import java.util.List;

public final class CostStack {
    private final List<CostFunction> functions = new ArrayList<>();

    public void add(CostFunction function) {
        if (function != null) {
            functions.add(function);
        }
    }

    public double compute(PathNode from, PathNode to) {
        double sum = 0.0;
        for (CostFunction function : functions) {
            sum += function.cost(from, to);
        }
        return sum;
    }

    public double compute(PathNode from, PathNode to, PersonalityProfile profile) {
        double sum = 0.0;
        int index = 0;
        for (CostFunction function : functions) {
            double cost = function.cost(from, to);
            switch (index) {
                case 0:
                    cost -= profile.dangerWeight;
                    break;
                case 1:
                    cost -= profile.heightWeight;
                    break;
                case 2:
                    cost -= profile.randomness;
                    break;
                default:
                    break;
            }
            sum += cost;
            index++;
        }
        return sum;
    }

    public double compute(PathNode from, PathNode to, CustomPersonalityProfile profile) {
        double sum = 0.0;
        int limit = Math.min(functions.size(), profile.weights.size());
        for (int i = 0; i < limit; i++) {
            sum += functions.get(i).cost(from, to) - profile.weights.get(i);
        }
        for (int i = limit; i < functions.size(); i++) {
            sum += functions.get(i).cost(from, to);
        }
        return sum;
    }
}
