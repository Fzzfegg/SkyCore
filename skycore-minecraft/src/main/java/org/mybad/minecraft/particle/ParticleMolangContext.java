package org.mybad.minecraft.particle;

import java.util.Map;

final class ParticleMolangContext {
    float particleAge;
    float particleLifetime;
    float emitterAge;
    float emitterLifetime;
    float random;
    float random1;
    float random2;
    float random3;
    float random4;
    final float[] randomExtra = new float[12];
    float emitterRandom1;
    float emitterRandom2;
    float emitterRandom3;
    float emitterRandom4;
    final float[] emitterRandomExtra = new float[12];
    float entityScale;
    Map<String, Float> curves;

    float getCurveValue(String name) {
        if (curves == null) {
            return 0.0f;
        }
        Float value = curves.get(name);
        return value != null ? value : 0.0f;
    }

    float getRandom(int index) {
        switch (index) {
            case 1:
                return random1;
            case 2:
                return random2;
            case 3:
                return random3;
            case 4:
                return random4;
            default:
                if (index >= 5 && index <= 16) {
                    return randomExtra[index - 5];
                }
                return 0.0f;
        }
    }

    void setRandom(int index, float value) {
        switch (index) {
            case 1:
                random1 = value;
                return;
            case 2:
                random2 = value;
                return;
            case 3:
                random3 = value;
                return;
            case 4:
                random4 = value;
                return;
            default:
                if (index >= 5 && index <= 16) {
                    randomExtra[index - 5] = value;
                }
        }
    }

    float getEmitterRandom(int index) {
        switch (index) {
            case 1:
                return emitterRandom1;
            case 2:
                return emitterRandom2;
            case 3:
                return emitterRandom3;
            case 4:
                return emitterRandom4;
            default:
                if (index >= 5 && index <= 16) {
                    return emitterRandomExtra[index - 5];
                }
                return 0.0f;
        }
    }

    void setEmitterRandom(int index, float value) {
        switch (index) {
            case 1:
                emitterRandom1 = value;
                return;
            case 2:
                emitterRandom2 = value;
                return;
            case 3:
                emitterRandom3 = value;
                return;
            case 4:
                emitterRandom4 = value;
                return;
            default:
                if (index >= 5 && index <= 16) {
                    emitterRandomExtra[index - 5] = value;
                }
        }
    }
}
