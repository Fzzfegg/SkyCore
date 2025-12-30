package gg.moonflower.molangcompiler.api;

import gg.moonflower.molangcompiler.api.exception.MolangException;
import org.mybad.core.legacy.expression.molang.exception.MolangLexException;
import org.mybad.core.legacy.expression.molang.exception.MolangParseException;
import org.mybad.core.legacy.expression.molang.parser.MolangParser;

/**
 * Global MoLang compiler entry (backed by Arcane).
 */
public final class GlobalMolangCompiler {

    private static MolangCompiler compiler = new ArcaneCompiler();

    private GlobalMolangCompiler() {
    }

    public static MolangCompiler get() {
        return compiler;
    }

    public static void set(MolangCompiler compiler) {
        if (compiler != null) {
            GlobalMolangCompiler.compiler = compiler;
        }
    }

    private static final class ArcaneCompiler implements MolangCompiler {
        @Override
        public MolangExpression compile(String input) throws MolangException {
            try {
                return new ArcaneExpression(input);
            } catch (RuntimeException ex) {
                throw new MolangException(ex);
            }
        }
    }

    private static final class ArcaneExpression implements MolangExpression {
        private final org.mybad.core.legacy.expression.molang.ast.MolangExpression expression;

        private ArcaneExpression(String input) {
            try {
                this.expression = MolangParser.parse(input);
            } catch (MolangLexException | MolangParseException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public float resolve(MolangEnvironment environment) {
            if (environment instanceof BindingMolangEnvironment) {
                org.mybad.core.legacy.expression.molang.ast.MolangExpression bound =
                    ((BindingMolangEnvironment) environment).bind(expression);
                if (bound != null) {
                    return bound.evaluate();
                }
            }
            return expression.evaluate();
        }

        @Override
        public String toString() {
            return String.valueOf(expression);
        }
    }
}
