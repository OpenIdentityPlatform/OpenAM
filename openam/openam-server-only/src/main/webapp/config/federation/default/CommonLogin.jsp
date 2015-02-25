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

   $Id: CommonLogin.jsp,v 1.4 2008/06/25 05:48:31 qcheng Exp $

--%>

<%--
    Portions Copyrighted 2013 ForgeRock AS
 --%>

<%@ page language="java" import="java.util.*,
com.sun.identity.saml.common.SAMLUtils,
com.sun.identity.shared.encode.Base64,
com.sun.liberty.LibertyManager,
org.owasp.esapi.ESAPI"
%>

<%@ include file="Header.jsp"%>

<p align="left">&nbsp;</p>

<table border="0" cellpadding="0" cellspacing="4" width="100%">
<tr>
    <td align="center"><font size="4" face="Arial, Helvetica, sans-serif"><b> List of Identity providers:</b></font></td>
  </tr>
<%
    Set providerSet = null;
    String metaAliasKey = LibertyManager.getMetaAliasKey();
    String metaAlias = request.getParameter(metaAliasKey);
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + metaAlias, metaAlias,
        "HTTPParameterValue", 2000, false)) {
             metaAlias = "";
    }
    String requestIDKey = LibertyManager.getRequestIDKey();
    String requestID = request.getParameter(requestIDKey);
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + requestID, requestID,
        "HTTPParameterValue", 2000, false)) {
             metaAlias = "";
    }
    String providerIDKey = LibertyManager.getProviderIDKey();
    String loginURL = LibertyManager.getLoginURL(request);
    String intersiteServletUrl = LibertyManager.getInterSiteURL(request);
    String realm = LibertyManager.getRealmByMetaAlias(metaAlias);
    String providerID = LibertyManager.getEntityID(metaAlias);
    Iterator providerIter = null;
    if(providerID != null) {
        providerIter = LibertyManager.getIDPList(realm, providerID);
    } else {
        response.sendError(response.SC_INTERNAL_SERVER_ERROR,"Not able to get Provider ID");
        return;
    }
    String idpID = null;
    String encodedID = null;
    if(requestID == null || requestID.length() <= 0)
        requestID = LibertyManager.getNewRequest(request);
    while(providerIter.hasNext()) {
    idpID = (String)providerIter.next();
    String succinctID = LibertyManager.getSuccinctID(idpID, "IDP");
    if(idpID != null) {
        encodedID = java.net.URLEncoder.encode(
            Base64.encode(SAMLUtils.stringToByteArray(succinctID)));
    }

    String intersiteUrl = intersiteServletUrl
                          + "?" + metaAliasKey + "=" + metaAlias
                          + "&" + requestIDKey + "="
                          + java.net.URLEncoder.encode(requestID) + "&" + providerIDKey + "=" + encodedID;
%>

  <tr>
    <td align="center"><font size="3" face="Arial, Helvetica, sans-serif"><a href="<%= intersiteUrl %>" target="_top">
        <%= idpID %>
    </a></font></td>
  </tr>

<%
    }
%>
 <tr>
	<td>
	</td>
</tr>
<tr>
	<td>
	</td>
</tr>

  <tr>
    <%
    String newQueryString = LibertyManager.cleanQueryString(request);
    if (newQueryString != null)
        loginURL = loginURL + "&" + newQueryString ;
    %>

    <td align="center"><font size="4" face="Arial, Helvetica, sans-serif"><a href="<%=loginURL %>" target="_top">
       Local Login
    </a></font></td>
  </tr>
</table>
<%@ include file="Footer.jsp"%>
