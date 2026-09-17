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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2025-2026 3A Systems LLC.
 */
package org.forgerock.openam.authentication.modules.saml2;

import static org.forgerock.openam.authentication.modules.saml2.Constants.*;
import static org.forgerock.openam.utils.Time.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.common.annotations.VisibleForTesting;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.openam.saml2.SAML2Store;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.xui.XUIState;
import org.owasp.esapi.ESAPI;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.profile.ResponseInfo;
import com.sun.identity.saml2.profile.SPACSUtils;
import com.sun.identity.saml2.profile.SPCache;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.shared.encode.URLEncDec;

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
     * <p>
     * No longer produced. It was only ever reached when the load balancer cookie bounce fired -
     * which needs a multi server platform and
     * <code>com.sun.identity.federation.cookieHashRedirectEnabled=true</code>, off by default - and
     * that case now answers the request instead of failing it. Kept because it is part of the
     * published error vocabulary and an older server in the same site can still emit it.
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

    private static final Debug DEBUG = Debug.getInstance("amAuthSAML2");

    /**
     * Private, utilities-class constructor.
     */
    private SAML2Proxy() {
    }

    private static String generateKey() {
        return UUID.randomUUID().toString();
    }

    /**
     * Processes the SAML response for the SAML2 authentication module and then directs the user back to the
     * authentication process differently for XUI and non-XUI cases.
     *
     * @param request The HTTP request.
     * @param response The HTTP response.
     * @param out The {@link PrintWriter}.
     * @throws IOException If there was an IO error while retrieving the SAML response.
     */
    public static void processSamlResponse(HttpServletRequest request, HttpServletResponse response, PrintWriter out)
            throws IOException {
        String url = getUrl(request, response);
        if (url == null) {
            // The load balancer cookie bounce has answered the request itself. Writing anything on
            // top of that replaces the answer the browser was supposed to act on.
            DEBUG.message("SAML2Proxy: request bounced for the load balancer cookie, nothing left to write");
            return;
        }
        if (response.isCommitted()) {
            // Anything else that left the response committed has the same consequence: sendRedirect
            // or a form write on a committed response raises an IllegalStateException over whatever
            // the client already received.
            //
            // It does not cover the response SPACSUtils.getResponse has already answered. That one
            // calls SAMLUtils.sendError before every SAML2Exception it throws, and in the default
            // configuration sendError forwards to the relative error page saml2error.jsp, which
            // ends in response.sendError(sc, msg) - the servlet call, which sets the status and
            // suspends the response rather than committing it. So isCommitted() is false here and
            // the error URL getUrl built from that exception still goes out on top of the error
            // page. (The absolute error URL over HTTP-POST arm renders autosubmittingerror.jsp
            // instead and does commit, which this guard does catch.) Closing the first arm wants
            // the same threaded signal the bounce just got, which is a change of its own.
            DEBUG.message("SAML2Proxy: response already committed, not writing {}", url);
            return;
        }
        XUIState xuiState = InjectorHolder.getInstance(XUIState.class);
        if (xuiState.isXUIEnabled()) {
            response.sendRedirect(url);
        } else {
            out.println(getAutoSubmittingFormHtml(url));
        }
    }

    /**
     * Builds the URL the browser has to be sent back to, or <code>null</code> when the load balancer
     * cookie bounce has already answered the request and the caller must not write at all.
     */
    private static String getUrl(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request == null || response == null) {
            DEBUG.error("SAML2Proxy: Null request or response");
            return getUrlWithError(request, BAD_REQUEST);
        }

        try {
            SAMLUtils.checkHTTPContentLength(request);
        } catch (ServletException se) {
            DEBUG.error("SAML2Proxy: content length too large");
            return getUrlWithError(request, BAD_REQUEST);
        }

        if (FSUtils.needSetLBCookieAndRedirect(request, response, false)) {
            // The bounce has answered the request: the POST branch forwarded to the auto submit JSP,
            // the GET branch issued the redirect to ?redirected=1. This signal has to be threaded
            // out rather than inferred from response.isCommitted(), because the GET branch does not
            // necessarily commit - Tomcat's sendRedirect flushes only when the context is configured
            // to send a redirect body, which it is not by default - and an error URL written after
            // it would silently replace the redirect the browser was meant to follow.
            return null;
        }

        // get entity id and orgName
        String requestURL = request.getRequestURL().toString();
        String metaAlias = SAML2MetaUtils.getMetaAliasByUri(requestURL);
        SAML2MetaManager metaManager = SAML2Utils.getSAML2MetaManager();
        String hostEntityId;

        if (metaManager == null) {
            DEBUG.error("SAML2Proxy: Unable to obtain metaManager");
            return getUrlWithError(request, MISSING_META_MANAGER);
        }

        try {
            hostEntityId = metaManager.getEntityByMetaAlias(metaAlias);
            if (hostEntityId == null) {
                throw new SAML2MetaException("Caught Instantly");
            }
        } catch (SAML2MetaException sme) {
            DEBUG.warning("SAML2Proxy: unable to find hosted entity with metaAlias: {} Exception: {}", metaAlias,
                    sme.toString());
            return getUrlWithError(request, META_DATA_ERROR);
        }

        String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);

        if (StringUtils.isEmpty(realm)) {
            realm = "/";
        }

        ResponseInfo respInfo;
        try {
            respInfo = SPACSUtils.getResponse(request, response, realm, hostEntityId, metaManager);
        } catch (SAML2Exception se) {
            DEBUG.error("SAML2Proxy: Unable to obtain SAML response", se);
            return getUrlWithError(request, SAML_GET_RESPONSE_ERROR, se.getL10NMessage(request.getLocale()));
        }

        Map smap;
        try {
            // check Response/Assertion and get back a Map of relevant data
            smap = SAML2Utils.verifyResponse(request, response, respInfo.getResponse(), realm, hostEntityId,
                    respInfo.getProfileBinding());
        } catch (SAML2Exception se) {
            DEBUG.error("SAML2Proxy: An error occurred while verifying the SAML response", se);
            return getUrlWithError(request, SAML_VERIFY_RESPONSE_ERROR, se.getL10NMessage(request.getLocale()));
        }
        String key = generateKey();

        //survival time is one hour

        SAML2ResponseData data = new SAML2ResponseData((String) smap.get(SAML2Constants.SESSION_INDEX),
                (Subject) smap.get(SAML2Constants.SUBJECT),
                (Assertion) smap.get(SAML2Constants.POST_ASSERTION),
                respInfo);

        if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
            try {
                long sessionExpireTime = currentTimeMillis() / 1000 + SPCache.interval; //counted in seconds
                SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(key, data, sessionExpireTime);
            } catch (SAML2TokenRepositoryException e) {
                DEBUG.error("An error occurred while persisting the SAML token", e);
                return getUrlWithError(request, SAML_FAILOVER_DISABLED_ERROR);
            }
        } else {
            SAML2Store.saveTokenWithKey(key, data);
        }

        return getUrlWithKey(request, key);
    }

    /**
     * Auto-submits the passed-in parameters to the authentication module, as taken from
     * a known cookie location in the request.
     *
     * @param req The request.
     * @param key The key to reference.
     * @return An HTML form to render to the user's user-agent.
     */
    protected static String getUrlWithKey(final HttpServletRequest req, final String key) {
        final StringBuilder value = getLocationValue(req);
        if (value == null) {
            throw new IllegalStateException(DEFAULT_ERROR_MESSAGE);
        }
        return encodeMessage(value, key);
    }

    /**
     * Creates a post form for forwarding error response information to the SAML2 authentication module.
     *
     * @param req The request.
     * @param errorType The Error type that has occurred.
     * @return An HTML form to render to the user's user-agent.
     */
    protected static String getUrlWithError(HttpServletRequest req, String errorType) {
        return getUrlWithError(req, errorType, DEFAULT_ERROR_MESSAGE);
    }

    /**
     * Creates a post form for forwarding error response information to the SAML2 authentication module.
     *
     * @param req The request.
     * @param errorType The Error type that has occurred.
     * @param messageDetail a text description of the message.
     * @return An HTML form to render to the user's user-agent.
     */
    protected static String getUrlWithError(HttpServletRequest req, String errorType, String messageDetail) {
        StringBuilder value = getLocationValue(req);

        if (value == null) {
            throw new IllegalStateException(DEFAULT_ERROR_MESSAGE);
        }

        value.append("&").append(ERROR_PARAM_KEY).append("=").append(true)
            .append("&").append(ERROR_CODE_PARAM_KEY).append("=").append(URLEncDec.encode(errorType))
            .append("&").append(ERROR_MESSAGE_PARAM_KEY).append("=").append(URLEncDec.encode(messageDetail));

        return value.toString();
    }

    private static String encodeMessage(StringBuilder value, String key) {
        if (value.toString().contains("?")) {
            value.append("&");
        } else {
            value.append("?");
        }

        value.append(RESPONSE_KEY).append("=").append(URLEncDec.encode(key))
                .append("&").append(ERROR_PARAM_KEY).append("=").append(false);

        return value.toString();
    }

    private static StringBuilder getLocationValue(HttpServletRequest req) {
        String value = CookieUtils.getCookieValueFromReq(req, AM_LOCATION_COOKIE);

        if (StringUtils.isEmpty(value)) {
            return null;
        }

        return new StringBuilder(value);
    }

    /**
     * Returns the HTML for an auto submitting form that will be submitted to the provided Url.
     *
     * @param value The form's action.
     * @return The HTML for the auto submitting form.
     */
    @VisibleForTesting
    protected static String getAutoSubmittingFormHtml(final String value) {
        StringBuilder html = new StringBuilder();

        html.append("<html>\n").append("<body onLoad=\"").append("document.postform.submit()").append("\">\n");
        html.append("<form name=\"postform\" action=\"").append(ESAPI.encoder().encodeForHTMLAttribute(value))
            .append("\" method=\"post\"").append(">\n");
        html.append("<noscript>\n<center>\n");
        html.append("<p>Your browser does not have JavaScript enabled, ");
        html.append("you must click the button below to continue</p>\n");
        html.append("<input type=\"submit\" value=\"submit\" />\n");
        html.append("</center>\n</noscript>\n").append("</form>\n").append("</body>\n").append("</html>\n");

        return html.toString();
    }
}
