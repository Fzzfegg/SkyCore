package org.mybad.core.legacy;

/**
 * SkyCore - 高性能、功能完整的 Minecraft Bedrock 模型和粒子系统框架
 *
 * @version 1.0.0
 * @author SkyCore Contributors
 */
public class SkyCore {
    /**
     * SkyCore 版本号
     */
    public static final String VERSION = "1.0.0";

    /**
     * SkyCore 框架名称
     */
    public static final String NAME = "SkyCore";

    /**
     * 获取 SkyCore 版本信息
     *
     * @return 版本字符串
     */
    public static String getVersion() {
        return VERSION;
    }

    /**
     * 获取 SkyCore 完整标识
     *
     * @return 标识字符串
     */
    public static String getIdentifier() {
        return NAME + " " + VERSION;
    }

    private SkyCore() {
        // 私有构造函数，禁止实例化
    }
}
