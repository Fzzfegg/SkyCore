package org.mybad.core.legacy.render;

import org.mybad.core.data.Model;
import org.mybad.core.data.ModelBone;
import org.mybad.core.data.ModelCube;
import org.mybad.core.render.CoreMatrixStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 立方体处理器：既提供 Chameleon 风格的顶点烘焙工具，也实现 {@link ModelProcessor}
 * 以便将结果输送到任意消费端（如 Minecraft 渲染缓冲或离线调试工具）。
 */
public class CubeRenderer implements ModelProcessor {

    private final QuadConsumer consumer;

    public CubeRenderer(QuadConsumer consumer) {
        this.consumer = Objects.requireNonNull(consumer, "QuadConsumer cannot be null");
    }

    @Override
    public void renderCube(Model model,
                           ModelBone bone,
                           ModelCube cube,
                           float textureWidth,
                           float textureHeight,
                           CoreMatrixStack stack) {
        List<RenderedQuad> quads;
        if (cube.hasQuads()) {
            quads = convertPrecomputedQuads(cube.getQuads(), stack);
        } else {
            quads = bakeQuads(cube, stack, textureWidth, textureHeight);
        }
        for (RenderedQuad quad : quads) {
            consumer.consume(model, bone, cube, quad);
        }
    }

    /**
     * 将立方体烘焙为世界空间下的四边形列表。
     */
    public static List<RenderedQuad> bakeQuads(ModelCube cube,
                                               CoreMatrixStack stack,
                                               float textureWidth,
                                               float textureHeight) {
        float safeTexWidth = textureWidth <= 0 ? 1.0f : textureWidth;
        float safeTexHeight = textureHeight <= 0 ? 1.0f : textureHeight;

        CubeData data = generateCubeData(cube, safeTexWidth, safeTexHeight);
        if (cube.isMirror()) {
            applyMirror(data, true);
        }
        float inflateAmount = cube.getInflateAmount();
        if (inflateAmount != 0f) {
            applyInflate(data, inflateAmount);
        }

        return bakeQuads(data, stack);
    }

    private static List<RenderedQuad> convertPrecomputedQuads(List<org.mybad.core.data.ModelQuad> precomputed,
                                                              CoreMatrixStack stack) {
        List<RenderedQuad> quads = new ArrayList<>(precomputed.size());
        for (org.mybad.core.data.ModelQuad quad : precomputed) {
            float[][] positions = new float[4][3];
            float[][] uvs = new float[4][2];
            float[][] normals = new float[4][3];

            float[] normal = new float[]{quad.normalX, quad.normalY, quad.normalZ};
            stack.transformNormal(normal);

            for (int i = 0; i < quad.vertices.length; i++) {
                org.mybad.core.data.ModelVertex vertex = quad.vertices[i];
                float[] position = new float[]{vertex.x, vertex.y, vertex.z};
                stack.transform(position);
                positions[i] = position;

                uvs[i] = new float[]{vertex.u, vertex.v};
                normals[i] = Arrays.copyOf(normal, 3);
            }

            quads.add(new RenderedQuad(quad.direction.ordinal(), positions, normals, uvs));
        }
        return quads;
    }

    private static List<RenderedQuad> bakeQuads(CubeData data, CoreMatrixStack stack) {
        List<RenderedQuad> quads = new ArrayList<>(6);
        float[][] transformedVertices = new float[8][3];
        for (int i = 0; i < data.vertices.length; i++) {
            float[] copy = Arrays.copyOf(data.vertices[i], 3);
            stack.transform(copy);
            transformedVertices[i] = copy;
        }

        for (int faceIndex = 0; faceIndex < data.faces.length; faceIndex++) {
            int[] face = data.faces[faceIndex];
            float[][] positions = new float[4][3];
            float[][] uvs = new float[4][2];
            for (int v = 0; v < face.length; v++) {
                int vertexIndex = face[v];
                positions[v] = Arrays.copyOf(transformedVertices[vertexIndex], 3);
                uvs[v] = Arrays.copyOf(data.texCoords[vertexIndex], 2);
            }

            float[] normal = Arrays.copyOf(data.normals[faceIndex], 3);
            stack.transformNormal(normal);
            float[][] normals = new float[4][3];
            for (int i = 0; i < normals.length; i++) {
                normals[i] = Arrays.copyOf(normal, 3);
            }

            quads.add(new RenderedQuad(faceIndex, positions, normals, uvs));
        }

        return quads;
    }

    /**
     * 立方体基础几何数据，包含局部顶点以及面信息。
     */
    public static class CubeData {
        public float[][] vertices;
        public float[][] normals;
        public float[][] texCoords;
        public int[][] faces;

        public CubeData() {
            this.vertices = new float[8][3];
            this.normals = new float[6][3];
            this.texCoords = new float[8][2];
            this.faces = new int[6][4];
        }
    }

    /**
     * 渲染用四边形描述。
     */
    public static class RenderedQuad {
        private final int faceIndex;
        private final float[][] positions;
        private final float[][] normals;
        private final float[][] uvs;

        public RenderedQuad(int faceIndex, float[][] positions, float[][] normals, float[][] uvs) {
            this.faceIndex = faceIndex;
            this.positions = positions;
            this.normals = normals;
            this.uvs = uvs;
        }

        public int getFaceIndex() {
            return faceIndex;
        }

        public float[][] getPositions() {
            return positions;
        }

        public float[][] getNormals() {
            return normals;
        }

        public float[][] getUvs() {
            return uvs;
        }
    }

    @FunctionalInterface
    public interface QuadConsumer {
        void consume(Model model, ModelBone bone, ModelCube cube, RenderedQuad quad);
    }

    /**
     * 生成局部空间的立方体数据。
     */
    public static CubeData generateCubeData(ModelCube cube, float textureWidth, float textureHeight) {
        CubeData data = new CubeData();

        float[] size = cube.getSize();
        float width = size[0];
        float height = size[1];
        float depth = size[2];
        generateVertices(data, width, height, depth);
        generateNormals(data);
        generateFaces(data);
        generateTexCoords(data, cube, textureWidth, textureHeight);

        return data;
    }

    private static void generateVertices(CubeData data, float width, float height, float depth) {
        float w = width / 2f;
        float h = height / 2f;
        float d = depth / 2f;

        data.vertices[0] = new float[]{-w, -h, -d};
        data.vertices[1] = new float[]{w, -h, -d};
        data.vertices[2] = new float[]{w, -h, d};
        data.vertices[3] = new float[]{-w, -h, d};

        data.vertices[4] = new float[]{-w, h, -d};
        data.vertices[5] = new float[]{w, h, -d};
        data.vertices[6] = new float[]{w, h, d};
        data.vertices[7] = new float[]{-w, h, d};
    }

    private static void generateNormals(CubeData data) {
        data.normals[0] = new float[]{0, -1, 0};
        data.normals[1] = new float[]{0, 1, 0};
        data.normals[2] = new float[]{0, 0, -1};
        data.normals[3] = new float[]{0, 0, 1};
        data.normals[4] = new float[]{-1, 0, 0};
        data.normals[5] = new float[]{1, 0, 0};
    }

    private static void generateTexCoords(CubeData data, ModelCube cube, float textureWidth, float textureHeight) {
        org.mybad.core.data.UVMapping mappingInfo = cube.getUV();
        if (mappingInfo == null) {
            return;
        }

        if (!mappingInfo.isPerFace()) {
            float[] boxUV = mappingInfo.getBoxUV();
            if (boxUV == null) {
                return;
            }
            float[] size = cube.getSize();
            mappingInfo.setupBoxUV(size[0], size[1], size[2], cube.isMirror());
        }

        String[] faces = {"down", "up", "north", "south", "west", "east"};
        for (int faceIndex = 0; faceIndex < faces.length; faceIndex++) {
            float[] faceUV = mappingInfo.getFaceUV(faces[faceIndex]);
            if (faceUV == null || faceUV.length < 4) {
                continue;
            }

            float u1 = faceUV[0] / textureWidth;
            float v1 = faceUV[1] / textureHeight;
            float u2 = faceUV[2] / textureWidth;
            float v2 = faceUV[3] / textureHeight;

            int[] face = data.faces[faceIndex];
            if (faceIndex == 0) {
                data.texCoords[face[0]] = new float[]{u2, v1};
                data.texCoords[face[1]] = new float[]{u1, v1};
                data.texCoords[face[2]] = new float[]{u1, v2};
                data.texCoords[face[3]] = new float[]{u2, v2};
            } else {
                data.texCoords[face[0]] = new float[]{u1, v2};
                data.texCoords[face[1]] = new float[]{u2, v2};
                data.texCoords[face[2]] = new float[]{u2, v1};
                data.texCoords[face[3]] = new float[]{u1, v1};
            }
        }
    }

    private static void generateFaces(CubeData data) {
        data.faces[0] = new int[]{0, 3, 2, 1};
        data.faces[1] = new int[]{4, 5, 6, 7};
        data.faces[2] = new int[]{4, 0, 1, 5};
        data.faces[3] = new int[]{3, 7, 6, 2};
        data.faces[4] = new int[]{0, 4, 7, 3};
        data.faces[5] = new int[]{1, 2, 6, 5};
    }

    public static void applyMirror(CubeData data, boolean mirror) {
        if (!mirror) {
            return;
        }
        for (float[] vertex : data.vertices) {
            vertex[0] = -vertex[0];
        }
        for (float[] normal : data.normals) {
            normal[0] = -normal[0];
        }
        for (int i = 0; i < data.faces.length; i++) {
            int[] face = data.faces[i];
            data.faces[i] = new int[]{face[0], face[3], face[2], face[1]};
        }
    }

    public static void applyInflate(CubeData data, float inflateAmount) {
        if (inflateAmount == 0) {
            return;
        }
        for (float[] vertex : data.vertices) {
            for (int i = 0; i < vertex.length; i++) {
                vertex[i] += vertex[i] >= 0 ? inflateAmount : -inflateAmount;
            }
        }
    }

    public static float[] calculateBounds(float[] size) {
        float w = size[0] / 2f;
        float h = size[1] / 2f;
        float d = size[2] / 2f;
        return new float[]{-w, -h, -d, w, h, d};
    }
}
