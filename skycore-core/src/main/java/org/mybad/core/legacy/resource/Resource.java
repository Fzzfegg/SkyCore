package org.mybad.core.legacy.resource;

/**
 * 资源接口
 * 定义所有可缓存资源的通用接口
 */
public interface Resource {
    /**
     * 获取资源标识符
     */
    String getResourceId();

    /**
     * 获取资源类型
     */
    String getResourceType();

    /**
     * 资源是否被加载
     */
    boolean isLoaded();

    /**
     * 加载资源
     */
    void load() throws Exception;

    /**
     * 卸载资源（释放内存）
     */
    void unload();

    /**
     * 获取资源大小（字节）
     */
    long getSize();

    /**
     * 资源是否可复用
     */
    boolean isReusable();
}
