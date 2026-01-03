package org.mybad.bedrockparticle.molangcompiler.impl.node;

import org.mybad.bedrockparticle.molangcompiler.api.MolangEnvironment;
import org.mybad.bedrockparticle.molangcompiler.api.MolangExpression;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public class MolangDynamicNode implements MolangExpression  {

    private final Supplier<Float> value;

    public MolangDynamicNode(Supplier<Float> value) {
        this.value = value;
    }

    public Supplier<Float> value() {
        return this.value;
    }

@Override
    public float get(MolangEnvironment environment) {
        return this.value.get();
    }

    @Override
    public String toString() {
        return Float.toString(this.value.get());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MolangDynamicNode)) return false;
        MolangDynamicNode that = (MolangDynamicNode) o;
        if (!java.util.Objects.equals(this.value, that.value)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }

}
