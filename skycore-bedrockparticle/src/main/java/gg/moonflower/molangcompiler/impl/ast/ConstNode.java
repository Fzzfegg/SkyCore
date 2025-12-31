package gg.moonflower.molangcompiler.impl.ast;

import gg.moonflower.molangcompiler.api.exception.MolangException;
import gg.moonflower.molangcompiler.impl.compiler.BytecodeCompiler;
import gg.moonflower.molangcompiler.impl.compiler.MolangBytecodeEnvironment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.MethodNode;

/**
 * Pushes a single constant number onto the stack.
 *
 * @param value The value to push onto the stack
 * @author Buddy, Ocelot
 */
@ApiStatus.Internal
public class ConstNode implements Node  {

    private final float value;

    public ConstNode(float value) {
        this.value = value;
    }

    public float value() {
        return this.value;
    }

@Override
    public String toString() {
        return String.valueOf(this.value);
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public float evaluate(MolangBytecodeEnvironment environment) throws MolangException {
        return this.value;
    }

    @Override
    public void writeBytecode(MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        BytecodeCompiler.writeFloatConst(method, this.value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConstNode)) return false;
        ConstNode that = (ConstNode) o;
        if (!java.util.Objects.equals(this.value, that.value)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }

}
