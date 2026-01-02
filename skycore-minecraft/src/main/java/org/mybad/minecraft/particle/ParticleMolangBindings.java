package org.mybad.minecraft.particle;

import gg.moonflower.molangcompiler.api.MolangEnvironmentBuilder;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.MolangRuntime;
import gg.moonflower.pinwheel.particle.ParticleData;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class ParticleMolangBindings {
    private ParticleMolangBindings() {
    }

    static MolangRuntime createRuntime(ParticleMolangContext context, Map<String, ParticleData.Curve> curves) {
        MolangRuntime.Builder builder = MolangRuntime.runtime();
        bindCommonVariables(builder, context);
        bindCurves(builder, context, curves);
        return builder.create();
    }

    static void bindCommonVariables(MolangEnvironmentBuilder<?> builder, ParticleMolangContext context) {
        builder.setVariable("particle_age", MolangExpression.of(() -> context.particleAge));
        builder.setVariable("particle_lifetime", MolangExpression.of(() -> context.particleLifetime));
        builder.setVariable("emitter_age", MolangExpression.of(() -> context.emitterAge));
        builder.setVariable("emitter_lifetime", MolangExpression.of(() -> context.emitterLifetime));
        builder.setVariable("random", MolangExpression.of(() -> context.random));
        builder.setVariable("entity_scale", MolangExpression.of(() -> context.entityScale));

        builder.setQuery("particle_age", MolangExpression.of(() -> context.particleAge));
        builder.setQuery("particle_lifetime", MolangExpression.of(() -> context.particleLifetime));
        builder.setQuery("emitter_age", MolangExpression.of(() -> context.emitterAge));
        builder.setQuery("emitter_lifetime", MolangExpression.of(() -> context.emitterLifetime));
        builder.setQuery("random", MolangExpression.of(() -> context.random));
        builder.setQuery("entity_scale", MolangExpression.of(() -> context.entityScale));
        builder.setQuery("age", MolangExpression.of(() -> context.particleAge));
        builder.setQuery("life_time", MolangExpression.of(() -> context.particleLifetime));

        for (int i = 1; i <= 16; i++) {
            final int index = i;
            builder.setVariable("random_" + i, MolangExpression.of(() -> context.getRandom(index)));
            builder.setVariable("particle_random_" + i, MolangExpression.of(() -> context.getRandom(index)));
            builder.setVariable("emitter_random_" + i, MolangExpression.of(() -> context.getEmitterRandom(index)));

            builder.setQuery("random_" + i, MolangExpression.of(() -> context.getRandom(index)));
            builder.setQuery("particle_random_" + i, MolangExpression.of(() -> context.getRandom(index)));
            builder.setQuery("emitter_random_" + i, MolangExpression.of(() -> context.getEmitterRandom(index)));
        }
    }

    static void bindCurves(MolangEnvironmentBuilder<?> builder, ParticleMolangContext context, Map<String, ParticleData.Curve> curves) {
        if (curves == null || curves.isEmpty()) {
            return;
        }
        for (String name : curves.keySet()) {
            final String key = name;
            builder.setVariable(key, MolangExpression.of(() -> context.getCurveValue(key)));
        }
    }

    static Map<String, ParticleData.Curve> buildCurveDefinitions(ParticleData data) {
        if (data == null || data.curves() == null || data.curves().isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, ParticleData.Curve> curves = new LinkedHashMap<>();
        for (Map.Entry<String, ParticleData.Curve> entry : data.curves().entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty()) {
                continue;
            }
            String name = key;
            int dot = key.indexOf('.');
            if (dot >= 0 && dot + 1 < key.length()) {
                name = key.substring(dot + 1);
            }
            curves.put(name, entry.getValue());
        }
        return curves;
    }
}
