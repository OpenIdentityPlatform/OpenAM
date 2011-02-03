<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: header.jsp,v 1.5 2008/06/25 05:49:26 qcheng Exp $

--%>

<%@ page language="java"
import="java.io.IOException,
        java.net.URLEncoder,
        java.text.MessageFormat,
        com.sun.identity.plugin.session.SessionException,
        com.sun.identity.plugin.session.SessionProvider,
        com.sun.identity.plugin.session.SessionManager,
        com.sun.identity.shared.Constants,
        com.sun.identity.shared.configuration.SystemPropertiesManager,
        com.sun.identity.cot.CircleOfTrustDescriptor,
        com.sun.identity.cot.CircleOfTrustManager,
        com.sun.identity.cot.COTConstants,
        com.sun.identity.cot.COTException,
        java.util.HashSet,
        java.util.List,
        java.util.Set"

%>

<%
    boolean loggedIn = false;
    String redirectUrl = null;
    String localAuthUrl = null;

    String baseURL = request.getRequestURI().toString();
    int idx = baseURL.indexOf('/', 1);
    String baseURI = baseURL.substring(idx);
    String localProto = request.getScheme(); 
    String localHost = request.getServerName();
    String localPort = "" + request.getServerPort(); 
    String localDeploymentURI = baseURL.substring(0, idx); 
    baseURL = localProto + "://" + localHost +
        ":" + localPort + localDeploymentURI;
    String baseHost = request.getServerName();
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

    String defaultOrg  = "/";
%>

<%!
    public void createCircleOfTrust(String cotName,String hostedEntityID,
                                    String remoteEntityID) throws COTException {
        // [START] Create Circle of Trust
        CircleOfTrustManager cotManager = new CircleOfTrustManager();
        Set cots = cotManager.getAllCirclesOfTrust("/");
        boolean cotExists =  ((cots != null && !cots.isEmpty()) 
                                            && cots.contains(cotName));
        if (cotExists) {
            Set memberList = 
                cotManager.listCircleOfTrustMember("/",cotName,
                                                   COTConstants.SAML2);
            if ((memberList != null && !memberList.isEmpty()) 
                                    && !memberList.contains(hostedEntityID)) {
                        cotManager.addCircleOfTrustMember("/",cotName,
                                                          COTConstants.SAML2,
                                                          hostedEntityID);
             }
             if ((memberList != null && !memberList.isEmpty()) 
                                     && !memberList.contains(remoteEntityID)) {
                        cotManager.addCircleOfTrustMember("/",cotName,
                                                          COTConstants.SAML2,
                                                          remoteEntityID);
             }
         } else {
             Set<String> providers = new HashSet<String>();
             providers.add(hostedEntityID + COTConstants.DELIMITER + 
                 COTConstants.SAML2);
             providers.add(remoteEntityID + COTConstants.DELIMITER +
                 COTConstants.SAML2);
             cotManager = new CircleOfTrustManager();
             cotManager.createCircleOfTrust("/",
                        new CircleOfTrustDescriptor(cotName, "/",
                            COTConstants.ACTIVE, "", null, null,
                            null, null, providers));
             // [END] Create Circle of Trust
        }
    }
%>
