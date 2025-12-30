package org.mybad.minecraft.particle;

import org.mybad.core.particle.ParticleEffect;
import org.mybad.core.particle.ParticleSystem;
import org.mybad.core.particle.render.ParticleRenderer;
import org.mybad.minecraft.resource.ResourceLoader;

/**
 * Minecraft 端粒子引擎（暴雪粒子）
 * 负责加载、更新和渲染粒子系统
 */
public class ParticleEngine {
    private final ResourceLoader resourceLoader;
    private final ParticleSystem particleSystem;
    private final ParticleRenderer renderer;
    private final MinecraftParticleRenderer processor;
    private long lastUpdateTime;

    public ParticleEngine(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.particleSystem = new ParticleSystem("skycore", "SkyCore ParticleSystem");
        this.renderer = new ParticleRenderer("skycore", particleSystem);
        this.processor = new MinecraftParticleRenderer(resourceLoader);
        this.renderer.registerProcessor(processor);
        this.particleSystem.start();
    }

    public void playEffect(String effectPath, double x, double y, double z) {
        if (effectPath == null || effectPath.isEmpty()) {
            return;
        }
        ParticleEffect effect = resourceLoader.loadParticleEffect(effectPath);
        if (effect == null) {
            return;
        }
        particleSystem.registerEffect(effect);
        if (!particleSystem.isRunning()) {
            particleSystem.start();
        }
        particleSystem.playEffect(effect.getEffectId(), (float) x, (float) y, (float) z);
    }

    public void update() {
        long now = System.currentTimeMillis();
        if (lastUpdateTime == 0L) {
            lastUpdateTime = now;
            return;
        }
        float delta = (now - lastUpdateTime) / 1000.0f;
        if (delta > 0.1f) {
            delta = 0.1f;
        } else if (delta < 0f) {
            delta = 0f;
        }
        particleSystem.update(delta);
        lastUpdateTime = now;
    }

    public void render(double cameraX, double cameraY, double cameraZ) {
        processor.setCameraPosition((float) cameraX, (float) cameraY, (float) cameraZ);
        processor.begin();
        renderer.render((float) cameraX, (float) cameraY, (float) cameraZ);
        processor.end();
    }

    public void updateAndRender(double cameraX, double cameraY, double cameraZ) {
        update();
        render(cameraX, cameraY, cameraZ);
    }

    public void clear() {
        particleSystem.clear();
        lastUpdateTime = 0L;
    }
}
