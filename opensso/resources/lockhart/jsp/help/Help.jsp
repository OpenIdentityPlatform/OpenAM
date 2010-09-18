<%--
/**
 * ident "@(#)Help.jsp 1.30 04/09/14 SMI"
 * 
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 */
--%>
<%@page language="java" %>
<%@page import="com.iplanet.jato.util.NonSyncStringBuffer" %>
<%@page import="com.sun.web.ui.common.CCI18N" %>
<%@page import="com.sun.web.ui.common.CCSystem" %>
<%@page import="java.net.URLEncoder" %>

<%@taglib uri="/WEB-INF/tld/com_iplanet_jato/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/tld/com_sun_web_ui/cc.tld" prefix="cc" %>

<%
    // Get query parameters.
    String windowTitle = (request.getParameter("windowTitle") != null)
	? request.getParameter("windowTitle") : "";
    String mastheadTitle = (request.getParameter("mastheadTitle") != null)
	? request.getParameter("mastheadTitle")	: "";
    String mastheadAlt = (request.getParameter("mastheadAlt") != null)
	? request.getParameter("mastheadAlt") : "";
    String helpFile = (request.getParameter("helpFile") != null)
	? request.getParameter("helpFile") : "";
    String helpLogoWidth = (request.getParameter("helpLogoWidth") != null)
        ? request.getParameter("helpLogoWidth")	: "";
    String helpLogoHeight= (request.getParameter("helpLogoHeight") != null)
        ? request.getParameter("helpLogoHeight") : "";	
    String showCloseButton = (request.getParameter("showCloseButton") != null)
	? request.getParameter("showCloseButton") : "";

    // Create masthead URL.
    NonSyncStringBuffer buffer =
        new NonSyncStringBuffer(request.getContextPath())
        .append("/cchelp/Masthead.jsp?");

    buffer.append("mastheadTitle=")
    	.append(URLEncoder.encode(mastheadTitle, CCI18N.UTF8_ENCODING))
	.append("&amp;mastheadAlt=")
	.append(URLEncoder.encode(mastheadAlt, CCI18N.UTF8_ENCODING))
	.append("&amp;helpLogoWidth=")
	.append(URLEncoder.encode(helpLogoWidth, CCI18N.UTF8_ENCODING))
	.append("&amp;helpLogoHeight=")
	.append(URLEncoder.encode(helpLogoHeight, CCI18N.UTF8_ENCODING))
	.append("&amp;showCloseButton=")
	.append(URLEncoder.encode(showCloseButton, CCI18N.UTF8_ENCODING))
	.append("&amp;windowTitle=")
        .append(URLEncoder.encode(windowTitle, CCI18N.UTF8_ENCODING));
%>

<jato:useViewBean className="com.sun.web.ui.servlet.help.HelpViewBean">

<html>
<title><%=windowTitle %></title>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
  <meta name="Copyright" content="Copyright &copy; 2004 by Sun Microsystems, Inc. All Rights Reserved.">
  <cc:stylesheet />
</head>

<cc:i18nbundle id="helpBundle"
 baseName="com.sun.web.ui.resources.Resources"/>

<frameset border="0" rows="110,*">
 <frame src="<%=buffer.toString() %>"
  scrolling="no"
  marginwidth="0"
  marginheight="0"
  name="masthead">
 <frame src="<%=helpFile %>"
  scrolling="auto"
  marginwidth="10"
  marginheight="10"
  name="help">
</frameset>

<noframes>
<body>
<p>
<cc:text name="Text1" bundleID="helpBundle" />
</p>
</body>
</noframes>
</html>
</jato:useViewBean>
