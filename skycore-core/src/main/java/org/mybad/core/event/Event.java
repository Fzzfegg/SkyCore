package org.mybad.core.event;

/**
 * 事件基类
 * 所有事件都应继承此类
 */
public abstract class Event {
    private String eventType;
    private long timestamp;
    private boolean cancelled;

    public Event(String eventType) {
        this.eventType = eventType;
        this.timestamp = System.currentTimeMillis();
        this.cancelled = false;
    }

    /**
     * 获取事件类型
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * 获取事件时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 取消事件
     * 某些事件可被取消，取消后不会继续处理
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * 检查事件是否被取消
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * 检查事件是否可被取消
     */
    public boolean isCancellable() {
        return false;
    }
}
