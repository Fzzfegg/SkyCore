package org.mybad.core.utils;

import java.util.*;

/**
 * 性能基准测试工具
 * 用于测量和分析代码性能
 */
public class PerformanceBenchmark {
    private String name;
    private List<Long> measurements;
    private long startTime;

    public PerformanceBenchmark(String name) {
        this.name = name;
        this.measurements = new ArrayList<>();
    }

    /**
     * 开始计时
     */
    public void start() {
        this.startTime = System.nanoTime();
    }

    /**
     * 结束计时并记录
     */
    public long end() {
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        measurements.add(duration);
        return duration;
    }

    /**
     * 结束计时并返回毫秒
     */
    public double endInMillis() {
        return end() / 1_000_000.0;
    }

    /**
     * 执行多次测试
     */
    public BenchmarkResult benchmark(Runnable task, int iterations) {
        measurements.clear();

        // 预热
        for (int i = 0; i < 10; i++) {
            task.run();
        }

        // 实际测试
        for (int i = 0; i < iterations; i++) {
            start();
            task.run();
            end();
        }

        return new BenchmarkResult(this);
    }

    /**
     * 基准测试结果
     */
    public static class BenchmarkResult {
        private PerformanceBenchmark benchmark;
        private long minTime;
        private long maxTime;
        private double avgTime;
        private double medianTime;
        private long totalTime;

        public BenchmarkResult(PerformanceBenchmark benchmark) {
            this.benchmark = benchmark;
            calculate();
        }

        private void calculate() {
            if (benchmark.measurements.isEmpty()) {
                return;
            }

            this.minTime = benchmark.measurements.stream().mapToLong(Long::longValue).min().orElse(0);
            this.maxTime = benchmark.measurements.stream().mapToLong(Long::longValue).max().orElse(0);
            this.totalTime = benchmark.measurements.stream().mapToLong(Long::longValue).sum();
            this.avgTime = (double) totalTime / benchmark.measurements.size();

            // 计算中位数
            List<Long> sorted = new ArrayList<>(benchmark.measurements);
            Collections.sort(sorted);
            int mid = sorted.size() / 2;
            this.medianTime = sorted.size() % 2 == 0 ?
                (sorted.get(mid - 1) + sorted.get(mid)) / 2.0 :
                sorted.get(mid);
        }

        public long getMinTimeNanos() { return minTime; }
        public long getMaxTimeNanos() { return maxTime; }
        public double getAvgTimeNanos() { return avgTime; }
        public double getMedianTimeNanos() { return medianTime; }
        public long getTotalTimeNanos() { return totalTime; }

        public double getMinTimeMillis() { return minTime / 1_000_000.0; }
        public double getMaxTimeMillis() { return maxTime / 1_000_000.0; }
        public double getAvgTimeMillis() { return avgTime / 1_000_000.0; }
        public double getMedianTimeMillis() { return medianTime / 1_000_000.0; }
        public double getTotalTimeMillis() { return totalTime / 1_000_000.0; }

        public int getIterationCount() {
            return benchmark.measurements.size();
        }

        @Override
        public String toString() {
            return String.format(
                "基准测试结果 [%s]:\n" +
                "  迭代次数: %d\n" +
                "  最小耗时: %.3f ms\n" +
                "  最大耗时: %.3f ms\n" +
                "  平均耗时: %.3f ms\n" +
                "  中位耗时: %.3f ms\n" +
                "  总耗时: %.3f ms",
                benchmark.name,
                getIterationCount(),
                getMinTimeMillis(),
                getMaxTimeMillis(),
                getAvgTimeMillis(),
                getMedianTimeMillis(),
                getTotalTimeMillis()
            );
        }
    }

    /**
     * 获取测量数据
     */
    public List<Long> getMeasurements() {
        return new ArrayList<>(measurements);
    }

    /**
     * 清空测量数据
     */
    public void clear() {
        measurements.clear();
    }

    /**
     * 获取基准测试名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取测量次数
     */
    public int getCount() {
        return measurements.size();
    }
}
