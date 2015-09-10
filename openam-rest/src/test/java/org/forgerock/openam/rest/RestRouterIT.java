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

package org.forgerock.openam.rest;

import static com.google.inject.multibindings.MapBinder.newMapBinder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.openam.audit.AuditConstants.Component.*;
import static org.forgerock.openam.rest.Routers.ssoToken;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.fail;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.guice.core.GuiceModuleLoader;
import org.forgerock.guice.core.GuiceModules;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.guice.core.InjectorConfiguration;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.Session;
import org.forgerock.http.context.AttributesContext;
import org.forgerock.http.context.RequestAuditContext;
import org.forgerock.http.context.RootContext;
import org.forgerock.http.context.SessionContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.ResourceApiVersionBehaviourManager;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.openam.audit.AbstractHttpAccessAuditFilter;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.configuration.AMAuditServiceConfiguration;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurator;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.authentication.service.AuthUtilsWrapper;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.http.HttpGuiceModule;
import org.forgerock.openam.http.annotations.Get;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@GuiceModules({HttpGuiceModule.class, RestGuiceModule.class})
public class RestRouterIT extends GuiceTestCase {

    private Handler handler;

    private SingletonResourceProvider configResource;
    private CollectionResourceProvider usersResource;
    private DashboardResource dashboardResource;
    private AuthenticateResource authenticateResource;
    private AbstractHttpAccessAuditFilter httpAccessAuditFilter;
    private AuditServiceConfigurator auditServiceConfigurator;
    private ResourceApiVersionBehaviourManager versionBehaviourManager;
    private SSOTokenManager ssoTokenManager;
    private AuthUtilsWrapper authUtilsWrapper;
    private CoreWrapper coreWrapper;
    private RestRealmValidator realmValidator;

    @BeforeMethod
    public void setupMocks() {
        configResource = mock(SingletonResourceProvider.class);
        usersResource = mock(CollectionResourceProvider.class);
        dashboardResource = spy(new DashboardResource());
        authenticateResource = spy(new AuthenticateResource());

        httpAccessAuditFilter = spy(new AbstractHttpAccessAuditFilter(AUTHENTICATION, mock(AuditEventPublisher.class),
                mock(AuditEventFactory.class)) {});
        auditServiceConfigurator = mock(AuditServiceConfigurator.class);

        versionBehaviourManager = mock(ResourceApiVersionBehaviourManager.class);

        ssoTokenManager = mock(SSOTokenManager.class);
        authUtilsWrapper = mock(AuthUtilsWrapper.class);


        coreWrapper = mock(CoreWrapper.class);
        SSOToken adminToken = mock(SSOToken.class);
        given(coreWrapper.getAdminToken()).willReturn(adminToken);
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


        binder.bind(AuditServiceConfigurator.class).toInstance(auditServiceConfigurator);


        binder.bind(Key.get(SingletonResourceProvider.class, Names.named("ConfigResource"))).toInstance(configResource);
        binder.bind(Key.get(CollectionResourceProvider.class, Names.named("UsersResource"))).toInstance(usersResource);
        binder.bind(Key.get(Object.class, Names.named("DashboardResource"))).toInstance(dashboardResource);
        binder.bind(Key.get(Object.class, Names.named("AuthenticateResource"))).toInstance(authenticateResource);


        binder.bind(SSOTokenManager.class).toInstance(ssoTokenManager);
        binder.bind(AuthUtilsWrapper.class).toInstance(authUtilsWrapper);


        binder.bind(CoreWrapper.class).toInstance(coreWrapper);
        binder.bind(RestRealmValidator.class).toInstance(realmValidator);
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
    }

    @Test
    public void shouldReadCrestEndpointOnRootHandler() throws Exception {

        //Given
        Context context = mockContext();
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
        Context context = mockContext();
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
        Context context = mockContext();
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
        Context context = mockContext();
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

    private Context mockContext() {
        AttributesContext httpRequestContext = new AttributesContext(new SessionContext(new RootContext(), mock(Session.class)));

        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        httpRequestContext.getAttributes().put(HttpServletRequest.class.getName(), httpServletRequest);

        return new RequestAuditContext(httpRequestContext);
    }

    private Request newRequest(String method, String uri) throws URISyntaxException {
        Request request = new Request()
                .setMethod(method)
                .setUri(URI.create(uri));
        request.getUri().setHost("HOSTNAME");
        return request;
    }

    private void auditingOff() {
        AMAuditServiceConfiguration auditServiceConfiguration = mock(AMAuditServiceConfiguration.class);
        given(auditServiceConfigurator.getAuditServiceConfiguration()).willReturn(auditServiceConfiguration);
        given(auditServiceConfiguration.isAuditEnabled()).willReturn(false);
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

        @Inject
        void setConfigResource(@Named("ConfigResource") SingletonResourceProvider configResource) {
            this.configResource = configResource;
        }

        @Inject
        void setUsersResource(@Named("UsersResource") CollectionResourceProvider usersResource) {
            this.usersResource = usersResource;
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
