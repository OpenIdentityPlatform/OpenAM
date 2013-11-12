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
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.restlet.ext.oauth2.consumer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.model.CoreToken;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeRequest;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Status;
import org.restlet.engine.security.AuthenticatorHelper;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.util.Series;

/**
 * An AccessTokenExtractor extracts the AccessToken from the Request.
 *
 */
public abstract class AccessTokenExtractor<T extends CoreToken> {

    protected AccessTokenExtractor() {
    }

    /**
     * Extracts the access token from the contents of an {@link Response}
     * 
     * @param response
     *            the contents of the response
     * @return OAuth2 access token
     */
    // public abstract T extract(Response response);

    /**
     * Extracts the access token from the contents of an {@link Request}
     * <p/>
     * This method used to get the token from the redirect GET
     * 
     * @return OAuth2 access token @ param request the contents of the request
     */
    // public abstract T extract(OAuth2Utils.ParameterLocation tokenLocation,
    // Request request);
    public abstract ChallengeResponse createChallengeResponse(T token);

    public abstract ChallengeRequest createChallengeRequest(String realm);

    public abstract ChallengeRequest createChallengeRequest(String realm,
            OAuthProblemException exception);

    public abstract Form createForm(T token);

    protected abstract T extractRequestToken(ChallengeResponse challengeResponse)
            throws OAuthProblemException;

    /**
     * @param request
     * @return
     * @throws OAuthProblemException
     */
    protected abstract T extractRequestToken(Request request) throws OAuthProblemException;

    protected abstract T extractRequestToken(Response response) throws OAuthProblemException;

    protected abstract T extractRequestToken(Form parameters) throws OAuthProblemException;

    /**
     * Returns the parameters to use for authentication.
     * <p/>
     * From Header - Authorization: Bearer vF9dft4qmT From Header -
     * Authorization: MAC
     * id="h480djs93hd8",ts="1336363200",nonce="dj83hs9s",mac=
     * "bhCQXTVyfj5cmA9uKkPFx1zeOXM=" From Query - ?access_token=vF9dft4qmT From
     * APPLICATION_WWW_FORM - access_token=vF9dft4qmT
     * 
     * @param request
     *            The request.
     * @return The access token taken from a given request.
     */
    public T extractToken(OAuth2Utils.ParameterLocation tokenLocation, Request request)
            throws OAuthProblemException {
        T token = null;
        switch (tokenLocation) {
        case HTTP_HEADER: {
            if (null != request.getChallengeResponse()) {
                token = extractRequestToken(request.getChallengeResponse());
            }
            break;
        }
        case HTTP_BODY: {
            if (null != request.getEntity()
                    && request.getEntity() instanceof EmptyRepresentation == false) {
                token = extractRequestToken(request);
            }
            break;
        }
        case HTTP_QUERY: {
            if (request.getResourceRef().hasQuery()) {
                token = extractRequestToken(request.getResourceRef().getQueryAsForm());
            }
            break;
        }
        case HTTP_FRAGMENT: {
            if (request.getResourceRef().hasFragment()) {
                token = extractRequestToken(new Form(request.getResourceRef().getFragment()));
            }
            break;
        }
        }

        /*
         * if (request.getResourceRef().hasFragment()) { token = new
         * HashMap<String, Object>(new
         * Form(request.getResourceRef().getFragment()).getValuesMap()); } else
         * if (request.getResourceRef().hasQuery()) { token = new
         * HashMap<String,
         * Object>(request.getResourceRef().getQueryAsForm().getValuesMap()); }
         * if (null != token) { OAuthProblemException exception =
         * extractException(token); if (exception != null) { throw exception; }
         * }
         */
        return token;
    }

    public T extractToken(OAuth2Utils.ParameterLocation tokenLocation, Response response)
            throws OAuthProblemException {
        T token = null;
        switch (tokenLocation) {
        case HTTP_HEADER: {
            // TODO Something nice to have
            /*
             * if (!response.getChallengeRequests().isEmpty()) { for
             * (ChallengeRequest cr : response.getChallengeRequests()) {
             * OAuthProblemException exception =
             * extractException(cr.getParameters().getValuesMap()); if (null !=
             * exception) { throw exception; } } }
             */
            break;
        }
        case HTTP_BODY: {
            if (null != response.getEntity()
                    && response.getEntity() instanceof EmptyRepresentation == false) {
                token = extractRequestToken(response);
            }
            break;
        }
        case HTTP_QUERY: {
            if (Status.REDIRECTION_FOUND.equals(response.getStatus())
                    && response.getLocationRef().hasQuery()) {
                token = extractRequestToken(response.getLocationRef().getQueryAsForm());
            }
            break;
        }
        case HTTP_FRAGMENT: {
            if (Status.REDIRECTION_FOUND.equals(response.getStatus())
                    && response.getLocationRef().hasFragment()) {
                token = extractRequestToken(new Form(response.getLocationRef().getFragment()));
            }
            break;
        }
        }
        return token;
    }

    public static Map<String, Object> extractToken(Request request) throws OAuthProblemException {
        Map<String, Object> token = null;
        if (request.getResourceRef().hasFragment()) {
            token =
                    new HashMap<String, Object>(new Form(request.getResourceRef()
                            .getFragment()).getValuesMap());
        } else if (request.getResourceRef().hasQuery()) {
            token =
                    new HashMap<String, Object>(request.getResourceRef().getQueryAsForm()
                            .getValuesMap());
        } else if (null != request.getEntity()
            && MediaType.APPLICATION_JSON.equals(request.getEntity().getMediaType())) {
            try {
                token = new JacksonRepresentation<Map>(request.getEntity(), Map.class).getObject();
            } catch (IOException e) {
                /* ignored */
            }
        }
        if (null != token) {
            OAuthProblemException exception = extractException(token);
            if (exception != null) {
                OAuth2Utils.DEBUG.error("Unable to extract token from request", exception);
                throw exception;
            }
        }
        return token;
    }

    public static Map<String, Object> extractToken(Response response) throws OAuthProblemException {
        Map<String, Object> token = null;
        if (Status.REDIRECTION_FOUND.equals(response.getStatus())) {
            if (response.getLocationRef().hasFragment()) {
                token =
                        new HashMap<String, Object>(new Form(response.getLocationRef()
                                .getFragment()).getValuesMap());
            } else if (response.getLocationRef().hasQuery()) {
                token =
                        new HashMap<String, Object>(response.getLocationRef().getQueryAsForm()
                                .getValuesMap());
            }
        } else if (null != response.getEntity()
                && MediaType.APPLICATION_JSON.equals(response.getEntity().getMediaType())) {
            try {
                token = new JacksonRepresentation<Map>(response.getEntity(), Map.class).getObject();

            } catch (IOException e) {
                /* ignored */
            }
        }
        if (null != token) {
            OAuthProblemException exception = extractException(token);
            if (exception != null) {
                OAuth2Utils.DEBUG.error("Unable to extract token from request", exception);
                throw exception;
            }
        }
        return token;
    }

    public static OAuthProblemException extractException(Map<String, Object> response) {
        Object error = response.get(OAuth2Constants.Params.ERROR);
        if (error instanceof String) {
            String error_uri = null;
            Object o = response.get(OAuth2Constants.Params.ERROR_URI);
            if (o instanceof String) {
                error_uri = (String) o;
            }
            String error_description = null;
            o = response.get(OAuth2Constants.Params.ERROR_DESCRIPTION);
            if (o instanceof String) {
                error_description = (String) o;
            }
            return extractException((String) error, error_description, error_uri);
        }
        return null;
    }

    public static OAuthProblemException extractException(Series<Parameter> response) {
        return extractException(response.getFirstValue(OAuth2Constants.Params.ERROR), response
                .getFirstValue(OAuth2Constants.Params.ERROR_DESCRIPTION), response
                .getFirstValue(OAuth2Constants.Params.ERROR_URI));
    }

    protected static OAuthProblemException extractException(String error, String error_description,
            String error_uri) {
        OAuthProblemException exception = null;
        if (null != error) {
            OAuthProblemException.OAuthError e = OAuthProblemException.OAuthError.UNKNOWN_ERROR;
            try {
                e =
                        Enum.valueOf(OAuthProblemException.OAuthError.class, ((String) error)
                                .toUpperCase());
            } catch (IllegalArgumentException ex) {
            }

            if (null != error_description) {
                exception = e.handle(/* Request.getCurrent() */null, error_description);
            } else {
                exception = e.handle(/* Request.getCurrent() */null);
            }

            if (null != error_uri) {
                exception.errorUri(error_uri);
            }
        }
        return exception;
    }
}
