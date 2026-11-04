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
import android.content.SharedPreferences;

import org.omnione.did.proximity.sdk.core.engagement.TransportPreference;
import org.omnione.did.proximity.verifier.data.model.BleTransferMode;
import org.omnione.did.proximity.verifier.data.model.Transport;
import org.omnione.did.proximity.verifier.data.model.TransportPreferences;
import org.omnione.did.proximity.verifier.data.model.VerifierSettings;
import org.omnione.did.proximity.verifier.util.BluetoothReadiness;
import org.omnione.did.proximity.verifier.util.WifiAwareReadiness;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class SettingsRepository {

    private static final String PREFS = "verifier_settings";
    private static final String SEPARATOR = ",";

    private final SharedPreferences prefs;

    @Inject
    public SettingsRepository(@ApplicationContext Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public VerifierSettings get() {
        return new VerifierSettings(
                isSkipIssuerAuth(),
                isUseL2cap(),
                getTransportOrder(),
                getBleMode());
    }

    public boolean isUseL2cap() {
        return prefs.getBoolean(VerifierSettings.KEY_USE_L2CAP, false);
    }

    public void set(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    public boolean isSkipIssuerAuth() {
        return prefs.getBoolean(VerifierSettings.KEY_SKIP_ISSUER_AUTH, true);
    }

    public BleTransferMode getBleMode() {
        return BleTransferMode.of(prefs.getString(VerifierSettings.KEY_BLE_MODE, null));
    }

    public void setBleMode(BleTransferMode mode) {
        prefs.edit().putString(VerifierSettings.KEY_BLE_MODE, mode.id).apply();
    }

    public List<String> getTransportOrder() {
        String stored = prefs.getString(VerifierSettings.KEY_TRANSPORT_ORDER, null);
        if (stored == null || stored.isEmpty()) return Transport.defaultOrder();

        List<String> order = new ArrayList<>();
        for (String id : stored.split(SEPARATOR)) {
            if (Transport.of(id) != null && !order.contains(id)) order.add(id);
        }
        return order.isEmpty() ? Transport.defaultOrder() : order;
    }

    public TransportPreference getTransportPreference(Context context) {
        return getTransportPreference(context, false);
    }

    public TransportPreference getTransportPreference(Context context, boolean nfcAllowed) {
        return TransportPreferences.from(getTransportOrder(), getBleMode(),
                BluetoothReadiness.isEnabled(context), WifiAwareReadiness.isReady(context),
                nfcAllowed);
    }

    public boolean wantsWifiAware() {
        return getTransportOrder().contains(Transport.WIFI_AWARE.id);
    }

    public boolean wantsBle() {
        return getTransportOrder().contains(Transport.BLE.id);
    }

    public boolean wantsNfc() {
        return getTransportOrder().contains(Transport.NFC.id);
    }

    public void setTransportOrder(List<String> order) {
        if (order.isEmpty()) return;
        prefs.edit()
                .putString(VerifierSettings.KEY_TRANSPORT_ORDER, String.join(SEPARATOR, order))
                .apply();
    }
}
