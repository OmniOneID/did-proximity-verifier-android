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

package org.omnione.did.proximity.verifier.ui.attributes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;

import org.omnione.did.proximity.verifier.R;
import org.omnione.did.proximity.verifier.data.model.DocGroup;
import org.omnione.did.proximity.verifier.databinding.ActivityCustomAttrsBinding;
import org.omnione.did.proximity.verifier.databinding.ItemAttrRowBinding;
import org.omnione.did.proximity.verifier.ui.common.BaseActivity;
import org.omnione.did.proximity.verifier.util.IntentRouter;

import java.util.LinkedHashMap;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CustomAttrsActivity extends BaseActivity {

    private ActivityCustomAttrsBinding binding;
    private CustomAttrsViewModel viewModel;
    private final Map<String, ItemAttrRowBinding> attrRows = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomAttrsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        applySystemBarInsets(binding.getRoot(), bars -> {
            applyTopInsetPadding(binding.appBar.getRoot(), bars.top);
            applyBottomInsetPadding(binding.bottomBar, dp(16), bars.bottom);
        });

        binding.appBar.appBarLeft.setImageResource(R.drawable.ic_back);
        binding.appBar.appBarLeft.setOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(CustomAttrsViewModel.class);
        viewModel.init(getIntent().getStringExtra(IntentRouter.EXTRA_GROUP_ID));

        DocGroup group = viewModel.getGroup();
        if (group == null) {
            finish();
            return;
        }

        binding.appBar.appBarTitle.setText(group.customTitleRes);
        binding.docName.setText(group.docRes);
        binding.docHeader.setOnClickListener(v -> viewModel.toggleExpanded());
        binding.selectAll.setOnClickListener(v -> viewModel.toggleAll());
        binding.cancelButton.setOnClickListener(v -> finish());
        binding.doneButton.setOnClickListener(v -> {
            viewModel.commit();
            finish();
        });

        buildAttrRows(group);
        viewModel.getState().observe(this, this::render);
    }

    private void buildAttrRows(DocGroup group) {
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < group.attrs.size(); i++) {
            String attr = group.attrs.get(i);
            ItemAttrRowBinding row =
                    ItemAttrRowBinding.inflate(inflater, binding.attrContainer, false);
            row.attrLabel.setText(attr);
            row.getRoot().setOnClickListener(v -> viewModel.toggle(attr));
            binding.attrContainer.addView(row.getRoot());
            attrRows.put(attr, row);

            if (i < group.attrs.size() - 1) {
                binding.attrContainer.addView(inflater.inflate(
                        R.layout.view_divider, binding.attrContainer, false));
            }
        }
    }

    private void render(CustomAttrsUiState state) {
        for (Map.Entry<String, ItemAttrRowBinding> entry : attrRows.entrySet()) {
            entry.getValue().attrCheckBox.setChecked(state.picked.contains(entry.getKey()));
        }
        binding.selectAllCheckBox.setChecked(state.allSelected);

        binding.attrsSection.setVisibility(state.expanded ? View.VISIBLE : View.GONE);
        binding.docHeaderChevron.setImageResource(state.expanded
                ? R.drawable.ic_chevron_up
                : R.drawable.ic_chevron_down);
        binding.card.setPadding(dp(16), dp(4), dp(16), state.expanded ? dp(12) : 0);

        binding.doneButton.setEnabled(state.doneEnabled);
    }
}
