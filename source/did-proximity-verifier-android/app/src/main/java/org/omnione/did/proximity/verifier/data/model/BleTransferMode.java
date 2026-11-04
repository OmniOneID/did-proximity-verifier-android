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
import androidx.annotation.StringRes;

import org.omnione.did.proximity.sdk.core.engagement.ModePreference;
import org.omnione.did.proximity.verifier.R;

public enum BleTransferMode {

    READER_CENTRAL_CLIENT("central", R.string.settings_ble_mode_central,
            ModePreference.PERIPHERAL_SERVER_FIRST),
    READER_PERIPHERAL_SERVER("peripheral", R.string.settings_ble_mode_peripheral,
            ModePreference.CENTRAL_CLIENT_FIRST);

    public static final BleTransferMode DEFAULT = READER_PERIPHERAL_SERVER;

    public final String id;
    @StringRes public final int labelRes;

    private final ModePreference holderPreference;

    BleTransferMode(String id, @StringRes int labelRes, ModePreference holderPreference) {
        this.id = id;
        this.labelRes = labelRes;
        this.holderPreference = holderPreference;
    }

    @NonNull
    public ModePreference holderModePreference() {
        return holderPreference;
    }

    @NonNull
    public static BleTransferMode of(@Nullable String id) {
        for (BleTransferMode mode : values()) {
            if (mode.id.equals(id)) return mode;
        }
        return DEFAULT;
    }
}
