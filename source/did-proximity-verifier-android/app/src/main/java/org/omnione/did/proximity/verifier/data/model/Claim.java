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

import java.util.Collections;
import java.util.List;

public final class Claim {

    public final String label;
    @Nullable public final String value;
    @Nullable public final List<Claim> children;
    @Nullable public final byte[] image;

    public Claim(String label, String value) {
        this.label = label;
        this.value = value;
        this.children = null;
        this.image = null;
    }

    public Claim(String label, List<Claim> children) {
        this.label = label;
        this.value = null;
        this.children = Collections.unmodifiableList(children);
        this.image = null;
    }

    public Claim(String label, byte[] image, @Nullable String fallback) {
        this.label = label;
        this.value = fallback;
        this.children = null;
        this.image = image;
    }

    public boolean isGroup() {
        return children != null;
    }

    public boolean isImage() {
        return image != null;
    }
}
