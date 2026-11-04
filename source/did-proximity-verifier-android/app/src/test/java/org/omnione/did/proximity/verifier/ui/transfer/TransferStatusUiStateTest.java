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

package org.omnione.did.proximity.verifier.ui.transfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.omnione.did.proximity.sdk.core.ProximityException;
import org.omnione.did.proximity.sdk.core.engagement.UnsupportedTransportException;
import org.omnione.did.proximity.verifier.R;
import org.omnione.did.proximity.verifier.data.model.RequestRow;

import java.util.Collections;
import java.util.List;

public class TransferStatusUiStateTest {

    private static final List<RequestRow> ROWS =
            Collections.singletonList(new RequestRow("PID", "Full"));

    @Test
    public void anUnsupportedTransportBecomesTheTransportMismatchNotice() {
        TransferStatusUiState state = TransferStatusUiState.fromError(ROWS,
                new UnsupportedTransportException(
                        "no supported device retrieval method in DeviceEngagement", null));

        assertEquals(TransferStatusUiState.Status.FAILED, state.status);
        assertEquals(R.string.transfer_error_transport_mismatch, state.messageRes);
        assertNull(state.errorMessage);
    }

    private static final String NFC_ONLY_ENGAGEMENT = "mdoc:owBjMS4wAYIB2BhYS6QBAiABIVgga73FxI4gjzyjd80_yKdpkFYRTVuGVOqiO5dYGHdzFl4iWCCVl9bEBZKyyUxSWqY1EKRsot8Z6ytGx_4EycpmIsdyzwKBgwEBogAY_wEZAQA";

    private static final String BLE_ONLY_ENGAGEMENT = "mdoc:owBjMS4wAYIB2BhYS6QBAiABIVgga73FxI4gjzyjd80_yKdpkFYRTVuGVOqiO5dYGHdzFl4iWCCVl9bEBZKyyUxSWqY1EKRsot8Z6ytGx_4EycpmIsdyzwKBgwIBowD0AfULUHxNvgAAAEAAgAAAAAAAwN4";

    private static final UnsupportedTransportException NO_OVERLAP = new UnsupportedTransportException(
            "no supported device retrieval method in DeviceEngagement", null);

    @Test
    public void connectAndResponseTimeoutsBecomeTheInterruptedNotice() {
        for (ProximityException.Reason reason : new ProximityException.Reason[] {
                ProximityException.Reason.ENGAGEMENT_TIMEOUT,
                ProximityException.Reason.RESPONSE_TIMEOUT}) {
            TransferStatusUiState state = TransferStatusUiState.fromError(ROWS,
                    new ProximityException("the mdoc did not connect in time", reason));

            assertEquals(TransferStatusUiState.Status.FAILED, state.status);
            assertEquals(R.string.transfer_error_message, state.messageRes);
            assertEquals(R.string.common_error, state.titleRes);
            assertEquals("the mdoc did not connect in time", state.errorMessage);
        }
    }

    @Test
    public void aQrHolderOfferingOnlyNfcIsNotRecommended() {
        TransferStatusUiState state = TransferStatusUiState.fromQrError(ROWS, NO_OVERLAP,
                NFC_ONLY_ENGAGEMENT, false, true);

        assertEquals(R.string.transfer_nfc_not_recommended_message, state.messageRes);
        assertEquals(R.string.transfer_nfc_not_recommended_title, state.titleRes);
    }

    @Test
    public void aQrHolderOfferingOnlyNfcIsAMismatchWhenTheReaderHasNoNfc() {
        TransferStatusUiState state = TransferStatusUiState.fromQrError(ROWS, NO_OVERLAP,
                NFC_ONLY_ENGAGEMENT, false, false);

        assertEquals(R.string.transfer_error_transport_mismatch, state.messageRes);
        assertEquals(R.string.common_error, state.titleRes);
    }

    @Test
    public void bluetoothOffOutranksNfcWhenTheHolderOffersBle() {
        TransferStatusUiState state = TransferStatusUiState.fromQrError(ROWS, NO_OVERLAP,
                BLE_ONLY_ENGAGEMENT, true, true);

        assertEquals(R.string.transfer_bluetooth_off_message, state.messageRes);
    }

    @Test
    public void bluetoothOffIsNotBlamedWhenTheHolderOffersNoBle() {
        TransferStatusUiState state = TransferStatusUiState.fromQrError(ROWS, NO_OVERLAP,
                NFC_ONLY_ENGAGEMENT, true, true);

        assertEquals(R.string.transfer_nfc_not_recommended_message, state.messageRes);
    }

    @Test
    public void anyOtherErrorKeepsItsOwnMessage() {
        TransferStatusUiState state = TransferStatusUiState.fromError(ROWS,
                new ProximityException("gatt disconnected"));

        assertEquals(TransferStatusUiState.Status.FAILED, state.status);
        assertEquals(0, state.messageRes);
        assertEquals("gatt disconnected", state.errorMessage);
    }
}
