package org.mybad.core.event;

import java.util.*;
import java.util.concurrent.*;

/**
 * 事件总线
 * 管理事件的发布和订阅
 */
public class EventBus {
    private final Map<String, List<EventListener>> listeners;
    private final Queue<Event> eventQueue;
    private final ExecutorService executorService;
    private boolean isProcessing;

    public EventBus() {
        this(null);
    }

    public EventBus(ExecutorService executorService) {
        this.listeners = new ConcurrentHashMap<>();
        this.eventQueue = new ConcurrentLinkedQueue<>();
        this.executorService = executorService != null ?
            executorService :
            Executors.newFixedThreadPool(2);
        this.isProcessing = false;
    }

    /**
     * 订阅事件
     */
    public void subscribe(String eventType, EventListener listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
            .add(listener);

        // 按优先级排序
        List<EventListener> listenerList = listeners.get(eventType);
        listenerList.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }

    /**
     * 取消订阅
     */
    public void unsubscribe(String eventType, EventListener listener) {
        List<EventListener> listenerList = listeners.get(eventType);
        if (listenerList != null) {
            listenerList.remove(listener);
            if (listenerList.isEmpty()) {
                listeners.remove(eventType);
            }
        }
    }

    /**
     * 取消某个类型的所有订阅
     */
    public void unsubscribeAll(String eventType) {
        listeners.remove(eventType);
    }

    /**
     * 发布事件（同步）
     */
    public void publish(Event event) {
        publish(event, false);
    }

    /**
     * 发布事件（可异步）
     */
    public void publish(Event event, boolean async) {
        if (async && executorService != null) {
            executorService.submit(() -> dispatchEvent(event));
        } else {
            dispatchEvent(event);
        }
    }

    /**
     * 发布事件到队列（批量处理）
     */
    public void enqueue(Event event) {
        eventQueue.offer(event);
    }

    /**
     * 处理队列中的所有事件
     */
    public void processQueue() {
        if (isProcessing) {
            return;
        }

        isProcessing = true;
        try {
            while (!eventQueue.isEmpty()) {
                Event event = eventQueue.poll();
                if (event != null) {
                    dispatchEvent(event);
                }
            }
        } finally {
            isProcessing = false;
        }
    }

    /**
     * 调度事件给所有监听器
     */
    private void dispatchEvent(Event event) {
        String eventType = event.getEventType();
        List<EventListener> listenerList = listeners.get(eventType);

        if (listenerList == null || listenerList.isEmpty()) {
            return;
        }

        for (EventListener listener : listenerList) {
            try {
                listener.onEvent(event);

                // 如果事件可被取消且已被取消，则停止处理
                if (event.isCancellable() && event.isCancelled()) {
                    break;
                }
            } catch (Exception e) {
                System.err.println("事件处理错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取指定事件类型的监听器数量
     */
    public int getListenerCount(String eventType) {
        List<EventListener> listenerList = listeners.get(eventType);
        return listenerList != null ? listenerList.size() : 0;
    }

    /**
     * 获取所有已注册的事件类型
     */
    public Set<String> getRegisteredEventTypes() {
        return new HashSet<>(listeners.keySet());
    }

    /**
     * 检查是否有指定类型的监听器
     */
    public boolean hasListeners(String eventType) {
        return listeners.containsKey(eventType) && !listeners.get(eventType).isEmpty();
    }

    /**
     * 清空所有监听器
     */
    public void clear() {
        listeners.clear();
        eventQueue.clear();
    }

    /**
     * 关闭事件总线（释放资源）
     */
    public void shutdown() {
        processQueue();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
    }

    /**
     * 获取队列大小
     */
    public int getQueueSize() {
        return eventQueue.size();
    }
}
