package org.mybad.core.particle;

import java.util.*;
import java.util.regex.*;

/**
 * 粒子效果解析器 - 从JSON加载粒子效果配置
 * 支持Bedrock粒子格式（暴雪粒子）
 */
public class ParticleParser {

    /**
     * 从JSON字符串解析粒子效果（暴雪格式）
     */
    public static ParticleEffect parseFromJson(String jsonContent, String effectId, String effectName) throws ParseException {
        if (jsonContent == null || jsonContent.isEmpty()) {
            throw new ParseException("JSON content is empty");
        }

        try {
            ParticleEffect effect = new ParticleEffect(effectId, effectName);

            // 解析顶级描述
            parseDescription(effect, jsonContent);

            // 解析所有发射器
            List<Emitter> emitters = parseEmitters(jsonContent);
            for (Emitter emitter : emitters) {
                effect.addEmitter(emitter);
            }

            // 验证效果
            if (!effect.validate()) {
                throw new ParseException("Invalid particle effect configuration");
            }

            return effect;

        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException("Failed to parse particle effect: " + e.getMessage(), e);
        }
    }

    /**
     * 解析描述部分
     */
    private static void parseDescription(ParticleEffect effect, String json) {
        Pattern descPattern = Pattern.compile("\"description\"\\s*:\\s*\\{([^}]*)\\}");
        Matcher descMatcher = descPattern.matcher(json);

        if (descMatcher.find()) {
            String descContent = descMatcher.group(1);

            // 解析identifier
            String identifier = extractJsonStringValue(descContent, "identifier");
            if (!identifier.isEmpty()) {
                effect.setMetadata("identifier", identifier);
            }

            // 解析材质
            Pattern matPattern = Pattern.compile("\"material\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matMatcher = matPattern.matcher(descContent);
            if (matMatcher.find()) {
                effect.setMetadata("material", matMatcher.group(1));
            }

            // 解析纹理
            String texture = extractJsonStringValue(descContent, "texture");
            if (!texture.isEmpty()) {
                effect.setTextureFile(texture);
            }

            // 解析最大粒子数
            String maxParticles = extractJsonValue(descContent, "max_particles");
            if (!maxParticles.isEmpty()) {
                try {
                    effect.setMaxParticles(Integer.parseInt(maxParticles));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    /**
     * 解析所有发射器
     */
    private static List<Emitter> parseEmitters(String json) throws ParseException {
        List<Emitter> emitters = new ArrayList<>();

        // 查找所有emitter块
        Pattern emitterPattern = Pattern.compile("\"emitter([^\"]*?)\"\\s*:\\s*\\{");
        Matcher emitterMatcher = emitterPattern.matcher(json);

        int emitterIndex = 0;
        while (emitterMatcher.find()) {
            String emitterName = emitterMatcher.group(1).trim();
            if (emitterName.startsWith("_")) {
                emitterName = emitterName.substring(1);
            }

            int startPos = emitterMatcher.start();
            int endPos = findMatchingBrace(json, json.indexOf('{', startPos));

            if (endPos > startPos) {
                String emitterContent = json.substring(startPos, endPos + 1);
                Emitter emitter = parseEmitterConfig(emitterContent, emitterIndex++, emitterName);
                if (emitter != null) {
                    emitters.add(emitter);
                }
            }
        }

        return emitters;
    }

    /**
     * 解析单个发射器配置
     */
    private static Emitter parseEmitterConfig(String emitterJson, int index, String name) {
        String emitterId = "emitter_" + index;
        Emitter emitter = new Emitter(emitterId, name);

        // 发射速率
        String rate = extractJsonValue(emitterJson, "emission_rate");
        if (!rate.isEmpty()) {
            try {
                emitter.setEmissionRate(Float.parseFloat(rate));
            } catch (NumberFormatException ignored) {}
        }

        // 粒子生命周期
        parseLifetime(emitter, emitterJson);

        // 初始速度
        parseInitialSpeed(emitter, emitterJson);

        // 初始缩放
        parseScaleRange(emitter, emitterJson);

        // 发射形状
        parseEmitterShape(emitter, emitterJson);

        // 位置
        parseEmitterPosition(emitter, emitterJson);

        // 旋转
        parseEmitterRotation(emitter, emitterJson);

        return emitter;
    }

    /**
     * 解析粒子生命周期
     */
    private static void parseLifetime(Emitter emitter, String json) {
        Pattern lifetimePattern = Pattern.compile("\"lifetime\"\\s*:\\s*\\{([^}]*)\\}");
        Matcher matcher = lifetimePattern.matcher(json);

        float minLife = 1.0f;
        float maxLife = 1.0f;

        if (matcher.find()) {
            String content = matcher.group(1);

            String min = extractJsonValue(content, "min");
            if (!min.isEmpty()) {
                try {
                    minLife = Float.parseFloat(min);
                } catch (NumberFormatException ignored) {}
            }

            String max = extractJsonValue(content, "max");
            if (!max.isEmpty()) {
                try {
                    maxLife = Float.parseFloat(max);
                } catch (NumberFormatException ignored) {}
            }
        }

        emitter.setLifetimeRange(minLife, maxLife);
    }

    /**
     * 解析初始速度
     */
    private static void parseInitialSpeed(Emitter emitter, String json) {
        Pattern speedPattern = Pattern.compile("\"initial_speed\"\\s*:\\s*\\{([^}]*)\\}");
        Matcher matcher = speedPattern.matcher(json);

        float minX = 0, maxX = 0, minY = 0, maxY = 1, minZ = 0, maxZ = 0;

        if (matcher.find()) {
            String content = matcher.group(1);

            minX = parseFloatValue(content, "min_x", 0);
            maxX = parseFloatValue(content, "max_x", 0);
            minY = parseFloatValue(content, "min_y", 0);
            maxY = parseFloatValue(content, "max_y", 1);
            minZ = parseFloatValue(content, "min_z", 0);
            maxZ = parseFloatValue(content, "max_z", 0);
        }

        emitter.setSpeedRange(minX, maxX, minY, maxY, minZ, maxZ);
    }

    /**
     * 解析缩放范围
     */
    private static void parseScaleRange(Emitter emitter, String json) {
        Pattern scalePattern = Pattern.compile("\"initial_scale\"\\s*:\\s*\\{([^}]*)\\}");
        Matcher matcher = scalePattern.matcher(json);

        float minScale = 1.0f;
        float maxScale = 1.0f;

        if (matcher.find()) {
            String content = matcher.group(1);

            minScale = parseFloatValue(content, "min", 1.0f);
            maxScale = parseFloatValue(content, "max", 1.0f);
        }

        emitter.setScaleRange(minScale, maxScale);
    }

    /**
     * 解析发射器形状
     */
    private static void parseEmitterShape(Emitter emitter, String json) {
        // 查找shape配置
        Pattern shapePattern = Pattern.compile("\"shape\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = shapePattern.matcher(json);

        if (matcher.find()) {
            String shapeType = matcher.group(1);

            Emitter.EmitterShape shape;
            switch (shapeType.toLowerCase()) {
                case "disc":
                    shape = Emitter.EmitterShape.DISC;
                    break;
                case "box":
                    shape = Emitter.EmitterShape.BOX;
                    break;
                case "sphere":
                    shape = Emitter.EmitterShape.SPHERE;
                    break;
                case "cylinder":
                    shape = Emitter.EmitterShape.CYLINDER;
                    break;
                case "entity_aabb":
                    shape = Emitter.EmitterShape.ENTITY_AABB;
                    break;
                default:
                    shape = Emitter.EmitterShape.POINT;
            }

            emitter.setShape(shape);

            // 解析形状大小
            Pattern sizePattern = Pattern.compile("\"size\"\\s*:\\s*(\\d+\\.?\\d*)");
            Matcher sizeMatcher = sizePattern.matcher(json);
            if (sizeMatcher.find()) {
                emitter.setShapeSize(Float.parseFloat(sizeMatcher.group(1)));
            }
        }
    }

    /**
     * 解析发射器位置
     */
    private static void parseEmitterPosition(Emitter emitter, String json) {
        float x = parseFloatValue(json, "position_x", 0);
        float y = parseFloatValue(json, "position_y", 0);
        float z = parseFloatValue(json, "position_z", 0);
        emitter.setPosition(x, y, z);
    }

    /**
     * 解析发射器旋转
     */
    private static void parseEmitterRotation(Emitter emitter, String json) {
        float x = parseFloatValue(json, "rotation_x", 0);
        float y = parseFloatValue(json, "rotation_y", 0);
        float z = parseFloatValue(json, "rotation_z", 0);
        emitter.setRotation(x, y, z);
    }

    /**
     * 从JSON配置创建ParticleEffect
     */
    public static ParticleEffect createFromConfig(String effectId, String effectName,
                                                   Map<String, Object> config) throws ParseException {
        ParticleEffect effect = new ParticleEffect(effectId, effectName);

        try {
            // 基础配置
            if (config.containsKey("lifetime")) {
                effect.setLifetime(((Number) config.get("lifetime")).floatValue());
            }

            if (config.containsKey("max_particles")) {
                effect.setMaxParticles(((Number) config.get("max_particles")).intValue());
            }

            if (config.containsKey("texture")) {
                effect.setTextureFile((String) config.get("texture"));
            }

            // 创建发射器
            List<Map<String, Object>> emitterConfigs = (List<Map<String, Object>>) config.get("emitters");
            if (emitterConfigs != null) {
                for (int i = 0; i < emitterConfigs.size(); i++) {
                    Map<String, Object> emitterConfig = emitterConfigs.get(i);
                    Emitter emitter = parseEmitterConfig(emitterConfig, i);
                    effect.addEmitter(emitter);
                }
            }

            if (!effect.validate()) {
                throw new ParseException("Invalid particle effect configuration");
            }

            return effect;

        } catch (Exception e) {
            throw new ParseException("Failed to create effect from config: " + e.getMessage(), e);
        }
    }

    /**
     * 解析发射器配置
     */
    private static Emitter parseEmitterConfig(Map<String, Object> config, int index) {
        String emitterId = "emitter_" + index;
        String emitterName = (String) config.getOrDefault("name", "emitter_" + index);

        Emitter emitter = new Emitter(emitterId, emitterName);

        // 发射参数
        if (config.containsKey("emission_rate")) {
            emitter.setEmissionRate(((Number) config.get("emission_rate")).floatValue());
        }

        if (config.containsKey("lifetime")) {
            Map<String, Object> lifetime = (Map<String, Object>) config.get("lifetime");
            float minLife = ((Number) lifetime.getOrDefault("min", 1.0)).floatValue();
            float maxLife = ((Number) lifetime.getOrDefault("max", 1.0)).floatValue();
            emitter.setLifetimeRange(minLife, maxLife);
        }

        // 速度参数
        if (config.containsKey("initial_speed")) {
            Map<String, Object> speed = (Map<String, Object>) config.get("initial_speed");
            float minSpeedX = ((Number) speed.getOrDefault("min_x", 0.0)).floatValue();
            float maxSpeedX = ((Number) speed.getOrDefault("max_x", 0.0)).floatValue();
            float minSpeedY = ((Number) speed.getOrDefault("min_y", 0.0)).floatValue();
            float maxSpeedY = ((Number) speed.getOrDefault("max_y", 1.0)).floatValue();
            float minSpeedZ = ((Number) speed.getOrDefault("min_z", 0.0)).floatValue();
            float maxSpeedZ = ((Number) speed.getOrDefault("max_z", 0.0)).floatValue();

            emitter.setSpeedRange(minSpeedX, maxSpeedX, minSpeedY, maxSpeedY, minSpeedZ, maxSpeedZ);
        }

        // 形状参数
        if (config.containsKey("shape")) {
            String shapeType = (String) config.get("shape");
            try {
                emitter.setShape(Emitter.EmitterShape.valueOf(shapeType.toUpperCase()));
            } catch (IllegalArgumentException e) {
                emitter.setShape(Emitter.EmitterShape.POINT);
            }
        }

        return emitter;
    }

    /**
     * 辅助方法：提取JSON字符串值
     */
    private static String extractJsonStringValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * 辅助方法：从JSON字符串中提取数值
     */
    private static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return "";
        }

        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) {
            return "";
        }

        int valueStart = json.indexOf("\"", colonIndex) + 1;
        int valueEnd = json.indexOf("\"", valueStart);

        if (valueStart > colonIndex && valueEnd > valueStart) {
            return json.substring(valueStart, valueEnd);
        }

        return "";
    }

    /**
     * 辅助方法：计算JSON中某个元素的出现次数
     */
    private static int countJsonElements(String json, String elementName) {
        String searchKey = "\"" + elementName + "\"";
        int count = 0;
        int index = 0;

        while ((index = json.indexOf(searchKey, index)) != -1) {
            count++;
            index += searchKey.length();
        }

        return count;
    }

    /**
     * 辅助方法：解析浮点值
     */
    private static float parseFloatValue(String json, String key, float defaultValue) {
        String value = extractJsonValue(json, key);
        if (!value.isEmpty()) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    /**
     * 辅助方法：找到匹配的右花括号
     */
    private static int findMatchingBrace(String json, int openBraceIndex) {
        if (openBraceIndex < 0 || openBraceIndex >= json.length()) {
            return -1;
        }

        int count = 1;
        for (int i = openBraceIndex + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                count++;
            } else if (c == '}') {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 解析异常
     */
    public static class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }

        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
