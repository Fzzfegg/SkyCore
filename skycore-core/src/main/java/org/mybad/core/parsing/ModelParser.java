package org.mybad.core.parsing;

import com.google.gson.*;
import org.mybad.core.data.*;
import org.mybad.core.exception.ParseException;

import java.util.*;

/**
 * 基岩格式模型解析器
 * 负责将JSON格式的基岩模型数据解析为内部数据结构
 *
 * 解析流程：
 * 1. 加载JSON → JsonObject
 * 2. 检查格式版本
 * 3. 第一遍扫描：创建骨骼对象
 * 4. 第二遍扫描：链接父子关系和其他引用
 * 5. 显式坐标转换（Bedrock -> Minecraft）
 * 6. 返回完整Model对象
 */
public class ModelParser {
    private static final String[] SUPPORTED_VERSIONS = {"1.12.0", "1.14.0", "1.16.0", "1.17.0", "1.18.0", "1.21.0"};

    private Gson gson;

    public ModelParser() {
        this.gson = new GsonBuilder().create();
    }

    /**
     * 从JSON字符串解析模型
     */
    public Model parse(String jsonContent) throws ParseException {
        try {
            JsonElement element = new JsonParser().parse(jsonContent);
            if (!element.isJsonObject()) {
                throw new ParseException("JSON必须是对象格式");
            }
            return parseJsonObject(element.getAsJsonObject());
        } catch (JsonSyntaxException e) {
            throw new ParseException("JSON语法错误: " + e.getMessage(), e);
        }
    }

    /**
     * 从JsonObject解析模型
     */
    public Model parseJsonObject(JsonObject root) throws ParseException {
        // 检查格式版本
        checkFormatVersion(root);

        // 获取geometry数组
        JsonArray geometryArray = ParseUtils.getArray(root, "minecraft:geometry");
        if (geometryArray.size() == 0) {
            throw new ParseException("未找到minecraft:geometry数组");
        }

        // 解析第一个geometry（通常只有一个）
        JsonObject geometry = geometryArray.get(0).getAsJsonObject();

        // 获取description对象
        JsonObject description = geometry.getAsJsonObject("description");
        if (description == null) {
            description = new JsonObject();
        }

        // 创建模型 - 从description中提取identifier
        String modelName = "model";
        if (description.has("identifier")) {
            modelName = description.get("identifier").getAsString();
        }
        Model model = new Model(modelName);

        // 设置纹理大小
        int textureWidth = 64;
        int textureHeight = 64;
        if (description.has("texture_width")) {
            textureWidth = description.get("texture_width").getAsInt();
        }
        if (description.has("texture_height")) {
            textureHeight = description.get("texture_height").getAsInt();
        }
        model.setTextureWidth(String.valueOf(textureWidth));
        model.setTextureHeight(String.valueOf(textureHeight));

        // 第一遍扫描：创建骨骼对象
        Map<String, ModelBone> boneMap = new HashMap<>();
        JsonArray bones = ParseUtils.getArray(geometry, "bones");
        for (int i = 0; i < bones.size(); i++) {
            JsonObject boneJson = bones.get(i).getAsJsonObject();
            ModelBone bone = parseBone(boneJson);
            boneMap.put(bone.getName(), bone);
            model.addBone(bone);
            parseBoneLocators(boneJson, bone.getName(), model);
        }

        // 第二遍扫描：链接父子关系
        for (int i = 0; i < bones.size(); i++) {
            JsonObject boneJson = bones.get(i).getAsJsonObject();
            String parentName = ParseUtils.getString(boneJson, "parent", null);
            if (parentName != null && !parentName.isEmpty()) {
                ModelBone bone = boneMap.get(boneJson.get("name").getAsString());
                ModelBone parent = boneMap.get(parentName);
                if (parent != null && bone != null) {
                    parent.addChild(bone);
                }
            }
        }

        // 解析定位器（不需要坐标转换）
        JsonObject locators = ParseUtils.getObject(geometry, "locators");
        for (Map.Entry<String, JsonElement> entry : locators.entrySet()) {
            String locatorName = entry.getKey();
            JsonElement locatorData = entry.getValue();
            if (locatorData.isJsonArray()) {
                float[] pos = ParseUtils.parseFloatArray(locatorData);
                ModelLocator locator = new ModelLocator(locatorName, pos[0], pos[1], pos[2]);
                model.addLocator(locatorName, locator);
            }
        }

        return model;
    }

    private void parseBoneLocators(JsonObject boneJson, String boneName, Model model) {
        if (boneJson == null || model == null) {
            return;
        }
        if (!boneJson.has("locators")) {
            return;
        }
        JsonElement locatorsElement = boneJson.get("locators");
        if (!locatorsElement.isJsonObject()) {
            return;
        }
        JsonObject locators = locatorsElement.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : locators.entrySet()) {
            String locatorName = entry.getKey();
            JsonElement locatorData = entry.getValue();
            if (locatorName == null || locatorName.isEmpty()) {
                continue;
            }
            float[] pos = null;
            float[] rot = null;
            boolean visible = true;

            if (locatorData.isJsonArray()) {
                pos = ParseUtils.parseFloatArray(locatorData);
            } else if (locatorData.isJsonObject()) {
                JsonObject obj = locatorData.getAsJsonObject();
                JsonElement posElem = obj.get("position");
                if (posElem == null) {
                    posElem = obj.get("offset");
                }
                if (posElem == null) {
                    posElem = obj.get("pivot");
                }
                if (posElem != null && posElem.isJsonArray()) {
                    pos = ParseUtils.parseFloatArray(posElem);
                }
                JsonElement rotElem = obj.get("rotation");
                if (rotElem != null && rotElem.isJsonArray()) {
                    rot = ParseUtils.parseFloatArray(rotElem);
                }
                if (obj.has("visible")) {
                    visible = ParseUtils.getBoolean(obj, "visible", true);
                } else if (obj.has("is_visible")) {
                    visible = ParseUtils.getBoolean(obj, "is_visible", true);
                }
            }

            ModelLocator locator;
            if (pos != null && pos.length >= 3) {
                locator = new ModelLocator(locatorName, pos[0], pos[1], pos[2]);
            } else {
                locator = new ModelLocator(locatorName);
            }
            locator.setAttachedBone(boneName);
            if (rot != null && rot.length >= 3) {
                locator.setRotation(rot[0], rot[1], rot[2]);
            }
            locator.setVisible(visible);
            model.addLocator(locatorName, locator);
        }
    }

    /**
     * 解析单个骨骼
     */
    private ModelBone parseBone(JsonObject boneJson) throws ParseException {
        String name = ParseUtils.getString(boneJson, "name", "bone");
        ModelBone bone = new ModelBone(name);

        // 解析变换属性
        // 注意：Bedrock 和 Minecraft 都使用 Y 轴向上的坐标系，不需要转换
        float[] pivot = ParseUtils.getFloatArray(boneJson, "pivot", 0, 0, 0);
        bone.setPivot(pivot);

        boolean hasPosition = boneJson.has("position");
        float[] position = hasPosition
            ? ParseUtils.getFloatArray(boneJson, "position", 0, 0, 0)
            : new float[]{0f, 0f, 0f};
        bone.setPosition(position);
        bone.setHasPositionOverride(hasPosition);

        float[] rotation = ParseUtils.getFloatArray(boneJson, "rotation", 0, 0, 0);
        bone.setRotation(rotation);

        // 骨骼大小（如果有的话）
        JsonArray sizeArray = ParseUtils.getArray(boneJson, "size");
        if (sizeArray.size() == 3) {
            float[] size = ParseUtils.parseFloatArray(sizeArray);
            bone.setSize(size);
        }

        // 标志
        bone.setMirror(ParseUtils.getBoolean(boneJson, "mirror", false));
        bone.setNeverRender(ParseUtils.getBoolean(boneJson, "neverRender", false));
        bone.setReset(ParseUtils.getBoolean(boneJson, "reset", false));

        // 解析立方体
        JsonArray cubes = ParseUtils.getArray(boneJson, "cubes");
        for (int i = 0; i < cubes.size(); i++) {
            JsonObject cubeJson = cubes.get(i).getAsJsonObject();
            ModelCube cube = parseCube(cubeJson);
            bone.addCube(cube);
        }

        bone.captureBindPose();

        return bone;
    }

    /**
     * 解析立方体
     */
    private ModelCube parseCube(JsonObject cubeJson) throws ParseException {
        ModelCube cube = new ModelCube();

        // 解析位置（不需要坐标转换）
        float[] origin = ParseUtils.getFloatArray(cubeJson, "origin", 0, 0, 0);
        cube.setOrigin(origin);

        // 解析大小
        float[] size = ParseUtils.getFloatArray(cubeJson, "size", 1, 1, 1);
        cube.setSize(size);

        // 解析旋转（立方体旋转，不需要转换）
        float[] rotation = ParseUtils.getFloatArray(cubeJson, "rotation", 0, 0, 0);
        cube.setRotation(rotation);

        // 解析旋转中心（不需要坐标转换）
        if (cubeJson.has("pivot")) {
            float[] pivot = ParseUtils.getFloatArray(cubeJson, "pivot", 0, 0, 0);
            cube.setPivot(pivot);
        }

        // 标志
        cube.setMirror(ParseUtils.getBoolean(cubeJson, "mirror", false));
        cube.setInflate(ParseUtils.getBoolean(cubeJson, "inflate", false));

        // 解析inflate值
        float inflateAmount = ParseUtils.getFloat(cubeJson, "inflate", 0);
        cube.setInflateAmount(inflateAmount);

        // 解析UV映射
        if (cubeJson.has("uv")) {
            JsonElement uvElement = cubeJson.get("uv");
            UVMapping uv = parseUV(uvElement);
            if (uv != null) {
                cube.setUV(uv);
            }
        }

        return cube;
    }

    /**
     * 解析UV映射
     * 支持简单格式 [u, v] 和 per-face格式
     */
    private UVMapping parseUV(JsonElement uvElement) {
        if (uvElement.isJsonArray()) {
            JsonArray arr = uvElement.getAsJsonArray();
            if (arr.size() == 2) {
                // 简单格式：[u, v]
                int u = arr.get(0).getAsInt();
                int v = arr.get(1).getAsInt();
                return new UVMapping(u, v);
            }
        } else if (uvElement.isJsonObject()) {
            // Per-face格式
            // Bedrock 格式: { "north": { "uv": [u, v], "uv_size": [width, height] }, ... }
            JsonObject uvObj = uvElement.getAsJsonObject();
            UVMapping uv = new UVMapping();

            String[] faces = {"north", "south", "east", "west", "up", "down"};
            for (String face : faces) {
                if (uvObj.has(face)) {
                    JsonObject faceData = uvObj.get(face).getAsJsonObject();
                    // uv 和 uv_size 都是数组 [x, y]
                    int[] uvArr = ParseUtils.getIntArray(faceData, "uv", 0, 0);
                    int[] uvSizeArr = ParseUtils.getIntArray(faceData, "uv_size", 0, 0);
                    uv.setFaceUV(face, uvArr[0], uvArr[1], uvSizeArr[0], uvSizeArr[1]);
                }
            }
            return uv;
        }

        return null;
    }

    /**
     * 检查格式版本
     */
    private void checkFormatVersion(JsonObject root) throws ParseException {
        String version = ParseUtils.getString(root, "format_version", "1.12.0");

        boolean supported = false;
        for (String supportedVersion : SUPPORTED_VERSIONS) {
            if (version.equals(supportedVersion)) {
                supported = true;
                break;
            }
        }

        if (!supported) {
            System.out.println("警告：不支持的格式版本 " + version + "，尝试继续解析...");
        }
    }
}
