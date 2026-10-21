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
package com.sun.identity.wsfederation.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.wsfederation.plugins.whitelist.ValidWReplyExtractor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests the wreply validation used by the WS-Federation logout JSPs.
 */
public class WReplyValidatorTest {

    private HttpServletRequest request;

    @BeforeMethod
    public void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("openam.example.com");
        when(request.getServerPort()).thenReturn(443);
        when(request.getContextPath()).thenReturn("/openam");
    }

    @DataProvider(name = "rejectedWReplyUrls")
    public Object[][] rejectedWReplyUrls() {
        // Inside this deployment, so that nothing but the length limit refuses it.
        StringBuilder tooLong = new StringBuilder("https://openam.example.com/openam/");
        while (tooLong.length() <= 2000) {
            tooLong.append('a');
        }
        return new Object[][] {
            { null },
            { "" },
            { "https://attacker.example.com/" },
            { "http://attacker.example.com/" },
            { "//attacker.example.com/" },
            { "https://openam.example.com@attacker.example.com/" },
            { "https://openam.example.com.attacker.example.com/" },
            { "https://openam.example.com:8443/openam/" },
            { "http://openam.example.com/openam/" },
            { "javascript:alert(1)" },
            { "data:text/html,<script>alert(1)</script>" },
            { "/\\attacker.example.com" },
            { "https:/\\attacker.example.com" },
            { " https://attacker.example.com/" },
            { tooLong.toString() },
        };
    }

    @Test(dataProvider = "rejectedWReplyUrls")
    public void shouldRejectWReplyUrlsLeavingThisServer(String wreply) {
        assertThat(WReplyValidator.pointsBackAtThisServer(request, wreply)).isFalse();
    }

    @DataProvider(name = "acceptedWReplyUrls")
    public Object[][] acceptedWReplyUrls() {
        return new Object[][] {
            { "/openam/XUI/#logout/" },
            { "openam/UI/Login" },
            { "https://openam.example.com/openam/" },
            { "https://openam.example.com:443/openam/" },
            { "https://OPENAM.EXAMPLE.COM/openam/" },
        };
    }

    @Test(dataProvider = "acceptedWReplyUrls")
    public void shouldAcceptWReplyUrlsPointingBackAtThisServer(String wreply) {
        assertThat(WReplyValidator.pointsBackAtThisServer(request, wreply)).isTrue();
    }

    // realmSelection.jsp and the deprecated WSFederationUtils.isWReplyURLValid reach the origin fallback
    // through the role-bearing overload instead, so it gets the same URL cases rather than relying on
    // pointsBackAtThisServer having been the only way in.

    @Test(dataProvider = "rejectedWReplyUrls")
    public void shouldRejectWReplyUrlsLeavingThisServerForAnEntityWithNoWhitelist(String wreply) {
        assertThat(WReplyValidator.isValid(request, null, null, wreply, SAML2Constants.SP_ROLE)).isFalse();
    }

    @Test(dataProvider = "acceptedWReplyUrls")
    public void shouldAcceptWReplyUrlsPointingBackAtThisServerForAnEntityWithNoWhitelist(String wreply) {
        assertThat(WReplyValidator.isValid(request, null, null, wreply, SAML2Constants.SP_ROLE)).isTrue();
    }

    // The tests above cover the endpoints whose hosted entity is named by the caller, where no whitelist is read
    // at all. The role selection that decides which whitelist would be read where the entity is trustworthy is a
    // pure function, and these pin it. The role is asserted through toString() because WSFederationEntityInfo
    // keeps its fields to itself.

    @Test
    public void shouldConsultOnlyTheIdpWhitelistForAnIdpEndpoint() {
        List<ValidWReplyExtractor.WSFederationEntityInfo> infos =
                WReplyValidator.entityInfos("/", "urn:federation:example", SAML2Constants.IDP_ROLE);

        assertThat(infos).hasSize(1);
        assertThat(infos.get(0).toString()).contains("role=" + SAML2Constants.IDP_ROLE);
    }

    @Test
    public void shouldConsultOnlyTheSpWhitelistForAnSpEndpoint() {
        List<ValidWReplyExtractor.WSFederationEntityInfo> infos =
                WReplyValidator.entityInfos("/", "urn:federation:example", SAML2Constants.SP_ROLE);

        assertThat(infos).hasSize(1);
        assertThat(infos.get(0).toString()).contains("role=" + SAML2Constants.SP_ROLE);
    }

    @Test
    public void shouldConsultBothWhitelistsWhereTheRoleIsUnknown() {
        // multi.jsp carries no metaAlias, and the entity of a WS-Federation signout may be hosted as either role,
        // so both whitelists count rather than whichever one happens to be configured.
        List<ValidWReplyExtractor.WSFederationEntityInfo> infos =
                WReplyValidator.entityInfos("/", "urn:federation:example", null);

        assertThat(infos).hasSize(2);
        assertThat(infos.get(0).toString()).contains("role=" + SAML2Constants.IDP_ROLE);
        assertThat(infos.get(1).toString()).contains("role=" + SAML2Constants.SP_ROLE);
        assertThat(WReplyValidator.entityInfos("/", "urn:federation:example", "SomeOtherRole")).hasSize(2);
    }

    @Test
    public void shouldNameNoConfigurationWithoutAHostedEntity() {
        // Without a whitelist to read, the origin of this server is all that is left, which is what the URL cases
        // above exercise.
        assertThat(WReplyValidator.entityInfos(null, "urn:federation:example", SAML2Constants.IDP_ROLE)).isEmpty();
        assertThat(WReplyValidator.entityInfos("/", null, SAML2Constants.IDP_ROLE)).isEmpty();
        assertThat(WReplyValidator.entityInfos("", "", null)).isEmpty();
    }
}
