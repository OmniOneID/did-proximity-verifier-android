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
import static org.junit.Assert.assertNull;

import com.upokecenter.cbor.CBORObject;

import org.junit.Test;
import org.omnione.did.proximity.sdk.core.message.DeviceAuthResult;
import org.omnione.did.proximity.verifier.data.model.IssuerVerdict;
import org.omnione.did.proximity.verifier.data.model.ReceivedDocument;
import org.omnione.did.proximity.verifier.data.model.TrustedIssuer;
import org.omnione.did.proximity.verifier.data.model.VerificationItem;
import org.omnione.did.proximity.verifier.util.IssuerTrust;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IssuerTrustedRowTest {

    private static final String DID = "did:omn:issuer";
    private static final String KID = DID + "?versionId=1#assert";
    private static final String PID = "org.omnione.pid.1";
    private static final String MDL = "org.iso.18013.5.1.mDL";
    private static final String BASE58 =
            "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    private static final KeyPair ISSUER = generate();
    private static final KeyPair OTHER = generate();

    @Test
    public void aListedIssuerWithAValidSignatureIsYesWithItsName() {
        VerificationItem row = row(IssuerTrust.evaluate(
                signed(ISSUER, KID), PID, cache("OpenDID Issuer", PID, ISSUER)));

        assertEquals("Issuer Trusted", row.label);
        assertEquals("Yes (OpenDID Issuer)", row.value);
        assertNull(row.note);
        assertFalse(row.danger);
    }

    @Test
    public void aListedIssuerWithoutANameIsPlainYes() {
        VerificationItem row = row(IssuerTrust.evaluate(
                signed(ISSUER, KID), PID, cache("", PID, ISSUER)));

        assertEquals("Yes", row.value);
        assertNull(row.note);
    }

    @Test
    public void skippingIssuerVerificationIsSkippedWithoutAReason() {
        VerificationItem row = row(IssuerVerdict.skipped());

        assertEquals("Skipped", row.value);
        assertNull(row.note);
    }

    @Test
    public void anIssuerAuthWithoutAKidIsUnidentified() {
        assertNo("The issuer could not be identified from this credential.",
                IssuerTrust.evaluate(signed(ISSUER, null), PID, cache("n", PID, ISSUER)));
    }

    @Test
    public void aKidThatIsNotADidIsUnidentified() {
        assertNo("The issuer could not be identified from this credential.",
                IssuerTrust.evaluate(signed(ISSUER, "#assert"), PID, cache("n", PID, ISSUER)));
    }

    @Test
    public void anEmptyCacheIsNotListed() {
        assertNo("The issuer is not in the trusted issuer list.",
                IssuerTrust.evaluate(signed(ISSUER, KID), PID,
                        Collections.<String, TrustedIssuer>emptyMap()));
    }

    @Test
    public void anotherIssuerInTheCacheIsNotListed() {
        assertNo("The issuer is not in the trusted issuer list.",
                IssuerTrust.evaluate(signed(ISSUER, "did:omn:stranger#assert"), PID,
                        cache("n", PID, ISSUER)));
    }

    @Test
    public void aDocTypeTheIssuerIsNotTrustedForIsDocType() {
        assertNo("The issuer is not trusted to issue this document type.",
                IssuerTrust.evaluate(signed(ISSUER, KID, MDL), MDL, cache("n", PID, ISSUER)));
    }

    @Test
    public void theListIsCheckedAgainstTheSignedMsoDocTypeNotTheOuterOne() {
        assertNo("The issuer is not trusted to issue this document type.",
                IssuerTrust.evaluate(signed(ISSUER, KID, MDL), PID, cache("n", PID, ISSUER)));
    }

    @Test
    public void aSignatureByAnotherKeyIsSignature() {
        assertNo("The issuer's signature could not be verified.",
                IssuerTrust.evaluate(signed(OTHER, KID), PID, cache("n", PID, ISSUER)));
    }

    @Test
    public void aKidWithoutAMatchingKeyIsSignature() {
        assertNo("The issuer's signature could not be verified.",
                IssuerTrust.evaluate(signed(ISSUER, DID + "#auth"), PID,
                        cache("n", PID, ISSUER)));
    }

    @Test
    public void anMsoSignedForAnotherDocTypeIsSignature() {
        Map<String, TrustedIssuer> both = cache("n", PID, ISSUER);
        both.put(DID, new TrustedIssuer(DID, "n", java.util.Arrays.asList(PID, MDL),
                both.get(DID).assertionKeys));

        assertNo("The issuer's signature could not be verified.",
                IssuerTrust.evaluate(signed(ISSUER, KID, MDL), PID, both));
    }

    @Test
    public void anMsoWithoutADocTypeFallsBackToTheReceivedDocTypeForTheList() {
        assertNo("The issuer is not trusted to issue this document type.",
                IssuerTrust.evaluate(signed(ISSUER, KID, null), MDL, cache("n", PID, ISSUER)));
    }

    @Test
    public void anMsoWithoutADocTypeIsSignature() {
        assertNo("The issuer's signature could not be verified.",
                IssuerTrust.evaluate(signed(ISSUER, KID, null), PID, cache("n", PID, ISSUER)));
    }

    @Test
    public void aFlippedSignatureByteIsSignature() {
        CBORObject sign1 = CBORObject.DecodeFromBytes(signed(ISSUER, KID));
        byte[] signature = sign1.get(3).GetByteString();
        signature[0] ^= 0x01;
        sign1.set(3, CBORObject.FromObject(signature));

        assertNo("The issuer's signature could not be verified.",
                IssuerTrust.evaluate(sign1.EncodeToBytes(), PID, cache("n", PID, ISSUER)));
    }

    private static void assertNo(String reason, IssuerVerdict verdict) {
        VerificationItem row = row(verdict);
        assertEquals("No", row.value);
        assertEquals(reason, row.note);
        assertFalse(row.danger);
    }

    private static VerificationItem row(IssuerVerdict verdict) {
        List<VerificationItem> rows = ShowDocumentsViewModel.verification(new ReceivedDocument(
                PID, new LinkedHashMap<String, Serializable>(), null, null,
                DeviceAuthResult.Outcome.VERIFIED, verdict));
        assertNull(rows.get(1).note);
        return rows.get(0);
    }

    private static Map<String, TrustedIssuer> cache(String name, String docType, KeyPair key) {
        Map<String, String> keys = new HashMap<>();
        keys.put("assert", "z" + base58(compressed((ECPublicKey) key.getPublic())));
        Map<String, TrustedIssuer> cache = new HashMap<>();
        cache.put(DID, new TrustedIssuer(DID, name, Collections.singletonList(docType), keys));
        return cache;
    }

    private static byte[] signed(KeyPair key, String kid) {
        return signed(key, kid, PID);
    }

    private static byte[] signed(KeyPair key, String kid, String msoDocType) {
        CBORObject header = CBORObject.NewMap();
        header.Add(1, -7);
        if (kid != null) header.Add(4, kid);
        byte[] protectedHeader = header.EncodeToBytes();
        CBORObject mso = CBORObject.NewMap();
        if (msoDocType != null) mso.Add("docType", msoDocType);
        byte[] payload = CBORObject.FromObjectAndTag(mso.EncodeToBytes(), 24).EncodeToBytes();

        CBORObject structure = CBORObject.NewArray();
        structure.Add("Signature1");
        structure.Add(protectedHeader);
        structure.Add(new byte[0]);
        structure.Add(payload);

        CBORObject sign1 = CBORObject.NewArray();
        sign1.Add(protectedHeader);
        sign1.Add(CBORObject.NewMap());
        sign1.Add(payload);
        try {
            Signature signer = Signature.getInstance("SHA256withECDSAinP1363Format");
            signer.initSign(key.getPrivate());
            signer.update(structure.EncodeToBytes());
            sign1.Add(signer.sign());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return sign1.EncodeToBytes();
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] compressed(ECPublicKey key) {
        byte[] x = key.getW().getAffineX().toByteArray();
        byte[] point = new byte[33];
        point[0] = (byte) (key.getW().getAffineY().testBit(0) ? 0x03 : 0x02);
        int length = Math.min(x.length, 32);
        System.arraycopy(x, x.length - length, point, 33 - length, length);
        return point;
    }

    private static String base58(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        BigInteger value = new BigInteger(1, bytes);
        BigInteger radix = BigInteger.valueOf(58);
        while (value.signum() > 0) {
            BigInteger[] division = value.divideAndRemainder(radix);
            out.append(BASE58.charAt(division[1].intValue()));
            value = division[0];
        }
        for (int i = 0; i < bytes.length && bytes[i] == 0; i++) out.append('1');
        return out.reverse().toString();
    }
}
