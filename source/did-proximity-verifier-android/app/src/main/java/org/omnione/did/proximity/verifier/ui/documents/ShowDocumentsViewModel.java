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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.omnione.did.proximity.verifier.data.model.Claim;
import org.omnione.did.proximity.sdk.core.message.DeviceAuthResult;
import org.omnione.did.proximity.verifier.data.model.IssuerVerdict;
import org.omnione.did.proximity.verifier.data.model.ReceivedDocument;
import org.omnione.did.proximity.verifier.data.model.ReturnedDocument;
import org.omnione.did.proximity.verifier.data.model.VerificationItem;
import org.omnione.did.proximity.verifier.data.repository.ReceivedDocumentRepository;

import android.util.Base64;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ShowDocumentsViewModel extends ViewModel {

    private final MutableLiveData<List<ReturnedDocument>> documents = new MutableLiveData<>();
    private final ReceivedDocumentRepository receivedDocumentRepository;

    @Inject
    public ShowDocumentsViewModel(ReceivedDocumentRepository receivedDocumentRepository) {
        this.receivedDocumentRepository = receivedDocumentRepository;
    }

    public LiveData<List<ReturnedDocument>> getDocuments() {
        return documents;
    }

    public void init() {
        List<ReturnedDocument> result = new ArrayList<>();
        for (ReceivedDocument document : receivedDocumentRepository.get()) {
            result.add(new ReturnedDocument(
                    document.docType, verification(document), claimsOf(document.claims)));
        }
        documents.setValue(result);
    }

    public void release() {
        receivedDocumentRepository.clear();
    }

    private static final int MAX_GROUP_DEPTH = 2;
    private static final List<String> IMAGE_ELEMENTS =
            Arrays.asList("portrait", "picture", "signature_usual_mark");
    private static final String BASE64_PREFIX_END = ";base64,";
    private static final String NO_VALUE = "-";
    private static final DateTimeFormatter VALIDITY_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private static List<Claim> claimsOf(Map<String, ? extends Serializable> source) {
        List<Claim> claims = new ArrayList<>();
        for (Map.Entry<String, ? extends Serializable> entry : source.entrySet()) {
            claims.add(claimOf(entry.getKey(), entry.getValue(), 0));
        }
        return claims;
    }

    private static Claim claimOf(String label, Object value, int depth) {
        if (isImageElement(label)) {
            byte[] image = imageBytes(value);
            if (image != null && image.length > 0) {
                return new Claim(label, image, value instanceof String ? (String) value : null);
            }
        }
        if (value instanceof byte[]) {
            return new Claim(label, byteCount(((byte[]) value).length));
        }
        if (value instanceof Map) {
            if (depth >= MAX_GROUP_DEPTH) return new Claim(label, String.valueOf(value));
            List<Claim> children = new ArrayList<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                children.add(claimOf(String.valueOf(entry.getKey()), entry.getValue(), depth + 1));
            }
            return new Claim(label, children);
        }
        if (value instanceof List) {
            if (depth >= MAX_GROUP_DEPTH) return new Claim(label, String.valueOf(value));
            return listClaim(label, (List<?>) value, depth);
        }
        return new Claim(label, String.valueOf(value));
    }

    private static Claim listClaim(String label, List<?> elements, int depth) {
        if (elements.size() == 1) {
            return claimOf(label, elements.get(0), depth);
        }
        List<Claim> children = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            children.add(claimOf(String.valueOf(i + 1), elements.get(i), depth + 1));
        }
        return new Claim(label, children);
    }

    private static boolean isImageElement(String element) {
        int dot = element.lastIndexOf('.');
        String tail = dot < 0 ? element : element.substring(dot + 1);
        return IMAGE_ELEMENTS.contains(tail);
    }

    @Nullable
    private static byte[] imageBytes(Object value) {
        if (value instanceof byte[]) return (byte[]) value;
        if (!(value instanceof String)) return null;

        String encoded = (String) value;
        int prefix = encoded.indexOf(BASE64_PREFIX_END);
        if (prefix >= 0) encoded = encoded.substring(prefix + BASE64_PREFIX_END.length());
        try {
            return Base64.decode(encoded, Base64.DEFAULT);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String byteCount(int length) {
        return "(" + length + " bytes)";
    }

    static List<VerificationItem> verification(ReceivedDocument document) {
        return Arrays.asList(
                new VerificationItem("Issuer Trusted", issuerTrustedValue(document.issuerTrust),
                        false, issuerNoReason(document.issuerTrust)),
                new VerificationItem("Device Auth", deviceAuthValue(document.deviceAuth), false),
                new VerificationItem("Valid from", validityDate(document.validFrom), false),
                new VerificationItem("Valid until", validityDate(document.validUntil), false));
    }

    static String issuerTrustedValue(IssuerVerdict verdict) {
        switch (verdict.outcome) {
            case YES:
                return verdict.name == null || verdict.name.isEmpty()
                        ? "Yes" : "Yes (" + verdict.name + ")";
            case NO:
                return "No";
            default:
                return "Skipped";
        }
    }

    @Nullable
    static String issuerNoReason(IssuerVerdict verdict) {
        if (verdict.outcome != IssuerVerdict.Outcome.NO || verdict.reason == null) return null;
        switch (verdict.reason) {
            case unidentified:
                return "The issuer could not be identified from this credential.";
            case notListed:
                return "The issuer is not in the trusted issuer list.";
            case docType:
                return "The issuer is not trusted to issue this document type.";
            default:
                return "The issuer's signature could not be verified.";
        }
    }

    static String deviceAuthValue(DeviceAuthResult.Outcome outcome) {
        return outcome == DeviceAuthResult.Outcome.VERIFIED ? "Yes" : "No";
    }

    private static String validityDate(@Nullable String tdate) {
        if (tdate == null) return NO_VALUE;
        try {
            return OffsetDateTime.parse(tdate).format(VALIDITY_DATE);
        } catch (DateTimeParseException e) {
            return tdate;
        }
    }
}
