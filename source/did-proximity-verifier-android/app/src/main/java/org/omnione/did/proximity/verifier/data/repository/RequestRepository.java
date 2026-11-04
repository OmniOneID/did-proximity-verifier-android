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

package org.omnione.did.proximity.verifier.data.repository;

import android.content.Context;

import androidx.annotation.Nullable;

import org.omnione.did.proximity.verifier.R;
import org.omnione.did.proximity.verifier.data.model.DocCatalog;
import org.omnione.did.proximity.verifier.data.model.DocGroup;
import org.omnione.did.proximity.verifier.data.model.RequestRow;
import org.omnione.did.proximity.verifier.data.model.RequestSelection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RequestRepository {

    private final Map<String, RequestSelection> selections = new LinkedHashMap<>();

    @Inject
    public RequestRepository() {
    }

    @Nullable
    public RequestSelection get(String groupId) {
        return selections.get(groupId);
    }

    public boolean isEmpty() {
        return selections.isEmpty();
    }

    public void toggleFull(String groupId) {
        RequestSelection current = selections.get(groupId);
        selections.clear();
        if (current == null || current.mode != RequestSelection.Mode.FULL) {
            selections.put(groupId, RequestSelection.full());
        }
    }

    public void setFull(String groupId) {
        selections.clear();
        selections.put(groupId, RequestSelection.full());
    }

    public void setCustom(String groupId, List<String> attrs) {
        selections.clear();
        selections.put(groupId, RequestSelection.custom(attrs));
    }

    @Nullable
    public DocGroup selectedGroup() {
        for (DocGroup group : DocCatalog.groups()) {
            if (selections.containsKey(group.id)) return group;
        }
        return null;
    }

    @Nullable
    public List<String> selectedAttrs() {
        DocGroup group = selectedGroup();
        if (group == null) return null;
        RequestSelection selection = selections.get(group.id);
        return selection.mode == RequestSelection.Mode.FULL ? group.attrs : selection.attrs;
    }

    public void clear() {
        selections.clear();
    }

    public List<RequestRow> buildRows(Context context) {
        List<RequestRow> rows = new ArrayList<>();
        for (DocGroup group : DocCatalog.groups()) {
            RequestSelection selection = selections.get(group.id);
            if (selection == null) continue;
            String scope = selection.mode == RequestSelection.Mode.FULL
                    ? context.getString(R.string.transfer_scope_full)
                    : context.getString(R.string.transfer_scope_custom,
                            selection.attrs.size(), group.attrs.size());
            rows.add(new RequestRow(context.getString(group.shortRes), scope));
        }
        return rows;
    }

}
