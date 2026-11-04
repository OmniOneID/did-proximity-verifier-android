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

import android.os.Handler;
import android.os.Looper;

import org.omnione.did.proximity.verifier.data.model.Claim;
import org.omnione.did.proximity.verifier.data.model.ReturnedDocument;
import org.omnione.did.proximity.verifier.data.model.VerificationItem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class VerificationRepository {

    public enum Status {
        CONNECTING, CONNECTED, ERROR
    }

    public interface SessionListener {
        void onStatus(Status status);

        void onDocuments(List<ReturnedDocument> documents);
    }

    private static final long TRANSFER_DELAY_MS = 2200L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private List<ReturnedDocument> documents = Collections.emptyList();
    private Runnable pending;

    @Inject
    public VerificationRepository() {
    }

    public void startSession(SessionListener listener) {
        cancelSession();
        listener.onStatus(Status.CONNECTED);
        pending = () -> {
            pending = null;
            documents = sampleDocuments(true);
            listener.onDocuments(documents);
        };
        handler.postDelayed(pending, TRANSFER_DELAY_MS);
    }

    public void cancelSession() {
        if (pending != null) {
            handler.removeCallbacks(pending);
            pending = null;
        }
    }

    public List<ReturnedDocument> getDocuments() {
        return documents;
    }

    public void releaseDocuments() {
        documents = Collections.emptyList();
    }

    private static List<VerificationItem> validity(boolean verified) {
        return Arrays.asList(
                new VerificationItem("Is data integrity intact?", "true", false),
                new VerificationItem("Is device signature valid?", "true", false),
                new VerificationItem("Is issuer signature valid?",
                        verified ? "true" : "false", !verified),
                new VerificationItem("Signed", "04 Aug 2026", false),
                new VerificationItem("Valid from", "04 Aug 2026", false),
                new VerificationItem("Valid until", "02 Nov 2026", false));
    }

    private static List<ReturnedDocument> sampleDocuments(boolean verified) {
        return Collections.singletonList(new ReturnedDocument("org.omnione.pid.1",
                validity(verified), Arrays.asList(
                new Claim("Family Name(s)", "W"),
                new Claim("Given Name(s)", "C"),
                new Claim("Birth Date", "2026-08-04"),
                new Claim("Portrait", "[jpeg]"))));
    }
}
