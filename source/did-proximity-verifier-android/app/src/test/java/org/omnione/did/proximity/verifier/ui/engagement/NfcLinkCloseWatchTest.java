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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class NfcLinkCloseWatchTest {

    private static final class ManualScheduler implements NfcLinkCloseWatch.Scheduler {
        final List<Runnable> queue = new ArrayList<>();
        final List<Long> delays = new ArrayList<>();
        long now;

        @Override
        public void postDelayed(Runnable task, long delayMillis) {
            queue.add(task);
            delays.add(delayMillis);
        }

        @Override
        public long uptimeMillis() {
            return now;
        }

        void runNext(long advanceMillis) {
            now += advanceMillis;
            Runnable task = queue.remove(0);
            task.run();
        }
    }

    @Test
    public void firesAsSoonAsTheLinkIsSeenClosed() {
        ManualScheduler scheduler = new ManualScheduler();
        boolean[] open = {true};
        int[] fired = {0};

        NfcLinkCloseWatch.start(scheduler, () -> open[0], 1_000L, () -> fired[0]++);

        assertEquals(1, scheduler.queue.size());
        scheduler.runNext(50L);
        assertEquals(0, fired[0]);
        open[0] = false;
        scheduler.runNext(50L);
        assertEquals(1, fired[0]);
        assertTrue(scheduler.queue.isEmpty());
    }

    @Test
    public void firesAtTheCapWhenTheLinkNeverCloses() {
        ManualScheduler scheduler = new ManualScheduler();
        int[] fired = {0};

        NfcLinkCloseWatch.start(scheduler, () -> true, 1_000L, () -> fired[0]++);

        for (int i = 0; i < 19; i++) {
            scheduler.runNext(50L);
        }
        assertEquals(0, fired[0]);
        scheduler.runNext(50L);
        assertEquals(1, fired[0]);
        assertTrue(scheduler.queue.isEmpty());
    }

    @Test
    public void anAlreadyClosedLinkStillFiresOffTheFirstTick() {
        ManualScheduler scheduler = new ManualScheduler();
        int[] fired = {0};

        NfcLinkCloseWatch.start(scheduler, () -> false, 1_000L, () -> fired[0]++);

        assertEquals(0, fired[0]);
        scheduler.runNext(0L);
        assertEquals(1, fired[0]);
    }

    @Test
    public void pollsAtTheDeclaredInterval() {
        ManualScheduler scheduler = new ManualScheduler();

        NfcLinkCloseWatch.start(scheduler, () -> true, 1_000L, () -> { });
        scheduler.runNext(50L);

        assertEquals(List.of(NfcLinkCloseWatch.POLL_INTERVAL_MS, NfcLinkCloseWatch.POLL_INTERVAL_MS),
                scheduler.delays);
    }
}
