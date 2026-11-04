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

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public final class BluetoothReadiness {

    private BluetoothReadiness() {
    }

    @NonNull
    public static String[] permissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE};
        }
        return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
    }

    public static boolean hasPermissions(@NonNull Context context) {
        for (String permission : permissions()) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEnabled(@NonNull Context context) {
        BluetoothManager manager = context.getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
        return adapter != null && adapter.isEnabled();
    }

    public static boolean hasLocationServices(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return true;
        }
        LocationManager manager = context.getSystemService(LocationManager.class);
        if (manager == null) {
            return true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return manager.isLocationEnabled();
        }
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}
