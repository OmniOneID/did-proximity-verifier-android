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

import androidx.annotation.Nullable;

import org.omnione.did.proximity.verifier.data.model.TrustedIssuer;

import java.util.Collections;
import java.util.List;

public final class TrustedIssuersUiState {

    public final List<TrustedIssuer> issuers;
    @Nullable
    public final String updatedAt;
    public final boolean refreshing;

    TrustedIssuersUiState(List<TrustedIssuer> issuers, @Nullable String updatedAt,
                          boolean refreshing) {
        this.issuers = Collections.unmodifiableList(issuers);
        this.updatedAt = updatedAt;
        this.refreshing = refreshing;
    }

    public boolean hasIssuers() {
        return !issuers.isEmpty();
    }
}
