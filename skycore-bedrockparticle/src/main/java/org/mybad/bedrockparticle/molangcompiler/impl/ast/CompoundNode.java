package org.mybad.bedrockparticle.molangcompiler.impl.ast;

import org.mybad.bedrockparticle.molangcompiler.api.exception.MolangException;
import org.mybad.bedrockparticle.molangcompiler.impl.compiler.MolangBytecodeEnvironment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.MethodNode;

/**
 * Inserts multiple nodes in order.
 *
 * @author Ocelot
 */
@ApiStatus.Internal
public class CompoundNode implements Node {
    private final Node[] nodes;

    public CompoundNode(Node... nodes) {
        this.nodes = nodes;
    }

    public Node[] nodes() {
        return this.nodes;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Node node : this.nodes) {
            builder.append(node).append(";\n");
        }
        return builder.toString();
    }

    @Override
    public boolean isConstant() {
        return this.nodes.length == 1 && this.nodes[0].isConstant();
    }

    @Override
    public boolean hasValue() {
        return this.nodes.length > 0 && this.nodes[this.nodes.length - 1].hasValue();
    }

    @Override
    public float evaluate(MolangBytecodeEnvironment environment) throws MolangException {
        return this.nodes[0].evaluate(environment);
    }

    @Override
    public void writeBytecode(MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        for (Node node : this.nodes) {
            node.writeBytecode(method, environment, breakLabel, continueLabel);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CompoundNode)) return false;
        CompoundNode that = (CompoundNode) o;
        if (!java.util.Arrays.equals(this.nodes, that.nodes)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(nodes);
    }
}
