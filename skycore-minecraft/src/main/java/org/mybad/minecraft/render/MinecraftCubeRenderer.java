package org.mybad.minecraft.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.mybad.core.data.ModelCube;
import org.mybad.core.data.ModelQuad;
import org.mybad.core.data.ModelVertex;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.util.List;

/**
 * Minecraft 立方体渲染器
 * 参考 Chameleon 的 ChameleonCubeRenderer 实现
 *
 * 特性：
 * - 预分配临时向量，避免 GC
 * - 支持 0 尺寸面的法线修正
 * - 支持颜色和透明度
 */
@SideOnly(Side.CLIENT)
public class MinecraftCubeRenderer {

    /** 预分配的临时向量 (Chameleon 风格) */
    private final Vector3f normal = new Vector3f();
    private final Vector4f vertex = new Vector4f();

    /** 调试计数 */
    private static int debugCounter = 0;

    /** 当前渲染颜色 */
    private float r = 1.0f;
    private float g = 1.0f;
    private float b = 1.0f;
    private float a = 1.0f;

    public MinecraftCubeRenderer() {
    }

    /**
     * 设置渲染颜色
     */
    public void setColor(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    /**
     * 重置颜色为默认值
     */
    public void resetColor() {
        this.r = 1.0f;
        this.g = 1.0f;
        this.b = 1.0f;
        this.a = 1.0f;
    }

    /**
     * 渲染立方体 (Chameleon 风格)
     * 每个 cube 都进行 push/pop，应用 pivot 和旋转变换
     *
     * @param buffer 顶点缓冲
     * @param stack  矩阵栈
     * @param cube   立方体数据（应已调用 generateQuads）
     * @param lightX 光照 X（天空光）
     * @param lightY 光照 Y（方块光）
     */
    public void renderCube(BufferBuilder buffer, MatrixStack stack, ModelCube cube, int lightX, int lightY) {
        List<ModelQuad> quads = cube.getQuads();
        if (quads.isEmpty()) {
            return;
        }

        // Chameleon 风格：每个 cube 都 push/pop
        stack.push();
        stack.moveToCubePivot(cube.getPivot());
        stack.rotateCube(cube.getRotation());
        stack.moveBackFromCubePivot(cube.getPivot());

        float[] size = cube.getSize();

        for (ModelQuad quad : quads) {
            // 变换法线
            normal.set(quad.normalX, quad.normalY, quad.normalZ);
            stack.getNormalMatrix().transform(normal);

            // Chameleon 的 0 尺寸面法线修正
            if (normal.x < 0 && (size[1] == 0 || size[2] == 0)) normal.x *= -1;
            if (normal.y < 0 && (size[0] == 0 || size[2] == 0)) normal.y *= -1;
            if (normal.z < 0 && (size[0] == 0 || size[1] == 0)) normal.z *= -1;

            // 渲染 4 个顶点
            for (ModelVertex v : quad.vertices) {
                vertex.set(v.x, v.y, v.z, 1.0f);
                stack.getModelMatrix().transform(vertex);

                // 调试输出（每10000帧输出一次）
                if (debugCounter++ % 10000 == 0) {
                    System.out.println("[SkyCore DEBUG] 顶点: (" + v.x + ", " + v.y + ", " + v.z +
                        ") -> (" + vertex.x + ", " + vertex.y + ", " + vertex.z + ") UV=(" + v.u + ", " + v.v + ")");
                }

                if (DEBUG_SIMPLE_FORMAT) {
                    // 简单格式: pos, tex, color
                    buffer.pos(vertex.x, vertex.y, vertex.z)
                            .tex(v.u, v.v)
                            .color(r, g, b, a)
                            .endVertex();
                } else {
                    // BLOCK 格式: pos, color, tex, lightmap, normal
                    buffer.pos(vertex.x, vertex.y, vertex.z)
                            .color(r, g, b, a)
                            .tex(v.u, v.v)
                            .lightmap(lightX, lightY)
                            .normal(normal.x, normal.y, normal.z)
                            .endVertex();
                }

                vertexCount++;
            }
        }

        stack.pop();
    }

    /**
     * 渲染立方体（使用组合光照值）
     */
    public void renderCube(BufferBuilder buffer, MatrixStack stack, ModelCube cube, int combinedLight) {
        int lightX = (int) OpenGlHelper.lastBrightnessX;
        int lightY = (int) OpenGlHelper.lastBrightnessY;

        // 如果提供了组合光照值，分解它
        if (combinedLight != 0) {
            lightY = combinedLight & 0xFFFF;
            lightX = (combinedLight >> 16) & 0xFFFF;
        }

        renderCube(buffer, stack, cube, lightX, lightY);
    }

    /** 顶点计数器用于调试 */
    private static int vertexCount = 0;

    /** 调试模式：使用简单顶点格式 */
    private static final boolean DEBUG_SIMPLE_FORMAT = true;

    /**
     * 开始批量渲染
     * 调试模式使用简单格式 POSITION_TEX_COLOR
     * 正常模式使用 BLOCK 格式（position, color, tex, lightmap, normal）
     */
    public static void beginBatch(BufferBuilder buffer) {
        if (DEBUG_SIMPLE_FORMAT) {
            // 简单格式：位置 + 纹理 + 颜色（不需要lightmap和normal）
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        } else {
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        }
        vertexCount = 0;
    }

    /**
     * 结束批量渲染
     */
    public static void endBatch() {
        // 调试输出（每秒一次）
        if (debugCounter % 60 == 0) {
            System.out.println("[SkyCore DEBUG] 批量渲染结束: 顶点数=" + vertexCount);
        }
        Tessellator.getInstance().draw();
    }

    /**
     * 增加顶点计数
     */
    public static void incrementVertexCount() {
        vertexCount++;
    }
}
