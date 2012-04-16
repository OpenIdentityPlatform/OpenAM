<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: header.jsp,v 1.3 2008/06/25 05:49:24 qcheng Exp $

--%>

<%@ page language="java"
import="java.io.IOException,
        java.net.URLEncoder,
        java.text.MessageFormat,
        com.sun.identity.plugin.session.SessionException,
        com.sun.identity.plugin.session.SessionProvider,
        com.sun.identity.plugin.session.SessionManager,
        com.sun.identity.shared.Constants,
        com.sun.identity.shared.configuration.SystemPropertiesManager"
%>

<%
    boolean loggedIn = false;
    String redirectUrl = null;
    String localAuthUrl = null;

    String baseURL = request.getRequestURI().toString();
    int idx = baseURL.indexOf('/', 1);
    String localProto = request.getScheme();
    String localHost = request.getServerName();
    String localPort = "" + request.getServerPort();
    String localDeploymentURI = baseURL.substring(0, idx);
    baseURL = localProto + "://" + localHost +
        ":" + localPort + localDeploymentURI;
    String realBaseURL =
        SystemPropertiesManager.get(Constants.AM_SERVER_PROTOCOL) + "://" +
        SystemPropertiesManager.get(Constants.AM_SERVER_HOST) + ":" +
        SystemPropertiesManager.get(Constants.AM_SERVER_PORT) +
        SystemPropertiesManager.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);

    try {
        SessionProvider provider = SessionManager.getProvider();
        Object sess = provider.getSession(request);
        if (sess != null) {
            loggedIn = provider.isValid(sess);
        }
    } catch (SessionException e) {
        //ignored
    }

    if(!loggedIn) {
        String gotoUrl = request.getRequestURL().toString();
        String qs = request.getQueryString();
        if ((qs != null) && (qs.length() > 0)) {
            gotoUrl = gotoUrl + "?" + qs;
        }
        redirectUrl = baseURL + "/preLogin?metaAlias={0}&goto=" + 
            URLEncoder.encode(gotoUrl);
        localAuthUrl = baseURL + "/UI/Login?goto=" + URLEncoder.encode(gotoUrl);
    }
%>
