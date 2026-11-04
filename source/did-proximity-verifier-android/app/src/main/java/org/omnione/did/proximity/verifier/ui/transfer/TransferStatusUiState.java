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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.omnione.did.proximity.sdk.core.ProximityException;
import org.omnione.did.proximity.sdk.core.engagement.DeviceEngagement;
import org.omnione.did.proximity.sdk.core.engagement.EngagementCodec;
import org.omnione.did.proximity.sdk.core.engagement.TransportMode;
import org.omnione.did.proximity.sdk.core.engagement.TransportPreference;
import org.omnione.did.proximity.sdk.core.engagement.UnsupportedTransportException;
import org.omnione.did.proximity.verifier.R;
import org.omnione.did.proximity.verifier.data.model.RequestRow;

import java.util.Collections;
import java.util.List;

public final class TransferStatusUiState {

    public enum Status {
        CONNECTING, CONNECTED, RECEIVED, FAILED, TERMINATED
    }

    public final List<RequestRow> rows;
    public final Status status;
    @Nullable public final String errorMessage;
    @StringRes public final int messageRes;
    @StringRes public final int titleRes;

    private TransferStatusUiState(List<RequestRow> rows,
                                  Status status,
                                  @Nullable String errorMessage,
                                  @StringRes int messageRes) {
        this.rows = Collections.unmodifiableList(rows);
        this.status = status;
        this.errorMessage = errorMessage;
        this.messageRes = messageRes;
        this.titleRes = messageRes == R.string.transfer_nfc_not_recommended_message
                ? R.string.transfer_nfc_not_recommended_title
                : R.string.common_error;
    }

    public static TransferStatusUiState connecting(List<RequestRow> rows) {
        return new TransferStatusUiState(rows, Status.CONNECTING, null, 0);
    }

    public static TransferStatusUiState connected(List<RequestRow> rows) {
        return new TransferStatusUiState(rows, Status.CONNECTED, null, 0);
    }

    public static TransferStatusUiState received(List<RequestRow> rows) {
        return new TransferStatusUiState(rows, Status.RECEIVED, null, 0);
    }

    public static TransferStatusUiState failed(List<RequestRow> rows, @Nullable String message) {
        return new TransferStatusUiState(rows, Status.FAILED, message, 0);
    }

    public static TransferStatusUiState fromError(List<RequestRow> rows,
                                                  @NonNull ProximityException error) {
        if (error instanceof UnsupportedTransportException) {
            return unavailable(rows, R.string.transfer_error_transport_mismatch);
        }
        if (error.reason() == ProximityException.Reason.ENGAGEMENT_TIMEOUT
                || error.reason() == ProximityException.Reason.RESPONSE_TIMEOUT) {
            return new TransferStatusUiState(rows, Status.FAILED, error.getMessage(),
                    R.string.transfer_error_message);
        }
        return failed(rows, error.getMessage());
    }

    public static TransferStatusUiState fromQrError(List<RequestRow> rows,
                                                    @NonNull ProximityException error,
                                                    @NonNull String engagement,
                                                    boolean bleDropped,
                                                    boolean nfcDropped) {
        if (error instanceof UnsupportedTransportException) {
            if (bleDropped && offers(engagement,
                    TransportMode.BLE_CENTRAL_CLIENT, TransportMode.BLE_PERIPHERAL_SERVER)) {
                return unavailable(rows, R.string.transfer_bluetooth_off_message);
            }
            if (nfcDropped && offers(engagement, TransportMode.NFC)) {
                return unavailable(rows, R.string.transfer_nfc_not_recommended_message);
            }
        }
        return fromError(rows, error);
    }

    private static boolean offers(@NonNull String engagement, @NonNull TransportMode... modes) {
        try {
            DeviceEngagement.parse(EngagementCodec.decode(engagement), TransportPreference.of(modes));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static TransferStatusUiState unavailable(List<RequestRow> rows, @StringRes int messageRes) {
        return new TransferStatusUiState(rows, Status.FAILED, null, messageRes);
    }

    public static TransferStatusUiState terminated(List<RequestRow> rows) {
        return new TransferStatusUiState(rows, Status.TERMINATED, null, 0);
    }

    public boolean isFailed() {
        return status == Status.FAILED || status == Status.TERMINATED;
    }
}
