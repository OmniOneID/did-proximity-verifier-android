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

package org.omnione.did.proximity.verifier.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.omnione.did.proximity.verifier.BuildConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class ServerUrlRepository {

    public static final String KEY_TAS_URL = "TAS_URL";
    public static final String KEY_API_GW_URL = "API_GW_URL";
    public static final List<String> KEYS = List.of(KEY_TAS_URL, KEY_API_GW_URL);

    private static final String PREFS = "verifier_server_urls";

    private final SharedPreferences prefs;

    @Inject
    public ServerUrlRepository(@ApplicationContext Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @NonNull
    public static String defaultUrl(@NonNull String key) {
        switch (key) {
            case KEY_TAS_URL: return BuildConfig.TAS_URL;
            case KEY_API_GW_URL: return BuildConfig.API_GW_URL;
            default: throw new IllegalArgumentException("unknown server url key: " + key);
        }
    }

    @NonNull
    public String url(@NonNull String key) {
        return prefs.getString(key, defaultUrl(key));
    }

    @NonNull
    public Map<String, String> custom() {
        Map<String, String> out = new LinkedHashMap<>();
        for (String key : KEYS) {
            String value = prefs.getString(key, null);
            if (value != null) out.put(key, value);
        }
        return out;
    }

    public boolean save(@NonNull Map<String, String> custom) {
        SharedPreferences.Editor editor = prefs.edit().clear();
        for (Map.Entry<String, String> entry : custom.entrySet()) {
            editor.putString(entry.getKey(), entry.getValue());
        }
        return editor.commit();
    }

    public static boolean changed(@NonNull Map<String, String> before,
                                  @NonNull Map<String, String> after) {
        for (String key : KEYS) {
            String fallback = defaultUrl(key);
            if (!before.getOrDefault(key, fallback).equals(after.getOrDefault(key, fallback))) {
                return true;
            }
        }
        return false;
    }
}
