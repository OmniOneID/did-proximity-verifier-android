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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.omnione.did.proximity.sdk.core.nfc.NfcHandoverResult;
import org.omnione.did.proximity.verifier.data.repository.NfcHandoverRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class NfcEngagementViewModel extends ViewModel {

    public static final long TAP_TIMEOUT_MS = 60_000L;

    private final NfcHandoverRepository handoverRepository;
    private final MutableLiveData<NfcEngagementUiState> state =
            new MutableLiveData<>(NfcEngagementUiState.waiting());

    private boolean settled;

    @Inject
    public NfcEngagementViewModel(@NonNull NfcHandoverRepository handoverRepository) {
        this.handoverRepository = handoverRepository;
    }

    @NonNull
    public LiveData<NfcEngagementUiState> state() {
        return state;
    }

    public boolean isSettled() {
        return settled;
    }

    public boolean publish(@NonNull NfcHandoverResult result) {
        if (!claim()) {
            return false;
        }
        handoverRepository.set(result);
        return true;
    }

    public boolean claim() {
        if (settled) {
            return false;
        }
        settled = true;
        return true;
    }

    public void report(@NonNull NfcEngagementUiState next) {
        if (settled) {
            return;
        }
        settled = next.status != NfcEngagementUiState.Status.WAITING;
        state.setValue(next);
    }

    public void reportUnavailable(@StringRes int messageRes) {
        report(NfcEngagementUiState.unavailable(messageRes));
    }

    public void reportTimeout(@StringRes int messageRes) {
        report(NfcEngagementUiState.timedOut(messageRes));
    }

    public void reportFailure(@StringRes int messageRes) {
        report(NfcEngagementUiState.failed(messageRes));
    }
}
