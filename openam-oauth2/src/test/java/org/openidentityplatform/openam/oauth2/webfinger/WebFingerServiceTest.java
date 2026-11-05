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

package org.openidentityplatform.openam.oauth2.webfinger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.http.Handler;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.http.annotations.Endpoints;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.services.baseurl.BaseURLProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openidconnect.OpenIDConnectProviderDiscovery;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class WebFingerServiceTest {

    private static final String ROOT_URL = "http://openam.example.org:8080/openam";
    private static final String ISSUER_REL = "http://openid.net/specs/connect/1.0/issuer";
    private static final String WRONG_REL = "http://example.com/not-the-issuer-rel";
    private static final String RESOURCE = "acct:demo@openam.example.org";

    private RealmTestHelper realmTestHelper;
    private Context context;
    private Handler endpoint;

    @BeforeMethod
    public void setUp() throws Exception {
        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        BaseURLProvider baseURLProvider = mock(BaseURLProvider.class);
        given(baseURLProvider.getRootURL(servletRequest)).willReturn(ROOT_URL);
        BaseURLProviderFactory baseURLProviderFactory = mock(BaseURLProviderFactory.class);
        // Only the realm of the request has a base URL provider: another realm would answer 500.
        given(baseURLProviderFactory.get("/sub")).willReturn(baseURLProvider);
        AttributesContext attributes = new AttributesContext(new RootContext());
        attributes.getAttributes().put(HttpServletRequest.class.getName(), servletRequest);
        context = new RealmContext(attributes, realmTestHelper.mockRealm("sub"));
        endpoint = Endpoints.from(new WebFingerService(new OpenIDConnectProviderDiscovery(), baseURLProviderFactory));
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    private Response call(String method, String query) throws Exception {
        Request request = new Request().setMethod(method)
                .setUri("http://openam.example.org:8080/openam/.well-known/webfinger" + query);
        return endpoint.handle(context, request).getOrThrowUninterruptibly();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void answersTheIssuerJrdWithTheDeploymentUrlOfTheRealm() throws Exception {
        Response response = call("GET", "?resource=" + RESOURCE + "&rel=" + ISSUER_REL);

        assertThat(response.getStatus()).isEqualTo(Status.OK);
        assertThat(response.getHeaders().getFirst(ContentTypeHeader.NAME))
                .isEqualTo("application/json; charset=UTF-8");
        Map<?, ?> jrd = (Map<?, ?>) response.getEntity().getJson();
        assertThat(jrd.get("subject")).isEqualTo(RESOURCE);
        assertThat((Collection<Object>) jrd.get("links"))
                .containsExactly(Map.of("rel", ISSUER_REL, "href", ROOT_URL + "/oauth2"));
    }

    @Test
    public void usesTheFirstValueOfARepeatedParameter() throws Exception {
        Response response = call("GET", "?resource=acct:first@openam.example.org"
                + "&resource=acct:second@openam.example.org&rel=" + ISSUER_REL + "&rel=" + WRONG_REL);

        assertThat(response.getStatus()).isEqualTo(Status.OK);
        assertThat(((Map<?, ?>) response.getEntity().getJson()).get("subject"))
                .isEqualTo("acct:first@openam.example.org");
    }

    @DataProvider
    public Object[][] badRequests() {
        return new Object[][] {
            {"?rel=" + ISSUER_REL, "No resource provided in discovery."},
            {"?resource=" + RESOURCE, "No or invalid rel provided in discovery."},
            {"?resource=" + RESOURCE + "&rel=" + WRONG_REL, "No or invalid rel provided in discovery."},
            {"?resource=mailto:demo@openam.example.org&rel=" + ISSUER_REL, "Invalid parameters."},
        };
    }

    @Test(dataProvider = "badRequests")
    public void answersDiscoveryErrorsInTheOAuth2ShapeWithoutACharset(String query, String description)
            throws Exception {
        Response response = call("GET", query);

        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST);
        assertThat(response.getHeaders().getFirst(ContentTypeHeader.NAME)).isEqualTo("application/json");
        assertThat(response.getEntity().getJson())
                .isEqualTo(Map.of("error", "bad_request", "error_description", description));
    }

    @Test
    public void answersHeadLikeGet() throws Exception {
        Response response = call("HEAD", "?resource=" + RESOURCE + "&rel=" + ISSUER_REL);

        assertThat(response.getStatus()).isEqualTo(Status.OK);
        assertThat(response.getHeaders().getFirst(ContentTypeHeader.NAME))
                .isEqualTo("application/json; charset=UTF-8");
    }

    @Test
    public void answersPostWith405AndAllowGet() throws Exception {
        Response response = call("POST", "?resource=" + RESOURCE + "&rel=" + ISSUER_REL);

        assertThat(response.getStatus()).isEqualTo(Status.METHOD_NOT_ALLOWED);
        assertThat(response.getHeaders().getFirst("Allow")).isEqualTo("GET");
    }
}
