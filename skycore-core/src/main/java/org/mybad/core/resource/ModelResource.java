package org.mybad.core.resource;

import org.mybad.core.data.*;
import org.mybad.core.parsing.*;

/**
 * 模型资源
 * 包装基岩模型并支持加载和卸载
 */
public class ModelResource implements Resource {
    private String resourceId;
    private Model model;
    private String jsonContent;
    private boolean loaded;

    public ModelResource(String resourceId, String jsonContent) {
        this.resourceId = resourceId;
        this.jsonContent = jsonContent;
        this.model = null;
        this.loaded = false;
    }

    public ModelResource(String resourceId, Model model) {
        this.resourceId = resourceId;
        this.model = model;
        this.jsonContent = null;
        this.loaded = true;
    }

    @Override
    public String getResourceId() {
        return resourceId;
    }

    @Override
    public String getResourceType() {
        return "Model";
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public void load() throws Exception {
        if (loaded) {
            return;
        }

        if (jsonContent == null) {
            throw new IllegalStateException("无法加载模型：JSON内容为空");
        }

        try {
            ModelParser parser = new ModelParser();
            model = parser.parse(jsonContent);
            loaded = true;
        } catch (org.mybad.core.exception.ParseException e) {
            throw new Exception("模型解析失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void unload() {
        model = null;
        jsonContent = null;
        loaded = false;
    }

    @Override
    public long getSize() {
        if (model == null) {
            return jsonContent != null ? jsonContent.length() : 0;
        }

        // 估算模型大小
        long size = 100;  // 基础对象开销
        size += model.getBones().size() * 500;  // 每个骨骼~500字节
        for (ModelBone bone : model.getBones()) {
            size += bone.getCubes().size() * 200;  // 每个立方体~200字节
        }
        return size;
    }

    @Override
    public boolean isReusable() {
        return true;
    }

    /**
     * 获取模型
     */
    public Model getModel() {
        return model;
    }
}
