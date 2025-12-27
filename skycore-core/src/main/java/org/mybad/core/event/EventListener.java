package org.mybad.core.event;

/**
 * 事件监听器接口
 * 处理特定类型的事件
 */
public interface EventListener {
    /**
     * 处理事件
     * @param event 事件对象
     */
    void onEvent(Event event);

    /**
     * 获取监听的事件类型
     */
    String getEventType();

    /**
     * 获取监听器优先级
     * 优先级高的监听器会先被调用
     * 默认值为0
     */
    default int getPriority() {
        return 0;
    }
}
