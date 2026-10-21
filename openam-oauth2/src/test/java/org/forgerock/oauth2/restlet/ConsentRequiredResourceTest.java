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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.oauth2.core.CsrfProtection;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.UserInfoClaims;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerConsentRequired;
import org.forgerock.openam.services.baseurl.BaseURLProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.xui.XUIState;
import org.restlet.Request;
import org.restlet.data.Method;
import org.restlet.routing.Router;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ConsentRequiredResourceTest {

    private static final String PAYLOAD = "<img src=x onerror=alert(1)>";

    private ConsentRequiredResource resource;
    private OAuth2Request o2request;

    @BeforeMethod
    public void setup() {
        BaseURLProviderFactory baseURLProviderFactory = mock(BaseURLProviderFactory.class);
        BaseURLProvider baseURLProvider = mock(BaseURLProvider.class);
        when(baseURLProviderFactory.get(nullable(String.class))).thenReturn(baseURLProvider);
        when(baseURLProvider.getRootURL(nullable(HttpServletRequest.class))).thenReturn("http://am.example.com/openam");
        CsrfProtection csrfProtection = mock(CsrfProtection.class);
        o2request = mock(OAuth2Request.class);
        when(csrfProtection.createCsrfToken(any(OAuth2Request.class))).thenReturn("csrf-token");

        resource = spy(new AuthorizeResource(null, null, null, null, Collections.emptySet(),
                mock(XUIState.class), mock(Router.class), baseURLProviderFactory, null, null, csrfProtection));
        doReturn(new Request(Method.GET, "http://am.example.com/openam/oauth2/authorize?display=page"))
                .when(resource).getRequest();
    }

    @Test
    public void shouldHtmlEncodeUserDisplayName() {
        //given
        ResourceOwnerConsentRequired consentRequired = consentRequiredFor(PAYLOAD);

        //when
        Map<String, Object> data = resource.getDataModel(consentRequired, o2request);

        //then
        assertThat((String) data.get("user_name"))
                .doesNotContain("<", ">")
                .isEqualTo("&lt;img src&#x3d;x onerror&#x3d;alert&#x28;1&#x29;&gt;");
    }

    @Test
    public void shouldLeaveUserNameAbsentWhenDisplayNameIsUnknown() {
        //when
        Map<String, Object> data = resource.getDataModel(consentRequiredFor(null), o2request);

        //then
        assertThat(data.get("user_name")).isNull();
    }

    private ResourceOwnerConsentRequired consentRequiredFor(String userDisplayName) {
        return new ResourceOwnerConsentRequired("client", "description", Collections.emptyMap(),
                Collections.emptyMap(), new UserInfoClaims(Collections.emptyMap(), Collections.emptyMap()),
                userDisplayName, false);
    }
}
