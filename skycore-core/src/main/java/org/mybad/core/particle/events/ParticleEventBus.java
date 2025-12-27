package org.mybad.core.particle.events;

import org.mybad.core.event.Event;
import org.mybad.core.event.EventBus;
import org.mybad.core.event.EventListener;
import java.util.*;

/**
 * 粒子事件总线 - 管理和分发粒子系统事件
 * 与模型系统的EventBus兼容
 */
public class ParticleEventBus {

    private String busId;
    private EventBus eventBus;  // 共享的事件总线

    private Map<String, List<ParticleEventListener>> listeners;
    private Queue<Object> eventQueue;
    private boolean asyncMode = false;

    // 统计
    private long eventsDispatched = 0;
    private long listenersNotified = 0;

    public ParticleEventBus(String busId) {
        this.busId = busId;
        this.listeners = new HashMap<>();
        this.eventQueue = new LinkedList<>();
    }

    /**
     * 设置共享事件总线（与模型系统集成）
     */
    public void setSharedEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * 注册粒子事件监听器
     */
    public void subscribe(String eventType, ParticleEventListener listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    /**
     * 取消注册粒子事件监听器
     */
    public void unsubscribe(String eventType, ParticleEventListener listener) {
        List<ParticleEventListener> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
        }
    }

    /**
     * 发布粒子生成事件
     */
    public void publishSpawnEvent(ParticleSpawnEvent event) {
        dispatch("particle:spawn", event);
    }

    /**
     * 发布粒子死亡事件
     */
    public void publishDeathEvent(ParticleDeathEvent event) {
        dispatch("particle:death", event);
    }

    /**
     * 发布发射器启动事件
     */
    public void publishEmitterStartEvent(EmitterEvent.EmitterStartEvent event) {
        dispatch("emitter:start", event);
    }

    /**
     * 发布发射器停止事件
     */
    public void publishEmitterStopEvent(EmitterEvent.EmitterStopEvent event) {
        dispatch("emitter:stop", event);
    }

    /**
     * 发布发射器爆发事件
     */
    public void publishEmitterBurstEvent(EmitterEvent.EmitterBurstEvent event) {
        dispatch("emitter:burst", event);
    }

    /**
     * 分发事件
     */
    private void dispatch(String eventType, Object event) {
        if (asyncMode) {
            eventQueue.offer(event);
        } else {
            notifyListeners(eventType, event);
        }

        // 也分发到共享事件总线
        if (eventBus != null) {
            eventBus.publish(new ParticleSystemEvent(eventType, event));
        }

        eventsDispatched++;
    }

    /**
     * 通知所有监听器
     */
    private void notifyListeners(String eventType, Object event) {
        List<ParticleEventListener> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            for (ParticleEventListener listener : eventListeners) {
                try {
                    listener.onEvent(eventType, event);
                    listenersNotified++;
                } catch (Exception e) {
                    System.err.println("Error notifying listener: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 处理异步事件队列
     */
    public void processEventQueue() {
        while (!eventQueue.isEmpty()) {
            Object event = eventQueue.poll();
            String eventType = getEventType(event);
            notifyListeners(eventType, event);
        }
    }

    /**
     * 获取事件类型
     */
    private String getEventType(Object event) {
        if (event instanceof ParticleSpawnEvent) {
            return "particle:spawn";
        } else if (event instanceof ParticleDeathEvent) {
            return "particle:death";
        } else if (event instanceof EmitterEvent.EmitterStartEvent) {
            return "emitter:start";
        } else if (event instanceof EmitterEvent.EmitterStopEvent) {
            return "emitter:stop";
        } else if (event instanceof EmitterEvent.EmitterBurstEvent) {
            return "emitter:burst";
        }
        return "unknown";
    }

    /**
     * 设置异步模式
     */
    public void setAsyncMode(boolean async) {
        this.asyncMode = async;
    }

    /**
     * 清空事件队列
     */
    public void clearEventQueue() {
        eventQueue.clear();
    }

    /**
     * 获取统计信息
     */
    public String getStats() {
        return String.format("ParticleEventBus [%s, Dispatched: %d, Notified: %d, Queued: %d]",
                busId, eventsDispatched, listenersNotified, eventQueue.size());
    }

    // Getters
    public String getBusId() { return busId; }
    public long getEventsDispatched() { return eventsDispatched; }
    public long getListenersNotified() { return listenersNotified; }
    public int getQueueSize() { return eventQueue.size(); }

    /**
     * 粒子事件监听器接口
     */
    @FunctionalInterface
    public interface ParticleEventListener {
        void onEvent(String eventType, Object event);
    }

    /**
     * 包装事件用于共享事件总线
     */
    private static class ParticleSystemEvent extends Event {
        private String particleEventType;
        private Object particleEvent;

        public ParticleSystemEvent(String particleEventType, Object particleEvent) {
            super("particle:" + particleEventType);
            this.particleEventType = particleEventType;
            this.particleEvent = particleEvent;
        }

        public String getParticleEventType() { return particleEventType; }
        public Object getParticleEvent() { return particleEvent; }
    }

    @Override
    public String toString() {
        return getStats();
    }
}
