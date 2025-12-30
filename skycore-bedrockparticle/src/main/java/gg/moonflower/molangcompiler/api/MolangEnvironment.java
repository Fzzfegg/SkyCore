package gg.moonflower.molangcompiler.api;

/**
 * Simplified MoLang environment interface.
 */
public interface MolangEnvironment {

    /**
     * 安全求值：出现异常时返回 0。
     */
    default float safeResolve(MolangExpression expression) {
        if (expression == null) {
            return 0.0f;
        }
        try {
            return expression.resolve(this);
        } catch (RuntimeException ex) {
            return 0.0f;
        }
    }
}
