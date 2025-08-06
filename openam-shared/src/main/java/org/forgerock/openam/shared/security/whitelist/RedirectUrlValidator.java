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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.shared.security.whitelist;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.whitelist.URLPatternMatcher;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import jakarta.servlet.http.HttpServletRequest;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.utils.StringUtils;

/**
 * Validates the provided redirect URL against the list of valid goto URL domains.
 *
 * @param <T> The type of the configuration information that is provided to find the collection of valid domains.
 */
public class RedirectUrlValidator<T> {

    /**
     * Go to url query parameter name.
     */
    public final static String GOTO = "goto";

    /**
     * Go to on fail query parameter name.
     */
    public final static String GOTO_ON_FAIL = "gotoOnFail";

    private static final Debug DEBUG = Debug.getInstance("patternMatching");
    private final ValidDomainExtractor<T> domainExtractor;

    private final static String MAX_URL_LENGTH_PROPERTY = "org.forgerock.openam.redirecturlvalidator.maxUrlLength";

    // Default of 2000 comes from the discussion in http://stackoverflow.com/a/417184
    private final static int MAX_URL_LENGTH = SystemPropertiesManager.getAsInt(MAX_URL_LENGTH_PROPERTY, 2000);

    /**
     * Constructs a new RedirectUrlValidator instance.
     *
     * @param domainExtractor A ValidDomainExtractor instance.
     */
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
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        final Collection<String> patterns = domainExtractor.extractValidDomains(configInfo);
        DEBUG.message("RedirectUrlValidator.isRedirectUrlValid: Validating goto URL {} against patterns: {}",
                url, patterns);

        if (url.length() > MAX_URL_LENGTH) {
            DEBUG.message("RedirectUrlValidator.isRedirectUrlValid:"
                    + " The url was length {} which is longer than the allowed maximum of {}",
                    url.length(), MAX_URL_LENGTH);
            return false;
        }

        try {
            final URI uri = new URI(url);
            // Both Absolute and scheme relative URLs should be validated.
            if (!uri.isAbsolute() && !url.startsWith("//")) {
                return true;
            }

            if (uri.getScheme() != null && !uri.getScheme().equals("http") && !uri.getScheme().equals("https")) {
                return false;
            }
        } catch (final URISyntaxException urise) {
            DEBUG.message("RedirectUrlValidator.isRedirectUrlValid: The goto URL {} is not a valid URI", url, urise);
            return false;
        }

        if (patterns == null || patterns.isEmpty()) {
            DEBUG.message("RedirectUrlValidator.isRedirectUrlValid:"
                    + " There are no patterns to validate the URL against, the goto URL {} is considered valid", url);
            return true;
        }

        final URLPatternMatcher patternMatcher = new URLPatternMatcher();
        try {
            return patternMatcher.match(url, patterns, true);
        } catch (MalformedURLException murle) {
            DEBUG.error("RedirectUrlValidator.isRedirectUrlValid: An error occurred while validating goto URL: {}",
                    url, murle);
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
            String decodedParameterValue = Base64.decodeAsUTF8String(value);
            if (decodedParameterValue == null) {
                DEBUG.warning("RedirectUrlValidator.getAndDecodeParameter: "
                        + "As parameter 'encoded' is true, parameter ['{}']='{}' should be base64 encoded",
                        paramName, value);
            }
            return decodedParameterValue;

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
