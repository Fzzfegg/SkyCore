package org.mybad.minecraft.render.glow;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Utility that scans model parts and picks those whose names imply glow/bloom usage.
 */
public final class GlowModelDetector {
    public static final GlowModelDetector INSTANCE = new GlowModelDetector();

    private static final Pattern PRIMARY_PATTERN = Pattern.compile("(?i)glow");
    private static final List<Pattern> SECONDARY_PATTERNS = Arrays.asList(
            Pattern.compile("(?i)emissive"),
            Pattern.compile("(?i)light"),
            Pattern.compile("(?i)bloom")
    );

    private GlowModelDetector() {
    }

    public boolean shouldGlow(String partName) {
        if (partName == null || partName.isEmpty()) {
            return false;
        }
        if (PRIMARY_PATTERN.matcher(partName).find()) {
            return true;
        }
        for (Pattern pattern : SECONDARY_PATTERNS) {
            if (pattern.matcher(partName).find()) {
                return true;
            }
        }
        return false;
    }

    public List<ModelRenderer> filterGlowParts(ModelBase model) {
        if (model == null || model.boxList == null) {
            return Collections.emptyList();
        }
        List<ModelRenderer> result = new ArrayList<>();
        for (Object obj : model.boxList) {
            if (!(obj instanceof ModelRenderer)) {
                continue;
            }
            ModelRenderer renderer = (ModelRenderer) obj;
            if (renderer == null) {
                continue;
            }
            if (shouldGlow(renderer.boxName)) {
                result.add(renderer);
            }
        }
        return result;
    }

    public List<ModelRenderer> getAllParts(ModelRenderer renderer) {
        if (renderer == null) {
            return Collections.emptyList();
        }
        List<ModelRenderer> result = new ArrayList<>();
        result.add(renderer);
        if (renderer.childModels != null) {
            for (ModelRenderer child : renderer.childModels) {
                if (child == null) {
                    continue;
                }
                result.addAll(getAllParts(child));
            }
        }
        return result;
    }

    public List<ModelRenderer> filterGlowPartsRecursive(ModelBase model) {
        if (model == null || model.boxList == null) {
            return Collections.emptyList();
        }
        List<ModelRenderer> result = new ArrayList<>();
        for (Object obj : model.boxList) {
            if (!(obj instanceof ModelRenderer)) {
                continue;
            }
            ModelRenderer renderer = (ModelRenderer) obj;
            if (renderer == null) {
                continue;
            }
            for (ModelRenderer part : getAllParts(renderer)) {
                if (part == null) {
                    continue;
                }
                if (shouldGlow(part.boxName)) {
                    result.add(part);
                }
            }
        }
        // remove duplicates while preserving order
        Set<ModelRenderer> seen = new LinkedHashSet<>(result);
        return new ArrayList<>(seen);
    }
}

