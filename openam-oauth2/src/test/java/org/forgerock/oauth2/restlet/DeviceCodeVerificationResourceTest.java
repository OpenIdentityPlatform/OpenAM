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

package org.forgerock.oauth2.restlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Map;

import org.forgerock.oauth2.core.AuthorizationService;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.CsrfProtection;
import org.forgerock.oauth2.core.DeviceCode;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.ResourceOwner;
import org.forgerock.oauth2.core.ResourceOwnerSessionValidator;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.OAuth2Utils;
import org.forgerock.openam.services.baseurl.BaseURLProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.xui.XUIState;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Status;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.routing.Router;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for DeviceCodeVerificationResource.
 */
public class DeviceCodeVerificationResourceTest {

    private static final String USER_CODE = "USER_CODE";

    private DeviceCodeVerificationResource resource;
    private OAuth2Request o2request;
    private TokenStore tokenStore;
    private AuthorizationService authorizationService;
    private ResourceOwnerSessionValidator resourceOwnerSessionValidator;
    private DeviceCode deviceCode;

    @BeforeMethod
    public void setup() throws Exception {
        Request request = new Request();
        OAuth2RequestFactory requestFactory = mock(OAuth2RequestFactory.class);
        o2request = mock(OAuth2Request.class);
        given(requestFactory.create(request)).willReturn(o2request);
        given(o2request.getParameter(OAuth2Constants.DeviceCode.USER_CODE)).willReturn(USER_CODE);
        given(o2request.getParameter("realm")).willReturn("/");
        given(o2request.getRequest()).willReturn(request);

        BaseURLProviderFactory baseURLProviderFactory = mock(BaseURLProviderFactory.class);
        given(baseURLProviderFactory.get(anyString())).willReturn(mock(BaseURLProvider.class));

        tokenStore = mock(TokenStore.class);
        deviceCode = mock(DeviceCode.class);
        given(deviceCode.getObject()).willReturn(Collections.emptyMap());
        given(tokenStore.readDeviceCode(USER_CODE, o2request)).willReturn(deviceCode);

        OAuth2ProviderSettingsFactory providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        given(providerSettingsFactory.get(o2request)).willReturn(providerSettings);
        given(providerSettings.clientsCanSkipConsent()).willReturn(true);

        ClientRegistrationStore clientRegistrationStore = mock(ClientRegistrationStore.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        given(clientRegistrationStore.get(any(), any())).willReturn(clientRegistration);
        given(clientRegistration.isConsentImplied()).willReturn(true);

        resourceOwnerSessionValidator = mock(ResourceOwnerSessionValidator.class);
        ResourceOwner resourceOwner = mock(ResourceOwner.class);
        given(resourceOwner.getId()).willReturn("RESOURCE_OWNER");
        given(resourceOwnerSessionValidator.validate(o2request)).willReturn(resourceOwner);

        authorizationService = mock(AuthorizationService.class);

        resource = spy(new DeviceCodeVerificationResource(mock(XUIState.class), mock(Router.class),
                baseURLProviderFactory, mock(OAuth2Representation.class), tokenStore, requestFactory,
                authorizationService, providerSettingsFactory, mock(ExceptionHandler.class),
                resourceOwnerSessionValidator, clientRegistrationStore, mock(OAuth2Utils.class),
                mock(CsrfProtection.class)));
        doReturn(request).when(resource).getRequest();
        doReturn(templatesFromClasspathContext()).when(resource).getContext();
    }

    @Test
    public void shouldAuthorizeValidDeviceCode() throws Exception {
        // Given
        given(deviceCode.getExpiryTime()).willReturn(currentTimeMillis() + 10000);

        // When
        Representation result = resource.verify(null);

        // Then
        assertThat(errorCode(result)).isNull();
        verify(deviceCode).setResourceOwnerId("RESOURCE_OWNER");
        verify(deviceCode).setAuthorized(true);
        verify(tokenStore).updateDeviceCode(deviceCode, o2request);
    }

    @Test
    public void shouldRejectExpiredDeviceCode() throws Exception {
        // Given
        given(deviceCode.getExpiryTime()).willReturn(currentTimeMillis() - 100);

        // When
        Representation result = resource.verify(null);

        // Then
        assertThat(errorCode(result)).isEqualTo("not_found");
        verify(deviceCode, never()).setAuthorized(anyBoolean());
        verify(tokenStore, never()).updateDeviceCode(any(), any());
        verifyZeroInteractions(resourceOwnerSessionValidator, authorizationService);
    }

    @Test
    public void shouldRejectDeviceCodeThatIsAlreadyAuthorized() throws Exception {
        // Given
        given(deviceCode.getExpiryTime()).willReturn(currentTimeMillis() + 10000);
        given(deviceCode.isAuthorized()).willReturn(true);

        // When
        Representation result = resource.verify(null);

        // Then
        assertThat(errorCode(result)).isEqualTo("not_found");
        verify(deviceCode, never()).setResourceOwnerId(anyString());
        verify(tokenStore, never()).updateDeviceCode(any(), any());
        verifyZeroInteractions(resourceOwnerSessionValidator, authorizationService);
    }

    /**
     * A context whose clap:// lookups find nothing, so the templates load from the classpath.
     */
    private static Context templatesFromClasspathContext() {
        Context context = new Context();
        context.setClientDispatcher(new Restlet() {
            @Override
            public void handle(Request request, Response response) {
                response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            }
        });
        return context;
    }

    @SuppressWarnings("unchecked")
    private static String errorCode(Representation representation) {
        return ((Map<String, String>) ((TemplateRepresentation) representation).getDataModel()).get("errorCode");
    }
}
