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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.rest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Form;
import org.restlet.data.Reference;
import org.restlet.engine.adapter.HttpRequest;
import org.restlet.ext.servlet.internal.ServletCall;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentHashMap;

public class RestletRealmRouterTest {
    @Test
    public void shouldExtractRealmFromRequest() {
        // Given
        String key = "badger";
        Request mockRequest = generateRequest();
        mockRequest.getAttributes().put(RestletRealmRouter.REALM, key);

        // When
        String result = RestletRealmRouter.getRealmFromRequest(mockRequest);

        // Then
        assertThat(result).isEqualTo(key);
    }

    @Test
    public void shouldReturnNullWhenMissing() {
        // Given
        Request mockRequest = generateRequest();

        // When
        String result = RestletRealmRouter.getRealmFromRequest(mockRequest);

        // Then
        assertThat(result).isNull();
    }

    /*

    get realm from policy advice
    get realm alias from policy advice
    get realm from uri
    get realm alias from uri
    get realm from query param
    get realm alias from query param
    get realm alias (with leading slash) from query param?
    get realm from dns alias

     */

    private RestletRealmRouter router;
    private RestRealmValidator realmValidator;
    private CoreWrapper coreWrapper;

    @BeforeMethod
    public void setUp() {

        realmValidator = mock(RestRealmValidator.class);
        coreWrapper = mock(CoreWrapper.class);

        router = new RestletRealmRouter(realmValidator, coreWrapper);
    }

    @DataProvider(name = "realmRoutingDataProvider")
    private Object[][] realmRoutingDataProvider() {
        return new Object[][]{
                {"dns", false},
                {"query", false},
                {"query", true},
                {"uri", false},
                {"uri", true}
        };
    }

    @Test(dataProvider = "realmRoutingDataProvider")
    public void shouldRouteToRealm(String realmLocation, boolean isRealmAlias) throws Exception {

        //Given
        SSOToken adminToken = mock(SSOToken.class);
        Restlet next = mock(Restlet.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        Request request = setUpRequest(httpRequest, adminToken);
        Response response = mock(Response.class);

        String realm;
        if (!isRealmAlias) {
            realm = "REALM";
        } else {
            realm = "REALM_ALIAS";
        }

        if ("dns".equalsIgnoreCase(realmLocation)) {
            //set up server name
            setUpServerName(request, adminToken, realm);
        }

        if ("query".equalsIgnoreCase(realmLocation)) {
            //set up query string
            setUpServerName(request, adminToken, "/");
            setUpQueryString(request, realm);
        }

        if ("uri".equalsIgnoreCase(realmLocation)) {
            //set up uri
            setUpUri(request, realm);
        }

        //set up validate realm
        setUpRealmValidator(realm, isRealmAlias, adminToken);

        //When
        router.doHandle(next, request, response);

        //Then
        assertThat(request.getAttributes()).containsEntry("realm", "/REALM");
        verify(httpRequest).setAttribute("realm", "/REALM");
    }

    private Request setUpRequest(HttpServletRequest httpRequest, SSOToken adminToken) {

        HttpRequest request = generateRequest();

        ServletCall serverCall = mock(ServletCall.class);
        given(request.getHttpCall()).willReturn(serverCall);
        given(serverCall.getRequest()).willReturn(httpRequest);

        Reference reference = mock(Reference.class);
        given(request.getResourceRef()).willReturn(reference);
        Form queryForm = mock(Form.class);
        given(reference.getQueryAsForm()).willReturn(queryForm);

        given(coreWrapper.getAdminToken()).willReturn(adminToken);

        return request;
    }

    private void setUpServerName(Request request, SSOToken adminToken, String realm) throws IdRepoException, SSOException {
        Reference reference = mock(Reference.class);
        given(request.getResourceRef()).willReturn(reference);
        Form queryForm = mock(Form.class);
        given(reference.getQueryAsForm()).willReturn(queryForm);
        given(request.getHostRef()).willReturn(reference);
        given(reference.getHostDomain()).willReturn("HOST_DOMAIN");

        given(coreWrapper.getOrganization(adminToken, "HOST_DOMAIN")).willReturn("REALM_HOST_DN");
        given(coreWrapper.convertOrgNameToRealmName("REALM_HOST_DN")).willReturn(realm.equals("/") ? realm : "/" + realm);
    }

    private void setUpQueryString(Request request, String realm) {
        Reference reference = mock(Reference.class);
        given(request.getResourceRef()).willReturn(reference);
        Form queryForm = mock(Form.class);
        given(reference.getQueryAsForm()).willReturn(queryForm);
        given(queryForm.getFirstValue("realm")).willReturn("/" + realm);
    }

    private void setUpUri(Request request, String realm) {
        request.getAttributes().put("realm", "/");
        request.getAttributes().put("subrealm", realm);
    }

    private void setUpRealmValidator(String realm, boolean isRealmAlias, SSOToken adminToken) throws IdRepoException, SSOException {
        given(realmValidator.isRealm(realm)).willReturn(!isRealmAlias);
        given(realmValidator.isRealm("/" + realm)).willReturn(!isRealmAlias);
        if (isRealmAlias) {
            given(coreWrapper.getOrganization(adminToken, "REALM_ALIAS")).willReturn("REALM_DN");
            given(coreWrapper.convertOrgNameToRealmName("REALM_DN")).willReturn("/REALM");
        }
    }

    private static HttpRequest generateRequest() {
        HttpRequest request = mock(HttpRequest.class);
        ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<String, Object>();
        given(request.getAttributes()).willReturn(map);
        return request;
    }
}