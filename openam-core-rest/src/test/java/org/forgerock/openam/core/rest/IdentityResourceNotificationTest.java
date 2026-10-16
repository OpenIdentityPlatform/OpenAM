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
package org.forgerock.openam.core.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.guice.core.GuiceModuleLoader;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.guice.core.InjectorConfiguration;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.forgerockrest.utils.MailServerLoader;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.services.RestSecurity;
import org.forgerock.openam.services.RestSecurityProvider;
import org.forgerock.openam.services.baseurl.BaseURLProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.services.email.MailServer;
import org.forgerock.openam.services.email.MailServerImpl;
import org.forgerock.openam.sm.config.ConsoleConfigHandler;
import org.forgerock.services.context.Context;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.iplanet.sso.SSOException;
import com.sun.identity.idsvcs.opensso.IdentityServicesImpl;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * GHSA-mw38-8gr7-c4x2: the anonymous register action used to take the subject and the body of the notification
 * straight out of the request body, so an unauthenticated caller chose the wording of a mail the server sent out
 * under its own From address, and a CR or an LF in the subject reached the Subject header.
 *
 * The tests drive the action the way a caller does and assert that what reaches the mail server is what the Email
 * Service of the realm carries, whatever the request asked for. Both versions of the resource that send the
 * notification are covered: the users endpoint routes 1.x to {@link IdentityResourceV1} and 2.x to
 * {@link IdentityResourceV2}, and 3.0 delegates the self service actions to the second of them.
 */
public class IdentityResourceNotificationTest extends GuiceTestCase {

    private static final String REALM = "/";
    private static final String EMAIL = "victim@example.com";

    /** What an administrator put in the Email Service of the realm. */
    private static final String CONFIGURED_SUBJECT = "Confirm your registration";
    private static final String CONFIGURED_MESSAGE = "Follow the link to confirm your registration.";

    /** What an unauthenticated caller used to be able to put in the mail instead. */
    private static final String HOSTILE_SUBJECT = "Your account is suspended\r\nBcc: attacker@example.org";
    private static final String HOSTILE_MESSAGE = "Sign in at https://attacker.example.org to keep your account.";

    private static final String CONFIRMATION_URL = "https://openam.example.com/openam/json/confirmation/register";
    private static final String REQUEST_PATH = "https://openam.example.com/openam/json/users";

    /**
     * The store the registration token is written to. Held statically because each resource reads it once per JVM,
     * through a lazily initialised holder.
     */
    private static final CTSPersistentStore CTS = mock(CTSPersistentStore.class);

    private RealmTestHelper realmTestHelper;
    private MailServer mailServer;
    private IdentityResourceV1 resourceV1;
    private IdentityResourceV2 resourceV2;

    /**
     * The modules of the running server cannot be built on this test classpath - the session module needs a
     * notification broker that is not there - so the first injector of this JVM has to be built from no modules at
     * all, and the bindings a test needs come from {@link #configure(Binder)}.
     *
     * The loader is not put back afterwards, and could not be: InjectorConfiguration exposes no way to read the
     * one in place, and the default implementation of it is not public either. It does not matter much, because
     * the loader is read once, while the first injector of the JVM is built, and after this class has run that
     * injector exists whatever the loader says. What a later test class in the same fork inherits is therefore an
     * empty injector - which is what it would have got in this module anyway, since the real modules cannot be
     * built here at all. A test that needs bindings has to bring them, the way this one does through
     * {@link GuiceTestCase}, which is also what puts the injector back after every test method.
     */
    @BeforeClass
    public void setupGuiceModuleLoader() {
        InjectorConfiguration.setGuiceModuleLoader(new GuiceModuleLoader() {
            @Override
            public Set<Class<? extends Module>> getGuiceModules(Class<? extends Annotation> moduleAnnotation) {
                return Collections.emptySet();
            }
        });
    }

    @BeforeMethod
    public void setupMocks() throws Exception {
        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();

        mailServer = mock(MailServer.class);
        MailServerLoader mailServerLoader = mock(MailServerLoader.class);
        given(mailServerLoader.load(anyString(), anyString())).willReturn(mailServer);

        RestSecurity restSecurity = mock(RestSecurity.class);
        given(restSecurity.isSelfRegistration()).willReturn(true);
        given(restSecurity.isSelfServiceRestEndpointEnabled()).willReturn(true);
        given(restSecurity.getSelfRegTLT()).willReturn(300L);
        given(restSecurity.getSelfRegistrationConfirmationUrl()).willReturn(CONFIRMATION_URL);

        RestSecurityProvider restSecurityProvider = mock(RestSecurityProvider.class);
        given(restSecurityProvider.get(REALM)).willReturn(restSecurity);

        ServiceConfig mailServiceConfig = mailServiceConfig();

        resourceV1 = new IdentityResourceV1(IdentityResourceV1.USER_TYPE, mailServiceConfigManager(mailServiceConfig),
                mailServerLoader, mock(IdentityServicesImpl.class), mock(CoreWrapper.class), restSecurityProvider,
                mock(ConsoleConfigHandler.class), Collections.<UiRolePredicate>emptySet());

        resourceV2 = new IdentityResourceV2(IdentityResourceV2.USER_TYPE, mailServiceConfig, mailServerLoader,
                mock(IdentityServicesImpl.class), mock(CoreWrapper.class), restSecurityProvider,
                mock(ConsoleConfigHandler.class), baseURLProviderFactory(),
                Collections.<UiRolePredicate>emptySet());
    }

    @BeforeMethod(dependsOnMethods = "setupMocks")
    @Override
    public void setupGuiceModules() throws Exception {
        super.setupGuiceModules();
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(CTSPersistentStore.class).toInstance(CTS);
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    /** The two versions of the resource that send the notification themselves. */
    @DataProvider(name = "resourceVersions")
    public Object[][] resourceVersions() {
        return new Object[][] {{1}, {2}};
    }

    /** The wording an administrator configured is what goes out, on a request that asks for nothing else. */
    @Test(dataProvider = "resourceVersions")
    public void shouldSendTheSubjectAndTheMessageOfTheEmailService(int version) throws Exception {
        register(version, json(object(field("email", EMAIL))));

        SentMail sent = sentMail();
        assertThat(sent.subject).isEqualTo(CONFIGURED_SUBJECT);
        assertThat(sent.message).startsWith(CONFIGURED_MESSAGE);
    }

    /**
     * The heart of the advisory: a subject and a message in the request body are ignored, so an anonymous caller
     * cannot word a mail the server sends out under its own From address.
     */
    @Test(dataProvider = "resourceVersions")
    public void shouldIgnoreTheSubjectAndTheMessageOfTheRequest(int version) throws Exception {
        register(version, json(object(field("email", EMAIL),
                                      field("subject", HOSTILE_SUBJECT),
                                      field("message", HOSTILE_MESSAGE))));

        SentMail sent = sentMail();
        assertThat(sent.subject).isEqualTo(CONFIGURED_SUBJECT);
        assertThat(sent.message).startsWith(CONFIGURED_MESSAGE);
        assertThat(sent.message).doesNotContain(HOSTILE_MESSAGE);
    }

    /** Nothing the request carries can reach the Subject header, header breaks included. */
    @Test(dataProvider = "resourceVersions")
    public void shouldNotLetTheRequestPutAHeaderBreakInTheSubject(int version) throws Exception {
        register(version, json(object(field("email", EMAIL), field("subject", HOSTILE_SUBJECT))));

        SentMail sent = sentMail();
        assertThat(sent.subject).doesNotContain("\r").doesNotContain("\n");
        assertThat(sent.subject).doesNotContain("Bcc:");
    }

    /** The confirmation link is the one thing the notification adds to the configured message. */
    @Test(dataProvider = "resourceVersions")
    public void shouldAddTheConfirmationLinkToTheConfiguredMessage(int version) throws Exception {
        register(version, json(object(field("email", EMAIL))));

        assertThat(sentMail().message).contains(CONFIRMATION_URL + "?confirmationId=");
    }

    private void register(int version, JsonValue content) throws ResourceException {
        ActionRequest request = Requests.newActionRequest("users", "register").setContent(content);
        ActionResponse response = resource(version).actionCollection(context(), request).getOrThrowUninterruptibly();

        assertThat(response).isNotNull();
    }

    private CollectionResourceProvider resource(int version) {
        return version == 1 ? resourceV1 : resourceV2;
    }

    /** What the mail server was handed. */
    private SentMail sentMail() throws Exception {
        ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(mailServer).sendEmail(to.capture(), subject.capture(), message.capture());

        return new SentMail(to.getValue(), subject.getValue(), message.getValue());
    }

    private static final class SentMail {
        private final String to;
        private final String subject;
        private final String message;

        private SentMail(String to, String subject, String message) {
            this.to = to;
            this.subject = subject;
            this.message = message;
        }
    }

    private Context context() {
        HttpContext httpContext = new HttpContext(json(object(
                field("headers", Collections.emptyMap()),
                field("parameters", Collections.emptyMap()),
                field("path", REQUEST_PATH))), null);
        return new RealmContext(httpContext, Realm.root());
    }

    private static ServiceConfig mailServiceConfig() {
        Map<String, Set<String>> attributes = new HashMap<>();
        attributes.put(IdentityResourceV1.MAIL_IMPL_CLASS, Collections.singleton(MailServerImpl.class.getName()));
        attributes.put(MailServerImpl.SUBJECT, Collections.singleton(CONFIGURED_SUBJECT));
        attributes.put(MailServerImpl.MESSAGE, Collections.singleton(CONFIGURED_MESSAGE));

        ServiceConfig serviceConfig = mock(ServiceConfig.class);
        given(serviceConfig.getAttributes()).willReturn(attributes);
        return serviceConfig;
    }

    private static ServiceConfigManager mailServiceConfigManager(ServiceConfig serviceConfig)
            throws SMSException, SSOException {
        ServiceConfigManager configManager = mock(ServiceConfigManager.class);
        given(configManager.getOrganizationConfig(eq(REALM), eq(null))).willReturn(serviceConfig);
        return configManager;
    }

    private static BaseURLProviderFactory baseURLProviderFactory() {
        BaseURLProvider baseURLProvider = mock(BaseURLProvider.class);
        given(baseURLProvider.getRootURL(any(HttpContext.class))).willReturn("https://openam.example.com/openam");

        BaseURLProviderFactory factory = mock(BaseURLProviderFactory.class);
        given(factory.get(REALM)).willReturn(baseURLProvider);
        return factory;
    }
}
