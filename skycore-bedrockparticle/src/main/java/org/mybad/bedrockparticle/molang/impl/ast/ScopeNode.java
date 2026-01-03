package org.mybad.bedrockparticle.molang.impl.ast;

import org.mybad.bedrockparticle.molang.api.exception.MolangException;
import org.mybad.bedrockparticle.molang.impl.compiler.MolangBytecodeEnvironment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.MethodNode;

/**
 * Represents a local scope with independent variables.
 *
 * @param node The node within this scope
 * @author Ocelot
 */
@ApiStatus.Internal
public class ScopeNode implements Node  {

    private final Node node;

    public ScopeNode(Node node) {
        this.node = node;
    }

    public Node node() {
        return this.node;
    }

@Override
    public String toString() {
        return "{" + this.node + "}";
    }

    @Override
    public boolean isConstant() {
        return this.node.isConstant();
    }

    @Override
    public boolean hasValue() {
        return this.node.hasValue();
    }

    @Override
    public float evaluate(MolangBytecodeEnvironment environment) throws MolangException {
        return this.node.evaluate(environment);
    }

    @Override
    public void writeBytecode(MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        MolangBytecodeEnvironment scopeEnvironment = new MolangBytecodeEnvironment(environment);
        this.node.writeBytecode(method, scopeEnvironment, breakLabel, continueLabel);
        scopeEnvironment.writeModifiedVariables(method);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ScopeNode)) return false;
        ScopeNode that = (ScopeNode) o;
        if (!java.util.Objects.equals(this.node, that.node)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(node);
    }

}
