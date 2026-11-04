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

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.omnione.did.proximity.sdk.core.ProximityException;
import org.omnione.did.proximity.sdk.core.engagement.TransportMode;
import org.omnione.did.proximity.sdk.core.engagement.TransportPreference;
import org.omnione.did.proximity.sdk.core.logger.ProximityLogger;
import org.omnione.did.proximity.sdk.core.message.DeviceAuthResult;
import org.omnione.did.proximity.sdk.core.message.Document;
import org.omnione.did.proximity.sdk.core.message.ItemsRequest;
import org.omnione.did.proximity.sdk.core.nfc.NfcHandoverResult;
import org.omnione.did.proximity.sdk.core.nfc.NfcHandoverResults;
import org.omnione.did.proximity.sdk.reader.ProximityResponse;
import org.omnione.did.proximity.sdk.reader.ReaderOptions;
import org.omnione.did.proximity.sdk.reader.ReaderSession;
import org.omnione.did.proximity.verifier.data.model.DocCatalog;
import org.omnione.did.proximity.verifier.data.model.DocGroup;
import org.omnione.did.proximity.verifier.data.model.IssuerVerdict;
import org.omnione.did.proximity.verifier.data.model.NoUsableTransportException;
import org.omnione.did.proximity.verifier.data.model.ReceivedDocument;
import org.omnione.did.proximity.verifier.data.model.RequestRow;
import org.omnione.did.proximity.verifier.data.model.TrustedIssuer;
import org.omnione.did.proximity.verifier.data.repository.NfcHandoverRepository;
import org.omnione.did.proximity.verifier.data.repository.ReceivedDocumentRepository;
import org.omnione.did.proximity.verifier.data.repository.RequestRepository;
import org.omnione.did.proximity.verifier.data.repository.SettingsRepository;
import org.omnione.did.proximity.verifier.data.repository.TrustedIssuerRepository;
import org.omnione.did.proximity.verifier.util.BluetoothReadiness;
import org.omnione.did.proximity.verifier.util.IssuerTrust;
import org.omnione.did.proximity.verifier.util.WifiAwareReadiness;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TransferStatusViewModel extends AndroidViewModel {

    public static final String RESULT_TAG = "TransferResult";

    private static final String DEFAULT_GROUP = DocCatalog.ID_PID;

    private final RequestRepository requestRepository;
    private final SettingsRepository settingsRepository;
    private final ReceivedDocumentRepository receivedDocumentRepository;
    private final NfcHandoverRepository nfcHandoverRepository;
    private final TrustedIssuerRepository trustedIssuerRepository;

    private final MutableLiveData<TransferStatusUiState> uiState = new MutableLiveData<>();

    private List<RequestRow> rows = Collections.emptyList();
    private boolean started;
    private volatile boolean received;
    private String requestedDocType = "-";
    private List<String> requestedElements = Collections.emptyList();
    private final AtomicBoolean outcomeLogged = new AtomicBoolean();

    private ReaderSession session;
    @Nullable private String qrEngagement;
    private ProximityResponse lastResponse;

    @Inject
    public TransferStatusViewModel(Application application, RequestRepository requestRepository,
                                   SettingsRepository settingsRepository,
                                   ReceivedDocumentRepository receivedDocumentRepository,
                                   NfcHandoverRepository nfcHandoverRepository,
                                   TrustedIssuerRepository trustedIssuerRepository) {
        super(application);
        this.requestRepository = requestRepository;
        this.settingsRepository = settingsRepository;
        this.receivedDocumentRepository = receivedDocumentRepository;
        this.nfcHandoverRepository = nfcHandoverRepository;
        this.trustedIssuerRepository = trustedIssuerRepository;
    }

    public LiveData<TransferStatusUiState> getState() {
        return uiState;
    }

    public void start(@NonNull Context context, @NonNull String engagement) {
        ItemsRequest request = begin(context);
        if (request == null) return;
        qrEngagement = engagement;
        session.start(engagement, request, listener());
    }

    public void startFromNfc(@NonNull Context context) {
        NfcHandoverResult handover = nfcHandoverRepository.take();
        if (handover == null) {
            reportUnavailable(org.omnione.did.proximity.verifier.R.string.transfer_error_message);
            return;
        }
        startFromNfc(context, handover);
    }

    public void startFromNfc(@NonNull Context context, @NonNull NfcHandoverResult handover) {
        boolean nfc = handover.selected() == TransportMode.NFC;
        ItemsRequest request = begin(context, nfc);
        if (request == null) {
            NfcHandoverResults.closeQuietly(handover);
            return;
        }
        session.start(handover, request, listener());
    }

    public boolean wantsWifiAware(@NonNull Context context) {
        return settingsRepository.wantsWifiAware() && WifiAwareReadiness.isSupported(context);
    }

    @Nullable
    private ItemsRequest begin(@NonNull Context context) {
        return begin(context, false);
    }

    @Nullable
    private ItemsRequest begin(@NonNull Context context, boolean nfcAllowed) {
        if (started) return null;
        started = true;

        rows = requestRepository.buildRows(getApplication());
        uiState.setValue(TransferStatusUiState.connecting(rows));

        ItemsRequest request = request(requestRepository);
        requestedDocType = request.docType();
        requestedElements = new ArrayList<>();
        for (Map<String, Boolean> elements : request.requestedElements().values()) {
            requestedElements.addAll(elements.keySet());
        }
        ProximityLogger.getInstance().d("requesting docType=" + request.docType()
                + " elements=" + request.requestedElements());

        TransportPreference preference;
        try {
            preference = settingsRepository.getTransportPreference(context, nfcAllowed);
        } catch (NoUsableTransportException e) {
            ProximityLogger.getInstance().d("no usable transport: " + e.getMessage());
            TransferStatusUiState unusable = TransferStatusUiState.unavailable(rows, e.messageRes);
            logOutcome(unusable);
            uiState.setValue(unusable);
            return null;
        }
        boolean l2cap = settingsRepository.isUseL2cap();
        ProximityLogger.getInstance().d("reader session l2cap=" + l2cap
                + " sdk=" + Build.VERSION.SDK_INT);
        session = ReaderSession.create(context, preference,
                ReaderOptions.defaults().withL2cap(l2cap));
        return request;
    }

    @NonNull
    private ReaderSession.Listener listener() {
        return new ReaderSession.Listener() {
            @Override
            public void onMdocConnected() {
                if (received) return;
                uiState.postValue(TransferStatusUiState.connected(rows));
            }

            @Override
            public void onResponse(@NonNull ProximityResponse response) {
                if (received) return;
                received = true;
                lastResponse = response;
                TransferStatusUiState state = TransferStatusUiState.received(rows);
                logOutcome(state);
                uiState.postValue(state);
            }

            @Override
            public void onError(@NonNull ProximityException error) {
                if (received) return;
                TransferStatusUiState state = qrEngagement == null
                        ? TransferStatusUiState.fromError(rows, error)
                        : TransferStatusUiState.fromQrError(rows, error, qrEngagement,
                                settingsRepository.wantsBle()
                                        && !BluetoothReadiness.isEnabled(getApplication()),
                                settingsRepository.wantsNfc());
                logOutcome(state);
                uiState.postValue(state);
            }

            @Override
            public void onTerminated() {
                if (received) return;
                TransferStatusUiState state = TransferStatusUiState.terminated(rows);
                logOutcome(state);
                uiState.postValue(state);
            }
        };
    }

    public void reportUnavailable(@StringRes int messageRes) {
        if (started) return;
        started = true;
        rows = requestRepository.buildRows(getApplication());
        TransferStatusUiState state = TransferStatusUiState.unavailable(rows, messageRes);
        logOutcome(state);
        uiState.setValue(state);
    }

    private void logOutcome(@NonNull TransferStatusUiState state) {
        if (!outcomeLogged.compareAndSet(false, true)) return;
        Log.i(RESULT_TAG, "transferResult outcome=" + state.status
                + " docType=" + requestedDocType
                + " docs=" + (lastResponse == null ? 0 : lastResponse.documents.size())
                + " deviceAuth=" + deviceAuthSummary()
                + " reason=" + reasonOf(state));
    }

    @NonNull
    private String deviceAuthSummary() {
        if (lastResponse == null) return "-";
        return lastResponse.deviceAuthVerified() ? "VERIFIED" : "NOT_VERIFIED";
    }

    @NonNull
    private String reasonOf(@NonNull TransferStatusUiState state) {
        if (state.messageRes != 0) {
            String name = getApplication().getResources().getResourceEntryName(state.messageRes);
            return state.errorMessage == null ? name : name + " (" + state.errorMessage + ")";
        }
        if (state.errorMessage == null || state.errorMessage.isEmpty()) return "-";
        return state.errorMessage.replaceAll("\\s+", " ");
    }

    public boolean isSettled() {
        return started;
    }

    public boolean isReceived() {
        return received;
    }

    public void cancel() {
        if (session != null) {
            session.terminate();
        }
    }

    public void publishReceivedDocuments() {
        receivedDocumentRepository.set(receivedDocuments());
    }

    private List<ReceivedDocument> receivedDocuments() {
        List<ReceivedDocument> result = new ArrayList<>();
        if (lastResponse == null) return result;

        List<IssuerVerdict> issuerVerdicts = issuerVerdicts(lastResponse.documents);
        for (int i = 0; i < lastResponse.documents.size(); i++) {
            Document document = lastResponse.documents.get(i);
            LinkedHashMap<String, Serializable> claims = new LinkedHashMap<>();
            for (Map.Entry<String, Object> claim
                    : inRequestOrder(document.flatClaims(), requestedElements).entrySet()) {
                claims.put(claim.getKey(), claimValue(claim.getValue()));
            }
            result.add(new ReceivedDocument(
                    document.docType, claims, document.validFrom, document.validUntil,
                    deviceAuthOf(lastResponse.deviceAuth, i), issuerVerdicts.get(i)));
        }
        return result;
    }

    @NonNull
    private List<IssuerVerdict> issuerVerdicts(@NonNull List<Document> documents) {
        boolean skip = settingsRepository.isSkipIssuerAuth();
        Map<String, TrustedIssuer> trusted = skip ? null : trustedIssuerRepository.byDid();
        List<IssuerVerdict> verdicts = new ArrayList<>();
        for (Document document : documents) {
            IssuerVerdict verdict = skip
                    ? IssuerVerdict.skipped()
                    : IssuerTrust.evaluate(document.issuerAuth, document.docType, trusted);
            Log.i(RESULT_TAG, "issuerTrust docType=" + document.docType
                    + " verdict=" + verdict.outcome
                    + " reason=" + (verdict.reason == null ? "-" : verdict.reason)
                    + " kid=" + (verdict.kid == null ? "-" : verdict.kid)
                    + " name=" + (verdict.name == null ? "-" : verdict.name));
            verdicts.add(verdict);
        }
        return verdicts;
    }

    static Map<String, Object> inRequestOrder(Map<String, Object> claims, List<String> order) {
        Map<String, Object> ordered = new LinkedHashMap<>();
        for (String element : order) {
            if (claims.containsKey(element)) ordered.put(element, claims.get(element));
        }
        ordered.putAll(claims);
        return ordered;
    }

    static DeviceAuthResult.Outcome deviceAuthOf(List<DeviceAuthResult> verdicts, int index) {
        if (index < 0 || index >= verdicts.size()) {
            return DeviceAuthResult.Outcome.INVALID;
        }
        return verdicts.get(index).outcome;
    }

    private static Serializable claimValue(Object value) {
        if (value instanceof Map) {
            LinkedHashMap<String, Serializable> nested = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                nested.put(String.valueOf(entry.getKey()), claimValue(entry.getValue()));
            }
            return nested;
        }
        if (value instanceof List) {
            ArrayList<Serializable> nested = new ArrayList<>();
            for (Object element : (List<?>) value) {
                nested.add(claimValue(element));
            }
            return nested;
        }
        if (value instanceof byte[]) {
            return ((byte[]) value).clone();
        }
        return String.valueOf(value);
    }

    @NonNull
    private static ItemsRequest request(@NonNull RequestRepository repository) {
        DocGroup group = repository.selectedGroup();
        List<String> attrs = repository.selectedAttrs();
        if (group == null || attrs == null || attrs.isEmpty()) {
            group = Objects.requireNonNull(DocCatalog.byId(DEFAULT_GROUP));
            attrs = group.attrs;
        }
        Map<String, Boolean> elements = new LinkedHashMap<>();
        for (String attr : attrs) {
            String element = group.elementOf(attr);
            if (element != null) elements.put(element, false);
        }
        Map<String, Map<String, Boolean>> nameSpaces = new LinkedHashMap<>();
        nameSpaces.put(group.nameSpace, elements);
        return ItemsRequest.of(group.docType, nameSpaces);
    }

    @Override
    protected void onCleared() {
        if (session != null) {
            session.closeWithoutNotifying();
        }
        super.onCleared();
    }
}
