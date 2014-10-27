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
  
   $Id: ssoadm.jsp,v 1.9 2010/01/20 00:46:52 veiming Exp $
  
--%>

<%--
   Portions copyright 2010-2014 ForgeRock AS.
--%>

<%@ page import="com.iplanet.am.util.SystemProperties" %>
<%@ page import="com.sun.identity.cli.*" %>
<%@ page import="com.sun.identity.shared.Constants" %>
<%@ page import="java.text.MessageFormat" %>

<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>OpenAM</title>
    <link rel="stylesheet" type="text/css" href="com_sun_web_ui/css/css_ns6up.css" />
    <link rel="shortcut icon" href="com_sun_web_ui/images/favicon/favicon.ico" type="image/x-icon" />
    <script language="Javascript" src="js/admincli.js"></script>
</head>
<body class="DefBdy">
    <div class="SkpMedGry1"><a href="#SkipAnchor3860"><img src="com_sun_web_ui/images/other/dot.gif" alt="Jump to End of Masthead" border="0" height="1" width="1" /></a></div><div class="MstDiv">
    <table class="MstTblBot" title="" border="0" cellpadding="0" cellspacing="0" width="100%">
        <tr>
        <td class="MstTdTtl">
        <div class="MstDivTtl"><img name="AMConfig.configurator.ProdName" src="com_sun_web_ui/images/PrimaryProductName.png" alt="OpenAM" border="0" /></div>
        </td>
        </tr>
    </table>
    </div>
    <table class="SkpMedGry1" border="0" cellpadding="5" cellspacing="0" width="100%"><tr><td><img src="com_sun_web_ui/images/other/dot.gif" alt="Jump to End of Masthead" border="0" height="1" width="1" /></a></td></tr></table>
    <table border="0" cellpadding="10" cellspacing="0" width="100%"><tr><td></td></tr></table>

<%@ include file="/WEB-INF/jsp/admincheck.jsp" %>
<%

    SSOToken ssoToken = requireAdminSSOToken(request, response, out, "showServerConfig.jsp");
    if (ssoToken == null) {
%>
</body></html>
<%
        return;
    }
%>
<table cellpadding=5>
<tr>
<td>

<pre>
<%
    String strDisabled = SystemProperties.get("ssoadm.disabled", "true");
    if (Boolean.parseBoolean(strDisabled)) {
        response.sendRedirect(SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR));

    } else {

        try {
            WebCLIHelper helper = new WebCLIHelper(
                    request,
                    "com.sun.identity.cli.AccessManager,com.sun.identity.federation.cli.FederationManager",
                    "ssoadm",
                    request.getContextPath() + "/ssoadm.jsp");
            out.println(helper.getHTML(request, ssoToken));
            Object[] param = {"0"};
            out.println(MessageFormat.format(CLIConstants.JSP_EXIT_CODE_TAG, param));

        } catch (CLIException e) {
            Object[] param = {Integer.toString(e.getExitCode())};
            out.println(MessageFormat.format(CLIConstants.JSP_EXIT_CODE_TAG, param));
            out.println(WebCLIHelper.escapeTags(e.getMessage()));
        }
    }
%>

</pre>
</td></tr>
</table>
</body></html>
