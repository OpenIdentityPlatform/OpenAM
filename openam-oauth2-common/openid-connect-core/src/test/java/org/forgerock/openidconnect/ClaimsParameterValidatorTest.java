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
* Copyright 2015 ForgeRock AS.
*/
package org.forgerock.openidconnect;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class ClaimsParameterValidatorTest {

    OAuth2ProviderSettingsFactory mockProviderSettingsFactory;
    ClaimsParameterValidator claimsParameterValidator;

    //when accessing userinfo endpoint should return name
    //when accessing id_token endpoint should reutrn name either "Sponge" | "Bob"
    String validClaimsString = "{\"userinfo\" : { \"name\" : null }, " +
                                "\"id_token\" : { \"name\" : { \"values\" : [ \"Sponge\", \"Bob\" ] } } }";

    String invalidClaimsString = "This is not valid JSON.";

    @BeforeTest
    public void setUp() {
        this.mockProviderSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        this.claimsParameterValidator = new ClaimsParameterValidator(mockProviderSettingsFactory);

    }

    @Test
    public void shouldValidateClaimsParameter() throws NotFoundException, BadRequestException,
            RedirectUriMismatchException, InvalidScopeException, InvalidRequestException, InvalidClientException,
            ServerException, UnsupportedResponseTypeException {

        //given
        OAuth2Request mockRequest = mock(OAuth2Request.class);
        OAuth2ProviderSettings mockProviderSettings = mock(OAuth2ProviderSettings.class);
        String responseTypes = "code token id_token";

        given(mockProviderSettingsFactory.get(mockRequest)).willReturn(mockProviderSettings);
        given(mockProviderSettings.getClaimsParameterSupported()).willReturn(true);
        given(mockRequest.getParameter(OAuth2Constants.Custom.CLAIMS)).willReturn(validClaimsString);
        given(mockRequest.getParameter(OAuth2Constants.Params.RESPONSE_TYPE)).willReturn(responseTypes);

        //when
        claimsParameterValidator.validateRequest(mockRequest);

        //then

    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldErrorValidatingJson() throws NotFoundException, BadRequestException,
            RedirectUriMismatchException, InvalidScopeException, InvalidRequestException, InvalidClientException,
            ServerException, UnsupportedResponseTypeException {

        //given
        OAuth2Request mockRequest = mock(OAuth2Request.class);
        OAuth2ProviderSettings mockProviderSettings = mock(OAuth2ProviderSettings.class);
        String responseTypes = "id_token";

        given(mockProviderSettingsFactory.get(mockRequest)).willReturn(mockProviderSettings);
        given(mockProviderSettings.getClaimsParameterSupported()).willReturn(true);
        given(mockRequest.getParameter(OAuth2Constants.Custom.CLAIMS)).willReturn(invalidClaimsString);
        given(mockRequest.getParameter(OAuth2Constants.Params.RESPONSE_TYPE)).willReturn(responseTypes);

        //when
        claimsParameterValidator.validateRequest(mockRequest);

        //then
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldErrorValidatingResponseType() throws NotFoundException, BadRequestException,
            RedirectUriMismatchException, InvalidScopeException, InvalidRequestException, InvalidClientException,
            ServerException, UnsupportedResponseTypeException {

        //given
        OAuth2Request mockRequest = mock(OAuth2Request.class);
        OAuth2ProviderSettings mockProviderSettings = mock(OAuth2ProviderSettings.class);
        String responseTypes = "id_token";

        given(mockProviderSettingsFactory.get(mockRequest)).willReturn(mockProviderSettings);
        given(mockProviderSettings.getClaimsParameterSupported()).willReturn(true);
        given(mockRequest.getParameter(OAuth2Constants.Custom.CLAIMS)).willReturn(validClaimsString);
        given(mockRequest.getParameter(OAuth2Constants.Params.RESPONSE_TYPE)).willReturn(responseTypes);

        //when
        claimsParameterValidator.validateRequest(mockRequest);

        //then
    }

}
