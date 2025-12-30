package org.mybad.core.legacy.expression.molang.ast.operator;

import org.mybad.core.legacy.expression.molang.ast.MolangExpression;
import org.mybad.core.legacy.expression.molang.reference.ExpressionBindingContext;
import org.jetbrains.annotations.Nullable;

public class DivisionExpression implements MolangExpression {
    private final MolangExpression left;
    private final MolangExpression right;

    public DivisionExpression(MolangExpression left, MolangExpression right) {
        this.left = left;
        this.right = right;
    }

    public MolangExpression getLeft() {
        return left;
    }

    public MolangExpression getRight() {
        return right;
    }

    @Override
    public float evaluate() {
        float bottom = right.evaluate();
        if(bottom == 0.0) {
            return 0.0f;
        }

        return left.evaluate() / bottom;
    }

    @Override
    public MolangExpression simplify() {
        @Nullable MolangExpression simplified = simplifyConstantExpression(left, right);
        return simplified == null ? new DivisionExpression(left.simplify(), right.simplify()) : simplified;
    }

    @Override
    public MolangExpression bind(ExpressionBindingContext context, Object... values) {
        return new DivisionExpression(
                left.bind(context, values),
                right.bind(context, values)
        );
    }
}
