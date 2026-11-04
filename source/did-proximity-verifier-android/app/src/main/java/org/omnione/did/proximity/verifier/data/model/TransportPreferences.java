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

import org.omnione.did.proximity.sdk.core.engagement.TransportMode;
import org.omnione.did.proximity.sdk.core.engagement.TransportPreference;
import org.omnione.did.proximity.verifier.R;

import java.util.ArrayList;
import java.util.List;

public final class TransportPreferences {

    private TransportPreferences() {
    }

    @NonNull
    public static TransportPreference from(@NonNull List<String> order,
                                           @NonNull BleTransferMode bleMode,
                                           boolean wifiAwareAvailable) {
        return from(order, bleMode, true, wifiAwareAvailable, false);
    }

    @NonNull
    public static TransportPreference from(@NonNull List<String> order,
                                           @NonNull BleTransferMode bleMode,
                                           boolean bleAvailable,
                                           boolean wifiAwareAvailable,
                                           boolean nfcAllowed) {
        List<TransportMode> modes = new ArrayList<>();
        for (String id : order) {
            Transport transport = Transport.of(id);
            if (transport == Transport.BLE && bleAvailable) {
                modes.addAll(bleMode.holderModePreference().transports());
            } else if (transport == Transport.WIFI_AWARE && wifiAwareAvailable) {
                modes.add(TransportMode.WIFI_AWARE);
            } else if (transport == Transport.NFC && nfcAllowed) {
                modes.add(TransportMode.NFC);
            }
        }
        if (modes.isEmpty()) {
            if (order.contains(Transport.BLE.id)) {
                throw new NoUsableTransportException("BLE is selected but Bluetooth is off",
                        R.string.transfer_bluetooth_off_message);
            }
            if (order.contains(Transport.NFC.id)) {
                throw new NoUsableTransportException("NFC is dropped from a QR session"
                        + " and nothing else is usable",
                        R.string.transfer_nfc_not_recommended_message);
            }
            throw new NoUsableTransportException("no selected transport is usable",
                    R.string.transfer_error_transport_mismatch);
        }
        return TransportPreference.of(modes);
    }
}
