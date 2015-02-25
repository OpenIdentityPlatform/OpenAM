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

   $Id: index.jsp,v 1.4 2008/08/15 01:05:35 veiming Exp $

--%>

<%@ page
import="com.sun.identity.common.SystemConfigurationUtil"
%>
                                                                                
<%
    String deployuri = null;
    deployuri = SystemConfigurationUtil.getProperty(
        "com.iplanet.am.services.deploymentDescriptor");
    if ((deployuri == null) || (deployuri.length() == 0)) {
        deployuri = "../../..";
    }
%>


<html>
<head>
<title>Identity Provider</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<link rel="stylesheet" type="text/css" href="<%= deployuri %>/com_sun_web_ui/css/css_ns6up.css" />
<%
    String metaAlias = "/idp";
%>
<%@ include file="../header.jsp"%>
<%
    if (redirectUrl != null) {
        out.println("<script language=\"Javascript\">");
        Object[] param = {metaAlias};
        out.println("top.location.replace('" + 
            MessageFormat.format(redirectUrl, param) + "');");
        out.println("</script>");
    }
%>

</head>
<body class="DefBdy">
                                                                                
<div class="MstDiv"><table width="100%" border="0" cellpadding="0" cellspacing="0" class="MstTblTop" title="">
<tbody><tr>
<td nowrap="nowrap">&nbsp;</td>
<td nowrap="nowrap">&nbsp;</td>
</tr></tbody></table>
                                                                                
<table width="100%" border="0" cellpadding="0" cellspacing="0" class="MstTblBot" title="">
<tbody><tr>
<td class="MstTdTtl" width="99%">
<div class="MstDivTtl"><img name="ProdName" src="<%= deployuri %>/console/images/PrimaryProductName.png" alt="" /></div></td><td class="MstTdLogo" width="1%"><img name="RMRealm.mhCommon.BrandLogo" src="<%= deployuri %>/com_sun_web_ui/images/other/javalogo.gif" alt="Java(TM) Logo" border="0" height="55" width="31" /></td></tr></tbody></table>
<table class="MstTblEnd" border="0" cellpadding="0" cellspacing="0" width="100%"><tbody><tr><td><img name="RMRealm.mhCommon.EndorserLogo" src="<%= deployuri %>/com_sun_web_ui/images/masthead/masthead-sunname.gif" alt="Sun(TM) Microsystems, Inc." align="right" border="0" height="10" width="108" /></td></tr></tbody></table></div><div class="SkpMedGry1"><a name="SkipAnchor2089" id="SkipAnchor2089"></a></div>
<div class="SkpMedGry1"><a href="#SkipAnchor4928"><img src="<%= deployuri %>/com_sun_web_ui/images/other/dot.gif" alt="Jump Over Tab Navigation Area. Current Selection is: Access Control" border="0" height="1" width="1" /></a></div>
                                                                                
<table border="0" cellpadding="10" cellspacing="0" width="100%">
<tr><td>
<p>&nbsp;</p>
&lt; <a href="../index.html">ID-FF Sample Page</a>
<p>&nbsp;</p>


<table width="100%" border="0" cellspacing="0" cellpadding="0" >

    <tr>
    <td align="center">
        <h2>Welcome To Identity Provider</h2>
    </td>
    </tr>
    <tr><td><hr size="1" noshade /></td></tr>

    <tr>
    <td>
    <p>
    Followings are the tasks that can be performed on the Identity Provider.
    <ul>
    <li><a href="<%= baseURL %>/config/federation/default/NameRegistration.jsp?metaAlias=<%= metaAlias %>">Register Name Identifier</a></li>
    <li><a href="<%= baseURL %>/liberty-logout?metaAlias=<%= metaAlias %>">Logout</a></li>
    <li><a href="<%= baseURL %>/config/federation/default/Termination.jsp?metaAlias=<%= metaAlias %>">Terminate Federation</a></li>
    </ul>
    </p>
    </td>
    </tr>
</table>
</td></tr></table>

</body>
</html>
