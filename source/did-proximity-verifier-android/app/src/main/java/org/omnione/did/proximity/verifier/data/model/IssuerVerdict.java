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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

public final class IssuerVerdict implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Outcome { YES, NO, SKIPPED }

    public enum Reason { unidentified, notListed, docType, signature }

    @NonNull public final Outcome outcome;
    @Nullable public final String name;
    @Nullable public final Reason reason;
    @Nullable public final String kid;

    private IssuerVerdict(@NonNull Outcome outcome, @Nullable String name,
                          @Nullable Reason reason, @Nullable String kid) {
        this.outcome = outcome;
        this.name = name;
        this.reason = reason;
        this.kid = kid;
    }

    @NonNull
    public static IssuerVerdict yes(@Nullable String name, @Nullable String kid) {
        return new IssuerVerdict(Outcome.YES, name, null, kid);
    }

    @NonNull
    public static IssuerVerdict no(@NonNull Reason reason, @Nullable String kid) {
        return new IssuerVerdict(Outcome.NO, null, reason, kid);
    }

    @NonNull
    public static IssuerVerdict skipped() {
        return new IssuerVerdict(Outcome.SKIPPED, null, null, null);
    }
}
