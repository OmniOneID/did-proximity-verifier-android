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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.omnione.did.proximity.verifier.data.model.BleTransferMode;
import org.omnione.did.proximity.verifier.data.model.VerifierSettings;
import org.omnione.did.proximity.verifier.data.repository.SettingsRepository;
import org.omnione.did.proximity.verifier.ui.Event;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SettingsViewModel extends ViewModel {

    private final SettingsRepository settingsRepository;
    private final MutableLiveData<VerifierSettings> state = new MutableLiveData<>();
    private final MutableLiveData<Event<BleTransferMode>> bleModeSheet = new MutableLiveData<>();

    @Inject
    public SettingsViewModel(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public LiveData<VerifierSettings> getState() {
        return state;
    }

    public LiveData<Event<BleTransferMode>> getBleModeSheet() {
        return bleModeSheet;
    }

    public void init() {
        state.setValue(settingsRepository.get());
    }

    public void setValue(String key, boolean value) {
        settingsRepository.set(key, value);
        state.setValue(settingsRepository.get());
    }

    public void openBleModeSheet() {
        bleModeSheet.setValue(new Event<>(settingsRepository.getBleMode()));
    }

    public void setBleMode(BleTransferMode mode) {
        settingsRepository.setBleMode(mode);
        state.setValue(settingsRepository.get());
    }

    public void setTransportOrder(List<String> order) {
        settingsRepository.setTransportOrder(order);
        state.setValue(settingsRepository.get());
    }
}
