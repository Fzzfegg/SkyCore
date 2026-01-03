package org.mybad.minecraft.particle.molang;

import java.util.Map;

public final class ParticleMolangContext {
    public float particleAge;
    public float particleLifetime;
    public float emitterAge;
    public float emitterLifetime;
    public float random;
    public float random1;
    public float random2;
    public float random3;
    public float random4;
    public final float[] randomExtra = new float[12];
    public float emitterRandom1;
    public float emitterRandom2;
    public float emitterRandom3;
    public float emitterRandom4;
    public final float[] emitterRandomExtra = new float[12];
    public float entityScale;
    public Map<String, Float> curves;

    public ParticleMolangContext() {
    }

    public float getCurveValue(String name) {
        if (curves == null) {
            return 0.0f;
        }
        Float value = curves.get(name);
        return value != null ? value : 0.0f;
    }

    public float getRandom(int index) {
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

    public void setRandom(int index, float value) {
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

    public float getEmitterRandom(int index) {
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

    public void setEmitterRandom(int index, float value) {
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
