package org.mybad.bedrockparticle.molangcompiler.impl.ast;

import org.mybad.bedrockparticle.molangcompiler.api.exception.MolangException;
import org.mybad.bedrockparticle.molangcompiler.api.exception.MolangSyntaxException;
import org.mybad.bedrockparticle.molangcompiler.impl.compiler.MolangBytecodeEnvironment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

/**
 * Goes to the next iteration of a loop if currently looping.
 *
 * @author Buddy
 */
@ApiStatus.Internal
public class ContinueNode implements Node {

    public static final ContinueNode INSTANCE = new ContinueNode();

    @Override
    public String toString() {
        return "continue";
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
        if (continueLabel == null) {
            throw new MolangSyntaxException("Cannot continue outside of loop");
        }
        method.visitJumpInsn(Opcodes.GOTO, continueLabel);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ContinueNode;
    }

    @Override
    public int hashCode() {
        return ContinueNode.class.hashCode();
    }
}
