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

package org.forgerock.openidconnect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.Map;

import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * WebFinger is anonymous, and the issuer it names is the same for every account, so its answer must not
 * depend on whether the account in the resource exists: a 404 for an unknown account would let anyone
 * check which accounts a realm holds.
 */
public class OpenIDConnectProviderDiscoveryTest {

    private static final String ISSUER_REL = "http://openid.net/specs/connect/1.0/issuer";
    private static final String DEPLOYMENT_URL = "http://openam.example.org:8080/openam";

    private OpenIDConnectProviderDiscovery discovery;

    @BeforeMethod
    public void setUp() {
        discovery = new OpenIDConnectProviderDiscovery();
    }

    @DataProvider
    public Object[][] resourcesOfAnAccountThatDoesNotExist() {
        return new Object[][] {
            { "acct:no-such-account@openam.example.org" },
            { "http://openam.example.org/no-such-account" },
        };
    }

    /** No account is looked up, so an unknown one gets the issuer like any other. */
    @Test(dataProvider = "resourcesOfAnAccountThatDoesNotExist")
    public void answersWithTheIssuerForAnAccountThatDoesNotExist(String resource) throws Exception {

        Map<String, Object> response = discovery.discover(resource, ISSUER_REL, DEPLOYMENT_URL,
                mock(OAuth2Request.class));

        assertThat(response).containsEntry("subject", resource);
        assertThat((Collection<Object>) response.get("links"))
                .containsExactly(Map.of("rel", ISSUER_REL, "href", DEPLOYMENT_URL + "/oauth2"));
    }

    @DataProvider
    public Object[][] malformedResources() {
        return new Object[][] {
            { null },
            { "" },
            { "no-scheme" },
            { "mailto:demo@openam.example.org" },
            { "http:demo" },
            { "acct:demo" },
            { "acct:demo@openam example org" },
        };
    }

    /** The resource is still parsed, and one that names neither an account nor a URL is rejected. */
    @Test(dataProvider = "malformedResources")
    public void rejectsAMalformedResource(String resource) {

        assertThatThrownBy(() -> discovery.discover(resource, ISSUER_REL, DEPLOYMENT_URL,
                mock(OAuth2Request.class)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    public void rejectsARelOtherThanTheIssuer() {

        assertThatThrownBy(() -> discovery.discover("acct:demo@openam.example.org",
                "http://webfinger.net/rel/profile-page", DEPLOYMENT_URL, mock(OAuth2Request.class)))
                .isInstanceOf(BadRequestException.class);
    }
}
