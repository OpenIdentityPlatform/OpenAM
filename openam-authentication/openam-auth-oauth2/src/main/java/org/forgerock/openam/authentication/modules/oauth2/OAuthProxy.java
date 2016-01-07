/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2016 ForgeRock AS.
 * Copyright 2011 Cybernetica AS.
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
 *
 */
package org.forgerock.openam.authentication.modules.oauth2;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Map;

import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.shared.encode.CookieUtils;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.xui.XUIState;
import org.owasp.esapi.ESAPI;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.*;

/*
 * OAuth module specific Get2Post gateway. 
 * When using the legacy authentication UI we need to transform the incoming GET request (from the OAuth2 AS) to a POST
 * request, so that the authentication framework can remain to use the same authentication session for the corresponding
 * authentication attempt (this is because GET requests made to /UI/Login always result in a new authentication
 * session).
 * The XUI on the other hand currently does not have a way to continue an existing authentication process, hence the
 * OAuth module currently just redirects to /openam retaining the query string that was used to access the
 * OAuthProxy.jsp. Since performing a POST request against a static resource can result in HTTP 405 (e.g. on WildFly),
 * in case XUI is enabled we perform a redirect instead to "continue" the authentication process.
 */
public class OAuthProxy  {

    public static void continueAuthentication(HttpServletRequest req, HttpServletResponse res, PrintWriter out) {
        OAuthUtil.debugMessage("toPostForm: started");

        String action = OAuthUtil.findCookie(req, COOKIE_ORIG_URL);
        
        if (OAuthUtil.isEmpty(action)) {
            out.println(getError("Request not valid !"));
            return;
        }

        Map<String, String[]> params = req.getParameterMap();
        
        if (!params.containsKey(PARAM_CODE) && !params.containsKey(PARAM_ACTIVATION)) {
            OAuthUtil.debugError("OAuthProxy.toPostForm: Parameters " + PARAM_CODE + " or " + PARAM_ACTIVATION
                    + " were not present in the request");
            out.println(getError("Request not valid, perhaps a permission problem"));
            return;
        }
        
        StringBuilder html = new StringBuilder();

        try {
            String code = req.getParameter(PARAM_CODE);
            if (code != null && !OAuthUtil.isEmpty(code)) {
                if (!ESAPI.validator().isValidInput(PARAM_CODE, code, "HTTPParameterValue", 2000, true)) {
                    OAuthUtil.debugError("OAuthProxy.toPostForm: Parameter " + PARAM_CODE
                            + " is not valid!! : " + code);
                    out.println(getError("Invalid authorization code"));
                    return;
                }
            }
            if (action.contains("?")) {
                action += "&" + req.getQueryString();
            } else {
                action += "?" + req.getQueryString();
            }

            XUIState xuiState = InjectorHolder.getInstance(XUIState.class);
            if (xuiState.isXUIEnabled()) {
                // OAuthProxy.jsp should be always accessed via GET, hence the querystring should contain all important
                // parameters already.
                res.sendRedirect(action);
                return;
            } else {
                action = ESAPI.encoder().encodeForHTMLAttribute(action);

                String onLoad = "document.postform.submit()";

                html.append("<html>\n").append("<body onLoad=\"")
                        .append(onLoad).append("\">\n");
                html.append("<form name=\"postform\" action=\"")
                        .append(action).append("\" method=\"post\">\n");

                String activation = req.getParameter(PARAM_ACTIVATION);
                if (activation != null && !OAuthUtil.isEmpty(activation)) {
                    if (ESAPI.validator().isValidInput(PARAM_ACTIVATION, activation,
                            "HTTPParameterValue", 512, true)) {
                        html.append(input(PARAM_ACTIVATION, activation));
                    } else {
                        OAuthUtil.debugError("OAuthProxy.toPostForm: Parameter " + PARAM_ACTIVATION
                                + " is not valid!! : " + activation);
                        out.println(getError("Request not valid"));
                        return;
                    }
                }
            }
        } catch (Exception e) {
            out.println(getError(e.getMessage()));
            return;
        }

        html.append("<noscript>\n<center>\n");
        html.append("<p>Your browser does not have JavaScript enabled, you must click"
                + " the button below to continue</p>\n");
        html.append("<input type=\"submit\" value=\"submit\" />\n");
        html.append("</center>\n</noscript>\n");
        html.append("</form>\n").append("</body>\n").append("</html>\n");

        OAuthUtil.debugMessage("OAuthProxy.toPostForm: form html:\n" + html);

        OAuthUtil.debugMessage("OAuthProxy.toPostForm: removing cookie " + COOKIE_ORIG_URL);

        for (String cookieDomain : AuthClientUtils.getCookieDomainsForRequest(req)) {
            CookieUtils.addCookieToResponse(res, CookieUtils.newCookie(COOKIE_ORIG_URL, "", 0, "/", cookieDomain));
        }
        out.println(html.toString());
    }
   
    private static StringBuilder input(String name, String value) {
        return new StringBuilder()
            .append("<input type=\"hidden\" name=\"")
            .append(name).append("\" value=\"")
            .append(value).append("\"/>\n");
    }
 
    private static String getError(String message) {
        StringBuffer html = new StringBuffer();
        html.append("<html>\n").append("<body>\n")
            .append("<h1>\n").append(message).append("</h1>\n")
            .append("</body>\n").append("</html>\n");
        return html.toString();
    }
}
