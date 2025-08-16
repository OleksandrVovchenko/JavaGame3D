package dev.alphacrisp;


import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Game {
    private long window;
    private int width = 1280, height = 720;
    private boolean isFullscreen = true;
    private int windowedWidth = 1280;
    private int windowedHeight = 720;
    private int windowedX;
    private int windowedY;
    private int program;
    private int vaoCube;
    private int vboCube;
    private int uProjectionLoc, uViewLoc, uModelLoc;
    private final Vector3f camPos = new Vector3f(1.5f, 1.6f, 1.5f);
    private float yaw = 0f, pitch = 0f;
    private double lastMouseX, lastMouseY;
    private double lastTime;
    private int frames;
    private double fpsTime;
    private int fps;
    private int overlayProgram;
    private int vaoText, vboText;
    private int uOrthoLoc, uColorLoc;
    private FloatBuffer textBuffer;
    private static final int CHAR_W = 5, CHAR_H = 7, CHAR_SP = 1;
    private static final int GLYPH_ADV = CHAR_W + CHAR_SP;

    private static final Map<Character, int[]> FONT = new HashMap<>();
    static {
        FONT.put('0', new int[]{0b11111,0b10001,0b10011,0b10101,0b11001,0b10001,0b11111});
        FONT.put('1', new int[]{0b00100,0b01100,0b00100,0b00100,0b00100,0b00100,0b01110});
        FONT.put('2', new int[]{0b11111,0b00001,0b00001,0b11111,0b10000,0b10000,0b11111});
        FONT.put('3', new int[]{0b11111,0b00001,0b00001,0b01111,0b00001,0b00001,0b11111});
        FONT.put('4', new int[]{0b10001,0b10001,0b10001,0b11111,0b00001,0b00001,0b00001});
        FONT.put('5', new int[]{0b11111,0b10000,0b10000,0b11111,0b00001,0b00001,0b11111});
        FONT.put('6', new int[]{0b11111,0b10000,0b10000,0b11111,0b10001,0b10001,0b11111});
        FONT.put('7', new int[]{0b11111,0b00001,0b00010,0b00100,0b01000,0b10000,0b10000});
        FONT.put('8', new int[]{0b11111,0b10001,0b10001,0b11111,0b10001,0b10001,0b11111});
        FONT.put('9', new int[]{0b11111,0b10001,0b10001,0b11111,0b00001,0b00001,0b11111});
        FONT.put('F', new int[]{0b11111,0b10000,0b10000,0b11110,0b10000,0b10000,0b10000});
        FONT.put('P', new int[]{0b11110,0b10001,0b10001,0b11110,0b10000,0b10000,0b10000});
        FONT.put('S', new int[]{0b01111,0b10000,0b10000,0b01110,0b00001,0b00001,0b11110});
        FONT.put(':', new int[]{0b00000,0b00100,0b00000,0b00000,0b00000,0b00100,0b00000});
        FONT.put(' ', new int[]{0b00000,0b00000,0b00000,0b00000,0b00000,0b00000,0b00000});
    }

    private int wallTexture;
    private int floorTexture;

    private final int[][] map = new int[][]{
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1},
            {1, 0, 0, 1, 1, 1, 0, 0, 0, 1, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1},
            {1, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 1},
            {1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
    };

    public static void main(String[] args) {
        new Game().run();
    }

    public void run() {
        initWindow();
        initGL();
        loop();
        cleanup();
    }

    private void initWindow() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);

        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode vidMode = glfwGetVideoMode(monitor);
        if (vidMode == null) throw new RuntimeException("Failed to get video mode");

        width = vidMode.width();
        height = vidMode.height();

        window = glfwCreateWindow(width, height, "FPS-RPG (Modern OpenGL)", monitor, NULL);
        if (window == NULL) throw new RuntimeException("Failed to create window");

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            glfwGetMonitorPos(monitor, x, y);
            windowedX = x.get(0) + (vidMode.width() - windowedWidth) / 2;
            windowedY = y.get(0) + (vidMode.height() - windowedHeight) / 2;
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);
        GL.createCapabilities();

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        double[] px = new double[1];
        double[] py = new double[1];
        glfwGetCursorPos(window, px, py);
        lastMouseX = px[0];
        lastMouseY = py[0];

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_F11 && action == GLFW_PRESS) {
                toggleFullscreen();
            }
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                glfwSetWindowShouldClose(window, true);
            }
        });

        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            width = Math.max(1, w);
            height = Math.max(1, h);
            glViewport(0, 0, width, height);
        });

        glViewport(0, 0, width, height);
        glEnable(GL_DEPTH_TEST);
        glClearColor(0.12f, 0.14f, 0.18f, 1f);
    }

    private void toggleFullscreen() {
        isFullscreen = !isFullscreen;
        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode vidMode = glfwGetVideoMode(monitor);
        if (vidMode == null) return;

        if (isFullscreen) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer x = stack.mallocInt(1);
                IntBuffer y = stack.mallocInt(1);
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);

                glfwGetWindowPos(window, x, y);
                glfwGetWindowSize(window, w, h);

                windowedX = x.get(0);
                windowedY = y.get(0);
                windowedWidth = w.get(0);
                windowedHeight = h.get(0);
            }

            glfwSetWindowMonitor(window, monitor, 0, 0, vidMode.width(), vidMode.height(), vidMode.refreshRate());
        } else {
            glfwSetWindowMonitor(window, NULL, windowedX, windowedY, windowedWidth, windowedHeight, 0);
        }
    }

    private void initGL() {
        program = createProgram(VERT_SRC, FRAG_SRC);
        uProjectionLoc = glGetUniformLocation(program, "uProjection");
        uViewLoc = glGetUniformLocation(program, "uView");
        uModelLoc = glGetUniformLocation(program, "uModel");

        wallTexture = loadTexture("wall.png");
        floorTexture = loadTexture("floor.png");

        vaoCube = glGenVertexArrays();
        glBindVertexArray(vaoCube);
        vboCube = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboCube);
        FloatBuffer cubeBuf = BufferUtils.createFloatBuffer(CUBE_VERTICES.length);
        cubeBuf.put(CUBE_VERTICES).flip();
        glBufferData(GL_ARRAY_BUFFER, cubeBuf, GL_STATIC_DRAW);

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);

        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);

        glBindVertexArray(0);

        overlayProgram = createProgram(VERT2D_SRC, FRAG2D_SRC);
        uOrthoLoc = glGetUniformLocation(overlayProgram, "uOrtho");
        uColorLoc = glGetUniformLocation(overlayProgram, "uColor");

        vaoText = glGenVertexArrays();
        vboText = glGenBuffers();
        glBindVertexArray(vaoText);
        glBindBuffer(GL_ARRAY_BUFFER, vboText);
        glBufferData(GL_ARRAY_BUFFER, 8192L*2*Float.BYTES, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2*Float.BYTES, 0);
        glBindVertexArray(0);
        textBuffer = BufferUtils.createFloatBuffer(8192*2);
    }

    private void loop() {
        lastTime = glfwGetTime();
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - lastTime);
            lastTime = now;

            frames++;
            fpsTime += dt;
            if (fpsTime >= 1.0) {
                fps = frames;
                frames = 0;
                fpsTime -= 1.0;
            }

            handleInput(dt);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(70.0), (float) width / height, 0.1f, 200f);
            Matrix4f view = buildView();

            glUseProgram(program);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer fb = stack.mallocFloat(16);
                glUniformMatrix4fv(uProjectionLoc, false, proj.get(fb));
                fb.clear();
                glUniformMatrix4fv(uViewLoc, false, view.get(fb));
            }

            glActiveTexture(GL_TEXTURE0);
            glUniform1i(glGetUniformLocation(program, "uTexture"), 0);

            glBindTexture(GL_TEXTURE_2D, wallTexture);
            for (int z = 0; z < map.length; z++) {
                for (int x = 0; x < map[0].length; x++) {
                    if (map[z][x] == 1) {
                        drawCube(new Vector3f(x + 0.5f, 0.5f, z + 0.5f), new Vector3f(1f, 1f, 1f));
                    }
                }
            }

            glBindTexture(GL_TEXTURE_2D, floorTexture);
            for (int z = 0; z < map.length; z++) {
                for (int x = 0; x < map[0].length; x++) {
                    if (map[z][x] == 0) {
                        drawCube(new Vector3f(x + 0.5f, -0.05f, z + 0.5f), new Vector3f(1f, 0.1f, 1f));
                    }
                }
            }

            glUseProgram(0);

            drawFPSOverlay();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void drawFPSOverlay() {
        glDisable(GL_DEPTH_TEST);
        glUseProgram(overlayProgram);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            new Matrix4f().setOrtho2D(0, width, height, 0).get(fb);
            glUniformMatrix4fv(uOrthoLoc, false, fb);
        }
        glUniform3f(uColorLoc, 1, 1, 1);

        String text = "FPS: " + fps;
        int scale = 2;
        int textWidth = text.length() * GLYPH_ADV * scale;
        int x = 10;
        int y = 20;

        int verts = buildTextMesh(text, x, y, scale, textBuffer);

        glBindVertexArray(vaoText);
        glBindBuffer(GL_ARRAY_BUFFER, vboText);
        textBuffer.flip();
        glBufferSubData(GL_ARRAY_BUFFER, 0, textBuffer);
        glDrawArrays(GL_TRIANGLES, 0, verts);
        glBindVertexArray(0);
        textBuffer.clear();

        glUseProgram(0);
        glEnable(GL_DEPTH_TEST);
    }

    private int buildTextMesh(String text, int startX, int startY, int scale, FloatBuffer out) {
        int x = startX;
        int y = startY;
        int verts = 0;
        for (char c : text.toCharArray()) {
            int[] glyph = FONT.getOrDefault(c, FONT.get(' '));
            for (int row = 0; row < CHAR_H; row++) {
                int bits = glyph[row];
                for (int col = 0; col < CHAR_W; col++) {
                    if (((bits >> (CHAR_W - 1 - col)) & 1) == 0) continue;
                    float px = x + col * scale;
                    float py = y + row * scale;
                    float w = scale, h = scale;
                    out.put(px).put(py);
                    out.put(px + w).put(py);
                    out.put(px + w).put(py + h);
                    out.put(px).put(py);
                    out.put(px + w).put(py + h);
                    out.put(px).put(py + h);
                    verts += 6;
                }
            }
            x += GLYPH_ADV * scale;
        }
        return verts;
    }

    private Matrix4f buildView() {
        pitch = Math.max(-89f, Math.min(89f, pitch));

        float cy = (float) Math.cos(Math.toRadians(yaw));
        float sy = (float) Math.sin(Math.toRadians(yaw));
        float cp = (float) Math.cos(Math.toRadians(pitch));
        float sp = (float) Math.sin(Math.toRadians(pitch));

        Vector3f forward = new Vector3f(sy * cp, sp, -cy * cp).normalize();
        Vector3f target = new Vector3f(camPos).add(forward);
        Vector3f up = new Vector3f(0, 1, 0);
        return new Matrix4f().lookAt(camPos, target, up);
    }

    private void handleInput(float dt) {
        double[] mx = new double[1];
        double[] my = new double[1];
        glfwGetCursorPos(window, mx, my);
        float dx = (float) (mx[0] - lastMouseX);
        float dy = (float) (my[0] - lastMouseY);
        lastMouseX = mx[0];
        lastMouseY = my[0];

        float sens = 0.12f;
        yaw += dx * sens;
        pitch -= dy * sens;
        pitch = Math.max(-89f, Math.min(89f, pitch));

        float cy = (float) Math.cos(Math.toRadians(yaw));
        float sy = (float) Math.sin(Math.toRadians(yaw));
        float cp = (float) Math.cos(Math.toRadians(pitch));
        float sp = (float) Math.sin(Math.toRadians(pitch));

        Vector3f front = new Vector3f(sy * cp, sp, -cy * cp).normalize();
        Vector3f right = new Vector3f(front).cross(0, 1, 0).normalize();

        float speed = (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) ? 6f : 3.2f;
        float velocity = speed * dt;

        Vector3f nextPos = new Vector3f(camPos);

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) nextPos.add(new Vector3f(front).mul(velocity));
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) nextPos.sub(new Vector3f(front).mul(velocity));
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) nextPos.sub(new Vector3f(right).mul(velocity));
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) nextPos.add(new Vector3f(right).mul(velocity));

        float radius = 0.3f;
        if (!collides(nextPos.x, camPos.z, radius)) camPos.x = nextPos.x;
        if (!collides(camPos.x, nextPos.z, radius)) camPos.z = nextPos.z;

        camPos.y = 1.6f;

    }

    private boolean collides(float x, float z, float r) {
        float[][] probes = new float[][]{
                {x - r, z - r}, {x + r, z - r}, {x - r, z + r}, {x + r, z + r}
        };
        for (float[] p : probes) {
            int cx = (int) Math.floor(p[0]);
            int cz = (int) Math.floor(p[1]);
            if (cx < 0 || cz < 0 || cz >= map.length || cx >= map[0].length) return true;
            if (map[cz][cx] == 1) return true;
        }
        return false;
    }

    private void drawCube(Vector3f position, Vector3f scale) {
        Matrix4f model = new Matrix4f().translation(position).scale(scale);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            glUniformMatrix4fv(uModelLoc, false, model.get(fb));
        }

        glBindVertexArray(vaoCube);
        glDrawArrays(GL_TRIANGLES, 0, CUBE_VERTEX_COUNT);
        glBindVertexArray(0);
    }

    private void cleanup() {
        glDeleteVertexArrays(vaoCube);
        glDeleteBuffers(vboCube);
        glDeleteProgram(program);
        glDeleteTextures(wallTexture);
        glDeleteTextures(floorTexture);

        glDeleteVertexArrays(vaoText);
        glDeleteBuffers(vboText);
        glDeleteProgram(overlayProgram);

        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private static int createProgram(String vertSrc, String fragSrc) {
        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, vertSrc);
        glCompileShader(vs);
        if (glGetShaderi(vs, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException("Vertex shader error: \n" + glGetShaderInfoLog(vs));

        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, fragSrc);
        glCompileShader(fs);
        if (glGetShaderi(fs, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException("Fragment shader error: \n" + glGetShaderInfoLog(fs));

        int prog = glCreateProgram();
        glAttachShader(prog, vs);
        glAttachShader(prog, fs);
        glLinkProgram(prog);
        if (glGetProgrami(prog, GL_LINK_STATUS) == GL_FALSE)
            throw new RuntimeException("Program link error: \n" + glGetProgramInfoLog(prog));

        glDeleteShader(vs);
        glDeleteShader(fs);
        return prog;
    }

    private static int loadTexture(String path) {
        ByteBuffer imageBuffer;
        try {
            InputStream is = Game.class.getClassLoader().getResourceAsStream(path);
            if (is == null) {
                throw new IOException("Resource not found: " + path);
            }
            byte[] bytes = is.readAllBytes();
            imageBuffer = BufferUtils.createByteBuffer(bytes.length);
            imageBuffer.put(bytes).flip();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load texture file: " + path, e);
        }

        int id;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer image = STBImage.stbi_load_from_memory(imageBuffer, w, h, channels, 4);
            if (image == null) {
                throw new RuntimeException("Failed to load texture from memory: " + path + "\n" + STBImage.stbi_failure_reason());
            }

            id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w.get(0), h.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            glGenerateMipmap(GL_TEXTURE_2D);

            STBImage.stbi_image_free(image);
        }
        return id;
    }

    private static final float[] CUBE_VERTICES = {
            // back face
            -0.5f,-0.5f,-0.5f, 0f,0f,
            0.5f,-0.5f,-0.5f, 1f,0f,
            0.5f, 0.5f,-0.5f, 1f,1f,
            -0.5f,-0.5f,-0.5f, 0f,0f,
            0.5f, 0.5f,-0.5f, 1f,1f,
            -0.5f, 0.5f,-0.5f, 0f,1f,
            // front face
            -0.5f,-0.5f, 0.5f, 0f,0f,
            0.5f,-0.5f, 0.5f, 1f,0f,
            0.5f, 0.5f, 0.5f, 1f,1f,
            -0.5f,-0.5f, 0.5f, 0f,0f,
            0.5f, 0.5f, 0.5f, 1f,1f,
            -0.5f, 0.5f, 0.5f, 0f,1f,
            // left face
            -0.5f, 0.5f, 0.5f, 1f,1f,
            -0.5f, 0.5f,-0.5f, 0f,1f,
            -0.5f,-0.5f,-0.5f, 0f,0f,
            -0.5f,-0.5f,-0.5f, 0f,0f,
            -0.5f,-0.5f, 0.5f, 1f,0f,
            -0.5f, 0.5f, 0.5f, 1f,1f,
            // right face
            0.5f, 0.5f, 0.5f, 1f,1f,
            0.5f, 0.5f,-0.5f, 0f,1f,
            0.5f,-0.5f,-0.5f, 0f,0f,
            0.5f,-0.5f,-0.5f, 0f,0f,
            0.5f,-0.5f, 0.5f, 1f,0f,
            0.5f, 0.5f, 0.5f, 1f,1f,
            // bottom face
            -0.5f,-0.5f,-0.5f, 0f,1f,
            0.5f,-0.5f,-0.5f, 1f,1f,
            0.5f,-0.5f, 0.5f, 1f,0f,
            -0.5f,-0.5f,-0.5f, 0f,1f,
            0.5f,-0.5f, 0.5f, 1f,0f,
            -0.5f,-0.5f, 0.5f, 0f,0f,
            // top face
            -0.5f, 0.5f,-0.5f, 0f,1f,
            0.5f, 0.5f,-0.5f, 1f,1f,
            0.5f, 0.5f, 0.5f, 1f,0f,
            -0.5f, 0.5f,-0.5f, 0f,1f,
            0.5f, 0.5f, 0.5f, 1f,0f,
            -0.5f, 0.5f, 0.5f, 0f,0f
    };
    private static final int CUBE_VERTEX_COUNT = CUBE_VERTICES.length / 5;

    private static final String VERT_SRC = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec2 aTexCoord;

            out vec2 vTexCoord;

            uniform mat4 uProjection;
            uniform mat4 uView;
            uniform mat4 uModel;

            void main(){
                gl_Position = uProjection * uView * uModel * vec4(aPos, 1.0);
                vTexCoord = aTexCoord;
            }
            """;

    private static final String FRAG_SRC = """
            #version 330 core
            in vec2 vTexCoord;
            out vec4 FragColor;
            uniform sampler2D uTexture;
            void main(){
                FragColor = texture(uTexture, vTexCoord);
            }
            """;

    private static final String VERT2D_SRC = """
            #version 330 core
            layout (location=0) in vec2 aPos;
            uniform mat4 uOrtho;
            void main(){
                gl_Position = uOrtho * vec4(aPos,0.0,1.0);
            }
            """;

    private static final String FRAG2D_SRC = """
            #version 330 core
            out vec4 FragColor;
            uniform vec3 uColor;
            void main(){
                FragColor = vec4(uColor, 1.0);
            }
            """;
}