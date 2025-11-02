/*
 * Copyright (c) 2025 Rising Revived Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mist.utils;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

public class RainbowTextView extends TextView {
    private LinearGradient mGradient;
    private Matrix mMatrix;
    private float mTranslate;
    private ValueAnimator mAnimator;
    private int mTextWidth;

    public RainbowTextView(Context context) {
        super(context);
        init();
    }

    public RainbowTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RainbowTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setShadowLayer(2f, 0f, 0f, 0x80000000);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        mMatrix = new Matrix();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0) return;

        // Measure text width for seamless animation
        mTextWidth = (int) getPaint().measureText(getText().toString());
        if (mTextWidth <= 0) mTextWidth = w;

        // Define the gradient colors - ensure they repeat perfectly
        int[] colors = {
            0xFF80CBC4, // Teal Accent 
            0xFF64B5F6, // Light Blue Accent
            0xFF7986CB, // Indigo Accent
            0xFFBA68C8, // Purple Accent
            0xFFE57373, // Soft Red Accent
            0xFFFFB74D, // Orange Accent
            0xFFFFD54F, // Yellow Accent
            0xFF80CBC4  // Teal Accent (repeat first color for seamless looping)
        };

        // Create a gradient exactly as wide as needed for one complete color cycle
        float gradientWidth = mTextWidth; // Width of one complete color cycle
        mGradient = new LinearGradient(0, 0, gradientWidth, 0, colors, null, Shader.TileMode.REPEAT);
        getPaint().setShader(mGradient);

        // Restart the animation
        startAnimation();
    }

    private void startAnimation() {
        if (mAnimator != null) {
            mAnimator.cancel();
        }

        // Animate exactly one color cycle width for perfect looping
        mAnimator = ValueAnimator.ofFloat(0, mTextWidth);

        // Set to 3 seconds (3000ms) as requested
        mAnimator.setDuration(3000);

        // Using INFINITE + RESTART with a perfect cycle means no visible seams
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.setRepeatMode(ValueAnimator.RESTART);
        mAnimator.setInterpolator(new LinearInterpolator());

        mAnimator.addUpdateListener(animation -> {
            mTranslate = (float) animation.getAnimatedValue();
            invalidate();
        });

        mAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mGradient != null) {
            mMatrix.setTranslate(-mTranslate, 0);
            mGradient.setLocalMatrix(mMatrix);
        }
        super.onDraw(canvas);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getVisibility() == VISIBLE) {
            post(() -> startAnimation());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == VISIBLE) {
            startAnimation();
        } else if (mAnimator != null) {
            mAnimator.cancel();
        }
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (getWidth() > 0) {
            onSizeChanged(getWidth(), getHeight(), getWidth(), getHeight());
        }
    }
}
