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
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.forgerock.openam.rest.jakarta.servlet.internal.ServletCall;
import org.forgerock.openam.services.baseurl.BaseURLProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.xui.XUIState;
import org.mockito.ArgumentCaptor;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.CacheDirective;
import org.restlet.data.ClientInfo;
import org.restlet.data.Reference;
import org.restlet.engine.adapter.HttpRequest;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.routing.Router;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests that the OAuth2 device verification endpoint only ever binds a device code to the resource owner's
 * session on a request that proves it came from OpenAM's own user code form, and that the code it binds is the
 * one the user submitted rather than one a link chose for them.
 *
 * @see <a href="https://github.com/OpenIdentityPlatform/OpenAM/security/advisories/GHSA-545c-rm35-2jj2">
 *     GHSA-545c-rm35-2jj2</a>
 */
public class DeviceCodeVerificationResourceCsrfTest {

    private static final String FORM = "templates/CodeVerificationForm.ftl";
    private static final String CONTEXT_PATH = "/openam";
    private static final String ENDPOINT = "http://openam.example.com:8080" + CONTEXT_PATH + "/oauth2/device/user";
    private static final String USER_CODE = "BDCT-HQKD";
    private static final String ATTACKER_USER_CODE = "ZZZZ-ZZZZ";
    private static final String CLIENT_ID = "attacker_client";
    private static final String CSRF_TOKEN = "a-freshly-minted-token";

    private DeviceCodeVerificationResource resource;
    private Request restletRequest;
    private HttpServletRequest servletRequest;
    private BaseURLProvider baseUrlProvider;
    private BaseURLProviderFactory baseURLProviderFactory;
    private OAuth2Request o2request;
    private DeviceCode deviceCode;
    private TokenStore tokenStore;
    private CsrfProtection csrfProtection;
    private OAuth2ProviderSettings providerSettings;
    private ClientRegistration clientRegistration;
    private ResourceOwnerSessionValidator resourceOwnerSessionValidator;
    private AuthorizationService authorizationService;
    private TemplateFactory templateFactory;
    private TemplateRepresentation page;
    private Response response;

    @BeforeMethod
    public void setup() throws Exception {
        // A request that arrived through the servlet container, as every request to this endpoint does
        servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getContextPath()).thenReturn(CONTEXT_PATH);
        ServletCall servletCall = mock(ServletCall.class);
        when(servletCall.getRequest()).thenReturn(servletRequest);
        HttpRequest httpRequest = mock(HttpRequest.class);
        when(httpRequest.getHttpCall()).thenReturn(servletCall);
        restletRequest = httpRequest;
        when(restletRequest.getAttributes()).thenReturn(new ConcurrentHashMap<String, Object>());
        when(restletRequest.getClientInfo()).thenReturn(new ClientInfo());
        givenARequestTo(ENDPOINT);

        o2request = mock(OAuth2Request.class);
        when(o2request.getRequest()).thenReturn(restletRequest);
        when(o2request.<String>getParameter(OAuth2Constants.DeviceCode.USER_CODE)).thenReturn(USER_CODE);
        when(o2request.<String>getParameter(OAuth2Constants.Params.CLIENT_ID)).thenReturn(CLIENT_ID);

        OAuth2RequestFactory requestFactory = mock(OAuth2RequestFactory.class);
        when(requestFactory.create(restletRequest)).thenReturn(o2request);

        deviceCode = newDeviceCode();
        tokenStore = mock(TokenStore.class);
        when(tokenStore.readDeviceCode(USER_CODE, o2request)).thenReturn(deviceCode);

        providerSettings = mock(OAuth2ProviderSettings.class);
        OAuth2ProviderSettingsFactory providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        when(providerSettingsFactory.get(o2request)).thenReturn(providerSettings);

        clientRegistration = mock(ClientRegistration.class);
        ClientRegistrationStore clientRegistrationStore = mock(ClientRegistrationStore.class);
        when(clientRegistrationStore.get(CLIENT_ID, o2request)).thenReturn(clientRegistration);

        resourceOwnerSessionValidator = mock(ResourceOwnerSessionValidator.class);
        ResourceOwner resourceOwner = mock(ResourceOwner.class);
        when(resourceOwner.getId()).thenReturn("victim");
        when(resourceOwnerSessionValidator.validate(o2request)).thenReturn(resourceOwner);

        csrfProtection = mock(CsrfProtection.class);
        when(csrfProtection.createCsrfToken(o2request)).thenReturn(CSRF_TOKEN);

        authorizationService = mock(AuthorizationService.class);

        OAuth2Utils oAuth2Utils = mock(OAuth2Utils.class);
        when(oAuth2Utils.join(any(Iterable.class), anyString())).thenReturn("");

        baseUrlProvider = mock(BaseURLProvider.class);
        when(baseUrlProvider.getRootURL(nullable(HttpServletRequest.class)))
                .thenReturn("http://openam.example.com:8080/openam");
        baseURLProviderFactory = mock(BaseURLProviderFactory.class);
        when(baseURLProviderFactory.get(any())).thenReturn(baseUrlProvider);

        resource = new DeviceCodeVerificationResource(mock(XUIState.class), mock(Router.class),
                baseURLProviderFactory, mock(OAuth2Representation.class), tokenStore, requestFactory,
                authorizationService, providerSettingsFactory, mock(ExceptionHandler.class),
                resourceOwnerSessionValidator, clientRegistrationStore, oAuth2Utils, csrfProtection);
        resource = spy(resource);
        doReturn(restletRequest).when(resource).getRequest();
        response = new Response(restletRequest);
        doReturn(response).when(resource).getResponse();

        // The endpoint reaches its template factory through the Restlet context, so putting one there is
        // enough: the data model it renders with is the one the production code builds, not a stub.
        page = mock(TemplateRepresentation.class);
        templateFactory = mock(TemplateFactory.class);
        when(templateFactory.getTemplateRepresentation(anyString())).thenReturn(page);
        Context context = new Context();
        context.getAttributes().put(TemplateFactory.class.getName(), templateFactory);
        doReturn(context).when(resource).getContext();
    }

    /** Points both views of the request - Restlet's and the servlet container's - at the same address. */
    private void givenARequestTo(String url) {
        final Reference reference = new Reference(url);
        when(restletRequest.getResourceRef()).thenReturn(reference);
        when(servletRequest.getRequestURI()).thenReturn(reference.getPath());
    }

    private static DeviceCode newDeviceCode() {
        return new DeviceCode("DEVICE-CODE", USER_CODE, null, CLIENT_ID, null, "device_code", null, null, null,
                null, null, null, null, System.currentTimeMillis() + 60000L,
                Collections.singleton("openid"), "/", null, null, "audit-tracking-id");
    }

    /** Realm allows implied consent and the client asks for it, so the skip-consent branch is taken. */
    private void givenConsentIsImplied() throws Exception {
        when(providerSettings.clientsCanSkipConsent()).thenReturn(true);
        when(clientRegistration.isConsentImplied()).thenReturn(true);
    }

    /** Consent is required, so the endpoint renders and then processes a consent decision. */
    private void givenConsentIsRequired() throws Exception {
        when(providerSettings.clientsCanSkipConsent()).thenReturn(false);
        when(clientRegistration.isConsentImplied()).thenReturn(false);
    }

    private void assertDeviceCodeNotAuthorized() throws Exception {
        assertThat(deviceCode.isAuthorized()).isFalse();
        assertThat(deviceCode.getResourceOwnerId()).isNull();
        verify(tokenStore, never()).updateDeviceCode(any(DeviceCode.class), any(OAuth2Request.class));
    }

    @Test
    public void shouldRejectImpliedConsentSubmissionWithoutValidCsrfToken() throws Exception {
        //given
        givenConsentIsImplied();
        when(csrfProtection.isCsrfAttack(o2request)).thenReturn(true);

        //when
        try {
            resource.verify(null);
            fail("expected the submission to be rejected");
        } catch (OAuth2RestletException e) {
            //then
            assertThat(e.getStatus().getCode()).isEqualTo(400);
            assertThat(e.getError()).isEqualTo("bad_request");
        }
        assertDeviceCodeNotAuthorized();
    }

    @Test
    public void shouldAuthorizeImpliedConsentSubmissionWithValidCsrfToken() throws Exception {
        //given
        givenConsentIsImplied();
        when(csrfProtection.isCsrfAttack(o2request)).thenReturn(false);

        //when
        resource.verify(null);

        //then
        assertThat(deviceCode.isAuthorized()).isTrue();
        assertThat(deviceCode.getResourceOwnerId()).isEqualTo("victim");
        verify(tokenStore).updateDeviceCode(deviceCode, o2request);
    }

    /**
     * The user code form has no action, so it posts back to the URL it was served from. A submission that
     * started as a navigation to {@code ?user_code=<attacker's>} therefore carries the attacker's code in the
     * query string and the user's own code in the body, and the query string is what
     * {@link OAuth2Request#getParameter} would return. The user would be confirming a code they never saw.
     */
    @Test
    public void shouldRejectSubmissionWhoseQueryAndFormCarryDifferentUserCodes() throws Exception {
        //given
        givenConsentIsImplied();
        when(csrfProtection.isCsrfAttack(o2request)).thenReturn(false);
        when(o2request.getQueryParameter(OAuth2Constants.DeviceCode.USER_CODE)).thenReturn(ATTACKER_USER_CODE);
        when(o2request.getBodyParameter(OAuth2Constants.DeviceCode.USER_CODE)).thenReturn(USER_CODE);
        when(o2request.<String>getParameter(OAuth2Constants.DeviceCode.USER_CODE)).thenReturn(ATTACKER_USER_CODE);

        //when
        try {
            resource.verify(null);
            fail("expected the submission to be rejected");
        } catch (OAuth2RestletException e) {
            //then
            assertThat(e.getStatus().getCode()).isEqualTo(400);
            assertThat(e.getError()).isEqualTo("invalid_request");
        }
        verify(tokenStore, never()).readDeviceCode(anyString(), any(OAuth2Request.class));
        assertDeviceCodeNotAuthorized();
    }

    /**
     * The form field is only {@code required} on the client side, so a submission can carry an empty
     * {@code user_code}. Treating that as "the form said nothing" would hand the decision straight back to the
     * attacker's query string.
     */
    @Test
    public void shouldRejectSubmissionWhoseFormClearsTheUserCodeCarriedByTheLink() throws Exception {
        //given
        givenConsentIsImplied();
        when(csrfProtection.isCsrfAttack(o2request)).thenReturn(false);
        when(o2request.getQueryParameter(OAuth2Constants.DeviceCode.USER_CODE)).thenReturn(ATTACKER_USER_CODE);
        when(o2request.getBodyParameter(OAuth2Constants.DeviceCode.USER_CODE)).thenReturn("");
        when(o2request.<String>getParameter(OAuth2Constants.DeviceCode.USER_CODE)).thenReturn(ATTACKER_USER_CODE);

        //when
        try {
            resource.verify(null);
            fail("expected the submission to be rejected");
        } catch (OAuth2RestletException e) {
            //then
            assertThat(e.getStatus().getCode()).isEqualTo(400);
            assertThat(e.getError()).isEqualTo("invalid_request");
        }
        verify(tokenStore, never()).readDeviceCode(anyString(), any(OAuth2Request.class));
        assertDeviceCodeNotAuthorized();
    }

    /**
     * The CSRF check runs before the device code is looked up, so a caller without a token cannot tell a live
     * user code from an unknown one. RFC 8628 section 5.2 calls out that enumeration risk.
     */
    @Test
    public void shouldNotLookUpTheUserCodeWithoutAValidCsrfToken() throws Exception {
        //given
        givenConsentIsImplied();
        when(csrfProtection.isCsrfAttack(o2request)).thenReturn(true);

        //when
        try {
            resource.verify(null);
            fail("expected the submission to be rejected");
        } catch (OAuth2RestletException e) {
            //then the answer is the same whether or not the code exists
            assertThat(e.getStatus().getCode()).isEqualTo(400);
        }
        verify(tokenStore, never()).readDeviceCode(anyString(), any(OAuth2Request.class));
    }

    /**
     * The branch that renders the consent page had no CSRF check of its own - the gap this advisory came from
     * was a guard living inside one branch instead of ahead of all of them.
     */
    @Test
    public void shouldRejectAConsentPageRequestWithoutValidCsrfToken() throws Exception {
        //given a submission with no decision yet, so the consent page would be rendered
        givenConsentIsRequired();
        when(csrfProtection.isCsrfAttack(o2request)).thenReturn(true);

        //when
        try {
            resource.verify(null);
            fail("expected the request to be rejected");
        } catch (OAuth2RestletException e) {
            //then
            assertThat(e.getStatus().getCode()).isEqualTo(400);
            assertThat(e.getError()).isEqualTo("bad_request");
        }
        verify(authorizationService, never()).authorize(any(OAuth2Request.class));
        assertDeviceCodeNotAuthorized();
    }

    /** The documented direct POST repeats the same code in the query string and the body. */
    @Test
    public void shouldAuthorizeSubmissionRepeatingTheSameUserCodeInQueryAndForm() throws Exception {
        //given
        givenConsentIsImplied();
        when(csrfProtection.isCsrfAttack(o2request)).thenReturn(false);
        when(o2request.getQueryParameter(OAuth2Constants.DeviceCode.USER_CODE)).thenReturn(USER_CODE);
        when(o2request.getBodyParameter(OAuth2Constants.DeviceCode.USER_CODE)).thenReturn(USER_CODE);

        //when
        resource.verify(null);

        //then
        assertThat(deviceCode.isAuthorized()).isTrue();
        verify(tokenStore).updateDeviceCode(deviceCode, o2request);
    }

    @Test
    public void shouldNotAuthorizeImpliedConsentDeviceCodeOnGet() throws Exception {
        //given a cross-site link carrying the attacker's user code, followed by the victim's browser
        givenConsentIsImplied();
        when(csrfProtection.isCsrfAttack(o2request)).thenReturn(false);

        //when
        Representation result = resource.userCodeForm();

        //then the victim gets the user code form back, and the endpoint does not even look the code up
        assertThat(result).isSameAs(page);
        verify(tokenStore, never()).readDeviceCode(anyString(), any(OAuth2Request.class));
        verify(authorizationService, never()).authorize(any(OAuth2Request.class));
        assertDeviceCodeNotAuthorized();
    }

    /**
     * Filling the code in for the user would leave the attacker's link one click from success, on a page that
     * names neither the client nor the scopes. Nothing in the product hands a browser such a link either -
     * {@code /oauth2/device/code} answers with a bare verification URI.
     */
    @Test
    public void shouldNotFillTheUserCodeFromTheRequestIntoTheForm() throws Exception {
        //given a request carrying a user code
        givenConsentIsImplied();
        when(o2request.getQueryParameter(OAuth2Constants.DeviceCode.USER_CODE)).thenReturn(ATTACKER_USER_CODE);

        //when
        resource.userCodeForm();

        //then the rendered form is empty
        assertThat(renderedFormDataModel()).doesNotContainKey("userCode")
                .doesNotContainValue(ATTACKER_USER_CODE).doesNotContainValue(USER_CODE);
    }

    @Test
    public void shouldMintCsrfTokenForTheUserCodeForm() throws Exception {
        //when
        resource.userCodeForm();

        //then the token the submission has to carry reaches the page
        assertThat(renderedFormDataModel()).containsEntry("csrf", CSRF_TOKEN);
        verify(authorizationService, never()).authorize(any(OAuth2Request.class));
    }

    /**
     * The error render goes through the same template as the empty form, so it has to carry a token too:
     * which of the two a theme shows is not this endpoint's business, and a rendered form whose token is
     * empty is one whose every submission is refused.
     */
    @Test
    public void shouldMintCsrfTokenForTheUserCodeFormShownWithAnError() throws Exception {
        //given a submission naming a user code that no longer exists
        givenConsentIsImplied();
        when(csrfProtection.isCsrfAttack(o2request)).thenReturn(false);
        when(tokenStore.readDeviceCode(USER_CODE, o2request)).thenReturn(null);

        //when
        resource.verify(null);

        //then
        assertThat(renderedFormDataModel()).containsEntry("csrf", CSRF_TOKEN)
                .containsEntry("errorCode", "not_found");
    }

    /**
     * The form has no action, so it posts back to the address it was served from. A user code left in that
     * address would be submitted alongside the one the user types, which
     * {@link DeviceCodeVerificationResource#getSubmittedUserCode} refuses - a bare 400 with no form to retry
     * on for anyone who followed such a link. The code is redirected out of the address instead.
     */
    @Test
    public void shouldRedirectAGetCarryingAUserCodeToTheSameAddressWithoutIt() throws Exception {
        //given a navigation to the endpoint carrying a user code, alongside a parameter that matters
        givenConsentIsImplied();
        givenARequestTo(ENDPOINT + "?user_code=" + ATTACKER_USER_CODE + "&realm=%2Fsubrealm");

        //when
        Representation result = resource.userCodeForm();

        //then the browser is sent to the same address without the code, and nothing is rendered yet
        assertThat(result).isNull();
        assertThat(response.getStatus().getCode()).isEqualTo(303);
        Reference location = response.getLocationRef();
        // Absolute, and on the host the request already reached: Restlet writes the Location header through
        // getTargetRef(), which refuses a relative reference that has no base to resolve against.
        assertThat(location.isAbsolute()).isTrue();
        assertThat(location.getHostDomain()).isEqualTo("openam.example.com");
        assertThat(location.getPath()).isEqualTo("/openam/oauth2/device/user");
        assertThat(location.getQueryAsForm().getFirstValue(OAuth2Constants.DeviceCode.USER_CODE)).isNull();
        assertThat(location.getQueryAsForm().getFirstValue("realm")).isEqualTo("/subrealm");
        verify(templateFactory, never()).getTemplateRepresentation(anyString());
        verify(tokenStore, never()).readDeviceCode(anyString(), any(OAuth2Request.class));
        assertDeviceCodeNotAuthorized();
    }

    /**
     * Only the path below the context path is taken from the request, so a path that would read as another
     * host if it were resolved on its own cannot make the redirect point anywhere else.
     */
    @Test
    public void shouldRedirectWithinTheConfiguredHost() throws Exception {
        //given a path below the context path that begins with the doubled slash that parses as an authority
        givenConsentIsImplied();
        givenARequestTo("http://openam.example.com:8080" + CONTEXT_PATH
                + "//evil.example.net/oauth2/device/user?user_code=" + ATTACKER_USER_CODE);

        //when
        resource.userCodeForm();

        //then
        Reference location = response.getLocationRef();
        assertThat(location.getHostDomain()).isEqualTo("openam.example.com");
        assertThat(location.getQueryAsForm().getFirstValue(OAuth2Constants.DeviceCode.USER_CODE)).isNull();
    }

    /**
     * Behind a proxy that terminates TLS without telling the connector, the container sees a plain-http
     * address on its own host. The redirect follows the realm's Base URL Source instead, as the page's own
     * {@code baseUrl} and the verification URI the device was given do - otherwise a link that used to render
     * the form would send the browser somewhere it cannot reach. The Base URL Source is the one of the
     * realm the request names, not the root realm.
     */
    @Test
    public void shouldRedirectToThePublicAddressTheBaseUrlSourceConfigures() throws Exception {
        //given a container that sees http on its own host, and a Base URL Source naming the public address
        givenConsentIsImplied();
        when(baseUrlProvider.getRootURL(servletRequest)).thenReturn("https://idp.example.org/sso");
        when(o2request.<String>getParameter(OAuth2Constants.Params.REALM)).thenReturn("/subrealm");
        givenARequestTo(ENDPOINT + "?user_code=" + ATTACKER_USER_CODE + "&realm=%2Fsubrealm");

        //when
        resource.userCodeForm();

        //then
        assertThat(response.getStatus().getCode()).isEqualTo(303);
        Reference location = response.getLocationRef();
        assertThat(location.getScheme()).isEqualTo("https");
        assertThat(location.getHostDomain()).isEqualTo("idp.example.org");
        assertThat(location.getHostPort()).isEqualTo(-1);
        assertThat(location.getPath()).isEqualTo("/sso/oauth2/device/user");
        assertThat(location.getQueryAsForm().getFirstValue(OAuth2Constants.DeviceCode.USER_CODE)).isNull();
        assertThat(location.getQueryAsForm().getFirstValue("realm")).isEqualTo("/subrealm");
        // The realm that names the Base URL Source is the one the request names, not the root - a
        // subrealm's redirect must not be built from the root realm's configured public address.
        verify(baseURLProviderFactory).get("/subrealm");
    }

    /** The address the redirect above leads to: no code in the query string, so the form is served. */
    @Test
    public void shouldRenderTheFormForAGetCarryingNoUserCode() throws Exception {
        //given
        givenConsentIsImplied();
        givenARequestTo(ENDPOINT + "?realm=%2Fsubrealm");

        //when
        Representation result = resource.userCodeForm();

        //then
        assertThat(result).isSameAs(page);
        assertThat(response.getStatus().isRedirection()).isFalse();
        assertThat(renderedFormDataModel()).containsEntry("csrf", CSRF_TOKEN);
    }

    @Test
    public void shouldRejectConsentDecisionWithoutValidCsrfToken() throws Exception {
        //given
        givenConsentIsRequired();
        when(o2request.getBodyParameter("decision")).thenReturn("allow");
        when(csrfProtection.isCsrfAttack(o2request)).thenReturn(true);

        //when
        try {
            resource.verify(null);
            fail("expected the consent decision to be rejected");
        } catch (OAuth2RestletException e) {
            //then
            assertThat(e.getStatus().getCode()).isEqualTo(400);
            assertThat(e.getError()).isEqualTo("bad_request");
        }
        assertDeviceCodeNotAuthorized();
    }

    @Test
    public void shouldAuthorizeConsentDecisionWithValidCsrfToken() throws Exception {
        //given
        givenConsentIsRequired();
        when(o2request.getBodyParameter("decision")).thenReturn("allow");
        when(csrfProtection.isCsrfAttack(o2request)).thenReturn(false);

        //when
        resource.verify(null);

        //then
        assertThat(deviceCode.isAuthorized()).isTrue();
        assertThat(deviceCode.getResourceOwnerId()).isEqualTo("victim");
        verify(tokenStore).updateDeviceCode(deviceCode, o2request);
    }

    /**
     * The user code form carries no decision field, so a link that put one in the query string would answer
     * the consent question with nothing in the user's own submission to contradict it: they would grant a
     * client and scopes they were never shown, and {@code save_consent} would record it permanently.
     */
    @Test
    public void shouldNotTakeAConsentDecisionFromTheQueryString() throws Exception {
        //given a link carrying a decision, and a form submission that carries none
        givenConsentIsRequired();
        when(csrfProtection.isCsrfAttack(o2request)).thenReturn(false);
        when(o2request.<String>getParameter("decision")).thenReturn("allow");
        when(o2request.<String>getParameter("save_consent")).thenReturn("on");

        //when
        resource.verify(null);

        //then the endpoint asks for consent instead of recording it
        verify(authorizationService).authorize(o2request);
        // any() rather than anyString(): the client id is unstubbed, and anyString() does not match its null
        verify(providerSettings, never()).saveConsent(any(), any(), any());
        assertDeviceCodeNotAuthorized();
    }

    /**
     * The consent page's own submission carries the decision, but whether to remember it has to come from the
     * same place - a link whose query string says {@code save_consent=on} must not turn a one-off "Allow"
     * into a grant recorded permanently.
     */
    @Test
    public void shouldNotSaveConsentFromTheQueryString() throws Exception {
        //given the resource owner clicked "Allow" without ticking "save", on a page whose URL says save_consent=on
        givenConsentIsRequired();
        when(o2request.getBodyParameter("decision")).thenReturn("allow");
        when(o2request.<String>getParameter("save_consent")).thenReturn("on");
        when(csrfProtection.isCsrfAttack(o2request)).thenReturn(false);

        //when
        resource.verify(null);

        //then the device code is authorized, and nothing is remembered
        assertThat(deviceCode.isAuthorized()).isTrue();
        verify(providerSettings, never()).saveConsent(any(), any(), any());
    }

    /**
     * The other half of the {@code save_consent} pair: a body {@code save_consent=on} alongside a body
     * {@code decision=allow} must record the grant. Without this, the mutant that reads
     * {@code save_consent} as always false stays green - the only guard that fires is the refusal one.
     */
    @Test
    public void shouldSaveConsentFromTheBody() throws Exception {
        //given the resource owner clicked "Allow" with "save" ticked
        givenConsentIsRequired();
        when(o2request.getBodyParameter("decision")).thenReturn("allow");
        when(o2request.getBodyParameter("save_consent")).thenReturn("on");
        when(csrfProtection.isCsrfAttack(o2request)).thenReturn(false);

        //when
        resource.verify(null);

        //then
        assertThat(deviceCode.isAuthorized()).isTrue();
        // any() rather than anyString(): the client id is unstubbed, so the real call passes null
        verify(providerSettings).saveConsent(any(), any(), any());
    }

    /**
     * A user code resolved to nothing at all would reach {@code readDeviceCode} as a null, which builds a CTS
     * equality filter that rejects a null assertion value with an unchecked exception - a 500 rather than an
     * answer.
     */
    @Test
    public void shouldAnswerTheFormWhenTheSubmissionNamesNoUserCode() throws Exception {
        //given
        givenConsentIsImplied();
        when(csrfProtection.isCsrfAttack(o2request)).thenReturn(false);
        when(o2request.<String>getParameter(OAuth2Constants.DeviceCode.USER_CODE)).thenReturn(null);

        //when
        Representation result = resource.verify(null);

        //then
        assertThat(result).isSameAs(page);
        verify(tokenStore, never()).readDeviceCode(nullable(String.class), any(OAuth2Request.class));
        assertDeviceCodeNotAuthorized();
    }

    /**
     * The form carries a freshly minted CSRF token, so no intermediary may hand it to a second visitor.
     * {@code OAuth2Filter} sets this header for the authorize and token endpoints; this route is not behind
     * it.
     */
    @Test
    public void shouldPreventCachingOfTheUserCodeForm() throws Exception {
        //when
        resource.userCodeForm();

        //then
        assertThat(response.getCacheDirectives()).contains(CacheDirective.noStore());
    }

    /**
     * Set before the request is branched on, so it covers every answer the POST can produce - the thanks
     * page, the "not found" form, the 400s, and the consent page, which is built by
     * {@link ConsentRequiredResource#getDataModel} and carries a CSRF token of its own but is returned
     * through {@code OAuth2Representation}, which sets no cache directive.
     */
    @Test
    public void shouldPreventCachingOfEveryAnswerToASubmission() throws Exception {
        //given
        givenConsentIsImplied();
        when(csrfProtection.isCsrfAttack(o2request)).thenReturn(true);

        //when
        try {
            resource.verify(null);
            fail("expected the submission to be rejected");
        } catch (OAuth2RestletException e) {
            //then the directive is already on the response the error is rendered into
            assertThat(response.getCacheDirectives()).contains(CacheDirective.noStore());
        }
    }

    /** The data model the rendered user code form was actually given. */
    @SuppressWarnings("unchecked")
    private Map<String, String> renderedFormDataModel() {
        verify(templateFactory).getTemplateRepresentation(FORM);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(page).setDataModel(captor.capture());
        return (Map<String, String>) captor.getValue();
    }
}
