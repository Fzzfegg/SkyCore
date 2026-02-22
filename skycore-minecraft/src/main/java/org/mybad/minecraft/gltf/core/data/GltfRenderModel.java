package org.mybad.minecraft.gltf.core.data;

import org.mybad.minecraft.gltf.GltfLog;
import org.mybad.minecraft.gltf.core.gl.SkinningSupport;
import java.nio.FloatBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

import org.joml.*;
import org.joml.Math;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL43;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;


public class GltfRenderModel {
    private static final AtomicInteger ACTIVE_INSTANCES = new AtomicInteger();
    private static final HashSet<String> setObj = new HashSet<String>();
    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);
    // Sort materials so opaque renders first, then translucent, with stable fallback ordering.
    private static final Comparator<DataMaterial> COMPARATOR_MATE = new Comparator<DataMaterial>() {

        @Override
        public int compare(DataMaterial o1, DataMaterial o2) {
            if (!o1.isTranslucent && o2.isTranslucent) {
                return -1;
            }
            // Render translucent materials after opaque ones (positive order).
            if (o1.isTranslucent && !o2.isTranslucent) {
                return 1;
            }
            // 同类型按其他规则排序（如深度）

            int depthOrder = Float.compare(o1.depthOffset, o2.depthOffset);
            if (depthOrder != 0) {
                return depthOrder;
            }

            String name1 = o1.name == null ? "" : o1.name;
            String name2 = o2.name == null ? "" : o2.name;
            return name1.compareTo(name2);
        }

    };

    public HashMap<String, NodeState> nodeStates = new HashMap<String, NodeState>();
    public NodeAnimationBlender animationCalBlender;
    public NodeAnimationMapper animationLoadMapper;

    public GltfDataModel geoModel;

    public GltfDataModel lastAniModel;
    public GltfDataModel aniModel;

    protected boolean initedNodeStates = false;
    protected int jointMatsBufferId = -1;
    private Matrix4f[] cpuJointMatrices;

    public float globalScale = 1.0f;
    private final int instanceId;
    private volatile String debugSourceId = "unknown";
    private volatile boolean cleaned;
    private ResourceLocation defaultBaseTexture;

    public static class NodeState {
        public Matrix4f mat = new Matrix4f();
    }

    public static class NodeAnimationBlender {
        public String name;

        public NodeAnimationBlender(String name) {
            this.name = name;
        }

        public void handle(DataNode node, Matrix4f mat) {

        }
    }
    
    public static class NodeAnimationMapper {
        public String name;

        public NodeAnimationMapper(String name) {
            this.name = name;
        }

        public void handle(GltfRenderModel model,GltfRenderModel other,String target) {

        }
    }

    public void setNodeAnimationCalBlender(NodeAnimationBlender blender) {
        animationCalBlender=blender;
    }
    
    public void setNodeAnimationLoadMapper(NodeAnimationMapper mapper) {
        animationLoadMapper=mapper;
    }

    public void setGlobalScale(float scale) {
        this.globalScale = scale;
    }

    public float getGlobalScale() {
        return this.globalScale;
    }

    public void setDefaultTexture(@Nullable ResourceLocation texture) {
        this.defaultBaseTexture = texture;
    }

    @Nullable
    public ResourceLocation getDefaultTexture() {
        return defaultBaseTexture;
    }

    public void cleanup() {
        if (cleaned) {
            return;
        }
        cleaned = true;
        cpuJointMatrices = null;
        if (jointMatsBufferId >= 0) {
            GL15.glDeleteBuffers(jointMatsBufferId);
            jointMatsBufferId = -1;
        }
    }

    public GltfRenderModel(GltfDataModel geoModel) {
        this.geoModel = geoModel;
        this.instanceId = ACTIVE_INSTANCES.incrementAndGet();
        if (GltfLog.LOGGER.isDebugEnabled()) {
            GltfLog.LOGGER.info("GltfRenderModel[{}] constructed (active={})", instanceId, ACTIVE_INSTANCES.get());
        }
    }

    public int getInstanceId() {
        return instanceId;
    }

    public void setDebugSourceId(String sourceId) {
        if (sourceId != null) {
            this.debugSourceId = sourceId;
        }
    }

    public String getDebugSourceId() {
        return debugSourceId;
    }

    public static int getActiveInstanceCount() {
        return ACTIVE_INSTANCES.get();
    }

    private Matrix4f[] ensureCpuJointMatrices() {
        if (cpuJointMatrices == null || cpuJointMatrices.length != geoModel.joints.size()) {
            cpuJointMatrices = new Matrix4f[geoModel.joints.size()];
            for (int i = 0; i < cpuJointMatrices.length; i++) {
                cpuJointMatrices[i] = new Matrix4f();
            }
        }
        return cpuJointMatrices;
    }

    private Matrix4f[] getCpuJointMatrices() {
        return cpuJointMatrices;
    }

    private void ensureNodeStatesInitialized() {
        if (!initedNodeStates) {
            geoModel.nodes.keySet().forEach(name -> nodeStates.put(name, new NodeState()));
            initedNodeStates = true;
        }
    }

    public void calculateAllNodePose(float time) {
        ensureNodeStatesInitialized();
        for (Entry<String, DataNode> entry : geoModel.rootNodes.entrySet()) {
            calculateNodeAndChildren(entry.getValue(), null, time);
        }
    }

    private void calculateNodeAndChildren(DataNode node, Matrix4f parent, float time) {
        Matrix4f matrix = composeNodeMatrix(node, time);

        if (animationCalBlender != null) {
            animationCalBlender.handle(node, matrix);
        }

        if (parent != null) {
            matrix.mulLocal(parent);
        }


        nodeStates.get(node.name).mat = matrix;
        for (String name : node.childlist) {
            calculateNodeAndChildren(geoModel.nodes.get(name), matrix, time);
        }
    }

    private Matrix4f composeNodeMatrix(DataNode node, float time) {
        Matrix4f matrix = new Matrix4f();
        DataAnimation animation = geoModel.animations.get(node.name);
        if (animation != null) {
            DataAnimation.Transform transform = animation.findTransform(time, node.pos, node.size, node.rot);
            matrix.translate(transform.pos.x, transform.pos.y, transform.pos.z);
            matrix.rotate(transform.rot);
            matrix.scale(transform.size.x, transform.size.y, transform.size.z);
        } else {
            matrix.translate(node.pos);
            matrix.rotate(node.rot);
            matrix.scale(node.size);
        }
        return matrix;
    }

    private void calculateAllNodePoseBlended(float baseTime, float targetTime, float weight) {
        ensureNodeStatesInitialized();
        for (Entry<String, DataNode> entry : geoModel.rootNodes.entrySet()) {
            calculateNodeAndChildrenBlended(entry.getValue(), null, baseTime, targetTime, weight);
        }
    }

    private void calculateNodeAndChildrenBlended(DataNode node, Matrix4f parent, float baseTime, float targetTime, float weight) {
        Matrix4f matrix = composeBlendedNodeMatrix(node, baseTime, targetTime, weight);

        if (animationCalBlender != null) {
            animationCalBlender.handle(node, matrix);
        }

        if (parent != null) {
            matrix.mulLocal(parent);
        }

        nodeStates.get(node.name).mat = matrix;
        for (String name : node.childlist) {
            calculateNodeAndChildrenBlended(geoModel.nodes.get(name), matrix, baseTime, targetTime, weight);
        }
    }

    private Matrix4f composeBlendedNodeMatrix(DataNode node, float baseTime, float targetTime, float weight) {
        DataAnimation animation = geoModel.animations.get(node.name);
        if (animation == null) {
            return composeNodeMatrix(node, baseTime);
        }
        if (weight <= 0f) {
            return composeNodeMatrix(node, baseTime);
        }
        if (weight >= 1f) {
            return composeNodeMatrix(node, targetTime);
        }

        DataAnimation.Transform baseTransform = animation.findTransform(baseTime, node.pos, node.size, node.rot);
        DataAnimation.Transform targetTransform = animation.findTransform(targetTime, node.pos, node.size, node.rot);

        Vector3f position = new Vector3f(baseTransform.pos.x, baseTransform.pos.y, baseTransform.pos.z);
        Vector3f targetPosition = new Vector3f(targetTransform.pos.x, targetTransform.pos.y, targetTransform.pos.z);
        position.lerp(targetPosition, weight);

        Vector3f scale = new Vector3f(baseTransform.size.x, baseTransform.size.y, baseTransform.size.z);
        Vector3f targetScale = new Vector3f(targetTransform.size.x, targetTransform.size.y, targetTransform.size.z);
        scale.lerp(targetScale, weight);

        Quaternionf rotation = new Quaternionf(baseTransform.rot);
        rotation.slerp(targetTransform.rot, weight);

        return new Matrix4f().translate(position).rotate(rotation).scale(scale);
    }

    public void uploadAllJointTransform() {
        if (geoModel.joints.size() == 0) {
            return;
        }
        Matrix4f[] cpuMatrices = ensureCpuJointMatrices();
        final boolean gpuSkinning = SkinningSupport.isGpuSkinningAvailable();
        if (gpuSkinning) {
            if (jointMatsBufferId == -1) {
                jointMatsBufferId = GL15.glGenBuffers();
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, jointMatsBufferId);
                GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, geoModel.joints.size() * 64, GL15.GL_DYNAMIC_DRAW);
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            }
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, jointMatsBufferId);
        } else {
            jointMatsBufferId = -1;
        }

        for (int i = 0; i < geoModel.joints.size(); i++) {
            Matrix4f inv = geoModel.inverseBindMatrices.get(i);
            Matrix4f pose = nodeStates.get(geoModel.joints.get(i)).mat;
            Matrix4f result = cpuMatrices[i];
            result.set(pose);
            result.mul(inv);
            if (gpuSkinning && jointMatsBufferId != -1) {
                MATRIX_BUFFER.clear();
                result.get(MATRIX_BUFFER);
                GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, i * 64, MATRIX_BUFFER);
            }
        }
        if (gpuSkinning && jointMatsBufferId != -1) {
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        }
    }

    private void skinNodeAndChildren(DataNode node, HashSet<String> sun, HashSet<String> moon, Matrix4f[] jointMatrices) {
        if (sun != null && !sun.isEmpty() && !sun.contains(node.name)) {
            return;
        }
        if (moon != null && !moon.isEmpty() && moon.contains(node.name)) {
            return;
        }
        if (geoModel.joints.size() == 0 || jointMatrices == null) {
            return;
        }
        node.meshes.values().forEach((mesh) -> {
            mesh.callSkinning(jointMatrices);
        });
        node.childlist.forEach((child) -> {
            skinNodeAndChildren(geoModel.nodes.get(child), sun, moon, jointMatrices);
        });
    }
    
    public boolean loadAnimation(GltfRenderModel other,boolean skin) {
        if(!other.initedNodeStates) {
            return false;
        }
        if (!initedNodeStates) {
            geoModel.nodes.keySet().forEach((name) -> {
                nodeStates.put(name, new NodeState());
            });
            initedNodeStates = true;
        }
        nodeStates.forEach((k,v)->{
            NodeState s=other.nodeStates.get(k);
            if(s!=null) {
                v.mat.set(s.mat);
            }
            if(animationLoadMapper!=null) {
                animationLoadMapper.handle(this,other, k);
            }
        });
        if (skin && geoModel.joints.size() > 0) {
            uploadAllJointTransform();
            Matrix4f[] jointMatrices = getCpuJointMatrices();
            if (jointMatrices != null) {
                if (SkinningSupport.isGpuSkinningAvailable() && jointMatsBufferId != -1) {
                    ShaderGltf.useShader();
                    GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, ShaderGltf.JOINTMATSBUFFERBINDING,
                        jointMatsBufferId);

                    GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);
                    for (Entry<String, DataNode> e : geoModel.rootNodes.entrySet()) {
                        skinNodeAndChildren(e.getValue(), null, null, jointMatrices);
                    }
                    GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);

                    GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, ShaderGltf.JOINTMATSBUFFERBINDING, 0);
                    GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, ShaderGltf.VERTEXBUFFERBINDING, 0);
                    GL20.glUseProgram(0);
                } else {
                    for (Entry<String, DataNode> e : geoModel.rootNodes.entrySet()) {
                        skinNodeAndChildren(e.getValue(), null, null, jointMatrices);
                    }
                }
            }
        }
        return true;
    }

    public boolean updateAnimation(float time, boolean skin) {
        if (!geoModel.loaded) {
            return false;
        }
        calculateAllNodePose(time);
        applySkinningIfNeeded(skin);
        return true;
    }

    public boolean updateAnimationBlended(float baseTime, float targetTime, float weight, boolean skin) {
        if (!geoModel.loaded) {
            return false;
        }
        if (weight <= 0f) {
            return updateAnimation(baseTime, skin);
        }
        if (weight >= 1f) {
            return updateAnimation(targetTime, skin);
        }
        calculateAllNodePoseBlended(baseTime, targetTime, weight);
        applySkinningIfNeeded(skin);
        return true;
    }

    private void applySkinningIfNeeded(boolean skin) {
        if (!skin || geoModel.joints.size() == 0) {
            return;
        }

        uploadAllJointTransform();
        Matrix4f[] jointMatrices = getCpuJointMatrices();
        if (jointMatrices == null) {
            return;
        }

        if (SkinningSupport.isGpuSkinningAvailable() && jointMatsBufferId != -1) {
            ShaderGltf.useShader();
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, ShaderGltf.JOINTMATSBUFFERBINDING, jointMatsBufferId);

            GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);
            for (Entry<String, DataNode> e : geoModel.rootNodes.entrySet()) {
                skinNodeAndChildren(e.getValue(), null, null, jointMatrices);
            }
            GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);

            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, ShaderGltf.JOINTMATSBUFFERBINDING, 0);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, ShaderGltf.VERTEXBUFFERBINDING, 0);
            GL20.glUseProgram(0);
        } else {
            for (Entry<String, DataNode> e : geoModel.rootNodes.entrySet()) {
                skinNodeAndChildren(e.getValue(), null, null, jointMatrices);
            }
        }
    }

    // 阴阳！哈哈哈 下次试试aplle和pear XD
    public void render(HashSet<String> sun, HashSet<String> moon) {
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        if (!geoModel.loaded) {
            return;
        }

        // 确保nodeStates已初始化
        if (!initedNodeStates) {
            geoModel.nodes.keySet().forEach((name) -> {
                nodeStates.put(name, new NodeState());
            });
            initedNodeStates = true;
        }

        // 应用全局缩放
        GlStateManager.pushMatrix();
        GlStateManager.scale(globalScale, globalScale, globalScale);

        for (Entry<String, DataNode> e : geoModel.nodes.entrySet()) {
            if (sun != null && !sun.isEmpty() && !sun.contains(e.getKey())) {
                continue;
            }
            if (moon != null && !moon.isEmpty() && moon.contains(e.getKey())) {
                continue;
            }
            e.getValue().meshes.entrySet().forEach(entry -> {
                String materialKey = entry.getKey();
                DataMesh mesh = entry.getValue();
                GlStateManager.pushMatrix();
                if (!mesh.skin) {
                    NodeState nodeState = nodeStates.get(e.getValue().name);
                    if (nodeState != null) {
                        MATRIX_BUFFER.clear();
                        nodeState.mat.get(MATRIX_BUFFER);
                        GlStateManager.multMatrix(MATRIX_BUFFER);
                    }
                }
                if (mesh.material == null) {
                    mesh.material = materialKey;
                }
                DataMaterial mat = resolveMaterial(mesh.material);
                RenderState state = RenderState.capture();
                applyAlphaMode(mat);
                GlStateManager.color(1f, 1f, 1f, 1f); // 确保主体渲染不受之前颜色状态影响
                bindMaterialTexture(mat);
                mesh.render();
                state.restore();
                GlStateManager.popMatrix();
            });
        }

        // 恢复矩阵
        GlStateManager.popMatrix();
    }

    public void renderAll() {
        render(null, null);
    }

    @Deprecated
    public void renderPart(String part) {
        HashSet<String> set = setObj;
        setObj.clear();
        set.add(part);
        render(set, null);
    }

    @Deprecated
    public void renderOnly(String[] part) {
        HashSet<String> set = setObj;
        setObj.clear();
        for (int i = 0; i < part.length; i++) {
            set.add(part[i]);
        }
        renderOnly(set);
    }

    @Deprecated
    public void renderExcept(String[] part) {
        HashSet<String> set = setObj;
        setObj.clear();
        for (int i = 0; i < part.length; i++) {
            set.add(part[i]);
        }
        renderExcept(set);
    }

    public void renderOnly(HashSet<String> part) {
        render(part, null);
    }

    public void renderExcept(HashSet<String> part) {
        render(null, part);
    }

    // --- Emissive helpers ---
    private float prevLightX = -1f;
    private float prevLightY = -1f;

    private DataMaterial resolveMaterial(String name) {
        if (geoModel.materials == null) {
            return null;
        }
        DataMaterial mat = geoModel.materials.get(name);
        if (mat != null) {
            return mat;
        }
        // fallback: first material
        if (!geoModel.materials.isEmpty()) {
            return geoModel.materials.values().iterator().next();
        }
        return null;
    }


    private void applyAlphaMode(DataMaterial mat) {
        if (mat == null) {
            return;
        }
        switch (mat.alphaMode) {
            case OPAQUE:
                GlStateManager.disableBlend();
                GL11.glDisable(GL11.GL_ALPHA_TEST);
                GlStateManager.depthMask(true);
                break;
            case MASK:
                GlStateManager.disableBlend();
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                GL11.glAlphaFunc(GL11.GL_GREATER, mat.alphaCutoff);
                GlStateManager.depthMask(true);
                break;
            case BLEND:
            default:
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDisable(GL11.GL_ALPHA_TEST);
                GlStateManager.depthMask(true);
                break;
        }
    }

    private void bindMaterialTexture(@Nullable DataMaterial material) {
        ResourceLocation toBind = null;
        if (material != null) {
            toBind = material.getBaseColorTextureResource();
        }
        if (toBind == null) {
            toBind = defaultBaseTexture;
        }
        if (toBind != null) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft != null) {
                minecraft.getTextureManager().bindTexture(toBind);
            }
        }
    }

    @Nullable
    public Vec3d sampleNodeWorldPosition(String nodeName, double baseX, double baseY, double baseZ,
                                         float yawDegrees, float pitchDegrees, float scaleMultiplier) {
        if (nodeName == null || nodeName.isEmpty() || nodeStates == null || nodeStates.isEmpty()) {
            return null;
        }

        // First try to find the node (with case-insensitive fallback)
        String resolvedName = nodeName;
        NodeState state = nodeStates.get(nodeName);
        if (state == null || state.mat == null) {
            for (Map.Entry<String, NodeState> entry : nodeStates.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(nodeName)) {
                    state = entry.getValue();
                    resolvedName = entry.getKey();
                    break;
                }
            }
            if (state == null || state.mat == null) {
                return null;
            }
        }

        Matrix4f matrix;

        // Check if this node is a joint (bone used for skinning)
        int jointIndex = geoModel.joints.indexOf(resolvedName);
        if (jointIndex >= 0 && jointIndex < geoModel.inverseBindMatrices.size()) {
            // For skinning joints:
            // - bindMatrix contains the bone's rest position in model space
            // - pose (nodeStates.mat) contains the animated transformation
            // - We compute: pose * bindMatrix to get the animated bone position
            Matrix4f invBind = geoModel.inverseBindMatrices.get(jointIndex);
            Matrix4f bindMatrix = new Matrix4f(invBind).invert();
            Matrix4f pose = state.mat;

            // Animated bone transform = pose * bindMatrix
            matrix = new Matrix4f(pose);
            matrix.mul(bindMatrix);
        } else {
            // Regular node - use nodeState directly
            matrix = new Matrix4f(state.mat);
        }

        Vector3f translation = new Vector3f();
        matrix.getTranslation(translation);

        float scale = getGlobalScale();
        if (scaleMultiplier > 0f) {
            scale *= scaleMultiplier;
        }

        translation.mul(scale);
        Vector3f rotated = rotateTranslation(translation, yawDegrees, pitchDegrees);
        return new Vec3d(baseX + rotated.x, baseY + rotated.y, baseZ + rotated.z);
    }

    /**
     * Returns a list of joint (bone) names that can be used for attachment.
     * These are the nodes that participate in skeletal animation (skinning).
     */
    public java.util.List<String> getJointNames() {
        return new java.util.ArrayList<>(geoModel.joints);
    }

    /**
     * Returns a list of all node names (including joints and mesh nodes).
     */
    public java.util.List<String> getAllNodeNames() {
        return new java.util.ArrayList<>(geoModel.nodes.keySet());
    }

    /**
     * Checks if a node exists in this model (case-insensitive).
     */
    public boolean hasNode(String nodeName) {
        if (nodeName == null || nodeName.isEmpty()) {
            return false;
        }
        if (geoModel.nodes.containsKey(nodeName)) {
            return true;
        }
        for (String name : geoModel.nodes.keySet()) {
            if (name.equalsIgnoreCase(nodeName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a node is a joint (bone used for skinning).
     */
    public boolean isJoint(String nodeName) {
        if (nodeName == null || nodeName.isEmpty()) {
            return false;
        }
        if (geoModel.joints.contains(nodeName)) {
            return true;
        }
        for (String joint : geoModel.joints) {
            if (joint.equalsIgnoreCase(nodeName)) {
                return true;
            }
        }
        return false;
    }

    private static Vector3f rotateTranslation(Vector3f translation, float yawDegrees, float pitchDegrees) {
        float x = translation.x;
        float y = translation.y;
        float z = translation.z;
        if (yawDegrees != 0f) {
            // Match GlStateManager.rotate(yaw, 0, -1, 0) - rotate around negative Y axis
            double yawRad = Math.toRadians(yawDegrees);
            double cosYaw = Math.cos(yawRad);
            double sinYaw = Math.sin(yawRad);
            float rotatedX = (float) (x * cosYaw - z * sinYaw);
            float rotatedZ = (float) (x * sinYaw + z * cosYaw);
            x = rotatedX;
            z = rotatedZ;
        }
        if (pitchDegrees != 0f) {
            double pitchRad = Math.toRadians(pitchDegrees);
            double cosPitch = Math.cos(pitchRad);
            double sinPitch = Math.sin(pitchRad);
            float rotatedY = (float) (y * cosPitch - z * sinPitch);
            float rotatedZ = (float) (y * sinPitch + z * cosPitch);
            y = rotatedY;
            z = rotatedZ;
        }
        return new Vector3f(x, y, z);
    }

    private static class RenderState {
        private final boolean blend;
        private final boolean alphaTest;
        private final boolean depthMask;
        private final int blendSrc;
        private final int blendDst;

        private RenderState(boolean blend, boolean alphaTest, boolean depthMask, int blendSrc, int blendDst) {
            this.blend = blend;
            this.alphaTest = alphaTest;
            this.depthMask = depthMask;
            this.blendSrc = blendSrc;
            this.blendDst = blendDst;
        }

        static RenderState capture() {
            boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
            boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
            boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            int src = GL11.glGetInteger(GL11.GL_BLEND_SRC);
            int dst = GL11.glGetInteger(GL11.GL_BLEND_DST);
            return new RenderState(blend, alpha, depthMask, src, dst);
        }

        void restore() {
            if (blend) {
                GlStateManager.enableBlend();
            } else {
                GlStateManager.disableBlend();
            }
            GL11.glBlendFunc(blendSrc, blendDst);
            if (alphaTest) {
                GL11.glEnable(GL11.GL_ALPHA_TEST);
            } else {
                GL11.glDisable(GL11.GL_ALPHA_TEST);
            }
            GlStateManager.depthMask(depthMask);
        }
    }
}
