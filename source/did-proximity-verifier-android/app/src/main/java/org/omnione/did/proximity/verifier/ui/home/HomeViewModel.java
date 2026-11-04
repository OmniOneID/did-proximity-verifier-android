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

package org.omnione.did.proximity.verifier.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.omnione.did.proximity.verifier.data.model.DocCatalog;
import org.omnione.did.proximity.verifier.data.model.DocGroup;
import org.omnione.did.proximity.verifier.data.model.RequestSelection;
import org.omnione.did.proximity.verifier.data.repository.RequestRepository;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HomeViewModel extends ViewModel {

    private final RequestRepository requestRepository;
    private final MutableLiveData<HomeUiState> state = new MutableLiveData<>();

    @Inject
    public HomeViewModel(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    public LiveData<HomeUiState> getState() {
        return state;
    }

    public void refresh() {
        Map<String, RequestSelection> selections = new LinkedHashMap<>();
        for (DocGroup group : DocCatalog.groups()) {
            RequestSelection selection = requestRepository.get(group.id);
            if (selection != null) selections.put(group.id, selection);
        }
        state.setValue(new HomeUiState(selections));
    }

    public void toggleFull(String groupId) {
        requestRepository.toggleFull(groupId);
        refresh();
    }

    public boolean clearIfCustom(String groupId) {
        RequestSelection selection = requestRepository.get(groupId);
        if (selection == null || selection.mode != RequestSelection.Mode.CUSTOM) return false;
        requestRepository.clear();
        refresh();
        return true;
    }
}
