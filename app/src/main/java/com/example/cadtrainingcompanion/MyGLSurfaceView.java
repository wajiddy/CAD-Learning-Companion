package com.example.cadtrainingcompanion;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class MyGLSurfaceView extends GLSurfaceView {

    private final MyRenderer renderer;

    private float prevX, prevY;

    public MyGLSurfaceView(Context context, int shape) {
        super(context);
        setEGLContextClientVersion(2);

        renderer = new MyRenderer(shape);
        setRenderer(renderer);

        // Continuous looks nicer for dragging points
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    public boolean toggleVertices() {
        renderer.showVertices = !renderer.showVertices;
        return renderer.showVertices;
    }

    public boolean toggleEditMode() {
        renderer.editMode = !renderer.editMode;
        renderer.clearSelection();
        return renderer.editMode;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {

        final float x = e.getX();
        final float y = e.getY();

        switch (e.getAction()) {

            case MotionEvent.ACTION_DOWN:
                if (renderer.editMode) {
                    queueEvent(() -> renderer.pickVertex(x, y));
                }
                break;

            case MotionEvent.ACTION_MOVE:
                final float dx = x - prevX;
                final float dy = y - prevY;

                if (renderer.editMode) {
                    queueEvent(() -> renderer.dragSelectedVertex(dx, dy));
                } else {
                    queueEvent(() -> {
                        renderer.angleY += dx * 0.5f;
                        renderer.angleX += dy * 0.5f;
                    });
                }
                break;
        }

        prevX = x;
        prevY = y;

        return true;
    }
}