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
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import org.omnione.did.proximity.verifier.R;

public class RequestChip extends LinearLayout {

    private final ImageView check;
    private final TextView label;
    private boolean selected;

    public RequestChip(Context context) {
        this(context, null);
    }

    public RequestChip(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RequestChip(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        check = new ImageView(context);
        LayoutParams checkParams = new LayoutParams(dp(15), dp(15));
        checkParams.setMarginEnd(dp(6));
        check.setLayoutParams(checkParams);
        check.setImageResource(R.drawable.ic_check);
        check.setImageTintList(ContextCompat.getColorStateList(context, R.color.white));
        check.setVisibility(GONE);
        addView(check);

        label = new TextView(context);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        addView(label, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RequestChip);
            CharSequence text = a.getText(R.styleable.RequestChip_chipText);
            if (text != null) label.setText(text);
            a.recycle();
        }

        setClickable(true);
        setFocusable(true);
        applyState();
    }

    public void setText(CharSequence text) {
        label.setText(text);
    }

    public boolean isChipSelected() {
        return selected;
    }

    public void setChipSelected(boolean value) {
        if (selected == value) return;
        selected = value;
        applyState();
    }

    private void applyState() {
        Context context = getContext();
        if (selected) {
            setBackgroundResource(R.drawable.bg_chip_selected);
            setPadding(dp(11), dp(9), dp(14), dp(9));
            check.setVisibility(VISIBLE);
            label.setTextColor(ContextCompat.getColor(context, R.color.white));
            label.setTypeface(ResourcesCompat.getFont(context, R.font.pretendard_semibold));
        } else {
            setBackgroundResource(R.drawable.bg_chip_unselected);
            setPadding(dp(14), dp(9), dp(14), dp(9));
            check.setVisibility(GONE);
            label.setTextColor(ContextCompat.getColor(context, R.color.brand_text_primary));
            label.setTypeface(ResourcesCompat.getFont(context, R.font.pretendard_medium));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
