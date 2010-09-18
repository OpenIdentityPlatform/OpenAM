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

   $Id: sts-client-user.jsp,v 1.3 2008/06/25 05:48:49 qcheng Exp $

--%>


<%@page import="
java.io.*,
java.net.*,
javax.servlet.*,
javax.servlet.http.*,
com.sun.identity.saml.common.SAMLUtils,
com.sun.identity.wss.sts.TrustAuthorityClient,
com.sun.identity.wss.security.SecurityToken,
com.sun.identity.wss.security.SecurityMechanism,
com.iplanet.dpro.session.SessionID,
com.iplanet.sso.SSOToken,
com.iplanet.sso.SSOTokenManager,
com.sun.identity.common.SystemConfigurationUtil,
com.sun.identity.saml.common.SAMLConstants,
com.sun.identity.shared.Constants,
com.iplanet.services.naming.WebtopNaming"
%>

<html xmlns="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
    <head><title>Security Token Service Client Sample with End user Token</title></head>
    <body bgcolor="white">
        <h1>Security Token Service Client Sample with End user Token</h1>

<%
       SSOToken ssoToken = null;
       String serverProtocol = 
           SystemConfigurationUtil.getProperty(Constants.AM_SERVER_PROTOCOL);
       String serverHost = 
           SystemConfigurationUtil.getProperty(Constants.AM_SERVER_HOST);
       String serverPort = 
           SystemConfigurationUtil.getProperty(Constants.AM_SERVER_PORT);
       String serviceUri = 
           SystemConfigurationUtil.getProperty(
           Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
       String famLoginUrl = 
           serverProtocol + "://" + serverHost + ":" + serverPort + serviceUri + 
           "/UI/Login?goto=" + (request.getRequestURL()).toString();

       try {
           SSOTokenManager manager = SSOTokenManager.getInstance();
           ssoToken = manager.createSSOToken(request);
           if (!manager.isValidToken(ssoToken)) {
               response.sendRedirect(famLoginUrl);
               return;
           }
       } catch (Exception e) {
           response.sendRedirect(famLoginUrl);
           return;
       }
       
       String endpointURL = null;
       String mexEndpointURL = null;
       try {
           URL stsService = 
               WebtopNaming.getServiceURL("sts", serverProtocol, serverHost,
               serverPort, serviceUri);
           endpointURL = stsService.toString();
           URL stsMexService = 
               WebtopNaming.getServiceURL("sts-mex", serverProtocol, serverHost,
               serverPort, serviceUri);
           mexEndpointURL = stsMexService.toString();
       } catch (Exception e) {
              %>Warning: cannot obtain STS end point URLs.<%
              e.printStackTrace();                      
       }
       
        SecurityToken securityToken = null;
        String sToken = null;
        try {
            TrustAuthorityClient client = new TrustAuthorityClient();
            
            securityToken = 
                client.getSecurityToken("default", endpointURL, mexEndpointURL, 
                (java.lang.Object)ssoToken, SecurityMechanism.STS_SECURITY_URI,
                (getServletConfig()).getServletContext());
            sToken = com.sun.identity.shared.xml.XMLUtils.print(
                     securityToken.toDocumentElement()); 
        } catch (Exception e) {
            %>Warning: cannot obtain security token from STS.<%
              e.printStackTrace();
        }
        if(sToken == null) {
%>
           <h2>Security Token:</h2>
                     Can not obtain security token .
                    <p><a href="sts-client-user.jsp">Return to sts-client-user.jsp</a></p>
<%
        } else {
%>
                    <h2>SecurityToken :</h2>
                    <pre><%= SAMLUtils.displayXML(sToken) %></pre>
<hr>
<%
        }
%>
        <hr />
    </body>
</html>
