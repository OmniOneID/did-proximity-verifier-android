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

package org.omnione.did.proximity.verifier.ui.engagement;

import android.os.Handler;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import java.util.function.BooleanSupplier;

final class NfcLinkCloseWatch {

    static final long POLL_INTERVAL_MS = 50L;

    interface Scheduler {
        void postDelayed(@NonNull Runnable task, long delayMillis);

        long uptimeMillis();
    }

    private NfcLinkCloseWatch() {
    }

    static void start(@NonNull Handler handler,
                      @NonNull BooleanSupplier linkOpen,
                      long capMillis,
                      @NonNull Runnable onClosed) {
        start(new Scheduler() {
            @Override
            public void postDelayed(@NonNull Runnable task, long delayMillis) {
                handler.postDelayed(task, delayMillis);
            }

            @Override
            public long uptimeMillis() {
                return SystemClock.uptimeMillis();
            }
        }, linkOpen, capMillis, onClosed);
    }

    static void start(@NonNull Scheduler scheduler,
                      @NonNull BooleanSupplier linkOpen,
                      long capMillis,
                      @NonNull Runnable onClosed) {
        long deadline = scheduler.uptimeMillis() + capMillis;
        Runnable tick = new Runnable() {
            @Override
            public void run() {
                boolean open;
                try {
                    open = linkOpen.getAsBoolean();
                } catch (RuntimeException e) {
                    open = false;
                }
                if (!open || scheduler.uptimeMillis() >= deadline) {
                    onClosed.run();
                    return;
                }
                scheduler.postDelayed(this, POLL_INTERVAL_MS);
            }
        };
        scheduler.postDelayed(tick, POLL_INTERVAL_MS);
    }
}
