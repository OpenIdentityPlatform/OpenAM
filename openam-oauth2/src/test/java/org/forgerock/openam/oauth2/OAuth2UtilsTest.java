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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.InvalidConfirmationKeyException;
import org.forgerock.openam.oauth2.OAuth2Constants.ProofOfPossession;
import org.forgerock.openam.oauth2.validation.ConfirmationKeyValidator;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link OAuth2Utils}.
 *
 * @since 14.0.0
 */
public final class OAuth2UtilsTest {

    private OAuth2Utils utils;

    @Mock
    private JacksonRepresentationFactory factory;
    @Mock
    private ConfirmationKeyValidator validator;
    @Mock
    private OAuth2Request request;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        utils = new OAuth2Utils(factory, validator);
    }

    @Test
    public void confirmationKeyExtractedFromRequest() throws InvalidConfirmationKeyException {
        // Given
        // Base64 string contains a dumb but valid JWK.
        given(request.getParameter(ProofOfPossession.CNF_KEY)).willReturn("eyAiandrIjogeyAidGVzdCI6IDEyMyB9IH0=");

        // When
        JsonValue cnfKey = utils.getConfirmationKey(request);

        // Then
        assertThat(cnfKey).isNotNull();
        assertThat(cnfKey.isDefined("jwk")).isTrue();
    }

    @Test
    public void confirmationKeyNotExtractedWhenNotInRequst() throws InvalidConfirmationKeyException {
        // Given
        given(request.getParameter(ProofOfPossession.CNF_KEY)).willReturn(null);

        // When
        JsonValue cnfKey = utils.getConfirmationKey(request);

        // Then
        assertThat(cnfKey).isNull();
    }

}