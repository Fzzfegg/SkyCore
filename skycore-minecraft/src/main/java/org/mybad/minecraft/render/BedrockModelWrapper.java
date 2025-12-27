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
import org.mybad.core.data.ModelQuad;
import org.mybad.core.render.CubeRenderer;
import org.mybad.core.render.ModelRenderer;
import org.mybad.core.render.MatrixStack;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;

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

    /** 四边形是否已生成 */
    private boolean quadsGenerated;
    private boolean autoYOffset = false;
    private float modelYOffset;
    private boolean modelYOffsetReady;
    private final Map<ModelBone, Integer> boneIndexMap;
    private final List<ModelBone> rootBones;
    private SkinnedMesh skinnedMesh;
    private FloatBuffer boneMatrixBuffer;
    private float[] boneMatrices;
    private final MatrixStack boneMatrixStack = new MatrixStack();
    private boolean gpuSkinningReady;

    /** 是否启用背面剔除 */
    private final boolean enableCull;

    public BedrockModelWrapper(Model model, Animation animation, ResourceLocation texture) {
        this(model, animation, texture, true);
    }

    public BedrockModelWrapper(Model model, Animation animation, ResourceLocation texture, boolean enableCull) {
        this.enableCull = enableCull;
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

        this.boneIndexMap = buildBoneIndexMap(model);
        this.rootBones = collectRootBones(model.getBones());

        // 预生成四边形
        generateAllQuads();

        initBoneMatrices();
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

        // 绑定纹理
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        // 设置 OpenGL 状态 - 根据配置控制背面剔除
        if (enableCull) {
            GlStateManager.enableCull();
        } else {
            GlStateManager.disableCull();
        }
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableColorMaterial();

        GlStateManager.pushMatrix();
        float yOffset = modelYOffsetReady ? modelYOffset : 0.0f;
        GlStateManager.translate((float) x, (float) y + yOffset, (float) z);


        if (entity != null) {
            GlStateManager.rotate(180.0F - entityYaw, 0.0F, 1.0F, 0.0F);
        }

        // 获取实际光照值
        int lightX = (int) OpenGlHelper.lastBrightnessX;
        int lightY = (int) OpenGlHelper.lastBrightnessY;
        // 设置 lightmap 纹理坐标
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) lightX, (float) lightY);

        if (ensureGpuSkinningReady()) {
            updateBoneMatrices();
            skinnedMesh.updateJointMatrices(boneMatrixBuffer);
            skinnedMesh.runSkinningPass();
            skinnedMesh.draw();
        } else {
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

            quadConsumer.end();
            Tessellator.getInstance().draw();
        }

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
        }
    }

    private static Map<ModelBone, Integer> buildBoneIndexMap(Model model) {
        Map<ModelBone, Integer> map = new HashMap<>();
        List<ModelBone> bones = model.getBones();
        for (int i = 0; i < bones.size(); i++) {
            map.put(bones.get(i), i);
        }
        return map;
    }

    private static List<ModelBone> collectRootBones(List<ModelBone> bones) {
        List<ModelBone> roots = new ArrayList<>();
        for (ModelBone bone : bones) {
            if (bone.getParent() == null) {
                roots.add(bone);
            }
        }
        return roots;
    }

    private void initBoneMatrices() {
        int boneCount = model.getBones().size();
        if (boneCount <= 0) {
            return;
        }
        boneMatrices = new float[boneCount * 16];
        boneMatrixBuffer = BufferUtils.createFloatBuffer(boneCount * 16);
    }

    private boolean ensureGpuSkinningReady() {
        if (gpuSkinningReady) {
            return true;
        }
        if (!GpuSkinningSupport.isGpuSkinningAvailable()) {
            return false;
        }
        skinnedMesh = buildSkinnedMesh();
        gpuSkinningReady = skinnedMesh != null;
        return gpuSkinningReady;
    }

    private SkinnedMesh buildSkinnedMesh() {
        int quadCount = countQuads();
        if (quadCount <= 0) {
            return null;
        }
        int vertexCount = quadCount * 6;
        FloatBuffer buffer = SkinnedMesh.allocateInputBuffer(vertexCount);
        int vertexIndex = 0;

        for (ModelBone bone : model.getBones()) {
            if (bone.isNeverRender()) {
                continue;
            }
            Integer boneIndex = boneIndexMap.get(bone);
            if (boneIndex == null) {
                continue;
            }
            for (ModelCube cube : bone.getCubes()) {
                if (!cube.hasQuads()) {
                    cube.generateQuads(textureWidth, textureHeight);
                }
                MatrixStack cubeRotation = buildCubeRotationStack(cube);
                for (ModelQuad quad : cube.getQuads()) {
                    int[] order = new int[]{0, 1, 2, 2, 3, 0};
                    for (int idx : order) {
                        float[] pos = new float[]{quad.vertices[idx].x, quad.vertices[idx].y, quad.vertices[idx].z};
                        float[] normal = new float[]{quad.normalX, quad.normalY, quad.normalZ};
                        if (cubeRotation != null) {
                            cubeRotation.transform(pos);
                            cubeRotation.transformNormal(normal);
                        }

                        buffer.put(pos[0]).put(pos[1]).put(pos[2]);
                        buffer.put(quad.vertices[idx].u).put(quad.vertices[idx].v);
                        buffer.put(normal[0]).put(normal[1]).put(normal[2]);

                        buffer.put(boneIndex.floatValue()).put(0f).put(0f).put(0f);
                        buffer.put(1f).put(0f).put(0f).put(0f);
                        buffer.put((float) vertexIndex);
                        vertexIndex++;
                    }
                }
            }
        }

        buffer.flip();
        return new SkinnedMesh(buffer, vertexIndex, model.getBones().size());
    }

    private void updateBoneMatrices() {
        if (boneMatrices == null || boneMatrixBuffer == null) {
            return;
        }

        applyConstraintsForGpu();

        boneMatrixStack.loadIdentity();
        for (ModelBone bone : rootBones) {
            fillBoneMatricesRecursive(bone, boneMatrixStack);
        }

        boneMatrixBuffer.clear();
        boneMatrixBuffer.put(boneMatrices, 0, boneMatrices.length);
        boneMatrixBuffer.flip();
    }

    private void fillBoneMatricesRecursive(ModelBone bone, MatrixStack stack) {
        stack.push();
        applyBoneTransform(bone, stack);

        Integer index = boneIndexMap.get(bone);
        if (index != null) {
            float[] current = stack.getCurrentMatrix();
            System.arraycopy(current, 0, boneMatrices, index * 16, 16);
        }

        for (ModelBone child : bone.getChildren()) {
            fillBoneMatricesRecursive(child, stack);
        }

        stack.pop();
    }

    private void applyConstraintsForGpu() {
        if (model.getConstraints().isEmpty()) {
            return;
        }
        for (org.mybad.core.constraint.Constraint constraint : model.getConstraints()) {
            ModelBone target = model.getBone(constraint.getTargetBone());
            ModelBone source = model.getBone(constraint.getSourceBone());
            if (target != null && source != null) {
                constraint.apply(target, source);
            }
        }
    }

    private static MatrixStack buildCubeRotationStack(ModelCube cube) {
        if (!cube.hasRotation()) {
            return null;
        }
        MatrixStack stack = new MatrixStack();
        float[] pivot = cube.getPivot();
        float pivotX = convertX(pivot[0]);
        float pivotY = convertY(pivot[1]);
        float pivotZ = convertZ(pivot[2]);

        stack.translate(pivotX, pivotY, pivotZ);
        float[] rotation = cube.getRotation();
        stack.rotateEuler(
            convertRotation(rotation[0], true),
            convertRotation(rotation[1], true),
            convertRotation(rotation[2], false)
        );
        stack.translate(-pivotX, -pivotY, -pivotZ);
        return stack;
    }

    private static void applyBoneTransform(ModelBone bone, MatrixStack stack) {
        float[] pivot = bone.getPivot();
        float pivotX = convertX(pivot[0]);
        float pivotY = convertY(pivot[1]);
        float pivotZ = convertZ(pivot[2]);

        stack.translate(pivotX, pivotY, pivotZ);

        float[] rotation = bone.getRotation();
        if (rotation[0] != 0 || rotation[1] != 0 || rotation[2] != 0) {
            stack.rotateEuler(
                convertRotation(rotation[0], true),
                convertRotation(rotation[1], true),
                convertRotation(rotation[2], false)
            );
        }

        float[] size = bone.getSize();
        if (size[0] != 1 || size[1] != 1 || size[2] != 1) {
            stack.scale(size[0], size[1], size[2]);
        }

        stack.translate(-pivotX, -pivotY, -pivotZ);

        float[] position = bone.getPosition();
        float translateX = convertX(position[0]);
        float translateY = convertY(position[1]);
        float translateZ = convertZ(position[2]);

        if (translateX != 0 || translateY != 0 || translateZ != 0) {
            stack.translate(translateX, translateY, translateZ);
        }
    }

    private static final float PIXEL_SCALE = 1.0f / 16.0f;

    private static float convertX(float raw) {
        return -raw * PIXEL_SCALE;
    }

    private static float convertY(float raw) {
        return raw * PIXEL_SCALE;
    }

    private static float convertZ(float raw) {
        return raw * PIXEL_SCALE;
    }

    private static float convertRotation(float raw, boolean invert) {
        return invert ? -raw : raw;
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

                // VertexFormatCompat: pos -> color -> tex -> lightmap -> normal
                buffer.pos(pos[0], pos[1], pos[2])
                    .color(r, g, b, a)
                    .tex(uv[0], uv[1]);
          
                buffer.lightmap(lightX, lightY);
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
