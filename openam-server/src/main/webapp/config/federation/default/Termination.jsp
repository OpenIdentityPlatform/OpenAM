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

   $Id: Termination.jsp,v 1.4 2008/06/25 05:48:34 qcheng Exp $

--%>


<%@ page language="java"
import="com.sun.liberty.LibertyManager,
java.util.Set,java.util.Iterator"
%>
<%
    String providerAlias = request.getParameter(
        LibertyManager.getMetaAliasKey());
    String realm = LibertyManager.getRealmByMetaAlias(providerAlias);
    String hostedProviderId = LibertyManager.getEntityID(providerAlias);
    String providerRole = LibertyManager.getProviderRole(providerAlias);
    String termDonePageURL = LibertyManager.getTerminationDonePageURL(
        realm, hostedProviderId, providerRole, request);
    String dest = termDonePageURL + "&termStatus=cancel";

    if ((providerAlias == null) || (providerAlias.length() < 1)) {
        response.sendError(response.SC_INTERNAL_SERVER_ERROR,
            "Provider Alias not found");
        return;
    }

    if ((hostedProviderId == null) || (hostedProviderId.length() < 1)) {
        response.sendError(response.SC_INTERNAL_SERVER_ERROR,
            "Provider not found");
        return;
    }

    String HOME_URL = LibertyManager.getHomeURL(
        realm, hostedProviderId, providerRole);
    String preLoginURL = LibertyManager.getPreLoginServletURL(
        realm, hostedProviderId, providerRole, request);
    String actionLocation = LibertyManager.getTerminationURL(
        realm, hostedProviderId, providerRole, request);
    String gotoUrl = HttpUtils.getRequestURL(request).toString()
        + "?" + request.getQueryString();
    String userDN = LibertyManager.getUser(request);

    if (userDN == null) {
        // redirect for authentication
        char delimiter;
        delimiter = preLoginURL.indexOf('?') < 0 ? '?' : '&';
        response.sendRedirect(preLoginURL + delimiter + "goto=" +
            java.net.URLEncoder.encode(gotoUrl));
        return;
    }
%>

<%@ include file="Header.jsp"%>
<center>

<script language="javascript">
    function doCancel() {
        location.href="<%=dest%>";
    }
</script>

<%
    try {
       Set providerList = LibertyManager.getFederatedProviders(
            userDN, realm, hostedProviderId, providerRole);
       if ((providerList != null) && !providerList.isEmpty()) {
%>

<form name="selectprovider" method="POST" action="<%= actionLocation%>">
<table width="100%" border="0" cellspacing="0" cellpadding="0">

<tr>
<td align="center">
    <b>Please select a remote provider to terminate federation with: </b>
</td>
</tr>

<tr>
<td align="center">
    <select name= <%=LibertyManager.getTerminationProviderIDKey()%> size="1" >
<%
    for (Iterator i = providerList.iterator(); i.hasNext(); ) {
        String providerId = (String)i.next();
%>
            <option value="<%=providerId%>"><%=providerId%></option>
<% } %>
   </select>
</td>
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
</form>

<% } else { %>

    <b>User has no active federations.</b>

    <p>
    <% if (HOME_URL == null){ %>
        <a href="http://www.sun.com">Continue</a>
    <% }else { %>
        <a href="<%=HOME_URL%>">Continue</a>
    <% } %>
    </p>

<% }
} catch(Exception ex) {
    response.sendRedirect(preLoginURL + "?goto=" +
        java.net.URLEncoder.encode(gotoUrl));
    return;
} %>

<p>
<center>
<table border=0 cellpadding=5 cellspacing=0 width="75%">
<tr>
<td bgcolor="#CCCCCC">
Federation termination results in the mapping established between the user's
account at service provider and Identity provider to be disabled. Once account
federation is terminated the Identity provider cannot issue authentication
assertions for this user to that service provider.
</td>
</tr>
</table>
</center>
</p>
</center>
<%@ include file="Footer.jsp"%>

