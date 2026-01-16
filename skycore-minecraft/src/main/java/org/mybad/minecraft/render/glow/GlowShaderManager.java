package org.mybad.minecraft.render.glow;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Loads and controls glow/bloom shader programs.
 */
public final class GlowShaderManager {
    public static final GlowShaderManager INSTANCE = new GlowShaderManager();

    private static final Logger LOGGER = LogManager.getLogger("SkyCore-GlowShader");
    private static final String SHADER_BASE = "/assets/skycore/shaders/";

    private final Map<Integer, Map<String, Integer>> uniformLocations = new HashMap<>();

    private boolean initialized;
    private boolean optifineChecked;
    private boolean optifineAvailable;
    private java.lang.reflect.Field optifineShaderPackLoadedField;

    private int programImage;
    private int programBlur;
    private int programBloom;
    private int programMask;

    private GlowShaderManager() {}

    public synchronized void init() {
        if (initialized) {
            return;
        }
        if (!OpenGlHelper.shadersSupported) {
            LOGGER.warn("[GlowShader] OpenGL shaders are not supported on this device");
            return;
        }
        try {
            int imageVert = loadShader(GL20.GL_VERTEX_SHADER, "image.vert");
            int imageFrag = loadShader(GL20.GL_FRAGMENT_SHADER, "image.frag");
            int blurFrag = loadShader(GL20.GL_FRAGMENT_SHADER, "blur.frag");
            int bloomFrag = loadShader(GL20.GL_FRAGMENT_SHADER, "bloom_combine.frag");
            int maskVert = loadShader(GL20.GL_VERTEX_SHADER, "mask.vert");
            int maskFrag = loadShader(GL20.GL_FRAGMENT_SHADER, "mask.frag");

            programImage = createProgram(imageVert, imageFrag);
            programBlur = createProgram(imageVert, blurFrag);
            programBloom = createProgram(imageVert, bloomFrag);
            programMask = createProgram(maskVert, maskFrag);
            initialized = true;
            LOGGER.info("[GlowShader] Programs compiled (image={}, blur={}, bloom={})", programImage, programBlur, programBloom);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("[GlowShader] Failed to initialize shaders", e);
            cleanup();
        }
    }

    public void cleanup() {
        deleteProgram(programImage);
        deleteProgram(programBlur);
        deleteProgram(programBloom);
        deleteProgram(programMask);
        uniformLocations.clear();
        initialized = false;
    }

    public boolean allowed() {
        return OpenGlHelper.shadersSupported && !isOptifineShaderActive();
    }

    public int getProgramImage() {
        return programImage;
    }

    public int getProgramBlur() {
        return programBlur;
    }

    public int getProgramBloom() {
        return programBloom;
    }

    public int getProgramMask() {
        return programMask;
    }

    public void renderFullscreen(Framebuffer target, int program, IntConsumer uniformSetup) {
        if (program <= 0) {
            return;
        }
        bindTarget(target);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL20.glUseProgram(program);
        if (uniformSetup != null) {
            uniformSetup.accept(program);
        }
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(-1f, -1f);
        GL11.glTexCoord2f(1f, 0f);
        GL11.glVertex2f(1f, -1f);
        GL11.glTexCoord2f(0f, 1f);
        GL11.glVertex2f(-1f, 1f);
        GL11.glTexCoord2f(1f, 1f);
        GL11.glVertex2f(1f, 1f);
        GL11.glEnd();
        GL20.glUseProgram(0);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        if (target == null) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }
    }

    public void setUniform1i(int program, String name, int value) {
        int location = getUniformLocation(program, name);
        if (location >= 0) {
            GL20.glUniform1i(location, value);
        }
    }

    public void setUniform1f(int program, String name, float value) {
        int location = getUniformLocation(program, name);
        if (location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    public void setUniform2f(int program, String name, float x, float y) {
        int location = getUniformLocation(program, name);
        if (location >= 0) {
            GL20.glUniform2f(location, x, y);
        }
    }

    public void setUniform3f(int program, String name, float x, float y, float z) {
        int location = getUniformLocation(program, name);
        if (location >= 0) {
            GL20.glUniform3f(location, x, y, z);
        }
    }

    private void bindTarget(Framebuffer target) {
        if (target != null) {
            target.bindFramebuffer(true);
            GL11.glViewport(0, 0, target.framebufferWidth, target.framebufferHeight);
        } else {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            Minecraft mc = Minecraft.getMinecraft();
            GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
        }
    }

    private int getUniformLocation(int program, String name) {
        if (program <= 0 || name == null) {
            return -1;
        }
        Map<String, Integer> perProgram = uniformLocations.computeIfAbsent(program, key -> new HashMap<>());
        Integer cached = perProgram.get(name);
        if (cached != null) {
            return cached;
        }
        int location = GL20.glGetUniformLocation(program, name);
        perProgram.put(name, location);
        return location;
    }

    private int loadShader(int type, String filename) throws IOException {
        String source = readShaderSource(filename);
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader, 1024);
            throw new IOException("Shader compile error (" + filename + "): " + log);
        }
        return shader;
    }

    private int createProgram(int vertexShader, int fragmentShader) {
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program, 1024);
            throw new RuntimeException("Failed to link shader program: " + log);
        }
        return program;
    }

    private void deleteProgram(int program) {
        if (program > 0) {
            GL20.glDeleteProgram(program);
        }
    }

    private String readShaderSource(String filename) throws IOException {
        String path = SHADER_BASE + filename;
        try (InputStream stream = GlowShaderManager.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Shader not found: " + path);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
                return builder.toString();
            }
        }
    }

    private boolean isOptifineShaderActive() {
        if (!optifineChecked) {
            optifineChecked = true;
            try {
                Class<?> shadersClass = Class.forName("net.optifine.shaders.Shaders");
                optifineShaderPackLoadedField = shadersClass.getDeclaredField("shaderPackLoaded");
                optifineShaderPackLoadedField.setAccessible(true);
                optifineAvailable = true;
            } catch (Throwable ignored) {
                optifineAvailable = false;
            }
        }
        if (!optifineAvailable || optifineShaderPackLoadedField == null) {
            return false;
        }
        try {
            return optifineShaderPackLoadedField.getBoolean(null);
        } catch (IllegalAccessException e) {
            return false;
        }
    }
}
