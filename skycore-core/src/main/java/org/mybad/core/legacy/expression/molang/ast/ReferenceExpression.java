package org.mybad.core.legacy.expression.molang.ast;

import org.mybad.core.legacy.expression.molang.Molang;
import org.mybad.core.legacy.expression.molang.reference.ExpressionBindingContext;
import org.mybad.core.legacy.expression.molang.reference.ReferenceType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ReferenceExpression implements MolangExpression {
    private final ReferenceType type;
    private final String value;

    public ReferenceExpression(ReferenceType type, String value) {
        this.type = type;
        this.value = value;
    }

    public ReferenceType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public float evaluate() {
        return 0.0f;
    }

    @Override
    public MolangExpression simplify() {
        return this;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public MolangExpression bind(ExpressionBindingContext context, Object[] values) {
        MolangExpression expression = this;

        // If the context provides a way to bind this ReferenceExpression to an Object value, try to simplify it down now~
        @Nullable List<ExpressionBindingContext.Binder<?>> evaluators = context.getEvaluators(type);
        if(evaluators != null) {
            for (ExpressionBindingContext.Binder binder : evaluators) {
                if(binder.getReferenceName().equals(value)) {
                    @Nullable Class<?> expectedClass = binder.getExpectedClass();

                    // Try to find the expected class the mapper wants from our Object[]
                    if(expectedClass != null) {
                        for (Object value : values) {
                            if(expectedClass.isAssignableFrom(value.getClass())) {
                                return binder.bind(value);
                            }
                        }
                    } else {
                        return binder.bind(null);
                    }
                }
            }
        }

        Molang.LOGGER.warning(String.format("Was not able to bind %s %s to a value!", type.name().toLowerCase(), value));
        return this;
    }
}
