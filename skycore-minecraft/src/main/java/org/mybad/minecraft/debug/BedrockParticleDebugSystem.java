package org.mybad.minecraft.debug;

import org.mybad.minecraft.particle.runtime.BedrockParticleSystem;
import org.mybad.minecraft.particle.transform.EmitterTransformProvider;
import org.mybad.minecraft.resource.ResourceLoader;

/**
 * Debug helper wrapper around the core BedrockParticleSystem.
 * Keeps debug-only entry points separate from the runtime.
 */
public final class BedrockParticleDebugSystem {
    private final BedrockParticleSystem system;

    public BedrockParticleDebugSystem(BedrockParticleSystem system) {
        this.system = system;
    }

    public BedrockParticleDebugSystem(ResourceLoader resourceLoader) {
        this(new BedrockParticleSystem(resourceLoader));
    }

    public BedrockParticleSystem getSystem() {
        return system;
    }

    public boolean spawn(String particlePath, double x, double y, double z, int overrideCount) {
        return system.spawn(particlePath, x, y, z, overrideCount);
    }

    public boolean spawn(String particlePath, EmitterTransformProvider provider, int overrideCount) {
        return system.spawn(particlePath, provider, overrideCount);
    }

    public void clear() {
        system.clear();
    }

    public int getActiveCount() {
        return system.getActiveCount();
    }
}
