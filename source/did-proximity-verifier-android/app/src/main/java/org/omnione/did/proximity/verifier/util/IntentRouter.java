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

import android.content.Context;
import android.content.Intent;

import org.omnione.did.proximity.verifier.ui.attributes.CustomAttrsActivity;
import org.omnione.did.proximity.verifier.ui.documents.ShowDocumentsActivity;
import org.omnione.did.proximity.verifier.ui.engagement.DeviceEngagementActivity;
import org.omnione.did.proximity.verifier.ui.engagement.NfcEngagementActivity;
import org.omnione.did.proximity.verifier.ui.home.HomeActivity;
import org.omnione.did.proximity.verifier.ui.issuers.ServerUrlActivity;
import org.omnione.did.proximity.verifier.ui.issuers.TrustedIssuersActivity;
import org.omnione.did.proximity.verifier.ui.settings.SettingsActivity;
import org.omnione.did.proximity.verifier.ui.transfer.TransferStatusActivity;

public final class IntentRouter {

    public static final String EXTRA_GROUP_ID = "groupId";

    private IntentRouter() {
    }

    public static void startHome(Context context) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    public static void startSettings(Context context) {
        context.startActivity(new Intent(context, SettingsActivity.class));
    }

    public static void startTrustedIssuers(Context context) {
        context.startActivity(new Intent(context, TrustedIssuersActivity.class));
    }

    public static void startServerUrl(Context context) {
        context.startActivity(new Intent(context, ServerUrlActivity.class));
    }

    public static void startCustomAttrs(Context context, String groupId) {
        Intent intent = new Intent(context, CustomAttrsActivity.class);
        intent.putExtra(EXTRA_GROUP_ID, groupId);
        context.startActivity(intent);
    }

    public static void startDeviceEngagement(Context context) {
        context.startActivity(new Intent(context, DeviceEngagementActivity.class));
    }

    public static void startNfcEngagement(Context context) {
        context.startActivity(new Intent(context, NfcEngagementActivity.class));
    }

    public static void startTransferStatus(Context context, String engagement) {
        Intent intent = new Intent(context, TransferStatusActivity.class);
        intent.putExtra(TransferStatusActivity.EXTRA_ENGAGEMENT, engagement);
        context.startActivity(intent);
    }

    public static void startTransferStatusFromNfc(Context context) {
        Intent intent = new Intent(context, TransferStatusActivity.class);
        intent.putExtra(TransferStatusActivity.EXTRA_SOURCE, TransferStatusActivity.SOURCE_NFC);
        context.startActivity(intent);
    }

    public static void startShowDocuments(Context context) {
        context.startActivity(new Intent(context, ShowDocumentsActivity.class));
    }

    public static void startShowDocumentsFromNfc(Context context) {
        Intent intent = new Intent(context, ShowDocumentsActivity.class);
        intent.putExtra(ShowDocumentsActivity.EXTRA_SOURCE, ShowDocumentsActivity.SOURCE_NFC);
        context.startActivity(intent);
    }
}
