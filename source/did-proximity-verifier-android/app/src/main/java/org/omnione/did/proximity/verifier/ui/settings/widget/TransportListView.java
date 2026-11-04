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

package org.omnione.did.proximity.verifier.ui.settings.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.omnione.did.proximity.verifier.R;
import org.omnione.did.proximity.verifier.data.model.Transport;
import org.omnione.did.proximity.verifier.databinding.ItemTransportRowBinding;

import java.util.ArrayList;
import java.util.List;

public class TransportListView extends LinearLayout {

    public interface OnOrderChanged {
        void onOrderChanged(List<String> order);
    }

    private static final float LOCKED_ALPHA = 0.45f;
    private static final int CORNER_RADIUS_DP = 12;
    private static final int BORDER_DP = 1;

    private final Path contentClip = new Path();

    private final List<String> order = new ArrayList<>();
    private final List<String> viewIds = new ArrayList<>();
    private final List<ItemTransportRowBinding> rows = new ArrayList<>();

    private OnOrderChanged listener;

    private String dragId;
    private int dragBaseIndex;
    private float dragStartRawY;

    public TransportListView(Context context) {
        this(context, null);
    }

    public TransportListView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TransportListView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        setBackgroundResource(R.drawable.bg_transport_list);
        int border = dp(BORDER_DP);
        setPadding(border, border, border, border);

        if (isInEditMode()) setOrder(Transport.defaultOrder());
    }

    public void setOnOrderChanged(OnOrderChanged listener) {
        this.listener = listener;
    }

    public void setOrder(List<String> next) {
        if (order.equals(next)) return;
        order.clear();
        order.addAll(next);
        build();
    }

    private void build() {
        removeAllViews();
        rows.clear();
        viewIds.clear();
        viewIds.addAll(order);
        for (Transport transport : Transport.values()) {
            if (!viewIds.contains(transport.id)) viewIds.add(transport.id);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < viewIds.size(); i++) {
            ItemTransportRowBinding row = ItemTransportRowBinding.inflate(inflater, this, false);
            bindRow(row, viewIds.get(i), i == viewIds.size() - 1);
            rows.add(row);
            addView(row.getRoot());
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindRow(ItemTransportRowBinding row, String id, boolean last) {
        Transport transport = Transport.of(id);
        if (transport == null) return;

        row.transportLabel.setText(transport.labelRes);
        row.getRoot().setBackgroundResource(
                last ? R.color.white : R.drawable.bg_transport_row);

        row.transportCheckBoxTouch.setOnClickListener(v -> toggle(id));
        row.transportHandle.setOnTouchListener((v, event) -> onHandleTouch(id, event));

        applyRowState(row, id);
    }

    private void applyRowState(ItemTransportRowBinding row, String id) {
        int index = order.indexOf(id);
        boolean enabled = index >= 0;
        boolean lockedOn = enabled && order.size() == 1;

        row.transportCheckBox.setChecked(enabled);
        row.transportCheckBoxTouch.setAlpha(lockedOn ? LOCKED_ALPHA : 1f);

        row.transportBadge.setText(enabled ? String.valueOf(index + 1) : "");
        row.transportBadge.setVisibility(enabled ? VISIBLE : INVISIBLE);

        row.transportLabel.setTextColor(ContextCompat.getColor(getContext(),
                enabled ? R.color.brand_text_primary : R.color.brand_gray500));

        row.transportHandle.setVisibility(enabled ? VISIBLE : INVISIBLE);
    }

    private void toggle(String id) {
        List<String> next = new ArrayList<>(order);
        if (!next.contains(id)) {
            next.add(id);
        } else {
            if (next.size() == 1) return;
            next.remove(id);
        }
        order.clear();
        order.addAll(next);
        build();
        notifyChanged();
    }

    private boolean onHandleTouch(String id, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                int index = order.indexOf(id);
                if (index < 0) return false;
                dragId = id;
                dragBaseIndex = index;
                dragStartRawY = event.getRawY();
                requestDisallowInterceptTouchEvent(true);
                rowOf(id).getRoot().setTranslationZ(dp(6));
                return true;

            case MotionEvent.ACTION_MOVE:
                if (dragId == null) return false;
                onDragMove(event.getRawY());
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragId == null) return false;
                requestDisallowInterceptTouchEvent(false);
                dragId = null;
                build();
                return true;

            default:
                return false;
        }
    }

    private void onDragMove(float rawY) {
        int rowHeight = dp(52);
        int shift = Math.round((rawY - dragStartRawY) / rowHeight);
        int target = Math.max(0, Math.min(order.size() - 1, dragBaseIndex + shift));

        if (target != dragBaseIndex) {
            order.remove(dragBaseIndex);
            order.add(target, dragId);
            dragStartRawY += (float) (target - dragBaseIndex) * rowHeight;
            dragBaseIndex = target;
            applyDragOffsets();
            notifyChanged();
        }

        float low = (float) -dragBaseIndex * rowHeight;
        float high = (float) (order.size() - 1 - dragBaseIndex) * rowHeight;
        float offset = Math.max(low, Math.min(high, rawY - dragStartRawY));
        float base = (float) (dragBaseIndex - viewIds.indexOf(dragId)) * rowHeight;
        rowOf(dragId).getRoot().setTranslationY(base + offset);
    }

    private void applyDragOffsets() {
        int rowHeight = dp(52);
        for (int i = 0; i < viewIds.size(); i++) {
            String id = viewIds.get(i);
            ItemTransportRowBinding row = rows.get(i);
            applyRowState(row, id);
            if (id.equals(dragId)) continue;
            int index = order.indexOf(id);
            row.getRoot().setTranslationY(index < 0 ? 0f : (float) (index - i) * rowHeight);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        float inset = dp(BORDER_DP);
        float radius = dp(CORNER_RADIUS_DP) - inset;
        contentClip.reset();
        contentClip.addRoundRect(inset, inset, getWidth() - inset, getHeight() - inset,
                radius, radius, Path.Direction.CW);

        int save = canvas.save();
        canvas.clipPath(contentClip);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(save);
    }

    private ItemTransportRowBinding rowOf(String id) {
        return rows.get(viewIds.indexOf(id));
    }

    private void notifyChanged() {
        if (listener != null) listener.onOrderChanged(new ArrayList<>(order));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        dragId = null;
    }
}
