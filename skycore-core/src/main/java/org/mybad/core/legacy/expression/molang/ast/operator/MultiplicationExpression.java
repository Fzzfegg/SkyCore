package org.mybad.core.legacy.expression.molang.ast.operator;

import org.mybad.core.legacy.expression.molang.ast.MolangExpression;
import org.mybad.core.legacy.expression.molang.reference.ExpressionBindingContext;
import org.jetbrains.annotations.Nullable;

public class MultiplicationExpression implements MolangExpression {
    private final MolangExpression left;
    private final MolangExpression right;

    public MultiplicationExpression(MolangExpression left, MolangExpression right) {
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
        return left.evaluate() * right.evaluate();
    }

    @Override
    public MolangExpression simplify() {
        @Nullable MolangExpression simplified = simplifyConstantExpression(left, right);
        return simplified == null ? new MultiplicationExpression(left.simplify(), right.simplify()) : simplified;
    }

    @Override
    public MolangExpression bind(ExpressionBindingContext context, Object... values) {
        return new MultiplicationExpression(
                left.bind(context, values),
                right.bind(context, values)
        );
    }
}
