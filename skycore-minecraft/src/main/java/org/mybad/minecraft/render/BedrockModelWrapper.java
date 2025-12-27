package org.mybad.minecraft.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;
import org.mybad.core.data.Model;
import org.mybad.core.data.ModelBone;
import org.mybad.core.data.ModelCube;
import org.mybad.core.render.CubeRenderer;
import org.mybad.core.render.ModelRenderer;
import org.mybad.core.render.MatrixStack;

import java.util.List;

/**
 * Bedrock 模型包装器
 * 参考 Chameleon 的 ChameleonRenderer 和 HammerAnimations 的 BedrockModelWrapper
 *
 * 特性：
 * - 静态 MatrixStack 和 CubeRenderer 实例，减少对象创建
 * - Chameleon 风格的骨骼变换顺序
 * - 预计算顶点（模型加载时生成四边形）
 * - 批量渲染
 */
@SideOnly(Side.CLIENT)
public class BedrockModelWrapper {

    private final CubeRenderer cubeRenderer;
    private final BufferingQuadConsumer quadConsumer;

    /** 模型数据 */
    private final Model model;

    /** 动画播放器 */
    private AnimationPlayer animationPlayer;

    /** 纹理位置 */
    private final ResourceLocation texture;

    /** 纹理尺寸 */
    private final int textureWidth;
    private final int textureHeight;

    /** 上一次更新时间 */
    private long lastUpdateTime;

    /** 上一次调试输出时间 */
    private long lastDebugOutputTime;

    /** 四边形是否已生成 */
    private boolean quadsGenerated;
    private boolean autoYOffset = false;
    private float modelYOffset;
    private boolean modelYOffsetReady;

    public BedrockModelWrapper(Model model, Animation animation, ResourceLocation texture) {
        this.model = model;
        this.texture = texture;

        // 获取纹理尺寸
        int texWidth = 64;
        int texHeight = 64;
        try {
            String tw = model.getTextureWidth();
            String th = model.getTextureHeight();
            if (tw != null && !tw.isEmpty()) {
                texWidth = Integer.parseInt(tw);
            }
            if (th != null && !th.isEmpty()) {
                texHeight = Integer.parseInt(th);
            }
        } catch (NumberFormatException ignored) {}

        this.textureWidth = texWidth;
        this.textureHeight = texHeight;

        // 创建动画播放器
        if (animation != null) {
            this.animationPlayer = new AnimationPlayer(animation);
            this.animationPlayer.play();
        }

        this.lastUpdateTime = System.currentTimeMillis();
        this.quadsGenerated = false;
        this.quadConsumer = new BufferingQuadConsumer();
        this.cubeRenderer = new CubeRenderer(quadConsumer);

        // 预生成四边形
        generateAllQuads();

        // 调试输出
        int boneCount = model.getBones().size();
        int cubeCount = countCubes();
        int quadCount = countQuads();
        System.out.println("[SkyCore DEBUG] 模型加载: bones=" + boneCount + ", cubes=" + cubeCount + ", quads=" + quadCount + ", texture=" + textureWidth + "x" + textureHeight);

        // 输出第一个 cube 的 UV 信息用于调试
        debugFirstCubeUV();
    }

    /**
     * 调试输出第一个 cube 的 UV 信息
     */
    private void debugFirstCubeUV() {
        for (ModelBone bone : model.getBones()) {
            for (ModelCube cube : bone.getCubes()) {
                if (!cube.getQuads().isEmpty()) {
                    System.out.println("[SkyCore DEBUG] 第一个 Cube UV 信息:");
                    System.out.println("  origin=" + java.util.Arrays.toString(cube.getOrigin()));
                    System.out.println("  size=" + java.util.Arrays.toString(cube.getSize()));
                    for (org.mybad.core.data.ModelQuad quad : cube.getQuads()) {
                        System.out.println("  Quad(" + quad.direction + "): vertices[0].uv=(" +
                            quad.vertices[0].u + ", " + quad.vertices[0].v + ")");
                    }
                    return;  // 只输出第一个
                }
            }
        }
    }

    private int countQuads() {
        int count = 0;
        for (ModelBone bone : model.getBones()) {
            count += countQuadsRecursive(bone);
        }
        return count;
    }

    private int countQuadsRecursive(ModelBone bone) {
        int count = 0;
        for (ModelCube cube : bone.getCubes()) {
            count += cube.getQuads().size();
        }
        for (ModelBone child : bone.getChildren()) {
            count += countQuadsRecursive(child);
        }
        return count;
    }

    private int countCubes() {
        int count = 0;
        for (ModelBone bone : model.getBones()) {
            count += countCubesRecursive(bone);
        }
        return count;
    }

    private int countCubesRecursive(ModelBone bone) {
        int count = bone.getCubes().size();
        for (ModelBone child : bone.getChildren()) {
            count += countCubesRecursive(child);
        }
        return count;
    }

    /**
     * 为模型中所有立方体生成四边形
     */
    private void generateAllQuads() {
        if (quadsGenerated) {
            return;
        }

        for (ModelBone bone : model.getBones()) {
            generateQuadsForBone(bone);
        }

        quadsGenerated = true;
    }

    /**
     * 递归为骨骼及其子骨骼的立方体生成四边形
     */
    private void generateQuadsForBone(ModelBone bone) {
        for (ModelCube cube : bone.getCubes()) {
            if (!cube.hasQuads()) {
                cube.generateQuads(textureWidth, textureHeight);
            }
        }

        for (ModelBone child : bone.getChildren()) {
            generateQuadsForBone(child);
        }
    }

    /**
     * 渲染模型
     */
    public void render(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        // 更新动画
        updateAnimation();

        // 应用动画到模型
        if (animationPlayer != null) {
            animationPlayer.apply(model);
        }

        // 调试：每帧输出一次（限制频率）
        long now = System.currentTimeMillis();
        boolean debugThisFrame = now - lastDebugOutputTime > 2000;
        if (debugThisFrame) {
            lastDebugOutputTime = now;
            System.out.println("[SkyCore DEBUG] ========== 渲染帧 ==========");
            System.out.println("[SkyCore DEBUG] 纹理: " + texture);
            System.out.println("[SkyCore DEBUG] 位置: (" + x + ", " + y + ", " + z + ")");
            System.out.println("[SkyCore DEBUG] 朝向: " + entityYaw);

            if (entity != null) {
                System.out.println("[SkyCore DEBUG] 实体世界坐标: (" + entity.posX + ", " + entity.posY + ", " + entity.posZ + ")");
            }

            Entity view = Minecraft.getMinecraft().getRenderViewEntity();
            if (view != null) {
                double camX = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
                double camY = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
                double camZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;
                System.out.println("[SkyCore DEBUG] 相机世界坐标: (" + camX + ", " + camY + ", " + camZ + ")");
                if (entity != null) {
                    double dx = entity.posX - camX;
                    double dy = entity.posY - camY;
                    double dz = entity.posZ - camZ;
                    System.out.println("[SkyCore DEBUG] 实体-相机偏移: (" + dx + ", " + dy + ", " + dz + ")");
                }
            }
        }

        // 绑定纹理
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        // 设置 OpenGL 状态
        if (FORCE_DISABLE_CULL) {
            GlStateManager.disableCull();
        } else {
            GlStateManager.enableCull();
        }
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();
        if (FORCE_DISABLE_LIGHTING) {
            GlStateManager.disableLighting();
        } else {
            GlStateManager.enableLighting();
        }

        GlStateManager.pushMatrix();
        float yOffset = modelYOffsetReady ? modelYOffset : 0.0f;
        GlStateManager.translate((float) x, (float) y + yOffset, (float) z);


        if (entity != null) {
            GlStateManager.rotate(180.0F - entityYaw, 0.0F, 1.0F, 0.0F);
        }

        // 获取光照值
        int lightX = (int) OpenGlHelper.lastBrightnessX;
        int lightY = (int) OpenGlHelper.lastBrightnessY;
        if (FORCE_FULL_BRIGHT) {
            lightX = 240;
            lightY = 240;
        }
        // 设置 lightmap 纹理坐标
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) lightX, (float) lightY);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        beginBatch(buffer);
        quadConsumer.begin(buffer, lightX, lightY);

        ModelRenderer.RenderOptions options = ModelRenderer.RenderOptions.builder()
            .matrixStack(new MatrixStack())
            .textureWidth(textureWidth)
            .textureHeight(textureHeight)
            .applyConstraints(true)
            .build();

        ModelRenderer.render(model, cubeRenderer, options);

        if (debugThisFrame) {
            quadConsumer.debugPrintBounds();
        }

        quadConsumer.end();
        Tessellator.getInstance().draw();

        GlStateManager.popMatrix();

        // 恢复 OpenGL 状态
        GlStateManager.disableBlend();
        GlStateManager.disableRescaleNormal();
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
    }

    /**
     * 更新动画
     */
    private void updateAnimation() {
        if (animationPlayer == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0F;
        lastUpdateTime = currentTime;

        // 限制 deltaTime 防止跳帧
        if (deltaTime > 0.1F) {
            deltaTime = 0.1F;
        }

        animationPlayer.update(deltaTime);
    }

    /**
     * 设置动画
     */
    public void setAnimation(Animation animation) {
        if (animation != null) {
            this.animationPlayer = new AnimationPlayer(animation);
            this.animationPlayer.play();
        } else {
            this.animationPlayer = null;
        }
    }

    /**
     * 重新开始当前动画
     */
    public void restartAnimation() {
        if (animationPlayer != null) {
            animationPlayer.restart();
        }
    }

    /**
     * 获取动画播放器
     */
    public AnimationPlayer getAnimationPlayer() {
        return animationPlayer;
    }

    /**
     * 获取模型
     */
    public Model getModel() {
        return model;
    }

    /**
     * 获取纹理
     */
    public ResourceLocation getTexture() {
        return texture;
    }

    /**
     * 重新生成四边形
     */
    public void regenerateQuads() {
        for (ModelBone bone : model.getBones()) {
            clearQuadsForBone(bone);
        }

        quadsGenerated = false;
        generateAllQuads();
    }

    private void computeStaticYOffset() {
        if (!autoYOffset) {
            return;
        }
        BoundsConsumer bounds = new BoundsConsumer();
        CubeRenderer boundsRenderer = new CubeRenderer(bounds);
        ModelRenderer.RenderOptions options = ModelRenderer.RenderOptions.builder()
            .matrixStack(new MatrixStack())
            .textureWidth(textureWidth)
            .textureHeight(textureHeight)
            .applyConstraints(true)
            .build();
        ModelRenderer.render(model, boundsRenderer, options);
        Float minY = bounds.getMinY();
        if (minY != null) {
            modelYOffset = -minY;
            modelYOffsetReady = true;
            System.out.println("[SkyCore DEBUG] 绑定姿态地面偏移: " + modelYOffset);
        }
    }

    /**
     * 递归清除骨骼的四边形
     */
    private void clearQuadsForBone(ModelBone bone) {
        for (ModelCube cube : bone.getCubes()) {
            cube.getQuads().clear();
        }
        for (ModelBone child : bone.getChildren()) {
            clearQuadsForBone(child);
        }
    }

    private static final boolean LIGHTMAP_SWAP = false;
    private static final boolean FORCE_DISABLE_CULL = true;
    private static final boolean FORCE_FULL_BRIGHT = true;
    private static final boolean FORCE_FLAT_NORMAL = true;
    private static final boolean FORCE_DISABLE_LIGHTING = true;

    private static void beginBatch(BufferBuilder buffer) {
        buffer.begin(org.lwjgl.opengl.GL11.GL_QUADS, VertexFormatCompat.getFormat(true, true, true, true));
    }

    /**
     * 将 core 渲染输出写入 Minecraft BufferBuilder。
     */
    private static class BufferingQuadConsumer implements CubeRenderer.QuadConsumer {
        private BufferBuilder buffer;
        private int lightX;
        private int lightY;
        private float r = 1.0f;
        private float g = 1.0f;
        private float b = 1.0f;
        private float a = 1.0f;
        private float minX;
        private float minY;
        private float minZ;
        private float maxX;
        private float maxY;
        private float maxZ;
        private int vertexCount;
        private boolean hasBounds;

        void begin(BufferBuilder buffer, int lightX, int lightY) {
            this.buffer = buffer;
            this.lightX = lightX;
            this.lightY = lightY;
            this.vertexCount = 0;
            this.hasBounds = false;
        }

        void end() {
            this.buffer = null;
        }

        void debugPrintBounds() {
            if (!hasBounds) {
                System.out.println("[SkyCore DEBUG] QuadBounds: <empty>");
                return;
            }
            float sizeX = maxX - minX;
            float sizeY = maxY - minY;
            float sizeZ = maxZ - minZ;
            System.out.println("[SkyCore DEBUG] QuadBounds: min=(" + minX + ", " + minY + ", " + minZ + ") max=(" + maxX + ", " + maxY + ", " + maxZ + ") size=(" + sizeX + ", " + sizeY + ", " + sizeZ + ") vertices=" + vertexCount);
        }

        Float getMinY() {
            return hasBounds ? minY : null;
        }

        @Override
        public void consume(Model model, ModelBone bone, ModelCube cube, CubeRenderer.RenderedQuad quad) {
            if (buffer == null) {
                return;
            }
            float[][] positions = quad.getPositions();
            float[][] uvs = quad.getUvs();
            float[][] normals = quad.getNormals();
            float[] cubeSize = cube.getSize();

            for (int i = 0; i < positions.length; i++) {
                float[] pos = positions[i];
                float[] uv = uvs[i];
                float[] normal = normals[i];

                if (!hasBounds) {
                    minX = maxX = pos[0];
                    minY = maxY = pos[1];
                    minZ = maxZ = pos[2];
                    hasBounds = true;
                } else {
                    if (pos[0] < minX) minX = pos[0];
                    if (pos[1] < minY) minY = pos[1];
                    if (pos[2] < minZ) minZ = pos[2];
                    if (pos[0] > maxX) maxX = pos[0];
                    if (pos[1] > maxY) maxY = pos[1];
                    if (pos[2] > maxZ) maxZ = pos[2];
                }
                vertexCount++;

                // Chameleon 风格：0 尺寸面法线修正，避免全黑
                if (normal[0] < 0 && (cubeSize[1] == 0 || cubeSize[2] == 0)) normal[0] *= -1;
                if (normal[1] < 0 && (cubeSize[0] == 0 || cubeSize[2] == 0)) normal[1] *= -1;
                if (normal[2] < 0 && (cubeSize[0] == 0 || cubeSize[1] == 0)) normal[2] *= -1;

                float nx = normal[0];
                float ny = normal[1];
                float nz = normal[2];
                if (FORCE_FLAT_NORMAL) {
                    nx = 0f;
                    ny = 1f;
                    nz = 0f;
                }

                // VertexFormatCompat: pos -> color -> tex -> lightmap -> normal
                buffer.pos(pos[0], pos[1], pos[2])
                    .color(r, g, b, a)
                    .tex(uv[0], uv[1]);
                if (LIGHTMAP_SWAP) {
                    buffer.lightmap(lightY, lightX);
                } else {
                    buffer.lightmap(lightX, lightY);
                }
                buffer.normal(nx, ny, nz)
                    .endVertex();
            }
        }
    }

    private static class BoundsConsumer implements CubeRenderer.QuadConsumer {
        private boolean hasBounds;
        private float minY;

        @Override
        public void consume(Model model, ModelBone bone, ModelCube cube, CubeRenderer.RenderedQuad quad) {
            float[][] positions = quad.getPositions();
            for (float[] pos : positions) {
                if (!hasBounds) {
                    minY = pos[1];
                    hasBounds = true;
                } else if (pos[1] < minY) {
                    minY = pos[1];
                }
            }
        }

        Float getMinY() {
            return hasBounds ? minY : null;
        }
    }
}
