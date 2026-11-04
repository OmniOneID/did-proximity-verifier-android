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

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public final class NfcEngagementUiState {

    public enum Status {
        WAITING,
        UNAVAILABLE,
        TIMED_OUT,
        FAILED
    }

    @NonNull public final Status status;
    @StringRes public final int messageRes;

    private NfcEngagementUiState(@NonNull Status status, @StringRes int messageRes) {
        this.status = status;
        this.messageRes = messageRes;
    }

    @NonNull
    public static NfcEngagementUiState waiting() {
        return new NfcEngagementUiState(Status.WAITING, 0);
    }

    @NonNull
    public static NfcEngagementUiState unavailable(@StringRes int messageRes) {
        return new NfcEngagementUiState(Status.UNAVAILABLE, messageRes);
    }

    @NonNull
    public static NfcEngagementUiState timedOut(@StringRes int messageRes) {
        return new NfcEngagementUiState(Status.TIMED_OUT, messageRes);
    }

    @NonNull
    public static NfcEngagementUiState failed(@StringRes int messageRes) {
        return new NfcEngagementUiState(Status.FAILED, messageRes);
    }

    public boolean isWaiting() {
        return status == Status.WAITING;
    }
}
