package org.mybad.bedrockparticle.molang.impl;

import org.mybad.bedrockparticle.molang.api.MolangCompiler;
import org.mybad.bedrockparticle.molang.api.MolangExpression;
import org.mybad.bedrockparticle.molang.api.exception.MolangSyntaxException;
import org.mybad.bedrockparticle.molang.impl.ast.Node;
import org.mybad.bedrockparticle.molang.impl.compiler.BytecodeCompiler;
import org.mybad.bedrockparticle.molang.impl.compiler.MolangLexer;
import org.mybad.bedrockparticle.molang.impl.compiler.MolangParser;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public class MolangCompilerImpl implements MolangCompiler {

    private final BytecodeCompiler compiler;

    public MolangCompilerImpl(int flags) {
        this.compiler = new BytecodeCompiler(flags, resolveClassLoader());
    }

    public MolangCompilerImpl(int flags, ClassLoader classLoader) {
        this.compiler = new BytecodeCompiler(flags, classLoader);
    }

    public MolangExpression compile(String input) throws MolangSyntaxException {
        MolangLexer.Token[] tokens = MolangLexer.createTokens(input);
        Node node = MolangParser.parseTokens(tokens);
        return this.compiler.build(node);
    }

    private static ClassLoader resolveClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = MolangExpression.class.getClassLoader();
        }
        return cl != null ? cl : ClassLoader.getSystemClassLoader();
    }
}
