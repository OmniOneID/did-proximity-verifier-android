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

import org.junit.Test;
import org.omnione.did.proximity.sdk.core.engagement.BleMode;
import org.omnione.did.proximity.sdk.core.nfc.BleCarrier;

import java.util.EnumSet;
import java.util.UUID;

public class BleTransferModeNfcLeRoleTest {

    private static final int AD_LE_ROLE = 0x1C;
    private static final int BOTH_PERIPHERAL_PREFERRED = 0x02;
    private static final int BOTH_CENTRAL_PREFERRED = 0x03;

    private static final UUID SERVICE_UUID =
            UUID.fromString("3a82d455-d5fc-4db3-bea0-6fd2f6ff32c8");

    private static int handoverRequestLeRole(BleTransferMode setting) {
        byte[] oob = BleCarrier
                .of(EnumSet.of(BleMode.CENTRAL_CLIENT, BleMode.PERIPHERAL_SERVER), SERVICE_UUID)
                .toHandoverRequest(setting.holderModePreference());

        int pos = 0;
        while (pos < oob.length) {
            int length = oob[pos] & 0xFF;
            if ((oob[pos + 1] & 0xFF) == AD_LE_ROLE) {
                return oob[pos + 2] & 0xFF;
            }
            pos += length + 1;
        }
        throw new AssertionError("no LE Role AD structure");
    }

    @Test
    public void defaultSettingRequestsMdocCentralClientMode() {
        assertEquals(BOTH_PERIPHERAL_PREFERRED, handoverRequestLeRole(BleTransferMode.DEFAULT));
    }

    @Test
    public void readerPeripheralServerEmitsPeripheralPreferred() {
        assertEquals(BOTH_PERIPHERAL_PREFERRED,
                handoverRequestLeRole(BleTransferMode.READER_PERIPHERAL_SERVER));
    }

    @Test
    public void readerCentralClientEmitsCentralPreferredDespiteItsName() {
        assertEquals(
                "READER_CENTRAL_CLIENT is named for the reader's own BLE role, so it maps to"
                        + " PERIPHERAL_SERVER_FIRST and then to LE Role 0x03. Expecting 0x02 here"
                        + " because the name says \"central\" is exactly the inversion this guards.",
                BOTH_CENTRAL_PREFERRED,
                handoverRequestLeRole(BleTransferMode.READER_CENTRAL_CLIENT));
    }

    @Test
    public void theTwoSettingsNeverProduceTheSameLeRole() {
        assertEquals(false,
                handoverRequestLeRole(BleTransferMode.READER_CENTRAL_CLIENT)
                        == handoverRequestLeRole(BleTransferMode.READER_PERIPHERAL_SERVER));
    }

    @Test
    public void staleStoredIdFallsBackToTheDefaultLeRole() {
        assertEquals(BOTH_PERIPHERAL_PREFERRED,
                handoverRequestLeRole(BleTransferMode.of("peripheral_server")));
        assertEquals(BOTH_CENTRAL_PREFERRED,
                handoverRequestLeRole(BleTransferMode.of("central")));
    }
}
