package org.mybad.core.expression.molang.exception;

import org.mybad.core.expression.molang.lexer.MolangLexer;

/**
 * Thrown when a lexing issue occurs in {@link MolangLexer}.
 */
public class MolangLexException extends MolangException {

    public MolangLexException(String message) {
        super(message);
    }
}
