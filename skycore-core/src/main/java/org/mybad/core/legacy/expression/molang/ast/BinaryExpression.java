package org.mybad.core.legacy.expression.molang.ast;

import org.mybad.core.legacy.expression.molang.reference.ExpressionBindingContext;
import org.mybad.core.legacy.expression.molang.lexer.MolangTokenType;

public class BinaryExpression implements MolangExpression {
    private final MolangExpression left;
    private final MolangExpression right;
    private final MolangTokenType operator;

    public BinaryExpression(MolangExpression left, MolangExpression right, MolangTokenType operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    public MolangExpression getLeft() {
        return left;
    }

    public MolangExpression getRight() {
        return right;
    }

    public MolangTokenType getOperator() {
        return operator;
    }

    @Override
    public float evaluate() {
        float leftValue = left.evaluate();
        float rightValue = right.evaluate();

        switch(operator) {
            case LESS_THAN:
                return leftValue < rightValue ? 1.0f : 0.0f;
            case GREATER_THAN:
                return leftValue > rightValue ? 1.0f : 0.0f;
            case LESS_THAN_OR_EQUAL:
                return leftValue <= rightValue ? 1.0f : 0.0f;
            case GREATER_THAN_OR_EQUAL:
                return leftValue >= rightValue ? 1.0f : 0.0f;
            case DOUBLE_AMPERSAND:
                if(leftValue == 0.0 || rightValue == 0.0) {
                    return 0.0f;
                }
                return 1.0f;
            case DOUBLE_PIPE:
                if(leftValue != 0.0 || rightValue != 0.0) {
                    return 1.0f;
                }
                return 0.0f;
            default:
                throw new RuntimeException("Operator type '" + operator + "' is not supported for Unary operations.");
        }
    }

    @Override
    public MolangExpression simplify() {
        MolangExpression leftSimplified = left.simplify();
        MolangExpression rightSimplified = right.simplify();
        BinaryExpression simplifiedExpression = new BinaryExpression(leftSimplified, rightSimplified, operator);

        // If both simplified inputs are constants, we can evaluate them now with the operand to pull out a single ConstantExpression.
        if(leftSimplified instanceof ConstantExpression && rightSimplified instanceof ConstantExpression) {
            return new ConstantExpression(simplifiedExpression.evaluate());
        }

        return simplifiedExpression;
    }

    @Override
    public MolangExpression bind(ExpressionBindingContext context, Object... values) {
        return new BinaryExpression(
                left.bind(context, values),
                right.bind(context, values),
                operator
        );
    }
}
