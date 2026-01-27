package org.mybad.core.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 基岩模型立方体
 * 代表模型中的一个可渲染的立方体单元
 * 包含位置、大小、旋转和UV映射
 *
 * 支持顶点预计算以提高渲染性能
 */
public class ModelCube {
    // 几何信息
    private float[] origin;     // 立方体起点 [x, y, z]
    private float[] size;       // 立方体大小 [width, height, depth]
    private float[] rotation;   // 立方体旋转 [x, y, z] 单位：度数
    private float[] pivot;      // 旋转中心 [x, y, z]

    // UV映射
    private UVMapping uv;

    // 标志
    private boolean mirror;
    private float inflate;

    // 预计算的四边形数据
    private List<ModelQuad> quads;

    public ModelCube() {
        this.origin = new float[]{0, 0, 0};
        this.size = new float[]{1, 1, 1};
        this.rotation = new float[]{0, 0, 0};
        this.pivot = new float[]{0, 0, 0};
        this.mirror = false;
        this.inflate = 0;
        this.quads = new ArrayList<>();
    }

    /**
     * 复制构造函数
     */
    public ModelCube(ModelCube other) {
        this.origin = other.origin.clone();
        this.size = other.size.clone();
        this.rotation = other.rotation.clone();
        this.pivot = other.pivot.clone();
        this.mirror = other.mirror;
        this.inflate = other.inflate;
        this.uv = other.uv != null ? new UVMapping(other.uv) : null;
        this.quads = new ArrayList<>();
    }

    /**
     * 生成预计算的四边形数据
     * 应在模型加载后调用一次
     *
     * @param textureWidth  纹理宽度
     * @param textureHeight 纹理高度
     */
    public void generateQuads(int textureWidth, int textureHeight) {
        quads.clear();

        if (uv == null) {
            return;
        }

        // 如果是 Box UV 模式，先计算各面 UV
        float width = size[0];
        float height = size[1];
        float depth = size[2];
        boolean flipX = width < 0;
        boolean flipY = height < 0;
        boolean flipZ = depth < 0;
        boolean flipWinding = flipX ^ flipY ^ flipZ; // 奇数个负轴时整体镜像
        if (!uv.isPerFace()) {
            uv.setupBoxUV(Math.abs(width), Math.abs(height), Math.abs(depth), mirror);
        }

        float tw = 1.0f / textureWidth;
        float th = 1.0f / textureHeight;

        // 计算顶点位置（参考 Chameleon 的坐标转换）
        // Bedrock: origin 是立方体的一个角，size 向正方向延伸
        // 但 Chameleon 的计算方式是 origin.x - size.x
        float convertedOriginX = -origin[0];
        float rawMinX = (convertedOriginX - width - inflate) / 16.0f;
        float rawMaxX = (convertedOriginX + inflate) / 16.0f;
        float rawMinY = (origin[1] - inflate) / 16.0f;
        float rawMaxY = (origin[1] + height + inflate) / 16.0f;
        float rawMinZ = (origin[2] - inflate) / 16.0f;
        float rawMaxZ = (origin[2] + depth + inflate) / 16.0f;

        float minX = Math.min(rawMinX, rawMaxX);
        float maxX = Math.max(rawMinX, rawMaxX);
        float minY = Math.min(rawMinY, rawMaxY);
        float maxY = Math.max(rawMinY, rawMaxY);
        float minZ = Math.min(rawMinZ, rawMaxZ);
        float maxZ = Math.max(rawMinZ, rawMaxZ);

        // 跳过 0 尺寸的面
        boolean hasWidth = Math.abs(width) > 0;
        boolean hasHeight = Math.abs(height) > 0;
        boolean hasDepth = Math.abs(depth) > 0;

        // North face (Z-)
        if (hasWidth && hasHeight && uv.getNorth() != null) {
            float[] uvn = uv.getNorth();
            quads.add(createQuad(
                new ModelVertex(maxX, minY, minZ, uvn[0] * tw, uvn[3] * th),
                new ModelVertex(minX, minY, minZ, uvn[2] * tw, uvn[3] * th),
                new ModelVertex(minX, maxY, minZ, uvn[2] * tw, uvn[1] * th),
                new ModelVertex(maxX, maxY, minZ, uvn[0] * tw, uvn[1] * th),
                ModelQuad.Direction.NORTH,
                flipWinding
            ));
        }

        // South face (Z+)
        if (hasWidth && hasHeight && uv.getSouth() != null) {
            float[] uvs = uv.getSouth();
            quads.add(createQuad(
                new ModelVertex(minX, minY, maxZ, uvs[0] * tw, uvs[3] * th),
                new ModelVertex(maxX, minY, maxZ, uvs[2] * tw, uvs[3] * th),
                new ModelVertex(maxX, maxY, maxZ, uvs[2] * tw, uvs[1] * th),
                new ModelVertex(minX, maxY, maxZ, uvs[0] * tw, uvs[1] * th),
                ModelQuad.Direction.SOUTH,
                flipWinding
            ));
        }

        // East face (X+)
        if (hasDepth && hasHeight && uv.getEast() != null) {
            float[] uve = uv.getEast();
            quads.add(createQuad(
                new ModelVertex(maxX, minY, maxZ, uve[0] * tw, uve[3] * th),
                new ModelVertex(maxX, minY, minZ, uve[2] * tw, uve[3] * th),
                new ModelVertex(maxX, maxY, minZ, uve[2] * tw, uve[1] * th),
                new ModelVertex(maxX, maxY, maxZ, uve[0] * tw, uve[1] * th),
                ModelQuad.Direction.EAST,
                flipWinding
            ));
        }

        // West face (X-)
        if (hasDepth && hasHeight && uv.getWest() != null) {
            float[] uvw = uv.getWest();
            quads.add(createQuad(
                new ModelVertex(minX, minY, minZ, uvw[0] * tw, uvw[3] * th),
                new ModelVertex(minX, minY, maxZ, uvw[2] * tw, uvw[3] * th),
                new ModelVertex(minX, maxY, maxZ, uvw[2] * tw, uvw[1] * th),
                new ModelVertex(minX, maxY, minZ, uvw[0] * tw, uvw[1] * th),
                ModelQuad.Direction.WEST,
                flipWinding
            ));
        }

        // Up face (Y+)
        if (hasWidth && hasDepth && uv.getUp() != null) {
            float[] uvu = uv.getUp();
            quads.add(createQuad(
                new ModelVertex(maxX, maxY, minZ, uvu[0] * tw, uvu[3] * th),
                new ModelVertex(minX, maxY, minZ, uvu[2] * tw, uvu[3] * th),
                new ModelVertex(minX, maxY, maxZ, uvu[2] * tw, uvu[1] * th),
                new ModelVertex(maxX, maxY, maxZ, uvu[0] * tw, uvu[1] * th),
                ModelQuad.Direction.UP,
                flipWinding
            ));
        }

        // Down face (Y-)
        if (hasWidth && hasDepth && uv.getDown() != null) {
            float[] uvd = uv.getDown();
            quads.add(createQuad(
                new ModelVertex(minX, minY, minZ, uvd[2] * tw, uvd[1] * th),
                new ModelVertex(maxX, minY, minZ, uvd[0] * tw, uvd[1] * th),
                new ModelVertex(maxX, minY, maxZ, uvd[0] * tw, uvd[3] * th),
                new ModelVertex(minX, minY, maxZ, uvd[2] * tw, uvd[3] * th),
                ModelQuad.Direction.DOWN,
                flipWinding
            ));
        }
    }

    /**
     * 创建四边形
     */
    private ModelQuad createQuad(ModelVertex v1, ModelVertex v2, ModelVertex v3, ModelVertex v4,
                                  ModelQuad.Direction direction, boolean invert) {
        if (invert) {
            return new ModelQuad(v4, v3, v2, v1, -direction.nx, -direction.ny, -direction.nz, direction);
        }
        return new ModelQuad(v1, v2, v3, v4, direction.nx, direction.ny, direction.nz, direction);
    }

    /**
     * 获取预计算的四边形列表
     */
    public List<ModelQuad> getQuads() {
        return quads;
    }

    /**
     * 检查是否已生成四边形
     */
    public boolean hasQuads() {
        return !quads.isEmpty();
    }

    /**
     * 检查立方体是否有旋转
     */
    public boolean hasRotation() {
        return rotation[0] != 0 || rotation[1] != 0 || rotation[2] != 0;
    }

    // Getters
    public float[] getOrigin() { return origin; }
    public float[] getSize() { return size; }
    public float[] getRotation() { return rotation; }
    public float[] getPivot() { return pivot; }
    public UVMapping getUV() { return uv; }
    public boolean isMirror() { return mirror; }
    public float getInflate() { return inflate; }
    public float getInflateAmount() { return inflate; }  // 别名

    // Setters
    public void setOrigin(float x, float y, float z) {
        this.origin[0] = x;
        this.origin[1] = y;
        this.origin[2] = z;
    }

    public void setOrigin(float[] origin) {
        this.origin = origin;
    }

    public void setSize(float width, float height, float depth) {
        this.size[0] = width;
        this.size[1] = height;
        this.size[2] = depth;
    }

    public void setSize(float[] size) {
        this.size = size;
    }

    public void setRotation(float x, float y, float z) {
        this.rotation[0] = x;
        this.rotation[1] = y;
        this.rotation[2] = z;
    }

    public void setRotation(float[] rotation) {
        this.rotation = rotation;
    }

    public void setPivot(float x, float y, float z) {
        this.pivot[0] = x;
        this.pivot[1] = y;
        this.pivot[2] = z;
    }

    public void setPivot(float[] pivot) {
        this.pivot = pivot;
    }

    public void setUV(UVMapping uv) { this.uv = uv; }
    public void setMirror(boolean mirror) { this.mirror = mirror; }
    public void setInflate(float inflate) { this.inflate = inflate; }
    public void setInflate(boolean inflate) { /* 兼容旧API */ }
    public void setInflateAmount(float amount) { this.inflate = amount; }
}
