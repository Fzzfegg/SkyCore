package org.mybad.core.legacy.expression.molang.ast;

import org.mybad.core.legacy.expression.molang.reference.ExpressionBindingContext;

public class TernaryExpression implements MolangExpression {
    private final MolangExpression condition;
    private final MolangExpression left;
    private final MolangExpression right;

    public TernaryExpression(MolangExpression condition, MolangExpression left, MolangExpression right) {
        this.condition = condition;
        this.left = left;
        this.right = right;
    }

    public MolangExpression getCondition() {
        return condition;
    }

    public MolangExpression getLeft() {
        return left;
    }

    public MolangExpression getRight() {
        return right;
    }

    @Override
    public float evaluate() {
        float conditionValue = condition.evaluate();

        // Condition is true if it isn't 0.0
        if(conditionValue != 0.0) {
            return  left.evaluate();
        }

        return right.evaluate();
    }

    @Override
    public MolangExpression simplify() {
        return new TernaryExpression(
                condition.simplify(),
                left.simplify(),
                right.simplify()
        );
    }

    @Override
    public MolangExpression bind(ExpressionBindingContext context, Object... values) {
        return new TernaryExpression(
                condition.bind(context, values),
                left.bind(context, values),
                right.bind(context, values)
        );
    }
}
