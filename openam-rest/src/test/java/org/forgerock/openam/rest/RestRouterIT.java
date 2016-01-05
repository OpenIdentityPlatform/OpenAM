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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.rest;

import static com.google.inject.multibindings.MapBinder.newMapBinder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.audit.AuditConstants.EventName;
import static org.forgerock.openam.audit.AuditConstants.ACCESS_TOPIC;
import static org.forgerock.openam.audit.AuditConstants.Component.AUTHENTICATION;
import static org.forgerock.openam.audit.AuditConstants.Component.CONFIG;
import static org.forgerock.openam.audit.AuditConstants.Component.USERS;
import static org.forgerock.openam.audit.AuditConstants.NO_REALM;
import static org.forgerock.openam.rest.Routers.ssoToken;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

import org.forgerock.guice.core.GuiceModuleLoader;
import org.forgerock.guice.core.GuiceModules;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.guice.core.InjectorConfiguration;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.ResourceApiVersionBehaviourManager;
import org.forgerock.http.session.Session;
import org.forgerock.http.session.SessionContext;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.audit.AbstractHttpAccessAuditFilter;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.AuditServiceProvider;
import org.forgerock.openam.authentication.service.AuthUtilsWrapper;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.http.HttpGuiceModule;
import org.forgerock.openam.http.annotations.Get;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RequestAuditContext;
import org.forgerock.services.context.RootContext;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@GuiceModules({HttpGuiceModule.class, RestGuiceModule.class})
public class RestRouterIT extends GuiceTestCase {

    private Handler handler;

    private SingletonResourceProvider configResource;
    private CollectionResourceProvider usersResource;
    private CollectionResourceProvider internalResource;
    private DashboardResource dashboardResource;
    private AuthenticateResource authenticateResource;
    private AbstractHttpAccessAuditFilter httpAccessAuditFilter;
    private AuditEventPublisher auditEventPublisher;
    private AuditServiceProvider auditServiceProvider;
    private ResourceApiVersionBehaviourManager versionBehaviourManager;
    private SSOTokenManager ssoTokenManager;
    private AuthUtilsWrapper authUtilsWrapper;
    private CoreWrapper coreWrapper;
    private RestRealmValidator realmValidator;

    @Mock
    private PrivilegedAction<SSOToken> ssoTokenAction;

    @BeforeMethod
    public void setupMocks() {
        MockitoAnnotations.initMocks(this);

        configResource = mock(SingletonResourceProvider.class);
        usersResource = mock(CollectionResourceProvider.class);
        internalResource = mock(CollectionResourceProvider.class);
        dashboardResource = spy(new DashboardResource());
        authenticateResource = spy(new AuthenticateResource());

        httpAccessAuditFilter = spy(new AbstractHttpAccessAuditFilter(AUTHENTICATION, mock(AuditEventPublisher.class)
                , mock(AuditEventFactory.class)) {
            @Override
            protected String getRealm(Context context) {
                return null;
            }
        });
        auditEventPublisher = mock(AuditEventPublisher.class);
        auditServiceProvider = mock(AuditServiceProvider.class);

        versionBehaviourManager = mock(ResourceApiVersionBehaviourManager.class);

        ssoTokenManager = mock(SSOTokenManager.class);
        authUtilsWrapper = mock(AuthUtilsWrapper.class);

        coreWrapper = mock(CoreWrapper.class);
        SSOToken adminToken = mock(SSOToken.class);
        given(coreWrapper.getAdminToken()).willReturn(adminToken);
        given(coreWrapper.isValidFQDN(anyString())).willReturn(true);
        realmValidator = mock(RestRealmValidator.class);
    }

    @BeforeMethod(dependsOnMethods = "setupMocks")
    @Override
    public void setupGuiceModules() throws Exception {
        InjectorConfiguration.setGuiceModuleLoader(new GuiceModuleLoader() {
            @Override
            public Set<Class<? extends Module>> getGuiceModules(Class<? extends Annotation> clazz) {
                return new HashSet<>();
            }
        });
        super.setupGuiceModules();
    }

    @Override
    public void configure(Binder binder) {
        MapBinder<AuditConstants.Component, AbstractHttpAccessAuditFilter> httpAccessAuditFilterMapBinder
                = newMapBinder(binder, AuditConstants.Component.class, AbstractHttpAccessAuditFilter.class);
        httpAccessAuditFilterMapBinder.addBinding(AUTHENTICATION).toInstance(httpAccessAuditFilter);


        binder.bind(AuditEventPublisher.class).toInstance(auditEventPublisher);
        binder.bind(AuditServiceProvider.class).toInstance(auditServiceProvider);


        binder.bind(Key.get(SingletonResourceProvider.class, Names.named("ConfigResource"))).toInstance(configResource);
        binder.bind(Key.get(CollectionResourceProvider.class, Names.named("UsersResource"))).toInstance(usersResource);
        binder.bind(Key.get(CollectionResourceProvider.class, Names.named("InternalResource"))).toInstance(internalResource);
        binder.bind(Key.get(Object.class, Names.named("DashboardResource"))).toInstance(dashboardResource);
        binder.bind(Key.get(Object.class, Names.named("AuthenticateResource"))).toInstance(authenticateResource);


        binder.bind(SSOTokenManager.class).toInstance(ssoTokenManager);
        binder.bind(AuthUtilsWrapper.class).toInstance(authUtilsWrapper);


        binder.bind(CoreWrapper.class).toInstance(coreWrapper);
        binder.bind(RestRealmValidator.class).toInstance(realmValidator);

        binder.bind(new TypeLiteral<PrivilegedAction<SSOToken>>() {}).toInstance(ssoTokenAction);

        binder.bind(SessionCache.class).toInstance(mock(SessionCache.class));
        binder.bind(Debug.class).annotatedWith(Names.named(SessionConstants.SESSION_DEBUG))
                .toInstance(mock(Debug.class));
    }

    @Override
    public void configureOverrideBindings(Binder binder) {
        binder.bind(ResourceApiVersionBehaviourManager.class).toInstance(versionBehaviourManager);
    }

    @BeforeMethod(dependsOnMethods = "setupGuiceModules")
    public void setup() throws Exception {
        handler = InjectorHolder.getInstance(HttpApplication.class).start();

        mockDnsAlias("HOSTNAME", "/");
        doThrow(IdRepoException.class).when(coreWrapper).getOrganization(any(SSOToken.class), eq("users"));
        doThrow(IdRepoException.class).when(coreWrapper).getOrganization(any(SSOToken.class), eq("authenticate"));
        doThrow(IdRepoException.class).when(coreWrapper).getOrganization(any(SSOToken.class), eq("internal"));
    }

    @Test
    public void shouldReadCrestEndpointOnRootHandler() throws Exception {

        //Given
        Context context = mockRequiredContexts();
        Request request = newRequest("GET", "/json/config");

        auditingOff();

        //When
        handler.handle(context, request);

        //Then
        verify(configResource).readInstance(any(Context.class), any(ReadRequest.class));
    }

    @Test
    public void shouldReadChfEndpointOnRootHandler() throws Exception {

        //Given
        Context context = mockContext();
        Request request = newRequest("GET", "/json/dashboard");

        //When
        handler.handle(context, request);

        //Then
        verify(dashboardResource).get();
    }

    @Test
    public void shouldReadCrestEndpointOnRealmHandlerWithRootRealm() throws Exception {

        //Given
        Context context = mockRequiredContexts();
        Request request = newRequest("GET", "/json/users/demo");

        auditingOff();

        //When
        handler.handle(context, request);

        //Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(usersResource).readInstance(contextCaptor.capture(), eq("demo"), any(ReadRequest.class));
        assertThat(contextCaptor.getValue().asContext(RealmContext.class).getResolvedRealm()).isEqualTo("/");
    }

    @Test
    public void shouldReadCrestEndpointOnRealmHandlerWithSubRealm() throws Exception {

        //Given
        Context context = mockRequiredContexts();
        Request request = newRequest("GET", "/json/subrealm/users/demo");

        auditingOff();
        mockRealm("/subrealm");

        //When
        handler.handle(context, request);

        //Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(usersResource).readInstance(contextCaptor.capture(), eq("demo"), any(ReadRequest.class));
        assertThat(contextCaptor.getValue().asContext(RealmContext.class).getResolvedRealm()).isEqualTo("/subrealm");
    }

    @Test
    public void shouldReadChfEndpointOnRealmHandlerWithRootRealm() throws Exception {

        //Given
        Context context = mockRequiredContexts();
        Request request = newRequest("GET", "/json/authenticate");

        //When
        handler.handle(context, request);

        //Then
        verify(authenticateResource).get();
    }

    @Test
    public void shouldReadChfEndpointOnRealmHandlerWithSubRealm() throws Exception {

        //Given
        Context context = mockContext();
        Request request = newRequest("GET", "/json/subrealm/authenticate");

        mockRealm("/subrealm");

        //When
        handler.handle(context, request);

        //Then
        verify(authenticateResource).get();
    }

    @Test
    public void shouldNotBePossibleToReachInternalResourceViaChf() throws Exception {
        // Given
        Context context = mockContext();
        Request request = newRequest("GET", "/json/internal");

        // When
        Promise<Response, NeverThrowsException> promise = handler.handle(context, request);

        // Then
        Response response = promise.get();
        assertThat(response.getStatus()).isEqualTo(Status.NOT_FOUND);

        verifyZeroInteractions(internalResource);
    }

    @Test
    public void shouldBeAbleToReachInternalViaInternalRouter() throws Exception {
        // Given
        Promise<ResourceResponse, ResourceException> promise =
                newResultPromise(newResourceResponse("1", "1", json(object())));
        given(internalResource.readInstance(any(Context.class), eq("123"), any(ReadRequest.class))).willReturn(promise);

        Router internalRouter = InjectorHolder.getInstance(Key.get(Router.class, Names.named("InternalCrestRouter")));

        Context context = mockRequiredContexts();
        ReadRequest request = Requests.newReadRequest("internal/123");

        // When
        internalRouter.handleRead(context, request);

        // Then
        verify(internalResource).readInstance(any(Context.class), eq("123"), any(ReadRequest.class));
    }

    private Context mockContext() {
        return mockContext(null);
    }

    private Context mockContext(Context parent) {
        if (parent == null) {
            parent = new RootContext();
        }
        AttributesContext httpRequestContext = new AttributesContext(new SessionContext(parent, mock(Session.class)));

        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        httpRequestContext.getAttributes().put(HttpServletRequest.class.getName(), httpServletRequest);

        return new RequestAuditContext(httpRequestContext);
    }

    private Context mockRequiredContexts() {
        final HttpContext httpContext = new HttpContext(json(object(
                field(HttpContext.ATTR_HEADERS, Collections.singletonMap("Accept-Language", Arrays.asList("en"))),
                field(HttpContext.ATTR_PARAMETERS, Collections.emptyMap()))), null);
        SecurityContext securityContext = new SecurityContext(mockContext(httpContext), null, null);
        return new SSOTokenContext(mock(Debug.class), null, securityContext) {
            @Override
            public Subject getCallerSubject() {
                return new Subject();
            }

            @Override
            public SSOToken getCallerSSOToken() {
                SSOToken token = mock(SSOToken.class);
                try {
                    given(token.getProperty(Constants.AM_CTX_ID)).willReturn("TRACKING_ID");
                    given(token.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("USER_ID");
                } catch (SSOException e) {
                    // won't happen - it's a mock
                }
                return token;
            }
        };
    }

    private Request newRequest(String method, String uri) throws URISyntaxException {
        Request request = new Request()
                .setMethod(method)
                .setUri(URI.create(uri));
        request.getUri().setHost("HOSTNAME");
        return request;
    }

    private void auditingOff() {
        given(auditEventPublisher.isAuditing(eq(NO_REALM), eq(ACCESS_TOPIC), any(EventName.class))).willReturn(false);
    }

    private void mockDnsAlias(String alias, String realm) throws Exception {
        mockRealmAlias(alias, realm);
    }
    private void mockRealm(String realm) throws Exception {
        given(coreWrapper.getOrganization(any(SSOToken.class), eq(realm))).willReturn(realm);
        given(coreWrapper.convertOrgNameToRealmName(realm)).willReturn(realm);
        given(realmValidator.isRealm(realm)).willReturn(true);
    }

    private void mockRealmAlias(String alias, String realm) throws Exception {
        given(coreWrapper.getOrganization(any(SSOToken.class), eq(alias))).willReturn(realm);
        given(coreWrapper.convertOrgNameToRealmName(realm)).willReturn(realm);
        given(realmValidator.isRealm(realm)).willReturn(true);
    }

    public static final class RestRouteTestRestRouteProvider extends AbstractRestRouteProvider {

        private SingletonResourceProvider configResource;
        private CollectionResourceProvider usersResource;
        private CollectionResourceProvider internalResource;
        private Object dashboardResource;
        private Object authenticateResource;

        @Override
        public void addResourceRoutes(ResourceRouter rootRouter, ResourceRouter realmRouter) {

            rootRouter.route("config")
                    .authenticateWith(ssoToken().exceptRead())
                    .auditAs(CONFIG)
                    .authorizeWith()
                    .through()
                    .forVersion(1, 1)
                    .authorizeWith()
                    .through()
                    .forVersion(2)
                    .authorizeWith()
                    .through()
                    .toSingleton(configResource);



            realmRouter.route("users")
                    .authenticateWith(ssoToken().exceptRead())
                    .auditAs(USERS)
                    .authorizeWith()
                    .through()
                    .forVersion(1, 1)
                    .authorizeWith()
                    .through()
                    .forVersion(2)
                    .authorizeWith()
                    .through()
                    .toCollection(usersResource);
        }

        @Override
        public void addServiceRoutes(ServiceRouter rootRouter, ServiceRouter realmRouter) {

            rootRouter.route("dashboard")
                    .through()
                    .forVersion(1, 1)
                    .through()
                    .forVersion(2)
                    .through()
                    .toService(STARTS_WITH, dashboardResource);



            realmRouter.route("authenticate")
                    .auditAs(AUTHENTICATION)
                    .through()
                    .forVersion(1, 1)
                    .through()
                    .forVersion(2)
                    .through()
                    .toService(EQUALS, authenticateResource);
        }

        @Override
        public void addInternalRoutes(ResourceRouter internalRouter) {
            internalRouter.route("internal")
                    .authenticateWith(ssoToken().exceptRead())
                    .toCollection(internalResource);
        }

        @Inject
        void setConfigResource(@Named("ConfigResource") SingletonResourceProvider configResource) {
            this.configResource = configResource;
        }

        @Inject
        void setUsersResource(@Named("UsersResource") CollectionResourceProvider usersResource) {
            this.usersResource = usersResource;
        }

        @Inject
        void setInternalResource(@Named("InternalResource") CollectionResourceProvider internalResource) {
            this.internalResource = internalResource;
        }

        @Inject
        void setDashboardResource(@Named("DashboardResource") Object dashboardResource) {
            this.dashboardResource = dashboardResource;
        }

        @Inject
        void setAuthenticateResource(@Named("AuthenticateResource") Object authenticateResource) {
            this.authenticateResource = authenticateResource;
        }
    }

    public static class DashboardResource {
        @Get
        public Response get() {
            return new Response(Status.OK);
        }
    }

    public static class AuthenticateResource {
        @Get
        public Response get() {
            return new Response(Status.OK);
        }
    }
}
