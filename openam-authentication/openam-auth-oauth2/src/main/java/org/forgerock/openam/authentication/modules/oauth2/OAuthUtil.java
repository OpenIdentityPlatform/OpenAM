/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2014 ForgeRock AS. All rights reserved.
 * Copyright © 2011 Cybernetica AS.
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
 * Portions Copyrighted 2025-2026 3A Systems LLC.
 */

package org.forgerock.openam.authentication.modules.oauth2;

import jakarta.servlet.http.HttpServletRequest;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.CookieUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import org.forgerock.openam.shared.security.whitelist.FailSafeRedirectUrlValidator;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.*;

public class OAuthUtil  {

    private static Debug debug = Debug.getInstance("amAuth");

    public static String findCookie(HttpServletRequest request, String cookieName) {

        String result = "";
        String value = CookieUtils.getCookieValueFromReq(request, cookieName);
        if (value != null) {
            result = value;
            debugMessage("OAuthUtil.findCookie()" + "Cookie "
                        + cookieName
                        + " found. "
                        + "Content is: " + value);
        }

        return result;
    }
    
    public static String getParamValue(String query, String param) {

        String paramValue = "";
        if (query != null && query.length() != 0) {
            String[] paramsArray = query.split("\\&");
            for (String parameter : paramsArray) {
                if (parameter.startsWith(param)) {
                    paramValue = parameter.substring(parameter.indexOf("=") + 1);
                    break;
                }
            }
        }
        return paramValue;
    }
    
    static boolean isEmpty(String value) {
        return value == null || "".equals(value);
    }
    
    public static void sendEmail(String from, String emailAddress, String activCode,
              Map<String, String> SMTPConfig, ResourceBundle bundle, String linkURL)
    throws NoEmailSentException {
        try {
            String gatewayEmailImplClass = SMTPConfig.get(KEY_EMAIL_GWY_IMPL);
            if (from != null || emailAddress != null) {
                // String from = bundle.getString(MESSAGE_FROM);
                String subject = bundle.getString(MESSAGE_SUBJECT);
                String message = bundle.getString(MESSAGE_BODY);
                message = message.replace("#ACTIVATION_CODE#", activCode);
                
                String link = "";
                try {
                     link = linkURL + "?" + PARAM_ACTIVATION + "=" +
                     URLEncoder.encode(activCode, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                   debugError("OAuthUtil.sendEmail(): Error while encoding", ex);
                }

                message = message.replace("#ACTIVATION_LINK#", link.toString());
                EmailGateway gateway =  Class.forName(gatewayEmailImplClass).
                        asSubclass(EmailGateway.class).newInstance();
                gateway.sendEmail(from, emailAddress, subject, message, SMTPConfig);
                debugMessage("OAuthUtil.sendEmail(): sent email to " +
                            emailAddress);
            } else {
                  debugMessage("OAuthUtil.sendEmail(): unable to send email");

            }
        } catch (ClassNotFoundException cnfe) {
            debugError("OAuthUtil.sendEmail(): " + "class not found " +
                        "EmailGateway class", cnfe);
        } catch (InstantiationException ie) {
            debugError("OAuthUtil.sendEmail(): " + "can not instantiate " +
                        "EmailGateway class", ie);
        } catch (IllegalAccessException iae) {
            debugError("OAuthUtil.sendEmail(): " + "can not access " +
                        "EmailGateway class", iae);
        }
    }
    
    public static boolean debugMessageEnabled() {
        return debug.messageEnabled();
    } 
    
    public static void debugMessage(String message) {
        if (debug.messageEnabled()) {
            debug.message(message);
        }
    }

    public static void debugWarning(String message) {
        if (debug.warningEnabled()) {
            debug.warning(message);
        }
    }
    
    public static void debugError(String message, Throwable t) {
        if (debug.errorEnabled()) {
            debug.error(message, t);
        }
    }
    
    public static void debugError(String message) {
        if (debug.errorEnabled()) {
            debug.error(message);
        }
    }
    
    /**
     * Returns the host named by a URL.
     *
     * <p>Scanning for <code>"//"</code> does not answer this: it is the delimiter of an authority only at the
     * start of one, so an empty path segment in a relative URL - <code>/openam/oauth2/logout//x</code> - reads
     * as a host, and an absolute URL that names no authority - <code>https:/elsewhere/x</code> - reads as
     * naming none.
     *
     * @param url The URL to read the host from, may be <code>null</code>.
     * @return The host, or <code>null</code> both where the URL names no authority and where it names one this
     * cannot read. Either way the host of the request is the only one left to name, so the caller does not have
     * to tell the two apart - but nothing is guessed out of an authority that is not a plain host and port.
     */
    public static String getUrlHost(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException urise) {
            // Not the URL itself: it is a request parameter, and a CR or LF in it would forge log records.
            debugWarning("OAuthUtil.getUrlHost(): the logout URL is not a valid URI");
            return null;
        }
        // A server based authority names its host even behind userinfo, which FailSafeRedirectUrlValidator refuses
        // outright. The name is only shown to the user, after that validator has accepted the URL.
        String host = uri.getHost();
        if (host != null) {
            return host;
        }
        // java.net.URI leaves getHost() null for an authority it will not parse as server based - a hostname
        // holding an underscore, for one. Such an authority is read exactly as FailSafeRedirectUrlValidator reads
        // it, raw and only where it is an ASCII host and an optional numeric port: its percent decoded form can
        // spell a different host than the one a browser resolves.
        String authority = FailSafeRedirectUrlValidator.getBareAuthority(uri);
        if (authority == null) {
            return null;
        }
        int colon = authority.indexOf(':');
        return colon < 0 ? authority : authority.substring(0, colon);
    }

    public static String oAuthEncode(String toEncode) throws UnsupportedEncodingException {
        if (toEncode != null && !toEncode.isEmpty()) {
            return URLEncoder.encode(toEncode, "UTF-8").
                    replace("+", "%20").
                    replace("*", "%2A").
                    replace("%7E", "~");

        } else {
            return "";
        }
    }
            
}
