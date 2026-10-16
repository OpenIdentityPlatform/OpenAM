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
 * Portions copyright 2026 3A Systems, LLC.
 */

package org.forgerock.openidconnect;

import static org.forgerock.openam.oauth2.OAuth2Constants.UrlLocation.FRAGMENT;
import static org.forgerock.openam.oauth2.OAuth2Constants.UrlLocation.QUERY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.OAuth2Constants.UrlLocation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * PKCE enforcement must cover every response_type that issues an authorization code,
 * including the OIDC hybrid flows ("code token", "code id_token", "code id_token token").
 */
public class CodeVerifierValidatorTest {

    private CodeVerifierValidator validator;
    private OAuth2ProviderSettings providerSettings;
    private OAuth2Request request;

    @BeforeMethod
    public void setUp() throws Exception {
        OAuth2ProviderSettingsFactory providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        providerSettings = mock(OAuth2ProviderSettings.class);
        request = mock(OAuth2Request.class);
        given(providerSettingsFactory.get(any(OAuth2Request.class))).willReturn(providerSettings);
        validator = new CodeVerifierValidator(providerSettingsFactory);
    }

    /** Stubs the provider settings and the three request parameters the validator reads. */
    private void givenRequest(String responseType, boolean enforced, String codeChallenge,
            String codeChallengeMethod) throws Exception {
        given(providerSettings.isCodeVerifierRequired()).willReturn(enforced);
        given(request.<String>getParameter(OAuth2Constants.Params.RESPONSE_TYPE)).willReturn(responseType);
        given(request.<String>getParameter(OAuth2Constants.Custom.CODE_CHALLENGE)).willReturn(codeChallenge);
        given(request.<String>getParameter(OAuth2Constants.Custom.CODE_CHALLENGE_METHOD))
                .willReturn(codeChallengeMethod);
    }

    @DataProvider(name = "rejected")
    public Object[][] rejected() {
        return new Object[][] {
                // case, response_type, enforced, code_challenge, code_challenge_method, error location.
                // OIDC Core 3.3.2.6: response types carrying id_token or token must report the error
                // in the fragment, everything else in the query string.
                {1, "code", true, null, null, QUERY},
                {2, "code token", true, null, null, FRAGMENT},
                {3, "code id_token", true, null, null, FRAGMENT},
                {4, "code id_token token", true, null, null, FRAGMENT},
                {9, "code", true, "aChallenge", "bogus", QUERY},
        };
    }

    @DataProvider(name = "accepted")
    public Object[][] accepted() {
        return new Object[][] {
                {5, "code token", true, "aChallenge", null},
                {6, "token", true, null, null},
                {7, "id_token token", true, null, null},
                {8, "code token", false, null, null},
                {10, "code", true, "aChallenge", OAuth2Constants.Custom.CODE_CHALLENGE_METHOD_S_256},
                {11, "code", true, "aChallenge", OAuth2Constants.Custom.CODE_CHALLENGE_METHOD_PLAIN},
        };
    }

    @Test(dataProvider = "rejected")
    public void shouldRejectRequest(int caseNumber, String responseType, boolean enforced, String codeChallenge,
            String codeChallengeMethod, UrlLocation expectedLocation) throws Exception {
        givenRequest(responseType, enforced, codeChallenge, codeChallengeMethod);

        try {
            validator.validateRequest(request);
            fail("case " + caseNumber + " [response_type=" + responseType + ", enforced=" + enforced
                    + ", code_challenge=" + codeChallenge + ", code_challenge_method=" + codeChallengeMethod
                    + "]: expected InvalidRequestException but none was thrown");
        } catch (InvalidRequestException expected) {
            // PKCE violation correctly rejected: the error must also be reported where the client can read it
            assertEquals(expected.getParameterLocation(), expectedLocation,
                    "case " + caseNumber + " [response_type=" + responseType + "]: wrong error location");
        }
    }

    @Test(dataProvider = "accepted")
    public void shouldAcceptRequest(int caseNumber, String responseType, boolean enforced, String codeChallenge,
            String codeChallengeMethod) throws Exception {
        givenRequest(responseType, enforced, codeChallenge, codeChallengeMethod);

        try {
            validator.validateRequest(request);
        } catch (InvalidRequestException e) {
            fail("case " + caseNumber + " [response_type=" + responseType + ", enforced=" + enforced
                    + ", code_challenge=" + codeChallenge + ", code_challenge_method=" + codeChallengeMethod
                    + "]: unexpected rejection - " + e.getMessage());
        }
    }

    /** Case 12: a missing response_type is a malformed request, not a server crash. */
    @Test
    public void shouldNotThrowNullPointerExceptionForMissingResponseType() throws Exception {
        givenRequest(null, true, null, null);

        try {
            validator.validateRequest(request);
        } catch (NullPointerException e) {
            fail("case 12 [response_type=null]: NullPointerException thrown");
        } catch (RuntimeException expected) {
            // any other rejection is acceptable
        }
    }
}
