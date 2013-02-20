<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
  
   The contents of this file are subject to the terms
   of the Common Development and Distribution License
   (the License). You may not use this file except in
   compliance with the License.

   You can obtain a copy of the License at
   https://opensso.dev.java.net/public/CDDLv1.0.html or
   opensso/legal/CDDLv1.0.txt
   See the License for the specific language governing
   permission and limitations under the License.

   When distributing Covered Code, include this CDDL
   Header Notice in each file and include the License file
   at opensso/legal/CDDLv1.0.txt.
   If applicable, add the following below the CDDL Header,
   with the fields enclosed by brackets [] replaced by
   your own identifying information:
   "Portions Copyrighted [year] [name of copyright owner]"

   $Id: proxy.jsp,v 1.5 2009/06/04 11:49:25 veiming Exp $

--%>


<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.sun.identity.admin.Scraper" %>
<%@page import="com.sun.identity.admin.Resources" %>
<%@page import="com.sun.identity.admin.Functions" %>
<%@page import="java.io.IOException" %>

<%
        String url;
        Scraper s;
        Exception ex = null;
        String result = null;

        url = request.getParameter("url");
        s = new Scraper(url);
        try {
            result = s.scrape();
        } catch (IOException ioe) {
            ex = ioe;
        }

        if (result == null || ex != null) {
            String localUri = request.getParameter("localUri");

            if (localUri != null) {
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
                b.append(localUri);

                url = b.toString();
                
                s = new Scraper(url);
                try {
                    result = s.scrape();
                } catch (IOException ioe) {
                    ex = ioe;
                }
            }
        }

        if (result == null) {
            Resources r = new Resources(request);
            result = r.getString(Functions.class, "scrapeError", ex);
        }
%>
<%= result != null ? result : ""%>
