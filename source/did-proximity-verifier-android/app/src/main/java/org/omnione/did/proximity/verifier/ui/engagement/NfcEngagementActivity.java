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

package org.omnione.did.proximity.verifier.ui.engagement;

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.Base64;
import java.util.UUID;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;

import org.omnione.did.proximity.sdk.core.ProximityException;
import org.omnione.did.proximity.sdk.core.engagement.BleMode;
import org.omnione.did.proximity.sdk.core.engagement.TransportMode;
import org.omnione.did.proximity.sdk.core.nfc.HandoverMessages;
import org.omnione.did.proximity.sdk.core.nfc.NfcHandoverResult;
import org.omnione.did.proximity.sdk.core.nfc.NfcHandoverResults;
import org.omnione.did.proximity.sdk.reader.ReaderSession;
import org.omnione.did.proximity.sdk.reader.nfc.NfcEngagementReader;
import org.omnione.did.proximity.verifier.R;
import org.omnione.did.proximity.verifier.data.model.NoUsableTransportException;
import org.omnione.did.proximity.verifier.data.repository.SettingsRepository;
import org.omnione.did.proximity.verifier.databinding.ActivityNfcEngagementBinding;
import org.omnione.did.proximity.verifier.ui.common.BaseActivity;
import org.omnione.did.proximity.verifier.ui.transfer.TransferStatusUiState;
import org.omnione.did.proximity.verifier.ui.transfer.TransferStatusViewModel;
import org.omnione.did.proximity.verifier.util.BluetoothReadiness;
import org.omnione.did.proximity.verifier.util.IntentRouter;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NfcEngagementActivity extends BaseActivity {

    private static final String TAG = "NfcEngagement";
    private static final String ACTION_LEAVE = "nfcLeave";
    private static final String EXTRA_HANDOVER_SELECT = "extra_handover_select";
    private static final String EXTRA_HANDOVER_REQUEST = "extra_handover_request";

    @Inject SettingsRepository settingsRepository;

    private ActivityNfcEngagementBinding binding;
    private NfcEngagementViewModel viewModel;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private final Handler timeout = new Handler(Looper.getMainLooper());

    @Nullable private NfcEngagementReader reader;
    @Nullable private ObjectAnimator pulse;
    @Nullable private TransferStatusViewModel transferViewModel;
    @Nullable private NfcHandoverResult nfcHandover;
    private boolean handled;
    private boolean readerRunning;
    private boolean transferring;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNfcEngagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        applySystemBarInsets(binding.getRoot(), bars -> {
            applyTopInsetPadding(binding.appBar.getRoot(), bars.top);
            applyBottomInsetPadding(binding.bottomBar, dp(16), bars.bottom);
        });

        viewModel = new ViewModelProvider(this).get(NfcEngagementViewModel.class);

        binding.appBar.appBarTitle.setText(R.string.nfc_engagement_title);
        binding.appBar.appBarLeft.setImageResource(R.drawable.ic_back);
        binding.appBar.appBarLeft.setOnClickListener(v -> leave());
        binding.cancelButton.setOnClickListener(v -> leave());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                leave();
            }
        });

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                granted -> armEngagement());

        viewModel.state().observe(this, this::render);

        if (viewModel.isSettled()) {
            return;
        }
        if (!settingsRepository.wantsBle() && !settingsRepository.wantsNfc()) {
            viewModel.reportUnavailable(R.string.transfer_error_transport_mismatch);
            return;
        }
        if (injectHandover()) {
            return;
        }
        if (!settingsRepository.wantsBle() || BluetoothReadiness.hasPermissions(this)) {
            armEngagement();
        } else {
            permissionLauncher.launch(BluetoothReadiness.permissions());
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (viewModel.isSettled()) {
            return;
        }
        injectHandover();
    }

    private boolean injectHandover() {
        String select = getIntent().getStringExtra(EXTRA_HANDOVER_SELECT);
        if (select == null) {
            return false;
        }
        try {
            byte[] handoverSelect = Base64.getUrlDecoder().decode(select);
            String request = getIntent().getStringExtra(EXTRA_HANDOVER_REQUEST);
            byte[] handoverRequest =
                    request == null ? null : Base64.getUrlDecoder().decode(request);
            HandoverMessages.Selected selected =
                    HandoverMessages.parseHandoverSelect(handoverSelect);
            if (selected.carrier == null) {
                throw new ProximityException("the injected Handover Select carries no BLE carrier");
            }
            UUID serviceUuid = selected.carrier.serviceUuid();
            if (serviceUuid == null) {
                throw new ProximityException("the injected Handover Select carries no UUID");
            }
            BleMode mode = settingsRepository.getBleMode().holderModePreference()
                    .preferred(selected.carrier.modes());
            onTapped(NfcHandoverResults.of(selected.deviceEngagement, mode, serviceUuid,
                    handoverSelect, handoverRequest,
                    mode == BleMode.PERIPHERAL_SERVER ? selected.carrier.psm() : null));
        } catch (RuntimeException e) {
            Log.w(TAG, "injected handover rejected", e);
            viewModel.reportFailure(R.string.transfer_error_message);
        }
        return true;
    }

    private void armEngagement() {
        if (viewModel.isSettled()) {
            return;
        }
        if (settingsRepository.wantsBle()) {
            if (!BluetoothReadiness.hasPermissions(this)) {
                viewModel.reportFailure(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        ? R.string.transfer_permission_message
                        : R.string.transfer_location_message);
                return;
            }
            if (!settingsRepository.wantsNfc() && !BluetoothReadiness.isEnabled(this)) {
                viewModel.reportUnavailable(R.string.transfer_bluetooth_off_message);
                return;
            }
            if (!BluetoothReadiness.hasLocationServices(this)) {
                viewModel.reportFailure(R.string.transfer_location_message);
                return;
            }
        }
        if (!NfcEngagementReader.isSupported(this) || !NfcEngagementReader.isEnabled(this)) {
            viewModel.reportUnavailable(R.string.nfc_unavailable_message);
            return;
        }
        try {
            reader = NfcEngagementReader.create(this,
                    settingsRepository.getTransportPreference(this, true));
        } catch (NoUsableTransportException e) {
            viewModel.reportUnavailable(e.messageRes);
            return;
        } catch (ProximityException e) {
            viewModel.reportUnavailable(R.string.nfc_unavailable_message);
            return;
        }
        startReader();
        timeout.postDelayed(this::onTimedOut, NfcEngagementViewModel.TAP_TIMEOUT_MS);
    }

    private void onTapped(@NonNull NfcHandoverResult result) {
        if (handled) {
            NfcHandoverResults.closeQuietly(result);
            return;
        }
        if (result.selected() == TransportMode.NFC) {
            startNfcTransfer(result);
            return;
        }
        if (!viewModel.publish(result)) {
            return;
        }
        handled = true;
        timeout.removeCallbacksAndMessages(null);
        Log.i(TAG, "handoverSelect=" + hex(result.handoverSelect()));
        IntentRouter.startTransferStatusFromNfc(this);
        finish();
    }

    private void startNfcTransfer(@NonNull NfcHandoverResult result) {
        if (!viewModel.claim()) {
            NfcHandoverResults.closeQuietly(result);
            return;
        }
        handled = true;
        transferring = true;
        nfcHandover = result;
        timeout.removeCallbacksAndMessages(null);
        Log.i(TAG, "handoverSelect=" + hex(result.handoverSelect())
                + " transport=nfc, hosting the transfer in place");
        binding.nfcTitle.setText(R.string.nfc_transfer_title);
        binding.nfcMessage.setText(R.string.nfc_transfer_message);
        TransferStatusViewModel transfer =
                new ViewModelProvider(this).get(TransferStatusViewModel.class);
        transferViewModel = transfer;
        transfer.getState().observe(this, this::renderTransfer);
        transfer.startFromNfc(this, result);
    }

    private void renderTransfer(@NonNull TransferStatusUiState state) {
        switch (state.status) {
            case RECEIVED:
                if (!transferring) {
                    return;
                }
                transferring = false;
                stopPulse();
                if (transferViewModel != null) {
                    transferViewModel.publishReceivedDocuments();
                }
                NfcHandoverResult handover = nfcHandover;
                NfcLinkCloseWatch.start(timeout,
                        () -> handover != null && handover.channel() != null
                                && handover.channel().isLinkOpen(),
                        ReaderSession.TERMINATION_GRACE_MS * 4,
                        this::leaveForShowDocuments);
                return;
            case FAILED:
            case TERMINATED:
                if (!transferring) {
                    return;
                }
                transferring = false;
                stopPulse();
                releaseReader();
                showNotice(getText(state.titleRes), transferFailureMessage(state),
                        ACTION_LEAVE);
                return;
            default:
                return;
        }
    }

    private void leaveForShowDocuments() {
        Log.i(TAG, "nfc link closed after termination, releasing reader mode");
        IntentRouter.startShowDocumentsFromNfc(this);
        finish();
    }

    @NonNull
    private CharSequence transferFailureMessage(@NonNull TransferStatusUiState state) {
        if (state.messageRes != 0) {
            return getText(state.messageRes);
        }
        if (state.status == TransferStatusUiState.Status.TERMINATED) {
            return getText(R.string.transfer_error_message);
        }
        if (state.errorMessage != null && !state.errorMessage.isEmpty()) {
            return state.errorMessage;
        }
        return getText(R.string.transfer_error_unexpected);
    }

    private void onTimedOut() {
        if (handled) {
            return;
        }
        releaseReader();
        viewModel.reportTimeout(R.string.nfc_timeout_message);
    }

    private void render(@NonNull NfcEngagementUiState state) {
        if (state.isWaiting()) {
            startPulse();
            return;
        }
        stopPulse();
        releaseReader();
        showNotice(getText(R.string.common_error), getText(state.messageRes), ACTION_LEAVE);
    }

    @Override
    protected void onNoticeConfirmed(@NonNull String action) {
        if (ACTION_LEAVE.equals(action)) {
            leave();
        }
    }

    private void startPulse() {
        if (pulse != null) {
            return;
        }
        pulse = ObjectAnimator.ofPropertyValuesHolder(binding.nfcHalo,
                PropertyValuesHolder.ofKeyframe(View.SCALE_X,
                        Keyframe.ofFloat(0f, 1f),
                        Keyframe.ofFloat(0.5f, 1.08f),
                        Keyframe.ofFloat(1f, 1f)),
                PropertyValuesHolder.ofKeyframe(View.SCALE_Y,
                        Keyframe.ofFloat(0f, 1f),
                        Keyframe.ofFloat(0.5f, 1.08f),
                        Keyframe.ofFloat(1f, 1f)),
                PropertyValuesHolder.ofKeyframe(View.ALPHA,
                        Keyframe.ofFloat(0f, 1f),
                        Keyframe.ofFloat(0.5f, 0.7f),
                        Keyframe.ofFloat(1f, 1f)));
        pulse.setDuration(1800L);
        pulse.setInterpolator(new AccelerateDecelerateInterpolator());
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setRepeatMode(ValueAnimator.RESTART);
        pulse.start();
    }

    private void stopPulse() {
        if (pulse == null) {
            return;
        }
        pulse.cancel();
        pulse = null;
        binding.nfcHalo.setScaleX(1f);
        binding.nfcHalo.setScaleY(1f);
        binding.nfcHalo.setAlpha(1f);
    }

    private void releaseReader() {
        NfcEngagementReader active = reader;
        if (active != null && readerRunning) {
            readerRunning = false;
            active.stop();
        }
    }

    private void leave() {
        IntentRouter.startHome(this);
        abandon();
    }

    private void abandon() {
        timeout.removeCallbacksAndMessages(null);
        if (transferring && transferViewModel != null) {
            transferring = false;
            transferViewModel.cancel();
        }
        releaseReader();
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations() && !isFinishing()
                && (transferViewModel == null || !transferViewModel.isReceived())) {
            abandon();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!handled) {
            releaseReader();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        NfcEngagementUiState state = viewModel.state().getValue();
        if (!handled && !readerRunning && reader != null && state != null && state.isWaiting()) {
            startReader();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timeout.removeCallbacksAndMessages(null);
        stopPulse();
        NfcEngagementReader active = reader;
        if (active != null) {
            active.close();
            reader = null;
        }
    }

    private void startReader() {
        NfcEngagementReader active = reader;
        if (active == null || readerRunning) {
            return;
        }
        readerRunning = true;
        active.start(new NfcEngagementReader.Listener() {
            @Override public void onHandover(@NonNull NfcHandoverResult result) {
                onTapped(result);
            }

            @Override public void onError(@NonNull ProximityException error) {
                Log.i(TAG, "nfc handover failed", error);
                viewModel.reportFailure(R.string.transfer_error_message);
            }
        });
    }

    @NonNull
    private static String hex(@NonNull byte[] value) {
        StringBuilder text = new StringBuilder(value.length * 2);
        for (byte b : value) {
            text.append(Character.forDigit((b >> 4) & 0x0F, 16));
            text.append(Character.forDigit(b & 0x0F, 16));
        }
        return text.toString();
    }
}
