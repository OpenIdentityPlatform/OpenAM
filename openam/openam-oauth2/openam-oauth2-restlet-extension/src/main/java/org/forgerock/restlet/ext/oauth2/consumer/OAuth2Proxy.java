/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of company]"
 */

package org.forgerock.restlet.ext.oauth2.consumer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.forgerock.openam.oauth2.model.CoreToken;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.restlet.ext.oauth2.consumer.RequestFactory.AuthorizationCodeRequest;
import org.forgerock.restlet.ext.oauth2.consumer.RequestFactory.AuthorizationTokenRequest;
import org.forgerock.restlet.ext.oauth2.consumer.RequestFactory.ClientCredentialsRequest;
import org.forgerock.restlet.ext.oauth2.consumer.RequestFactory.ImplicitRequest;
import org.forgerock.restlet.ext.oauth2.consumer.RequestFactory.PasswordRequest;
import org.forgerock.restlet.ext.oauth2.consumer.RequestFactory.RefreshTokenRequest;
import org.forgerock.restlet.ext.oauth2.consumer.RequestFactory.SAML20AssertionRequest;
import org.forgerock.restlet.ext.oauth2.consumer.RequestFactory.TokenRequestFactory;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.Uniform;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.engine.util.Base64;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Redirector;
import org.restlet.util.Series;

/**
 * Triggers the OAuth2 Flows for the demo
 *
 */
public abstract class OAuth2Proxy<T extends AccessTokenExtractor<U>, U extends CoreToken>
        extends Restlet {

    private Reference authorizationEndpoint = null;
    private Reference tokenEndpoint = null;
    private Reference redirectionEndpoint = null;
    private ChallengeResponse challengeResponse = null;
    private String client_id = null;
    private String client_secret = null;
    private String username = null;
    private String password = null;

    /**
     * The OAuth2 Request handler.
     */
    private volatile Uniform oAuth2Client;

    protected OAuth2Proxy(Uniform oAuth2Client) {
        this.oAuth2Proxy = null;
        this.oAuth2Client = oAuth2Client;
        this.flow = null;
    }

    protected OAuth2Proxy(Context context, Uniform oAuth2Client) {
        super(context);
        this.oAuth2Client = oAuth2Client;
        this.oAuth2Proxy = null;
        this.flow = null;
    }

    /**
     * @param authorizationEndpoint
     * @return
     * @see <a href=
     *      "http://tools.ietf.org/html/draft-ietf-oauth-v2-25#section-3.1>Authorizati
     *      o n Endpoint</a>
     */
    public OAuth2Proxy setAuthorizationEndpoint(Reference authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
        return this;
    }

    public Reference getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    /**
     * @param tokenEndpoint
     * @return
     * @see <a href=
     *      "http://tools.ietf.org/html/draft-ietf-oauth-v2-25#section-3.2>Token
     *      Endpoint</a>
     */
    public OAuth2Proxy setTokenEndpoint(Reference tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
        return this;
    }

    public Reference getTokenEndpoint() {
        return tokenEndpoint;
    }

    protected Protocol getProtocol() {
        Reference ref = getTokenEndpoint();
        if (ref == null) {
            ref = getAuthorizationEndpoint();
        }
        if (ref == null || ref.getSchemeProtocol() == null) {
            return Protocol.HTTPS;
        } else {
            return ref.getSchemeProtocol();
        }
    }

    /**
     * @return
     * @see <a href=
     *      "http://tools.ietf.org/html/draft-ietf-oauth-v2-25#section-3.1.2>Redirecti
     *      o n Endpoint</a>
     */
    public OAuth2Proxy setRedirectionEndpoint(Reference redirectionEndpoint) {
        this.redirectionEndpoint = redirectionEndpoint;
        return this;
    }

    public Reference getRedirectionEndpoint() {
        return new Reference(redirectionEndpoint);
    }

    public OAuth2Proxy setChallengeResponse(ChallengeResponse challengeResponse) {
        this.challengeResponse = challengeResponse;
        return this;
    }

    public ChallengeResponse getChallengeResponse() {
        return challengeResponse;
    }

    public OAuth2Proxy setClientCredentials(String client_id, String client_secret) {
        if (OAuth2Utils.isNotBlank(client_id) && OAuth2Utils.isNotBlank(client_secret)) {
            this.client_id = client_id;
            this.client_secret = client_secret;
        } else {
            this.client_id = null;
            this.client_secret = null;
        }
        return this;
    }

    public String getClientId() {
        return client_id;
    }

    public String getClientSecret() {
        return client_secret;
    }

    public OAuth2Proxy setResourceOwnerCredentials(String username, String password) {
        if (OAuth2Utils.isNotBlank(username) && OAuth2Utils.isNotBlank(password)) {
            this.username = username;
            this.password = password;
        } else {
            this.username = null;
            this.password = null;
        }
        return this;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    protected OAuth2Proxy.Flow getAuthenticationFlow() {
        return flow;
    }

    public abstract T getAccessTokenExtractor();

    public Uniform getClientResource() {

        Uniform result = this.oAuth2Client;

        if (result == null) {
            synchronized (this) {
                result = this.oAuth2Client;
                if (result == null) {
                    result = createRestletClient();

                    if (result != null) {
                        setClientResource(result);
                    }
                }
            }
        }

        return result;
    }

    public void setClientResource(Uniform oauth2Client) {
        this.oAuth2Client = oauth2Client;
    }

    /**
     * Creates a next Restlet is no one is set. By default, it creates a new
     * {@link Client} based on the protocol of the resource's URI reference.
     * 
     * @return The created next Restlet or null.
     */
    protected Uniform createRestletClient() {

        Uniform result = null;

        // Prefer the outbound root
        result = getApplication().getOutboundRoot();

        if ((result == null) && (getContext() != null)) {
            // Try using directly the client dispatcher
            result = getContext().getClientDispatcher();
        }

        if (result == null) {
            // As a final option, try creating a client connector
            Protocol p = getProtocol();

            result = new Client(getProtocol());
            if (Protocol.HTTPS.equals(p)) {
                // SslContextFactory factory = new
                // CustomSSLContextFactory(keyManagers, trustManagers,
                // configuration.getClientKeyAlias());
                // client.getContext().getAttributes().put("sslContextFactory",
                // factory);
            }
        }
        return result;
    }

    // Authorization Redirects

    public Redirector handleAuthorizationCodeRequest(Request request, Series<Parameter> parameters) {
        return handleAuthorizationCodeRequest(request.getResourceRef().toString(), null, Base64
                .encode(request.getResourceRef().normalize().toString().toCharArray(),
                        "ISO-8859-1", false), parameters);
    }

    public Redirector handleAuthorizationCodeRequest(String redirectUri, Collection<String> scope,
            String state, Series<Parameter> parameters) {
        AuthorizationCodeRequest factory =
                getAuthorizationCodeRequest().setRedirectUri(redirectUri)
                        .setClientId(getClientId()).setState(state).addParameters(parameters);
        if (null != scope) {
            factory.getScope().addAll(scope);
        }
        Reference reference = factory.build();
        return new Redirector(getContext(), reference.toString(), Redirector.MODE_CLIENT_FOUND);

    }

    public Redirector handleImplicitRequest(Request request, Series<Parameter> parameters) {
        return handleImplicitRequest(request.getResourceRef().toString(), null,
                Base64.encode(request.getResourceRef().normalize().toString().toCharArray(),
                        "ISO-8859-1", false), parameters);
    }

    public Redirector handleImplicitRequest(String redirectUri, Collection<String> scope,
            String state, Series<Parameter> parameters) {
        ImplicitRequest factory =
                getImplicitRequest().setRedirectUri(redirectUri).setClientId(getClientId())
                        .setState(state).addParameters(parameters);
        if (null != scope) {
            factory.getScope().addAll(scope);
        }
        Reference reference = factory.build();
        return new Redirector(getContext(), reference.toString(), Redirector.MODE_CLIENT_FOUND);
    }

    // Token Requests

    /**
     * @param code
     * @return
     * @throws ResourceException
     * @throws OAuthProblemException
     */
    public U flowAuthorizationToken(String code, Series<Parameter> parameters) {
        Request request =
                getAuthorizationTokenRequest().setCode(code).addParameters(parameters).build();
        Response response = new Response(request);
        Uniform client = getClientResource();
        client.handle(request, response);
        return getAccessTokenExtractor().extractToken(OAuth2Utils.ParameterLocation.HTTP_BODY,
                response);
    }

    /**
     * @param code
     * @return
     * @throws ResourceException
     * @throws OAuthProblemException
     */
    public U flowAuthorizationToken(String code) {
        return flowAuthorizationToken(code, null);
    }

    public U flowPassword(Collection<String> scope, Series<Parameter> parameters) {
        PasswordRequest factory = getPasswordRequest();
        return executeTokenFlow(scope, parameters, factory);
    }

    public U flowPassword(String... scope) {
        return flowPassword(null != scope ? Arrays.asList(scope) : null, null);
    }

    public U flowClientCredentials(Collection<String> scope, Series<Parameter> parameters) {
        ClientCredentialsRequest factory = getClientCredentialsRequest();
        return executeTokenFlow(scope, parameters, factory);
    }

    public U flowClientCredentials(String... scope) {
        return flowClientCredentials(null != scope ? Arrays.asList(scope) : null, null);
    }

    public U flowRefreshToken(String refreshToken, Collection<String> scope,
            Series<Parameter> parameters) {
        RefreshTokenRequest factory = getRefreshTokenRequest(refreshToken);
        return executeTokenFlow(scope, parameters, factory);
    }

    public U flowRefreshToken(String refreshToken, String... scope) {
        return flowRefreshToken(refreshToken, null != scope ? Arrays.asList(scope) : null, null);
    }

    protected U executeTokenFlow(Collection<String> scope, Series<Parameter> parameters,
            TokenRequestFactory factory) {
        if (null != parameters) {
            factory.getParameters().addAll(parameters);
        }
        setScope(factory, scope);
        Request request = factory.build();
        Response response = new Response(request);
        getClientResource().handle(request, response);
        return getAccessTokenExtractor().extractToken(OAuth2Utils.ParameterLocation.HTTP_BODY,
                response);
    }

    protected void setScope(RequestFactory factory, Collection<String> scopes) {
        if (null != scopes) {
            for (String scope : scopes) {
                factory.getScope().addAll(scopes);
            }
        }
    }

    // Request Factories

    public AuthorizationCodeRequest getAuthorizationCodeRequest() {
        return RequestFactory.newInstance(AuthorizationCodeRequest.class,
                getAuthorizationEndpoint());
    }

    public ImplicitRequest getImplicitRequest() {
        return RequestFactory.newInstance(ImplicitRequest.class, getAuthorizationEndpoint());
    }

    public AuthorizationTokenRequest getAuthorizationTokenRequest() {
        return RequestFactory.newInstance(AuthorizationTokenRequest.class, getTokenEndpoint())
                .setRedirectUri(getRedirectionEndpoint().toString()).setChallengeResponse(
                        getChallengeResponse()).setClientCredentials(getClientId(),
                        getClientSecret());
    }

    public PasswordRequest getPasswordRequest() {
        return RequestFactory.newInstance(PasswordRequest.class, getTokenEndpoint())
                .setChallengeResponse(getChallengeResponse()).setClientCredentials(getClientId(),
                        getClientSecret())
                .setResourceOwnerCredentials(getUsername(), getPassword());
    }

    public ClientCredentialsRequest getClientCredentialsRequest() {
        return RequestFactory.newInstance(ClientCredentialsRequest.class, getTokenEndpoint())
                .setChallengeResponse(getChallengeResponse()).setClientCredentials(getClientId(),
                        getClientSecret());
    }

    public RefreshTokenRequest getRefreshTokenRequest(String refreshToken) {
        return RequestFactory.newInstance(RefreshTokenRequest.class, getTokenEndpoint())
                .setRefreshToken(refreshToken).setChallengeResponse(getChallengeResponse())
                .setClientCredentials(getClientId(), getClientSecret());
    }

    public SAML20AssertionRequest getSAML20AssertionRequest() {
        return RequestFactory.newInstance(SAML20AssertionRequest.class, getAuthorizationEndpoint());
    }

    // TODO Have it as an inner class this can not work without parent
    // OAuth2Proxy<T, U> oAuth2Proxy
    // TODO Have a factory method: public InnerOAuth2Proxy newOAuth2Proxy(Flow
    // flow)
    // ----------------------------------------------------------------------------------------------

    public enum Flow {
        AUTHORIZATION_CODE, IMPLICIT, PASSWORD, CLIENT_CREDENTIALS, REFRESH_TOKEN, SAML20_INSERTION;
    }

    public enum AuthenticationStatus {
        UNAUTHENTICATED, REDIRECTED, AUTHENTICATED;
    }

    protected OAuth2Proxy(OAuth2Proxy<T, U> oAuth2Proxy, Flow flow) {
        this.oAuth2Proxy = oAuth2Proxy;
        this.flow = flow;
    }

    protected OAuth2Proxy(Context context, OAuth2Proxy<T, U> oAuth2Proxy, Flow flow) {
        super(context);
        this.oAuth2Proxy = oAuth2Proxy;
        this.flow = flow;
    }

    /**
     * The OAuth2Proxy.
     */
    private volatile OAuth2Proxy<T, U> oAuth2Proxy;

    private OAuth2Proxy.Flow flow;

    /**
     * The next Restlet.
     */
    private volatile Uniform next = null;
    private volatile Set<String> scope = null;
    private volatile Series<Parameter> parameters = null;

    public abstract RequestCallbackHandler<U> getRequestCallbackHandler(Request request,
            Response response);

    public OAuth2Proxy<T, U> getOAuth2Proxy() {
        return oAuth2Proxy;
    }

    /**
     * Returns the next Restlet.
     * 
     * @return The next Restlet or null.
     */
    public Uniform getNext() {
        return this.next;
    }

    /**
     * Sets the next Restlet.
     * <p/>
     * In addition, this method will set the context of the next Restlet if it
     * is null by passing a reference to its own context.
     * 
     * @param next
     *            The next Restlet.
     */
    public void setNext(Uniform next) {
        if (next instanceof Restlet) {
            Restlet nextRestlet = (Restlet) next;

            if (nextRestlet.getContext() == null) {
                nextRestlet.setContext(getContext());
            }
        }
        this.next = next;
    }

    public Set<String> getScope() {
        return scope;
    }

    public void setScope(Collection<String> requiredScope) {
        if (requiredScope instanceof Set) {
            this.scope = Collections.unmodifiableSet((Set<? extends String>) requiredScope);
        } else if (null == requiredScope) {
            this.scope = Collections.emptySet(); // TODO Set to null instead
        } else {
            this.scope = Collections.unmodifiableSet(new HashSet<String>(requiredScope));
        }
    }

    /**
     * Get a new instance of the parameters
     * <p/>
     * If the initial parameters is null the this method return null as well. If
     * the initial parameters is not null then it return a new modifiable copy
     * of the original parameters.
     * 
     * @return null or new instance of initial parameters
     */
    public Series<Parameter> getParameters() {
        if (null != parameters) {
            return new Series<Parameter>(Parameter.class, new Vector<Parameter>(parameters));
        }
        return null;
    }

    public void setParameters(Series<Parameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public void handle(Request request, Response response) {
        super.handle(request, response);
        if (getNext() != null) {

            switch (beforeHandle(request, response)) {
            case AUTHENTICATED: {
                getNext().handle(request, response);
                break;
            }
            case UNAUTHENTICATED: {
                response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
                break;
            }
            case REDIRECTED: {
                // The ClientResource should know to STOP processing the request
                // and let the User-Agent to redirected
                response.setStatus(Status.REDIRECTION_FOUND);
            }
            }

            // Re-associate the response to the current thread
            Response.setCurrent(response);

            // Associate the context to the current thread
            if (getContext() != null) {
                Context.setCurrent(getContext());
            }
        } else {
            response.setStatus(Status.SERVER_ERROR_INTERNAL);
            getLogger().warning("The Proxy was executed without a next Restlet attached to it.");
        }
    }

    protected AuthenticationStatus beforeHandle(Request request, Response response) {
        RequestCallbackHandler<U> callbackHandler = getRequestCallbackHandler(request, response);
        U token = callbackHandler.popAccessToken(request);
        if (null == token || !token.isExpired()) {

            // TODO Call the REFRESH_TOKEN if it's possible
            Flow au = getAuthenticationFlow();
            if (null != token && token.getRefreshToken() != null) {
                switch (au) {
                case AUTHORIZATION_CODE: {
                    au = Flow.REFRESH_TOKEN;
                    break;
                }
                case PASSWORD: {
                    au = Flow.REFRESH_TOKEN;
                    break;
                }
                }
            }
            switch (au) {
            case AUTHORIZATION_CODE: {
                return callbackHandler.authorizationRedirect(getOAuth2Proxy()
                        .handleAuthorizationCodeRequest(
                                callbackHandler.getRedirectionEndpoint(request, getOAuth2Proxy()
                                        .getRedirectionEndpoint()),
                                callbackHandler.getScope(request, getScope()),
                                callbackHandler.getState(request),
                                callbackHandler.decorateParameters(getParameters())));
            }
            case IMPLICIT: {
                return callbackHandler.authorizationRedirect(getOAuth2Proxy()
                        .handleImplicitRequest(
                                callbackHandler.getRedirectionEndpoint(request, getOAuth2Proxy()
                                        .getRedirectionEndpoint()),
                                callbackHandler.getScope(request, getScope()),
                                callbackHandler.getState(request),
                                callbackHandler.decorateParameters(getParameters())));
            }
            case PASSWORD: {
                token =
                        getOAuth2Proxy().flowPassword(
                                callbackHandler.getScope(request, getScope()),
                                callbackHandler.decorateParameters(getParameters()));
                break;
            }
            case CLIENT_CREDENTIALS: {
                token =
                        getOAuth2Proxy().flowClientCredentials(
                                callbackHandler.getScope(request, getScope()),
                                callbackHandler.decorateParameters(getParameters()));
                break;
            }
            case REFRESH_TOKEN: {
                if (token == null || token.getRefreshToken() == null) {
                    return AuthenticationStatus.UNAUTHENTICATED;
                }
                token =
                        getOAuth2Proxy().flowRefreshToken(token.getRefreshToken(),
                                callbackHandler.getScope(request, getScope()),
                                callbackHandler.decorateParameters(getParameters()));
                break;
            }
            case SAML20_INSERTION: {
                break;
            }
            }
            callbackHandler.pushAccessToken(request, token);
        }
        return beforeHandle(token, callbackHandler.getTokenLocation(request), request, response);
    }

    protected AuthenticationStatus beforeHandle(U token,
            OAuth2Utils.ParameterLocation tokenLocation, Request request, Response response) {
        boolean authenticated = token != null;
        if (authenticated) {
            if (null != tokenLocation) {
                switch (tokenLocation) {
                case HTTP_HEADER: {
                    request.setChallengeResponse(getAccessTokenExtractor().createChallengeResponse(
                            token));
                    break;
                }
                case HTTP_BODY: {
                    if (!Method.GET.equals(request.getMethod())
                            && (request.getEntity() == null || MediaType.APPLICATION_WWW_FORM
                                    .equals(request.getEntity().getMediaType()))) {
                        Form parameters = getAccessTokenExtractor().createForm(token);

                        if (null != request.getEntity()) {
                            Set<String> names = parameters.getNames();
                            for (Parameter parameter : new Form(request.getEntity())) {
                                if (names.contains(parameter.getName())) {
                                    continue;
                                }
                                parameters.add(parameter);
                            }
                        }

                        request.setEntity(parameters.getWebRepresentation());
                        break;
                    }
                }
                case HTTP_FRAGMENT: {
                    Form parameters =
                            request.getReferrerRef().hasFragment() ? new Form(request
                                    .getReferrerRef().getFragment()) : new Form();
                    parameters.addAll(getAccessTokenExtractor().createForm(token));
                    request.getResourceRef().setFragment(parameters.getQueryString());
                    break;
                }
                case HTTP_QUERY: {
                    for (Parameter parameter : getAccessTokenExtractor().createForm(token)) {
                        request.getResourceRef().addQueryParameter(parameter);
                    }
                    break;
                }
                }
            }
        }
        return authenticated ? AuthenticationStatus.AUTHENTICATED
                : AuthenticationStatus.UNAUTHENTICATED;
    }
}
