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

import com.upokecenter.cbor.CBORObject;

import org.junit.Test;
import org.omnione.did.proximity.sdk.core.message.DeviceAuthResult;
import org.omnione.did.proximity.sdk.core.message.DeviceAuthVerifier;

import java.util.Collections;
import java.util.List;

public class DeviceAuthIndexTest {

    private static List<DeviceAuthResult> verdicts() {
        CBORObject withoutDeviceSigned = CBORObject.NewMap();
        withoutDeviceSigned.Add("docType", "org.omnione.pid.1");

        CBORObject bothMechanisms = CBORObject.NewMap();
        bothMechanisms.Add("deviceSignature", CBORObject.NewArray());
        bothMechanisms.Add("deviceMac", CBORObject.NewArray());
        CBORObject deviceSigned = CBORObject.NewMap();
        deviceSigned.Add("deviceAuth", bothMechanisms);
        CBORObject rejected = CBORObject.NewMap();
        rejected.Add("docType", "org.omnione.pid.1");
        rejected.Add("deviceSigned", deviceSigned);

        CBORObject documents = CBORObject.NewArray();
        documents.Add(withoutDeviceSigned);
        documents.Add(rejected);

        CBORObject response = CBORObject.NewMap();
        response.Add("documents", documents);

        return DeviceAuthVerifier.verify(
                response.EncodeToBytes(), new byte[]{(byte) 0x80}, null);
    }

    @Test
    public void eachDocumentTakesTheVerdictAtItsOwnIndex() {
        List<DeviceAuthResult> list = verdicts();

        assertEquals(2, list.size());
        assertEquals(DeviceAuthResult.Outcome.ABSENT,
                TransferStatusViewModel.deviceAuthOf(list, 0));
        assertEquals(DeviceAuthResult.Outcome.INVALID,
                TransferStatusViewModel.deviceAuthOf(list, 1));
    }

    @Test
    public void aMissingVerdictNeverReadsAsVerifiedOrAbsent() {
        assertEquals(DeviceAuthResult.Outcome.INVALID,
                TransferStatusViewModel.deviceAuthOf(Collections.emptyList(), 0));
        assertEquals(DeviceAuthResult.Outcome.INVALID,
                TransferStatusViewModel.deviceAuthOf(verdicts(), 2));
    }
}
