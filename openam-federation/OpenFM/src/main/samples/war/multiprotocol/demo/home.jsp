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

   $Id: home.jsp,v 1.6 2008/11/25 23:50:41 exu Exp $

--%>
<%--
   Portions Copyrighted 2011 ForgeRock AS
--%>
<%@ page import="com.iplanet.sso.SSOTokenManager,
            com.iplanet.sso.SSOException,
            com.iplanet.sso.SSOToken"
%>
<%@ page import="com.sun.identity.common.SystemConfigurationUtil" %>
<%@ page import="com.sun.identity.cot.CircleOfTrustManager" %>
<%@ page import="com.sun.identity.cot.CircleOfTrustDescriptor" %>
<%@ page import="com.sun.identity.federation.accountmgmt.FSAccountManager" %>
<%@ page import="com.sun.identity.federation.meta.IDFFMetaManager" %>
<%@ page import="com.sun.identity.multiprotocol.MultiProtocolUtils" %>
<%@ page import="com.sun.identity.multiprotocol.SingleLogoutManager" %>
<%@ page import="com.sun.identity.saml2.common.AccountUtils" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Constants" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Utils" %>
<%@ page import="com.sun.identity.saml2.meta.SAML2MetaManager" %>
<%@ page import="com.sun.identity.wsfederation.meta.WSFederationMetaManager" %>
<%@ page import="java.util.*, java.net.URLEncoder" %>

<%
    String deployuri = SystemConfigurationUtil.getProperty(
        "com.iplanet.am.services.deploymentDescriptor");
    String UNIVERSAL_IDENTIFIER = "sun.am.UniversalIdentifier";
    if ((deployuri == null) || (deployuri.length() == 0)) {
        deployuri = "../../..";
    }

    String utf8 = "UTF-8";
    String REALM = "/";
    String SAMPLES_DIR = "samples/multiprotocol/demo";

    // Change the value if you want to show  a different title in your install
    String idpTitle = "Multi-Federation Protocol Identity Provider";

    // Change the value if you want to show  a different title in your install
    String spTitle = "Service Provider";
    
    String SAMPLE_COT_NAME = "samplemultiprotocolcot";
    String IDFF_PROTOCOL = "ID-FF";
    String SAML2_PROTOCOL = "SAMLv2";
    String WSFED_PROTOCOL = "WS-Federation";

    boolean iAmIDP = false;
    boolean iAmSAML2SP = false;
    boolean iAmIDFFSP = false;
    boolean iAmWSFedSP = false;

    String spProtocol = "";
    String saml2SPMetaAlias = null;
    String saml2SPEntityID = null;
    String idffSPMetaAlias = null;
    String idffSPEntityID = null;
    String wsfedSPMetaAlias = null;
    String wsfedSPEntityID = null;
    String saml2IDPMetaAlias = null;
    String saml2IDPEntityID = null;
    String idffIDPMetaAlias = null;
    String idffIDPEntityID = null;
    String wsfedIDPMetaAlias = null;
    String wsfedIDPEntityID = null;

    String thisUrl = request.getRequestURL().toString();
    String appBase = thisUrl.substring(0, thisUrl.lastIndexOf("/samples") + 1);
    String localLoginUrl = appBase + "UI/Login";
    String localLogoutUrl = appBase + "UI/Logout";
    String samplesBase = appBase + SAMPLES_DIR + "/";
    
    String myTitle = "";

    SSOToken ssoToken = null;
    boolean userLoggedIn = false;
    String userName = "";
    String userLabel = "";
    boolean federatedWithPartner = false; 

    CircleOfTrustManager cotManager = new CircleOfTrustManager();
    CircleOfTrustDescriptor cot = null;
    try {
        cot = cotManager.getCircleOfTrust(REALM, SAMPLE_COT_NAME);
    } catch (Exception e) {
        // ignore as COT might not be created.
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
                        iAmIDP = true;
                        saml2IDPEntityID = config3.getEntityID();
                        saml2IDPMetaAlias = idpConfig.getMetaAlias();
                    } else if (spConfig != null) {
                        iAmSAML2SP = true;
                        saml2SPEntityID = config3.getEntityID();
                        saml2SPMetaAlias = spConfig.getMetaAlias();
                        spProtocol = SAML2_PROTOCOL;
                    }
                } else {
                    // remote provider
                    if (idpConfig != null) {
                        saml2IDPEntityID = config3.getEntityID();
                    } else if (spConfig != null) {
                        saml2SPEntityID = config3.getEntityID();
                    }
                }
            }
            //idpTitle = saml2IDPEntityID;
        }
        
        Set idffProvider = cot.getTrustedProviders(SingleLogoutManager.IDFF);
        if ((idffProvider != null) && !idffProvider.isEmpty()) {
            Iterator it = idffProvider.iterator();
            while (it.hasNext()) {
                String entityID = (String) it.next();
                IDFFMetaManager mm = new IDFFMetaManager(null);
                com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement 
                    config2 = mm.getEntityConfig(REALM, entityID);
                com.sun.identity.federation.jaxb.entityconfig.IDPDescriptorConfigElement
                            idpConfig = mm.getIDPDescriptorConfig(REALM, entityID);
                com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement
                            spConfig = mm.getSPDescriptorConfig(REALM, entityID);
                if (config2.isHosted()) {
                    // hosted provider
                    if (idpConfig != null) {
                        iAmIDP = true;
                        idffIDPEntityID = config2.getEntityID();
                        idffIDPMetaAlias = idpConfig.getMetaAlias();
                    } else if (spConfig != null) {
                        iAmIDFFSP = true;
                        idffSPEntityID = config2.getEntityID();
                        idffSPMetaAlias = spConfig.getMetaAlias();
                        spProtocol = IDFF_PROTOCOL;
                    }
                } else {
                    // remote provider
                    if (idpConfig != null) {
                        idffIDPEntityID = config2.getEntityID();
                    } else if (spConfig != null) {
                        idffSPEntityID = config2.getEntityID();
                    }
                }
            }
            //idpTitle = idffIDPEntityID;
        }
        
        Set wsfedProviders = cot.getTrustedProviders(SingleLogoutManager.WS_FED);
        if ((wsfedProviders != null) && !wsfedProviders.isEmpty()) {
            Iterator it = wsfedProviders.iterator();
            while (it.hasNext()) {
                String entityID = (String) it.next();
                WSFederationMetaManager wsfedMetaManager = new WSFederationMetaManager();
                com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement
                    config3 = wsfedMetaManager.getEntityConfig(REALM, entityID);
                com.sun.identity.wsfederation.jaxb.entityconfig.IDPSSOConfigElement
                        idpConfig = wsfedMetaManager.getIDPSSOConfig(REALM, entityID);
                com.sun.identity.wsfederation.jaxb.entityconfig.SPSSOConfigElement
                        spConfig = wsfedMetaManager.getSPSSOConfig(REALM, entityID);
                if (config3.isHosted()) {
                    // hosted provider
                    if (idpConfig != null) {
                        iAmIDP = true;
                        wsfedIDPEntityID = config3.getFederationID();
                        wsfedIDPMetaAlias = idpConfig.getMetaAlias();
                    } else if (spConfig != null) {
                        iAmWSFedSP = true;
                        wsfedSPEntityID = config3.getFederationID();
                        wsfedSPMetaAlias = spConfig.getMetaAlias();
                        spProtocol = WSFED_PROTOCOL;
                    }
                } else {
                    // remote provider
                    if (idpConfig != null) {
                        wsfedIDPEntityID = config3.getFederationID();
                    } else if (spConfig != null) {
                        wsfedSPEntityID = config3.getFederationID();
                    }
                }
            }
        }
        
        if(!iAmIDP && !iAmSAML2SP && !iAmIDFFSP && !iAmWSFedSP) {
            response.sendError(response.SC_BAD_REQUEST, 
                "No Hosted Service or Identity Provider configured for this instance."
                + "<br/>Please configure the sample first.");
            return;
        }
    
        if (iAmIDP) {
            myTitle = idpTitle;
        } else {
            myTitle = spProtocol + " " +  spTitle;
        }
        
        try {
            SSOTokenManager tokenManager = SSOTokenManager.getInstance();
            ssoToken = tokenManager.createSSOToken(request);
            if ((ssoToken != null) && tokenManager.isValidToken(ssoToken)) {
                userLoggedIn = true;
                userName = ssoToken.getProperty(UNIVERSAL_IDENTIFIER);
                userLabel = userName;
                int j = userName.indexOf("=");
                int k = userName.indexOf(",");
                if ((j > 0) && (k > j)) {
                    userLabel = userName.substring(j+1,k).trim();
                }
                userLabel = userLabel.substring(0,1).toUpperCase() 
                        + ((userLabel.length() > 0) 
                        ? userLabel.substring(1, userLabel.length())
                        : "");
            }
        } catch (SSOException e) {
            //response.sendError(response.SC_INTERNAL_SERVER_ERROR);
        }
    
        if (userLoggedIn) {
           if (iAmSAML2SP) {
               federatedWithPartner = (AccountUtils.getAccountFederation(userName, 
                   saml2SPEntityID, saml2IDPEntityID) == null) ? false : true;
           } else if (iAmIDFFSP) {
               FSAccountManager manager = 
                   FSAccountManager.getInstance(idffSPMetaAlias);
               federatedWithPartner = (manager.readAccountFedInfo(userName,
                   idffIDPEntityID) == null) ? false : true; 
           } else if (iAmWSFedSP) {
               // no persistent federation for WS-Federation
               federatedWithPartner = false; 
           }
        }
    }  
%>

<html>
<head>
<title>Multi-Federation Protocol Demo</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<link rel="stylesheet" type="text/css" href="<%= deployuri %>/com_sun_web_ui/css/css_ns6up.css" />
</head>
<body>

<%@ include file="header.jspf" %>

<p>&nbsp;</p>
&lt; <a href="Readme.html">Multi-Federation Protocol Sample Page</a>

<p>&nbsp;</p>                                                                                
    <h3><center><%= myTitle%> <p>Welcome <%= userLoggedIn ? " " + userLabel : ""%> to the Demo.</center></h3>
    <hr/>
    <table cellpadding="2" cellspacing="2" border="0" width="100%">
    <tr>
    <td valign="top" align="left">
    <% if (cot == null) { %>
        This instance is not configured, please start sample configuration from a Service Provider.
       </tr>
    <%  } else {
            String status = request.getParameter("logoutStatus");
            if ((status != null) && status.equals("logoutSuccess")) {
    %>
            <b>Single Logout succeeded.</b><br><p>
    <%
            } else if ((status != null) && !status.equals("")) {
    %>
            <b><font color="red">Single Logout failed.</font><br><p>
    <%
            }
            if (iAmIDP) { %>  
    Followings are the tasks that can be performed on the multi-federation protocol Identity Provider:
    <%      } else { %>  
    Followings are the tasks that can be performed on the <%= spProtocol %> Service Provider:
    <%      } %>
    </td>
    </tr>
    <tr>
    <td valign="top" align="left">  </td>
    </tr>
    <tr>
    <!-- Login/Logout prompt -->
    <td valign="top" align="left">
      <ul>
        <% if(!userLoggedIn) { %>   <!-- user not logged in -->
            <% if(iAmIDP) { %>      <!-- not logged in, i am idp -->
                <li>
                <a href="<%= localLoginUrl %>?goto=<%= thisUrl %>">Login</a>
                </li>
            <% } else {  // not logged in, I am sp
                    if (spProtocol.equals(SAML2_PROTOCOL)) { %> 
                <li>
                <a href="<%= appBase %>spssoinit?metaAlias=<%= saml2SPMetaAlias %>&idpEntityID=<%= saml2IDPEntityID %>&<%= SAML2Constants.BINDING %>=HTTP-Artifact&RelayState=<%= thisUrl %>">
                    Login, provided by <%= spProtocol %> Identity Provider (<%=  idpTitle%>)</a>
                </li>
           <%       } else if (spProtocol.equals(IDFF_PROTOCOL)) { %>
                <li>
                <a href="<%= localLoginUrl %>?goto=<%= thisUrl %>">Local Login</a>
                </li>
                <li>
                <a href="<%= appBase %>preLogin?metaAlias=<%= idffSPMetaAlias %>&goto=<%= thisUrl %>">
                    Login, provided by <%= spProtocol %> Identity Provider (<%=  idpTitle%>)</a>
                </li>
           <%       } else if (spProtocol.equals(WSFED_PROTOCOL)) { %>
                <li>
                <a href="<%= appBase %>WSFederationServlet/metaAlias<%= wsfedSPMetaAlias %>?goto=<%= thisUrl %>">
                    Login provided by <%= spProtocol %> Identity Provider (<%=  idpTitle%>)</a>
                </li>
           <%       } else { // should not come here %>
               Sample not configured, please start configuration from a Service Provider.  
           <%       } 
                } %>
        <%  } else { %>             <!-- user logged in -->
           <% if (iAmIDP) { %>      <!-- logged in, i am idp -->
               <% boolean localLogout = true; %>
               <% if ((saml2SPEntityID != null) && MultiProtocolUtils.usedInProtocol(request, SingleLogoutManager.SAML2)) { %> <!-- SAML2 protocol configured -->
                <li>
               <a href="<%= appBase %>IDPSloInit?<%= SAML2Constants.BINDING %>=<%= SAML2Constants.HTTP_REDIRECT %>&RelayState=<%= thisUrl %>">
                    Logout initiated using SAMLv2 protocol. </a>
                </li>
               <%     localLogout = false;
                  } %>
               <% if ((idffSPEntityID != null) && MultiProtocolUtils.usedInProtocol(request, SingleLogoutManager.IDFF)) { %> <!-- SAML2 protocol configured -->
                <li>
               <a href="<%= appBase %>liberty-logout?metaAlias=<%= idffIDPMetaAlias %>&RelayState=<%= thisUrl %>">
                    Logout initiated using ID-FF protocol. </a>
                </li>
               <%     localLogout = false;
                  } %>
               <% if (localLogout) { %>
                <li>
                  <a href="<%= localLogoutUrl %>?goto=<%= thisUrl %>">Logout</a>
                </li>
               <% } %>
       <% if ((wsfedSPEntityID != null) && MultiProtocolUtils.usedInProtocol(request, SingleLogoutManager.WS_FED)) { %> <!-- WS-Fed protocol configured -->
	<li>
       <a href="<%= appBase %>WSFederationServlet/metaAlias<%= wsfedIDPMetaAlias %>?wa=wsignout1.0&wreply=<%= thisUrl %>">
	    Logout initiated using WS-Federation protocol. </a>
	</li>
       <%     localLogout = false;
	  } %>
           <% } else { // I am login, I am SP
                  if (spProtocol.equals(SAML2_PROTOCOL)) { %> 
                <li>
                <a href="<%= appBase %>SPSloInit?idpEntityID=<%= saml2IDPEntityID %>&<%= SAML2Constants.BINDING %>=<%= SAML2Constants.HTTP_REDIRECT %>&RelayState=<%= thisUrl %>">
                    Logout</a>
                </li>
           <%     } else if (spProtocol.equals(IDFF_PROTOCOL)) { %>
           <%         if (federatedWithPartner) { %>
                <li>
                <a href="<%= appBase %>liberty-logout?metaAlias=<%= idffSPMetaAlias %>&goto=<%= thisUrl %>">
                    Logout </a>
                </li>
           <%         } else { 
                          String relayState = URLEncoder.encode(appBase + "config/federation/default/FederationDone.jsp?metaAlias=" + idffSPMetaAlias); 
           %>
                <li>
                <a href="<%= appBase %>federation?metaAlias=<%= idffSPMetaAlias %>&selectedprovider=<%= idffIDPEntityID %>&RelayState=<%= relayState %>">
                    Federate with <%= spProtocol %> Identity Provider (<%=  idpTitle%>)</a>
                </li>
           <%         }  %>
           <%     } else if (spProtocol.equals(WSFED_PROTOCOL)) { %>
                <li>
                <a href="<%= appBase %>WSFederationServlet/metaAlias<%= wsfedSPMetaAlias %>?wa=wsignout1.0&wreply=<%= thisUrl %>">
                    Logout</a>
                </li>
           <%     } else { // should not come here %>
               Sample not configured, please start configuration from a Service Provider.  
           <%     }    
              } %>
        <%  } %>
    <% } %>
        </td>
      </tr>
    </table>

</body>
</html>
