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
package org.forgerock.openam.shared.security.whitelist;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.whitelist.URLPatternMatcher;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import org.forgerock.json.fluent.JsonValue;

/**
 * Validates the provided redirect URL against the list of valid goto URL domains.
 *
 * @param <T> The type of the configuration information that is provided to find the collection of valid domains.
 */
public class RedirectUrlValidator<T> {

    public final static String GOTO = "goto";
    public final static String GOTO_ON_FAIL = "gotoOnFail";

    private static final Debug DEBUG = Debug.getInstance("patternMatching");
    private final ValidDomainExtractor<T> domainExtractor;

    public RedirectUrlValidator(final ValidDomainExtractor<T> domainExtractor) {
        this.domainExtractor = domainExtractor;
    }

    /**
     * Validates the provided redirect URL against the collection of valid goto URL domains found based on the
     * configuration info.
     *
     * @param url The URL that needs to be validated. May be null.
     * @param configInfo The necessary information about the configuration to determine the collection of valid goto
     * URL domains. May not be null.
     * @return <code>true</code> if the provided URL is valid, <code>false</code> otherwise.
     */
    public boolean isRedirectUrlValid(final String url, final T configInfo) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        final Collection<String> patterns = domainExtractor.extractValidDomains(configInfo);
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Validating goto URL " + url + " against patterns:\n" + patterns);
        }
        if (patterns == null || patterns.isEmpty()) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("There are no patterns to validate the URL against, the goto URL is considered valid");
            }
            return true;
        }

        try {
            final URI uri = new URI(url);
            if (!uri.isAbsolute()) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message(url + " is a relative URI, the goto URL is considered valid");
                }
                return true;
            }
        } catch (final URISyntaxException urise) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("The goto URL " + url + " is not a valid URI", urise);
            }
            return false;
        }

        final URLPatternMatcher patternMatcher = new URLPatternMatcher();
        try {
            return patternMatcher.match(url, patterns, true);
        } catch (MalformedURLException murle) {
            DEBUG.error("An error occurred while validating goto URL: " + url, murle);
            return false;
        }
    }

    /**
     * Returns the appopriate redirectUrl, given the options of the provided URL and the default one, as well
     * as a configuration. The configuration determines whether gotoUrl is valid and complies with the
     * configuration's URL policy. If so, the gotoUrl is returned, otherwise the alternateUrl is returned. In the case
     * where the alternateUrl is null, null is returned.
     *
     * @param configInfo Config in the given type which informs the collection of valid domain URLs.
     * @param gotoUrl The goto URL to compare against the config. If null alternateUrl will be returned.
     * @param alternateUrl The URL to default to. May be null.
     * @return The URL to use, or null if no URL is appropriate.
     */
    public String getRedirectUrl(T configInfo, String gotoUrl, String alternateUrl) {

        String returnValue = null;

        if (gotoUrl == null) {
            return alternateUrl;
        }

        if (isRedirectUrlValid(gotoUrl, configInfo)) {
            returnValue = gotoUrl;
        }

        if (returnValue == null || returnValue.isEmpty()) {
            returnValue = alternateUrl;
        }

        return returnValue;
    }

    /**
     * Helper function to retrieve a field from the provided request's query parameters.
     * This exists for the old-style interfaces, and takes the parameter name as an argument also.
     *
     * If the queryParameters contain the "encoded" parameter, then the result is Base64 decoded before
     * being returned.
     *
     * @param request Request containing the parameters to retrieve.
     * @param paramName The name of the parameter whose value to (possibly decode) return.
     * @return The (possibly decoded) value of the paramName's key within the requests's query parameters.
     */
    public String getAndDecodeParameter(HttpServletRequest request, String paramName) {
        String value = request.getParameter(paramName);

        if (value == null) {
            return null;
        }

        String encoded = request.getParameter("encoded");
        if (Boolean.parseBoolean(encoded)) {
            return Base64.decodeAsUTF8String(value);
        } else {
            return value;
        }
    }

    /**
     * Helper function to retrieve a field from the provided request's JSON POST data.
     * This exists for the new-style interfaces, and takes the parameter name as an argument also.
     *
     * @param input JsonValue containing the key "goto" and a paired URL.
     * @param paramName The key whose value to attempt to read.
     * @return The String representation fo the "goto" key's value, or null.
     */
    public String getValueFromJson(JsonValue input, String paramName) {
        if (input == null || !input.isDefined(paramName)) {
            return null;
        }  else {
            return input.get(paramName).asString();
        }
    }

}
