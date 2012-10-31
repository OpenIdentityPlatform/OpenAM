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

   $Id: validatorFooter.jsp,v 1.7 2009/10/27 17:57:38 asyhuang Exp $

--%>

<%@ page import="com.sun.identity.common.SystemConfigurationUtil" %>
<%@ page import="com.sun.identity.shared.Constants" %>
<%@ page import="com.sun.identity.workflow.ValidateSAML2" %>
<%@ page import="org.owasp.esapi.ESAPI" %>
<%@ page contentType="text/html; charset=utf-8" language="java" %>

<%
    String deployuri = SystemConfigurationUtil.getProperty(
        Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
    String msg = request.getParameter("m");
    String locale = request.getParameter("locale");
    String backLoginBtnLabel = ValidateSAML2.getMessage(
        "button.backtoLogin", locale);
    String message = ((msg != null) && (msg.length() > 0)) ?
        ValidateSAML2.getMessage(msg, locale) : null;
    org.owasp.esapi.Encoder enc = ESAPI.encoder();
    message=enc.encodeForHTML(message);
%>
<html>
<head>
<link rel="stylesheet" type="text/css" href="<%= deployuri %>/com_sun_web_ui/css/css_ns6up.css" />
<link rel="stylesheet" type="text/css" href="<%= deployuri %>/console/css/openam.css" />
</head>
<body class="DefBdy">

<table border=0 cellpadding=5 cellspacing=0 width="100%">
<tr>
<td width="10"></td>
<td align="left">
<%
    if (message != null) {
        out.println(message);
    }
%>
</td>
<td align="right" nowrap="nowrap" valign="bottom">
<div class="TtlBtnDiv"> <input name="btnBacktoLogin" type="submit" class="Btn1" value="<%= backLoginBtnLabel %>" onmouseover="javascript: this.className='Btn1Hov'" onmouseout="javascript: this.className='Btn1'" onblur="javascript: this.className='Btn1'" onfocus="javascript: this.className='Btn1Hov'" onClick="top.cancelOp();return false;"/>
</div>
</td>
<td width="10"></td>
</tr>
</table>
</body>
</head>
</html>
