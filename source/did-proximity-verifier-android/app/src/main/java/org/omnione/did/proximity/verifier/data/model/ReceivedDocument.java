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

import org.omnione.did.proximity.sdk.core.message.DeviceAuthResult;

import java.io.Serializable;
import java.util.LinkedHashMap;

public final class ReceivedDocument implements Serializable {

    private static final long serialVersionUID = 5L;

    public final String docType;
    public final LinkedHashMap<String, Serializable> claims;
    @Nullable public final String validFrom;
    @Nullable public final String validUntil;
    @NonNull public final DeviceAuthResult.Outcome deviceAuth;
    @NonNull public final IssuerVerdict issuerTrust;

    public ReceivedDocument(String docType, LinkedHashMap<String, Serializable> claims,
                            @Nullable String validFrom, @Nullable String validUntil,
                            @NonNull DeviceAuthResult.Outcome deviceAuth,
                            @NonNull IssuerVerdict issuerTrust) {
        this.docType = docType;
        this.claims = claims;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.deviceAuth = deviceAuth;
        this.issuerTrust = issuerTrust;
    }
}
