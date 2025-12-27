package org.mybad.core.parsing;

import com.google.gson.*;
import org.mybad.core.constraint.*;
import org.mybad.core.data.*;
import org.mybad.core.exception.ParseException;

import java.util.*;

/**
 * 约束解析器
 * 负责从JSON中解析骨骼约束信息
 *
 * 支持的约束类型：
 * - rotation: 旋转约束
 * - scale: 缩放约束
 * - translation: 平移约束
 */
public class ConstraintParser {

    /**
     * 从JSON对象中解析约束列表
     */
    public List<Constraint> parseConstraints(JsonObject constraintsObj) throws ParseException {
        List<Constraint> constraints = new ArrayList<>();

        for (Map.Entry<String, JsonElement> entry : constraintsObj.entrySet()) {
            JsonElement elem = entry.getValue();
            if (elem.isJsonObject()) {
                Constraint constraint = parseConstraint(entry.getKey(), elem.getAsJsonObject());
                if (constraint != null) {
                    constraints.add(constraint);
                }
            }
        }

        return constraints;
    }

    /**
     * 解析单个约束
     */
    private Constraint parseConstraint(String constraintName, JsonObject constraintJson) throws ParseException {
        String type = ParseUtils.getString(constraintJson, "type", "rotation");
        String targetBone = ParseUtils.getString(constraintJson, "bone", "");
        String sourceBone = ParseUtils.getString(constraintJson, "source", "");

        if (ParseUtils.isEmpty(targetBone) || ParseUtils.isEmpty(sourceBone)) {
            throw new ParseException("约束必须指定bone和source: " + constraintName);
        }

        Constraint constraint = null;

        switch (type.toLowerCase()) {
            case "rotation":
                constraint = parseRotationConstraint(constraintName, targetBone, sourceBone, constraintJson);
                break;
            case "scale":
                constraint = parseScaleConstraint(constraintName, targetBone, sourceBone, constraintJson);
                break;
            case "translation":
            case "position":
                constraint = parseTranslationConstraint(constraintName, targetBone, sourceBone, constraintJson);
                break;
            default:
                System.out.println("警告：未知的约束类型 " + type);
        }

        return constraint;
    }

    /**
     * 解析旋转约束
     */
    private RotationConstraint parseRotationConstraint(String name, String targetBone, String sourceBone, JsonObject json) {
        RotationConstraint constraint = new RotationConstraint(name, targetBone, sourceBone);

        // 解析轴向选择
        String axis = ParseUtils.getString(json, "axis", "xyz");
        constraint.setConstrainX(axis.contains("x"));
        constraint.setConstrainY(axis.contains("y"));
        constraint.setConstrainZ(axis.contains("z"));

        // 解析强度
        float strength = ParseUtils.getFloat(json, "strength", 1.0f);
        constraint.setStrength(strength);

        // 解析范围限制
        if (json.has("min_x") && json.has("max_x")) {
            constraint.setRange("x",
                ParseUtils.getFloat(json, "min_x", -180),
                ParseUtils.getFloat(json, "max_x", 180)
            );
        }

        if (json.has("min_y") && json.has("max_y")) {
            constraint.setRange("y",
                ParseUtils.getFloat(json, "min_y", -180),
                ParseUtils.getFloat(json, "max_y", 180)
            );
        }

        if (json.has("min_z") && json.has("max_z")) {
            constraint.setRange("z",
                ParseUtils.getFloat(json, "min_z", -180),
                ParseUtils.getFloat(json, "max_z", 180)
            );
        }

        return constraint;
    }

    /**
     * 解析缩放约束
     */
    private ScaleConstraint parseScaleConstraint(String name, String targetBone, String sourceBone, JsonObject json) {
        ScaleConstraint constraint = new ScaleConstraint(name, targetBone, sourceBone);

        // 解析轴向选择
        String axis = ParseUtils.getString(json, "axis", "xyz");
        constraint.setConstrainX(axis.contains("x"));
        constraint.setConstrainY(axis.contains("y"));
        constraint.setConstrainZ(axis.contains("z"));

        // 解析强度
        float strength = ParseUtils.getFloat(json, "strength", 1.0f);
        constraint.setStrength(strength);

        // 解析范围限制
        if (json.has("min_x") && json.has("max_x")) {
            constraint.setRange("x",
                ParseUtils.getFloat(json, "min_x", 0),
                ParseUtils.getFloat(json, "max_x", 2)
            );
        }

        if (json.has("min_y") && json.has("max_y")) {
            constraint.setRange("y",
                ParseUtils.getFloat(json, "min_y", 0),
                ParseUtils.getFloat(json, "max_y", 2)
            );
        }

        if (json.has("min_z") && json.has("max_z")) {
            constraint.setRange("z",
                ParseUtils.getFloat(json, "min_z", 0),
                ParseUtils.getFloat(json, "max_z", 2)
            );
        }

        return constraint;
    }

    /**
     * 解析平移约束
     */
    private TranslationConstraint parseTranslationConstraint(String name, String targetBone, String sourceBone, JsonObject json) {
        TranslationConstraint constraint = new TranslationConstraint(name, targetBone, sourceBone);

        // 解析轴向选择
        String axis = ParseUtils.getString(json, "axis", "xyz");
        constraint.setConstrainX(axis.contains("x"));
        constraint.setConstrainY(axis.contains("y"));
        constraint.setConstrainZ(axis.contains("z"));

        // 解析强度
        float strength = ParseUtils.getFloat(json, "strength", 1.0f);
        constraint.setStrength(strength);

        // 解析范围限制
        if (json.has("min_x") && json.has("max_x")) {
            constraint.setRange("x",
                ParseUtils.getFloat(json, "min_x", -100),
                ParseUtils.getFloat(json, "max_x", 100)
            );
        }

        if (json.has("min_y") && json.has("max_y")) {
            constraint.setRange("y",
                ParseUtils.getFloat(json, "min_y", -100),
                ParseUtils.getFloat(json, "max_y", 100)
            );
        }

        if (json.has("min_z") && json.has("max_z")) {
            constraint.setRange("z",
                ParseUtils.getFloat(json, "min_z", -100),
                ParseUtils.getFloat(json, "max_z", 100)
            );
        }

        return constraint;
    }
}
