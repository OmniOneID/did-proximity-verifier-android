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

package org.omnione.did.proximity.verifier.ui.issuers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.omnione.did.proximity.verifier.BuildConfig;
import org.omnione.did.proximity.verifier.R;
import org.omnione.did.proximity.verifier.data.repository.ServerUrlRepository;

import java.util.Map;

public class ServerUrlTest {

    private static final String TAS = ServerUrlRepository.KEY_TAS_URL;
    private static final String API_GW = ServerUrlRepository.KEY_API_GW_URL;

    @Test
    public void normalizeTrimsAndDropsTrailingSlashes() {
        assertEquals("http://a:8090", ServerUrlViewModel.normalize("http://a:8090/"));
        assertEquals("http://a:8090", ServerUrlViewModel.normalize("http://a:8090///"));
        assertEquals("http://a", ServerUrlViewModel.normalize("  http://a  "));
        assertEquals("/", ServerUrlViewModel.normalize("/"));
    }

    @Test
    public void validateReportsEachError() {
        assertEquals(R.string.server_url_error_empty, ServerUrlViewModel.validate("  "));
        assertEquals(R.string.server_url_error_format,
                ServerUrlViewModel.validate("htp://10.0.0.5:8090"));
        assertEquals(R.string.server_url_error_port,
                ServerUrlViewModel.validate("http://10.0.0.5:70000"));
        assertEquals(0, ServerUrlViewModel.validate(" http://10.0.0.5:8090/path "));
    }

    @Test
    public void defaultPromotedToCustomKeepsCache() {
        assertFalse(ServerUrlRepository.changed(Map.of(), Map.of(TAS, BuildConfig.TAS_URL)));
    }

    @Test
    public void trailingSlashOnlyKeepsCache() {
        String next = ServerUrlViewModel.normalize(BuildConfig.TAS_URL + "/");
        assertFalse(ServerUrlRepository.changed(Map.of(), Map.of(TAS, next)));
    }

    @Test
    public void portChangeClearsCache() {
        assertTrue(ServerUrlRepository.changed(Map.of(), Map.of(TAS, "http://192.168.3.110:8091")));
        assertTrue(ServerUrlRepository.changed(Map.of(), Map.of(API_GW, "http://192.168.3.110:8094")));
    }

    @Test
    public void backToDefaultFromSameValueKeepsCache() {
        assertFalse(ServerUrlRepository.changed(Map.of(TAS, BuildConfig.TAS_URL), Map.of()));
    }

    @Test
    public void backToDefaultFromOtherValueClearsCache() {
        assertTrue(ServerUrlRepository.changed(Map.of(TAS, "http://10.0.0.5:8090"), Map.of()));
    }
}
