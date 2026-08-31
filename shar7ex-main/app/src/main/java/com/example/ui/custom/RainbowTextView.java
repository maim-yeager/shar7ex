package com.example.ui.custom;

import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * Custom TextView providing animated and static Rainbow gradient font coloring
 * as requested for "M.SHAREX" and "Created by NH MAIM".
 */
public class RainbowTextView extends AppCompatTextView {

    private LinearGradient linearGradient;
    private Matrix matrix;
    private float translate = 0;
    private boolean isAnimating = true;

    private static final int[] RAINBOW_COLORS = new int[]{
            0xFFFF0055, // Red-Pink
            0xFFFF5500, // Vibrant Orange
            0xFFFFCC00, // Golden Yellow
            0xFF00E676, // Spring Green
            0xFF00D2FF, // Cyan
            0xFF7000FF, // Electric Purple
            0xFFFF00D4, // Magenta
            0xFFFF0055  // Loop back
    };

    public RainbowTextView(@NonNull Context context) {
        super(context);
        init();
    }

    public RainbowTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RainbowTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        matrix = new Matrix();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0) {
            linearGradient = new LinearGradient(
                    0, 0, w, 0,
                    RAINBOW_COLORS,
                    null,
                    Shader.TileMode.REPEAT
            );
            getPaint().setShader(linearGradient);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAnimating = true;
        post(animationRunnable);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAnimating = false;
        removeCallbacks(animationRunnable);
    }

    private final Runnable animationRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAnimating) return;
            if (linearGradient != null && getWidth() > 0) {
                translate += 4f;
                if (translate > getWidth()) {
                    translate = 0;
                }
                matrix.setTranslate(translate, 0);
                linearGradient.setLocalMatrix(matrix);
                invalidate();
            }
            postDelayed(this, 30);
        }
    };
}
