package org.mybad.core.expression.molang.ast;

import org.mybad.core.expression.molang.reference.ExpressionBindingContext;
import org.mybad.core.expression.molang.lexer.MolangTokenType;

public class UnaryExpression implements MolangExpression {
    private final MolangExpression expression;
    private final MolangTokenType operator;

    public UnaryExpression(MolangExpression expression, MolangTokenType operator) {
        this.expression = expression;
        this.operator = operator;
    }

    public MolangExpression getExpression() {
        return expression;
    }

    public MolangTokenType getOperator() {
        return operator;
    }

    @Override
    public float evaluate() {
        float value = expression.evaluate();

        switch(operator) {
            case BANG:
                if(value == 0.0f) {
                    return 1.0f;
                } else if (value == 1.0f) {
                    return 0.0f;
                }
                return 0.0f;
            case MINUS:
                return -value;
            case PLUS:
                return value;
            default:
                throw new IllegalStateException("UnaryExpression found invalid MoLangTokenType " + operator.toString());
        }
    }

    @Override
    public MolangExpression simplify() {
        MolangExpression simplifiedInput = expression.simplify();
        UnaryExpression simplifiedUnary = new UnaryExpression(simplifiedInput, operator);

        if(simplifiedInput instanceof ConstantExpression) {
            return new ConstantExpression(simplifiedUnary.evaluate());
        }

        return simplifiedUnary;
    }

    @Override
    public MolangExpression bind(ExpressionBindingContext context, Object[] values) {
        return new UnaryExpression(expression.bind(context, values), operator);
    }
}
