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

   $Id: Error.jsp,v 1.4 2009/10/08 18:19:05 sean_brydon Exp $

--%>

<%@ page language="java" import="java.io.*,java.util.*,
com.sun.liberty.LibertyManager"
%>
<%@ page import="org.owasp.esapi.ESAPI" %>

<%
    String ERROR_URL = LibertyManager.getFedErrorKey();
    String ERROR_REMARK = LibertyManager.getFedRemarkKey();
    String providerAlias =
        request.getParameter(LibertyManager.getMetaAliasKey());
    String providerId = LibertyManager.getEntityID(providerAlias);
    String providerRole = LibertyManager.getProviderRole(providerAlias);
    String HOME_URI = null;
    if (providerId != null) {
        HOME_URI = LibertyManager.getHomeURL(providerId, providerRole);
    }
%>
<%@ include file="Header.jsp"%>
<table border="0" cellpadding="5" cellspacing="0" width="100%">
<% if (request.getParameter(ERROR_URL) != null) { %>

<tr>
<td>Error : <i><%=ESAPI.encoder().encodeForHTML(request.getParameter(ERROR_URL))%></i></td>
</tr>
<tr>
<td>Remark : <i><%=ESAPI.encoder().encodeForHTML(request.getParameter(ERROR_REMARK))%></i></td>
</tr>

<% } else { %>
<tr>
<td>
<b>Error occured during proccessing</b>.
<br>Please contact your Service Provider.
</td>
</tr>
<%}%>
</table>
<p>
<% if (HOME_URI == null){ %>
    <a href="http://www.sun.com">Continue</a>
<%}else {%>
    <a href="<%=HOME_URI%>">Continue</a>
<% } %>
<p>

<%@ include file="Footer.jsp"%>




