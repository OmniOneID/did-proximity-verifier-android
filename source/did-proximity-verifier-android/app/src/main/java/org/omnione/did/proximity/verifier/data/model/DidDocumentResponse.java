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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import org.omnione.did.proximity.verifier.util.IssuerTrust;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DidDocumentResponse {

    private static final Gson GSON = new Gson();
    private static final String P256_KEY_TYPE = "Secp256r1VerificationKey2018";

    @SerializedName("didDoc") @Nullable public String didDoc;

    public static final class DidDocument {

        @SerializedName("id") @Nullable public String id;
        @SerializedName("deactivated") @Nullable public Boolean deactivated;
        @SerializedName("verificationMethod") @Nullable public List<VerificationMethod> verificationMethod;
        @SerializedName("assertionMethod") @Nullable public List<JsonElement> assertionMethod;
    }

    public static final class VerificationMethod {

        @SerializedName("id") @Nullable public String id;
        @SerializedName("type") @Nullable public String type;
        @SerializedName("publicKeyMultibase") @Nullable public String publicKeyMultibase;
    }

    @NonNull
    public static Map<String, String> assertionKeys(@NonNull String json, @NonNull String did) {
        DidDocumentResponse response = GSON.fromJson(json, DidDocumentResponse.class);
        if (response == null || response.didDoc == null || !response.didDoc.startsWith("m")) {
            throw new JsonParseException("didDoc is not multibase base64");
        }
        String decoded = new String(Base64.getDecoder().decode(response.didDoc.substring(1)),
                StandardCharsets.UTF_8);
        DidDocument document = GSON.fromJson(decoded, DidDocument.class);
        if (document == null || !did.equals(document.id)) {
            throw new JsonParseException("DID Document id does not match " + did);
        }

        Map<String, String> keys = new LinkedHashMap<>();
        if (document.assertionMethod == null || Boolean.TRUE.equals(document.deactivated)) {
            return keys;
        }
        Map<String, String> methods = new LinkedHashMap<>();
        if (document.verificationMethod != null) {
            for (VerificationMethod method : document.verificationMethod) {
                putP256(methods, did, method);
            }
        }
        for (JsonElement entry : document.assertionMethod) {
            if (entry == null || entry.isJsonNull()) continue;
            if (entry.isJsonObject()) {
                putP256(keys, did, GSON.fromJson(entry, VerificationMethod.class));
            } else if (entry.isJsonPrimitive()) {
                String fragment = ownFragment(did, entry.getAsString());
                String key = methods.get(fragment);
                if (key != null) keys.put(fragment, key);
            }
        }
        return keys;
    }

    private static void putP256(@NonNull Map<String, String> keys, @NonNull String did,
                                @Nullable VerificationMethod method) {
        if (method == null || method.id == null || !P256_KEY_TYPE.equals(method.type)
                || method.publicKeyMultibase == null || method.publicKeyMultibase.isEmpty()) {
            return;
        }
        keys.put(ownFragment(did, method.id), method.publicKeyMultibase);
    }

    @NonNull
    private static String ownFragment(@NonNull String did, @NonNull String id) {
        int hash = id.indexOf('#');
        if (hash < 0) return id;
        String base = id.substring(0, hash);
        int query = base.indexOf('?');
        if (query >= 0) base = base.substring(0, query);
        return base.isEmpty() || base.equals(did) ? IssuerTrust.fragmentOf(id) : id;
    }
}
