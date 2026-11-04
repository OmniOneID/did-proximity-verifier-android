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

package org.omnione.did.proximity.verifier.ui.issuers;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.omnione.did.proximity.verifier.data.repository.TrustedIssuerRepository;
import org.omnione.did.proximity.verifier.ui.Event;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TrustedIssuersViewModel extends ViewModel {

    private final TrustedIssuerRepository trustedIssuerRepository;
    private final MutableLiveData<TrustedIssuersUiState> state = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> refreshFailed = new MutableLiveData<>();
    private final TrustedIssuerRepository.RefreshCallback refreshListener = success -> {
        if (!success) refreshFailed.setValue(new Event<>(true));
        publish();
    };

    @Inject
    public TrustedIssuersViewModel(TrustedIssuerRepository trustedIssuerRepository) {
        this.trustedIssuerRepository = trustedIssuerRepository;
        trustedIssuerRepository.addRefreshListener(refreshListener);
    }

    @Override
    protected void onCleared() {
        trustedIssuerRepository.removeRefreshListener(refreshListener);
        super.onCleared();
    }

    public LiveData<TrustedIssuersUiState> getState() {
        return state;
    }

    public LiveData<Event<Boolean>> getRefreshFailed() {
        return refreshFailed;
    }

    public void init() {
        publish();
    }

    public void refresh() {
        if (trustedIssuerRepository.isRefreshing()) return;
        trustedIssuerRepository.refresh();
        publish();
    }

    private void publish() {
        state.setValue(new TrustedIssuersUiState(
                trustedIssuerRepository.getIssuers(),
                trustedIssuerRepository.getUpdatedAt(),
                trustedIssuerRepository.isRefreshing()));
    }
}
