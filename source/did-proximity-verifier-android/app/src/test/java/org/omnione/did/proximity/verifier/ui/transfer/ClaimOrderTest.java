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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClaimOrderTest {

    @Test
    public void claimsFollowTheRequestOrderAndKeepUnrequestedOnesAfter() {
        Map<String, Object> received = new LinkedHashMap<>();
        received.put("given_name", "Raon");
        received.put("extra", "x");
        received.put("portrait", new byte[]{1});
        received.put("family_name", "Kim");
        received.put("birth_date", "2026-10-22");

        Map<String, Object> ordered = TransferStatusViewModel.inRequestOrder(received,
                Arrays.asList("family_name", "given_name", "birth_date", "portrait", "missing"));

        assertEquals(Arrays.asList("family_name", "given_name", "birth_date", "portrait", "extra"),
                new ArrayList<>(ordered.keySet()));
        assertEquals("Kim", ordered.get("family_name"));
    }
}
