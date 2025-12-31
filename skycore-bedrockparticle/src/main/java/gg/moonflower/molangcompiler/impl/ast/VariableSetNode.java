package gg.moonflower.molangcompiler.impl.ast;

import gg.moonflower.molangcompiler.api.exception.MolangException;
import gg.moonflower.molangcompiler.impl.compiler.MolangBytecodeEnvironment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

/**
 * Sets the value of a variable.
 *
 * @param object      The object the variable is stored in
 * @param name        The name of the variable
 * @param value       The value to set to the variable
 * @param returnValue Whether to return the new value of the variable
 * @author Ocelot
 */
@ApiStatus.Internal
public class VariableSetNode implements OptionalValueNode {

    private final String object;
    private final String name;
    private final Node value;
    private final boolean returnValue;

    public VariableSetNode(String object, String name, Node value, boolean returnValue) {
        this.object = object;
        this.name = name;
        this.value = value;
        this.returnValue = returnValue;
    }

    public VariableSetNode(String object, String name, Node value) {
        this(object, name, value, false);
    }

    public String object() {
        return this.object;
    }

    public String name() {
        return this.name;
    }

    public Node value() {
        return this.value;
    }

    public boolean returnValue() {
        return this.returnValue;
    }

    @Override
    public String toString() {
        return this.object + "." + this.name + " = " + this.value;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return this.returnValue;
    }

    @Override
    public void writeBytecode(MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        if (!"temp".equals(this.object)) {
            // Insert at earliest opportunity if required
            environment.getObjectIndex(method, this.object);
        }

        this.value.writeBytecode(method, environment, breakLabel, continueLabel);
        if (this.returnValue) {
            method.visitInsn(Opcodes.DUP);
        }
        int index = environment.allocateVariable(this.object + "." + this.name);
        method.visitVarInsn(Opcodes.FSTORE, index);
        environment.markDirty(this.object, this.name);
    }

    @Override
    public VariableSetNode withReturnValue() {
        return this.returnValue ? this : new VariableSetNode(this.object, this.name, this.value, true);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VariableSetNode)) return false;
        VariableSetNode that = (VariableSetNode) o;
        if (!java.util.Objects.equals(this.object, that.object)) return false;
        if (!java.util.Objects.equals(this.name, that.name)) return false;
        if (!java.util.Objects.equals(this.value, that.value)) return false;
        if (this.returnValue != that.returnValue) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(object, name, value, returnValue);
    }
}
