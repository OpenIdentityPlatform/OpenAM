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
 * $Id: RestServiceManager.java,v 1.1 2009/11/12 18:37:35 veiming Exp $
 */

package com.sun.identity.rest;

import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.rest.spi.IAuthentication;
import com.sun.identity.rest.spi.IAuthorization;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.StringTokenizer;
import javax.security.auth.Subject;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author dennis
 */
public class RestServiceManager {
    private static RestServiceManager serviceManager = new RestServiceManager();

    // TOFIX: make DEFAULT_AUTHN_SCHEME configurable
    public static final String DEFAULT_AUTHN_SCHEME = "sstoken";
    public static final String SUBJECT_HEADER_NAME = "X-Query-Parameters";
    public static final String HASHED_SUBJECT_QUERY = "subject";
    public static final String SSOTOKEN_SUBJECT_PREFIX = "ssotoken";
    public static final String SUBJECT_DELIMITER = ":";
    public static final String DISABLE_HASHED_SUBJECT_CHECK =
        "rest.disable.hashed.subject.validation";

    // TOFIX: make DEFAULT_AUTHZ_SCHEME configurable
    public static final String DEFAULT_AUTHZ_SCHEME = SSOTOKEN_SUBJECT_PREFIX;
    
    private Map<String, IAuthentication> authNServices = new
        HashMap<String, IAuthentication>();
    private Map<String, IAuthorization> authZServices = new
        HashMap<String, IAuthorization>();
    
    private RestServiceManager() {
    }

    public static RestServiceManager getInstance() {
        return serviceManager;
    }

    public synchronized void destroy() {
        for (IAuthentication auth : authNServices.values()) {
            try {
                auth.destroy();
            } catch (Exception e) {
                // catch all exception, so that all auth filters have
                // the chance to shutdown.
                PrivilegeManager.debug.error("AuthNFilter.destroy", e);
            }
        }
        authNServices.clear();

        for (IAuthorization auth : authZServices.values()) {
            try {
                auth.destroy();
            } catch (Exception e) {
                // catch all exception, so that all auth filters have
                // the chance to shutdown.
                PrivilegeManager.debug.error("AuthZFilter.destroy", e);
            }
        }
        authZServices.clear();
    }

    public void initAuthN(FilterConfig config) {
        ServiceLoader<IAuthentication> filters = ServiceLoader.load(
            IAuthentication.class);
        for (IAuthentication p : filters) {
            try {
                p.init(config);
                String[] acceptMtd = p.accept();
                for (int i = 0; i < acceptMtd.length; i++) {
                    authNServices.put(acceptMtd[i], p);
                }
            } catch (Exception e) {
                // catch all exception, so that all auth filters have
                // the chance to registered
                PrivilegeManager.debug.error("ServiceManager.initAuthN", e);
            }
        }
    }

    public void initAuthZ(FilterConfig config) {
        ServiceLoader<IAuthorization> filters = ServiceLoader.load(
            IAuthorization.class);
        for (IAuthorization p : filters) {
            try {
                p.init(config);
                String[] acceptMtd = p.accept();
                for (int i = 0; i < acceptMtd.length; i++) {
                    authZServices.put(acceptMtd[i], p);
                }
            } catch (Exception e) {
                // catch all exception, so that all auth filters have
                // the chance to registered
                PrivilegeManager.debug.error("AuthZFilter.init", e);
            }
        }

    }

    public IAuthentication getAuthenticationFilter(HttpServletRequest req) {
        String acceptAuth = req.getHeader("X-Accept-Authentication");
        if (acceptAuth == null) {
            return authNServices.get(DEFAULT_AUTHN_SCHEME);
        }

        StringTokenizer st = new StringTokenizer(acceptAuth, ",");
        while (st.hasMoreTokens()) {
            String mtd = st.nextToken();
            IAuthentication auth = authNServices.get(mtd);
            if (auth != null) {
                return auth;
            }
        }

        return null;
    }

    public IAuthorization getAuthorizationFilter(HttpServletRequest req) {
        String subjectHeader = req.getHeader(SUBJECT_HEADER_NAME);
        if (subjectHeader == null) {
            return authZServices.get(DEFAULT_AUTHZ_SCHEME);
        }

        int idx = subjectHeader.indexOf(SUBJECT_DELIMITER);
        if (idx == -1) {
            return authZServices.get(DEFAULT_AUTHZ_SCHEME);
        }

        String schema = subjectHeader.substring(0, idx);
        return authZServices.get(schema);
    }

    public Subject getAuthZSubject(HttpServletRequest request)
        throws RestException {
        IAuthorization authz =  getAuthorizationFilter(request);
        return authz.getAuthZSubject(request);
    }
}
