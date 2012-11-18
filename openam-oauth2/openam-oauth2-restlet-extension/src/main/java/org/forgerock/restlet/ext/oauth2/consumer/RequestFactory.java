/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
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
 * ""Portions Copyrighted [2012] [ForgeRock Inc]""
 */

package org.forgerock.restlet.ext.oauth2.consumer;

import java.lang.reflect.Constructor;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashSet;
import java.util.Set;

import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.Form;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.util.Series;

/**
 * Creates a request
 *
 */
public class RequestFactory {

    private Reference endpoint;
    protected Form parameters;
    protected ChallengeResponse challengeResponse = null;
    protected Set<String> scope = null;

    public static <T extends RequestFactory> T newInstance(Class<T> factory, Reference endpoint) {
        try {
            for (Constructor<?> constructor : factory.getConstructors()) {
                System.out.println(constructor);
            }
            Constructor<T> constructor =
                    factory.getConstructor(new Class<?>[] { RequestFactory.class, Reference.class });
            return constructor.newInstance(new RequestFactory(endpoint), endpoint);
        } catch (Exception e) {
            // Should never happen.
            throw new UndeclaredThrowableException(e);
        }
    }

    protected RequestFactory(Reference protocolEndpoint) {
        endpoint = protocolEndpoint;
        parameters = new Form();
    }

    public Reference getEndpoint() {
        return endpoint;
    }

    protected static abstract class TokenRequestFactory extends RequestFactory {
        protected TokenRequestFactory(Reference endpoint) {
            super(endpoint);
        }

        public Request build(Context context) {
            Request request =
                    new Request(Method.POST, getEndpoint(), refreshParameters(context)
                            .getWebRepresentation());
            if (null != challengeResponse) {
                request.setChallengeResponse(challengeResponse);
            }
            return request;
        }

        public Request build() {
            return build(Context.getCurrent());
        }
    }

    protected static abstract class AuthorizationRequestFactory extends RequestFactory {
        protected AuthorizationRequestFactory(Reference endpoint) {
            super(endpoint);
            parameters = endpoint.getQueryAsForm();
        }

        public Reference build(Context context) {
            Reference request = new Reference(getEndpoint());
            request.addQueryParameters(refreshParameters(context));
            return request;
        }

        public Reference build() {
            return build(Context.getCurrent());
        }

        public Request buildRequest(Context context) {
            return new Request(Method.GET, build(context));
        }

        public Request buildRequest() {
            return buildRequest(Context.getCurrent());
        }
    }

    public Form getParameters() {
        return parameters;
    }

    protected void addCustomParameters(Series<Parameter> parameterSeries) {
        if (null != parameterSeries) {
            Set<String> names = parameters.getNames();
            for (Parameter parameter : parameterSeries) {
                if (names.contains(parameter.getName())) {
                    continue;
                }
                parameters.add(parameter);
            }
        }
    }

    protected void setParameterValue(String name, String value) {
        if (OAuth2Utils.isBlank(value)) {
            parameters.removeAll(name);
        } else {
            parameters.set(name, value);
        }
    }

    public Set<String> getScope() {
        if (null == scope) {
            scope = new HashSet<String>();
        }
        return scope;
    }

    protected Form refreshParameters(Context context) {
        if (scope == null) {
            // Do nothing
        } else if (scope.isEmpty()) {
            parameters.removeAll(OAuth2Constants.Params.SCOPE);
        } else {
            parameters.set(OAuth2Constants.Params.SCOPE, OAuth2Utils.join(scope, OAuth2Utils
                    .getScopeDelimiter(context)));
        }
        return parameters;
    }

    //

    public class AuthorizationCodeRequest extends AuthorizationRequestFactory {
        public AuthorizationCodeRequest(Reference endpoint) {
            super(endpoint);
            parameters.set(OAuth2Constants.Params.RESPONSE_TYPE, OAuth2Constants.AuthorizationEndpoint.CODE);
        }

        public AuthorizationCodeRequest addParameters(Series<Parameter> parameterSeries) {
            addCustomParameters(parameterSeries);
            return this;
        }

        public AuthorizationCodeRequest setState(String state) {
            setParameterValue(OAuth2Constants.Params.STATE, state);
            return this;
        }

        public AuthorizationCodeRequest setClientId(String client_id) {
            setParameterValue(OAuth2Constants.Params.CLIENT_ID, client_id);
            return this;
        }

        public AuthorizationCodeRequest setRedirectUri(String redirectUri) {
            setParameterValue(OAuth2Constants.Params.REDIRECT_URI, redirectUri);
            return this;
        }
    }

    public class ImplicitRequest extends AuthorizationRequestFactory {
        public ImplicitRequest(Reference endpoint) {
            super(endpoint);
            parameters.set(OAuth2Constants.Params.RESPONSE_TYPE, OAuth2Constants.AuthorizationEndpoint.TOKEN);
        }

        public ImplicitRequest addParameters(Series<Parameter> parameterSeries) {
            addCustomParameters(parameterSeries);
            return this;
        }

        public ImplicitRequest setState(String state) {
            setParameterValue(OAuth2Constants.Params.STATE, state);
            return this;
        }

        public ImplicitRequest setClientId(String client_id) {
            setParameterValue(OAuth2Constants.Params.CLIENT_ID, client_id);
            return this;
        }

        public ImplicitRequest setRedirectUri(String redirectUri) {
            setParameterValue(OAuth2Constants.Params.REDIRECT_URI, redirectUri);
            return this;
        }
    }

    //

    public class AuthorizationTokenRequest extends TokenRequestFactory {
        public AuthorizationTokenRequest(Reference endpoint) {
            super(endpoint);
            parameters.set(OAuth2Constants.Params.GRANT_TYPE, OAuth2Constants.TokeEndpoint.AUTHORIZATION_CODE);
        }

        public AuthorizationTokenRequest setChallengeResponse(ChallengeResponse challengeResponse) {
            this.challengeResponse = challengeResponse;
            return this;
        }

        public AuthorizationTokenRequest setClientCredentials(String client_id, String client_secret) {
            setParameterValue(OAuth2Constants.Params.CLIENT_ID, client_id);
            setParameterValue(OAuth2Constants.Params.CLIENT_SECRET, client_secret);
            return this;
        }

        public AuthorizationTokenRequest addParameters(Series<Parameter> parameterSeries) {
            addCustomParameters(parameterSeries);
            return this;
        }

        public AuthorizationTokenRequest setCode(String code) {
            setParameterValue(OAuth2Constants.Params.CODE, code);
            return this;
        }

        public AuthorizationTokenRequest setRedirectUri(String redirectUri) {
            setParameterValue(OAuth2Constants.Params.REDIRECT_URI, redirectUri);
            return this;
        }
    }

    public class PasswordRequest extends TokenRequestFactory {
        public PasswordRequest(Reference endpoint) {
            super(endpoint);
            parameters.set(OAuth2Constants.Params.GRANT_TYPE, OAuth2Constants.TokeEndpoint.PASSWORD);
        }

        public PasswordRequest setChallengeResponse(ChallengeResponse challengeResponse) {
            this.challengeResponse = challengeResponse;
            return this;
        }

        public PasswordRequest setClientCredentials(String client_id, String client_secret) {
            setParameterValue(OAuth2Constants.Params.CLIENT_ID, client_id);
            setParameterValue(OAuth2Constants.Params.CLIENT_SECRET, client_secret);
            return this;
        }

        public PasswordRequest addParameters(Series<Parameter> parameterSeries) {
            addCustomParameters(parameterSeries);
            return this;
        }

        public PasswordRequest setResourceOwnerCredentials(String username, String password) {
            setParameterValue(OAuth2Constants.Params.USERNAME, username);
            setParameterValue(OAuth2Constants.Params.PASSWORD, password);
            return this;
        }
    }

    public class ClientCredentialsRequest extends TokenRequestFactory {
        public ClientCredentialsRequest(Reference endpoint) {
            super(endpoint);
            parameters.set(OAuth2Constants.Params.GRANT_TYPE, OAuth2Constants.TokeEndpoint.CLIENT_CREDENTIALS);
        }

        public ClientCredentialsRequest setChallengeResponse(ChallengeResponse challengeResponse) {
            this.challengeResponse = challengeResponse;
            return this;
        }

        public ClientCredentialsRequest setClientCredentials(String client_id, String client_secret) {
            setParameterValue(OAuth2Constants.Params.CLIENT_ID, client_id);
            setParameterValue(OAuth2Constants.Params.CLIENT_SECRET, client_secret);
            return this;
        }

        public ClientCredentialsRequest addParameters(Series<Parameter> parameterSeries) {
            addCustomParameters(parameterSeries);
            return this;
        }
    }

    public class RefreshTokenRequest extends TokenRequestFactory {
        public RefreshTokenRequest(Reference endpoint) {
            super(endpoint);
            parameters.set(OAuth2Constants.Params.GRANT_TYPE, OAuth2Constants.TokeEndpoint.REFRESH_TOKEN);
        }

        public RefreshTokenRequest setChallengeResponse(ChallengeResponse challengeResponse) {
            this.challengeResponse = challengeResponse;
            return this;
        }

        public RefreshTokenRequest setClientCredentials(String client_id, String client_secret) {
            setParameterValue(OAuth2Constants.Params.CLIENT_ID, client_id);
            setParameterValue(OAuth2Constants.Params.CLIENT_SECRET, client_secret);
            return this;
        }

        public RefreshTokenRequest addParameters(Series<Parameter> parameterSeries) {
            addCustomParameters(parameterSeries);
            return this;
        }

        public RefreshTokenRequest setRefreshToken(String refreshToken) {
            setParameterValue(OAuth2Constants.Params.REFRESH_TOKEN, refreshToken);
            return this;
        }
    }

    public class SAML20AssertionRequest extends TokenRequestFactory {
        public SAML20AssertionRequest(Reference endpoint) {
            super(endpoint);
            parameters.set(OAuth2Constants.Params.GRANT_TYPE, OAuth2Constants.SAML20.GRANT_TYPE_URI);
        }

        public SAML20AssertionRequest addParameters(Series<Parameter> parameterSeries) {
            addCustomParameters(parameterSeries);
            return this;
        }

        public SAML20AssertionRequest setAssertion(String assertion) {
            parameters.add(OAuth2Constants.SAML20.ASSERTION, assertion);
            return this;
        }

        public SAML20AssertionRequest setClientAssertion(String assertion) {
            parameters.add(OAuth2Constants.SAML20.CLIENT_ASSERTION, assertion);
            return this;
        }
    }

}
