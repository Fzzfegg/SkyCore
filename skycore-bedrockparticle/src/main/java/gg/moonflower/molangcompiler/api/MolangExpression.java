package gg.moonflower.molangcompiler.api;

/**
 * Simplified MoLang expression.
 */
public interface MolangExpression {

    MolangExpression ZERO = of(0.0f);

    float resolve(MolangEnvironment environment);

    static MolangExpression of(float value) {
        return new ConstantExpression(value);
    }

    static MolangExpression of(boolean value) {
        return new ConstantExpression(value ? 1.0f : 0.0f);
    }

    final class ConstantExpression implements MolangExpression {
        private final float value;

        public ConstantExpression(float value) {
            this.value = value;
        }

        @Override
        public float resolve(MolangEnvironment environment) {
            return value;
        }

        @Override
        public String toString() {
            return Float.toString(value);
        }
    }
}
