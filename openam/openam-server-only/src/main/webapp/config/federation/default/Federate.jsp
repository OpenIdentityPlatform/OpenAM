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

   $Id: Federate.jsp,v 1.4 2008/06/25 05:48:31 qcheng Exp $

   Portions Copyrighted 2013 ForgeRock AS
 --%>

<%@ page language="java" import="java.util.*,
    java.io.*,
    com.sun.liberty.LibertyManager,
    org.owasp.esapi.ESAPI"
%>
<%
boolean bLECP = LibertyManager.isLECPProfile(request);
if(bLECP) {
    response.setContentType(LibertyManager.getLECPContentType());
    response.setHeader(
        LibertyManager.getLECPHeaderName(),
        request.getHeader(LibertyManager.getLECPHeaderName()));
    String responseData = LibertyManager.getAuthnRequestEnvelope(request);
    out.print(responseData);
}
%>
<%
    String metaAliasKey = LibertyManager.getMetaAliasKey();
    String metaAlias = request.getParameter(metaAliasKey);
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + metaAlias,
        metaAlias,"HTTPParameterValue", 2000, false)) {
            metaAlias = "";
    }
%>

<%@ include file="Header.jsp"%>

<SCRIPT language="javascript">
    function doSubmit() {
        document.form1.action.value = 'submit';
        document.form1.submit();
    }
    function doCancel() {
       location.href="FederationDone.jsp?metaAlias=<%=metaAlias%>&termStatus=cancel";
    }
</SCRIPT>

<%
    Set providerSet = null;
    String LRURLKey = LibertyManager.getLRURLKey();
    String selectedProvider = LibertyManager.getSelectedProviderKey();
    String LRURL = request.getParameter(LRURLKey);
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + LRURL,
            LRURL, "HTTPURI", 2000, false)){
        LRURL = "";
    }
    String userDN = LibertyManager.getUser(request);
    String realm = LibertyManager.getRealmByMetaAlias(metaAlias);
    String providerID = LibertyManager.getEntityID(metaAlias);
    String providerRole = LibertyManager.getProviderRole(metaAlias);
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + providerRole,
        providerRole, "HTTPParameterValue", 2000, false)){
            providerRole = "";
    }
    String HOME_URL = LibertyManager.getHomeURL(realm, providerID, providerRole);
    if (userDN == null) {
        String gotoUrl = HttpUtils.getRequestURL(request).toString()
            + "?" + request.getQueryString();
        String preLoginURL = LibertyManager.getPreLoginServletURL(
            realm, providerID, providerRole, request);
        char delimiter = preLoginURL.indexOf('?') < 0 ? '?' : '&';
        response.sendRedirect(preLoginURL + delimiter + "goto=" +
            java.net.URLEncoder.encode(gotoUrl));
        return;
    }

    String actionURL = LibertyManager.getFederationHandlerURL(request);
    if(providerID != null) {
        providerSet = LibertyManager.getProvidersToFederate(
            realm, providerID, providerRole, userDN);
    } else {
        response.sendError(response.SC_INTERNAL_SERVER_ERROR,
            "Not able to get providerID");
        return;
    }
    if (LRURL == null || LRURL.length() <= 0) {
        LRURL = LibertyManager.getFederationDonePageURL(
            realm, providerID, providerRole, request);
    }
%>
<table width="100%" border="0" cellspacing="0" cellpadding="0">

    <tr>
    <td colspan="2">&nbsp;</td>
    <td width="100%">

    <% if ((providerSet == null) || providerSet.isEmpty()) { %>

        <p><center><b>There are no providers to federate with.</b></p>

        <% if (HOME_URL == null){ %>
            <a href="http://www.sun.com">Continue</a>
        <% } else { %>
            <a href="<%=HOME_URL%>">Continue</a>
        <% } %>
        </center>

    <% } else { %>

        <form name="form1" method="post" action="<%= actionURL %>">
        <input type="hidden" name="RelayState" value="<%= LRURL %>">
        <input type="hidden" name="metaAlias" value="<%= metaAlias%>">
        <input type="hidden" name="action" >

        <center>
        <table border="0" cellspacing="3" cellpadding="0" align=center>
        <tr>
        <td align="center">
            <b>Please select an Identity Provider to federate with:</b></td>
        </tr>

        <tr>
        <td align="center">
            <select name="<%= selectedProvider%>" size="1" >
    <%  try {
            for (Iterator i = providerSet.iterator(); i.hasNext(); ) {
            String idpID = (String)i.next();
    %>
                <option value="<%= idpID%>"><%= idpID%></option>
    <%      }
        } catch(Exception ex) {
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                "Error in handling request");
            return;
        }
    %>
            </select>
        </td>
        </tr>
       <tr>
        <td align="center">
            <p>
            <br />
            <input name="button" type="button" onClick='doSubmit()' value="submit">
            <input name="button2" type="button" onClick='doCancel()' value="cancel">
            </p>
        </td>
        </tr>
        </table>
        </center>
        </form>
    <% } %>


<p>
<center>
<table border=0 cellpadding=5 cellspacing=0 width="75%">
<tr>
<td bgcolor="#CCCCCC">
Account federation is the means to establish a mapping between a user's
accounts at the service provider and identity provider. A user whose account
is so federated can at all later times authenticate at identity provider and
seamlessly be single signed on to the service provider.
</td>
</tr>
</table>
</center>
</p>
<%@ include file="Footer.jsp"%>

