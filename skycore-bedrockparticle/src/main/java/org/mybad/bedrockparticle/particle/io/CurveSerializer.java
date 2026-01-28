package org.mybad.bedrockparticle.particle.io;

import com.google.gson.JsonPrimitive;
import org.mybad.bedrockparticle.molang.api.MolangExpression;
import org.mybad.bedrockparticle.particle.ParticleData;
import org.mybad.bedrockparticle.particle.json.ParticleJsonTupleParser;
import org.mybad.core.binary.BinaryDataReader;
import org.mybad.core.binary.BinaryDataWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class CurveSerializer {
    private CurveSerializer() {}

    public static void writeCurve(BinaryDataWriter writer, ParticleData.Curve curve) throws IOException {
        if (curve == null) {
            writer.writeString("");
            return;
        }
        writer.writeString(curve.type().name());
        writer.writeString(expressionToString(curve.input()));
        writer.writeString(expressionToString(curve.horizontalRange()));
        ParticleData.CurveNode[] nodes = curve.nodes();
        writer.writeVarInt(nodes == null ? 0 : nodes.length);
        if (nodes != null) {
            for (ParticleData.CurveNode node : nodes) {
                writer.writeFloat(node.getTime());
                writer.writeString(expressionToString(node.getValue()));
            }
        }
    }

    public static ParticleData.Curve readCurve(BinaryDataReader reader) throws IOException {
        String typeName = reader.readString();
        if (typeName == null || typeName.isEmpty()) {
            return null;
        }
        ParticleData.CurveType type = ParticleData.CurveType.valueOf(typeName);
        MolangExpression input = parseExpression(reader.readString());
        MolangExpression range = parseExpression(reader.readString());
        int count = Math.max(0, reader.readVarInt());
        List<ParticleData.CurveNode> nodes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            float time = reader.readFloat();
            MolangExpression value = parseExpression(reader.readString());
            nodes.add(new ParticleData.CurveNode(time, value));
        }
        return new ParticleData.Curve(type, nodes.toArray(new ParticleData.CurveNode[0]), input, range);
    }

    private static String expressionToString(MolangExpression expression) {
        if (expression == null) {
            return "";
        }
        return expression.toString();
    }

    private static MolangExpression parseExpression(String text) {
        if (text == null || text.isEmpty()) {
            return MolangExpression.ZERO;
        }
        return ParticleJsonTupleParser.parseExpression(new JsonPrimitive(text), text);
    }
}
