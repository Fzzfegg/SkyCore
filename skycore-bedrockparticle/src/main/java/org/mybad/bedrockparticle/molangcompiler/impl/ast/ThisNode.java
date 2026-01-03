package org.mybad.bedrockparticle.molangcompiler.impl.ast;

import org.mybad.bedrockparticle.molangcompiler.api.exception.MolangException;
import org.mybad.bedrockparticle.molangcompiler.impl.compiler.BytecodeCompiler;
import org.mybad.bedrockparticle.molangcompiler.impl.compiler.MolangBytecodeEnvironment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

/**
 * Retrieves the value of a "this" and puts it onto the stack.
 */
@ApiStatus.Internal
public class ThisNode implements Node {

    public static final ThisNode INSTANCE = new ThisNode();

    @Override
    public String toString() {
        return "this";
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
        Integer index = environment.variables().get("this");
        if (index == null) {
            index = environment.allocateVariable("this");
            method.visitVarInsn(Opcodes.ALOAD, BytecodeCompiler.RUNTIME_INDEX);
            method.visitMethodInsn(
                    Opcodes.INVOKEINTERFACE,
                    "gg/moonflower/molangcompiler/api/MolangEnvironment",
                    "getThis",
                    "()F",
                    true
            );
            method.visitVarInsn(Opcodes.FSTORE, index);
        }

        method.visitVarInsn(Opcodes.FLOAD, index);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ThisNode;
    }

    @Override
    public int hashCode() {
        return ThisNode.class.hashCode();
    }
}
