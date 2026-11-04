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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TrustedIssuerResponseTest {

    private static final String DID = "did:omn:issuer";
    private static final String KEY = "z27NeEZoqzMgsCGqDsWtx7vkSNXXtfP1MgT5a9wGiT9uHE";

    @Test
    public void readsTheServerListShape() {
        List<TrustedIssuerListResponse.Item> items = TrustedIssuerListResponse.parseItems(
                "{\"updatedAt\":\"2026-10-16T02:34:59Z\",\"count\":1,\"items\":[{\"did\":\"did:omn:issuer\","
                        + "\"name\":\"OpenDID Issuer\",\"docTypes\":[\"eu.europa.ec.eudi.pid.1\"],\"extra\":1}]}");

        assertEquals(1, items.size());
        assertEquals(DID, items.get(0).did);
        assertEquals("OpenDID Issuer", items.get(0).name());
        assertEquals(Collections.singletonList("eu.europa.ec.eudi.pid.1"), items.get(0).docTypes());
    }

    @Test
    public void aMissingOrNullNameBecomesEmpty() {
        List<TrustedIssuerListResponse.Item> items = TrustedIssuerListResponse.parseItems(
                "{\"items\":[{\"did\":\"did:omn:a\"},{\"did\":\"did:omn:b\",\"name\":null}]}");

        assertEquals("", items.get(0).name());
        assertEquals("", items.get(1).name());
    }

    @Test
    public void missingDocTypesBecomeEmpty() {
        assertTrue(TrustedIssuerListResponse.parseItems("{\"items\":[{\"did\":\"did:omn:a\"}]}")
                .get(0).docTypes().isEmpty());
    }

    @Test
    public void anEmptyListParsesToNoItems() {
        assertTrue(TrustedIssuerListResponse.parseItems("{\"count\":0,\"items\":[]}").isEmpty());
    }

    @Test
    public void anItemWithoutADidFailsTheRefresh() {
        assertThrows(RuntimeException.class, () -> TrustedIssuerListResponse.parseItems(
                "{\"items\":[{\"name\":\"x\",\"docTypes\":[]}]}"));
        assertThrows(RuntimeException.class, () -> TrustedIssuerListResponse.parseItems(
                "{\"items\":[{\"did\":null}]}"));
    }

    @Test
    public void aResponseWithoutItemsOrNotJsonFailsTheRefresh() {
        assertThrows(RuntimeException.class, () -> TrustedIssuerListResponse.parseItems("{}"));
        assertThrows(RuntimeException.class, () -> TrustedIssuerListResponse.parseItems("<html>"));
    }

    @Test
    public void readsTheAssertionKeyOfTheServerDocumentShape() {
        Map<String, String> keys = DidDocumentResponse.assertionKeys(response(document(DID,
                "\"assertionMethod\":[\"assert\"]",
                "{\"id\":\"assert\",\"type\":\"Secp256r1VerificationKey2018\",\"publicKeyMultibase\":\"" + KEY + "\"},"
                        + "{\"id\":\"auth\",\"type\":\"Secp256r1VerificationKey2018\",\"publicKeyMultibase\":\"zOther\"}")),
                DID);

        assertEquals(Collections.singletonMap("assert", KEY), keys);
    }

    @Test
    public void fullUrlIdsAndEmbeddedAssertionMethodsAreNormalisedToFragments() {
        Map<String, String> keys = DidDocumentResponse.assertionKeys(response(document(DID,
                "\"assertionMethod\":[\"did:omn:issuer?versionId=1#assert\","
                        + "{\"id\":\"did:omn:issuer#embedded\",\"type\":\"Secp256r1VerificationKey2018\","
                        + "\"publicKeyMultibase\":\"zEmbedded\"}]",
                "{\"id\":\"did:omn:issuer#assert\",\"type\":\"Secp256r1VerificationKey2018\","
                        + "\"publicKeyMultibase\":\"" + KEY + "\"}")), DID);

        assertEquals(Arrays.asList("assert", "embedded"), Arrays.asList(keys.keySet().toArray()));
    }

    @Test
    public void onlyP256KeysAreKeptAndAnotherDidsKeyNeverBecomesAFragment() {
        Map<String, String> keys = DidDocumentResponse.assertionKeys(response(document(DID,
                "\"assertionMethod\":[\"k1\",\"did:omn:other#assert\"]",
                "{\"id\":\"k1\",\"type\":\"EcdsaSecp256k1VerificationKey2019\",\"publicKeyMultibase\":\"zK1\"},"
                        + "{\"id\":\"did:omn:other#assert\",\"type\":\"Secp256r1VerificationKey2018\","
                        + "\"publicKeyMultibase\":\"zForeign\"}")), DID);

        assertEquals(Collections.singletonMap("did:omn:other#assert", "zForeign"), keys);
    }

    @Test
    public void aDeactivatedDocumentContributesNoKeys() {
        Map<String, String> keys = DidDocumentResponse.assertionKeys(response(
                document(DID, "\"deactivated\":true,\"assertionMethod\":[\"assert\"]",
                        "{\"id\":\"assert\",\"type\":\"Secp256r1VerificationKey2018\",\"publicKeyMultibase\":\"" + KEY + "\"}")),
                DID);

        assertTrue(keys.isEmpty());
    }

    @Test
    public void aDocumentForAnotherDidFailsTheRefresh() {
        assertThrows(RuntimeException.class, () -> DidDocumentResponse.assertionKeys(
                response(document("did:omn:other", "\"assertionMethod\":[]", "")), DID));
    }

    @Test
    public void aMissingOrNonMultibaseOrUndecodableDidDocFailsTheRefresh() {
        assertThrows(RuntimeException.class, () -> DidDocumentResponse.assertionKeys("{}", DID));
        assertThrows(RuntimeException.class,
                () -> DidDocumentResponse.assertionKeys("{\"didDoc\":\"uAAAA\"}", DID));
        assertThrows(RuntimeException.class,
                () -> DidDocumentResponse.assertionKeys("{\"didDoc\":\"m!!!\"}", DID));
    }

    private static String document(String id, String extra, String methods) {
        return "{\"@context\":[\"https://www.w3.org/ns/did/v1\"],\"id\":\"" + id + "\",\"versionId\":\"1\","
                + extra + ",\"verificationMethod\":[" + methods + "]}";
    }

    private static String response(String document) {
        return "{\"didDoc\":\"m" + Base64.getEncoder().withoutPadding()
                .encodeToString(document.getBytes(StandardCharsets.UTF_8)) + "\"}";
    }
}
