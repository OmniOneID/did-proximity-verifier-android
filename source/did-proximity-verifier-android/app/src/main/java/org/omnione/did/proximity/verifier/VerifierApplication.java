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

package org.omnione.did.proximity.verifier;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import org.omnione.did.proximity.sdk.core.logger.ProximityLogger;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class VerifierApplication extends Application {

    public static final String STAMP_TAG = "VerifierBuildStamp";

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            ProximityLogger.getInstance().enable();
        }
        logInstallStamp();
    }

    private void logInstallStamp() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            Log.i(STAMP_TAG, "package=" + getPackageName()
                    + " versionName=" + info.versionName
                    + " flavor=" + BuildConfig.FLAVOR
                    + " buildType=" + BuildConfig.BUILD_TYPE
                    + " lastUpdateTime=" + info.lastUpdateTime);
        } catch (PackageManager.NameNotFoundException impossible) {
            Log.w(STAMP_TAG, "package info unavailable", impossible);
        }
    }
}
