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
 * $Id: PermissionFilter.java,v 1.2 2009/06/09 22:40:36 farble1670 Exp $
 */

package com.sun.identity.admin;

import com.sun.identity.admin.dao.PermissionDao;
import com.sun.identity.admin.dao.RealmDao;
import com.sun.identity.admin.model.PermissionsBean;
import com.sun.identity.admin.model.RealmBean;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletResponse;
import com.sun.identity.admin.model.ViewId;

public class PermissionFilter implements Filter {
    private FilterConfig filterConfig = null;

    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (isAllowed(httpRequest, httpResponse)) {
            chain.doFilter(request, response);
        } else {
            String deniedUrl = getDeniedUrl(httpRequest);
            httpResponse.sendRedirect(deniedUrl);
        }
    }

    private boolean isAllowed(HttpServletRequest request, HttpServletResponse response) {
        String viewId = request.getServletPath();
        ViewId vid = ViewId.valueOfId(viewId);
        if (vid == null) {
            return false;
        }

        if (vid == ViewId.PERMISSION_DENIED) { 
            return true;
        }

        ManagedBeanResolver mbr = new ManagedBeanResolver(filterConfig.getServletContext(), request, response);
        PermissionsBean psb = (PermissionsBean)mbr.resolve("permissionsBean");

        return psb.isViewAllowed(vid);
    }

    private String getDeniedUrl(HttpServletRequest request) {
        StringBuffer b = new StringBuffer();

        String scheme = request.getScheme();
        String server = request.getServerName();
        int port = request.getServerPort();
        String path = request.getContextPath();

        b.append(scheme);
        b.append("://");
        b.append(server);
        b.append(":");
        b.append(port);
        b.append(path);

        b.append(ViewId.PERMISSION_DENIED.getId());

        return b.toString();
    }

    public FilterConfig getFilterConfig() {
        return (this.filterConfig);
    }

    public void setFilterConfig(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    public void destroy() {
    }

    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }
}
