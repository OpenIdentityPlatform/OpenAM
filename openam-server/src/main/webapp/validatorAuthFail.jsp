<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: validatorAuthFail.jsp,v 1.4 2009/01/05 23:23:25 veiming Exp $

--%>

<%@ page import="com.sun.identity.common.SystemConfigurationUtil" %>
<%@ page import="com.sun.identity.shared.Constants" %>
<%@ page import="com.sun.identity.workflow.ValidateSAML2" %>
<%@ page contentType="text/html; charset=utf-8" language="java" %>

<html>
<head>

<%
    String deployuri = SystemConfigurationUtil.getProperty(
        Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
    String m = request.getParameter("m");
    String locale = request.getParameter("locale");
    if (m == null) {
        m = "";
    }
%>

<link rel="stylesheet" type="text/css" href="<%= deployuri %>/com_sun_web_ui/css/css_ns6up.css" />
<link rel="stylesheet" type="text/css" href="<%= deployuri %>/console/css/openam.css" />
</head>

<body class="DefBdy">
    <div><img src="<%= deployuri %>/com_sun_web_ui/images/other/dot.gif" alt="" border="0" height="50" width="1" /></div>
    <center>
    <table border=0 cellpadding=2 cellspacing=0 width="400">
    <tr>
    <td bgcolor="#333333">
        <table bgcolor="#FFFFFF" border=0 cellpadding=2 cellspacing=10>
        <tr>
        <td align=center><img width=28 height=26 src="<%= deployuri %>/console/images/progressfailed.gif" />
        <img src="<%= deployuri %>/com_sun_web_ui/images/other/dot.gif" alt="" border="0" height="10" width="1" /></br /><b>
        <%
            out.println(ValidateSAML2.getMessage("validator.title.auth.failed", locale));
        %>
        </b></td></tr>
        <tr>
        <td>
        <%
            out.println(ValidateSAML2.getMessage("validator.message.auth.failed", locale));
        %>
        <tr>
        <td align="right">

        <% 
            if (m.equals("idp")) {
                out.print("<a href=\"javascript:top.authIdp()\">");
            } else if (m.equals("sp")) {
                out.print("<a href=\"javascript:top.authSp()\">");
            } else if (m.equals("sso")) {
                out.print("<a href=\"javascript:top.singleLogin()\">");
            } else {
                out.print("<a href=\"#\">");
            }

            out.print(ValidateSAML2.getMessage("validator.link.auth.failed", locale));
            out.print("</a>");
        %>
        </td></tr>
        </table>
    </td></tr>
    </table>
<!-- content -->
</div>
       
</body>
</html>
