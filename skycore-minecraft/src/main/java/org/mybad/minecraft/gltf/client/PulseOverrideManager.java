package org.mybad.minecraft.gltf.client;

import org.mybad.minecraft.gltf.core.data.DataMaterial;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import java.util.function.Consumer;

final class PulseOverrideManager {

    interface Target {
        boolean applyAlpha(PulseOverrideCommand command, long now);
        boolean applyColor(ColorPulseOverrideCommand command, long now);
    }

    private final Target target;
    private final List<PulseOverrideCommand> pendingAlpha = new ArrayList<>();
    private final List<ColorPulseOverrideCommand> pendingColor = new ArrayList<>();

    PulseOverrideManager(Target target) {
        this.target = Objects.requireNonNull(target, "target");
    }

    void applyAlphaOverride(String materialName, String overlayId,
                            DataMaterial.OverlayLayer.PulseSettings pulse, long durationMs,
                            @Nullable Consumer<PulseOverrideCommand> onQueued) {
        PulseOverrideCommand command = PulseOverrideCommand.create(materialName, overlayId, pulse, durationMs);
        if (command == null) {
            return;
        }
        long now = System.currentTimeMillis();
        purgeExpired(now);
        pendingAlpha.removeIf(existing -> existing.matches(command));
        pendingAlpha.add(command);
        boolean applied = target.applyAlpha(command, now);
        if (!applied && onQueued != null) {
            onQueued.accept(command);
        }
        if (command.isExpired(now)) {
            pendingAlpha.remove(command);
        }
    }

    void applyColorOverride(String materialName, String overlayId,
                            DataMaterial.OverlayLayer.ColorPulseSettings pulse, long durationMs,
                            @Nullable Consumer<ColorPulseOverrideCommand> onQueued) {
        ColorPulseOverrideCommand command = ColorPulseOverrideCommand.create(materialName, overlayId, pulse, durationMs);
        if (command == null) {
            return;
        }
        long now = System.currentTimeMillis();
        purgeExpired(now);
        pendingColor.removeIf(existing -> existing.matches(command));
        pendingColor.add(command);
        boolean applied = target.applyColor(command, now);
        if (!applied && onQueued != null) {
            onQueued.accept(command);
        }
        if (command.isExpired(now)) {
            pendingColor.remove(command);
        }
    }

    void reapplyPending() {
        long now = System.currentTimeMillis();
        reapply(pendingAlpha, now, true);
        reapply(pendingColor, now, false);
    }

    void purgeExpired() {
        purgeExpired(System.currentTimeMillis());
    }

    private void purgeExpired(long now) {
        pendingAlpha.removeIf(command -> command == null || command.isExpired(now));
        pendingColor.removeIf(command -> command == null || command.isExpired(now));
    }

    private void reapply(List<?> list, long now, boolean alpha) {
        Iterator<?> iterator = list.iterator();
        while (iterator.hasNext()) {
            Object entry = iterator.next();
            if (entry == null) {
                iterator.remove();
                continue;
            }
            if (alpha) {
                PulseOverrideCommand command = (PulseOverrideCommand) entry;
                if (command.isExpired(now) || !target.applyAlpha(command, now)) {
                    iterator.remove();
                }
            } else {
                ColorPulseOverrideCommand command = (ColorPulseOverrideCommand) entry;
                if (command.isExpired(now) || !target.applyColor(command, now)) {
                    iterator.remove();
                }
            }
        }
    }

    static final class PulseOverrideCommand {
        private final String materialName;
        private final String overlayId;
        private final DataMaterial.OverlayLayer.PulseSettings pulse;
        private final long expireAtMs;

        private PulseOverrideCommand(String materialName, String overlayId,
                                     DataMaterial.OverlayLayer.PulseSettings pulse, long expireAtMs) {
            this.materialName = materialName;
            this.overlayId = overlayId;
            this.pulse = pulse;
            this.expireAtMs = expireAtMs;
        }

        static PulseOverrideCommand create(String materialName, String overlayId,
                                           DataMaterial.OverlayLayer.PulseSettings pulse, long durationMs) {
            if (materialName == null || overlayId == null || pulse == null) {
                return null;
            }
            String trimmedMaterial = materialName.trim();
            String trimmedOverlay = overlayId.trim();
            if (trimmedMaterial.isEmpty() || trimmedOverlay.isEmpty()) {
                return null;
            }
            DataMaterial.OverlayLayer.PulseSettings copy = new DataMaterial.OverlayLayer.PulseSettings();
            copy.copyFrom(pulse);
            long expireAt = durationMs > 0 ? System.currentTimeMillis() + durationMs : 0L;
            return new PulseOverrideCommand(trimmedMaterial, trimmedOverlay, copy, expireAt);
        }

        boolean matches(PulseOverrideCommand other) {
            if (other == null) {
                return false;
            }
            return materialName.equals(other.materialName) && overlayId.equals(other.overlayId);
        }

        boolean isExpired(long now) {
            return expireAtMs > 0 && now >= expireAtMs;
        }

        boolean apply(@Nullable org.mybad.minecraft.gltf.core.data.GltfRenderModel model, long now) {
            if (model == null || isExpired(now)) {
                return false;
            }
            long durationMs = expireAtMs > 0 ? Math.max(0L, expireAtMs - now) : 0L;
            model.applyOverlayPulseOverride(materialName, overlayId, pulse, durationMs);
            return true;
        }

        String getMaterialName() {
            return materialName;
        }

        String getOverlayId() {
            return overlayId;
        }
    }

    static final class ColorPulseOverrideCommand {
        private final String materialName;
        private final String overlayId;
        private final DataMaterial.OverlayLayer.ColorPulseSettings pulse;
        private final long expireAtMs;

        private ColorPulseOverrideCommand(String materialName, String overlayId,
                                          DataMaterial.OverlayLayer.ColorPulseSettings pulse, long expireAtMs) {
            this.materialName = materialName;
            this.overlayId = overlayId;
            this.pulse = pulse;
            this.expireAtMs = expireAtMs;
        }

        static ColorPulseOverrideCommand create(String materialName, String overlayId,
                                                DataMaterial.OverlayLayer.ColorPulseSettings pulse, long durationMs) {
            if (materialName == null || overlayId == null || pulse == null) {
                return null;
            }
            String trimmedMaterial = materialName.trim();
            String trimmedOverlay = overlayId.trim();
            if (trimmedMaterial.isEmpty() || trimmedOverlay.isEmpty()) {
                return null;
            }
            DataMaterial.OverlayLayer.ColorPulseSettings copy = new DataMaterial.OverlayLayer.ColorPulseSettings();
            copy.copyFrom(pulse);
            long expireAt = durationMs > 0 ? System.currentTimeMillis() + durationMs : 0L;
            return new ColorPulseOverrideCommand(trimmedMaterial, trimmedOverlay, copy, expireAt);
        }

        boolean matches(ColorPulseOverrideCommand other) {
            if (other == null) {
                return false;
            }
            return materialName.equals(other.materialName) && overlayId.equals(other.overlayId);
        }

        boolean isExpired(long now) {
            return expireAtMs > 0 && now >= expireAtMs;
        }

        boolean apply(@Nullable org.mybad.minecraft.gltf.core.data.GltfRenderModel model, long now) {
            if (model == null || isExpired(now)) {
                return false;
            }
            long durationMs = expireAtMs > 0 ? Math.max(0L, expireAtMs - now) : 0L;
            model.applyOverlayColorPulseOverride(materialName, overlayId, pulse, durationMs);
            return true;
        }

        String getMaterialName() {
            return materialName;
        }

        String getOverlayId() {
            return overlayId;
        }
    }
}
