package gg.moonflower.molangcompiler.api.object;

import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.exception.MolangRuntimeException;

import java.util.Collection;

/**
 * A {@link MolangObject} that cannot have any values modified.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public class ImmutableMolangObject implements MolangObject  {

    private final MolangObject parent;

    public ImmutableMolangObject(MolangObject parent) {
        this.parent = parent;
    }

    public MolangObject parent() {
        return this.parent;
    }

    @Override
    public MolangObject getCopy() {
        return this.createCopy();
    }

    @Override
    public MolangObject createCopy() {
        return new ImmutableMolangObject(this.parent.createCopy());
    }

@Override
    public void set(String name, MolangExpression value) throws MolangRuntimeException {
        throw new MolangRuntimeException("Cannot set values on an immutable object");
    }

    @Override
    public void remove(String name) throws MolangRuntimeException {
        throw new MolangRuntimeException("Cannot set values on an immutable object");
    }

    @Override
    public MolangExpression get(String name) throws MolangRuntimeException {
        return this.parent.get(name);
    }

    @Override
    public boolean has(String name) {
        return this.parent.has(name);
    }

    @Override
    public Collection<String> getKeys() {
        return this.parent.getKeys();
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public String toString() {
        return this.parent.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ImmutableMolangObject)) return false;
        ImmutableMolangObject that = (ImmutableMolangObject) o;
        if (!java.util.Objects.equals(this.parent, that.parent)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(parent);
    }

}
