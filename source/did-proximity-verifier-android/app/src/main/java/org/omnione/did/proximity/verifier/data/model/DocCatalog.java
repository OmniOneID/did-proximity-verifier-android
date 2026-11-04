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

import androidx.annotation.Nullable;

import org.omnione.did.proximity.verifier.R;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DocCatalog {

    public static final String ID_PID = "pid";

    private static final String NS_PID = "org.omnione.pid.1";

    private static final Map<String, String> PID_ELEMENTS = elements(
            "Family Name(s)", "family_name",
            "Given Name(s)", "given_name",
            "Birth Date", "birth_date",
            "Portrait", "portrait");

    private static Map<String, String> elements(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

    private static final List<DocGroup> GROUPS = Collections.singletonList(
            new DocGroup(ID_PID,
                    NS_PID,
                    NS_PID,
                    R.string.home_group_pid_title,
                    R.string.doc_pid,
                    R.string.doc_short_pid,
                    R.string.custom_attrs_title_pid,
                    R.string.chip_full_pid,
                    R.string.chip_custom_pid,
                    PID_ELEMENTS));

    private DocCatalog() {
    }

    public static List<DocGroup> groups() {
        return GROUPS;
    }

    @Nullable
    public static DocGroup byId(String id) {
        for (DocGroup g : GROUPS) {
            if (g.id.equals(id)) return g;
        }
        return null;
    }
}
