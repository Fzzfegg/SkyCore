package org.mybad.core.expression.molang.exception;

public class MolangException extends Exception {

    public MolangException(String message) {
        super(message);
    }

    public MolangException(String message, Throwable cause) {
        super(message, cause);
    }
}
