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

   $Id: ListOfCOTs.jsp,v 1.3 2008/06/25 05:48:34 qcheng Exp $

--%>

 <%--
     Portions Copyrighted 2013 ForgeRock AS
     Portions Copyright 2026 3A Systems, LLC.
  --%>


<%@ page language="java" import="java.util.*,
    com.sun.liberty.LibertyManager,
    org.owasp.esapi.ESAPI"
 %>
<%@ page import="com.sun.identity.authentication.service.AuthUtils" %>
<%@ page import="com.sun.identity.authentication.service.AuthD" %>
<%@ page import="com.sun.identity.federation.common.FSUtils" %>

<%@ include file="Header.jsp"%>

<script language="javascript">
    function doSubmit() {
        document.form1.action.value = 'submit';
        document.form1.submit();
    }
    function doCancel() {
        document.form1.action.value = 'cancel';
        document.form1.submit();
    }
</script>
<%

    Set cotSet = null;
    String metaAliasKey = LibertyManager.getMetaAliasKey();
    String LRURLKey = LibertyManager.getLRURLKey();
    String COTKey = LibertyManager.getCOTKey();
    String metaAlias = request.getParameter(metaAliasKey);
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + metaAlias, metaAlias,
        "HTTPParameterValue", 2000, false)){
            metaAlias = "";
    }
    String LRURL = request.getParameter(LRURLKey);
    String actionURL = LibertyManager.getConsentHandlerURL(request);
    String providerID = LibertyManager.getEntityID(metaAlias);
    String providerRole = LibertyManager.getProviderRole(metaAlias);
    if (providerID != null){
        cotSet = LibertyManager.getListOfCOTs(providerID, providerRole);
    } else {
        response.sendError(response.SC_INTERNAL_SERVER_ERROR,"Not able to get Provider ID");
        return;
    }
    if (LRURL != null && LRURL.length() > 0) {
        // LRURL arrives on the request and ends up in a redirect, so it has to be checked. The realm whose
        // goto whitelist judges it is determined by this server rather than by the caller: the metaAlias this
        // page is entered with is a request parameter, so a realm derived from it would be as caller
        // controlled as the URL being judged - the caller would name whichever realm has a permissive
        // whitelist and have it vouch for their own redirect. getDomainNameByRequest() with no query
        // parameters to read resolves the realm from the name this server was reached by, which answers the
        // root realm unless the deployment maps a host name per realm. A relative URL is kept whether that
        // realm configures a goto list or not; an absolute one pointing back at this server is kept only
        // while it configures none, since a list that is configured decides on its own.
        String orgDN = AuthUtils.getDomainNameByRequest(request, new HashMap<String, String>());
        if (!AuthD.getAuth().isGotoUrlValid(request, LRURL, orgDN)) {
            // The configured home URL is used instead, the way an absent LRURL is answered, so say in the log
            // why the one on the request was dropped.
            FSUtils.debug.warning("ListOfCOTs.jsp: the LRURL is not a permitted redirect target, "
                + "falling back to the home URL of the provider");
            LRURL = null;
        }
    } else {
        LRURL = null;
    }
    if (LRURL == null) {
        // The home URL comes from the provider's own metadata rather than from the request, so it is an
        // administrator configured destination and is not measured against this server's origin.
        LRURL = LibertyManager.getHomeURL(providerID, providerRole);
    }
    if (cotSet == null) {
        // An empty home URL would make this a redirect to the current URI, i.e. back to this page.
        if (LRURL != null && LRURL.length() > 0) {
            response.sendRedirect(LRURL);
        } else {
            response.sendError(response.SC_INTERNAL_SERVER_ERROR, "Not able to determine the return URL");
        }
        return;
    }
%>

<form name="form1" method="post" action="<%= actionURL %>">
<table border="0" cellspacing="3" cellpadding="0" align=center>
    <tr>
    <td align="center">
        <h3>The Identity provider belongs to multiple authentication domains.</h3>
        <b>Please select an authentication domain to set the<br>
        preferred Identity provider information:</b>
    </td>
    </tr>

    <tr>
    <td align="center">
        <input type="hidden" name="LRURL" value="<%= LRURL == null ? "" :
            ESAPI.encoder().encodeForHTMLAttribute(LRURL) %>">
        <input type="hidden" name="metaAlias" value="<%=
            ESAPI.encoder().encodeForHTMLAttribute(metaAlias) %>">
        <input type="hidden" name="action" >
        <select name="<%=COTKey%>" size="1" >

<%  try {
        Iterator cotIter = cotSet.iterator();
        String cotID = "";
        while (cotIter.hasNext()) {
            cotID = (String)cotIter.next();
%>
            <option value="<%= cotID%>"> <%= cotID%> </option>
<%
        }//end of while
    } catch(Exception ex) {
        response.sendError(response.SC_INTERNAL_SERVER_ERROR,
            "Error in handling request");
        return;
    }
%>
        </select>
        <p>&nbsp;</p>
    </td>
    </tr>
    <tr>
    <td align="center">
        <input name="button" type="button" onClick='doSubmit()' value="submit">
        <input name="button2" type="button" onClick='doCancel()' value="cancel">
    </td>
    </tr>
</table>
</form>

<p>
An Identity Provider can belong to more than one authentication domain. In such cases the user will have to
select the authentication domain where he/she wants to publish this provider as the user's preferred Identity provider.
This information can later be used by service providers in this authentication
domain to seamlessly single sign on the user.
</p>
<%@ include file="Footer.jsp"%>

