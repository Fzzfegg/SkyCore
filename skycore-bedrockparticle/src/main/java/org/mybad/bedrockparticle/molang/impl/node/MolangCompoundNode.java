package org.mybad.bedrockparticle.molang.impl.node;

import org.mybad.bedrockparticle.molang.api.MolangEnvironment;
import org.mybad.bedrockparticle.molang.api.MolangExpression;
import org.mybad.bedrockparticle.molang.api.exception.MolangRuntimeException;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public class MolangCompoundNode implements MolangExpression  {

    private final MolangExpression[] expressions;

    public MolangCompoundNode(MolangExpression... expressions) {
        this.expressions = expressions;
    }

    public MolangExpression[] expressions() {
        return this.expressions;
    }

@Override
    public float get(MolangEnvironment environment) throws MolangRuntimeException {
        for (int i = 0; i < this.expressions.length; i++) {
            float result = environment.resolve(this.expressions[i]);
            // The last expression is expected to have the `return`
            if (i >= this.expressions.length - 1) {
                return result;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < this.expressions.length; i++) {
            if (i >= this.expressions.length - 1) {
                builder.append("return ");
            }
            builder.append(this.expressions[i]);
            builder.append(';');
            if (i < this.expressions.length - 1) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MolangCompoundNode)) return false;
        MolangCompoundNode that = (MolangCompoundNode) o;
        if (!java.util.Objects.equals(this.expressions, that.expressions)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(expressions);
    }

}
