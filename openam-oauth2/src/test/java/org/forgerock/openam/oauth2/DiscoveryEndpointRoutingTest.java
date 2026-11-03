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

package org.forgerock.openam.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Collections;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.guice.core.InjectorConfiguration;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.restlet.AccessTokenFlowFinder;
import org.forgerock.oauth2.restlet.AuthorizeResource;
import org.forgerock.oauth2.restlet.DeviceCodeResource;
import org.forgerock.oauth2.restlet.ExceptionHandler;
import org.forgerock.oauth2.restlet.TokenIntrospectionResource;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.oauth2.rest.OAuth2RouterProvider;
import org.forgerock.openam.oauth2.rest.TokenRevocationResource;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.forgerock.openidconnect.OpenIDConnectProviderConfiguration;
import org.forgerock.openidconnect.restlet.ConnectClientRegistration;
import org.forgerock.openidconnect.restlet.EndSession;
import org.forgerock.openidconnect.restlet.OpenIDConnectCheckSessionEndpoint;
import org.forgerock.openidconnect.restlet.OpenIDConnectJWKEndpoint;
import org.forgerock.openidconnect.restlet.UserInfo;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Method;
import org.restlet.resource.Finder;
import org.restlet.routing.Filter;
import org.restlet.routing.Router;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Every endpoint the OpenID Connect discovery document advertises has to be served by the OAuth2 router, by the
 * resource that implements it. A client that takes its endpoints from discovery has no other way to find them.
 */
public class DiscoveryEndpointRoutingTest extends GuiceTestCase {

    private static final String BASE_URL = "https://openam.example.com:8443/openam/oauth2";

    private Router router;
    private JsonValue discovery;

    /**
     * {@code GuiceTestCase} overrides the injector {@code InjectorHolder} holds, so the holder has to build one
     * first. It builds it once, from the module loader in place when it is first touched, and the Guice modules on
     * this classpath cannot make an injector without a configured OpenAM - so ask for none before any test of the
     * suite runs. The loader is a JVM wide setting, which is why it is not restored.
     */
    @BeforeSuite(alwaysRun = true)
    public void withoutGuiceModules() {
        InjectorConfiguration.setGuiceModuleLoader(moduleAnnotation -> Collections.emptySet());
    }

    /**
     * The router looks up the resource set registration endpoint and the token endpoint's dependencies while it is
     * built, and each {@code RestletUtils.wrap} finder creates its resource through the injector.
     */
    @Override
    public void configure(Binder binder) {
        binder.bind(Key.get(Restlet.class, Names.named(OAuth2Constants.Custom.RSR_ENDPOINT)))
                .toProvider(Providers.of(mock(Restlet.class)));
        bindMock(binder, OAuth2RequestFactory.class);
        bindMock(binder, ExceptionHandler.class);
        bindMock(binder, AuthorizeResource.class);
        bindMock(binder, TokenIntrospectionResource.class);
        bindMock(binder, TokenRevocationResource.class);
        bindMock(binder, UserInfo.class);
        bindMock(binder, OpenIDConnectCheckSessionEndpoint.class);
        bindMock(binder, EndSession.class);
        bindMock(binder, OpenIDConnectJWKEndpoint.class);
        bindMock(binder, ConnectClientRegistration.class);
        bindMock(binder, DeviceCodeResource.class);
    }

    private static <T> void bindMock(Binder binder, Class<T> type) {
        binder.bind(type).toProvider(Providers.of(mock(type)));
    }

    @BeforeMethod
    public void setup() throws Exception {
        router = new OAuth2RouterProvider(mock(AuditEventPublisher.class), mock(AuditEventFactory.class),
                mock(OAuth2RequestFactory.class), mock(JacksonRepresentationFactory.class)).get();

        OAuth2Request request = mock(OAuth2Request.class);
        OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        given(providerSettings.exists()).willReturn(true);
        OAuth2ProviderSettingsFactory providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        given(providerSettingsFactory.get(request)).willReturn(providerSettings);
        OAuth2UrisFactory urisFactory = mock(OAuth2UrisFactory.class);
        given(urisFactory.get(request)).willReturn(
                new OAuth2UrisFactory.OAuth2UrisImpl("https://openam.example.com:8443/openam", "/",
                        providerSettings, BASE_URL));

        discovery = new OpenIDConnectProviderConfiguration(providerSettingsFactory, urisFactory)
                .getConfiguration(request);
    }

    @DataProvider
    public Object[][] advertisedEndpoints() {
        return new Object[][] {
                {"authorization_endpoint", AuthorizeResource.class},
                {"token_endpoint", AccessTokenFlowFinder.class},
                {"introspection_endpoint", TokenIntrospectionResource.class},
                {"revocation_endpoint", TokenRevocationResource.class},
                {"userinfo_endpoint", UserInfo.class},
                {"check_session_iframe", OpenIDConnectCheckSessionEndpoint.class},
                {"end_session_endpoint", EndSession.class},
                {"jwks_uri", OpenIDConnectJWKEndpoint.class},
                {"registration_endpoint", ConnectClientRegistration.class},
                {"device_authorization_endpoint", DeviceCodeResource.class},
        };
    }

    @Test(dataProvider = "advertisedEndpoints")
    public void shouldRouteAdvertisedEndpointToItsResource(String metadata, Class<?> resource) {
        String url = discovery.get(metadata).asString();

        assertThat(url).as(metadata).startsWith(BASE_URL + "/");
        assertThat(resourceAt(url)).as(metadata + " " + url).isInstanceOf(resource);
    }

    private Object resourceAt(String url) {
        Request request = new Request(Method.POST, url);
        request.getResourceRef().setBaseRef(BASE_URL);
        Response response = new Response(request);

        Restlet target = router.getNext(request, response);
        while (target instanceof Filter) {
            target = ((Filter) target).getNext();
        }
        // The token endpoint's finder picks the resource by grant type, so there the finder is what is routed to.
        if (target instanceof Finder && !(target instanceof AccessTokenFlowFinder)) {
            return ((Finder) target).create(request, response);
        }
        return target;
    }
}
