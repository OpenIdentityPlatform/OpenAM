/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
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
 * $Id: SSOTokenAuthZ.java,v 1.4 2009/12/11 09:24:42 veiming Exp $
 */

package com.sun.identity.rest.spi;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.rest.ISubjectable;
import com.sun.identity.rest.RestException;
import com.sun.identity.rest.RestServiceManager;
import com.sun.identity.shared.encode.Hash;
import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author dennis
 */
public class SSOTokenAuthZ implements IAuthorization {
    private static Map<String, String> mapMethodToAction =
        new HashMap<String, String>();

    static {
        mapMethodToAction.put("GET", "READ");
        mapMethodToAction.put("DELETE", "MODIFY");
        mapMethodToAction.put("POST", "MODIFY");
        mapMethodToAction.put("PUT", "MODIFY");
    }

    public String[] accept() {
        String[] method = { RestServiceManager.DEFAULT_AUTHZ_SCHEME };
        return method;
    }

    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain
    ) throws IOException, ServletException {
        int statusCode = HttpServletResponse.SC_OK;
        String statusMessage = null;

        Principal clientPrincipal = ((HttpServletRequest)request).
            getUserPrincipal();
        if (clientPrincipal instanceof ISubjectable) {
            try {
                Subject clientSubject =
                    ((ISubjectable) clientPrincipal).createSubject();
                DelegationEvaluator eval = new DelegationEvaluator();
                SSOToken token = SubjectUtils.getSSOToken(clientSubject);

                String action = mapMethodToAction.get((
                    (HttpServletRequest) request).getMethod());
                if (action == null) {
                    statusCode = HttpServletResponse.SC_UNAUTHORIZED;
                    statusMessage = "Unable to get HTTP method for request.";
                } else {
                    Set<String> setAction = new HashSet<String>();
                    setAction.add(action);
                    DelegationPermission permission = new DelegationPermission(
                        "/", "sunEntitlementService", "1.0", "application",
                        getURI(request), setAction, null);
                    if (!eval.isAllowed(token, permission,
                        Collections.EMPTY_MAP)) {
                        statusCode = HttpServletResponse.SC_UNAUTHORIZED;
                        statusMessage = "Unauthorized.";
                    }
                }
            } catch (Exception e) {
                statusCode = HttpServletResponse.SC_UNAUTHORIZED;
                statusMessage = e.getMessage();
            }
        } else {
            statusCode = HttpServletResponse.SC_UNAUTHORIZED;
            statusMessage = "Unable to obtain subject.";
        }

        if (statusCode == HttpServletResponse.SC_OK) {
            statusCode = validateTokenId((HttpServletRequest) request);

            if (statusCode == HttpServletResponse.SC_OK) {
                chain.doFilter(request, response);
            } else {
                statusMessage = "SSO token is invalid or has expired.";
            }
        }
        
        if (statusCode != HttpServletResponse.SC_OK) {
            ((HttpServletResponse)response).sendError(statusCode, 
                statusMessage);
        }
    }

    private String getURI(ServletRequest req) {
        String uri = ((HttpServletRequest)req).getRequestURI();
        int idx = uri.indexOf('/', 1);
        return (idx != -1) ? uri.substring(idx+1) : uri;
    }

    private int validateTokenId(HttpServletRequest request)
        throws ServletException, IOException {
        String tokenId = request.getHeader(
            RestServiceManager.SUBJECT_HEADER_NAME);
        String hashed = request.getParameter(
            RestServiceManager.HASHED_SUBJECT_QUERY);

        if (((tokenId == null) || (tokenId.trim().length() == 0)) &&
            ((hashed == null) || (hashed.trim().length() == 0))) {
            // by pass the check
            return HttpServletResponse.SC_OK;
        }

        if ((tokenId == null) || (tokenId.trim().length() == 0)) {
            try {
                SSOTokenManager mgr = SSOTokenManager.getInstance();
                SSOToken token = mgr.createSSOToken(request);
                tokenId = token.getTokenID().toString();
            } catch (SSOException e) {
                return HttpServletResponse.SC_UNAUTHORIZED;
            }
        }

        if (!Boolean.parseBoolean(SystemProperties.get(
            RestServiceManager.DISABLE_HASHED_SUBJECT_CHECK, "false"))) {
            if ((hashed == null) || (hashed.trim().length() == 0)) {
                return HttpServletResponse.SC_UNAUTHORIZED;
            } else {
                int idx = tokenId.indexOf(':');
                if (idx != -1) {
                    tokenId = tokenId.substring(idx + 1);
                }
                if (!Hash.hash(tokenId).equals(hashed)) {
                    return HttpServletResponse.SC_UNAUTHORIZED;
                }
            }
        }

        return HttpServletResponse.SC_OK;
    }

    public void init(FilterConfig arg0) throws ServletException {
    }

    public void destroy() {
    }

    public Subject getAuthZSubject(HttpServletRequest req)
        throws RestException {
        try {
            String tokenId = req.getHeader(
                RestServiceManager.SUBJECT_HEADER_NAME);

            if ((tokenId == null) || (tokenId.trim().length() == 0)) {
                SSOTokenManager mgr = SSOTokenManager.getInstance();
                SSOToken token = mgr.createSSOToken(req);
                return SubjectUtils.createSubject(token);
            } else {
                int idx = tokenId.indexOf(':');
                if (idx != -1) {
                    tokenId = tokenId.substring(idx + 1);
                }
                SSOTokenManager mgr = SSOTokenManager.getInstance();
                SSOToken token = mgr.createSSOToken(tokenId);
                return SubjectUtils.createSubject(token);
            }
        } catch (SSOException ex) {
            throw new RestException(1, ex);
        }
    }
}
