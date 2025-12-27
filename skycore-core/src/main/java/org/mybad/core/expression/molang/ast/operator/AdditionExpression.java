package org.mybad.core.expression.molang.ast.operator;

import org.mybad.core.expression.molang.ast.MolangExpression;
import org.mybad.core.expression.molang.reference.ExpressionBindingContext;
import org.jetbrains.annotations.Nullable;

public class AdditionExpression implements MolangExpression {
    private final MolangExpression left;
    private final MolangExpression right;

    public AdditionExpression(MolangExpression left, MolangExpression right) {
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
        return left.evaluate() + right.evaluate();
    }

    @Override
    public MolangExpression simplify() {
        @Nullable MolangExpression simplified = simplifyConstantExpression(left.simplify(), right.simplify());
        return simplified == null ? new AdditionExpression(left.simplify(), right.simplify()) : simplified;
    }

    @Override
    public MolangExpression bind(ExpressionBindingContext context, Object... values) {
        return new AdditionExpression(
                left.bind(context, values),
                right.bind(context, values)
        );
    }
}
