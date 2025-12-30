package gg.moonflower.molangcompiler.api.exception;

public class MolangException extends Exception {
    public MolangException(String message) {
        super(message);
    }

    public MolangException(Throwable cause) {
        super(cause);
    }

    public MolangException(String message, Throwable cause) {
        super(message, cause);
    }
}
