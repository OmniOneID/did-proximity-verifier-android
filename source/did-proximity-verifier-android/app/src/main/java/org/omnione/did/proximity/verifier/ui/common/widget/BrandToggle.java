/*
 * Copyright 2026 OmniOne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnione.did.proximity.verifier.ui.common.widget;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.omnione.did.proximity.verifier.R;

public class BrandToggle extends FrameLayout {

    public interface OnCheckedChangeListener {
        void onCheckedChanged(BrandToggle toggle, boolean checked);
    }

    private static final long DURATION_MS = 140L;

    private final GradientDrawable track = new GradientDrawable();
    private final View thumb;
    private final int colorOn;
    private final int colorOff;
    private final float travelPx;

    private boolean checked;
    private ValueAnimator animator;
    @Nullable
    private OnCheckedChangeListener listener;

    public BrandToggle(Context context) {
        this(context, null);
    }

    public BrandToggle(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BrandToggle(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        colorOn = ContextCompat.getColor(context, R.color.brand_primary);
        colorOff = ContextCompat.getColor(context, R.color.brand_gray300);
        travelPx = dp(46) - dp(3) * 2f - dp(20);

        track.setShape(GradientDrawable.RECTANGLE);
        track.setCornerRadius(dp(13));
        track.setColor(colorOff);
        setBackground(track);

        thumb = new View(context);
        LayoutParams params = new LayoutParams(dpInt(20), dpInt(20));
        params.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        params.setMarginStart(dpInt(3));
        thumb.setLayoutParams(params);
        thumb.setBackgroundResource(R.drawable.bg_toggle_thumb);
        thumb.setElevation(dp(1.5f));
        addView(thumb);

        setClickable(true);
        setFocusable(true);
        setOnClickListener(v -> toggle());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(Math.round(dp(46)), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Math.round(dp(26)), MeasureSpec.EXACTLY));
    }

    public boolean isChecked() {
        return checked;
    }

    public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    public void setChecked(boolean value) {
        setChecked(value, true);
    }

    public void setCheckedSilently(boolean value, boolean animate) {
        OnCheckedChangeListener saved = listener;
        listener = null;
        setChecked(value, animate);
        listener = saved;
    }

    public void setChecked(boolean value, boolean animate) {
        if (checked == value) return;
        checked = value;
        applyState(animate && !isInEditMode());
        if (listener != null) listener.onCheckedChanged(this, checked);
    }

    private void toggle() {
        setChecked(!checked, true);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return Switch.class.getName();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setCheckable(true);
        info.setChecked(checked);
    }

    private void applyState(boolean animate) {
        float target = checked ? travelPx : 0f;
        int targetColor = checked ? colorOn : colorOff;

        if (animator != null) animator.cancel();
        if (!animate) {
            thumb.setTranslationX(target);
            track.setColor(targetColor);
            return;
        }

        float startX = thumb.getTranslationX();
        int startColor = checked ? colorOff : colorOn;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(DURATION_MS);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        ArgbEvaluator evaluator = new ArgbEvaluator();
        animator.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            thumb.setTranslationX(startX + (target - startX) * f);
            track.setColor((int) evaluator.evaluate(f, startColor, targetColor));
        });
        animator.start();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private int dpInt(int value) {
        return Math.round(dp(value));
    }
}
