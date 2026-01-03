package org.mybad.bedrockparticle.molang.impl.ast;

import org.mybad.bedrockparticle.molang.api.exception.MolangException;
import org.mybad.bedrockparticle.molang.api.exception.MolangSyntaxException;
import org.mybad.bedrockparticle.molang.impl.compiler.MolangBytecodeEnvironment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

/**
 * Breaks free from a loop if currently looping.
 *
 * @author Buddy
 */
@ApiStatus.Internal
public class BreakNode implements Node {

    public static final BreakNode INSTANCE = new BreakNode();

    @Override
    public String toString() {
        return "break";
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public void writeBytecode(MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        if (breakLabel == null) {
            throw new MolangSyntaxException("Cannot break outside of loop");
        }
        method.visitJumpInsn(Opcodes.GOTO, breakLabel);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BreakNode;
    }

    @Override
    public int hashCode() {
        return BreakNode.class.hashCode();
    }
}
