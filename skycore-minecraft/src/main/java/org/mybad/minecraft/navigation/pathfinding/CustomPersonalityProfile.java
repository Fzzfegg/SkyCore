package org.mybad.minecraft.navigation.pathfinding;

import java.util.ArrayList;
import java.util.List;

public final class CustomPersonalityProfile {
    public final List<Double> weights = new ArrayList<>();

    public void addWeight(double value) {
        weights.add(value);
    }
}
