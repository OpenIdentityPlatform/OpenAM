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
package org.forgerock.openam.saml2.plugins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.entityconfig.AttributeType;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.meta.SAML2MetaException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.forgerock.openam.saml2.plugins.ValidRelayStateExtractor.SAMLEntityInfo;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests the two answers {@link ValidRelayStateExtractor} gives for "no patterns here" - an entity that was read and
 * has no <code>relayStateUrlList</code>, and one whose configuration could not be read at all - and which list of an
 * entity hosting both roles it reads.
 */
public class ValidRelayStateExtractorTest {

    private static final String REALM = "/";

    private static final String ENTITY = "urn:example:entity";

    private static final List<String> SP_LIST = Collections.singletonList("https://sp.example.com/*");

    private static final List<String> IDP_LIST = Collections.singletonList("https://idp.example.com/*");

    private StubExtractor extractor;

    @BeforeMethod
    public void setUp() {
        extractor = new StubExtractor();
    }

    @Test
    public void shouldReadTheListOfTheSpRole() {
        extractor.spConfig = configWith(SP_LIST);
        extractor.idpConfig = configWith(IDP_LIST);

        assertThat(extractor.extractValidDomains(entityInfo(SAML2Constants.SP_ROLE))).isEqualTo(SP_LIST);
        assertThat(extractor.rolesRead).containsExactly(SAML2Constants.SP_ROLE);
    }

    @Test
    public void shouldReadTheListOfTheIdpRole() {
        extractor.spConfig = configWith(SP_LIST);
        extractor.idpConfig = configWith(IDP_LIST);

        assertThat(extractor.extractValidDomains(entityInfo(SAML2Constants.IDP_ROLE))).isEqualTo(IDP_LIST);
        assertThat(extractor.rolesRead).containsExactly(SAML2Constants.IDP_ROLE);
    }

    @Test
    public void shouldReportAnUnresolvedRoleAsAConfigurationThatCouldNotBeRead() {
        extractor.spConfig = configWith(SP_LIST);
        extractor.idpConfig = configWith(IDP_LIST);

        assertThat(extractor.extractValidDomains(entityInfo(SAML2Constants.UNKNOWN_ROLE))).isNull();
        assertThat(extractor.rolesRead).isEmpty();
    }

    @Test
    public void shouldReportAnEntityWithoutAListAsOneThatWasRead() {
        extractor.spConfig = configWith(null);

        assertThat(extractor.extractValidDomains(entityInfo(SAML2Constants.SP_ROLE))).isEmpty();
    }

    @Test
    public void shouldReportAnEntityNotHostingTheRoleAsOneWithoutAList() {
        extractor.spConfig = null;

        assertThat(extractor.extractValidDomains(entityInfo(SAML2Constants.SP_ROLE))).isEmpty();
    }

    @Test
    public void shouldReportAStoreFailureAsAConfigurationThatCouldNotBeRead() {
        extractor.failure = new SAML2MetaException("the configuration store is unreachable");

        assertThat(extractor.extractValidDomains(entityInfo(SAML2Constants.SP_ROLE))).isNull();
    }

    private static SAMLEntityInfo entityInfo(String role) {
        return SAMLEntityInfo.from(REALM, ENTITY, role);
    }

    /**
     * A hosted entity configuration carrying the given relayStateUrlList, or none when it is <code>null</code>.
     */
    private static BaseConfigType configWith(List<String> relayStateUrlList) {
        BaseConfigType config = mock(BaseConfigType.class);
        if (relayStateUrlList == null) {
            when(config.getAttribute()).thenReturn(Collections.emptyList());
            return config;
        }
        AttributeType attribute = mock(AttributeType.class);
        when(attribute.getName()).thenReturn(SAML2Constants.RELAY_STATE_URL_LIST);
        when(attribute.getValue()).thenReturn(relayStateUrlList);
        when(config.getAttribute()).thenReturn(Arrays.asList(attribute));
        return config;
    }

    /**
     * Answers with the configuration the test set, and records which role's configuration was asked for. The meta
     * manager itself is out of reach here: it reads a configuration store to be built at all.
     */
    private static final class StubExtractor extends ValidRelayStateExtractor {

        private final List<String> rolesRead = new ArrayList<String>();

        private BaseConfigType spConfig;

        private BaseConfigType idpConfig;

        private SAML2MetaException failure;

        @Override
        BaseConfigType getSPSSOConfig(String realm, String entityId) throws SAML2MetaException {
            rolesRead.add(SAML2Constants.SP_ROLE);
            if (failure != null) {
                throw failure;
            }
            return spConfig;
        }

        @Override
        BaseConfigType getIDPSSOConfig(String realm, String entityId) throws SAML2MetaException {
            rolesRead.add(SAML2Constants.IDP_ROLE);
            if (failure != null) {
                throw failure;
            }
            return idpConfig;
        }
    }
}
