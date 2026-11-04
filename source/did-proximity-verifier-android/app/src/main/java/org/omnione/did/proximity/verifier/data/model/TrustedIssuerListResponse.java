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
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TrustedIssuerListResponse {

    private static final Gson GSON = new Gson();

    @SerializedName("updatedAt") @Nullable public String updatedAt;
    @SerializedName("count") @Nullable public Integer count;
    @SerializedName("items") @Nullable public List<Item> items;

    public static final class Item {

        @SerializedName("did") @Nullable public String did;
        @SerializedName("name") @Nullable public String name;
        @SerializedName("docTypes") @Nullable public List<String> docTypes;

        @NonNull
        public String name() {
            return name == null ? "" : name;
        }

        @NonNull
        public List<String> docTypes() {
            if (docTypes == null) return Collections.emptyList();
            List<String> values = new ArrayList<>();
            for (String docType : docTypes) {
                if (docType != null) values.add(docType);
            }
            return values;
        }
    }

    @NonNull
    public static List<Item> parseItems(@NonNull String json) {
        TrustedIssuerListResponse response = GSON.fromJson(json, TrustedIssuerListResponse.class);
        if (response == null || response.items == null) {
            throw new JsonParseException("trusted issuer list has no items");
        }
        for (Item item : response.items) {
            if (item == null || item.did == null || item.did.isEmpty()) {
                throw new JsonParseException("trusted issuer item has no did");
            }
        }
        return response.items;
    }
}
