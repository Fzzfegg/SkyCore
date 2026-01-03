/*
 * Original work Copyright (C) 2023 Ocelot
 * Original code licensed under MIT License
 *
 * Modified by 17Artist on 2025-3-29
 * Modifications and redistribution licensed under GNU Lesser General Public License v3.0
 *
 * Changes:
 * - Renamed package from 'org.mybad.bedrockparticle.pinwheel.*' to 'priv.seventeen.artist.*' (all subpackages)
 * - Changed license from MIT to LGPL v3.0
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package org.mybad.bedrockparticle.pinwheel.particle;

import org.mybad.bedrockparticle.molangcompiler.api.GlobalMolangCompiler;
import org.mybad.bedrockparticle.molangcompiler.api.MolangCompiler;

/**
 * Retrieves the compiler instance pinwheel should use.
 *
 * @author Ocelot
 * @since 1.1.0
 */
public final class PinwheelMolangCompiler {

    private static MolangCompiler compiler = input -> GlobalMolangCompiler.get().compile(input);

    /**
     * @return The current molang compiler instance
     */
    public static MolangCompiler get() {
        return compiler;
    }

    /**
     * Sets the current molang compiler instance.
     *
     * @param compiler The new compiler to use
     */
    public static void set(MolangCompiler compiler) {
        PinwheelMolangCompiler.compiler = compiler;
    }
}
