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
import jakarta.servlet.http.HttpServletRequest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests the RelayState validation used by the SAML2 JSPs, for a hosted entity with no relayStateUrlList.
 */
public class RelayStateValidatorTest {

    private HttpServletRequest request;

    @BeforeMethod
    public void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("openam.example.com");
        when(request.getServerPort()).thenReturn(443);
        when(request.getContextPath()).thenReturn("/openam");
    }

    @DataProvider(name = "rejectedRelayStates")
    public Object[][] rejectedRelayStates() {
        return new Object[][] {
            { null },
            { "" },
            { "https://attacker.example.com/" },
            { "//attacker.example.com/" },
            { "https://openam.example.com@attacker.example.com/" },
            { "https://openam.example.com:8443/openam/" },
            { "javascript:alert(1)" },
            { "/\\attacker.example.com" },
        };
    }

    @Test(dataProvider = "rejectedRelayStates")
    public void shouldRejectRelayStateLeavingThisServer(String relayState) {
        assertThat(RelayStateValidator.isValid(request, null, null, relayState, SAML2Constants.SP_ROLE)).isFalse();
    }

    @DataProvider(name = "acceptedRelayStates")
    public Object[][] acceptedRelayStates() {
        return new Object[][] {
            { "/openam/saml2/jsp/default.jsp" },
            { "https://openam.example.com/openam/" },
            { "https://OPENAM.EXAMPLE.COM:443/openam/" },
        };
    }

    @Test(dataProvider = "acceptedRelayStates")
    public void shouldAcceptRelayStatePointingBackAtThisServer(String relayState) {
        assertThat(RelayStateValidator.isValid(request, null, null, relayState, SAML2Constants.SP_ROLE)).isTrue();
    }

    @Test
    public void shouldRefuseToLeaveThisServerForAHostedEntityWhoseWhitelistCanNotBeRead() {
        // The cases above name no hosted entity, so they short circuit before ValidRelayStateExtractor is asked
        // for anything. This one does name one - there is no configuration store behind it here, which is exactly
        // the case that has to stay fail safe rather than fail open. It does not pin the role selection: both
        // verdicts are settled before the role is ever read, which is what entityInfo() below is for.
        assertThat(RelayStateValidator.isValid(request, "/", "https://sp.example.com",
                "https://attacker.example.com/", SAML2Constants.SP_ROLE)).isFalse();
        assertThat(RelayStateValidator.isValid(request, "/", "https://sp.example.com",
                "https://openam.example.com/openam/XUI/", SAML2Constants.SP_ROLE)).isTrue();
    }

    // ValidRelayStateExtractor builds its own SAML2MetaManager, so there is nothing to inject and no verdict of
    // isValid() can depend on which configuration would have been read. The selection that decides it is a pure
    // function, and these pin it. The fields are asserted through toString() because SAMLEntityInfo keeps them
    // to itself.

    @Test
    public void shouldNameTheConfigurationOfTheRoleTheEndpointWasReachedFor() {
        assertThat(RelayStateValidator.entityInfo("/", "https://sp.example.com", SAML2Constants.SP_ROLE).toString())
                .contains("realm=/")
                .contains("entityID=https://sp.example.com")
                .contains("role=" + SAML2Constants.SP_ROLE);
        assertThat(RelayStateValidator.entityInfo("/", "https://idp.example.com", SAML2Constants.IDP_ROLE)
                .toString())
                .contains("realm=/")
                .contains("entityID=https://idp.example.com")
                .contains("role=" + SAML2Constants.IDP_ROLE);
    }

    @Test
    public void shouldCarryAnUnknownRoleThroughRatherThanNameOneOfItsOwn() {
        // ValidRelayStateExtractor reads the IdP configuration for anything that is not SPRole. That contract is
        // its own, so this must not quietly substitute IDPRole here and hide a caller that names no role.
        assertThat(RelayStateValidator.entityInfo("/", "https://sp.example.com", null).toString())
                .contains("role=null");
    }

    @Test
    public void shouldNameNoConfigurationWithoutAHostedEntity() {
        // Without a whitelist to read, the origin of this server is all that is left, which is what the URL cases
        // above exercise.
        assertThat(RelayStateValidator.entityInfo(null, "https://sp.example.com", SAML2Constants.SP_ROLE)).isNull();
        assertThat(RelayStateValidator.entityInfo("/", null, SAML2Constants.SP_ROLE)).isNull();
        assertThat(RelayStateValidator.entityInfo("", "", SAML2Constants.SP_ROLE)).isNull();
    }
}
