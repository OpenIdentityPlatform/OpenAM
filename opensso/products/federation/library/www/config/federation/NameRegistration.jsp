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

   $Id: NameRegistration.jsp,v 1.4 2008/06/25 05:48:34 qcheng Exp $

--%>

<%@page language="java"
import="com.sun.liberty.LibertyManager, java.util.Set, java.util.Iterator"
%>
<%@include file="Header.jsp"%>

<%
    String providerAlias = request.getParameter(
        LibertyManager.getMetaAliasKey());
    String realm = LibertyManager.getRealmByMetaAlias(providerAlias);
    String providerID = LibertyManager.getEntityID(providerAlias);
    String providerRole = LibertyManager.getProviderRole(providerAlias);
    String homeURL = LibertyManager.getHomeURL(realm, providerID, providerRole);
    String preLoginURL = LibertyManager.getPreLoginServletURL(
        realm, providerID, providerRole, request);
    String actionLocation = LibertyManager.getNameRegistrationURL(
        realm, providerID, providerRole, request);
    String gotoUrl = HttpUtils.getRequestURL(request).toString()
        + "?" + request.getQueryString();
    String userDN = LibertyManager.getUser(request);
    String nameRegistrationDonePageURL = 
        LibertyManager.getNameRegistrationDonePageURL(
            realm, providerID, providerRole, request);
    String dest = nameRegistrationDonePageURL + "&regStatus=cancel";
%>

<script language="javascript">
    function doCancel() {
        location.href="<%=dest%>";
    }
</script>

<center>
<%
    if (providerAlias == null || providerAlias.length() <= 0) {
        response.sendError(response.SC_INTERNAL_SERVER_ERROR,
            "Provider alias not found");
        return;
    } else if (providerID == null || providerID.length() <= 0) {
        response.sendError(response.SC_INTERNAL_SERVER_ERROR,
            "Provider not found");
        return;
    }
    if (userDN == null)	{
        // redirect for authentication
        char delimiter;
        delimiter = preLoginURL.indexOf('?') < 0 ? '?' : '&';
        response.sendRedirect(preLoginURL + delimiter + "goto=" +
                        java.net.URLEncoder.encode(gotoUrl));
        return;
    }
    try {
        Set providerList = LibertyManager.getRegisteredProviders(
            userDN, realm, providerID, providerRole);
    if (providerList != null && !providerList.isEmpty()) {
%>

<form name="selectprovider" method="POST" action="<%= actionLocation%>">
<table cellpadding='0' cellspacing='3'>
<tr>
<td align="center">
    <b>Please select a remote provider to register with: </b>
</td>
</tr>
<tr>
    <td align="center">
        <select name="<%=LibertyManager.getNameRegistrationProviderIDKey()%>"
            size="1" >
<%
        Iterator iterProvider = providerList.iterator();
        while (iterProvider.hasNext()) {
            String providerId = (String)iterProvider.next();
%>
            <option value="<%=providerId%>"><%=providerId%></option>
<%
        }
%>
        </select>
    </td>
</tr>

</tr>
<tr>
    <td align="center">
        <p>
        <br />
        <input name="doIt" type="submit" value="submit">
        <input name="button2" type="button" onClick='doCancel()' value="cancel">
        </p>
    </td>
</tr>
</table>

<%  } else { %>
    <p><b>User has no active registrations.</b><p>

    <% if (homeURL == null) { %>
        <a href="http://www.sun.com">Continue</a>
    <% } else { %>
        <a href="<%=homeURL%>">Continue</a>
    <% } %>
    </p>
<% }

    } catch(Exception ex){
        response.sendRedirect(preLoginURL + "?goto=" +
            java.net.URLEncoder.encode(gotoUrl));
        return;
    }
%>
</center>
<p>&nbsp</p>
<%@ include file="Footer.jsp"%>



