package org.mybad.bedrockparticle.molang.impl.ast;

import org.mybad.bedrockparticle.molang.api.exception.MolangException;
import org.mybad.bedrockparticle.molang.impl.compiler.MolangBytecodeEnvironment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

/**
 * Retrieves the value of a variable and puts it onto the stack.
 *
 * @param object The object the variable is stored in
 * @param name   The name of the variable
 * @author Ocelot
 */
@ApiStatus.Internal
public class VariableGetNode implements Node  {

    private final String object;

    private final String name;

    public VariableGetNode(String object, String name) {
        this.object = object;
        this.name = name;
    }

    public String object() {
        return this.object;
    }

    public String name() {
        return this.name;
    }

@Override
    public String toString() {
        return this.object + "." + this.name;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void writeBytecode(MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        int index = environment.loadVariable(method, this.object, this.name);
        method.visitVarInsn(Opcodes.FLOAD, index);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VariableGetNode)) return false;
        VariableGetNode that = (VariableGetNode) o;
        if (!java.util.Objects.equals(this.object, that.object)) return false;
        if (!java.util.Objects.equals(this.name, that.name)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(object, name);
    }

}
