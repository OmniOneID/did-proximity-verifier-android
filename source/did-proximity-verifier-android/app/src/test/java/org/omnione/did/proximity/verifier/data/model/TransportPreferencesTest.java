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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.omnione.did.proximity.sdk.core.engagement.TransportMode;
import org.omnione.did.proximity.verifier.R;

import java.util.Arrays;
import java.util.Collections;

public class TransportPreferencesTest {

    @Test
    public void theDefaultOrderPutsBleBeforeWifiAwareAndSkipsNfc() {
        assertEquals(Arrays.asList(TransportMode.BLE_CENTRAL_CLIENT,
                        TransportMode.BLE_PERIPHERAL_SERVER, TransportMode.WIFI_AWARE),
                TransportPreferences.from(Transport.defaultOrder(),
                        BleTransferMode.READER_PERIPHERAL_SERVER, true).order());
    }

    @Test
    public void wifiAwareFirstIsHonoured() {
        assertEquals(Arrays.asList(TransportMode.WIFI_AWARE,
                        TransportMode.BLE_PERIPHERAL_SERVER, TransportMode.BLE_CENTRAL_CLIENT),
                TransportPreferences.from(Arrays.asList("wifiAware", "ble"),
                        BleTransferMode.READER_CENTRAL_CLIENT, true).order());
    }

    @Test
    public void anUnavailableWifiAwareIsDroppedQuietly() {
        assertEquals(Arrays.asList(TransportMode.BLE_CENTRAL_CLIENT,
                        TransportMode.BLE_PERIPHERAL_SERVER),
                TransportPreferences.from(Arrays.asList("wifiAware", "ble", "nfc"),
                        BleTransferMode.READER_PERIPHERAL_SERVER, false).order());
    }

    @Test
    public void wifiAwareOnlyTriesWifiAwareAlone() {
        assertEquals(Collections.singletonList(TransportMode.WIFI_AWARE),
                TransportPreferences.from(Collections.singletonList("wifiAware"),
                        BleTransferMode.READER_PERIPHERAL_SERVER, true).order());
    }

    @Test
    public void anUncheckedBleIsNeverAddedBack() {
        assertEquals(Collections.singletonList(TransportMode.WIFI_AWARE),
                TransportPreferences.from(Arrays.asList("wifiAware", "nfc"),
                        BleTransferMode.READER_PERIPHERAL_SERVER, true).order());
    }

    @Test
    public void nfcIsHonouredInPlaceWhenTheTapAllowsIt() {
        assertEquals(Arrays.asList(TransportMode.NFC, TransportMode.BLE_PERIPHERAL_SERVER,
                        TransportMode.BLE_CENTRAL_CLIENT, TransportMode.WIFI_AWARE),
                TransportPreferences.from(Arrays.asList("nfc", "ble", "wifiAware"),
                        BleTransferMode.READER_CENTRAL_CLIENT, true, true, true).order());
        assertEquals(Arrays.asList(TransportMode.BLE_CENTRAL_CLIENT,
                        TransportMode.BLE_PERIPHERAL_SERVER, TransportMode.NFC),
                TransportPreferences.from(Arrays.asList("ble", "nfc"),
                        BleTransferMode.READER_PERIPHERAL_SERVER, true, true, true).order());
    }

    @Test
    public void nfcOnlyIsUsableWhenTheTapAllowsIt() {
        assertEquals(Collections.singletonList(TransportMode.NFC),
                TransportPreferences.from(Collections.singletonList("nfc"),
                        BleTransferMode.READER_PERIPHERAL_SERVER, true, true, true).order());
    }

    @Test
    public void theThreeArgOverloadStillSkipsNfc() {
        assertEquals(Arrays.asList(TransportMode.BLE_CENTRAL_CLIENT,
                        TransportMode.BLE_PERIPHERAL_SERVER),
                TransportPreferences.from(Arrays.asList("nfc", "ble"),
                        BleTransferMode.READER_PERIPHERAL_SERVER, true).order());
    }

    @Test
    public void nfcOnlyOnAQrSessionIsNotRecommended() {
        NoUsableTransportException e = assertThrows(NoUsableTransportException.class,
                () -> TransportPreferences.from(Collections.singletonList("nfc"),
                        BleTransferMode.READER_PERIPHERAL_SERVER, true));
        assertEquals(R.string.transfer_nfc_not_recommended_message, e.messageRes);
    }

    @Test
    public void wifiAwareOnlyWithoutWifiAwareReportsNoCompatibleTransport() {
        NoUsableTransportException e = assertThrows(NoUsableTransportException.class,
                () -> TransportPreferences.from(Collections.singletonList("wifiAware"),
                        BleTransferMode.READER_PERIPHERAL_SERVER, false));
        assertEquals(R.string.transfer_error_transport_mismatch, e.messageRes);
    }

    @Test
    public void nfcAndUnavailableWifiAwareOnAQrSessionIsNotRecommended() {
        NoUsableTransportException e = assertThrows(NoUsableTransportException.class,
                () -> TransportPreferences.from(Arrays.asList("nfc", "wifiAware"),
                        BleTransferMode.READER_PERIPHERAL_SERVER, false));
        assertEquals(R.string.transfer_nfc_not_recommended_message, e.messageRes);
    }

    @Test
    public void bluetoothOffDropsBleAndKeepsTheOtherTransports() {
        assertEquals(Arrays.asList(TransportMode.WIFI_AWARE, TransportMode.NFC),
                TransportPreferences.from(Arrays.asList("ble", "wifiAware", "nfc"),
                        BleTransferMode.READER_PERIPHERAL_SERVER, false, true, true).order());
    }

    @Test
    public void bluetoothOffOnTheNfcPathFallsBackToNfc() {
        assertEquals(Collections.singletonList(TransportMode.NFC),
                TransportPreferences.from(Arrays.asList("ble", "nfc"),
                        BleTransferMode.READER_PERIPHERAL_SERVER, false, false, true).order());
    }

    @Test
    public void bluetoothOffWithNothingElseUsableReportsBluetoothOff() {
        NoUsableTransportException e = assertThrows(NoUsableTransportException.class,
                () -> TransportPreferences.from(Arrays.asList("ble", "wifiAware"),
                        BleTransferMode.READER_PERIPHERAL_SERVER, false, false, false));
        assertEquals(R.string.transfer_bluetooth_off_message, e.messageRes);
    }

    @Test
    public void bluetoothOffOutranksNfcDroppedFromAQrSession() {
        NoUsableTransportException e = assertThrows(NoUsableTransportException.class,
                () -> TransportPreferences.from(Arrays.asList("ble", "nfc"),
                        BleTransferMode.READER_PERIPHERAL_SERVER, false, true, false));
        assertEquals(R.string.transfer_bluetooth_off_message, e.messageRes);
    }
}
