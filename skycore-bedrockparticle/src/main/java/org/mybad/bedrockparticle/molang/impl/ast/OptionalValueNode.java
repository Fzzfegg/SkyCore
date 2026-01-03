package org.mybad.bedrockparticle.molang.impl.ast;

import org.jetbrains.annotations.ApiStatus;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public interface OptionalValueNode extends Node {

    Node withReturnValue();
}
