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

package org.omnione.did.proximity.verifier.ui.common;

import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FontSpan extends MetricAffectingSpan {

    @Nullable
    private final Typeface typeface;

    public FontSpan(@Nullable Typeface typeface) {
        this.typeface = typeface;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint paint) {
        apply(paint);
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint paint) {
        apply(paint);
    }

    private void apply(TextPaint paint) {
        if (typeface != null) paint.setTypeface(typeface);
    }
}
