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

   $Id: sts-client-wsc.jsp,v 1.3 2008/06/25 05:48:49 qcheng Exp $

--%>


<%@page import="
java.io.*,
java.net.*,
javax.servlet.*,
javax.servlet.http.*,
com.sun.identity.saml.common.SAMLUtils,
com.sun.identity.wss.sts.TrustAuthorityClient,
com.sun.identity.wss.security.SecurityToken,
com.sun.identity.wss.provider.ProviderConfig"
%>

<html xmlns="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
    <head><title>Security Token Service Client Sample with WSC's Token</title></head>
    <body bgcolor="white">
        <h1>Security Token Service Client Sample with WSC's Token</h1>
<%
        if (request.getMethod().equals("GET")) {
%>
            <form method="POST">
                <table>
                    <tr>
                        <td>Web Service Provider Name</td>
                        <td><input type="text" name="providerName" value="wsc"/></td>
                    </tr>
                </table>
                <input type="submit" value="Get Token" />
            </form>
<%
        } else {
            String providerName = request.getParameter("providerName");
            SecurityToken securityToken = null;
            String sToken = null;
            try {
                TrustAuthorityClient client = new TrustAuthorityClient();
                ProviderConfig pc = ProviderConfig.getProvider(
                                    providerName, ProviderConfig.WSC);
                securityToken = 
                    client.getSecurityToken(pc, (java.lang.Object)null,
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
                        <p><a href="sts-client-wsc.jsp">Return to sts-client-wsc.jsp</a></p>
<%
            } else {
%>
                        <h2>SecurityToken :</h2>
                        <pre><%= SAMLUtils.displayXML(sToken) %></pre>
<hr>
<%
            }
        }
%>
        <hr />
    </body>
</html>
