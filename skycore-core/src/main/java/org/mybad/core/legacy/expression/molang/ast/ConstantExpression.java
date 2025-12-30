package org.mybad.core.legacy.expression.molang.ast;

import org.mybad.core.legacy.expression.molang.reference.ExpressionBindingContext;

public class ConstantExpression implements MolangExpression {
    private final float value;

    public ConstantExpression(float value) {
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    @Override
    public float evaluate() {
        return value;
    }

    @Override
    public MolangExpression simplify() {
        return this;
    }

    @Override
    public MolangExpression bind(ExpressionBindingContext context, Object[] values) {
        return this;
    }
}
