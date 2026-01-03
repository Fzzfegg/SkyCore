package org.mybad.minecraft.render.transform;

import org.mybad.core.constraint.Constraint;
import org.mybad.core.data.Model;
import org.mybad.core.data.ModelBone;

/**
 * Applies model constraints to target bones.
 */
public final class ConstraintApplier {
    private ConstraintApplier() {
    }

    public static void apply(Model model) {
        if (model == null || model.getConstraints().isEmpty()) {
            return;
        }
        for (Constraint constraint : model.getConstraints()) {
            ModelBone target = model.getBone(constraint.getTargetBone());
            ModelBone source = model.getBone(constraint.getSourceBone());
            if (target != null && source != null) {
                constraint.apply(target, source);
            }
        }
    }
}
