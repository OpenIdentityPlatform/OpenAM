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

   $Id: multi.jsp,v 1.1 2009/07/02 22:00:17 exu Exp $

--%>

<%--
 Portions Copyrighted 2013 ForgeRock AS
--%>

<%@page
    import="com.sun.identity.wsfederation.common.WSFederationConstants"
    import="com.sun.identity.plugin.session.SessionManager"
    import="com.sun.identity.wsfederation.common.WSFederationUtils"
    import="org.owasp.esapi.ESAPI"
%><%
    // handle multi-federation protocol case
    Object uSession = null;
    try {
        uSession = SessionManager.getProvider().getSession(request);
    } catch (Exception e) {
    }
    
    if ((uSession == null) || !SessionManager.getProvider().isValid(uSession)) {
        String wreply = request.getParameter(WSFederationConstants.LOGOUT_WREPLY);
        if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + wreply,
            wreply, "URL", 2000, true)){
                wreply = null;
        }
        if ((wreply != null) && (wreply.length() != 0)) {
            response.sendRedirect(wreply);
        }
    } else {
        String logout = request.getParameter(WSFederationConstants.LOGOUT_WREPLY);
        if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + logout,
            logout, "URL", 2000, true)){
                logout = null;
        }
        request.setAttribute(WSFederationConstants.LOGOUT_WREPLY, logout);
        String realm = request.getParameter(
            WSFederationConstants.REALM_PARAM);
        if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + realm,
            realm, "HTTPParameterValue", 2000, true)){
                realm = null;
        }
        request.setAttribute(WSFederationConstants.REALM_PARAM, realm);
        String entityID = request.getParameter(
            WSFederationConstants.ENTITYID_PARAM);
        if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + entityID,
            entityID, "HTTPParameterValue", 2000, true)){
                entityID = null;
        }
        request.setAttribute(WSFederationConstants.ENTITYID_PARAM, entityID);

        WSFederationUtils.processMultiProtocolLogout(request, response, uSession);
    }
%>
