/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems, LLC.
 */
package org.openidentityplatform.openam.federation.plugins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import org.testng.annotations.Test;

public class RealmGotoUrlExtractorTest {

    private static ConfigurationInstance validationService(Map<String, Object> realmAttributes)
            throws ConfigurationException {
        ConfigurationInstance instance = mock(ConfigurationInstance.class);
        when(instance.getConfiguration("/myrealm", null)).thenReturn(realmAttributes);
        return instance;
    }

    @Test
    public void readsTheRealmsValidGotoResources() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("openam-auth-valid-goto-resources",
                new HashSet<>(Arrays.asList("https://app.example.com/*", "https://portal.example.com/")));
        RealmGotoUrlExtractor extractor = new RealmGotoUrlExtractor(() -> validationService(attributes));

        assertThat(extractor.extractValidDomains("/myrealm"))
                .containsExactlyInAnyOrder("https://app.example.com/*", "https://portal.example.com/");
    }

    @Test
    public void treatsAnAbsentAttributeAsNoList() throws Exception {
        RealmGotoUrlExtractor extractor =
                new RealmGotoUrlExtractor(() -> validationService(new HashMap<String, Object>()));

        assertThat(extractor.extractValidDomains("/myrealm")).isNull();
    }

    @Test
    public void treatsAnEmptyAttributeAsNoList() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("openam-auth-valid-goto-resources", Collections.emptySet());
        RealmGotoUrlExtractor extractor = new RealmGotoUrlExtractor(() -> validationService(attributes));

        assertThat(extractor.extractValidDomains("/myrealm")).isNull();
    }

    @Test
    public void yieldsNoListWhenTheConfigurationCannotBeRead() {
        RealmGotoUrlExtractor extractor = new RealmGotoUrlExtractor(() -> {
            throw new ConfigurationException("boom");
        });

        assertThat(extractor.extractValidDomains("/myrealm")).isNull();
    }

    @Test
    public void yieldsNoListWithoutAConfigurationPlugin() {
        RealmGotoUrlExtractor extractor = new RealmGotoUrlExtractor(() -> null);

        assertThat(extractor.extractValidDomains("/myrealm")).isNull();
    }

    @Test
    public void yieldsNoListWhenTheRealmHasNoConfiguration() throws Exception {
        ConfigurationInstance instance = mock(ConfigurationInstance.class);
        when(instance.getConfiguration("/myrealm", null)).thenReturn(null);
        RealmGotoUrlExtractor extractor = new RealmGotoUrlExtractor(() -> instance);

        assertThat(extractor.extractValidDomains("/myrealm")).isNull();
    }

    @Test
    public void readsTheRootRealmForAMissingRealm() throws Exception {
        ConfigurationInstance instance = mock(ConfigurationInstance.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("openam-auth-valid-goto-resources", Collections.singleton("https://root.example.com/*"));
        when(instance.getConfiguration("/", null)).thenReturn(attributes);
        RealmGotoUrlExtractor extractor = new RealmGotoUrlExtractor(() -> instance);

        assertThat(extractor.extractValidDomains(null)).containsExactly("https://root.example.com/*");
    }
}
