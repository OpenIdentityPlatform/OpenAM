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
 * Copyright 2014-2015 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.rest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentHashMap;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Form;
import org.restlet.data.Reference;
import org.restlet.engine.adapter.HttpRequest;
import org.forgerock.openam.rest.jakarta.servlet.internal.ServletCall;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

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
    private RealmTestHelper realmTestHelper;

    @BeforeMethod
    public void setUp() throws Exception {

        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();
        realmTestHelper.mockRealm("realm");

        router = new RestletRealmRouter();
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    @DataProvider(name = "realmRoutingDataProvider")
    private Object[][] realmRoutingDataProvider() {
        return new Object[][]{
                {"dns"},
                {"query"},
                {"uri"},
        };
    }

    @Test(dataProvider = "realmRoutingDataProvider")
    public void shouldRouteToRealm(String realmLocation) throws Exception {

        //Given
        Restlet next = mock(Restlet.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        Request request = setUpRequest(httpRequest);
        Response response = mock(Response.class);

        String realm = "realm";

        if ("dns".equalsIgnoreCase(realmLocation)) {
            //set up server name
            setUpServerName(request, realm);
        }

        if ("query".equalsIgnoreCase(realmLocation)) {
            //set up query string
            setUpServerName(request);
            setUpQueryString(request, realm);
        }

        if ("uri".equalsIgnoreCase(realmLocation)) {
            //set up uri
            setUpServerName(request);
            setUpUri(request, realm);
        }

        //When
        router.doHandle(next, request, response);

        //Then
        assertThat(request.getAttributes()).containsEntry("realm", "/realm");
        assertThat(request.getAttributes()).containsEntry("realmObject", Realm.of("/realm"));
        verify(httpRequest).setAttribute("realm", "/realm");
        verify(httpRequest).setAttribute("realmObject", Realm.of("/realm"));
        assertThat(request.getAttributes()).containsEntry("realmUrl", "The base url");
    }

    @Test
    public void shouldHandleQueryParamRealmWithNoLeadingSlash() throws Exception {

        //Given
        Restlet next = mock(Restlet.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        Request request = setUpRequest(httpRequest);
        Response response = mock(Response.class);

        setUpServerName(request);

        Reference reference = mock(Reference.class);
        given(request.getResourceRef()).willReturn(reference);
        Reference baseRef = mock(Reference.class);
        given(reference.getBaseRef()).willReturn(baseRef);
        given(baseRef.toString()).willReturn("The base url");
        Form queryForm = mock(Form.class);
        given(reference.getQueryAsForm()).willReturn(queryForm);
        given(queryForm.getFirstValue("realm")).willReturn("REALM");
        realmTestHelper.mockRealmAlias("REALM", "realm");

        //When
        router.doHandle(next, request, response);

        //Then
        assertThat(request.getAttributes()).containsEntry("realm", "/realm");
        assertThat(request.getAttributes()).containsEntry("realmObject", Realm.of("/realm"));
        verify(httpRequest).setAttribute("realm", "/realm");
        verify(httpRequest).setAttribute("realmObject", Realm.of("/realm"));
    }

    private Request setUpRequest(HttpServletRequest httpRequest) {

        HttpRequest request = generateRequest();

        ServletCall serverCall = mock(ServletCall.class);
        given(request.getHttpCall()).willReturn(serverCall);
        given(serverCall.getRequest()).willReturn(httpRequest);

        Reference reference = mock(Reference.class);
        given(request.getResourceRef()).willReturn(reference);
        Form queryForm = mock(Form.class);
        given(reference.getQueryAsForm()).willReturn(queryForm);
        Reference baseReference = mock(Reference.class);
        given(reference.getBaseRef()).willReturn(baseReference);
        given(baseReference.toString()).willReturn("The base url");

        return request;
    }

    private void setUpServerName(Request request, String... realmParts) throws IdRepoException, SSOException {
        Reference reference = request.getResourceRef();
        given(request.getHostRef()).willReturn(reference);
        given(reference.getHostDomain()).willReturn("HOST_DOMAIN");

        realmTestHelper.mockDnsAlias("HOST_DOMAIN", realmParts);
    }

    private void setUpQueryString(Request request, String realm) {
        Form form = request.getResourceRef().getQueryAsForm();
        given(form.getFirstValue("realm")).willReturn("/" + realm);
        realmTestHelper.mockRealm(realm);
    }

    private void setUpUri(Request request, String realm) {
        request.getAttributes().put("realm", "/");
        request.getAttributes().put("realmObject", Realm.root());
        request.getAttributes().put("subrealm", realm);
        realmTestHelper.mockRealm(realm);
    }

    private static HttpRequest generateRequest() {
        HttpRequest request = mock(HttpRequest.class);
        ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<String, Object>();
        given(request.getAttributes()).willReturn(map);
        return request;
    }
}