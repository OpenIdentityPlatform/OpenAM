<%--
/**
 * ident "@(#)Version.jsp 1.13 05/07/14 SMI"
 * 
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 */
--%>
<%@ page language="java" %>
<%@taglib uri="/WEB-INF/tld/com_iplanet_jato/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/tld/com_sun_web_ui/cc.tld" prefix="cc" %>

<%@ page import="com.sun.web.ui.common.CCI18N" %>
<%@ page import="com.sun.web.ui.common.CCSystem" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="com.iplanet.jato.util.NonSyncStringBuffer" %>

<%
    // Get query parameters.
    String windowTitle = (request.getParameter("windowTitle") != null)
	? request.getParameter("windowTitle") : "";
    String productNameSrc = (request.getParameter("productNameSrc") != null)
	? request.getParameter("productNameSrc") : "";
    String versionFile = (request.getParameter("versionFile") != null)
	? request.getParameter("versionFile") : "";

    String versionNumber = (request.getParameter("versionNumber") != null)
	? request.getParameter("versionNumber") : "";
    if (versionNumber == null || versionNumber.length() == 0) {
        versionNumber = CCSystem.getVersionTxt(versionFile,
            request.getContextPath());
    }

    String productNameHeight =
	(request.getParameter("productNameHeight") != null)
        ? request.getParameter("productNameHeight") : "";
    String productNameWidth =
	(request.getParameter("productNameWidth") != null)
        ? request.getParameter("productNameWidth") : "";

    // Create button frame URL.
    NonSyncStringBuffer buttonBuffer =
        new NonSyncStringBuffer(request.getContextPath())
        .append("/ccversion/ButtonFrame?")
	.append("versionNumber=")
        .append(URLEncoder.encode(versionNumber, CCI18N.UTF8_ENCODING));

    // Create masthead frame URL.
    NonSyncStringBuffer buffer =
        new NonSyncStringBuffer(request.getContextPath())
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

<jato:useViewBean className="com.sun.web.ui.servlet.version.VersionViewBean">

<html>
<title><%=windowTitle %></title>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
  <meta name="Copyright" content="Copyright &copy; 2004 by Sun Microsystems, Inc. All Rights Reserved.">
  <cc:stylesheet />
</head>

<cc:i18nbundle id="bundle"
 baseName="com.sun.web.ui.resources.Resources"/>

<frameset rows="110,*,60" frameborder="no" border="0" framespacing="0">
  <frame src="<%=buffer.toString() %>" name="topFrame" scrolling="no" noresize="noresize" id="topFrame" title="Masthead Frame">
  <frame src="<%=versionFile%>?versionNumber=<%=versionNumber%>" name="mainFrame" id="mainFrame" title="Content Frame">
  <frame src="<%=buttonBuffer.toString() %>" name="buttonFrame" scrolling="no" noresize="noresize" id="bottomFrame" title="Button Frame">
  <noframes>
    <body>
      <p><cc:text name="Text" bundleID="bundle" /></p>
    </body>
  </noframes>
</frameset>

</html>

</jato:useViewBean>
