package gg.moonflower.molangcompiler.impl.node;

import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.bridge.MolangVariable;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public class MolangVariableNode implements MolangExpression, MolangVariable  {

    private final MolangVariable value;

    public MolangVariableNode(MolangVariable value) {
        this.value = value;
    }

    public MolangVariable value() {
        return this.value;
    }

@Override
    public float get(MolangEnvironment environment) {
        return this.value.getValue();
    }

    @Override
    public String toString() {
        return Float.toString(this.value.getValue());
    }

    @Override
    public float getValue() {
        return this.value.getValue();
    }

    @Override
    public void setValue(float value) {
        this.value.setValue(value);
    }

    @Override
    public MolangExpression createCopy() {
        return new MolangVariableNode(this.copy());
    }

    @Override
    public MolangVariable copy() {
        return this.value.copy();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MolangVariableNode)) return false;
        MolangVariableNode that = (MolangVariableNode) o;
        if (!java.util.Objects.equals(this.value, that.value)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }

}
