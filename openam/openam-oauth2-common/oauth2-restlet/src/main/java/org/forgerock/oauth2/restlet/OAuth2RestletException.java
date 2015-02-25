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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.oauth2.restlet;

import org.forgerock.oauth2.core.OAuth2Constants.UrlLocation;
import org.restlet.data.Status;

import java.util.HashMap;
import java.util.Map;

import static org.forgerock.oauth2.core.Utils.isEmpty;

/**
 * Thrown when a OAuth2 endpoint catches a {@link org.forgerock.oauth2.core.exceptions.OAuth2Exception}.
 *
 * @since 12.0.0
 */
public class OAuth2RestletException extends Exception {

    private final int statusCode;
    private final String error;
    private final String redirectUri;
    private final String state;
    private String errorUri;
    private final UrlLocation parameterLocation;

    /**
     * Constructs a new OAuth2RestletException without a redirect uri.
     *
     * @param statusCode The status code.
     * @param error The error.
     * @param description The description.
     * @param state The state from the request.
     */
    public OAuth2RestletException(int statusCode, String error, String description, String state) {
        this(statusCode, error, description, null, state);
    }

    /**
     * Constructs a new OAuth2RestletException with a redirect uri.
     * The {@link UrlLocation} for the parameters are defaulted to QUERY.
     *
     * @param statusCode The status code.
     * @param error The error.
     * @param description The description.
     * @param redirectUri The redirect uri from the request.
     * @param state The state from the request.
     */
    public OAuth2RestletException(int statusCode, String error, String description, String redirectUri, String state) {
        this(statusCode, error, description, redirectUri, state, UrlLocation.QUERY);
    }

    /**
     * Constructs a new OAuth2RestletException with a redirect uri.
     *
     * @param statusCode The status code.
     * @param error The error.
     * @param description The description.
     * @param redirectUri The redirect uri from the request.
     * @param state The state from the request.
     */
    public OAuth2RestletException(int statusCode, String error, String description, String redirectUri, String state,
                                  UrlLocation parameterLocation) {
        super(description);
        this.statusCode = statusCode;
        this.error = error;
        this.redirectUri = redirectUri;
        this.state = state;
        this.parameterLocation = parameterLocation;
    }

    /**
     * Gets the error.
     *
     * @return The error.
     */
    public String getError() {
        return error;
    }

    /**
     * Gets the error description.
     *
     * @return The error description.
     */
    public String getErrorDescription() {
        return getMessage();
    }

    /**
     * Gets the redirect uri.
     *
     * @return The redirect uri.
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * Gets the state that was on the request.
     *
     * @return The state.
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the error uri.
     *
     * @param errorUri The error uri.
     */
    public void setErrorUri(String errorUri) {
        this.errorUri = errorUri;
    }

    /**
     * Gets the error uri.
     *
     * @return The error uri.
     */
    public String  getErrorUri() {
        return errorUri;
    }

    /**
     * Gets the status code.
     *
     * @return The status code.
     */
    public Status getStatus() {
        return new Status(statusCode);
    }

    /**
     * Gets the location of the parameters in the URL.
     *
     * @return the location of the parameters.
     */
    public UrlLocation getParameterLocation() {
        return parameterLocation;
    }

    /**
     * Converts the exception into a map of its properties.
     *
     * @return A {@code Map}.
     */
    public final Map<String, String> asMap() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("error", getError());
        if (!isEmpty(getErrorDescription())) {
            map.put("error_description", getErrorDescription());
        }
        if (!isEmpty(getErrorUri())) {
            map.put("error_uri", getErrorUri());
        }
        if (!isEmpty(getState())) {
            map.put("state", getState());
        }
        return map;
    }
}
