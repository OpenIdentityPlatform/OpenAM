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
import static org.assertj.core.api.Assertions.fail;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.core.rest.IdentityRestUtils.AGENT_TYPE;
import static org.forgerock.openam.core.rest.IdentityRestUtils.GROUP_TYPE;
import static org.forgerock.openam.core.rest.IdentityRestUtils.USER_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idsvcs.Attribute;
import com.sun.identity.idsvcs.IdentityDetails;
import com.sun.identity.idsvcs.opensso.IdentityServicesImpl;
import com.sun.identity.shared.encode.Hash;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceNotFoundException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.core.rest.UserAttributeInfo.UserAttributeInfoBuilder;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.forgerockrest.utils.MailServerLoader;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.services.RestSecurity;
import org.forgerock.openam.services.RestSecurityProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.sm.config.ConsoleConfigHandler;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.selfservice.core.SelfServiceContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * Tests the legacy {@literal anonymousCreate} action, which creates the identity with the system admin token and so
 * is the last place able to restrict what an anonymous caller may set.
 */
public class IdentityResourceV2Test {

    private static final String REALM = "/";
    private static final String TOKEN_ID = "a-registration-token";
    private static final String EMAIL = "demo@example.com";
    private static final String AM_ENCRYPTION_PWD = "am.encryption.pwd";

    /** The out of the box value of the selfServiceUserRegistrationValidUserAttributes setting. */
    private static final Set<String> DEFAULT_WHITELIST = new HashSet<>(Arrays.asList(
            "mail", "inetUserStatus", "sn", "username", "userPassword", "kbaInfo", "givenName"));

    @Mock
    private MailServerLoader mailServerLoader;
    @Mock
    private IdentityServicesImpl identityServices;
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private RestSecurityProvider restSecurityProvider;
    @Mock
    private RestSecurity restSecurity;
    @Mock
    private ConsoleConfigHandler configHandler;
    @Mock
    private BaseURLProviderFactory baseURLProviderFactory;
    @Mock
    private CTSPersistentStore cts;
    /** Supplied so the create reaches the identity service, which production reaches under RestUtils.getToken(). */
    @Mock
    private SSOToken adminToken;

    private RealmTestHelper realmTestHelper;
    private Context context;

    /**
     * Before any test in the suite, not only before this class: whichever test first touches InjectorHolder decides
     * the modules for the life of the JVM - see InjectorHolderTestSupport.
     */
    @BeforeSuite(alwaysRun = true)
    public void installNoModuleLoaderBeforeAnyTest() {
        InjectorHolderTestSupport.withoutGuiceModules();
    }

    @BeforeMethod
    public void setUp() throws Exception {
        InjectorHolderTestSupport.withoutGuiceModules();
        MockitoAnnotations.initMocks(this);

        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();
        context = new RealmContext(new RootContext(), Realm.root());

        given(restSecurityProvider.get(REALM)).willReturn(restSecurity);
        given(restSecurity.isSelfRegistration()).willReturn(true);
        given(configHandler.getConfig(eq(REALM), eq(UserAttributeInfoBuilder.class)))
                .willReturn(userAttributeInfo(DEFAULT_WHITELIST));
        given(cts.read(TOKEN_ID)).willReturn(registrationToken());
    }

    @AfterMethod
    public void tearDown() {
        if (realmTestHelper != null) {
            realmTestHelper.tearDownRealmClass();
        }
    }

    /**
     * GHSA-jv8f-m88g-h6mm: anonymousCreate used to hand the whole request body to the identity service under the
     * system admin token, so memberof let a self registering user join an administrator group.
     */
    @Test
    public void shouldRejectGroupMembership() {
        JsonValue content = registrationRequest();
        content.put("memberof", array("cn=admins,ou=groups,dc=openam,dc=example,dc=com"));

        ResourceException e = expectFailure(USER_TYPE, content);

        assertThat(e).isInstanceOf(BadRequestException.class);
        assertThat(e.getMessage()).contains("memberof");
    }

    @Test
    public void shouldRejectAttributeThatIsNotWhitelisted() {
        JsonValue content = registrationRequest();
        content.put("telephonenumber", "555-2368");

        ResourceException e = expectFailure(USER_TYPE, content);

        assertThat(e).isInstanceOf(BadRequestException.class);
        assertThat(e.getMessage()).contains("telephonenumber");
    }

    /**
     * The action is reachable without a session, and a whitelist rejection names the attribute it rejected, so
     * checking the whitelist before the registration token would answer "is this attribute whitelisted?" - a
     * whitelisted one failing on the token, a rejected one saying so - to a caller holding no token at all.
     */
    @Test
    public void shouldNotAnswerWhitelistProbesWithoutAValidToken() throws Exception {
        JsonValue content = registrationRequest();
        content.put("tokenId", "not-a-registration-token");
        content.put("telephonenumber", "555-2368");

        ResourceException e = expectFailure(USER_TYPE, content);

        assertThat(e.getMessage()).doesNotContain("telephonenumber");
        verify(configHandler, never()).getConfig(REALM, UserAttributeInfoBuilder.class);
    }

    /**
     * The identity repository matches attribute names case insensitively, and so does the whitelist - so a "Mail"
     * sent alongside the action's own email parameter would be accepted by the whitelist and land on the account
     * beside, or instead of, the address the confirmation id actually vouches for.
     */
    @Test
    public void shouldNotLetACaseVariantOfMailThroughBesideTheVerifiedAddress() {
        JsonValue content = registrationRequest();
        content.put("Mail", "victim@example.org");

        ActionRequest request = Requests.newActionRequest("users", "anonymousCreate").setContent(content);
        try {
            resource(USER_TYPE).actionCollection(context, request).getOrThrowUninterruptibly();
        } catch (Throwable expected) {
            // The read after the create returns null from the mock, so the action fails; what this pins is the
            // object it was handed.
        }

        assertThat(content.isDefined("Mail")).isFalse();
        assertThat(content.get("mail").asString()).isEqualTo(EMAIL);
    }

    /**
     * The tokenId, confirmationId and email fields are parameters of the action rather than user attributes, so they
     * must not be held against the whitelist - otherwise every legitimate registration would be rejected.
     */
    @Test
    public void shouldNotRejectTheActionParametersThemselves() {
        // The read after the create returns null from the mock, so the action ends in a NotFoundException - what
        // is asserted is that the whitelist ran and did not refuse, not what the request eventually failed on.
        Throwable failure = expectAnyFailure(USER_TYPE, registrationRequest());

        verify(configHandler).getConfig(REALM, UserAttributeInfoBuilder.class);
        assertThat(failure).isNotInstanceOf(BadRequestException.class);
    }

    /**
     * The username is read straight out of the request body by jsonValueToIdentityDetails, which swallows what it
     * cannot convert - leaving the whitelist with no attributes to check on a request no one had to authenticate to
     * make.
     */
    @Test
    public void shouldRejectAUsernameThatIsNotAString() {
        JsonValue content = registrationRequest();
        content.put("username", 42);

        ResourceException e = expectFailure(USER_TYPE, content);

        assertThat(e).isInstanceOf(BadRequestException.class);
        assertThat(e.getMessage()).contains("Invalid user object");
    }

    /**
     * GHSA-jv8f-m88g-h6mm: the self service user registration flow creates the identity here, with the system admin
     * token, so this is the call the advisory actually travels through.
     *
     * Deleting the whitelist call, or moving it back below the admin token, both fail this test - but by
     * IllegalArgumentException out of getCookieFromServerContext rather than by an assertion, since the context
     * carries no SecurityContext and the token acquisition is the next thing the method does.  So what fails is the
     * try/catch below, not the assertions in it; giving the context a SecurityContext would change that failure, and
     * the test has to be checked again by deleting the call if it is ever given one.
     */
    @Test
    public void shouldRejectGroupMembershipOnSelfServiceCreate() {
        CreateRequest request = Requests.newCreateRequest("users", json(object(
                field("username", "demo"),
                field("memberof", array("cn=admins,ou=groups,dc=openam,dc=example,dc=com")))));

        try {
            resource(USER_TYPE).createInstance(new SelfServiceContext(context), request).getOrThrowUninterruptibly();
            fail("Expected the create to be rejected");
        } catch (ResourceException e) {
            assertThat(e).isInstanceOf(BadRequestException.class);
            assertThat(e.getMessage()).contains("memberof");
        }
    }

    /**
     * The whitelist has nothing to say about anything but users, so a self service create of another identity type
     * must not be made to depend on the self service configuration being readable either.  The rule itself lives in
     * enforceWhiteList, but the whitelist is an argument to it - so it is read before that method can return, and
     * the question has to be asked again by the caller for the read to be skipped.
     */
    @Test
    public void shouldNotReadTheWhiteListForANonUserSelfServiceCreate() {
        CreateRequest request = Requests.newCreateRequest("groups", json(object(
                field("username", "admins"),
                field("uniqueMember", "uid=demo,ou=people,dc=openam,dc=example,dc=com"))));

        try {
            resource(GROUP_TYPE).createInstance(new SelfServiceContext(context), request)
                    .getOrThrowUninterruptibly();
        } catch (Throwable expected) {
            // The create cannot get far without a SecurityContext to read a cookie from; what this pins is that
            // the self service configuration was not consulted on the way.
        }

        verify(configHandler, never()).getConfig(REALM, UserAttributeInfoBuilder.class);
    }

    /**
     * The whitelist is the rule for what the self service framework creates on behalf of an anonymous user.  An
     * authenticated create is restricted by the caller's own privileges instead, and must not be held against it -
     * nor made to depend on the self service configuration being readable.
     */
    @Test
    public void shouldNotApplyTheWhiteListOutsideOfASelfServiceContext() {
        CreateRequest request = Requests.newCreateRequest("users", json(object(
                field("username", "demo"),
                field("telephonenumber", "555-2368"))));

        // The create cannot get far without a SecurityContext to read a cookie from, so what this pins is what
        // did not happen on the way: no whitelist rejection, and no read of the self service configuration.
        Throwable failure = null;
        try {
            resource(USER_TYPE).createInstance(context, request).getOrThrowUninterruptibly();
        } catch (Throwable t) {
            failure = t;
        }

        assertThat(failure).isNotInstanceOf(ResourceException.class);
        verify(configHandler, never()).getConfig(REALM, UserAttributeInfoBuilder.class);
    }

    /**
     * The users endpoint is the only one reachable without a session, but the same resource backs the groups and
     * agents endpoints, where an authenticated caller would otherwise have created identities as the admin.
     */
    @Test
    public void shouldRejectIdentityTypesOtherThanUser() {
        for (String objectType : Arrays.asList(GROUP_TYPE, AGENT_TYPE)) {
            ResourceException e = expectFailure(objectType, registrationRequest());

            assertThat(e).as(objectType).isInstanceOf(BadRequestException.class);
            assertThat(e.getMessage()).as(objectType).contains("only supported for users");
        }
    }

    @Test
    public void shouldRejectWhenSelfRegistrationIsDisabled() throws Exception {
        given(restSecurity.isSelfRegistration()).willReturn(false);

        ResourceException e = expectFailure(USER_TYPE, registrationRequest());

        assertThat(e).isInstanceOf(BadRequestException.class);
        assertThat(e.getMessage()).contains("Self-registration disabled");
    }

    /**
     * The action is reachable without a session, and ResourceException reports its cause's message when it has none
     * of its own - so a token store failure constructed from the cause alone would hand the store's text to the
     * caller.
     */
    @Test
    public void shouldNotEchoATokenStoreFailure() throws Exception {
        given(cts.read(TOKEN_ID)).willThrow(new CoreTokenException("token store internals"));

        ResourceException e = expectFailure(USER_TYPE, registrationRequest());

        assertThat(e).isInstanceOf(InternalServerErrorException.class);
        assertThat(e.getMessage()).isEqualTo("Unable to complete the registration request");
    }

    /** As above, for the self registration setting that cannot be read. */
    @Test
    public void shouldNotEchoASelfRegistrationSettingThatCannotBeRead() throws Exception {
        given(restSecurity.isSelfRegistration()).willThrow(new ServiceNotFoundException("service internals"));

        ResourceException e = expectFailure(USER_TYPE, registrationRequest());

        assertThat(e).isInstanceOf(InternalServerErrorException.class);
        assertThat(e.getMessage()).isEqualTo("Unable to complete the registration request");
    }

    /** The action parameters are read with asString(), so one of another type is the caller's mistake, not ours. */
    @Test
    public void shouldAnswerANonStringActionParameterWithABadRequest() {
        JsonValue content = registrationRequest();
        content.put("tokenId", 42);

        ResourceException e = expectFailure(USER_TYPE, content);

        assertThat(e).isInstanceOf(BadRequestException.class);
        assertThat(e.getMessage()).isEqualTo("Invalid registration request");
    }

    /**
     * The whitelist is only authoritative if the object it inspects is the object that gets created.  Nothing in the
     * request travels around it: this captures what actually reaches the identity service and holds every attribute
     * on it against the same whitelist the check used, so a key added to the request after the check - or an
     * exemption that puts one back - is answered here rather than by reading the method.
     */
    @Test
    public void shouldCreateOnlyWhatTheWhiteListChecked() throws Exception {
        JsonValue content = registrationRequest();
        content.put("Mail", "victim@example.org");

        // The read after the create returns null from the mock, so the action ends in a NotFoundException - well
        // past the create, which is what is being captured.
        expectAnyFailure(USER_TYPE, content);

        ArgumentCaptor<IdentityDetails> created = ArgumentCaptor.forClass(IdentityDetails.class);
        verify(identityServices).create(created.capture(), eq(adminToken));

        Set<String> lowerCaseWhiteList = new HashSet<>();
        for (String attribute : DEFAULT_WHITELIST) {
            lowerCaseWhiteList.add(attribute.toLowerCase(Locale.ROOT));
        }
        for (Attribute attribute : created.getValue().getAttributes()) {
            assertThat(lowerCaseWhiteList).contains(attribute.getName().toLowerCase(Locale.ROOT));
        }
        assertThat(created.getValue().getGroupList()).isNull();
        assertThat(mailValues(created.getValue())).containsExactly(EMAIL);
    }

    /**
     * anonymousCreate reaches attemptResourceCreation with a null CreateRequest, so the conflict branch used to
     * dereference null and answer the commonest self service mistake - picking a name that is taken - with a 500.
     */
    @Test
    public void shouldAnswerATakenUsernameWithAConflict() throws Exception {
        doThrow(new ConflictException("demo already exists"))
                .when(identityServices).create(any(IdentityDetails.class), eq(adminToken));

        ResourceException e = expectFailure(USER_TYPE, registrationRequest());

        assertThat(e).isInstanceOf(ConflictException.class);
        assertThat(e.getCode()).isEqualTo(409);
    }

    /** Every value the created identity carries under any spelling of mail. */
    private static Set<String> mailValues(IdentityDetails identity) {
        Set<String> values = new HashSet<>();
        for (Attribute attribute : identity.getAttributes()) {
            if ("mail".equalsIgnoreCase(attribute.getName())) {
                values.addAll(Arrays.asList(attribute.getValues()));
            }
        }
        return values;
    }

    private JsonValue registrationRequest() {
        return json(object(
                field("tokenId", TOKEN_ID),
                field("confirmationId", confirmationId()),
                field("email", EMAIL),
                field("username", "demo"),
                field("userpassword", "s3cr3t")));
    }

    /** The confirmation id createRegistrationEmail hands out, which is what validateToken checks against. */
    private static String confirmationId() {
        return Hash.hash(TOKEN_ID + EMAIL + SystemProperties.get(AM_ENCRYPTION_PWD));
    }

    private static Token registrationToken() {
        Token token = new Token(TOKEN_ID, TokenType.REST);
        token.setAttribute(CoreTokenField.STRING_ONE, REALM);
        Calendar expiry = Calendar.getInstance();
        expiry.setTimeInMillis(expiry.getTimeInMillis() + 60000L);
        token.setExpiryTimestamp(expiry);
        return token;
    }

    private ResourceException expectFailure(String objectType, JsonValue content) {
        Throwable failure = expectAnyFailure(objectType, content);
        assertThat(failure).isInstanceOf(ResourceException.class);
        return (ResourceException) failure;
    }

    private Throwable expectAnyFailure(String objectType, JsonValue content) {
        ActionRequest request = Requests.newActionRequest("users", "anonymousCreate").setContent(content);
        try {
            resource(objectType).actionCollection(context, request).getOrThrowUninterruptibly();
        } catch (Throwable t) {
            return t;
        }
        fail("Expected the action to be rejected");
        return null;
    }

    private IdentityResourceV2 resource(String objectType) {
        return new IdentityResourceV2(objectType, mock(ServiceConfig.class), mailServerLoader,
                identityServices, coreWrapper, restSecurityProvider, configHandler,
                baseURLProviderFactory, Collections.<UiRolePredicate>emptySet(), cts, adminToken);
    }

    private static UserAttributeInfo userAttributeInfo(Set<String> validCreationAttributes) {
        UserAttributeInfoBuilder builder = new UserAttributeInfoBuilder();
        builder.setValidCreationAttributes(validCreationAttributes);
        builder.setProtectedUpdateAttributes(Collections.<String>emptySet());
        return builder.build(Collections.<String, Set<String>>emptyMap());
    }
}
