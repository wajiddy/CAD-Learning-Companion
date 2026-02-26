package com.example.cadtrainingcompanion;


import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyRenderer implements GLSurfaceView.Renderer {

    // Rotation (disabled while editMode = true)
    public float angleX = 20f;
    public float angleY = 30f;

    // Toggles
    public boolean showVertices = true;
    public boolean editMode = false;

    // Viewport for picking
    private int vpW = 1, vpH = 1;

    // Matrices
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Programs
    private int faceProgram;
    private int pointProgram;

    // Shape data (editable vertices + face indices)
    private float[] verts;       // xyz xyz xyz...
    private short[] faces;       // triplets of vertex indices
    private FloatBuffer triBuffer;    // expanded triangles
    private FloatBuffer pointBuffer;  // points (unique vertices)

    private int selectedVertex = -1;

    // Simple move scale for dragging (tweak if you want faster/slower)
    private static final float DRAG_SCALE = 0.0025f;

    public MyRenderer(int shape) {
        buildShape(shape);
    }

    public void clearSelection() {
        selectedVertex = -1;
    }

    // ---------- Shape definitions ----------
    private void buildShape(int shape) {
        if (shape == ShapeSelectActivity.SHAPE_TRIANGLE) {
            // Pyramid: 4 vertices (apex + 3 base)
            verts = new float[] {
                    0f,  0.65f, 0f,     // 0 apex
                    -0.5f, -0.5f, 0.5f,  // 1
                    0.5f, -0.5f, 0.5f,   // 2
                    0f, -0.5f, -0.55f   // 3
            };
            // 4 side faces + base split into 1 triangle (simple)
            faces = new short[] {
                    0,1,2,
                    0,2,3,
                    0,3,1,
                    1,3,2
            };
        } else if (shape == ShapeSelectActivity.SHAPE_SQUARE) {
            // Cube (8 vertices)
            float s = 0.55f;
            verts = new float[] {
                    -s, -s,  s,  // 0
                    s, -s,  s,  // 1
                    s,  s,  s,  // 2
                    -s,  s,  s,  // 3
                    -s, -s, -s,  // 4
                    s, -s, -s,  // 5
                    s,  s, -s,  // 6
                    -s,  s, -s   // 7
            };
            faces = cubeFaces();
        } else {
            // Rectangular prism (8 vertices, stretched)
            float sx = 0.75f, sy = 0.45f, sz = 0.35f;
            verts = new float[] {
                    -sx, -sy,  sz, // 0
                    sx, -sy,  sz, // 1
                    sx,  sy,  sz, // 2
                    -sx,  sy,  sz, // 3
                    -sx, -sy, -sz, // 4
                    sx, -sy, -sz, // 5
                    sx,  sy, -sz, // 6
                    -sx,  sy, -sz  // 7
            };
            faces = cubeFaces();
        }
    }

    private short[] cubeFaces() {
        // 12 triangles (2 per face)
        return new short[] {
                // front (0,1,2,3)
                0,1,2,  0,2,3,
                // right (1,5,6,2)
                1,5,6,  1,6,2,
                // back (5,4,7,6)
                5,4,7,  5,7,6,
                // left (4,0,3,7)
                4,0,3,  4,3,7,
                // top (3,2,6,7)
                3,2,6,  3,6,7,
                // bottom (4,5,1,0)
                4,5,1,  4,1,0
        };
    }

    // ---------- Shaders ----------
    private static final String FACE_VS =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 aPosition;" +
                    "void main(){ gl_Position = uMVPMatrix * aPosition; }";

    private static final String FACE_FS =
            "precision mediump float;" +
                    "uniform vec4 uColor;" +
                    "void main(){ gl_FragColor = uColor; }";

    private static final String POINT_VS =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 aPosition;" +
                    "uniform float uPointSize;" +
                    "void main(){ gl_Position = uMVPMatrix * aPosition; gl_PointSize = uPointSize; }";

    private static final String POINT_FS =
            "precision mediump float;" +
                    "uniform vec4 uColor;" +
                    "void main(){ gl_FragColor = uColor; }";

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.06f, 0.06f, 0.08f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        faceProgram = buildProgram(FACE_VS, FACE_FS);
        pointProgram = buildProgram(POINT_VS, POINT_FS);

        // Camera: move back a bit so it’s not “too close”
        Matrix.setLookAtM(viewMatrix, 0,
                0f, 0.4f, 3.4f,
                0f, 0f, 0f,
                0f, 1f, 0f
        );
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        vpW = width;
        vpH = height;
        GLES20.glViewport(0, 0, width, height);

        float aspect = (float) width / (float) height;
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspect, 0.1f, 100f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Build model matrix
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, angleX, 1f, 0f, 0f);
        Matrix.rotateM(modelMatrix, 0, angleY, 0f, 1f, 0f);

        float[] vp = new float[16];
        Matrix.multiplyMM(vp, 0, projectionMatrix, 0, viewMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, vp, 0, modelMatrix, 0);

        // Update buffers from current editable vertices
        updateTriangleBuffer();
        updatePointBuffer();

        drawFaces();
        if (showVertices) drawPoints();
    }

    private void updateTriangleBuffer() {
        // Expand faces into xyz xyz xyz...
        float[] tris = new float[faces.length * 3];
        int out = 0;
        for (int i = 0; i < faces.length; i++) {
            int vidx = faces[i];
            int base = vidx * 3;
            tris[out++] = verts[base];
            tris[out++] = verts[base + 1];
            tris[out++] = verts[base + 2];
        }
        triBuffer = toFloatBuffer(tris);
    }

    private void updatePointBuffer() {
        pointBuffer = toFloatBuffer(verts);
    }

    private void drawFaces() {
        GLES20.glUseProgram(faceProgram);

        int pos = GLES20.glGetAttribLocation(faceProgram, "aPosition");
        int mvp = GLES20.glGetUniformLocation(faceProgram, "uMVPMatrix");
        int col = GLES20.glGetUniformLocation(faceProgram, "uColor");

        GLES20.glUniformMatrix4fv(mvp, 1, false, mvpMatrix, 0);

        // Face color (cyan-ish)
        GLES20.glUniform4f(col, 0.2f, 0.85f, 0.9f, 1f);

        GLES20.glEnableVertexAttribArray(pos);
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 3 * 4, triBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, faces.length);

        GLES20.glDisableVertexAttribArray(pos);
    }

    private void drawPoints() {

        // Disable depth so points render on top
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        GLES20.glUseProgram(pointProgram);

        int pos = GLES20.glGetAttribLocation(pointProgram, "aPosition");
        int mvp = GLES20.glGetUniformLocation(pointProgram, "uMVPMatrix");
        int col = GLES20.glGetUniformLocation(pointProgram, "uColor");
        int size = GLES20.glGetUniformLocation(pointProgram, "uPointSize");

        GLES20.glUniformMatrix4fv(mvp, 1, false, mvpMatrix, 0);

        // Make points larger so easier to tap
        GLES20.glUniform1f(size, 26f);

        // Draw all vertices (red)
        GLES20.glUniform4f(col, 0.95f, 0.2f, 0.25f, 1f);

        GLES20.glEnableVertexAttribArray(pos);
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 3 * 4, pointBuffer);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, verts.length / 3);

        // Highlight selected vertex (yellow)
        if (selectedVertex >= 0) {
            float[] one = new float[] {
                    verts[selectedVertex * 3],
                    verts[selectedVertex * 3 + 1],
                    verts[selectedVertex * 3 + 2]
            };

            FloatBuffer oneBuf = toFloatBuffer(one);

            GLES20.glUniform4f(col, 1f, 0.9f, 0.2f, 1f);
            GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 3 * 4, oneBuf);
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
        }

        GLES20.glDisableVertexAttribArray(pos);

        // Re-enable depth for normal drawing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    // ---------- Vertex picking + dragging ----------
    public void pickVertex(float touchX, float touchY) {
        // Find closest vertex in screen space
        int best = -1;
        float bestDist2 = Float.MAX_VALUE;

        for (int i = 0; i < verts.length / 3; i++) {
            float[] p = projectToScreen(i);
            if (p == null) continue;

            float dx = touchX - p[0];
            float dy = touchY - p[1];
            float d2 = dx * dx + dy * dy;

            if (d2 < bestDist2) {
                bestDist2 = d2;
                best = i;
            }
        }

        // Selection threshold (~70px)
        if (bestDist2 <= 70f * 70f) selectedVertex = best;
        else selectedVertex = -1;
    }

    public void dragSelectedVertex(float dx, float dy) {
        if (selectedVertex < 0) return;

        // Simple: move in model/object X/Y (screen drag mapped to x/y)
        int base = selectedVertex * 3;
        verts[base]     += dx * DRAG_SCALE;
        verts[base + 1] -= dy * DRAG_SCALE; // invert screen Y
        // verts[base + 2] unchanged (keeping it simple)
    }

    private float[] projectToScreen(int vertexIndex) {
        // Object coords
        int base = vertexIndex * 3;
        float[] v = new float[] { verts[base], verts[base+1], verts[base+2], 1f };

        // Clip coords = MVP * v
        float[] clip = new float[4];
        Matrix.multiplyMV(clip, 0, mvpMatrix, 0, v, 0);

        if (clip[3] == 0f) return null;

        // NDC
        float ndcX = clip[0] / clip[3];
        float ndcY = clip[1] / clip[3];

        // Convert to screen
        float screenX = (ndcX * 0.5f + 0.5f) * vpW;
        float screenY = (1f - (ndcY * 0.5f + 0.5f)) * vpH;

        return new float[] { screenX, screenY };
    }

    // ---------- GL helpers ----------
    private static FloatBuffer toFloatBuffer(float[] arr) {
        ByteBuffer bb = ByteBuffer.allocateDirect(arr.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(arr);
        fb.position(0);
        return fb;
    }

    private static int buildProgram(String vs, String fs) {
        int v = loadShader(GLES20.GL_VERTEX_SHADER, vs);
        int f = loadShader(GLES20.GL_FRAGMENT_SHADER, fs);
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, v);
        GLES20.glAttachShader(p, f);
        GLES20.glLinkProgram(p);
        return p;
    }

    private static int loadShader(int type, String code) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, code);
        GLES20.glCompileShader(s);
        return s;
    }
}