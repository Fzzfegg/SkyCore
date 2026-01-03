package org.mybad.bedrockparticle.util;

import java.util.Optional;

public final class Either<L, R> {
    private final L left;
    private final R right;
    private final boolean isLeft;

    private Either(L left, R right, boolean isLeft) {
        this.left = left;
        this.right = right;
        this.isLeft = isLeft;
    }

    public static <L, R> Either<L, R> left(L value) {
        return new Either<>(value, null, true);
    }

    public static <L, R> Either<L, R> right(R value) {
        return new Either<>(null, value, false);
    }

    public Optional<L> left() {
        return isLeft ? Optional.ofNullable(left) : Optional.empty();
    }

    public Optional<R> right() {
        return isLeft ? Optional.empty() : Optional.ofNullable(right);
    }

    public boolean isLeft() {
        return isLeft;
    }

    public boolean isRight() {
        return !isLeft;
    }
}
