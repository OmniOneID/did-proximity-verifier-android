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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;

import org.omnione.did.proximity.verifier.R;
import org.omnione.did.proximity.verifier.data.model.BleTransferMode;
import org.omnione.did.proximity.verifier.data.model.Transport;
import org.omnione.did.proximity.verifier.data.model.VerifierSettings;
import org.omnione.did.proximity.verifier.databinding.ActivitySettingsBinding;
import org.omnione.did.proximity.verifier.databinding.ItemSettingRowBinding;
import org.omnione.did.proximity.verifier.ui.common.BaseActivity;
import org.omnione.did.proximity.verifier.util.IntentRouter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsActivity extends BaseActivity
        implements BleModeSheetFragment.OnModeSelected {

    private ActivitySettingsBinding binding;
    private SettingsViewModel viewModel;
    private final Map<String, ItemSettingRowBinding> rows = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        applySystemBarInsets(binding.getRoot(), bars -> {
            applyTopInsetPadding(binding.appBar.getRoot(), bars.top);
            applyBottomInsetPadding(binding.content, dp(24), bars.bottom);
        });

        binding.appBar.appBarTitle.setText(R.string.settings_title);
        binding.appBar.appBarLeft.setImageResource(R.drawable.ic_back);
        binding.appBar.appBarLeft.setOnClickListener(v -> finish());

        binding.rowTrustedIssuers.navTitle.setText(R.string.settings_trusted_issuers);
        binding.rowTrustedIssuers.getRoot()
                .setOnClickListener(v -> IntentRouter.startTrustedIssuers(this));

        binding.rowBleMode.valueTitle.setText(R.string.settings_ble_mode_title);

        bindRow(VerifierSettings.KEY_SKIP_ISSUER_AUTH, binding.rowSkipIssuerAuth,
                R.string.settings_skip_issuer_auth);
        bindRow(VerifierSettings.KEY_USE_L2CAP, binding.rowUseL2cap,
                R.string.settings_use_l2cap);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        binding.rowBleMode.getRoot().setOnClickListener(v -> viewModel.openBleModeSheet());
        binding.transportList.setOnOrderChanged(viewModel::setTransportOrder);
        viewModel.getState().observe(this, this::render);
        viewModel.getBleModeSheet().observe(this, event -> {
            BleTransferMode selected = event.getIfNotHandled();
            if (selected == null) return;
            BleModeSheetFragment.newInstance(selected).show(getSupportFragmentManager());
        });
        viewModel.init();
    }

    private void bindRow(String key, ItemSettingRowBinding row, int titleRes) {
        row.settingTitle.setText(titleRes);

        row.settingToggle.setContentDescription(getText(titleRes));
        row.settingToggle.setOnCheckedChangeListener(
                (toggle, checked) -> viewModel.setValue(key, checked));
        rows.put(key, row);
    }

    private void render(VerifierSettings settings) {
        for (Map.Entry<String, ItemSettingRowBinding> entry : rows.entrySet()) {
            entry.getValue().settingToggle
                    .setCheckedSilently(settings.valueOf(entry.getKey()), false);
        }
        binding.rowBleMode.valueSummary.setText(settings.bleMode.labelRes);
        binding.transportList.setOrder(settings.transportOrder);
        binding.transportSummary.setText(transportSummary(settings.transportOrder));
    }

    @Override
    public void onBleModeSelected(@NonNull BleTransferMode mode) {
        viewModel.setBleMode(mode);
    }

    private String transportSummary(List<String> order) {
        if (order.isEmpty()) return "";
        if (order.size() == 1) return getString(R.string.settings_transports_only, label(order.get(0)));

        StringBuilder summary = new StringBuilder();
        String separator = getString(R.string.settings_transports_separator);
        for (int i = 0; i < order.size(); i++) {
            if (i > 0) summary.append(separator);
            summary.append(label(order.get(i)));
        }
        return summary.toString();
    }

    private String label(String id) {
        Transport transport = Transport.of(id);
        return transport == null ? id : getString(transport.labelRes);
    }
}
