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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;

import org.omnione.did.proximity.verifier.R;
import org.omnione.did.proximity.verifier.data.model.DocCatalog;
import org.omnione.did.proximity.verifier.data.model.DocGroup;
import org.omnione.did.proximity.verifier.data.model.RequestSelection;
import org.omnione.did.proximity.verifier.databinding.ActivityHomeBinding;
import org.omnione.did.proximity.verifier.databinding.ViewRequestGroupBinding;
import org.omnione.did.proximity.verifier.ui.common.BaseActivity;
import org.omnione.did.proximity.verifier.util.IntentRouter;

import java.util.LinkedHashMap;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeActivity extends BaseActivity
        implements EngagementMethodSheetFragment.OnMethodSelected {

    private ActivityHomeBinding binding;
    private HomeViewModel viewModel;
    private final Map<String, ViewRequestGroupBinding> groupBindings = new LinkedHashMap<>();

    @Override
    public void onScanQrSelected() {
        IntentRouter.startDeviceEngagement(this);
    }

    @Override
    public void onTapNfcSelected() {
        IntentRouter.startNfcEngagement(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        applySystemBarInsets(binding.getRoot(), bars -> {
            applyTopInsetPadding(binding.appBar.getRoot(), bars.top);
            applyBottomInsetPadding(binding.bottomBar, dp(16), bars.bottom);
        });

        binding.appBar.appBarTitle.setText(R.string.home_title);
        binding.appBar.appBarLeft.setImageResource(R.drawable.ic_menu);
        binding.appBar.appBarLeft.setContentDescription(getText(R.string.common_menu));
        binding.appBar.appBarLeft.setOnClickListener(v -> IntentRouter.startSettings(this));

        buildGroups();

        binding.startVerificationButton.setOnClickListener(
                v -> new EngagementMethodSheetFragment().show(getSupportFragmentManager()));

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        viewModel.getState().observe(this, this::render);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refresh();
    }

    private void buildGroups() {
        LayoutInflater inflater = getLayoutInflater();
        boolean first = true;
        for (DocGroup group : DocCatalog.groups()) {
            ViewRequestGroupBinding groupBinding =
                    ViewRequestGroupBinding.inflate(inflater, binding.groupContainer, false);
            if (!first) {
                ((android.widget.LinearLayout.LayoutParams) groupBinding.getRoot()
                        .getLayoutParams()).topMargin = dp(20);
            }
            first = false;

            groupBinding.groupSectionTitle.setTitle(getText(group.titleRes));
            groupBinding.chipFull.setText(getText(group.chipFullRes));
            groupBinding.chipCustom.setText(getText(group.chipCustomRes));
            groupBinding.chipFull.setOnClickListener(v -> viewModel.toggleFull(group.id));
            groupBinding.chipCustom.setOnClickListener(v -> {
                if (!viewModel.clearIfCustom(group.id)) {
                    IntentRouter.startCustomAttrs(this, group.id);
                }
            });

            binding.groupContainer.addView(groupBinding.getRoot());
            groupBindings.put(group.id, groupBinding);
        }
    }

    private void render(HomeUiState state) {
        for (DocGroup group : DocCatalog.groups()) {
            ViewRequestGroupBinding groupBinding = groupBindings.get(group.id);
            if (groupBinding == null) continue;

            RequestSelection selection = state.selectionOf(group.id);
            boolean full = selection != null && selection.mode == RequestSelection.Mode.FULL;
            boolean custom = selection != null && selection.mode == RequestSelection.Mode.CUSTOM;

            groupBinding.chipFull.setChipSelected(full);
            groupBinding.chipCustom.setChipSelected(custom);

            if (custom) {
                groupBinding.customCaption.setText(getString(R.string.home_attrs_selected,
                        selection.attrs.size(), group.attrs.size()));
                groupBinding.customCaption.setVisibility(View.VISIBLE);
            } else {
                groupBinding.customCaption.setVisibility(View.GONE);
            }
        }
        binding.startVerificationButton.setEnabled(state.hasRequest);
    }
}
