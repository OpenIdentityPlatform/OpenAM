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

   $Id: configure.jsp,v 1.6 2008/11/25 23:50:41 exu Exp $

--%>
<%--
   Portions Copyrighted 2012 ForgeRock Inc
   Portions Copyrighted 2012 Open Source Solution Technology Corporation
--%>

<html>
<head>
<title>Configure Identity Provider</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<link rel="stylesheet" type="text/css" href="../../../com_sun_web_ui/css/css_ns6up.css" />

<%
    String errorMsg = null;
%>

<%@ include file="../util.jspf" %>
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
    boolean configured = false;
    String hostedIDPEntityID = null;
    String remoteSPEntityID = null;
    String SAMPLE_COT_NAME = "samplemultiprotocolcot";
    String gotoURL = request.getRequestURL().toString();
    String redirectURL = null;
    if (request.getQueryString() != null) {
        gotoURL = gotoURL + "?" + request.getQueryString(); 
    }
    
    if ((localAuthUrl == null) && (errorMsg == null)) {
        String spBaseUrl = request.getParameter(SP_BASE_URL);
        String fedProtocol = request.getParameter(PROTOCOL_PARAM_NAME);
        hostedIDPEntityID = baseURL.trim() + "/" + SAMPLE_PREFIX + 
            fedProtocol + IDP_SUFFIX;
        
        if ((spBaseUrl != null) && (fedProtocol != null)) {
            spBaseUrl = spBaseUrl.trim();
            fedProtocol = fedProtocol.trim();

            if ((spBaseUrl.length() > 0) && (fedProtocol.length() > 0)) {
                remoteSPEntityID = spBaseUrl + "/" + SAMPLE_PREFIX + 
                    fedProtocol + SP_SUFFIX;
                try {
                    redirectURL = spBaseUrl + 
                        "/samples/multiprotocol/sp/configurationDone.jsp?" +
                        STATUS + "=success";
                    if (fedProtocol.equals(SingleLogoutManager.SAML2)) {
                        configureSAML2IdentityProvider(hostedIDPEntityID, 
                            remoteSPEntityID, request);
                        configured = true;
                    } else if (fedProtocol.equals(SingleLogoutManager.IDFF)) {
                        configureIDFFIdentityProvider(hostedIDPEntityID, 
                            remoteSPEntityID, request);
                        configured = true;
                    } else if (fedProtocol.equals(SingleLogoutManager.WS_FED)) {
                        configureWSFedIdentityProvider(hostedIDPEntityID, 
                            remoteSPEntityID, request);
                        configured = true;
                    } else {
                        errorMsg = "Invalid federation protocol " + fedProtocol;
                    }
                } catch (Exception clie) {
                    errorMsg = clie.getMessage();
                }
            } else {
                errorMsg = "Please initialize configuration from a Service Provider";
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
    You have not logged in to this Identity Provider. Click 
    <a href=<%= baseURL + "/UI/Login?goto=" + URLEncoder.encode(gotoURL) %>>
       here</a> to login.

<%
    } else {
        if (!configured) {
%>

<h3>Configuring this instance as Identity Provider</h3>

    This sample will create and load metadata for a hosted Identity Provider and a remote Service Provider.<br/>
    It will also setup circle of trust for the two providers.
    <p>&nbsp;</p>
    Please initialize new configuration from a Service Provider.
    <p>&nbsp;</p>

    <table border=0 cellpadding=5 cellspacing=0>

<%
    if (errorMsg != null) {
%>
    <tr>
    <td  align="center">
    <b><font color="red"><%= errorMsg %></font></b>
    <br><br>
    </td>
    </tr>
    </table>
<%
}
    String saml2IDPEntityID = null;
    String idffIDPEntityID = null;
    String wsfedIDPEntityID = null;
    Set saml2SPEntityIDs = new HashSet();
    Set idffSPEntityIDs = new HashSet();
    Set wsfedSPEntityIDs = new HashSet();
    String REALM = "/";
    CircleOfTrustManager cotManager = new CircleOfTrustManager();
    CircleOfTrustDescriptor cot = null;
    try {
        cot = cotManager.getCircleOfTrust(REALM, SAMPLE_COT_NAME);
    } catch (Exception e) {
        // ignore, as COT might not be exists.
    }

    if (cot != null) {
        Set saml2Provider = cot.getTrustedProviders(SingleLogoutManager.SAML2);
        if ((saml2Provider != null) && !saml2Provider.isEmpty()) {
            Iterator it = saml2Provider.iterator();
            while (it.hasNext()) {
                String entityID = (String) it.next();
                SAML2MetaManager mm = SAML2Utils.getSAML2MetaManager();
                com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement
                        config3 = mm.getEntityConfig(REALM, entityID);
                com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement
                        idpConfig = mm.getIDPSSOConfig(REALM, entityID);
                com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement
                        spConfig = mm.getSPSSOConfig(REALM, entityID);
                if (config3.isHosted()) {
                    // hosted provider
                    if (idpConfig != null) {
                        saml2IDPEntityID = config3.getEntityID();
                    }
                } else {
                    if (spConfig != null) {
                        saml2SPEntityIDs.add(config3.getEntityID());
                    }
                }
            }
        }
        
        Set idffProvider = cot.getTrustedProviders(SingleLogoutManager.IDFF);
        if ((idffProvider != null) && !idffProvider.isEmpty()) {
            Iterator it = idffProvider.iterator();
            while (it.hasNext()) {
                String entityID = (String) it.next();
                IDFFMetaManager mm = new IDFFMetaManager(ssoToken);
                com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement
                    config2 = mm.getEntityConfig(REALM, entityID);
                com.sun.identity.federation.jaxb.entityconfig.IDPDescriptorConfigElement
                        idpConfig = mm.getIDPDescriptorConfig(REALM, entityID);
                com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement
                        spConfig = mm.getSPDescriptorConfig(REALM, entityID);
                if (config2.isHosted()) {
                    // hosted provider
                    if (idpConfig != null) {
                        idffIDPEntityID = config2.getEntityID();
                    }
                } else {
                    // remote provider
                    if (spConfig != null) {
                        idffSPEntityIDs.add(config2.getEntityID());
                    }
                }
            }
        }
        
        Set wsfedProviders = cot.getTrustedProviders(SingleLogoutManager.WS_FED);
        if ((wsfedProviders != null) && !wsfedProviders.isEmpty()) {
            Iterator it = wsfedProviders.iterator();
            while (it.hasNext()) {
                String entityID = (String) it.next();
                WSFederationMetaManager wsfedMetaManager = new WSFederationMetaManager();
                com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement
                    config3 = wsfedMetaManager.getEntityConfig(defaultRealm, entityID);
                com.sun.identity.wsfederation.jaxb.entityconfig.IDPSSOConfigElement
                        idpConfig = wsfedMetaManager.getIDPSSOConfig(defaultRealm, entityID);
                com.sun.identity.wsfederation.jaxb.entityconfig.SPSSOConfigElement
                        spConfig = wsfedMetaManager.getSPSSOConfig(defaultRealm, entityID);
                if (config3.isHosted()) {
                    // hosted provider
                    if (idpConfig != null) {
                        wsfedIDPEntityID = config3.getFederationID();
                    }
                } else {
                    // remote provider
                    if (spConfig != null) {
                        wsfedSPEntityIDs.add(config3.getFederationID());
                    }
                }
            }
        }
    }

    if ((saml2IDPEntityID != null) || (idffIDPEntityID != null) ||
        (wsfedIDPEntityID != null)) { 
%>

    Current Protocol Configured:<br/><p>
    <table border=2 cellpadding=5 cellspacing=0>
    <tr>
        <td><b>Protocol</b></td>
        <td><b>Hosted Identity Provider ID</b></td>
        <td><b>Remote Service provider ID</b></td>
    </tr>
<%
        if (saml2IDPEntityID != null) {
%>
    <tr>
    <td>SAML2</td>
    <td><%= saml2IDPEntityID %></td>
    <td>
<%
            Iterator it = saml2SPEntityIDs.iterator();
            while (it.hasNext()) {
                String spID = (String) it.next();
%>
        <%= spID %><br/>
<%
            }
%>
    </td>
    </tr>
    <tr>
<%
        }
        if (idffIDPEntityID != null) {
%>
    <tr>
    <td>ID-FF</td>
    <td><%= idffIDPEntityID %></td>
    <td>
<%
            Iterator it = idffSPEntityIDs.iterator();
            while (it.hasNext()) {
                String spID = (String) it.next();
%>
        <%= spID %><br/>
<%
            }
%>
    </td>
    </tr>
    <tr>
<%
        }
        if (wsfedIDPEntityID != null) {
%>
    <tr>
    <td>WS-Federation</td>
    <td><%= wsfedIDPEntityID %></td>
    <td>
<%
            Iterator it = wsfedSPEntityIDs.iterator();
            while (it.hasNext()) {
                String spID = (String) it.next();
%>
        <%= spID %><br/>
<%
            }
%>
    </td>
    </tr>
    <tr>
<%
        }
%>
    </table>
<%
    } else {
%>
         No federation protocol configured for this sample yet.<br/>
         <p><br/>
<%
    }
  } else {
      response.sendRedirect(redirectURL);
  }
}
%>
</td></tr></table>
</body>
</html>
