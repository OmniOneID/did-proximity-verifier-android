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

import org.omnione.did.proximity.verifier.R;

import java.util.ArrayList;
import java.util.List;

public enum Transport {

    BLE("ble", R.string.transport_ble),
    WIFI_AWARE("wifiAware", R.string.transport_wifi_aware),
    NFC("nfc", R.string.transport_nfc);

    public final String id;
    @StringRes public final int labelRes;

    Transport(String id, @StringRes int labelRes) {
        this.id = id;
        this.labelRes = labelRes;
    }

    public static Transport of(String id) {
        for (Transport transport : values()) {
            if (transport.id.equals(id)) return transport;
        }
        return null;
    }

    public static List<String> defaultOrder() {
        List<String> ids = new ArrayList<>();
        for (Transport transport : values()) {
            ids.add(transport.id);
        }
        return ids;
    }
}
