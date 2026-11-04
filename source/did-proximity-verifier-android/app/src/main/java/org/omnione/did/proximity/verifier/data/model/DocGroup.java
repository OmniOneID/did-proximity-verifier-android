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

package org.omnione.did.proximity.verifier.data.model;

import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DocGroup {

    public final String id;
    public final String docType;
    public final String nameSpace;
    @StringRes public final int titleRes;
    @StringRes public final int docRes;
    @StringRes public final int shortRes;
    @StringRes public final int customTitleRes;
    @StringRes public final int chipFullRes;
    @StringRes public final int chipCustomRes;
    public final List<String> attrs;

    private final Map<String, String> elements;

    DocGroup(String id,
             String docType,
             String nameSpace,
             @StringRes int titleRes,
             @StringRes int docRes,
             @StringRes int shortRes,
             @StringRes int customTitleRes,
             @StringRes int chipFullRes,
             @StringRes int chipCustomRes,
             Map<String, String> elements) {
        this.id = id;
        this.docType = docType;
        this.nameSpace = nameSpace;
        this.titleRes = titleRes;
        this.docRes = docRes;
        this.shortRes = shortRes;
        this.customTitleRes = customTitleRes;
        this.chipFullRes = chipFullRes;
        this.chipCustomRes = chipCustomRes;
        this.elements = Collections.unmodifiableMap(new LinkedHashMap<>(elements));
        this.attrs = Collections.unmodifiableList(new ArrayList<>(this.elements.keySet()));
    }

    public String elementOf(String attr) {
        return elements.get(attr);
    }
}
