/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: ECPFilter.java,v 1.1 2007/10/04 16:55:28 hengming Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.saml2.profile.SPSSOFederate;
import com.sun.identity.shared.debug.Debug;

public final class ECPFilter implements Filter {

    public static Debug debug = Debug.getInstance("ECPFilter");

    /**
     * Redirects request to configuration page if the product is not yet 
     * configured.
     *
     * @param request Servlet Request.
     * @param response Servlet Response.
     * @param filterChain Filter Chain.
     * @throws IOException if configuration file cannot be read.
     * @throws ServletException if there are errors in the servlet space.
     */
    public void doFilter(
        ServletRequest request, 
        ServletResponse response, 
        FilterChain filterChain
    ) throws IOException, ServletException 
    {
        HttpServletRequest  httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response ;

        if (!SPSSOFederate.isFromECP(httpRequest)) {
            filterChain.doFilter(httpRequest, httpResponse);
            if (debug.messageEnabled()) {
                debug.message("ECPFilter.doFilter: not from ECP. request = " +
                    httpRequest.getRequestURL());
            }  
            return;
        }
        if (debug.messageEnabled()) {
            debug.message("ECPFilter.doFilter: from ECP. request = " +
                httpRequest.getRequestURL());
        }  

        Object session = null;

        try {
            session = SessionManager.getProvider().getSession(httpRequest);
        } catch (SessionException se) {
            if (debug.messageEnabled()) {
                debug.message("ECPFilter.doFilter:", se);
            }
        }

        try {
            if (session != null) {
                if (debug.messageEnabled()) {
                    debug.message("ECPFilter.doFilter: session found");
                }  


                filterChain.doFilter(httpRequest, httpResponse);
            } else {
                String resource = "/SPECP?metaAlias=/sp&RelayState=" +
                    httpRequest.getRequestURL();
                if (debug.messageEnabled()) {
                    debug.message(
                        "ECPFilter.doFilter: Forwarding to :" + resource);
                }  
                RequestDispatcher dispatcher = 
                    request.getRequestDispatcher(resource);
                dispatcher.forward(request, response);

            }
        } catch(Exception ex) {
            throw new ServletException("ECPFilter.doFilter", ex);
        }
    }

    /**
     * Initializes the filter.
     *
     * @param filterConfig Filter Configuration.
     */
    public void init(FilterConfig filterConfig) {
    }

    public void destroy() {
    }
}
