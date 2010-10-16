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

   $Id: Help2Nav6up.jsp,v 1.1 2009/08/04 21:54:49 veiming Exp $

--%>

<%@page language="java" %>
<%@page import="com.sun.web.ui.common.CCI18N" %>
<%@page import="com.sun.web.ui.common.CCSystem" %>
<%@page import="com.iplanet.jato.util.NonSyncStringBuffer" %>
<%@page import="com.sun.identity.console.help.Help2ViewBean" %>
<%@page import="java.net.URL" %>
<%@page import="java.net.URLEncoder" %>

<%@taglib uri="/WEB-INF/tld/com_iplanet_jato/jato.tld" prefix="jato"%>
<%@taglib uri="/WEB-INF/tld/com_sun_web_ui/cc.tld" prefix="cc"%>

<%
  // Params from the URL.
  String appName = (request.getParameter("appName") != null)
    ? request.getParameter("appName") : "";
  if (appName == null || appName.equals("")) {
    appName = request.getContextPath();
    if (appName == null) {
      appName = "";
    } else if (appName.startsWith("/")) {
      appName = appName.substring(1);
    }
  }

  // Get query parameters.
  String windowTitle = (request.getParameter("windowTitle") != null)
    ? request.getParameter("windowTitle") : "";
  String mastheadTitle = (request.getParameter("mastheadTitle") != null)
    ? request.getParameter("mastheadTitle") : "";
  String mastheadAlt = (request.getParameter("mastheadAlt") != null)
    ? request.getParameter("mastheadAlt") : "";
  String helpFile = (request.getParameter("helpFile") != null)
    ? request.getParameter("helpFile") : "";
  String helpLogoHeight= (request.getParameter("helpLogoHeight") != null)
    ? request.getParameter("helpLogoHeight") : "";
  String helpLogoWidth = (request.getParameter("helpLogoWidth") != null)
    ? request.getParameter("helpLogoWidth") : "";
  String firstLoad = (request.getParameter("firstLoad") != null)
    ? request.getParameter("firstLoad") : "false";
  String pathPrefix = (request.getParameter("pathPrefix") != null)
    ? request.getParameter("pathPrefix") : "";

  helpFile = Help2ViewBean.validateHelpFile(request, helpFile);
  windowTitle = Help2ViewBean.escapeHTML(windowTitle);

  // This param is used in several places; use a var rather than encoding it
  // multiple times.
  String windowTitleEncoded =
    "windowTitle=" + URLEncoder.encode(windowTitle, CCI18N.UTF8_ENCODING);

  // src value for the Masthead frame.
  String mastheadSrc = (new NonSyncStringBuffer(request.getContextPath()))
    .append("/cchelp2/Masthead?mastheadTitle=")
    .append(URLEncoder.encode(mastheadTitle, CCI18N.UTF8_ENCODING))
    .append("&amp;mastheadAlt=")
    .append(URLEncoder.encode(mastheadAlt, CCI18N.UTF8_ENCODING))
    .append("&amp;helpLogoWidth=")
    .append(URLEncoder.encode(helpLogoWidth, CCI18N.UTF8_ENCODING))
    .append("&amp;helpLogoHeight=")
    .append(URLEncoder.encode(helpLogoHeight, CCI18N.UTF8_ENCODING))
    .append("&amp;")
    .append(windowTitleEncoded).toString();

  // src value for the Navigator frame.
  String navigatorSrc = (new NonSyncStringBuffer(request.getContextPath()))
    .append("/cchelp2/Navigator?")
    .append(windowTitleEncoded)
    .append("&amp;firstLoad=")
    .append(URLEncoder.encode(firstLoad, CCI18N.UTF8_ENCODING))
    .append("&amp;appName=")
    .append(URLEncoder.encode(appName, CCI18N.UTF8_ENCODING))
    .append("&amp;helpFile=")
    .append(URLEncoder.encode(helpFile, CCI18N.UTF8_ENCODING))
    .append("&amp;pathPrefix=")
    .append(URLEncoder.encode(pathPrefix, CCI18N.UTF8_ENCODING)).toString();

  // src value for the ButtonNav frame.
  String buttonNavSrc = (new NonSyncStringBuffer(request.getContextPath()))
    .append("/cchelp2/ButtonNav?")
    .append(windowTitleEncoded).toString();
%>

<jato:useViewBean className="com.sun.identity.console.help.Help2ViewBean">

<html>
<head>
  <title><%=windowTitle %></title>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
  <meta name="Copyright" content="Copyright &copy; 2004 by Sun Microsystems, Inc. All Rights Reserved.">
  <cc:stylesheet />
</head>

<cc:i18nbundle name="i18nBundle" id="help2Bundle"
 baseName="com.sun.web.ui.resources.Resources" />

<!-- Frameset for Masthead frame -->
<frameset rows="100,*"
 frameborder="no"
 border="0"
 framespacing="0">

<!-- Masthead frame -->
<frame src="<%=mastheadSrc %>"
 name="mastheadFrame"
 scrolling="no"
 id="mastheadFrame"
 title="Frame Containing Masthead and Page Title" />

<!-- Frameset for Nav, ButtonNav, and Content frames -->
<frameset cols="33%,67%"
 frameborder="yes"
 border="2">

<!-- Nav Frame -->
<frame src="<%=navigatorSrc %>"
 name="navFrame"
 frameBorder="yes"
 scrolling="auto"
 id="navFrame"
 title="Frame Containing Table of Contents, Index, and Search" />

<!-- Frameset for ButtonNav and Content Frames -->
<frameset rows="31,*"
 frameborder="yes"
 border="1">

<!-- ButtonNav Frame -->
<frame src="<%=buttonNavSrc %>"
 name="buttonNavFrame"
 frameBorder="yes"
 scrolling="no"
 id="buttonNavFrame"
 title="Frame Containing Navigation Buttons" />

<!-- Content Frame -->
<frame src="<%=helpFile %>"
 name="contentFrame"
 frameBorder="no"
 scrolling="auto"
 id="contentFrame"
 title="Frame Containing Online Help Text" />

</frameset>
</frameset>
</frameset>

<noframes>
<body>
<cc:text name="NoFramesText" bundleID="help2Bundle"
 defaultValue="help.noframes" />
</body>
</noframes>

</html>

</jato:useViewBean>
