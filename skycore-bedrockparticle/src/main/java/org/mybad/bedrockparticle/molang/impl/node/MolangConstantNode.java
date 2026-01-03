package org.mybad.bedrockparticle.molang.impl.node;

import org.mybad.bedrockparticle.molang.api.MolangEnvironment;
import org.mybad.bedrockparticle.molang.api.MolangExpression;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public class MolangConstantNode implements MolangExpression  {

    private final float value;

    public MolangConstantNode(float value) {
        this.value = value;
    }

    public float value() {
        return this.value;
    }

@Override
    public float get(@Nullable MolangEnvironment environment) {
        return this.value;
    }

    @Override
    public float getConstant() {
        return this.value;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public @NotNull String toString() {
        return Float.toString(this.value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MolangConstantNode)) return false;
        MolangConstantNode that = (MolangConstantNode) o;
        if (!java.util.Objects.equals(this.value, that.value)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }

}
