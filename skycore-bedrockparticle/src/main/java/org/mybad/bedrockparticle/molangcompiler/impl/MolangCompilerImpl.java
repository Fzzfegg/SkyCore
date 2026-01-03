package org.mybad.bedrockparticle.molangcompiler.impl;

import org.mybad.bedrockparticle.molangcompiler.api.MolangCompiler;
import org.mybad.bedrockparticle.molangcompiler.api.MolangExpression;
import org.mybad.bedrockparticle.molangcompiler.api.exception.MolangSyntaxException;
import org.mybad.bedrockparticle.molangcompiler.impl.ast.Node;
import org.mybad.bedrockparticle.molangcompiler.impl.compiler.BytecodeCompiler;
import org.mybad.bedrockparticle.molangcompiler.impl.compiler.MolangLexer;
import org.mybad.bedrockparticle.molangcompiler.impl.compiler.MolangParser;
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
