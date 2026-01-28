package org.mybad.core.binary.model;

import org.mybad.core.binary.BinaryDataReader;
import org.mybad.core.binary.BinaryDataWriter;
import org.mybad.core.binary.BinaryResourceSerializer;
import org.mybad.core.binary.BinaryResourceType;
import org.mybad.core.data.Model;
import org.mybad.core.data.ModelBone;
import org.mybad.core.data.ModelCube;
import org.mybad.core.data.ModelLocator;
import org.mybad.core.data.UVMapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializes {@link Model} into the SkyCore binary archive payload.
 */
public final class ModelBinarySerializer implements BinaryResourceSerializer<Model> {
    private static final int VERSION = 1;
    private static final String[] UV_FACES = {"north", "south", "east", "west", "up", "down"};

    @Override
    public BinaryResourceType getType() {
        return BinaryResourceType.MODEL;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public void write(BinaryDataWriter writer, Model model) throws IOException {
        if (model == null) {
            writer.writeString("");
            writer.writeVarInt(64);
            writer.writeVarInt(64);
            writer.writeVarInt(0);
            writer.writeVarInt(0);
            writer.writeVarInt(0);
            return;
        }

        writer.writeString(safe(model.getName()));
        writer.writeVarInt(model.getTextureWidth());
        writer.writeVarInt(model.getTextureHeight());

        List<ModelBone> bones = model.getBones();
        writer.writeVarInt(bones == null ? 0 : bones.size());
        if (bones != null) {
            for (ModelBone bone : bones) {
                writeBone(writer, bone);
            }
        }

        Map<String, ModelLocator> locatorMap = model.getLocators();
        List<ModelLocator> locators = new ArrayList<>();
        if (locatorMap != null) {
            locators.addAll(locatorMap.values());
            locators.sort((a, b) -> {
                String la = a == null ? null : a.getName();
                String lb = b == null ? null : b.getName();
                if (la == null && lb == null) {
                    return 0;
                }
                if (la == null) {
                    return -1;
                }
                if (lb == null) {
                    return 1;
                }
                return la.compareTo(lb);
            });
        }
        writer.writeVarInt(locators.size());
        for (ModelLocator locator : locators) {
            writeLocator(writer, locator);
        }

        // Placeholder for future constraint serialization (currently unused)
        writer.writeVarInt(0);
    }

    @Override
    public Model read(BinaryDataReader reader) throws IOException {
        String name = reader.readString();
        int textureWidth = reader.readVarInt();
        int textureHeight = reader.readVarInt();
        Model model = new Model(name == null || name.isEmpty() ? "model" : name);
        model.setTextureWidth(Math.max(1, textureWidth));
        model.setTextureHeight(Math.max(1, textureHeight));

        int boneCount = Math.max(0, reader.readVarInt());
        List<BoneStub> stubs = new ArrayList<>(boneCount);
        for (int i = 0; i < boneCount; i++) {
            stubs.add(readBone(reader));
        }

        Map<String, ModelBone> boneMap = new HashMap<>();
        for (BoneStub stub : stubs) {
            if (stub.bone != null) {
                model.addBone(stub.bone);
                boneMap.put(stub.bone.getName(), stub.bone);
            }
        }
        for (BoneStub stub : stubs) {
            if (stub.bone == null) {
                continue;
            }
            if (stub.parentName == null || stub.parentName.isEmpty()) {
                continue;
            }
            ModelBone parent = boneMap.get(stub.parentName);
            if (parent != null) {
                parent.addChild(stub.bone);
            }
        }

        int locatorCount = Math.max(0, reader.readVarInt());
        for (int i = 0; i < locatorCount; i++) {
            ModelLocator locator = readLocator(reader);
            if (locator != null && locator.getName() != null) {
                model.addLocator(locator.getName(), locator);
            }
        }

        int constraintCount = Math.max(0, reader.readVarInt());
        for (int i = 0; i < constraintCount; i++) {
            skipConstraint(reader);
        }
        return model;
    }

    private void writeBone(BinaryDataWriter writer, ModelBone bone) throws IOException {
        writer.writeString(safe(bone == null ? null : bone.getName()));
        String parentName = bone != null && bone.getParent() != null ? bone.getParent().getName() : "";
        writer.writeString(safe(parentName));
        writeVec3(writer, bone == null ? null : bone.getPivot());
        writeVec3(writer, bone == null ? null : bone.getPosition());
        writeVec3(writer, bone == null ? null : bone.getRotation());
        writeVec3(writer, bone == null ? null : bone.getSize());
        writer.writeBoolean(bone != null && bone.hasPositionOverride());
        writer.writeBoolean(bone != null && bone.isMirror());
        writer.writeBoolean(bone != null && bone.isNeverRender());
        writer.writeBoolean(bone != null && bone.isReset());

        List<ModelCube> cubes = bone == null ? null : bone.getCubes();
        writer.writeVarInt(cubes == null ? 0 : cubes.size());
        if (cubes != null) {
            for (ModelCube cube : cubes) {
                writeCube(writer, cube);
            }
        }
    }

    private BoneStub readBone(BinaryDataReader reader) throws IOException {
        String name = reader.readString();
        String parentName = reader.readString();
        ModelBone bone = new ModelBone(name == null || name.isEmpty() ? "bone" : name);
        bone.setPivot(readVec3(reader));
        bone.setPosition(readVec3(reader));
        bone.setRotation(readVec3(reader));
        bone.setSize(readVec3(reader));
        bone.setHasPositionOverride(reader.readBoolean());
        bone.setMirror(reader.readBoolean());
        bone.setNeverRender(reader.readBoolean());
        bone.setReset(reader.readBoolean());

        int cubeCount = Math.max(0, reader.readVarInt());
        for (int i = 0; i < cubeCount; i++) {
            ModelCube cube = readCube(reader);
            if (cube != null) {
                bone.addCube(cube);
            }
        }
        bone.captureBindPose();
        return new BoneStub(bone, parentName);
    }

    private void writeCube(BinaryDataWriter writer, ModelCube cube) throws IOException {
        writeVec3(writer, cube == null ? null : cube.getOrigin());
        writeVec3(writer, cube == null ? null : cube.getSize());
        writeVec3(writer, cube == null ? null : cube.getRotation());
        writeVec3(writer, cube == null ? null : cube.getPivot());
        writer.writeFloat(cube == null ? 0f : cube.getInflate());
        writer.writeBoolean(cube != null && cube.isMirror());
        writeUVMapping(writer, cube == null ? null : cube.getUV());
    }

    private ModelCube readCube(BinaryDataReader reader) throws IOException {
        ModelCube cube = new ModelCube();
        cube.setOrigin(readVec3(reader));
        cube.setSize(readVec3(reader));
        cube.setRotation(readVec3(reader));
        cube.setPivot(readVec3(reader));
        cube.setInflate(reader.readFloat());
        cube.setMirror(reader.readBoolean());
        UVMapping uv = readUVMapping(reader);
        if (uv != null) {
            cube.setUV(uv);
        }
        return cube;
    }

    private void writeLocator(BinaryDataWriter writer, ModelLocator locator) throws IOException {
        writer.writeString(locator == null ? "" : safe(locator.getName()));
        writer.writeString(locator == null ? "" : safe(locator.getAttachedBone()));
        writeVec3(writer, locator == null ? null : locator.getPosition());
        writeVec3(writer, locator == null ? null : locator.getRotation());
        writer.writeBoolean(locator != null && locator.isVisible());
    }

    private ModelLocator readLocator(BinaryDataReader reader) throws IOException {
        String name = reader.readString();
        String attached = reader.readString();
        float[] position = readVec3(reader);
        float[] rotation = readVec3(reader);
        boolean visible = reader.readBoolean();
        ModelLocator locator = new ModelLocator(name == null || name.isEmpty() ? "locator" : name);
        locator.setAttachedBone(attached == null || attached.isEmpty() ? null : attached);
        locator.setPosition(position);
        locator.setRotation(rotation);
        locator.setVisible(visible);
        return locator;
    }

    private void writeUVMapping(BinaryDataWriter writer, UVMapping uv) throws IOException {
        if (uv == null) {
            writer.writeByte(0);
            return;
        }
        float[] box = uv.getBoxUV();
        if (box != null) {
            writer.writeByte(1);
            writer.writeFloat(box[0]);
            writer.writeFloat(box[1]);
            return;
        }
        writer.writeByte(2);
        for (String face : UV_FACES) {
            float[] coords = uv.getFaceUV(face);
            if (coords == null || coords.length < 4) {
                writer.writeBoolean(false);
            } else {
                writer.writeBoolean(true);
                writer.writeFloat(coords[0]);
                writer.writeFloat(coords[1]);
                writer.writeFloat(coords[2]);
                writer.writeFloat(coords[3]);
            }
        }
    }

    private UVMapping readUVMapping(BinaryDataReader reader) throws IOException {
        int mode = reader.readByte();
        switch (mode) {
            case 0:
                return null;
            case 1: {
                float u = reader.readFloat();
                float v = reader.readFloat();
                return new UVMapping(u, v);
            }
            case 2: {
                UVMapping uv = new UVMapping();
                for (String face : UV_FACES) {
                    boolean present = reader.readBoolean();
                    if (!present) {
                        continue;
                    }
                    float u1 = reader.readFloat();
                    float v1 = reader.readFloat();
                    float u2 = reader.readFloat();
                    float v2 = reader.readFloat();
                    uv.setFaceUV(face, u1, v1, u2 - u1, v2 - v1);
                }
                return uv;
            }
            default:
                throw new IOException("Unknown UV mapping mode: " + mode);
        }
    }

    private void writeVec3(BinaryDataWriter writer, float[] values) throws IOException {
        float x = values != null && values.length > 0 ? values[0] : 0f;
        float y = values != null && values.length > 1 ? values[1] : 0f;
        float z = values != null && values.length > 2 ? values[2] : 0f;
        writer.writeFloat(x);
        writer.writeFloat(y);
        writer.writeFloat(z);
    }

    private float[] readVec3(BinaryDataReader reader) throws IOException {
        return new float[]{reader.readFloat(), reader.readFloat(), reader.readFloat()};
    }

    private void skipConstraint(BinaryDataReader reader) throws IOException {
        // Reserved for future use: currently constraints are not serialized.
        // For forward compatibility, consume a placeholder boolean flag.
        reader.readBoolean();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class BoneStub {
        private final ModelBone bone;
        private final String parentName;

        private BoneStub(ModelBone bone, String parentName) {
            this.bone = bone;
            this.parentName = parentName;
        }
    }
}
