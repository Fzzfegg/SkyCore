package org.mybad.minecraft.render;

import java.lang.reflect.Field;

import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumType;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage;

/**
 * VertexFormat helper that stays compatible with OptiFine shader attributes.
 * Based on the approach used in mclib's VertexBuilder.
 */
public final class VertexFormatCompat {
    private static final VertexFormat[] CACHE = new VertexFormat[4];

    private static int entityAttrib = -1;
    private static int midTexCoordAttrib = -1;
    private static int tangentAttrib = -1;
    private static int velocityAttrib = -1;
    private static int midBlockAttrib = -1;

    static {
        try {
            Class<?> clazz = Class.forName("net.optifine.shaders.Shaders");

            Field fieldEntity = clazz.getField("entityAttrib");
            Field fieldMidTexCoord = clazz.getField("midTexCoordAttrib");
            Field fieldTangent = clazz.getField("tangentAttrib");

            entityAttrib = fieldEntity.getInt(null);
            midTexCoordAttrib = fieldMidTexCoord.getInt(null);
            tangentAttrib = fieldTangent.getInt(null);

            Field fieldVelocity = clazz.getField("velocityAttrib");
            Field fieldMidBlock = clazz.getField("midBlockAttrib");

            velocityAttrib = fieldVelocity.getInt(null);
            midBlockAttrib = fieldMidBlock.getInt(null);
        } catch (ClassNotFoundException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ignored) {
        }
    }

    public static VertexFormat getFormat(boolean color, boolean tex, boolean lightmap, boolean normal) {
        int index = (color ? 1 : 0) | (lightmap ? 1 : 0) << 1;

        if (CACHE[index] == null) {
            VertexFormat format = new VertexFormat();

            format.addElement(new VertexFormatElement(0, EnumType.FLOAT, EnumUsage.POSITION, 3));
            format.addElement(new VertexFormatElement(0, EnumType.UBYTE, color ? EnumUsage.COLOR : EnumUsage.PADDING, 4));
            format.addElement(new VertexFormatElement(0, EnumType.FLOAT, tex ? EnumUsage.UV : EnumUsage.PADDING, 2));
            format.addElement(new VertexFormatElement(lightmap ? 1 : 0, EnumType.SHORT, lightmap ? EnumUsage.UV : EnumUsage.PADDING, 2));
            format.addElement(new VertexFormatElement(0, EnumType.BYTE, normal ? EnumUsage.NORMAL : EnumUsage.PADDING, 3));
            format.addElement(new VertexFormatElement(0, EnumType.BYTE, EnumUsage.PADDING, 1));

            if (entityAttrib != -1) {
                if (velocityAttrib != -1) {
                    format.addElement(new VertexFormatElement(0, EnumType.BYTE, EnumUsage.PADDING, 4));
                }

                format.addElement(new VertexFormatElement(0, EnumType.FLOAT, EnumUsage.PADDING, 2));
                format.addElement(new VertexFormatElement(0, EnumType.SHORT, EnumUsage.PADDING, 4));
                format.addElement(new VertexFormatElement(0, EnumType.SHORT, EnumUsage.PADDING, 4));

                if (velocityAttrib != -1) {
                    format.addElement(new VertexFormatElement(0, EnumType.FLOAT, EnumUsage.PADDING, 3));
                }
            }

            CACHE[index] = format;
        }

        return CACHE[index];
    }

    private VertexFormatCompat() {
    }
}
