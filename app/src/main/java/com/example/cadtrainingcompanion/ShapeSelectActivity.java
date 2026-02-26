package com.example.cadtrainingcompanion;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

public class ShapeSelectActivity extends AppCompatActivity {

    public static final String EXTRA_SHAPE = "shape";
    public static final int SHAPE_TRIANGLE = 0;
    public static final int SHAPE_SQUARE = 1;
    public static final int SHAPE_RECTANGLE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shape_select);

        RadioGroup rg = findViewById(R.id.rgShapes);
        Button start = findViewById(R.id.btnStart);

        start.setOnClickListener(v -> {
            int checkedId = rg.getCheckedRadioButtonId();
            int shape = SHAPE_TRIANGLE;

            if (checkedId == R.id.rbSquare) shape = SHAPE_SQUARE;
            else if (checkedId == R.id.rbRectangle) shape = SHAPE_RECTANGLE;

            Intent i = new Intent(this, GLActivity.class);
            i.putExtra(EXTRA_SHAPE, shape);
            startActivity(i);
        });
    }
}