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
package org.forgerock.openam.cors;

/**
 * Holds all the necessary constants for the CORS Filter.
 *
 * Includes both keys for the configuration, as well as strings used
 * throughout the CORS protocol.
 */
public final class CORSConstants {

    /**
     * Not for use
     */
    private CORSConstants() {
        //do not instantiate
    }

    /**
     * Configuration Allowed Headers Key.
     */
    public static final String HEADERS_KEY = "headers";

    /**
     * Expected hostname Key
     */
    public static final String EXPECTED_HOSTNAME_KEY = "expectedHostname";

    /**
     * Configuration Allowed Origins Key.
     * REQUIRED CONFIGURATION
     */
    public static final String ORIGINS_KEY = "origins";

    /**
     * Configuration Allowed Methods Key.
     * REQUIRED CONFIGURATION
     */
    public static final String METHODS_KEY = "methods";

    /**
     * Exposed Headers Key.
     */
    public static final String EXPOSE_HEADERS_KEY = "exposeHeaders";

    /**
     * Allow credentials Key.
     */
    public static final String ALLOW_CREDENTIALS_KEY = "allowCredentials";

    /**
     * Configuration Max Age Key.
     */
    public static final String MAX_AGE_KEY = "maxAge";

    /**
     * Case-sensitive HTTP OPTIONS method.
     */
    public static final String HTTP_OPTIONS = "OPTIONS";

    /**
     * Indicative that every server should be trusted.
     */
    public static final String ALL = "*";

    /**
     * Origin header.
     */
    public static final String ORIGIN = "Origin";

    /**
     * Response header containing list of allowed origins.
     */
    public static final String AC_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    /**
     * Response header indicating access to the cookies.
     */
    public static final String AC_ALLOW_CREDS = "Access-Control-Allow-Credentials";

    /**
     * Case-sensitive true string for use in Access-Control-Allow-Credentials response
     */
    public static final String AC_CREDENTIALS_TRUE = "true";

    /**
     * Response header indicating list of allowed methods.
     */
    public static final String AC_ALLOW_METHODS= "Access-Control-Allow-Methods";

    /**
     * Response header indicating list of allowed headers.
     */
    public static final String AC_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    /**
     * Preflight-request method - asking us if CORS will handle this method.
     */
    public static final String AC_REQUEST_METHOD = "Access-Control-Request-Method";

    /**
     * Preflight-request header - asking us if CORS will handle these headers.
     */
    public static final String AC_REQUEST_HEADERS = "Access-Control-Request-Headers";

    /**
     * List of headers the caller should expose to its CORS client.
     */
    public static final String AC_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

    /**
     * Max length of time (in seconds) a user agent's CORS client should cache preflight responses.
     */
    public static final String AC_MAX_AGE = "Access-Control-Max-Age";

    /**
     * Used to indicate that the Origin header should not be cached
     */
    public static final String VARY = "Vary";

    /**
     * Used to check the host name provided is the same as the one the server belongs to
     */
    public static final String HOST = "Host";
}
