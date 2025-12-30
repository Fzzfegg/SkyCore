package gg.moonflower.molangcompiler.api;

/**
 * Optional environment that can bind Arcane Molang expressions to runtime values.
 */
public interface BindingMolangEnvironment extends MolangEnvironment {

    /**
     * Bind the compiled expression to this environment for evaluation.
     */
    org.mybad.core.legacy.expression.molang.ast.MolangExpression bind(
        org.mybad.core.legacy.expression.molang.ast.MolangExpression expression
    );
}
