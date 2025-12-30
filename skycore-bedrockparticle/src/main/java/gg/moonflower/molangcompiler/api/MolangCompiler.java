package gg.moonflower.molangcompiler.api;

import gg.moonflower.molangcompiler.api.exception.MolangException;

public interface MolangCompiler {
    MolangExpression compile(String input) throws MolangException;
}
