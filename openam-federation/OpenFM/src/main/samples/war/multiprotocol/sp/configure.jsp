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

   $Id: configure.jsp,v 1.6 2008/11/25 23:50:42 exu Exp $

--%>
<%--
   Portions Copyrighted 2012 ForgeRock Inc
   Portions Copyrighted 2012 Open Source Solution Technology Corporation
--%>

<html>
<head>
<title>Configure Service Provider</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<link rel="stylesheet" type="text/css" href="../../../com_sun_web_ui/css/css_ns6up.css"/>

<%
    String errorMsg = null;
%>

<%@ include file="../util.jspf"%>
<%@ page
    import="com.sun.identity.federation.meta.IDFFMetaManager,
        com.sun.identity.multiprotocol.SingleLogoutManager,
        com.sun.identity.saml2.common.SAML2Utils,
        com.sun.identity.saml2.meta.SAML2MetaException,
        com.sun.identity.saml2.meta.SAML2MetaManager,
        com.sun.identity.saml2.meta.SAML2MetaUtils,
        com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement,
        com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement,
        com.sun.identity.cot.CircleOfTrustDescriptor,
        com.sun.identity.cot.CircleOfTrustManager,
        com.sun.identity.cot.COTConstants,
        java.util.HashSet,
        java.util.Iterator,
        java.util.List,
        java.util.Set"
%>
<%
    if (localAuthUrl != null) {
        out.println("<script language=\"Javascript\">");
        out.println("top.location.replace('" + localAuthUrl + "');");
        out.println("</script>");
    }
%>

<%
    boolean doneConfiguration = false;
    boolean alreadyConfigured = false;
    String hostedSPEntityID = null;
    String remoteIDPEntityID = null;
    String fedProtocol = null;
    String proto = request.getParameter("proto");
    String host = request.getParameter("host");
    String port = request.getParameter("port");
    String deploymenturi = request.getParameter("deploymenturi");
    if ((localAuthUrl == null) && (errorMsg == null)) {
        // fedProtocol would be value of "saml2", or "idff" or "ws-fed"
        fedProtocol = request.getParameter(PROTOCOL_PARAM_NAME);
        
        hostedSPEntityID = baseURL.trim() + "/" + SAMPLE_PREFIX + 
            fedProtocol + SP_SUFFIX;
        
        if ((proto != null) && (host != null) && (port != null) &&
            (deploymenturi != null)) {
            proto = proto.trim();
            host = host.trim();
            port = port.trim();
            deploymenturi = deploymenturi.trim();
            
            if ((proto.length() > 0) && (host.length() > 0) && 
                (port.length() > 0) && (deploymenturi.length() > 0) &&
                (fedProtocol.length() > 0)) {
                if (deploymenturi.charAt(0) != '/') {
                    deploymenturi = "/" + deploymenturi;
                }
                remoteIDPEntityID = new StringBuffer()
                    .append(proto).append("://").append(host).append(":")
                    .append(port).append(deploymenturi).append("/")
                    .append(SAMPLE_PREFIX).append(fedProtocol)
                    .append(IDP_SUFFIX).toString();
                try {
                    if (fedProtocol.equals(SingleLogoutManager.SAML2)) {
                        configureSAML2ServiceProvider(remoteIDPEntityID, 
                            hostedSPEntityID, request);
                        doneConfiguration = true;
                    } else if (fedProtocol.equals(SingleLogoutManager.IDFF)) {
                        configureIDFFServiceProvider(remoteIDPEntityID, 
                            hostedSPEntityID, request);
                        doneConfiguration = true;
                    } else if (fedProtocol.equals(SingleLogoutManager.WS_FED)) {
                        configureWSFedServiceProvider(remoteIDPEntityID, 
                            hostedSPEntityID, request);
                        doneConfiguration = true;
                    } else {
                        errorMsg = "Invalid federation protocol " + fedProtocol;
                    }
                } catch (Exception clie) {
                    clie.printStackTrace();
                    errorMsg = clie.getMessage();
                }
            } else {
                errorMsg = "Required fields are missing.";
            }
        } else {
            try {
                checkCurrentSPConfiguration(baseURL.trim() + "/" + 
                    SAMPLE_PREFIX, ssoToken);
            } catch (Exception e) {
                alreadyConfigured = true;
                errorMsg = e.getMessage();
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
<div class="MstDivTtl"><img name="ProdName" src="../../../console/images/PrimaryProductName.png" alt="" /></div></td><td class="MstTdLogo" width="1%"><img name="RMRealm.mhCommon.BrandLogo" src="../../../com_sun_web_ui/images/other/javalogo.gif" alt="Java(TM) Logo" border="0" height="55" width="31" /></td></tr></tbody></table>
<table class="MstTblEnd" border="0" cellpadding="0" cellspacing="0" width="100%"><tbody><tr><td><img name="RMRealm.mhCommon.EndorserLogo" src="../../../com_sun_web_ui/images/masthead/masthead-sunname.gif" alt="Sun(TM) Microsystems, Inc." align="right" border="0" height="10" width="108" /></td></tr></tbody></table></div><div class="SkpMedGry1"><a name="SkipAnchor2089" id="SkipAnchor2089"></a></div>
<div class="SkpMedGry1"><a href="#SkipAnchor4928"><img src="../../../com_sun_web_ui/images/other/dot.gif" alt="Jump Over Tab Navigation Area. Current Selection is: Access Control" border="0" height="1" width="1" /></a></div>
                                                                                
<table border="0" cellpadding="10" cellspacing="0" width="100%">
<tr><td>

<%
    if (!loggedIn) {
  %>
   <p>&nbsp;</p>
    You have not logged in to this Service Provider. Click 
    <a href=<%= baseURL+"/UI/Login?goto=" + baseURL + "/" + baseURI %>>here</a> to login .
 <% 
    } else { 
    if (!doneConfiguration) {
%>

<h3>Configuring this instance as Service Provider</h3>

<form action="configure.jsp" method="GET">
    This sample will create and load metadata for a hosted Service Provider 
    and a remote Identity Provider. <br/> 
    It will also setup circle of trust for the two providers.
    <p>&nbsp;</p>    

    <p>    

    <table border=0 cellpadding=5 cellspacing=0>

<%
    if (errorMsg != null) {
%>
    <tr>
    <td colspan="2" align="center">
    <b><font color="red"><%= errorMsg %></font></b>
    <br><br>
    </td>
    </tr>
<%
}
    if (!alreadyConfigured) {
%>

    <tr>
    <td colspan="2">Please provide the remote Identity Provider (must also be an
        OpenAM instance) information:</td>
    </tr>
    <tr>
    <td valign="center" width="25%">Federation Protocol:</td>
    <td align="left">
        <input name="spFederationProtocol" type="radio" value="saml2"/>SAML2<br/>
        <input name="spFederationProtocol" type="radio" value="idff"/>IDFF<br/>
        <input name="spFederationProtocol" type="radio" value="wsfed"/>WS-Federation<br/>
    </td>    
    </tr>
    <td valign="center">Protocol:</td>
    <td>
        <input name="proto" type="radio" value="http" />HTTP<br/>
        <input name="proto" type="radio" value="https" />HTTPS<br/> 
    </td>
    </tr>
    <tr>
    <td>Host:</td>
    <td><input name="host" type="text" size="30" value="" /></td>
    </tr>
    <tr>
    <td>Port:</td>
    <td><input name="port" type="text" size="6" value="" /></td>
    </tr>
    <tr>
    <td>Deployment URI:</td>
    <td><input name="deploymenturi" type="text" size="15" value="" /></td>
    </tr>
    <tr>
    <td colspan="2" align="center">
    <input type="submit" value="Configure" />
    <input type="reset" value="Reset" />
    </td>
    </tr>
    
<%
}
%>

    </table>
</form>

<%
} else {
        String idpBaseUrl = proto + "://" + host + ":" + port + deploymenturi;
        String idpSampleUrl = idpBaseUrl + 
            "/samples/multiprotocol/idp/configure.jsp";
        String redirectURL = idpSampleUrl + "?" + SP_BASE_URL +
            "=" + baseURL + "&" + PROTOCOL_PARAM_NAME + "=" + fedProtocol;
%>
<p>&nbsp;</p>
Hosted Service Provider <%= hostedSPEntityID %> is created.
<p>&nbsp;</p>
Remote Identity Provider <%= remoteIDPEntityID %> is created.
<p>&nbsp;</p>
Circle of Trust <%= SAMPLE_COT_NAME %> is created.
<p>&nbsp;</p>
<p>&nbsp;</p>
Service Provider is configured. Click <a href="<%= redirectURL %>">here</a> to configure
remote Identity Provider.
<%
}
}
%>

</td></tr>
</table>

</body>
</html>
