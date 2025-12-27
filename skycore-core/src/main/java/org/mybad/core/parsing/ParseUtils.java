package org.mybad.core.parsing;

import com.google.gson.*;

/**
 * 解析工具函数
 * 提供通用的JSON解析辅助方法
 */
public class ParseUtils {

    /**
     * 从JsonElement中获取float数组
     * 支持多种格式：[1.0, 2.0, 3.0] 或 ["1.0", "2.0", "3.0"]
     */
    public static float[] parseFloatArray(JsonElement element, float... defaults) {
        if (element == null || element.isJsonNull()) {
            return defaults.length > 0 ? defaults : new float[]{0, 0, 0};
        }

        if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            float[] result = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                JsonElement item = arr.get(i);
                if (item.isJsonPrimitive()) {
                    result[i] = item.getAsFloat();
                } else if (item.isJsonNull()) {
                    result[i] = i < defaults.length ? defaults[i] : 0;
                }
            }
            return result;
        }

        return defaults.length > 0 ? defaults : new float[]{0, 0, 0};
    }

    /**
     * 从JsonElement中获取int数组
     */
    public static int[] parseIntArray(JsonElement element, int... defaults) {
        if (element == null || element.isJsonNull()) {
            return defaults.length > 0 ? defaults : new int[]{0, 0, 0};
        }

        if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            int[] result = new int[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                JsonElement item = arr.get(i);
                if (item.isJsonPrimitive()) {
                    result[i] = item.getAsInt();
                } else if (item.isJsonNull()) {
                    result[i] = i < defaults.length ? defaults[i] : 0;
                }
            }
            return result;
        }

        return defaults.length > 0 ? defaults : new int[]{0, 0, 0};
    }

    /**
     * 安全获取float值
     */
    public static float getFloat(JsonObject obj, String key, float defaultValue) {
        if (obj.has(key)) {
            JsonElement elem = obj.get(key);
            if (elem.isJsonPrimitive()) {
                try {
                    return elem.getAsFloat();
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    /**
     * 安全获取int值
     */
    public static int getInt(JsonObject obj, String key, int defaultValue) {
        if (obj.has(key)) {
            JsonElement elem = obj.get(key);
            if (elem.isJsonPrimitive()) {
                try {
                    return elem.getAsInt();
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    /**
     * 安全获取boolean值
     */
    public static boolean getBoolean(JsonObject obj, String key, boolean defaultValue) {
        if (obj.has(key)) {
            JsonElement elem = obj.get(key);
            if (elem.isJsonPrimitive()) {
                return elem.getAsBoolean();
            }
        }
        return defaultValue;
    }

    /**
     * 安全获取string值
     */
    public static String getString(JsonObject obj, String key, String defaultValue) {
        if (obj.has(key)) {
            JsonElement elem = obj.get(key);
            if (elem.isJsonPrimitive()) {
                return elem.getAsString();
            }
        }
        return defaultValue;
    }

    /**
     * 安全获取float数组
     */
    public static float[] getFloatArray(JsonObject obj, String key, float... defaults) {
        if (obj.has(key)) {
            JsonElement elem = obj.get(key);
            return parseFloatArray(elem, defaults);
        }
        return defaults.length > 0 ? defaults : new float[]{0, 0, 0};
    }

    /**
     * 安全获取int数组
     */
    public static int[] getIntArray(JsonObject obj, String key, int... defaults) {
        if (obj.has(key)) {
            JsonElement elem = obj.get(key);
            return parseIntArray(elem, defaults);
        }
        return defaults.length > 0 ? defaults : new int[]{0, 0, 0};
    }

    /**
     * 转换坐标：Bedrock -> Minecraft
     * Bedrock使用Z轴朝上，Minecraft使用Y轴朝上
     * 坐标映射：[x, y, z] -> [x, z, -y]
     */
    public static float[] convertCoordinate(float[] bedrockCoord) {
        if (bedrockCoord == null || bedrockCoord.length < 3) {
            return new float[]{0, 0, 0};
        }
        return new float[]{
            bedrockCoord[0],
            bedrockCoord[2],
            -bedrockCoord[1]
        };
    }

    /**
     * 反向转换坐标：Minecraft -> Bedrock
     */
    public static float[] convertCoordinateBack(float[] mcCoord) {
        if (mcCoord == null || mcCoord.length < 3) {
            return new float[]{0, 0, 0};
        }
        return new float[]{
            mcCoord[0],
            -mcCoord[2],
            mcCoord[1]
        };
    }

    /**
     * 规范化欧拉角（将角度限制在0-360范围内）
     */
    public static float normalizeAngle(float angle) {
        while (angle < 0) angle += 360;
        while (angle >= 360) angle -= 360;
        return angle;
    }

    /**
     * 检查字符串是否为空或null
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 获取json对象中的子对象
     */
    public static JsonObject getObject(JsonObject parent, String key) {
        if (parent.has(key)) {
            JsonElement elem = parent.get(key);
            if (elem.isJsonObject()) {
                return elem.getAsJsonObject();
            }
        }
        return new JsonObject();
    }

    /**
     * 获取json对象中的数组
     */
    public static JsonArray getArray(JsonObject parent, String key) {
        if (parent.has(key)) {
            JsonElement elem = parent.get(key);
            if (elem.isJsonArray()) {
                return elem.getAsJsonArray();
            }
        }
        return new JsonArray();
    }
}
