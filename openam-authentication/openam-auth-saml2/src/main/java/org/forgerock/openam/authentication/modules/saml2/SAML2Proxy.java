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
package org.forgerock.openam.authentication.modules.saml2;

import static org.forgerock.openam.authentication.modules.saml2.Constants.*;

import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.shared.encode.URLEncDec;
import javax.servlet.http.HttpServletRequest;
import org.forgerock.openam.utils.StringUtils;
import org.owasp.esapi.ESAPI;

/**
 * Called on the way back into the SAML2 Authentication Module
 * by the saml2AuthAssertionConsumer jsp.
 */
public final class SAML2Proxy {

    /**
     * Key for looking up the response key in the http query string.
     */
    public static final String RESPONSE_KEY = "responsekey";
    /**
     * Default message to display when we can't even forward an error back to the Authentication Module.
     */
    public static final String DEFAULT_ERROR_MESSAGE = "Request not valid!";
    /**
     * Constant to indicate the Proxy received a bad request.
     */
    public static final String BAD_REQUEST = "badRequest";
    /**
     * Constant to indicate the Proxy did not find the expected cookie.
     */
    public static final String MISSING_COOKIE = "missingCookie";
    /**
     * Constant to indicate that the proxy was unable to locate the meta manager.
     */
    public static final String MISSING_META_MANAGER = "missingMeta";
    /**
     * Constant to indicate that the proxy was unable to extract data from the meta manager.
     */
    public static final String META_DATA_ERROR = "metaError";
    /**
     * Constant to indicate that the proxy was unable to extract the SAML response.
     */
    public static final String SAML_GET_RESPONSE_ERROR = "samlGet";
    /**
     * Constant to indicate that the proxy was unable to verify the response.
     */
    public static final String SAML_VERIFY_RESPONSE_ERROR = "samlVerify";
    /**
     * Constant to indicate that SAML failover was not enabled as required.
     */
    public static final String SAML_FAILOVER_DISABLED_ERROR = "samlFailover";
    /**
     * Key for looking up the boolean error state from the http query string.
     */
    public static final String ERROR_PARAM_KEY = "error";
    /**
     * Key for looking up the error type from the http query string.
     */
    public static final String ERROR_CODE_PARAM_KEY = "errorCode";
    /**
     * Key for looking up the error message from the http query string.
     */
    public static final String ERROR_MESSAGE_PARAM_KEY = "errorMessage";

    /**
     * Private, utilities-class constructor.
     */
    private SAML2Proxy() {
    }

    /**
     * Auto-submits the passed-in parameters to the authentication module, as taken from
     * a known cookie location in the request.
     *
     * @param req The request.
     * @param key The key to reference.
     * @return An HTML form to render to the user's user-agent.
     */
    public static String toPostForm(final HttpServletRequest req, final String key) {
        final StringBuilder value = checkRequest(req);
        if (value == null) {
            return getError(DEFAULT_ERROR_MESSAGE).toString();
        }
        return getAutoSubmittingFormHtml(encodeMsg(value, key));
    }

    /**
     * Creates a post form for forwarding error response information to the SAML2 authentication module.
     *
     * @param req The request.
     * @param errorType The Error type that has occurred.
     * @return An HTML form to render to the user's user-agent.
     */
    public static String toPostWithErrorForm(HttpServletRequest req, String errorType) {
        return toPostWithErrorForm(req, errorType, DEFAULT_ERROR_MESSAGE);
    }

    /**
     * Creates a post form for forwarding error response information to the SAML2 authentication module.
     *
     * @param req The request.
     * @param errorType The Error type that has occurred.
     * @param messageDetail a text description of the message.
     * @return An HTML form to render to the user's user-agent.
     */
    public static String toPostWithErrorForm(HttpServletRequest req, String errorType, String messageDetail) {
        StringBuilder value = checkRequest(req);

        if (value == null) {
            return getError(DEFAULT_ERROR_MESSAGE).toString();
        }

        value.append("&").append(ERROR_PARAM_KEY).append("=").append(true)
            .append("&").append(ERROR_CODE_PARAM_KEY).append("=").append(URLEncDec.encode(errorType))
            .append("&").append(ERROR_MESSAGE_PARAM_KEY).append("=").append(URLEncDec.encode(messageDetail));

        return getAutoSubmittingFormHtml(value.toString());
    }

    private static StringBuilder getError(String message) {
        StringBuilder html = new StringBuilder();
        html.append("<html>\n").append("<body>\n")
                .append("<h1>\n").append(ESAPI.encoder().encodeForHTML(message)).append("</h1>\n")
                .append("</body>\n").append("</html>\n");
        return html;
    }

    private static String encodeMsg(StringBuilder value, String key) {

        if (value.toString().contains("?")) {
            value.append("&");
        } else {
            value.append("?");
        }

        value.append(RESPONSE_KEY).append("=").append(URLEncDec.encode(key))
                .append("&").append(ERROR_PARAM_KEY).append("=").append(false);

        return value.toString();
    }

    private static StringBuilder checkRequest(HttpServletRequest req) {
        String value = CookieUtils.getCookieValueFromReq(req, AM_LOCATION_COOKIE);

        if (StringUtils.isEmpty(value)) {
            return null;
        }

        return new StringBuilder(value);
    }

    private static String getAutoSubmittingFormHtml(final String value) {
        StringBuilder html = new StringBuilder();

        html.append("<html>\n").append("<body onLoad=\"").append("document.postform.submit()").append("\">\n");
        html.append("<form name=\"postform\" action=\"").append(ESAPI.encoder().encodeForHTMLAttribute(value))
            .append("\" method=\"post\"").append(">\n");
        html.append("<noscript>\n<center>\n");
        html.append("<p>Your browser does not have JavaScript enabled, you must click"
                + " the button below to continue</p>\n");

        html.append("</center>\n</noscript>\n").append("</form>\n").append("</body>\n").append("</html>\n");

        return html.toString();
    }

}
