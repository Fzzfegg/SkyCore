package org.mybad.minecraft.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import org.mybad.bedrockparticle.molang.api.MolangCompiler;
import org.mybad.bedrockparticle.molang.api.MolangExpression;
import org.mybad.bedrockparticle.molang.api.MolangRuntime;
import org.mybad.bedrockparticle.molang.api.exception.MolangRuntimeException;
import org.mybad.bedrockparticle.molang.api.exception.MolangSyntaxException;
import org.mybad.bedrockparticle.particle.ParticleMolangCompiler;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.skycoreproto.SkyCoreProto;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.math.RoundingMode;


public final class EntityHeadBarManager {

    private static final ThreadLocal<DecimalFormat> PERCENT_FORMAT = ThreadLocal.withInitial(() -> {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        DecimalFormat format = new DecimalFormat("0.##", symbols);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    });

    private final MolangCompiler molangCompiler = ParticleMolangCompiler.get();
    private final Map<String, MolangExpression> expressionCache = new HashMap<>();
    private final HeadBarMolangContext molangContext = new HeadBarMolangContext();
    private final MolangRuntime molangRuntime;
    private final List<QueuedHeadBar> renderQueue = new ArrayList<>();
    private volatile List<HeadBarDefinition> definitions = Collections.emptyList();

    EntityHeadBarManager() {
        MolangRuntime.Builder builder = MolangRuntime.runtime();
        molangContext.bind(builder);
        this.molangRuntime = builder.create();
        reload();
    }

    void reload() {
        expressionCache.clear();
        SkyCoreProto.HeadBarConfig config = HeadBarConfigStore.getConfig();
        if (config == null || config.getDefinitionsCount() == 0) {
            definitions = Collections.emptyList();
            return;
        }
        definitions = parseDefinitions(config);
    }

    public void beginFrame() {
        renderQueue.clear();
    }

    void queueHeadBar(EntityLivingBase entity,
                      EntityWrapperEntry entry,
                      double x, double y, double z,
                      float partialTicks) {
        if (entity == null || definitions.isEmpty()) {
            return;
        }
        HeadBarDefinition definition = matchDefinition(entity);
        if (definition == null || definition.layers.isEmpty()) {
            return;
        }
        double hp = Math.max(0.0, entity.getHealth());
        double maxHp = Math.max(0.001, entity.getMaxHealth());
        double hpPercent = MathHelper.clamp(hp / maxHp, 0.0, 1.0);

        double baseHeight = resolveBaseHeight(entity, entry);
        RenderState state = new RenderState(entity, hp, maxHp, hpPercent);
        renderQueue.add(new QueuedHeadBar(entity, definition, state, x, y, z, baseHeight));
    }

    public void renderQueued(float partialTicks) {
        if (renderQueue.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            renderQueue.clear();
            return;
        }
        FontRenderer fr = mc.fontRenderer;
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.enableDepth();
        GlStateManager.enablePolygonOffset();
        GlStateManager.doPolygonOffset(-3.0F, -3.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        for (QueuedHeadBar task : renderQueue) {
            if (task == null || task.definition == null || task.state == null || task.entity == null) {
                continue;
            }
            if (task.entity.isDead) {
                continue;
            }
            molangContext.update(task.state);
            double renderY = task.y + task.baseHeight + task.definition.offset;
            GlStateManager.pushMatrix();
            GlStateManager.translate(task.x, renderY, task.z);
            GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
            float scale = 0.025f * task.definition.scale;
            GlStateManager.scale(-scale, -scale, scale);
            GlStateManager.translate(0.0F, 0.0F, -0.05F);
            for (Layer layer : task.definition.layers) {
                layer.render(mc, fr, task.state, this::evaluateExpression, this::formatText);
            }
            GlStateManager.popMatrix();
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.disablePolygonOffset();
        GlStateManager.doPolygonOffset(0.0F, 0.0F);
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
        renderQueue.clear();
    }

    private double resolveBaseHeight(EntityLivingBase entity, EntityWrapperEntry entry) {
        double vanillaHeight = entity != null ? entity.height : 0.0d;
        if (entry == null || entry.mapping == null) {
            return vanillaHeight;
        }
        org.mybad.minecraft.config.EntityModelMapping mapping = entry.mapping;
        if (mapping.getRenderBoxHeight() > 0f) {
            return mapping.getRenderBoxHeight();
        }
        float scale = mapping.getModelScale();
        if (scale > 0f && vanillaHeight > 0d) {
            return vanillaHeight * scale;
        }
        return vanillaHeight;
    }

    private List<HeadBarDefinition> parseDefinitions(SkyCoreProto.HeadBarConfig config) {
        List<HeadBarDefinition> defs = new ArrayList<>();
        for (SkyCoreProto.HeadBarDefinition proto : config.getDefinitionsList()) {
            HeadBarDefinition def = HeadBarDefinition.fromProto(proto, this::compileExpression);
            if (def != null) {
                defs.add(def);
            }
        }
        return defs;
    }

    private HeadBarDefinition matchDefinition(EntityLivingBase entity) {
        String rawName = entity.hasCustomName()
            ? entity.getCustomNameTag()
            : entity.getDisplayName().getFormattedText();
        String plainName = TextFormatting.getTextWithoutFormattingCodes(rawName);
        if (plainName == null) {
            plainName = "";
        }
        String lower = plainName.toLowerCase(Locale.ROOT);
        for (HeadBarDefinition definition : definitions) {
            if (definition.matches(lower)) {
                return definition;
            }
        }
        return null;
    }

    private double evaluateExpression(MolangExpression expression, RenderState state) {
        if (expression == null) {
            return state.hpPercent;
        }
        try {
            return molangRuntime.resolve(expression);
        } catch (MolangRuntimeException ex) {
            SkyCoreMod.LOGGER.info("[SkyCore] HeadBar 表达式执行失败: {}", ex.getMessage());
            return state.hpPercent;
        }
    }

    private String formatText(String template, RenderState state) {
        if (template == null) {
            return "";
        }
        return template
            .replace("{current_hp}", String.valueOf(Math.round(state.hp)))
            .replace("{max_hp}", String.valueOf(Math.round(state.maxHp)))
            .replace("{hp_percent}", formatPercentValue(state.hpPercent))
            .replace("{hp_missing}", String.valueOf(Math.round(state.hpMissing)))
            .replace("{name}", state.entityName);
    }

    private String formatPercentValue(double ratio) {
        DecimalFormat format = PERCENT_FORMAT.get();
        return format.format(ratio * 100.0) + "%";
    }

    private MolangExpression compileExpression(String expr) {
        if (expr == null) {
            return null;
        }
        String trimmed = expr.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return expressionCache.computeIfAbsent(trimmed, key -> {
            try {
                return molangCompiler.compile(key);
            } catch (MolangSyntaxException ex) {
                return null;
            }
        });
    }

    private static final class RenderState {
        final EntityLivingBase entity;
        final double hp;
        final double maxHp;
        final double hpPercent;
        final double hpMissing;
        final String entityName;

        RenderState(EntityLivingBase entity, double hp, double maxHp, double hpPercent) {
            this.entity = entity;
            this.hp = hp;
            this.maxHp = maxHp;
            this.hpPercent = hpPercent;
            this.hpMissing = Math.max(0.0, maxHp - hp);
            String rawName = entity.hasCustomName()
                ? entity.getCustomNameTag()
                : entity.getDisplayName().getFormattedText();
            String stripped = TextFormatting.getTextWithoutFormattingCodes(rawName);
            this.entityName = stripped == null ? "" : stripped;
        }
    }

    private static abstract class Layer {
        final float offsetX;
        final float offsetY;

        Layer(float offsetX, float offsetY) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }

        abstract void render(Minecraft mc,
                             FontRenderer fontRenderer,
                             RenderState state,
                             BiFunction<MolangExpression, RenderState, Double> evaluator,
                             BiFunction<String, RenderState, String> formatter);
    }

    private static class ImageLayer extends Layer {
        final ResourceLocation texture;
        final float width;
        final float height;
        final int color;

        ImageLayer(ResourceLocation texture, float width, float height, float offsetX, float offsetY, int color) {
            super(offsetX, offsetY);
            this.texture = texture;
            this.width = width;
            this.height = height;
            this.color = color;
        }

        @Override
        void render(Minecraft mc, FontRenderer fontRenderer, RenderState state,
                    BiFunction<MolangExpression, RenderState, Double> evaluator,
                    BiFunction<String, RenderState, String> formatter) {
            if (texture == null) {
                return;
            }
            mc.getTextureManager().bindTexture(texture);
            float halfWidth = width / 2.0f;
            float halfHeight = height / 2.0f;
            GlStateManager.enableTexture2D();
            setGlColor(color);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
            float x = offsetX - halfWidth;
            float y = offsetY - halfHeight;
            buffer.pos(x, y + height, 0).tex(0, 1).endVertex();
            buffer.pos(x + width, y + height, 0).tex(1, 1).endVertex();
            buffer.pos(x + width, y, 0).tex(1, 0).endVertex();
            buffer.pos(x, y, 0).tex(0, 0).endVertex();
            tessellator.draw();
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
    }

    private static final class BarLayer extends ImageLayer {
        final MolangExpression progressExpression;
        final boolean anchorLeft;
        final Map<EntityLivingBase, Double> smoothValues = new WeakHashMap<>();

        BarLayer(ResourceLocation texture, float width, float height, float offsetX, float offsetY,
                 int color, MolangExpression progressExpression, boolean anchorLeft) {
            super(texture, width, height, offsetX, offsetY, color);
            this.progressExpression = progressExpression;
            this.anchorLeft = anchorLeft;
        }

        @Override
        void render(Minecraft mc, FontRenderer fontRenderer, RenderState state,
                    BiFunction<MolangExpression, RenderState, Double> evaluator,
                    BiFunction<String, RenderState, String> formatter) {
            double target = MathHelper.clamp(evaluator.apply(progressExpression, state), 0.0, 1.0);
            double ratio = smoothRatio(state, target);
            float filledWidth = (float) (width * ratio);
            if (filledWidth <= 0.001f || texture == null) {
                return;
            }
            mc.getTextureManager().bindTexture(texture);
            float xStart = anchorLeft ? offsetX - width / 2.0f : offsetX - filledWidth / 2.0f;
            float y = offsetY - height / 2.0f;
            GlStateManager.enableTexture2D();
            setGlColor(color);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(xStart, y + height, 0).tex(0, 1).endVertex();
            buffer.pos(xStart + filledWidth, y + height, 0).tex(ratio, 1).endVertex();
            buffer.pos(xStart + filledWidth, y, 0).tex(ratio, 0).endVertex();
            buffer.pos(xStart, y, 0).tex(0, 0).endVertex();
            tessellator.draw();
            GlStateManager.color(1f, 1f, 1f, 1f);
        }

        private double smoothRatio(RenderState state, double target) {
            EntityLivingBase entity = state != null ? state.entity : null;
            if (entity == null) {
                return target;
            }
            double current = smoothValues.getOrDefault(entity, target);
            double delta = target - current;
            double step = 0.12 * delta;
            double next = current + step;
            if (Math.abs(delta) < 0.002) {
                next = target;
            }
            smoothValues.put(entity, next);
            return next;
        }
    }

    private static final class TextLayer extends Layer {
        final String text;
        final int color;
        final boolean shadow;
        final float scale;

        TextLayer(String text, int color, boolean shadow, float scale, float offsetX, float offsetY) {
            super(offsetX, offsetY);
            this.text = text;
            this.color = color;
            this.shadow = shadow;
            this.scale = scale <= 0 ? 1.0f : scale;
        }

        @Override
        void render(Minecraft mc, FontRenderer fontRenderer, RenderState state,
                    BiFunction<MolangExpression, RenderState, Double> evaluator,
                    BiFunction<String, RenderState, String> formatter) {
            if (text == null || text.isEmpty()) {
                return;
            }
            String content = formatter.apply(text, state);
            GlStateManager.pushMatrix();
            GlStateManager.translate(offsetX, offsetY, 0);
            GlStateManager.scale(scale, scale, scale);
            int width = fontRenderer.getStringWidth(content);
            fontRenderer.drawString(content, -width / 2, -fontRenderer.FONT_HEIGHT / 2, color, shadow);
            GlStateManager.popMatrix();
        }
    }

    private static final class HeadBarDefinition {
        final List<String> nameKeywords;
        final float offset;
        final float scale;
        final List<Layer> layers;

        HeadBarDefinition(List<String> nameKeywords, float offset, float scale, List<Layer> layers) {
            this.nameKeywords = nameKeywords;
            this.offset = offset;
            this.scale = scale <= 0 ? 1.0f : scale;
            this.layers = layers;
        }

        boolean matches(String entityNameLower) {
            for (String keyword : nameKeywords) {
                if (entityNameLower.contains(keyword)) {
                    return true;
                }
            }
            return false;
        }

        static HeadBarDefinition fromProto(SkyCoreProto.HeadBarDefinition proto,
                                           Function<String, MolangExpression> expressionCompiler) {
            if (proto.getNameContainsCount() == 0 || proto.getLayersCount() == 0) {
                return null;
            }
            List<String> keywords = new ArrayList<>();
            proto.getNameContainsList().forEach(name ->
                keywords.add(TextFormatting.getTextWithoutFormattingCodes(name).toLowerCase(Locale.ROOT))
            );
            List<Layer> layers = new ArrayList<>();
            for (SkyCoreProto.HeadBarLayer layerProto : proto.getLayersList()) {
                Layer layer = parseLayer(layerProto, expressionCompiler);
                if (layer != null) {
                    layers.add(layer);
                }
            }
            if (layers.isEmpty()) {
                return null;
            }
            return new HeadBarDefinition(keywords, proto.getOffset(), proto.getScale(), layers);
        }

        private static Layer parseLayer(SkyCoreProto.HeadBarLayer proto,
                                        Function<String, MolangExpression> expressionCompiler) {
            ResourceLocation texture = parseTexture(proto.getTexture());
            int color = proto.getColor() != 0 ? proto.getColor() : 0xFFFFFFFF;
            float width = proto.getWidth() > 0 ? proto.getWidth() : (proto.getType() == SkyCoreProto.HeadBarLayer.LayerType.BAR ? 80f : 80f);
            float height = proto.getHeight() > 0 ? proto.getHeight() : defaultHeight(proto.getType());
            float offsetX = proto.getOffsetX();
            float offsetY = proto.getOffsetY();
            switch (proto.getType()) {
                case IMAGE:
                    return new ImageLayer(texture, width, height, offsetX, offsetY, color);
                case BAR:
                    MolangExpression expr = expressionCompiler.apply(
                        proto.getProgressExpr().isEmpty() ? "hp_percent" : proto.getProgressExpr());
                    return new BarLayer(texture, width, height, offsetX, offsetY,
                        color, expr, proto.getAnchorLeft());
                case TEXT:
                    float scale = proto.getTextScale() > 0 ? proto.getTextScale() : 1.0f;
                    boolean shadow = proto.getShadow();
                    return new TextLayer(proto.getText(), color, shadow, scale, offsetX, offsetY == 0 ? -10f : offsetY);
                default:
                    return null;
            }
        }

        private static float defaultHeight(SkyCoreProto.HeadBarLayer.LayerType type) {
            switch (type) {
                case IMAGE:
                    return 8f;
                case BAR:
                    return 6f;
                default:
                    return 0f;
            }
        }

        private static ResourceLocation parseTexture(String raw) {
            if (raw == null || raw.isEmpty()) {
                return null;
            }
            try {
                if (raw.contains(":")) {
                    return new ResourceLocation(raw);
                }
                return new ResourceLocation(SkyCoreMod.MOD_ID, raw);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private static void setGlColor(int argb) {
        float a = ((argb >> 24) & 0xFF) / 255.0f;
        float r = ((argb >> 16) & 0xFF) / 255.0f;
        float g = ((argb >> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;
        GlStateManager.color(r, g, b, a);
    }

    private static final class HeadBarMolangContext {
        private float hp;
        private float maxHp;
        private float hpPercent;
        private float hpMissing;

        void bind(MolangRuntime.Builder builder) {
            builder.setQuery("hp", () -> hp);
            builder.setQuery("current_hp", () -> hp);
            builder.setQuery("max_hp", () -> maxHp);
            builder.setQuery("hp_max", () -> maxHp);
            builder.setQuery("hp_percent", () -> hpPercent);
            builder.setQuery("hpPercent", () -> hpPercent);
            builder.setQuery("percent", () -> hpPercent);
            builder.setQuery("hp_missing", () -> hpMissing);
            builder.setQuery("missing_hp", () -> hpMissing);
            builder.setQuery("hpMissing", () -> hpMissing);
        }

        void update(RenderState state) {
            this.hp = (float) state.hp;
            this.maxHp = (float) state.maxHp;
            this.hpPercent = (float) state.hpPercent;
            this.hpMissing = (float) state.hpMissing;
        }
    }

    private static final class QueuedHeadBar {
        final EntityLivingBase entity;
        final HeadBarDefinition definition;
        final RenderState state;
        final double x;
        final double y;
        final double z;
        final double baseHeight;

        QueuedHeadBar(EntityLivingBase entity, HeadBarDefinition definition, RenderState state,
                      double x, double y, double z, double baseHeight) {
            this.entity = entity;
            this.definition = definition;
            this.state = state;
            this.x = x;
            this.y = y;
            this.z = z;
            this.baseHeight = baseHeight;
        }
    }
}
