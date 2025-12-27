package org.mybad.core.expression.molang.lexer;

public class MolangTokenInstance {
    private final MolangTokenType type;
    private final String lexeme;
    private final Object value;

    public MolangTokenInstance(MolangTokenType type, String lexeme, Object value) {
        this.type = type;
        this.lexeme = lexeme;
        this.value = value;
    }

    public MolangTokenInstance(MolangTokenType type, String lexeme) {
        this(type, lexeme, null);
    }

    public MolangTokenType type() {
        return type;
    }

    public String lexeme() {
        return lexeme;
    }

    public Object value() {
        return value;
    }
}
