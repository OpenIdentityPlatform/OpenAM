<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: Configurator.jsp,v 1.4 2009/11/03 00:51:42 madan_ranganath Exp $

--%>
<%--
   Portions Copyrighted 2012-2013 ForgeRock Inc
   Portions Copyrighted 2012 Open Source Solution Technology Corporation
--%>

<html>
<head>
<title>Configure IDP Discovery Service</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<link rel="stylesheet" type="text/css" href="com_sun_web_ui/css/css_ns6up.css" />


<%@ page import="
com.sun.identity.saml2.idpdiscovery.IDPDiscoveryWARConfigurator,
com.sun.identity.saml2.idpdiscovery.SystemProperties,
java.io.*,
java.security.AccessController,
java.util.Properties"
%>

<%
    String configFile = System.getProperty("user.home") +
        File.separator + "libIDPDiscoveryConfig.properties";
    String configTemplate = "/libIDPDiscoveryConfig.properties";
    String errorMsg = null;
    boolean configured = false;
    String debugDir = null;
    String debugLevel = null; 
    String cookieType = null;
    String cookieDomain = null;
    String secureCookie = null;
    String encodeCookie = null;
    String httpOnlyCookie = null;

    File configF = new File(configFile);
    if (configF.exists()) {
        errorMsg = "The IDP Discpvery Service have already been configued.<br>"
            + "Configuration file : " + configFile + "<br><p><br>";
        // reinitialize properties
        SystemProperties.initializeProperties(configFile);
        configured = true;
    } else {
        debugLevel = request.getParameter("debugLevel");
        cookieType = request.getParameter("cookieType");
        cookieDomain = request.getParameter("cookieDomain");
        secureCookie = request.getParameter("secureCookie");
        debugDir = request.getParameter("debugDir");
        encodeCookie = request.getParameter("encodeCookie");
        httpOnlyCookie = request.getParameter("httpOnlyCookie");
        String submit = request.getParameter("submit");
        String servletPath = request.getServletPath();

        if (submit != null) { 
            if ((debugLevel != null) && !debugLevel.equals("") && 
                (cookieType != null) && !cookieType.equals("") && 
                (secureCookie != null) && !secureCookie.equals("") && 
                (debugDir != null) && !debugDir.equals("") &&
                (encodeCookie != null) && !encodeCookie.equals("") &&
                (httpOnlyCookie != null) && !httpOnlyCookie.equals("")) {
                if (cookieDomain == null) {
                    cookieDomain = "";
                }
                Properties props = new Properties();
                props.setProperty("DEBUG_DIR", debugDir);
                props.setProperty("DEBUG_LEVEL", debugLevel);
                props.setProperty("COOKIE_TYPE", cookieType);
                props.setProperty("COOKIE_DOMAIN", cookieDomain);
                props.setProperty("SECURE_COOKIE", secureCookie);
                props.setProperty("ENCODE_COOKIE", encodeCookie);
                props.setProperty("HTTP_ONLY_COOKIE", httpOnlyCookie);
                try {
                    IDPDiscoveryWARConfigurator configurator = 
                        new IDPDiscoveryWARConfigurator();
                    configurator.createIDPDiscoveryConfig(configFile, 
                        configTemplate, props);
                    configurator.setIDPDiscoveryConfig(configFile);
                } catch (IOException ioex) {
                    ioex.printStackTrace();
                    errorMsg = "Unable to create sample AMConfig.properties " +
                       "file: " + ioex.getMessage();
                }
                configured = true;
            } else {
                errorMsg = "Missing one or more required fields."; 
            }
        }
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
<div class="MstDivTtl"><img name="ProdName" src="console/images/PrimaryProductName.png" alt="" /></div></td><td class="MstTdLogo" width="1%"><img name="RMRealm.mhCommon.BrandLogo" src="com_sun_web_ui/images/other/javalogo.gif" alt="Java(TM) Logo" border="0" height="55" width="31" /></td></tr></tbody></table>
<table class="MstTblEnd" border="0" cellpadding="0" cellspacing="0" width="100%"><tbody><tr><td><img name="RMRealm.mhCommon.EndorserLogo" src="com_sun_web_ui/images/masthead/masthead-sunname.gif" alt="Sun(TM) Microsystems, Inc." align="right" border="0" height="10" width="108" /></td></tr></tbody></table></div><div class="SkpMedGry1"><a name="SkipAnchor2089" id="SkipAnchor2089"></a></div>
<div class="SkpMedGry1"><a href="#SkipAnchor4928"><img src="com_sun_web_ui/images/other/dot.gif" alt="Jump Over Tab Navigation Area. Current Selection is: Access Control" border="0" height="1" width="1" /></a></div>
                                                                                
<table border="0" cellpadding="10" cellspacing="0" width="100%">
<tr><td>


<%
    if (!configured) {
%>

<h3>Configuring IDP Discovery Service</h3>

<form action="Configurator.jsp" method="GET" 
    name="idpdiscoveryconfigurator">
    Please provide the IDP Discovery service information
    <p>&nbsp;</p>    

    <table border=0 cellpadding=5 cellspacing=0>

<%
    if (errorMsg != null) {
%>
    <tr>
    <td colspan="2" align="left">
    <b><font color="red"><%= errorMsg %></font></b>
    <br><br>
    </td>
    </tr>
<%
}
%>

    <tr>
    <td>Debug directory</td>
    <td><input name="debugDir" type="text" size="20" value="<%= debugDir == null ? "" : debugDir %>" /></td>
    </tr>
    <tr>
    <td>Debug Level:</td>
    <td>
        <select name="debugLevel">
            <option value ="error" selected="selected">error</option>
            <option value ="warning">warning</option>
            <option value ="message">message</option>
            <option value ="off">off</option>
        </select>
    </td>
    </tr>
    <tr>
    <td>Cookie Type:</td>
    <td>
        <input type="radio" name="cookieType" value="PERSISTENT" CHECKED>PERSISTENT
        <input type="radio" name="cookieType" value="SESSION">SESSION
    </td>
    </tr>
    <tr>
    <td>Cookie Domain:</td>
    <td><input name="cookieDomain" type="text" size="20" value="<%= cookieDomain == null ? "" : cookieDomain %>" /></td>
    </tr>
    <tr>
    <td>Secure Cookie:</td>
    <td>
        <input type="radio" name="secureCookie" value="true">True
        <input type="radio" name="secureCookie" value="false" CHECKED>False
    </td>
    </tr>
    <tr>
    <td>Encode Cookie:</td>
    <td>
        <input type="radio" name="encodeCookie" value="true" CHECKED>True
        <input type="radio" name="encodeCookie" value="false">False
    </td>
    </tr>
    <tr>
        <td>  </td>
    </tr>
    <tr>
    <td>HTTP-Only Cookie:</td>
    <td>
        <input type="radio" name="httpOnlyCookie" value="true" >True
        <input type="radio" name="httpOnlyCookie" value="false" CHECKED>False
    </td>
    </tr>
    <tr>
    <td colspan="2" align="center">
    <input type="submit" name="submit" value="Configure" />
    <input type="reset" value="Reset" />
    </td>
    </tr>
    </table>
</form>

<%
} else {
%>
<p>&nbsp;</p>
<%
    if (errorMsg != null) {
%>
<%= errorMsg %>
<%
} else {
%>
IDP Discovery servce is successfully configured.<br>
Configuration property is created at <%= configFile %><br>
<br>
<p>
<%
    }
}
%>
</td></tr></table>
</body>
</html>
