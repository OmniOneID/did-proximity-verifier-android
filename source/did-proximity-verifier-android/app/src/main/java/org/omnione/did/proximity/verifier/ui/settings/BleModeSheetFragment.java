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

package org.omnione.did.proximity.verifier.ui.settings;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.omnione.did.proximity.verifier.R;
import org.omnione.did.proximity.verifier.data.model.BleTransferMode;

public class BleModeSheetFragment extends BottomSheetDialogFragment {

    public interface OnModeSelected {
        void onBleModeSelected(@NonNull BleTransferMode mode);
    }

    private static final String TAG = "BleModeSheetFragment";
    private static final String ARG_SELECTED = "selected";
    private static final float SCRIM_ALPHA = 0.4f;

    @NonNull
    public static BleModeSheetFragment newInstance(@NonNull BleTransferMode selected) {
        BleModeSheetFragment sheet = new BleModeSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED, selected.id);
        sheet.setArguments(args);
        return sheet;
    }

    public void show(@NonNull FragmentManager fm) {
        if (fm.isStateSaved() || fm.findFragmentByTag(TAG) != null) return;
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

        ((TextView) view.findViewById(R.id.sheetTitle)).setText(R.string.settings_ble_mode_title);
        ((TextView) view.findViewById(R.id.sheetDescription))
                .setText(R.string.settings_ble_mode_description);

        BleTransferMode selected =
                BleTransferMode.of(requireArguments().getString(ARG_SELECTED));
        LinearLayout items = view.findViewById(R.id.sheetItems);
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (BleTransferMode mode : BleTransferMode.values()) {
            if (items.getChildCount() > 0) {
                inflater.inflate(R.layout.view_divider, items, true);
            }
            View row = inflater.inflate(R.layout.item_sheet_select_row, items, false);
            ((TextView) row.findViewById(R.id.selectLabel)).setText(mode.labelRes);

            boolean checked = mode == selected;
            row.findViewById(R.id.selectCheck)
                    .setVisibility(checked ? View.VISIBLE : View.INVISIBLE);
            row.setBackgroundResource(
                    checked ? R.color.brand_primary_pale : android.R.color.transparent);
            row.setOnClickListener(v -> select(mode));
            items.addView(row);
        }
    }

    private void select(@NonNull BleTransferMode mode) {
        if (getActivity() instanceof OnModeSelected) {
            ((OnModeSelected) getActivity()).onBleModeSelected(mode);
        }
        dismissAllowingStateLoss();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) return;

        View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) sheet.setBackgroundColor(Color.TRANSPARENT);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setDimAmount(SCRIM_ALPHA);
            window.setWindowAnimations(R.style.Animation_DidVerifier_BottomSheet);
        }
    }
}
