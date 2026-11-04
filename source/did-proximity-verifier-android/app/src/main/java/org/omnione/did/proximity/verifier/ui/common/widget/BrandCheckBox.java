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

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.omnione.did.proximity.verifier.R;

public class BrandCheckBox extends FrameLayout {

    private final ImageView check;
    private final boolean small;
    private boolean checked;

    public BrandCheckBox(Context context) {
        this(context, null);
    }

    public BrandCheckBox(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BrandCheckBox(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        boolean isSmall = false;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BrandCheckBox);
            isSmall = a.getBoolean(R.styleable.BrandCheckBox_checkBoxSmall, false);
            a.recycle();
        }
        small = isSmall;

        check = new ImageView(context);
        int checkSize = dp(small ? 12 : 14);
        LayoutParams params = new LayoutParams(checkSize, checkSize);
        params.gravity = Gravity.CENTER;
        check.setLayoutParams(params);
        check.setImageResource(R.drawable.ic_check);
        check.setImageTintList(ContextCompat.getColorStateList(context, R.color.white));
        addView(check);

        applyState();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = dp(small ? 18 : 22);
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY));
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean value) {
        if (checked == value) return;
        checked = value;
        applyState();
    }

    private void applyState() {
        if (small) {
            setBackgroundResource(checked
                    ? R.drawable.bg_checkbox_small_on
                    : R.drawable.bg_checkbox_small_off);
        } else {
            setBackgroundResource(checked
                    ? R.drawable.bg_checkbox_on
                    : R.drawable.bg_checkbox_off);
        }
        check.setVisibility(checked ? VISIBLE : GONE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
