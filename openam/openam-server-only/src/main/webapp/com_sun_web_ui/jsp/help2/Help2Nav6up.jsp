<%--
/**
 * ident "@(#)Help2Nav6up.jsp 1.29 04/09/14 SMI"
 * 
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 */
--%>

<%--
   Portions Copyrighted 2013 ForgeRock AS
--%>

<%@page language="java" %>
<%@page import="com.sun.web.ui.common.CCI18N" %>
<%@page import="com.iplanet.jato.util.NonSyncStringBuffer" %>
<%@page import="java.net.URLEncoder" %>
<%@ page import="org.owasp.esapi.ESAPI" %>

<%@taglib uri="/WEB-INF/tld/com_iplanet_jato/jato.tld" prefix="jato"%>
<%@taglib uri="/WEB-INF/tld/com_sun_web_ui/cc.tld" prefix="cc"%>

<%
  // Params from the URL.
  String appName = (request.getParameter("appName") != null) ? request.getParameter("appName") : "";
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + appName, appName,
            "HTTPParameterValue", 2000, false)) {
        appName = "";
    }
  if (appName == null || appName.equals("")) {
    appName = request.getContextPath();
    if (appName == null) {
      appName = "";
    } else if (appName.startsWith("/")) {
      appName = appName.substring(1);
    }
  }

  // Get query parameters.
    String windowTitle = (request.getParameter("windowTitle") != null) ? request.getParameter("windowTitle") : "";
      if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + windowTitle, windowTitle,
              "HTTPParameterValue", 2000, false)) {
          windowTitle = "";
      }
    String mastheadTitle = (request.getParameter("mastheadTitle") != null) ? request.getParameter("mastheadTitle") : "";
      if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + mastheadTitle, mastheadTitle,
              "HTTPParameterValue", 2000, false)) {
          mastheadTitle = "";
      }
    String mastheadAlt = (request.getParameter("mastheadAlt") != null) ? request.getParameter("mastheadAlt") : "";
      if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + mastheadAlt, mastheadAlt,
              "HTTPParameterValue", 2000, false)) {
          mastheadAlt = "";
      }
    String helpFile = (request.getParameter("helpFile") != null) ? request.getParameter("helpFile") : "";
      if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + helpFile, helpFile,
              "HTTPParameterValue", 2000, false)) {
          helpFile = "";
      }
    String helpLogoHeight= (request.getParameter("helpLogoHeight") != null) ? request.getParameter("helpLogoHeight") : "";
      if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + helpLogoHeight, helpLogoHeight,
              "HTTPParameterValue", 2000, false)) {
          helpLogoHeight = "";
      }
    String helpLogoWidth = (request.getParameter("helpLogoWidth") != null) ? request.getParameter("helpLogoWidth") : "";
      if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + helpLogoWidth, helpLogoWidth,
              "HTTPParameterValue", 2000, false)) {
          helpLogoWidth = "";
      }
    String firstLoad = (request.getParameter("firstLoad") != null) ? request.getParameter("firstLoad") : "false";
      if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + firstLoad, firstLoad,
              "HTTPParameterValue", 2000, false)) {
          firstLoad = "false";
      }
    String pathPrefix = (request.getParameter("pathPrefix") != null) ? request.getParameter("pathPrefix") : "";
      if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + pathPrefix, pathPrefix,
              "HTTPParameterValue", 2000, false)) {
          pathPrefix = "";
      }

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

<jato:useViewBean className="com.sun.web.ui.servlet.help2.Help2ViewBean">

<html>
<head>
  <title><%=ESAPI.encoder().encodeForHTML(windowTitle) %></title>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
  <meta name="Copyright" content="Copyright &copy; 2004 by Sun Microsystems, Inc. All Rights Reserved." />
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
