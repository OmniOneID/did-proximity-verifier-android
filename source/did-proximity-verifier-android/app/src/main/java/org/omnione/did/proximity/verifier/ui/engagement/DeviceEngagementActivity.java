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

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.Lifecycle;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CameraPreview;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import org.omnione.did.proximity.sdk.core.engagement.DeviceEngagement;
import org.omnione.did.proximity.sdk.core.engagement.EngagementCodec;
import org.omnione.did.proximity.sdk.core.engagement.UnsupportedTransportException;
import org.omnione.did.proximity.verifier.R;
import org.omnione.did.proximity.verifier.databinding.ActivityDeviceEngagementBinding;
import org.omnione.did.proximity.verifier.ui.common.BaseActivity;
import org.omnione.did.proximity.verifier.util.IntentRouter;

import java.util.Collections;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DeviceEngagementActivity extends BaseActivity {

    public static final String EXTRA_PAYLOAD = "extra_payload";

    private static final String ACTION_PERMISSION_DENIED = "permissionDenied";
    private static final String ACTION_INVALID_ENGAGEMENT = "invalidEngagement";
    private static final String ACTION_CAMERA_UNAVAILABLE = "cameraUnavailable";

    private static final String TAG = "DeviceEngagement";
    private static final String STATE_PROCESSED = "processed";
    private static final String STATE_REJECTED = "rejected";
    private static final String STATE_REJECTED_UNTIL = "rejectedUntil";
    private static final long REJECTED_COOLDOWN_MS = 2000;

    private ActivityDeviceEngagementBinding binding;
    private ActivityResultLauncher<String> permissionLauncher;

    private boolean cameraStarted;
    private boolean processed;
    private String rejected;
    private long rejectedUntil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            processed = savedInstanceState.getBoolean(STATE_PROCESSED);
            rejected = savedInstanceState.getString(STATE_REJECTED);
            rejectedUntil = savedInstanceState.getLong(STATE_REJECTED_UNTIL);
        }
        binding = ActivityDeviceEngagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        new WindowInsetsControllerCompat(getWindow(), binding.getRoot())
                .setAppearanceLightStatusBars(false);
        applySystemBarInsets(binding.getRoot(), bars ->
                applyTopInsetPadding(binding.topContainer, bars.top));

        binding.closeButton.setOnClickListener(v -> finish());

        String injected = getIntent().getStringExtra(EXTRA_PAYLOAD);
        if (injected != null) {
            if (savedInstanceState == null) onEngagementRead(injected);
            return;
        }

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        startScanner();
                    } else {
                        showPermissionDeniedDialog();
                    }
                });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startScanner();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String injected = intent.getStringExtra(EXTRA_PAYLOAD);
        if (injected != null) {
            onEngagementRead(injected);
        }
    }

    private void startScanner() {
        binding.cameraView.setDecoderFactory(
                new DefaultDecoderFactory(Collections.singletonList(BarcodeFormat.QR_CODE)));
        binding.cameraView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                onEngagementRead(result.getText());
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
            }
        });
        binding.cameraView.addStateListener(new CameraPreview.StateListener() {
            @Override
            public void previewSized() {
            }

            @Override
            public void previewStarted() {
            }

            @Override
            public void previewStopped() {
            }

            @Override
            public void cameraError(Exception error) {
                Log.w(TAG, "camera unavailable", error);
                if (processed || getSupportFragmentManager().isStateSaved()) return;
                processed = true;
                showNotice(R.string.common_error, R.string.engagement_camera_unavailable_message,
                        ACTION_CAMERA_UNAVAILABLE);
            }

            @Override
            public void cameraClosed() {
            }
        });
        if (!processed) binding.cameraView.resume();
        cameraStarted = true;
    }

    private void onEngagementRead(String engagement) {
        if (processed || getSupportFragmentManager().isStateSaved()) return;
        if (engagement.equals(rejected) && SystemClock.elapsedRealtime() < rejectedUntil) {
            Log.i(TAG, "ignored the engagement rejected moments ago");
            return;
        }
        processed = true;
        if (cameraStarted) binding.cameraView.pause();
        if (!isDeviceEngagement(engagement)) {
            rejected = engagement;
            showNotice(R.string.common_error, R.string.engagement_invalid_message,
                    ACTION_INVALID_ENGAGEMENT);
            return;
        }
        Log.i(TAG, "deviceEngagement=" + engagement);
        IntentRouter.startTransferStatus(this, engagement);
        finish();
    }

    private static boolean isDeviceEngagement(String engagement) {
        try {
            DeviceEngagement.parse(EngagementCodec.decode(engagement));
            return true;
        } catch (UnsupportedTransportException e) {
            return true;
        } catch (RuntimeException e) {
            Log.w(TAG, "invalid device engagement", e);
            return false;
        }
    }

    private void showPermissionDeniedDialog() {
        showNotice(R.string.common_error, R.string.engagement_permission_message,
                ACTION_PERMISSION_DENIED);
    }

    @Override
    protected void onNoticeConfirmed(@NonNull String action) {
        if (ACTION_INVALID_ENGAGEMENT.equals(action)) {
            rejectedUntil = SystemClock.elapsedRealtime() + REJECTED_COOLDOWN_MS;
            binding.getRoot().post(() -> {
                if (!getSupportFragmentManager().isStateSaved()) {
                    getSupportFragmentManager().executePendingTransactions();
                }
                processed = false;
                if (cameraStarted
                        && getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    binding.cameraView.resume();
                }
            });
        } else if (ACTION_PERMISSION_DENIED.equals(action)
                || ACTION_CAMERA_UNAVAILABLE.equals(action)) {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraStarted && !processed) {
            binding.cameraView.resume();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_PROCESSED, processed);
        outState.putString(STATE_REJECTED, rejected);
        outState.putLong(STATE_REJECTED_UNTIL, rejectedUntil);
    }

    @Override
    protected void onPause() {
        if (cameraStarted) {
            binding.cameraView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations() && !isFinishing()) {
            finish();
        }
    }
}
