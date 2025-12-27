package org.mybad.core.data;

/**
 * 预计算的四边形数据
 * 包含4个顶点和法线方向
 */
public class ModelQuad {
    /** 四个顶点 (逆时针顺序) */
    public final ModelVertex[] vertices;

    /** 法线方向 */
    public final float normalX, normalY, normalZ;

    /** 面的方向 */
    public final Direction direction;

    public ModelQuad(ModelVertex v1, ModelVertex v2, ModelVertex v3, ModelVertex v4,
                     float normalX, float normalY, float normalZ, Direction direction) {
        this.vertices = new ModelVertex[]{v1, v2, v3, v4};
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
        this.direction = direction;
    }

    /**
     * 面的方向枚举
     */
    public enum Direction {
        NORTH(0, 0, -1),   // Z-
        SOUTH(0, 0, 1),    // Z+
        EAST(1, 0, 0),     // X+
        WEST(-1, 0, 0),    // X-
        UP(0, 1, 0),       // Y+
        DOWN(0, -1, 0);    // Y-

        public final float nx, ny, nz;

        Direction(float nx, float ny, float nz) {
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
        }
    }

    /**
     * 构建器，方便链式调用
     */
    public static class Builder {
        private ModelVertex v1, v2, v3, v4;
        private Direction direction;

        public Builder vertex(ModelVertex v1, ModelVertex v2, ModelVertex v3, ModelVertex v4) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            this.v4 = v4;
            return this;
        }

        public Builder direction(Direction direction) {
            this.direction = direction;
            return this;
        }

        public ModelQuad build() {
            return new ModelQuad(v1, v2, v3, v4, direction.nx, direction.ny, direction.nz, direction);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
