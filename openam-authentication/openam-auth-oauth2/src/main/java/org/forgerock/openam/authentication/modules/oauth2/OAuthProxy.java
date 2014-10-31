/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2014 ForgeRock AS.
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
import java.util.Map;
import org.owasp.esapi.ESAPI;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.*;


/*
 * OAuth module specific Get2Post gateway. 
 * In some conditions OpenAM would prefer POST method over GET.
 * OAuthProxy is more like workaround over some specific scenarios,
 * that did not work. OAuthProxy may not be needed with future
 * versions of OpenAM.
 */
public class OAuthProxy  {


    public static String toPostForm(HttpServletRequest req,
            HttpServletResponse res) {

            OAuthUtil.debugMessage("toPostForm: started");

 
        String action = OAuthUtil.findCookie(req, COOKIE_ORIG_URL);
        
        if (OAuthUtil.isEmpty(action)) {
            return getError("Request not valid !");
        }

        Map<String, String[]> params = req.getParameterMap();
        
        if (!params.keySet().contains(PARAM_CODE)
                && !params.keySet().contains(PARAM_ACTIVATION)) {
            OAuthUtil.debugError("OAuthProxy.toPostForm: Parameters " + PARAM_CODE
                    + " or " + PARAM_ACTIVATION + " were not present in the request");
            return getError("Request not valid, perhaps a permission problem");
        }
        
        StringBuilder html = new StringBuilder();
        
        try {
            String code = req.getParameter(PARAM_CODE);
            if (code != null && !OAuthUtil.isEmpty(code)) {
                if (!ESAPI.validator().isValidInput(PARAM_CODE, code, "HTTPParameterValue", 512, true)) {
                    OAuthUtil.debugError("OAuthProxy.toPostForm: Parameter " + PARAM_CODE
                            + " is not valid!! : " + code);
                    return getError("Request not valid");
                }
            }
            if (action.contains("?")) {
                action += "&" + req.getQueryString();
            } else {
                action += "?" + req.getQueryString();
            }

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
                    return getError("Request not valid");
                }
            }
            
        } catch (Exception e) {
            return getError(e.getMessage());
        }
        
        html.append("<noscript>\n<center>\n");
        html.append("<p>Your browser does not have JavaScript enabled, you must click"
                + " the button below to continue</p>\n");
        html.append("<input type=\"submit\" value=\"submit\" />\n");
        html.append("</center>\n</noscript>\n");
        html.append("</form>\n").append("</body>\n").append("</html>\n");

        OAuthUtil.debugMessage("OAuthProxy.toPostForm: form html:\n" + html);

        return html.toString();
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
