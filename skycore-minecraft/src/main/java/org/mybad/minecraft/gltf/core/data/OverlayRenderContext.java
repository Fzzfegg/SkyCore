package org.mybad.minecraft.gltf.core.data;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Runtime flags describing which overlay triggers are currently satisfied.
 */
public class OverlayRenderContext {
    private boolean hoverModel;
    private boolean hoverBlock;
    private Set<String> hoveredNodes = Collections.emptySet();

    public OverlayRenderContext reset() {
        hoverModel = false;
        hoverBlock = false;
        hoveredNodes = Collections.emptySet();
        return this;
    }

    public OverlayRenderContext copyFrom(OverlayRenderContext other) {
        if (other == null) {
            return reset();
        }
        this.hoverModel = other.hoverModel;
        this.hoverBlock = other.hoverBlock;
        if (other.hoveredNodes == null || other.hoveredNodes.isEmpty()) {
            this.hoveredNodes = Collections.emptySet();
        } else {
            this.hoveredNodes = new HashSet<>(other.hoveredNodes);
        }
        return this;
    }

    public boolean isHoverModel() {
        return hoverModel;
    }

    public OverlayRenderContext setHoverModel(boolean hoverModel) {
        this.hoverModel = hoverModel;
        return this;
    }

    public boolean isHoverBlock() {
        return hoverBlock;
    }

    public OverlayRenderContext setHoverBlock(boolean hoverBlock) {
        this.hoverBlock = hoverBlock;
        return this;
    }

    public OverlayRenderContext setHoveredNodes(Set<String> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            this.hoveredNodes = Collections.emptySet();
        } else if (nodes instanceof HashSet) {
            this.hoveredNodes = new HashSet<>(nodes);
        } else {
            this.hoveredNodes = new HashSet<>(nodes);
        }
        return this;
    }

    public boolean isNodeHovered(String nodeName) {
        if (nodeName == null || nodeName.isEmpty()) {
            return false;
        }
        return hoveredNodes != null && hoveredNodes.contains(nodeName);
    }

    public Set<String> getHoveredNodes() {
        return hoveredNodes;
    }
}
