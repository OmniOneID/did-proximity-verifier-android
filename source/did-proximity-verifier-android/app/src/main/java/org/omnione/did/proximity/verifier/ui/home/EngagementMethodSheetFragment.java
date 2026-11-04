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

package org.omnione.did.proximity.verifier.ui.home;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.omnione.did.proximity.verifier.R;

public class EngagementMethodSheetFragment extends BottomSheetDialogFragment {

    public interface OnMethodSelected {
        void onScanQrSelected();

        void onTapNfcSelected();
    }

    private static final String TAG = "EngagementMethodSheet";
    private static final float SCRIM_ALPHA = 0.4f;

    public void show(@NonNull FragmentManager fm) {
        if (fm.isStateSaved() || fm.findFragmentByTag(TAG) != null) {
            return;
        }
        showNow(fm, TAG);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_select, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((TextView) view.findViewById(R.id.sheetTitle)).setText(R.string.home_sheet_title);
        ((TextView) view.findViewById(R.id.sheetDescription))
                .setText(R.string.home_sheet_description);

        LinearLayout items = view.findViewById(R.id.sheetItems);
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        addRow(inflater, items, R.drawable.ic_qr, R.string.home_sheet_scan_qr, false,
                () -> deliver(true));
        addRow(inflater, items, R.drawable.ic_nfc, R.string.home_sheet_tap_nfc, true,
                () -> deliver(false));
    }

    private void addRow(@NonNull LayoutInflater inflater,
                        @NonNull LinearLayout items,
                        @DrawableRes int icon,
                        @StringRes int label,
                        boolean separated,
                        @NonNull Runnable onClick) {
        if (separated) {
            inflater.inflate(R.layout.view_divider, items, true);
        }
        View row = inflater.inflate(R.layout.item_sheet_nav_row, items, false);
        ((ImageView) row.findViewById(R.id.navIcon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.navLabel)).setText(label);
        row.setOnClickListener(v -> onClick.run());
        items.addView(row);
    }

    private void deliver(boolean scanQr) {
        if (getActivity() instanceof OnMethodSelected) {
            OnMethodSelected host = (OnMethodSelected) getActivity();
            if (scanQr) {
                host.onScanQrSelected();
            } else {
                host.onTapNfcSelected();
            }
        }
        dismissAllowingStateLoss();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) {
            sheet.setBackgroundColor(Color.TRANSPARENT);
        }
        Window window = dialog.getWindow();
        if (window != null) {
            window.setDimAmount(SCRIM_ALPHA);
            window.setWindowAnimations(R.style.Animation_DidVerifier_BottomSheet);
        }
    }
}
