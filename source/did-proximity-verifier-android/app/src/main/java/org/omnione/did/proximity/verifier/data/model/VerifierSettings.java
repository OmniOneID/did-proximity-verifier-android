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

import java.util.Collections;
import java.util.List;

public final class VerifierSettings {

    public static final String KEY_SKIP_ISSUER_AUTH = "skipIssuerAuth";
    public static final String KEY_USE_L2CAP = "useL2cap";
    public static final String KEY_TRANSPORT_ORDER = "transportOrder";
    public static final String KEY_BLE_MODE = "bleMode";

    public final boolean skipIssuerAuth;
    public final boolean useL2cap;
    public final List<String> transportOrder;
    public final BleTransferMode bleMode;

    public VerifierSettings(boolean skipIssuerAuth, boolean useL2cap,
                            List<String> transportOrder, BleTransferMode bleMode) {
        this.skipIssuerAuth = skipIssuerAuth;
        this.useL2cap = useL2cap;
        this.transportOrder = Collections.unmodifiableList(transportOrder);
        this.bleMode = bleMode;
    }

    public boolean valueOf(String key) {
        switch (key) {
            case KEY_SKIP_ISSUER_AUTH: return skipIssuerAuth;
            case KEY_USE_L2CAP: return useL2cap;
            default: return false;
        }
    }
}
