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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.omnione.did.proximity.sdk.core.crypto.Crypto;
import org.omnione.did.proximity.sdk.core.message.IssuerAuthVerifier;
import org.omnione.did.proximity.verifier.data.model.IssuerVerdict;
import org.omnione.did.proximity.verifier.data.model.TrustedIssuer;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

public final class IssuerTrust {

    private static final Pattern DID = Pattern.compile("did:[a-z0-9]+:\\S+");
    private static final String BASE58 =
            "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final int COORDINATE_LENGTH = 32;

    private IssuerTrust() {}

    @NonNull
    public static IssuerVerdict evaluate(@NonNull byte[] issuerAuth, @NonNull String docType,
                                         @NonNull Map<String, TrustedIssuer> trusted) {
        String kid = IssuerAuthVerifier.kid(issuerAuth);
        String did = kid == null ? null : baseDid(kid);
        if (did == null) return IssuerVerdict.no(IssuerVerdict.Reason.unidentified, kid);

        TrustedIssuer issuer = trusted.get(did);
        if (issuer == null) return IssuerVerdict.no(IssuerVerdict.Reason.notListed, kid);
        String msoDocType = IssuerAuthVerifier.msoDocType(issuerAuth);
        if (!issuer.docTypes.contains(msoDocType != null ? msoDocType : docType)) {
            return IssuerVerdict.no(IssuerVerdict.Reason.docType, kid);
        }

        ECPublicKey key = kid.indexOf('#') < 0
                ? null : publicKey(issuer.assertionKeys.get(fragmentOf(kid)));
        if (key == null || !IssuerAuthVerifier.verify(issuerAuth, key)
                || !docType.equals(msoDocType)) {
            return IssuerVerdict.no(IssuerVerdict.Reason.signature, kid);
        }
        return IssuerVerdict.yes(issuer.name, kid);
    }

    @Nullable
    static String baseDid(@NonNull String kid) {
        int end = kid.length();
        int query = kid.indexOf('?');
        int fragment = kid.indexOf('#');
        if (query >= 0) end = query;
        if (fragment >= 0) end = Math.min(end, fragment);
        String did = kid.substring(0, end);
        return DID.matcher(did).matches() ? did : null;
    }

    @NonNull
    public static String fragmentOf(@NonNull String id) {
        return id.substring(id.indexOf('#') + 1);
    }

    @Nullable
    static ECPublicKey publicKey(@Nullable String publicKeyMultibase) {
        if (publicKeyMultibase == null || !publicKeyMultibase.startsWith("z")) return null;
        try {
            return p256(base58(publicKeyMultibase.substring(1)));
        } catch (GeneralSecurityException | IllegalArgumentException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    @NonNull
    private static ECPublicKey p256(@NonNull byte[] point) throws GeneralSecurityException {
        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC", Crypto.provider());
        parameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec spec = parameters.getParameterSpec(ECParameterSpec.class);

        BigInteger p = ((ECFieldFp) spec.getCurve().getField()).getP();
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(point, 1, 1 + COORDINATE_LENGTH));
        BigInteger rhs = x.pow(3).add(spec.getCurve().getA().multiply(x))
                .add(spec.getCurve().getB()).mod(p);
        BigInteger y;
        if (point.length == 1 + 2 * COORDINATE_LENGTH && point[0] == 0x04) {
            y = new BigInteger(1, Arrays.copyOfRange(point, 1 + COORDINATE_LENGTH, point.length));
        } else if (point.length == 1 + COORDINATE_LENGTH && (point[0] == 0x02 || point[0] == 0x03)) {
            y = rhs.modPow(p.add(BigInteger.ONE).shiftRight(2), p);
            if (y.testBit(0) != (point[0] == 0x03)) y = p.subtract(y);
        } else {
            throw new IllegalArgumentException("unsupported point encoding");
        }
        if (x.compareTo(p) >= 0 || y.compareTo(p) >= 0 || !y.multiply(y).mod(p).equals(rhs)) {
            throw new IllegalArgumentException("not a point on P-256");
        }
        return (ECPublicKey) KeyFactory.getInstance("EC", Crypto.provider())
                .generatePublic(new ECPublicKeySpec(new ECPoint(x, y), spec));
    }

    @NonNull
    static byte[] base58(@NonNull String encoded) {
        BigInteger value = BigInteger.ZERO;
        BigInteger radix = BigInteger.valueOf(58);
        int zeros = 0;
        while (zeros < encoded.length() && encoded.charAt(zeros) == '1') zeros++;
        for (int i = 0; i < encoded.length(); i++) {
            int digit = BASE58.indexOf(encoded.charAt(i));
            if (digit < 0) throw new IllegalArgumentException("not base58");
            value = value.multiply(radix).add(BigInteger.valueOf(digit));
        }
        byte[] magnitude = value.signum() == 0 ? new byte[0] : value.toByteArray();
        int sign = magnitude.length > 0 && magnitude[0] == 0 ? 1 : 0;
        byte[] out = new byte[zeros + magnitude.length - sign];
        System.arraycopy(magnitude, sign, out, zeros, magnitude.length - sign);
        return out;
    }
}
