package org.mybad.minecraft.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;
import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.core.data.Model;
import org.mybad.core.data.ModelBone;
import org.mybad.core.data.ModelCube;
import org.mybad.core.data.ModelQuad;
import org.mybad.core.data.ModelLocator;
import org.mybad.core.render.CoreMatrixStack;

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
 * - Chameleon 风格的骨骼变换顺序
 * - 预计算顶点（模型加载时生成四边形）
 * - 批量渲染
 */
@SideOnly(Side.CLIENT)
public class BedrockModelWrapper {

    /** 模型数据 */
    private final Model model;

    /** 动画播放器 */
    private AnimationPlayer animationPlayer;
    private AnimationPlayer activePlayer;
    private AnimationPlayer previousPlayer;
    private float primaryFadeTime;
    private float primaryFadeDuration = 0.12f;
    private final Map<Animation, AnimationPlayer> overlayPlayers = new HashMap<>();
    private List<EntityAnimationController.OverlayState> overlayStates = java.util.Collections.emptyList();

    /** 纹理位置 */
    private final ResourceLocation texture;
    private final ResourceLocation emissiveTexture;
    private float emissiveStrength = 1.0f;

    /** 纹理尺寸 */
    private final int textureWidth;
    private final int textureHeight;

    /** 上一次更新时间 */
    private long lastUpdateTime;

    /** 四边形是否已生成 */
    private boolean quadsGenerated;
    private float modelScale = 1.0f;
    private static final GeometryCache FALLBACK_GEOMETRY_CACHE = new GeometryCache();
    private final Map<ModelBone, Integer> boneIndexMap;
    private final List<ModelBone> rootBones;
    private final GeometryCache geometryCache;
    private final GeometryCache.Key geometryKey;
    private SkinnedMesh skinnedMesh;
    private SharedGeometry sharedGeometry;
    private boolean shaderAcquired;
    private FloatBuffer boneMatrixBuffer;
    private float[] boneMatrices;
    private final MatrixStack boneMatrixStack = new MatrixStack();
    private boolean gpuSkinningReady;

    /** 是否启用背面剔除 */
    private final boolean enableCull;

    public BedrockModelWrapper(Model model, Animation animation, ResourceLocation texture) {
        this(model, animation, texture, null, true, null, null);
    }

    public BedrockModelWrapper(Model model, Animation animation, ResourceLocation texture, boolean enableCull) {
        this(model, animation, texture, null, enableCull, null, null);
    }

    public BedrockModelWrapper(Model model, Animation animation, ResourceLocation texture, boolean enableCull, String modelId) {
        this(model, animation, texture, null, enableCull, modelId, null);
    }

    public BedrockModelWrapper(Model model, Animation animation, ResourceLocation texture, ResourceLocation emissiveTexture, boolean enableCull, String modelId, GeometryCache geometryCache) {
        this.enableCull = enableCull;
        Model baseModel = model;
        this.model = baseModel != null ? baseModel.createInstance() : null;
        this.texture = texture;
        this.emissiveTexture = emissiveTexture;

        // 获取纹理尺寸
        int texWidth = 64;
        int texHeight = 64;
        try {
            String tw = baseModel.getTextureWidth();
            String th = baseModel.getTextureHeight();
            if (tw != null && !tw.isEmpty()) {
                texWidth = Integer.parseInt(tw);
            }
            if (th != null && !th.isEmpty()) {
                texHeight = Integer.parseInt(th);
            }
        } catch (NumberFormatException ignored) {}

        this.textureWidth = texWidth;
        this.textureHeight = texHeight;
        this.geometryCache = geometryCache != null ? geometryCache : FALLBACK_GEOMETRY_CACHE;
        this.geometryKey = GeometryCache.key(normalizeModelId(modelId, model), textureWidth, textureHeight);

        // 创建动画播放器
        if (animation != null) {
            this.animationPlayer = new AnimationPlayer(animation);
            this.animationPlayer.play();
            this.activePlayer = this.animationPlayer;
        }

        this.lastUpdateTime = System.currentTimeMillis();
        this.quadsGenerated = false;

        this.boneIndexMap = buildBoneIndexMap(this.model);
        this.rootBones = collectRootBones(this.model.getBones());

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
        AnimationPlayer player = activePlayer;
        if (player != null || previousPlayer != null || !overlayStates.isEmpty()) {
            model.resetToBindPose();
            if (player != null) {
                player.apply(model);
            }
            float previousWeight = getPrimaryFadeWeight();
            if (previousPlayer != null && previousWeight > 0f) {
                previousPlayer.apply(model, previousWeight);
            }
            applyOverlays();
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
        GlStateManager.translate((float) x, (float) y, (float) z);
        if (modelScale != 1.0f) {
            GlStateManager.scale(modelScale, modelScale, modelScale);
        }


        if (entity != null) {
            GlStateManager.rotate(180.0F - entityYaw, 0.0F, 1.0F, 0.0F);
        }

        // 获取实际光照值
        int lightX = (int) OpenGlHelper.lastBrightnessX;
        int lightY = (int) OpenGlHelper.lastBrightnessY;
        // 设置 lightmap 纹理坐标
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) lightX, (float) lightY);

        boolean gpu = ensureGpuSkinningReady();
        if (!gpu) {
            GlStateManager.popMatrix();
            GlStateManager.disableBlend();
            GlStateManager.disableRescaleNormal();
            GlStateManager.enableDepth();
            GlStateManager.enableCull();
            return;
        }

        updateBoneMatrices();
        skinnedMesh.updateJointMatrices(boneMatrixBuffer);
        skinnedMesh.runSkinningPass();
        skinnedMesh.draw();

        if (emissiveTexture != null) {
            renderEmissivePass(lightX, lightY);
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
        if (animationPlayer == null && activePlayer == null && previousPlayer == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0F;
        lastUpdateTime = currentTime;

        // 限制 deltaTime 防止跳帧
        if (deltaTime > 0.1F) {
            deltaTime = 0.1F;
        }

        AnimationPlayer desiredPlayer = null;
        if (animationPlayer != null) {
            animationPlayer.update(deltaTime);
            desiredPlayer = animationPlayer;
        }

        if (desiredPlayer != activePlayer) {
            beginPrimaryTransition(desiredPlayer);
        }

        if (previousPlayer != null) {
            previousPlayer.update(deltaTime);
            primaryFadeTime += deltaTime;
            if (primaryFadeTime >= primaryFadeDuration) {
                previousPlayer = null;
            }
        }
    }

    private void applyOverlays() {
        if (overlayStates.isEmpty()) {
            return;
        }
        for (EntityAnimationController.OverlayState state : overlayStates) {
            if (state == null || state.weight <= 0f || state.animation == null) {
                continue;
            }
            AnimationPlayer player = overlayPlayers.get(state.animation);
            if (player == null) {
                player = new AnimationPlayer(state.animation);
                player.play();
                overlayPlayers.put(state.animation, player);
            }
            player.setCurrentTime(state.time);
            player.apply(model, state.weight);
        }
    }

    /**
     * 设置动画
     */
    public void setAnimation(Animation animation) {
        this.overlayPlayers.clear();
        this.overlayStates = java.util.Collections.emptyList();
        if (animation != null) {
            if (this.animationPlayer == null || this.animationPlayer.getAnimation() != animation) {
                this.animationPlayer = new AnimationPlayer(animation);
                this.animationPlayer.play();
            }
        } else {
            this.animationPlayer = null;
        }
        beginPrimaryTransition(this.animationPlayer);
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

    public AnimationPlayer getActiveAnimationPlayer() {
        return activePlayer != null ? activePlayer : animationPlayer;
    }

    public void setOverlayStates(List<EntityAnimationController.OverlayState> states) {
        if (states == null || states.isEmpty()) {
            overlayStates = java.util.Collections.emptyList();
            return;
        }
        overlayStates = new ArrayList<>(states);
    }

    public void clearOverlayStates() {
        overlayStates = java.util.Collections.emptyList();
    }

    /**
     * 获取模型
     */
    public Model getModel() {
        return model;
    }

    public float[] getLocatorPosition(String locatorName) {
        if (locatorName == null || locatorName.isEmpty() || model == null) {
            return null;
        }
        ModelLocator locator = model.getLocator(locatorName);
        if (locator == null) {
            return null;
        }
        applyConstraintsForGpu();
        float[] raw = locator.getPosition();
        float[] local = new float[]{
            convertX(raw[0]),
            convertY(raw[1]),
            convertZ(raw[2])
        };
        String boneName = locator.getAttachedBone();
        if (boneName == null || boneName.isEmpty()) {
            return local;
        }
        ModelBone bone = model.getBone(boneName);
        if (bone == null) {
            return local;
        }
        MatrixStack stack = new MatrixStack();
        applyBoneTransformRecursive(bone, stack);
        stack.transform(local);
        return local;
    }

    private void renderEmissivePass(int lightX, int lightY) {
        if (emissiveStrength <= 0f) {
            return;
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(emissiveTexture);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0f, 1.0f, 1.0f, emissiveStrength);
        GlStateManager.disableLighting();
        GlStateManager.disableColorMaterial();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);

        int fullBright = 240;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) fullBright, (float) fullBright);

        skinnedMesh.draw();

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) lightX, (float) lightY);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableColorMaterial();
        GlStateManager.enableLighting();
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
    }

    public void setPrimaryFadeDuration(float seconds) {
        if (Float.isNaN(seconds) || seconds < 0f) {
            return;
        }
        this.primaryFadeDuration = seconds;
    }

    public void setEmissiveStrength(float strength) {
        if (Float.isNaN(strength)) {
            return;
        }
        if (strength < 0f) {
            strength = 0f;
        } else if (strength > 1f) {
            strength = 1f;
        }
        this.emissiveStrength = strength;
    }

    public void setModelScale(float scale) {
        if (Float.isNaN(scale) || scale <= 0f) {
            return;
        }
        this.modelScale = scale;
    }

    public float getModelScale() {
        return modelScale;
    }

    private void beginPrimaryTransition(AnimationPlayer next) {
        if (next == activePlayer) {
            return;
        }
        if (primaryFadeDuration > 0f) {
            previousPlayer = activePlayer;
            primaryFadeTime = 0f;
        } else {
            previousPlayer = null;
            primaryFadeTime = primaryFadeDuration;
        }
        activePlayer = next;
    }

    private float getPrimaryFadeWeight() {
        if (previousPlayer == null || primaryFadeDuration <= 0f) {
            return 0f;
        }
        float t = primaryFadeTime / primaryFadeDuration;
        if (t >= 1f) {
            return 0f;
        }
        return 1f - t;
    }

    public void dispose() {
        if (skinnedMesh != null) {
            skinnedMesh.destroy();
            skinnedMesh = null;
        }
        releaseSharedGeometry();
        if (shaderAcquired) {
            GpuSkinningShader.release();
            shaderAcquired = false;
        }
        gpuSkinningReady = false;
        overlayPlayers.clear();
        overlayStates = java.util.Collections.emptyList();
        activePlayer = null;
    }

    public static void clearSharedResources() {
        FALLBACK_GEOMETRY_CACHE.clear();
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
        if (!shaderAcquired) {
            GpuSkinningShader.acquire();
            shaderAcquired = true;
        }
        if (sharedGeometry == null || sharedGeometry.isDestroyed()) {
            sharedGeometry = geometryCache.acquire(geometryKey, this::buildSharedGeometry);
        }
        if (sharedGeometry == null || sharedGeometry.isDestroyed()) {
            if (shaderAcquired) {
                GpuSkinningShader.release();
                shaderAcquired = false;
            }
            return false;
        }
        skinnedMesh = new SkinnedMesh(sharedGeometry, model.getBones().size());
        gpuSkinningReady = skinnedMesh != null;
        if (!gpuSkinningReady) {
            releaseSharedGeometry();
            if (shaderAcquired) {
                GpuSkinningShader.release();
                shaderAcquired = false;
            }
        }
        return gpuSkinningReady;
    }

    private void releaseSharedGeometry() {
        if (sharedGeometry == null) {
            return;
        }
        geometryCache.release(geometryKey, sharedGeometry);
        sharedGeometry = null;
    }

    private static String normalizeModelId(String modelId, Model model) {
        if (modelId != null && !modelId.isEmpty()) {
            return modelId;
        }
        String name = model != null ? model.getName() : null;
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return "unknown";
    }

    private SharedGeometry buildSharedGeometry() {
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
        return new SharedGeometry(buffer, vertexIndex);
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

    private static void applyBoneTransformRecursive(ModelBone bone, MatrixStack stack) {
        if (bone.getParent() != null) {
            applyBoneTransformRecursive(bone.getParent(), stack);
        }
        applyBoneTransform(bone, stack);
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
    
}
