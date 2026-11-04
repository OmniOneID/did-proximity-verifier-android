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

package org.omnione.did.proximity.verifier.ui.transfer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;

import org.omnione.did.proximity.sdk.reader.nfc.NfcTagDispatchGuard;
import org.omnione.did.proximity.verifier.R;
import org.omnione.did.proximity.verifier.data.model.RequestRow;
import org.omnione.did.proximity.verifier.databinding.ItemRequestRowBinding;
import org.omnione.did.proximity.verifier.databinding.ActivityTransferStatusBinding;
import org.omnione.did.proximity.verifier.ui.common.BaseActivity;
import org.omnione.did.proximity.verifier.util.BluetoothReadiness;
import org.omnione.did.proximity.verifier.util.IntentRouter;
import org.omnione.did.proximity.verifier.util.WifiAwareReadiness;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TransferStatusActivity extends BaseActivity {

    public static final String EXTRA_ENGAGEMENT = "extra_engagement";
    public static final String EXTRA_SOURCE = "extra_source";
    public static final String SOURCE_NFC = "nfc";

    private static final String ACTION_TRANSFER_FAILED = "transferFailed";

    private ActivityTransferStatusBinding binding;
    private TransferStatusViewModel viewModel;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private String engagement;
    private boolean fromNfc;
    private NfcTagDispatchGuard tagDispatchGuard;
    private boolean noticeShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTransferStatusBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        applySystemBarInsets(binding.getRoot(), bars -> {
            applyTopInsetPadding(binding.appBar.getRoot(), bars.top);
            applyBottomInsetPadding(binding.bottomBar, dp(16), bars.bottom);
        });

        binding.appBar.appBarTitle.setText(R.string.transfer_title);
        binding.appBar.appBarLeft.setImageResource(R.drawable.ic_back);
        binding.appBar.appBarLeft.setOnClickListener(v -> cancelAndLeave());
        binding.dismissButton.setOnClickListener(v -> cancelAndLeave());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cancelAndLeave();
            }
        });

        viewModel = new ViewModelProvider(this).get(TransferStatusViewModel.class);
        viewModel.getState().observe(this, this::render);

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                results -> {
                    if (hasBluetoothPermissions()) {
                        startTransfer();
                    } else {
                        viewModel.reportUnavailable(R.string.transfer_permission_message);
                    }
                });

        tagDispatchGuard = NfcTagDispatchGuard.create(this);
        engagement = getIntent().getStringExtra(EXTRA_ENGAGEMENT);
        fromNfc = SOURCE_NFC.equals(getIntent().getStringExtra(EXTRA_SOURCE));
        if (engagement == null && !fromNfc) {
            finish();
            return;
        }
        if (viewModel.isSettled()) {
            return;
        }
        if (hasBluetoothPermissions() && hasOptionalWifiAwarePermissions()) {
            startTransfer();
        } else {
            permissionLauncher.launch(transferPermissions());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (fromNfc) {
            tagDispatchGuard.engage();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        tagDispatchGuard.release();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations() && !isFinishing() && !viewModel.isReceived()) {
            viewModel.cancel();
            finish();
        }
    }

    private void startTransfer() {
        if (!hasLocationServices()) {
            viewModel.reportUnavailable(R.string.transfer_location_message);
            return;
        }
        if (fromNfc) {
            viewModel.startFromNfc(this);
        } else {
            viewModel.start(this, engagement);
        }
    }

    private boolean hasLocationServices() {
        return BluetoothReadiness.hasLocationServices(this);
    }

    private String[] transferPermissions() {
        String[] bluetooth = BluetoothReadiness.permissions();
        if (!viewModel.wantsWifiAware(this)) {
            return bluetooth;
        }
        LinkedHashSet<String> all = new LinkedHashSet<>(Arrays.asList(bluetooth));
        all.addAll(Arrays.asList(WifiAwareReadiness.permissions()));
        return all.toArray(new String[0]);
    }

    private boolean hasBluetoothPermissions() {
        return BluetoothReadiness.hasPermissions(this);
    }

    private boolean hasOptionalWifiAwarePermissions() {
        return !viewModel.wantsWifiAware(this) || WifiAwareReadiness.hasPermissions(this);
    }

    private void cancelAndLeave() {
        viewModel.cancel();
        IntentRouter.startHome(this);
        finish();
    }

    private void render(TransferStatusUiState state) {
        if (state.status == TransferStatusUiState.Status.RECEIVED) {
            viewModel.publishReceivedDocuments();
            IntentRouter.startShowDocuments(this);
            finish();
            return;
        }

        bindRequestRows(state.rows);
        binding.statusText.setText(statusLabel(state.status));

        if (state.isFailed()) showFailureNotice(state);
    }

    private void bindRequestRows(List<RequestRow> rows) {
        binding.requestTitle.setVisibility(rows.isEmpty() ? View.GONE : View.VISIBLE);
        binding.requestContainer.setVisibility(rows.isEmpty() ? View.GONE : View.VISIBLE);
        if (binding.requestContainer.getChildCount() > 0 || rows.isEmpty()) return;

        for (int i = 0; i < rows.size(); i++) {
            RequestRow row = rows.get(i);
            ItemRequestRowBinding item = ItemRequestRowBinding.inflate(
                    getLayoutInflater(), binding.requestContainer, false);
            item.requestLabel.setText(row.label);
            item.requestScope.setText(row.scope);
            binding.requestContainer.addView(item.getRoot());
            if (i < rows.size() - 1) {
                binding.requestContainer.addView(getLayoutInflater().inflate(
                        R.layout.view_divider, binding.requestContainer, false));
            }
        }
    }

    private void showFailureNotice(TransferStatusUiState state) {
        if (noticeShown) return;
        noticeShown = true;

        CharSequence message;
        if (state.messageRes != 0) {
            message = getText(state.messageRes);
        } else if (state.status == TransferStatusUiState.Status.TERMINATED) {
            message = getText(R.string.transfer_error_message);
        } else if (state.errorMessage != null && !state.errorMessage.isEmpty()) {
            message = state.errorMessage;
        } else {
            message = getText(R.string.transfer_error_unexpected);
        }
        showNotice(getText(state.titleRes), message, ACTION_TRANSFER_FAILED);
    }

    @Override
    protected void onNoticeConfirmed(@NonNull String action) {
        if (ACTION_TRANSFER_FAILED.equals(action)) cancelAndLeave();
    }

    private String statusLabel(TransferStatusUiState.Status status) {
        switch (status) {
            case TERMINATED:
                return getString(R.string.transfer_status_disconnected);
            case CONNECTED:
                return getString(R.string.transfer_status_connected);
            case CONNECTING:
            case FAILED:
            case RECEIVED:
            default:
                return getString(R.string.transfer_status_connecting);
        }
    }
}
