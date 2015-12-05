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

package org.forgerock.openam.core.rest.authn.http;

import static org.forgerock.json.JsonValue.*;

import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.L10NMessage;
import com.sun.identity.shared.locale.Locale;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.forgerock.http.header.AcceptLanguageHeader;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.header.MalformedHeaderException;
import org.forgerock.http.protocol.Entity;
import org.forgerock.http.protocol.Form;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.authn.RestAuthenticationHandler;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.http.annotations.Contextual;
import org.forgerock.openam.http.annotations.Post;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.restlet.resource.ResourceException;

/**
 *
 */
public class AuthenticationServiceV1 {

    private static final Debug DEBUG = Debug.getInstance("amAuthREST");

    private static final ContentTypeHeader APPLICATION_JSON_CONTENT_TYPE =
            ContentTypeHeader.valueOf("application/json");
    private static final String CACHE_CONTROL_HEADER_NAME = "Cache-Control";
    private static final String NO_CACHE_CACHE_CONTROL_HEADER = "no-cache, no-store, must-revalidate";
    private static final String PRAGMA_HEADER_NAME = "Pragma";
    private static final String PRAGMA_NO_CACHE_HEADER = "no-cache";
    private static final String EXPIRES_HEADER_NAME = "Expires";
    private static final String ALWAYS_EXPIRE_HEADER = "0";
    private static final String CONTENT_TYPE_HEADER_NAME = "Content-Type";
    private static final String REALM = "realm";
    private static final String JSONCONTENT = "jsonContent";

    private final RestAuthenticationHandler restAuthenticationHandler;

    /**
     * Constructs an instance of the AuthenticationRestService.
     *
     * @param restAuthenticationHandler An instance of the RestAuthenticationHandler.
     */
    @Inject
    public AuthenticationServiceV1(RestAuthenticationHandler restAuthenticationHandler) {
        this.restAuthenticationHandler = restAuthenticationHandler;
    }

    /**
     * Handles both initial and subsequent RESTful calls from clients submitting Callbacks for the authentication
     * process to continue. This is determined by checking if the POST body is empty or not. If it is empty then this
     * is initiating the authentication process otherwise it is a subsequent call submitting Callbacks.
     *
     * Initiating authentication request using the query parameters from the URL starts the login process and either
     * returns an SSOToken on successful authentication or a number of Callbacks needing to be completed before
     * authentication can proceed or an exception if any problems occurred whilst trying to authenticate.
     *
     * Using the body of the POST request the method continues the login process, submitting the given Callbacks and
     * then either returns an SSOToken on successful authentication or a number of additional Callbacks needing to be
     * completed before authentication can proceed or an exception if any problems occurred whilst trying to
     * authenticate.
     *
     * @param context The request context.
     * @param httpRequest The HTTP request.
     * @return A Json Representation of the response body. The response will contain either a JSON object containing the
     * SSOToken id from a successful authentication, a JSON object containing a number of Callbacks for the client to
     * complete and return or a JSON object containing an exception message.
     * @throws ResourceException If there is an error processing the authentication request.
     */
    @Post
    public Response authenticate(@Contextual Context context, @Contextual Request httpRequest) {

        if (!isSupportedMediaType(httpRequest)) {
            if (DEBUG.errorEnabled()) {
                DEBUG.error("AuthenticationService :: Unable to handle media type request : " + ContentTypeHeader.valueOf(httpRequest).getType());
            }
            return handleErrorResponse(httpRequest, Status.UNSUPPORTED_MEDIA_TYPE, null);
        }

        final HttpServletResponse response = getHttpServletResponse(context);

        Form urlQueryString = getUrlQueryString(httpRequest);
        final String sessionUpgradeSSOTokenId = urlQueryString.getFirst("sessionUpgradeSSOTokenId");

        try {
            JsonValue jsonContent;
            try {
                jsonContent = getJsonContent(httpRequest);
            } catch (IOException e) {
                DEBUG.message("AuthenticationService.authenticate() :: JSON parsing error", e);
                return handleErrorResponse(httpRequest, Status.BAD_REQUEST, e);
            }
            final HttpServletRequest request = getHttpServletRequest(context, jsonContent);
            JsonValue jsonResponse;

            if (jsonContent != null && jsonContent.size() > 0) {
                // submit requirements
                jsonResponse = restAuthenticationHandler.continueAuthentication(request, response, jsonContent,
                        sessionUpgradeSSOTokenId);
            } else {
                // initiate
                final String authIndexType = urlQueryString.getFirst("authIndexType");
                final String authIndexValue = urlQueryString.getFirst("authIndexValue");
                jsonResponse = restAuthenticationHandler.initiateAuthentication(request, response, authIndexType,
                        authIndexValue, sessionUpgradeSSOTokenId);
            }

            return createResponse(jsonResponse);

        } catch (RestAuthResponseException e) {
            DEBUG.message("AuthenticationService.authenticate() :: Exception from CallbackHandler", e);
            return handleErrorResponse(httpRequest, Status.valueOf(e.getStatusCode()), e);
        } catch (RestAuthException e) {
            DEBUG.message("AuthenticationService.authenticate() :: Rest Authentication Exception", e);
            return handleErrorResponse(httpRequest, Status.valueOf(e.getStatusCode()), e);
        } catch (IOException e) {
            DEBUG.error("AuthenticationService.authenticate() :: Internal Error", e);
            return handleErrorResponse(httpRequest, Status.INTERNAL_SERVER_ERROR, e);
        }
    }

    private Form getUrlQueryString(Request request) {
        return new Form().fromRequestQuery(request);
    }

    private HttpServletResponse getHttpServletResponse(Context context) {
        AttributesContext requestContext = context.asContext(AttributesContext.class);
        Map<String, Object> requestAttributes = requestContext.getAttributes();
        return (HttpServletResponse) requestAttributes.get(HttpServletResponse.class.getName());
    }

    private boolean isSupportedMediaType(Request request) {
        return request.getEntity().isDecodedContentEmpty()
                || APPLICATION_JSON_CONTENT_TYPE.getType().equals(ContentTypeHeader.valueOf(request).getType());
    }

    /**
     * Gets the HttpServletRequest from Restlet and wraps the HttpServletRequest with the URI realm as long as
     * the request does not contain the realm as a query parameter.
     *
     * @return The HttpServletRequest
     */
    private HttpServletRequest getHttpServletRequest(Context context, JsonValue jsonValue) {
        AttributesContext requestContext = context.asContext(AttributesContext.class);
        Map<String, Object> requestAttributes = requestContext.getAttributes();
        final HttpServletRequest request = (HttpServletRequest) requestAttributes.get(HttpServletRequest.class.getName());

        // The request contains the realm query param then use that over any realm parsed from the URI
        final String queryParamRealm = request.getParameter(REALM);
        if (queryParamRealm != null && !queryParamRealm.isEmpty()) {
            RealmContext rc = new RealmContext(context);
            rc.setOverrideRealm(queryParamRealm);
            return wrapRequest(request, rc, jsonValue);
        }

        return wrapRequest(request, context.asContext(RealmContext.class), jsonValue);
    }

    /**
     * Wraps the HttpServletRequest with the realm information used in the URI.
     *
     * @param request The HttpServletRequest.
     * @return The wrapped HttpServletRequest.
     */
    private HttpServletRequest wrapRequest(final HttpServletRequest request,
                                           final RealmContext realmContext,
                                           final JsonValue jsonValue) {

        return new HttpServletRequestWrapper(request) {

            @Override
            public String getParameter(String name) {
                if (REALM.equals(name)) {
                    return realmContext.getResolvedRealm();
                }

                if (JSONCONTENT.equals(name)) {
                    return jsonValue.toString();
                }

                return super.getParameter(name);
            }

            @Override
            public Map getParameterMap() {
                Map params = super.getParameterMap();
                Map p = new HashMap(params);
                p.put(REALM, realmContext.getResolvedRealm());
                return p;
            }

            @Override
            public Enumeration getParameterNames() {
                Set<String> names = new HashSet<>();
                Enumeration<String> paramNames = super.getParameterNames();
                while (paramNames.hasMoreElements()) {
                    names.add(paramNames.nextElement());
                }
                names.add(REALM);
                return Collections.enumeration(names);
            }

            @Override
            public String[] getParameterValues(String name) {
                if (REALM.equals(name)) {
                    return new String[]{realmContext.getResolvedRealm()};
                }
                return super.getParameterValues(name);
            }
        };
    }

    /**
     * Creates a JsonValue from the request post body.
     *
     * @param request The request containing JSON body.
     * @return A JsonValue of the request posy body.
     * @throws IOException If there is a problem parsing the Json entity.
     */
    private JsonValue getJsonContent(Request request) throws IOException {
        Entity entity = request.getEntity();
        if (entity == null) {
            return null;
        } else if (!entity.isDecodedContentEmpty()) {
            return json(entity.getJson());
        } else {
            return json(object());
        }
    }

    /**
     * Creates a response from the given JsonValue.
     *
     * @param jsonResponse The Json response body.
     * @return a response.
     * @throws IOException If there is a problem creating the response.
     */
    private Response createResponse(final JsonValue jsonResponse) throws IOException {

        Response response = new Response(Status.OK);

        response.getHeaders().put(CACHE_CONTROL_HEADER_NAME, NO_CACHE_CACHE_CONTROL_HEADER);
        response.getHeaders().put(PRAGMA_HEADER_NAME, PRAGMA_NO_CACHE_HEADER);
        response.getHeaders().put(EXPIRES_HEADER_NAME, ALWAYS_EXPIRE_HEADER);
        response.getHeaders().put(CONTENT_TYPE_HEADER_NAME, "application/json");

        response.setEntity(jsonResponse.getObject());
        return response;
    }

    /**
     * Processes the given Exception into a Restlet response representation or wrap it into
     * a ResourceException, which will be thrown.
     *
     * @param status The status to set the response to.
     * @param exception The Exception to be handled.
     * @return The Restlet Response Representation.
     * @throws ResourceException If the given exception is wrapped in a ResourceException.
     */
    protected Response handleErrorResponse(Request request, Status status, Exception exception) {
        Reject.ifNull(status);
        Response response = new Response(status);
        final Map<String, Object> rep = new HashMap<>();
        if (exception instanceof RestAuthResponseException) {
            final RestAuthResponseException authResponseException = (RestAuthResponseException) exception;
            for (Map.Entry<String, String> entry : authResponseException.getResponseHeaders().entrySet()) {
                response.getHeaders().put(entry.getKey(), entry.getValue());
            }
            rep.putAll(authResponseException.getJsonResponse().asMap());

        } else if (exception instanceof RestAuthException) {
            final RestAuthException authException = (RestAuthException)exception;
            if (authException.getFailureUrl() != null) {
                rep.put("failureUrl", authException.getFailureUrl());
            }
            rep.put("errorMessage", getLocalizedMessage(request, exception));

        } else if (exception == null) {
            rep.put("errorMessage", status.getReasonPhrase());
        } else {
            rep.put("errorMessage", getLocalizedMessage(request, exception));
        }

        response.setEntity(rep);

        return response;
    }

    /**
     * Get the localized message for the requested language if the given exception or its cause
     * is an instance of <code>L10NMessage</code>.
     *
     * @param exception The exception that contains the localized message.
     *
     * @return The localized message.
     */
    protected String getLocalizedMessage(Request request, Exception exception) {
        AcceptLanguageHeader languages = null;
        try {
            languages = request.getHeaders().get(AcceptLanguageHeader.class);
        } catch (MalformedHeaderException e) {
            DEBUG.warning("Could not parse accept language header", e);
        }
        String message = null;
        L10NMessage localizedException = null;
        if (exception instanceof L10NMessage) {
            localizedException = (L10NMessage) exception;
        } else if (exception.getCause() instanceof L10NMessage) {
            localizedException = (L10NMessage) exception.getCause();
        }
        if (localizedException != null) {
            if (languages == null) {
                message = localizeMessage(localizedException, Locale.getDefaultLocale());
            } else {
                for (java.util.Locale language : languages.getLocales().getLocales()) {
                    message = localizeMessage(localizedException, language);
                    if (message != null) {
                        break;
                    }
                }
            }
        }
        if (message == null) {
            message = exception.getMessage();
        }
        return message;
    }

    private String localizeMessage(L10NMessage localizedException, java.util.Locale locale) {
        String message = localizedException.getL10NMessage(locale);
        // Old UI used a jsp template to display the error message, which we need to strip off here
        int delimiterIndex = message.indexOf(AuthClientUtils.MSG_DELIMITER);
        if (delimiterIndex > -1) {
            message = message.substring(0, delimiterIndex);
        }
        return message;
    }
}
