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

package org.omnione.did.proximity.verifier.ui.attributes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.omnione.did.proximity.verifier.data.model.DocCatalog;
import org.omnione.did.proximity.verifier.data.model.DocGroup;
import org.omnione.did.proximity.verifier.data.repository.RequestRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CustomAttrsViewModel extends ViewModel {

    private final RequestRepository requestRepository;
    private final MutableLiveData<CustomAttrsUiState> state = new MutableLiveData<>();

    private DocGroup group;
    private final Set<String> picked = new LinkedHashSet<>();
    private boolean expanded = true;

    @Inject
    public CustomAttrsViewModel(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    public LiveData<CustomAttrsUiState> getState() {
        return state;
    }

    public DocGroup getGroup() {
        return group;
    }

    public void init(String groupId) {
        if (group != null) return;
        group = DocCatalog.byId(groupId);
        if (group == null) return;

        picked.addAll(group.attrs);
        publish();
    }

    public void toggle(String attr) {
        if (!picked.remove(attr)) picked.add(attr);
        publish();
    }

    public void toggleAll() {
        if (picked.size() == group.attrs.size()) {
            picked.clear();
        } else {
            picked.clear();
            picked.addAll(group.attrs);
        }
        publish();
    }

    public void toggleExpanded() {
        expanded = !expanded;
        publish();
    }

    public void commit() {
        ArrayList<String> ordered = new ArrayList<>();
        for (String attr : group.attrs) {
            if (picked.contains(attr)) ordered.add(attr);
        }
        if (ordered.size() == group.attrs.size()) {
            requestRepository.setFull(group.id);
        } else {
            requestRepository.setCustom(group.id, ordered);
        }
    }

    private void publish() {
        state.setValue(new CustomAttrsUiState(
                new LinkedHashSet<>(picked), expanded, group.attrs.size()));
    }
}
