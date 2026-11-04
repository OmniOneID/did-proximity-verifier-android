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

package org.omnione.did.proximity.verifier.ui.documents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.omnione.did.proximity.sdk.core.message.DeviceAuthResult;
import org.omnione.did.proximity.verifier.data.model.IssuerVerdict;
import org.omnione.did.proximity.verifier.data.model.ReceivedDocument;
import org.omnione.did.proximity.verifier.data.model.VerificationItem;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;

public class DeviceAuthRowTest {

    private static ReceivedDocument document(DeviceAuthResult.Outcome outcome) {
        return new ReceivedDocument("org.omnione.pid.1",
                new LinkedHashMap<String, Serializable>(), null, null, outcome,
                IssuerVerdict.skipped());
    }

    @Test
    public void onlyAVerifiedOutcomeRendersYes() {
        assertEquals("Yes",
                ShowDocumentsViewModel.deviceAuthValue(DeviceAuthResult.Outcome.VERIFIED));
        assertEquals("No",
                ShowDocumentsViewModel.deviceAuthValue(DeviceAuthResult.Outcome.INVALID));
        assertEquals("No",
                ShowDocumentsViewModel.deviceAuthValue(DeviceAuthResult.Outcome.ABSENT));
    }

    @Test
    public void theScreenCollapsesTheReasonButTheModelKeepsIt() {
        ReceivedDocument absent = document(DeviceAuthResult.Outcome.ABSENT);
        ReceivedDocument invalid = document(DeviceAuthResult.Outcome.INVALID);

        assertEquals(ShowDocumentsViewModel.verification(absent).get(1).value,
                ShowDocumentsViewModel.verification(invalid).get(1).value);
        assertNotEquals(absent.deviceAuth, invalid.deviceAuth);
    }

    @Test
    public void theDeviceAuthRowSitsDirectlyBelowIssuerTrusted() {
        List<VerificationItem> rows = ShowDocumentsViewModel.verification(
                document(DeviceAuthResult.Outcome.VERIFIED));

        assertEquals(4, rows.size());
        assertEquals("Issuer Trusted", rows.get(0).label);
        assertEquals("Device Auth", rows.get(1).label);
        assertEquals("Valid from", rows.get(2).label);
        assertEquals("Valid until", rows.get(3).label);
    }

    @Test
    public void theDeviceAuthRowIsNeverHighlighted() {
        for (DeviceAuthResult.Outcome outcome : DeviceAuthResult.Outcome.values()) {
            List<VerificationItem> rows = ShowDocumentsViewModel.verification(document(outcome));
            assertFalse(outcome.toString(), rows.get(1).danger);
        }
    }

    @Test
    public void skippingIssuerVerificationDoesNotChangeTheDeviceAuthRow() {
        List<VerificationItem> rows = ShowDocumentsViewModel.verification(
                document(DeviceAuthResult.Outcome.INVALID));

        assertEquals("Skipped", rows.get(0).value);
        assertEquals("No", rows.get(1).value);
    }
}
