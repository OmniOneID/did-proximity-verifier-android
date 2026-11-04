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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import org.omnione.did.proximity.verifier.R;

public class SectionTitleView extends LinearLayout {

    private final TextView titleView;
    private final TextView badgeView;

    public SectionTitleView(Context context) {
        this(context, null);
    }

    public SectionTitleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SectionTitleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(4), 0, dp(4), dp(8));

        View rule = new View(context);
        LayoutParams ruleParams = new LayoutParams(dp(4), dp(16));
        ruleParams.setMarginEnd(dp(8));
        rule.setLayoutParams(ruleParams);
        rule.setBackgroundResource(R.drawable.bg_section_marker_pale);
        addView(rule);

        titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        titleView.setTypeface(ResourcesCompat.getFont(context, R.font.pretendard_bold));
        titleView.setTextColor(ContextCompat.getColor(context, R.color.brand_text_primary));
        titleView.setLetterSpacing(-0.008f);
        addView(titleView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        badgeView = new TextView(context);
        badgeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        badgeView.setTypeface(ResourcesCompat.getFont(context, R.font.pretendard_bold));
        badgeView.setTextColor(ContextCompat.getColor(context, R.color.brand_primary_dark));
        badgeView.setBackgroundResource(R.drawable.bg_count_badge);
        badgeView.setPadding(dp(9), dp(3), dp(9), dp(3));
        badgeView.setLetterSpacing(0.018f);
        badgeView.setVisibility(GONE);
        LayoutParams badgeParams =
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        badgeParams.weight = 0f;
        badgeParams.gravity = Gravity.CENTER_VERTICAL;
        addView(badgeView, badgeParams);

        View spacer = new View(context);
        LayoutParams spacerParams = new LayoutParams(0, 0);
        spacerParams.weight = 1f;
        addView(spacer, 2, spacerParams);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SectionTitleView);
            CharSequence title = a.getText(R.styleable.SectionTitleView_sectionTitle);
            if (title != null) titleView.setText(title);
            int badge = a.getInt(R.styleable.SectionTitleView_sectionBadge, -1);
            if (badge >= 0) setBadgeCount(badge);
            a.recycle();
        }
    }

    public void setTitle(CharSequence title) {
        titleView.setText(title);
    }

    public void setBadgeCount(int count) {
        if (count < 0) {
            badgeView.setVisibility(GONE);
            return;
        }
        badgeView.setText(String.valueOf(count));
        badgeView.setVisibility(VISIBLE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
