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

   $Id: Version.jsp,v 1.1 2009/08/05 20:15:51 veiming Exp $

--%>
<%--
   Portions Copyrighted 2011 ForgeRock AS
--%>

<%@ page language="java" %>
<%@taglib uri="/WEB-INF/tld/com_iplanet_jato/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/tld/com_sun_web_ui/cc.tld" prefix="cc" %>

<%@ page import="com.sun.web.ui.common.CCI18N" %>
<%@ page import="com.sun.web.ui.common.CCSystem" %>
<%@ page import="java.net.URLEncoder" %>
<%@page import="com.sun.identity.console.version.VersionViewBean" %>

<%
    // Get query parameters.
    String windowTitle = (request.getParameter("windowTitle") != null)
	? request.getParameter("windowTitle") : "";
    String productNameSrc = (request.getParameter("productNameSrc") != null)
	? request.getParameter("productNameSrc") : "";
    String versionFile = (request.getParameter("versionFile") != null)
	? request.getParameter("versionFile") : "";

    windowTitle = VersionViewBean.escapeHTML(windowTitle);

    String productNameHeight =
	(request.getParameter("productNameHeight") != null)
        ? request.getParameter("productNameHeight") : "";
    String productNameWidth =
	(request.getParameter("productNameWidth") != null)
        ? request.getParameter("productNameWidth") : "";

    // Create button frame URL.
    StringBuilder buttonBuffer =
        new StringBuilder(request.getContextPath())
        .append("/ccversion/ButtonFrame");

    // Create masthead frame URL.
    StringBuilder buffer =
        new StringBuilder(request.getContextPath())
        .append("/ccversion/Masthead.jsp?");

    buffer.append("productNameSrc=")
        .append(URLEncoder.encode(productNameSrc, CCI18N.UTF8_ENCODING))
        .append("&amp;versionFile=")
	.append(URLEncoder.encode(versionFile, CCI18N.UTF8_ENCODING))
	.append("&amp;productNameHeight=")
        .append(URLEncoder.encode(productNameHeight, CCI18N.UTF8_ENCODING))
        .append("&amp;productNameWidth=")
        .append(URLEncoder.encode(productNameWidth, CCI18N.UTF8_ENCODING));
%>

<jato:useViewBean className="com.sun.identity.console.version.VersionViewBean">

<html>
<title><%=windowTitle %></title>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
  <meta name="Copyright" content="Copyright &copy; 2010-2012 by ForgeRock, Inc. All Rights Reserved.">
  <cc:stylesheet />
</head>

<cc:i18nbundle id="bundle"
 baseName="com.sun.web.ui.resources.Resources"/>

<frameset rows="110,*,60" frameborder="no" border="0" framespacing="0">
  <frame src="<%=buffer.toString() %>" name="topFrame" scrolling="no" noresize="noresize" id="topFrame" title="Masthead Frame">
  <frame src="<%= request.getContextPath() + "/base/Version" %>" name="mainFrame" id="mainFrame" title="Content Frame">
  <frame src="<%=buttonBuffer.toString() %>" name="buttonFrame" scrolling="no" noresize="noresize" id="bottomFrame" title="Button Frame">
  <noframes>
    <body>
      <p><cc:text name="Text" bundleID="bundle" /></p>
    </body>
  </noframes>
</frameset>

</html>

</jato:useViewBean>
