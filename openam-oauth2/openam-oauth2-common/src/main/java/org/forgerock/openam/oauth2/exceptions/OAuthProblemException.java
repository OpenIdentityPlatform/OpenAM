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

package org.forgerock.openam.oauth2.exceptions;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

public class OAuthProblemException extends ResourceException {

    private static final long serialVersionUID = 1934721539808864898L;

    public enum OAuthError {
        METHOD_NOT_ALLOWED(
                OAuth2Constants.Error.METHOD_NOT_ALLOWED,
                "The Method is not allowed.",
                "", 405),
        INVALID_REQUEST(
                OAuth2Constants.Error.INVALID_REQUEST,
                "The request is missing a required parameter, includes an invalid parameter value, or is otherwise malformed.",
                "", 400),
        UNAUTHORIZED_CLIENT(OAuth2Constants.Error.UNAUTHORIZED_CLIENT,
                "The client is not authorized to request an authorization code using this method.",
                ""),
        ACCESS_DENIED(OAuth2Constants.Error.ACCESS_DENIED,
                "The resource owner or authorization server denied the request.", ""),
        UNSUPPORTED_RESPONSE_TYPE(
                OAuth2Constants.Error.UNSUPPORTED_RESPONSE_TYPE,
                "The authorization server does not support obtaining an authorization code using this method.",
                "", 400),
        INVALID_SCOPE(OAuth2Constants.Error.INVALID_SCOPE,
                "The requested scope is invalid, unknown, or malformed.", ""),
        SERVER_ERROR(
                OAuth2Constants.Error.SERVER_ERROR,
                "The authorization server encountered an unexpected condition which prevented it from fulfilling the request.",
                ""),
        TEMPORARILY_UNAVAILABLE(
                OAuth2Constants.Error.TEMPORARILY_UNAVAILABLE,
                "The authorization server is currently unable to handle the request due to a temporary overloading or maintenance of the server.",
                ""),
        INVALID_TOKEN(
                OAuth2Constants.Error.INVALID_TOKEN,
                "The access token provided is expired, revoked, malformed, or invalid for other reasons.",
                "", 403),
        INSUFFICIENT_SCOPE(OAuth2Constants.Error.INSUFFICIENT_SCOPE,
                "The request requires higher privileges than provided by the access token.", "",
                403),
        EXPIRED_TOKEN(OAuth2Constants.Error.EXPIRED_TOKEN,
                "The request contains a token no longer valid.", "",
                401),
        INVALID_CLIENT(
                OAuth2Constants.Error.INVALID_CLIENT,
                "The client identifier provided is invalid, the client failed to authenticate, the client did not include its credentials, provided multiple client credentials, or used unsupported credentials type.",
                "", 400),
        UNKNOWN_ERROR(
                OAuth2Constants.Error.UNKNOWN_ERROR,
                "The authenticated client is not authorized to use the access grant type provided.",
                "", 403),
        INVALID_GRANT(OAuth2Constants.Error.INVALID_GRANT,
                "The provided access grant is invalid, expired, or revoked.", "", 400),
        UNSUPPORTED_GRANT_TYPE(
                OAuth2Constants.Error.UNSUPPORTED_GRANT_TYPE,
                "The provided access grant is invalid, expired, or revoked (e.g. invalid assertion, expired authorization token, bad end-user password credentials, or mismatching authorization code and redirection URI).",
                "", 400),
        INVALID_CODE(OAuth2Constants.Error.INVALID_CODE, "The code provided is invalid.",
                ""),
        REDIRECT_URI_MISMATCH(OAuth2Constants.Error.REDIRECT_URI_MISMATCH,
                "The redirection URI provided does not match a pre-registered value.", "", 400),
        UNSUPPORTED_AUTH_TYPE(OAuth2Constants.Error.UNSUPPORTED_AUTH_TYPE,
                "The requested authentication type is not supported by the authorization server.",
                ""),
        NOT_FOUND(OAuth2Constants.Error.NOT_FOUND,
                "The request is for data which does not exist.", "", 404),
        INVALID_CLIENT_METADATA(OAuth2Constants.Error.INVALID_CLIENT_METADATA,
                "The request contains invalid metadata.", "", 400),
        BAD_REQUEST(OAuth2Constants.Error.BAD_REQUEST,
                "The request could not be understood by the server due to malformed syntax", "", 400);
        Status status;

        private OAuthError(String reasonPhrase, String description, String uri) {
            this.status = new Status(400, reasonPhrase, description, uri);
        }

        private OAuthError(String reasonPhrase, String description, String uri, int code) {
            this.status = new Status(code, reasonPhrase, description, uri);
        }

        /**
         * Create a new exception from the given {@code request} parameter.
         * 
         * @param request
         * @return new instance of OAuthProblemException
         */
        public OAuthProblemException handle(Request request) {
            return new OAuthProblemException(this, request);
        }

        public OAuthProblemException handle(Request request, String description) {
            return new OAuthProblemException(this, request).description(description);
        }
    }

    private String description;
    private String errorUri;
    //
    // private Status status;
    private Request request;
    private URI redirectTargetPattern;
    //
    private String state;
    private String scope;

    private Map<String, String> parameters = new HashMap<String, String>();

    // Constructors
    private OAuthProblemException(OAuthError error, Request request) {
        super(error.status);
        this.description = null;
        this.errorUri = null;
        this.request = request;
        if (null != this.request) {
            String redirect =
                    OAuth2Utils.getRequestParameter(request, OAuth2Constants.Params.REDIRECT_URI,
                            String.class);
            this.redirectTargetPattern = null != redirect ? URI.create(redirect) : null;
            this.state =
                    OAuth2Utils.getRequestParameter(request, OAuth2Constants.Params.STATE, String.class);
            this.scope =
                    OAuth2Utils.getRequestParameter(request, OAuth2Constants.Params.SCOPE, String.class);
        } else {
            this.redirectTargetPattern = null;
            this.state = null;
            this.scope = null;
        }

    }

    public OAuthProblemException(int code, String error, String description, String errorUri) {
        super(new Status(code, error, description, errorUri));
        this.description = null;
        this.errorUri = null;
        this.request = null;
        this.redirectTargetPattern = null;
        this.state = null;
        this.scope = null;
    }

    public OAuthProblemException(Status status, String description, Throwable cause) {
        super(new Status(status.getCode(), status.getReasonPhrase(), description, status.getUri()),
                cause);
        this.description = null;
        this.errorUri = null;
        this.request = null;
        this.redirectTargetPattern = null;
        this.state = null;
        this.scope = null;
    }

    // ConsumerFlow builder
    public OAuthProblemException description(String description) {
        this.description = description;
        return this;
    }

    public OAuthProblemException errorUri(String uri) {
        this.errorUri = uri;
        return this;
    }

    public OAuthProblemException redirectUri(URI redirectTargetPattern) {
        this.redirectTargetPattern = redirectTargetPattern;
        return this;
    }

    public OAuthProblemException state(String state) {
        this.state = state;
        return this;
    }

    public OAuthProblemException scope(String scope) {
        this.scope = scope;
        return this;
    }

    public OAuthProblemException setParameter(String name, String value) {
        parameters.put(name, value);
        return this;
    }

    // Getters
    public String getError() {
        return getStatus().getReasonPhrase();
    }

    public String getDescription() {
        return description != null ? description : super.getStatus().getDescription();
    }

    public String getErrorUri() {
        return errorUri != null ? errorUri : getStatus().getUri();
    }

    public String getState() {
        return state;
    }

    public String getScope() {
        return scope;
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public URI getRedirectUri() {
        return redirectTargetPattern;
    }

    /**
     * {@inheritDoc}
     */
    public Status getStatus() {
        if (null == description) {
            return super.getStatus();
        } else {
            return new Status(super.getStatus(), getCause(), description);
        }
    }

    /**
     * Save the exception into the request.
     * <p/>
     * Save the OAuthProblemException into the attributes and the
     * {@link OAuthProblemException#popException(org.restlet.Request)} method
     * retreive it.
     * 
     * @throws ResourceException
     *             if the embedded request is null
     */
    //public Class<? extends AbstractFlow> pushException() throws ResourceException {
    public Object pushException() throws ResourceException {
        if (null != request) {
            request.getAttributes().put(OAuthProblemException.class.getName(), this);
        } else {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Failed to push Exception",
                    this);
        }
        //return ErrorServerResource.class;
        return null;
    }

    /**
     * Used for formatting error according to chapter 5.2.
     * 
     * @see <a
     *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-5.2">5.2.
     *      Error Response</a>
     */
    public Map<String, Object> getErrorMessage() {
        Map<String, Object> response = new HashMap<String, Object>(3);
        response.put(OAuth2Constants.Error.ERROR, getError());
        if (OAuth2Utils.isNotBlank(getDescription())) {
            response.put(OAuth2Constants.Error.ERROR_DESCRIPTION, getDescription());
        }
        if (errorUri != null && errorUri.length() > 0) {
            response.put(OAuth2Constants.Error.ERROR_URI, getError().toString());
        }
        return response;
    }

    /**
     * Used for formatting error according to chapter 4.2.2.1.
     * <p/>
     * Authorization Code (Query) HTTP/1.1 302 Found Location:
     * https://client.example.com/cb?error=access_denied&state=xyz
     * <p/>
     * Implicit (Fragment) HTTP/1.1 302 Found Location:
     * https://client.example.com/cb#error=access_denied&state=xyz
     * 
     * @see <a
     *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-4.2.2.1">4.2.2.1.
     *      Error Response</a>
     */
    public Form getErrorForm() {
        Form response = new Form();
        response.add(OAuth2Constants.Error.ERROR, getError());
        if (OAuth2Utils.isNotBlank(getDescription())) {
            response.add(OAuth2Constants.Error.ERROR_DESCRIPTION, getDescription());
        }
        if (errorUri != null && errorUri.length() > 0) {
            response.add(OAuth2Constants.Error.ERROR_URI, errorUri);
        }
        // TODO could automatically check for state....
        if (OAuth2Utils.isNotBlank(getState())) {
            response.add(OAuth2Constants.Params.STATE, getState());
        }
        return response;
    }

    //
    public static OAuthProblemException error(String error) {
        return new OAuthProblemException(401, error, null, null);
    }

    public static OAuthProblemException error(String error, String description) {
        return new OAuthProblemException(401, error, description, null);
    }

    public static OAuthProblemException error(int code, String error, String description) {
        return new OAuthProblemException(code, error, description, null);
    }

    /**
     * Creates invalid_request exception with given message
     * 
     * @param message
     *            error message
     * @return new instance of OAuthProblemException
     */
    public static OAuthProblemException handleOAuthProblemException(String message) {
        return OAuthProblemException.error(OAuth2Constants.Error.INVALID_REQUEST).description(message);
    }

    /**
     * Creates OAuthProblemException that contains set of missing oauth
     * parameters
     * 
     * @param missingParams
     *            missing oauth parameters
     * @return OAuthProblemException with user friendly message about missing
     *         oauth parameters
     */
    public static OAuthProblemException handleMissingParameters(Set<String> missingParams) {
        StringBuilder sb = new StringBuilder("Missing parameters: ");
        if (null != missingParams && !missingParams.isEmpty()) {
            for (String missingParam : missingParams) {
                sb.append(missingParam).append(" ");
            }
        }
        return handleOAuthProblemException(sb.toString().trim());
    }

    public static OAuthProblemException handleBadContentTypeException(String expectedContentType) {
        StringBuilder errorMsg =
                new StringBuilder("Bad request content type. Expecting: ")
                        .append(expectedContentType);
        return handleOAuthProblemException(errorMsg.toString());
    }

    public static OAuthProblemException handleNotAllowedParametersOAuthException(
            List<String> notAllowedParams) {
        StringBuffer sb = new StringBuffer("Not allowed parameters: ");
        if (notAllowedParams != null) {
            for (String notAllowed : notAllowedParams) {
                sb.append(notAllowed).append(" ");
            }
        }
        return handleOAuthProblemException(sb.toString().trim());
    }

    public static OAuthProblemException popException(Request r) {
        Object o = r.getAttributes().remove(OAuthProblemException.class.getName());
        if (o instanceof OAuthProblemException) {
            return (OAuthProblemException) o;
        }
        return null;
    }
}
