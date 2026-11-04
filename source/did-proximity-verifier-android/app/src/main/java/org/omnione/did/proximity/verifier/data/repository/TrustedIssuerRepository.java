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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.omnione.did.proximity.verifier.data.model.DidDocumentResponse;
import org.omnione.did.proximity.verifier.data.model.TrustedIssuer;
import org.omnione.did.proximity.verifier.data.model.TrustedIssuerListResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class TrustedIssuerRepository {

    public interface RefreshCallback {
        void onComplete(boolean success);
    }

    private static final String TAG = "TrustedIssuers";
    private static final String PREFS = "verifier_trusted_issuers";
    private static final String KEY_LEGACY_ISSUERS = "issuers";
    private static final String KEY_TRUSTED_ISSUERS = "trustedIssuers";
    private static final String KEY_UPDATED_AT = "updatedAt";
    private static final String LIST_PATH = "/list/api/v1/mdoc/issuers";
    private static final String DID_DOC_PATH = "/api-gateway/api/v1/did-doc?did=";
    private static final int TIMEOUT_MS = 10_000;
    private static final int MAX_BODY_BYTES = 1024 * 1024;

    private final SharedPreferences prefs;
    private final ServerUrlRepository serverUrls;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Set<RefreshCallback> listeners = new LinkedHashSet<>();
    private boolean refreshing;

    @Inject
    public TrustedIssuerRepository(@ApplicationContext Context context,
                                   ServerUrlRepository serverUrls) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.serverUrls = serverUrls;
    }

    @NonNull
    public List<TrustedIssuer> getIssuers() {
        String raw = prefs.getString(KEY_TRUSTED_ISSUERS, null);
        if (raw == null) return Collections.emptyList();
        try {
            return fromJson(new JSONArray(raw));
        } catch (JSONException e) {
            return Collections.emptyList();
        }
    }

    @NonNull
    public Map<String, TrustedIssuer> byDid() {
        Map<String, TrustedIssuer> trusted = new LinkedHashMap<>();
        for (TrustedIssuer issuer : getIssuers()) trusted.put(issuer.did, issuer);
        return trusted;
    }

    @Nullable
    public String getUpdatedAt() {
        return prefs.getString(KEY_UPDATED_AT, null);
    }

    public boolean isRefreshing() {
        return refreshing;
    }

    public void addRefreshListener(RefreshCallback listener) {
        listeners.add(listener);
    }

    public void removeRefreshListener(RefreshCallback listener) {
        listeners.remove(listener);
    }

    public void refresh() {
        if (refreshing) return;
        refreshing = true;
        String tasUrl = serverUrls.url(ServerUrlRepository.KEY_TAS_URL);
        String apiGwUrl = serverUrls.url(ServerUrlRepository.KEY_API_GW_URL);
        executor.execute(() -> {
            List<TrustedIssuer> fetched = null;
            try {
                fetched = fetch(tasUrl, apiGwUrl);
            } catch (Exception e) {
                Log.w(TAG, "refresh failed, keeping the cache: " + e);
            }
            List<TrustedIssuer> result = fetched;
            handler.post(() -> {
                if (result != null) store(result);
                refreshing = false;
                for (RefreshCallback listener : new ArrayList<>(listeners)) {
                    listener.onComplete(result != null);
                }
            });
        });
    }

    @NonNull
    private static List<TrustedIssuer> fetch(@NonNull String tasUrl, @NonNull String apiGwUrl)
            throws IOException {
        List<TrustedIssuer> issuers = new ArrayList<>();
        for (TrustedIssuerListResponse.Item item
                : TrustedIssuerListResponse.parseItems(get(tasUrl + LIST_PATH))) {
            issuers.add(new TrustedIssuer(item.did, item.name(), item.docTypes(),
                    assertionKeys(apiGwUrl, item.did)));
        }
        return issuers;
    }

    @NonNull
    private static Map<String, String> assertionKeys(@NonNull String apiGwUrl, @NonNull String did)
            throws IOException {
        return DidDocumentResponse.assertionKeys(get(apiGwUrl + DID_DOC_PATH
                + URLEncoder.encode(did, StandardCharsets.UTF_8.name())), did);
    }

    @NonNull
    private static String get(@NonNull String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + code + " from " + url);
            }
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                byte[] chunk = new byte[4096];
                int read;
                while ((read = in.read(chunk)) != -1) {
                    body.write(chunk, 0, read);
                    if (body.size() > MAX_BODY_BYTES) throw new IOException("response too large");
                }
                return body.toString(StandardCharsets.UTF_8.name());
            }
        } finally {
            connection.disconnect();
        }
    }

    public void clear() {
        store(Collections.emptyList());
    }

    private void store(@NonNull List<TrustedIssuer> issuers) {
        SharedPreferences.Editor editor = prefs.edit().remove(KEY_LEGACY_ISSUERS);
        if (issuers.isEmpty()) {
            editor.remove(KEY_TRUSTED_ISSUERS);
            editor.remove(KEY_UPDATED_AT);
        } else {
            editor.putString(KEY_TRUSTED_ISSUERS, toJson(issuers).toString());
            editor.putString(KEY_UPDATED_AT, formatStamp(new Date()));
        }
        editor.apply();
    }

    @NonNull
    private static JSONArray toJson(@NonNull List<TrustedIssuer> issuers) {
        JSONArray array = new JSONArray();
        try {
            for (TrustedIssuer issuer : issuers) {
                array.put(new JSONObject()
                        .put("did", issuer.did)
                        .put("name", issuer.name)
                        .put("docTypes", new JSONArray(issuer.docTypes))
                        .put("assertionKeys", new JSONObject(issuer.assertionKeys)));
            }
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        return array;
    }

    @NonNull
    private static List<TrustedIssuer> fromJson(@NonNull JSONArray array) throws JSONException {
        List<TrustedIssuer> issuers = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject issuer = array.getJSONObject(i);
            JSONObject keys = issuer.getJSONObject("assertionKeys");
            Map<String, String> assertionKeys = new LinkedHashMap<>();
            for (Iterator<String> it = keys.keys(); it.hasNext(); ) {
                String fragment = it.next();
                assertionKeys.put(fragment, keys.getString(fragment));
            }
            issuers.add(new TrustedIssuer(issuer.getString("did"), issuer.getString("name"),
                    strings(issuer.getJSONArray("docTypes")), assertionKeys));
        }
        return issuers;
    }

    @NonNull
    private static List<String> strings(@NonNull JSONArray array) throws JSONException {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) values.add(array.getString(i));
        return values;
    }

    private static String formatStamp(Date date) {
        return new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.ENGLISH).format(date);
    }
}
