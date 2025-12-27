package org.mybad.core.particle.optimization;

import org.mybad.core.particle.*;

/**
 * 粒子系统性能基准测试
 * 测试不同粒子数量和配置下的性能
 * 目标：
 * - 1000粒子 @ <5ms
 * - 5000粒子 @ <10ms
 * - 10000粒子 @ <15ms
 */
public class PerformanceBenchmark {

    private ParticleSystem particleSystem;
    private String benchmarkName;

    // 基准目标（毫秒）
    private static final float TARGET_1K = 5.0f;
    private static final float TARGET_5K = 10.0f;
    private static final float TARGET_10K = 15.0f;

    // 测试结果
    private BenchmarkResult result;

    public PerformanceBenchmark(String benchmarkName, ParticleSystem particleSystem) {
        this.benchmarkName = benchmarkName;
        this.particleSystem = particleSystem;
        this.result = new BenchmarkResult();
    }

    /**
     * 运行完整的基准测试
     */
    public BenchmarkResult runFullBenchmark() {
        result = new BenchmarkResult();
        result.benchmarkName = benchmarkName;

        // 测试1K粒子
        testParticleCount(1000, TARGET_1K);

        // 测试5K粒子
        testParticleCount(5000, TARGET_5K);

        // 测试10K粒子
        testParticleCount(10000, TARGET_10K);

        return result;
    }

    /**
     * 测试特定粒子数量
     */
    private void testParticleCount(int particleCount, float targetTime) {
        // 创建粒子效果
        ParticleEffect effect = new ParticleEffect("benchmark_effect", "BenchmarkEffect");
        Emitter emitter = new Emitter("benchmark_emitter", "BenchmarkEmitter");
        emitter.setEmissionRate(particleCount);
        emitter.setLifetimeRange(2.0f, 2.0f);
        emitter.setSpeedRange(0, 1, 0, 1, 0, 1);
        effect.addEmitter(emitter);

        particleSystem.registerEffect(effect);
        particleSystem.start();
        particleSystem.playEffect("benchmark_effect", 0, 0, 0);

        // 预热几帧
        for (int i = 0; i < 5; i++) {
            particleSystem.update(0.016f);
        }

        // 实际测试
        long startTime = System.nanoTime();
        int frameCount = 0;
        float totalFrameTime = 0;

        while (particleSystem.getActiveParticleCount() > 0 && frameCount < 60) {
            long frameStart = System.nanoTime();
            particleSystem.update(0.016f);
            long frameEnd = System.nanoTime();

            float frameTime = (frameEnd - frameStart) / 1_000_000.0f; // 转换为毫秒
            totalFrameTime += frameTime;
            frameCount++;
        }

        long endTime = System.nanoTime();
        float totalTime = (endTime - startTime) / 1_000_000.0f;
        float averageFrameTime = totalFrameTime / frameCount;

        // 记录结果
        BenchmarkResult.TestResult testResult = new BenchmarkResult.TestResult();
        testResult.particleCount = particleCount;
        testResult.frameCount = frameCount;
        testResult.totalTime = totalTime;
        testResult.averageFrameTime = averageFrameTime;
        testResult.targetTime = targetTime;
        testResult.passed = averageFrameTime <= targetTime;
        testResult.peakParticles = particleSystem.getActiveParticleCount();

        result.addTestResult(testResult);

        // 清理
        particleSystem.clear();
        particleSystem.stop();
    }

    /**
     * 测试单帧性能
     */
    public float testSingleFrame(int particleCount) {
        // 创建测试效果
        ParticleEffect effect = new ParticleEffect("test_effect", "TestEffect");
        Emitter emitter = new Emitter("test_emitter", "TestEmitter");
        emitter.setEmissionRate(particleCount);
        effect.addEmitter(emitter);

        particleSystem.registerEffect(effect);
        particleSystem.start();
        particleSystem.playEffect("test_effect", 0, 0, 0);

        // 等待粒子生成
        for (int i = 0; i < 2; i++) {
            particleSystem.update(0.016f);
        }

        // 测试一帧
        long startTime = System.nanoTime();
        particleSystem.update(0.016f);
        long endTime = System.nanoTime();

        float frameTime = (endTime - startTime) / 1_000_000.0f;

        // 清理
        particleSystem.clear();
        particleSystem.stop();

        return frameTime;
    }

    /**
     * 获取基准结果
     */
    public BenchmarkResult getResult() {
        return result;
    }

    /**
     * 打印基准报告
     */
    public void printReport() {
        System.out.println("\n========== 性能基准测试报告 ==========");
        System.out.println("测试名称: " + benchmarkName);
        System.out.println();

        for (BenchmarkResult.TestResult test : result.testResults) {
            System.out.println("粒子数: " + test.particleCount);
            System.out.println("  帧数: " + test.frameCount);
            System.out.println("  总时间: " + String.format("%.2f", test.totalTime) + "ms");
            System.out.println("  平均帧时: " + String.format("%.2f", test.averageFrameTime) + "ms");
            System.out.println("  目标时间: " + String.format("%.2f", test.targetTime) + "ms");
            System.out.println("  状态: " + (test.passed ? "✓ 通过" : "✗ 失败"));
            System.out.println("  峰值粒子数: " + test.peakParticles);
            System.out.println();
        }

        System.out.println("整体成功率: " + result.getPassRate() + "%");
        System.out.println("=====================================\n");
    }

    /**
     * 基准结果类
     */
    public static class BenchmarkResult {
        public String benchmarkName;
        public java.util.List<TestResult> testResults = new java.util.ArrayList<>();

        public void addTestResult(TestResult result) {
            testResults.add(result);
        }

        public int getPassRate() {
            if (testResults.isEmpty()) return 0;
            long passCount = testResults.stream().filter(t -> t.passed).count();
            return (int) ((passCount * 100) / testResults.size());
        }

        /**
         * 单个测试结果
         */
        public static class TestResult {
            public int particleCount;
            public int frameCount;
            public float totalTime;
            public float averageFrameTime;
            public float targetTime;
            public boolean passed;
            public int peakParticles;
        }
    }

    @Override
    public String toString() {
        return "PerformanceBenchmark [" + benchmarkName + "]";
    }
}
