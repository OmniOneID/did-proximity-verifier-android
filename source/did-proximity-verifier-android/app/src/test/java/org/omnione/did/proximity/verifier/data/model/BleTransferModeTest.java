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
import org.omnione.did.proximity.sdk.core.engagement.ModePreference;

public class BleTransferModeTest {

    @Test
    public void readerPeripheralServerPrefersHolderCentralClient() {
        assertEquals(ModePreference.CENTRAL_CLIENT_FIRST,
                BleTransferMode.READER_PERIPHERAL_SERVER.holderModePreference());
    }

    @Test
    public void readerCentralClientPrefersHolderPeripheralServer() {
        assertEquals(ModePreference.PERIPHERAL_SERVER_FIRST,
                BleTransferMode.READER_CENTRAL_CLIENT.holderModePreference());
    }

    @Test
    public void defaultModeIsReaderPeripheralServer() {
        assertEquals(BleTransferMode.READER_PERIPHERAL_SERVER, BleTransferMode.DEFAULT);
    }

    @Test
    public void storedIdsResolveToTheirModes() {
        assertEquals(BleTransferMode.READER_CENTRAL_CLIENT, BleTransferMode.of("central"));
        assertEquals(BleTransferMode.READER_PERIPHERAL_SERVER, BleTransferMode.of("peripheral"));
    }

    @Test
    public void nullIdFallsBackToDefault() {
        assertEquals(BleTransferMode.DEFAULT, BleTransferMode.of(null));
    }

    @Test
    public void unknownIdFallsBackToDefault() {
        assertEquals(BleTransferMode.DEFAULT, BleTransferMode.of("peripheral_server"));
    }
}
