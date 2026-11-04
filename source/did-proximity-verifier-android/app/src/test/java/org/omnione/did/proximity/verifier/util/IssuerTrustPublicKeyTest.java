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

package org.omnione.did.proximity.verifier.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

public class IssuerTrustPublicKeyTest {

    private static final String BASE58 =
            "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    @Test
    public void malformedMultibaseIsNull() {
        byte[] badPrefix = new byte[33];
        badPrefix[0] = 0x05;
        badPrefix[1] = 0x01;
        byte[] offField = new byte[33];
        Arrays.fill(offField, (byte) 0xFF);
        offField[0] = 0x02;
        byte[] offCurve = new byte[65];
        offCurve[0] = 0x04;
        offCurve[64] = 0x01;
        for (String input : new String[] {null, "abc", "z", "z0", "z1111",
                "z" + base58(badPrefix), "z" + base58(offField), "z" + base58(offCurve)}) {
            assertNull(input, IssuerTrust.publicKey(input));
        }
    }

    @Test
    public void p256KeyRoundTrips() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        ECPublicKey key = (ECPublicKey) generator.generateKeyPair().getPublic();
        byte[] x = key.getW().getAffineX().toByteArray();
        byte[] point = new byte[33];
        point[0] = (byte) (key.getW().getAffineY().testBit(0) ? 0x03 : 0x02);
        int length = Math.min(x.length, 32);
        System.arraycopy(x, x.length - length, point, 33 - length, length);
        byte[] y = key.getW().getAffineY().toByteArray();
        byte[] uncompressed = Arrays.copyOf(point, 65);
        uncompressed[0] = 0x04;
        int yLength = Math.min(y.length, 32);
        System.arraycopy(y, y.length - yLength, uncompressed, 65 - yLength, yLength);

        assertEquals(key.getW(), IssuerTrust.publicKey("z" + base58(point)).getW());
        assertEquals(key.getW(), IssuerTrust.publicKey("z" + base58(uncompressed)).getW());
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
