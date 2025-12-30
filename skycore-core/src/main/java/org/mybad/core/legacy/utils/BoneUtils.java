package org.mybad.core.legacy.utils;

import org.mybad.core.data.*;
import java.util.*;

/**
 * 骨骼工具库
 * 提供骨骼操作的便利函数
 */
public class BoneUtils {

    /**
     * 获取骨骼的完整层级路径
     */
    public static List<ModelBone> getBonePath(ModelBone bone) {
        List<ModelBone> path = new ArrayList<>();
        ModelBone current = bone;

        while (current != null) {
            path.add(0, current);  // 从根到叶子
            current = current.getParent();
        }

        return path;
    }

    /**
     * 获取骨骼的完整名称路径
     */
    public static String getBonePathString(ModelBone bone) {
        List<ModelBone> path = getBonePath(bone);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < path.size(); i++) {
            if (i > 0) sb.append("/");
            sb.append(path.get(i).getName());
        }

        return sb.toString();
    }

    /**
     * 获取骨骼深度（根为0）
     */
    public static int getBoneDepth(ModelBone bone) {
        int depth = 0;
        ModelBone current = bone.getParent();

        while (current != null) {
            depth++;
            current = current.getParent();
        }

        return depth;
    }

    /**
     * 获取骨骼的所有子孙（不包括自己）
     */
    public static List<ModelBone> getAllDescendants(ModelBone bone) {
        List<ModelBone> descendants = new ArrayList<>();
        addDescendantsRecursively(bone, descendants);
        return descendants;
    }

    private static void addDescendantsRecursively(ModelBone bone, List<ModelBone> result) {
        for (ModelBone child : bone.getChildren()) {
            result.add(child);
            addDescendantsRecursively(child, result);
        }
    }

    /**
     * 获取骨骼的所有直接或间接子骨骼数量
     */
    public static int getDescendantCount(ModelBone bone) {
        return getAllDescendants(bone).size();
    }

    /**
     * 获取骨骼树的高度（最大深度）
     */
    public static int getTreeHeight(ModelBone bone) {
        if (bone.getChildren().isEmpty()) {
            return 0;
        }

        int maxHeight = 0;
        for (ModelBone child : bone.getChildren()) {
            maxHeight = Math.max(maxHeight, getTreeHeight(child));
        }

        return maxHeight + 1;
    }

    /**
     * 找到两个骨骼的最近公共祖先
     */
    public static ModelBone findCommonAncestor(ModelBone bone1, ModelBone bone2) {
        List<ModelBone> path1 = getBonePath(bone1);
        List<ModelBone> path2 = getBonePath(bone2);

        ModelBone common = null;
        for (int i = 0; i < Math.min(path1.size(), path2.size()); i++) {
            if (path1.get(i) == path2.get(i)) {
                common = path1.get(i);
            } else {
                break;
            }
        }

        return common;
    }

    /**
     * 检查骨骼A是否是骨骼B的祖先
     */
    public static boolean isAncestor(ModelBone ancestor, ModelBone bone) {
        ModelBone current = bone.getParent();

        while (current != null) {
            if (current == ancestor) {
                return true;
            }
            current = current.getParent();
        }

        return false;
    }

    /**
     * 检查骨骼A是否是骨骼B的子孙
     */
    public static boolean isDescendant(ModelBone descendant, ModelBone bone) {
        return isAncestor(bone, descendant);
    }

    /**
     * 计算骨骼的绑定姿态（所有变换累积）
     */
    public static float[] calculateBindPose(ModelBone bone) {
        List<ModelBone> path = getBonePath(bone);
        float[] position = new float[]{0, 0, 0};

        for (ModelBone b : path) {
            float[] bonePos = b.getPosition();
            position[0] += bonePos[0];
            position[1] += bonePos[1];
            position[2] += bonePos[2];
        }

        return position;
    }

    /**
     * 获取骨骼的局部变换矩阵值
     */
    public static float[] getLocalTransform(ModelBone bone) {
        float[] pos = bone.getPosition();
        float[] rot = bone.getRotation();
        float[] size = bone.getSize();

        return new float[]{
            pos[0], pos[1], pos[2],
            rot[0], rot[1], rot[2],
            size[0], size[1], size[2]
        };
    }

    /**
     * 设置骨骼的局部变换矩阵值
     */
    public static void setLocalTransform(ModelBone bone, float[] transform) {
        if (transform.length >= 9) {
            bone.setPosition(transform[0], transform[1], transform[2]);
            bone.setRotation(transform[3], transform[4], transform[5]);
            bone.setSize(transform[6], transform[7], transform[8]);
        }
    }

    /**
     * 重置骨骼变换到绑定姿态
     */
    public static void resetToBindPose(ModelBone bone) {
        bone.setPosition(0, 0, 0);
        bone.setRotation(0, 0, 0);
        bone.setSize(1, 1, 1);
    }

    /**
     * 复制骨骼变换
     */
    public static void copyTransform(ModelBone source, ModelBone target) {
        float[] srcPos = source.getPosition();
        float[] srcRot = source.getRotation();
        float[] srcSize = source.getSize();

        target.setPosition(srcPos[0], srcPos[1], srcPos[2]);
        target.setRotation(srcRot[0], srcRot[1], srcRot[2]);
        target.setSize(srcSize[0], srcSize[1], srcSize[2]);
    }

    /**
     * 获取包含所有骨骼的骨骼名称列表
     */
    public static List<String> getAllBoneNames(Model model) {
        List<String> names = new ArrayList<>();

        for (ModelBone bone : model.getBones()) {
            collectBoneNames(bone, names);
        }

        return names;
    }

    private static void collectBoneNames(ModelBone bone, List<String> names) {
        names.add(bone.getName());

        for (ModelBone child : bone.getChildren()) {
            collectBoneNames(child, names);
        }
    }

    /**
     * 计算骨骼的总立方体数量
     */
    public static int getTotalCubeCount(Model model) {
        int count = 0;

        for (ModelBone bone : model.getBones()) {
            count += bone.getCubes().size();
            count += getTotalCubeCountRecursive(bone);
        }

        return count;
    }

    private static int getTotalCubeCountRecursive(ModelBone bone) {
        int count = 0;

        for (ModelBone child : bone.getChildren()) {
            count += child.getCubes().size();
            count += getTotalCubeCountRecursive(child);
        }

        return count;
    }

    /**
     * 获取骨骼树的统计信息
     */
    public static TreeStats getTreeStats(Model model) {
        TreeStats stats = new TreeStats();

        for (ModelBone rootBone : model.getBones()) {
            if (rootBone.getParent() == null) {
                collectTreeStats(rootBone, stats, 0);
            }
        }

        stats.locatorCount = model.getLocators().size();
        stats.constraintCount = model.getConstraints().size();

        return stats;
    }

    private static void collectTreeStats(ModelBone bone, TreeStats stats, int depth) {
        stats.boneCount++;
        stats.cubeCount += bone.getCubes().size();
        stats.maxDepth = Math.max(stats.maxDepth, depth);

        for (ModelBone child : bone.getChildren()) {
            collectTreeStats(child, stats, depth + 1);
        }
    }

    /**
     * 树统计信息
     */
    public static class TreeStats {
        public int boneCount = 0;
        public int cubeCount = 0;
        public int maxDepth = 0;
        public int locatorCount = 0;
        public int constraintCount = 0;
        public int totalBones;

        @Override
        public String toString() {
            return String.format(
                "骨骼树统计: 骨骼=%d, 立方体=%d, 最大深度=%d, 定位器=%d, 约束=%d",
                boneCount, cubeCount, maxDepth, locatorCount, constraintCount
            );
        }
    }
}
