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

   Portions Copyrighted 2011-2016 ForgeRock AS.
   Portions Copyrighted 2024 3A Systems LLC.
--%>

<%@ page import="com.sun.web.ui.common.CCI18N" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.MissingResourceException" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="org.owasp.esapi.ESAPI" %>


<%

    ResourceBundle resourceBundle;
    try {
        resourceBundle = ResourceBundle.getBundle("com.sun.web.ui.resources.Resources", request.getLocale());
    } catch (MissingResourceException mr) {
        resourceBundle = ResourceBundle.getBundle("com.sun.web.ui.resources.Resources");
    }

    String windowTitle = resourceBundle.getString("masthead.versionWindowTitle");
    // Get query parameters.
    String productNameSrc = (request.getParameter("productNameSrc") != null)
            ? request.getParameter("productNameSrc") : "";
    String versionFile = (request.getParameter("versionFile") != null)
            ? request.getParameter("versionFile") : "";

    windowTitle = ESAPI.encoder().encodeForHTML(windowTitle);

    String productNameHeight =
            (request.getParameter("productNameHeight") != null)
                    ? request.getParameter("productNameHeight") : "";
    String productNameWidth =
            (request.getParameter("productNameWidth") != null)
                    ? request.getParameter("productNameWidth") : "";

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

<html>
<title><%=windowTitle %></title>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="Copyright" content="Copyright &copy; 2011-2016 ForgeRock AS, 2024, 3A Systems LLC. All Rights Reserved.">
    <link rel="stylesheet" type="text/css" href="<%= request.getContextPath() + "/com_sun_web_ui/css/css_ns6up.css"%>" />
</head>

<body style="margin: 0;">
<div style="display: flex; flex-direction: column; height: 100%">
    <div class="VrsMstBdy" style="margin: 8px; flex: 0 1 auto;">
        <table width="100%" border="0" cellpadding="0" cellspacing="0" title="">
            <tbody>
            <tr>
                <td class="VrsPrdTd">
                    <div class="VrsPrdDiv">
                        <img src="<%= request.getContextPath()%>/console/images/PrimaryProductName.png" alt="" border="0" height="" width="">
                    </div>
                </td>
                <td class="VrsLgoTd"><img src="<%= request.getContextPath()%>/com_sun_web_ui/images/version/javalogo-color.gif" width="31" height="55" alt="Java(TM) Logo"></td>
            </tr>
            </tbody>
        </table>
    </div>
    <div class="VrsBtnBdy" style="display: flex; flex-direction: column; flex: 1 1  auto; align-self: stretch;">
        <div class="VrsBtnBdy" style="flex: 1 1  auto; align-self: stretch;">
            <iframe width="100%" style="border: none;"  src="<%= request.getContextPath() + "/base/Version" %>" name="mainFrame" id="mainFrame" title="Content Frame"></iframe>
        </div>
        <div class="VrsBtnBdy VrsBtnAryDiv" style="margin-block-end: 1em;">
            <input name="ButtonFrame.Close" type="submit" class="Btn1"
                   value="Close" onclick="javascript: window.close(); return false;"
                   onmouseover="if (this.disabled==0) this.className='Btn1Hov'; if (this.disabled==0) this.className='Btn1Hov';if (this.disabled==0) this.className='Btn1Hov';if (this.disabled==0) this.className='Btn1Hov'"
                   onmouseout="if (this.disabled==0) this.className='Btn1';if (this.disabled==0) this.className='Btn1';if (this.disabled==0) this.className='Btn1';if (this.disabled==0) this.className='Btn1'"
                   onblur="if (this.disabled==0) this.className='Btn1';if (this.disabled==0) this.className='Btn1';if (this.disabled==0) this.className='Btn1';if (this.disabled==0) this.className='Btn1'"
                   onfocus="if (this.disabled==0) this.className='Btn1Hov';javascript: if (this.disabled==0) this.className='Btn1Hov';if (this.disabled==0) this.className='Btn1Hov';if (this.disabled==0) this.className='Btn1Hov'" />
        </div>
    </div>
</div>
</body>
</html>
