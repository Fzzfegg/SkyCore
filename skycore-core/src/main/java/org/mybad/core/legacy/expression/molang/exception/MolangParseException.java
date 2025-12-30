package org.mybad.core.legacy.expression.molang.exception;

/**
 * Thrown when a parsing issue occurs in {@link dev.omega.arcane.parser.MolangParser}.
 */
public class MolangParseException extends MolangException {

    public MolangParseException(String message) {
        super(message);
    }
}
