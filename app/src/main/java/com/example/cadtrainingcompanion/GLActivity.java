package com.example.cadtrainingcompanion;

import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

public class GLActivity extends AppCompatActivity {

    private MyGLSurfaceView glView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gl);

        int shape = getIntent().getIntExtra(ShapeSelectActivity.EXTRA_SHAPE,
                ShapeSelectActivity.SHAPE_TRIANGLE);

        FrameLayout root = findViewById(R.id.glRoot);

        glView = new MyGLSurfaceView(this, shape);
        root.addView(glView, 0); // behind the buttons overlay

        Button btnVerts = findViewById(R.id.btnToggleVertices);
        Button btnEdit = findViewById(R.id.btnToggleEdit);

        btnVerts.setOnClickListener(v -> {
            boolean on = glView.toggleVertices();
            btnVerts.setText(on ? "Vertices: ON" : "Vertices: OFF");
        });

        btnEdit.setOnClickListener(v -> {
            boolean on = glView.toggleEditMode();
            btnEdit.setText(on ? "Edit Mode: ON" : "Edit Mode: OFF");
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        glView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glView.onResume();
    }
}