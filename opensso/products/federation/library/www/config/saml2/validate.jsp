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

   $Id: validate.jsp,v 1.6 2008/08/15 01:05:33 veiming Exp $

--%>

<%--
TOFIX: localize all the messages.
--%>

<%-- imports --%>
<%@ page import="com.sun.identity.common.SystemConfigurationUtil" %>
<%@ page import="com.sun.identity.saml2.common.AccountUtils" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Constants" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Utils" %>
<%@ page import="com.sun.identity.saml2.meta.SAML2MetaManager" %>
<%@ page import="com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement" %>
<%@ page import="com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement" %>
<%@ page import="java.util.*, java.net.URLEncoder" %>
<%@ page import="com.iplanet.sso.SSOTokenManager,
            com.iplanet.sso.SSOException,
            com.iplanet.sso.SSOToken"
%>

<html>
<head>
<title>SAMLv2 Setup Validation</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />

<%-- functions --%>
<%!
    private static final String UNIVERSAL_IDENTIFIER =
        "sun.am.UniversalIdentifier";
%>
<%

    String deployuri = SystemConfigurationUtil.getProperty(
        "com.iplanet.am.services.deploymentDescriptor");
    if ((deployuri == null) || (deployuri.length() == 0)) {
        deployuri = "../..";
    }
%>

<link rel="stylesheet" type="text/css" href="<%= deployuri %>/com_sun_web_ui/css/css_ns6up.css" />

<script language="javascript">
    var type = "idp";
    function reSubmitPage() {
	var hosted = "Identity";
	var remote = "Service";
	if (type == "sp") {
	    hosted = "Service";
	    remote = "Identity";
	}
	
	
	var f1 = validator.field1.value;
	if (f1.length == 0) {
	    alert("You need to speficy the URL for the "+hosted+" Provider.");
	    return false;
	}

	var f3 = validator.field3.value;
	if (f3.length == 0) {
	    alert("You need to speficy the realm of the "+ hosted + " Provider.");
	    return false;
	}

	var f2 = validator.field2.value;
	if (f2.length == 0) {
	    alert("You need to speficy the URL for the "+ remote+" Provider.");
	    return false;
	}

	var entity = "idp";
	if (type == "idp") {
	   entity = "sp";
	} 

	this.location.replace("./validate.jsp?"+type+"MetaAlias="+f1+"&"+entity+"Entity="+f2+"&realmName="+f3);
    }

    function spSelected() {
	type = "sp";
	var x = document.getElementById("label1");
	x.innerHTML = "Service Provider:";

	var y = document.getElementById("label2");
	y.innerHTML = "Identity Provider:";
    }

    function idpSelected() {
	type = "idp";

	var x = document.getElementById("label1");
	x.innerHTML = "Identity Provider:";

	var y = document.getElementById("label2");
	y.innerHTML = "Service Provider:";
    }
</script>

<style type="text/css">
    .error {
        color: red;
    }
</style>
</head>


<body  class="DefBdy">

<div class="MstDiv"><table width="100%" border="0" cellpadding="0" cellspacing="0" class="MstTblTop" title="">
<tbody><tr>
<td nowrap="nowrap">&nbsp;</td>
<td nowrap="nowrap">&nbsp;</td>
</tr></tbody></table>
<table width="100%" border="0" cellpadding="0" cellspacing="0" class="MstTblBot" title="">

<tbody><tr>
<td class="MstTdTtl" width="99%">
<div class="MstDivTtl"><img name="ProdName" src="../../console/images/PrimaryProductName.png" alt="" /></div></td><td class="MstTdLogo" width="1%"><img name="RMRealm.mhCommon.BrandLogo" src="../../com_sun_web_ui/images/other/javalogo.gif" alt="Java(TM) Logo" border="0" height="55" width="31" /></td></tr></tbody></table>

<table class="MstTblEnd" border="0" cellpadding="0" cellspacing="0" width="100%"><tbody><tr><td><img name="RMRealm.mhCommon.EndorserLogo" src="../../com_sun_web_ui/images/masthead/masthead-sunname.gif" alt="Sun(TM) Microsystems, Inc." align="right" border="0" height="10" width="108" /></td></tr></tbody></table></div><div class="SkpMedGry1"><a name="SkipAnchor2089" id="SkipAnchor2089"></a></div>
<div class="SkpMedGry1"><a href="#SkipAnchor4928"><img src="../../com_sun_web_ui/images/other/dot.gif" alt="Jump Over Tab Navigation Area. Current Selection is: Access Control" border="0" height="1" width="1" /></a></div>

<ul>
<h1>IDP/SP SSO Validation Test</h1>


<%
    String hostedIDP = request.getParameter("idpMetaAlias");
    String hostedSP = request.getParameter("spMetaAlias");
    String remoteIDP = request.getParameter("idpEntity");
    String spEntity = request.getParameter("spEntity");
    String realm = request.getParameter("realmName");

    boolean foundIDPEntity = true;
    boolean foundIDPMetaAlias = true;
    boolean foundSPEntity = true;
    boolean foundSPMetaAlias = true;

    if ((remoteIDP == null) || (remoteIDP.length() == 0)) {
	foundIDPEntity = false;
    }
    if ((hostedIDP == null) || (hostedIDP.length() == 0)) {
	foundIDPMetaAlias = false;
    }
    if ((spEntity == null) || (spEntity.length() == 0)) {
	foundSPEntity = false;
    }
    if ((hostedSP == null) || (hostedSP.length() == 0)) {
	foundSPMetaAlias = false;
    }

    // this displayed only if validate.jsp is accessed without any querry
    // params
    if ((!foundSPMetaAlias && !foundIDPMetaAlias) &&
	(!foundSPEntity && !foundIDPEntity))
    {
        %>
	<form name="validator">
	Validate the single sign-on capability between one Identity Provider (IDP) and
	one Service Provider (SP) which exist within the same Circle of Trust. The values
	for the IDP and SP URLs, and the realm name for the hosted provider,
	 can be obtained from the Administration Console. Open the Federation tab, and locate the IDP or SP in the Entity Providers table. The URL to enter is the value found in the name column, and the realm can be found in the realm column. 
	<h3>
	Are you accessing this validation page from the IDP or SP?
	<input type="radio" name="type" value="idp" id="idpRadio" onClick="idpSelected()" checked="checked" />IDP
	<input type="radio" name="type" value="sp" id="spRadio" onClick="spSelected()" />SP
	</h3>

	<p/>
	<table border="0">
	    <tr>
 	    	<th align="left" colspan=3>
		    <span id="label1">Identity Provider</span>
		</th>
	    </tr>
	    <tr>
		<td width="10px">&nbsp;</td>
 	    	<th align="left"> URL: </th>
		<td> <input type="textbox" name="field1" size=60> </td>
            </tr>
            <tr>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>ex: http://opensso.co.com:port/opensso</td>
            </tr>

 	    <tr>
                <td>&nbsp;</td>
 	    	<th align="left"> Realm: </th>
		<td> <input type="textbox" name="field3" size=20> </td>
	    </tr>
            <tr>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>ex: /realm1/subrealm
            </tr>

            <tr>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
            </tr>

	    <tr>
 	    	<th align="left" colspan=3>
		    <span id="label2">Service Provider</span>
		</th>
	    </tr>

            <tr>
                <td>&nbsp;</td>
                <th align="left">URL:</span></td>
                <td><input type="textbox" name="field2" size=60></td>
            </tr>
            <tr>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td>ex: http://opensso.co.com:port/opensso</td>
            </tr>
        </table>
	<p/>
	<input type="button" value="Start Validation" onClick="reSubmitPage();" />
	</form>
	<%
	return;
    }

    // At least one querry param entered

    String ssoProfile = request.getParameter("ssoProfile");
    if ((ssoProfile == null) || (ssoProfile.length() == 0)) {
        ssoProfile = SAML2Constants.HTTP_ARTIFACT;
    }
    String sloProfile = request.getParameter("sloProfile");
    if ((sloProfile == null) || (sloProfile.length() == 0)) {
        sloProfile = SAML2Constants.HTTP_REDIRECT;
    }

    boolean iAmIdp = false;
    boolean iAmSp = false;

    String myMetaAlias = null;
    String myEntityID = null;
    String myTitle = null;
    String partnerEntityID = null;
    String partnerTitle = null;

    String thisUrl = request.getRequestURL().toString();
    String relayState = thisUrl;
    String queryString = request.getQueryString();
    if (queryString != null) {
        relayState = thisUrl + "?" + queryString;
    }
    
    String appBase = thisUrl.substring(0, thisUrl.lastIndexOf("/saml2") + 1);

    SSOToken ssoToken = null;
    boolean userLoggedIn = false;
    String userName = "";
    String userLabel = "";

    SAML2MetaManager mm = SAML2Utils.getSAML2MetaManager();
    if (foundIDPMetaAlias) {
	IDPSSOConfigElement idpConfig = mm.getIDPSSOConfig(realm, hostedIDP);
	if (idpConfig == null) {
	    %>
	    <span class="error">Could not locate the Identity Provider</span><br>Make sure the values <%=realm%> and <%=hostedIDP%> are correct.<p><a href="<%=thisUrl%>>Back to validation page...</a>
    	    <%
	    return;
	}
        myMetaAlias = idpConfig.getMetaAlias();
        iAmIdp = true;
	if (!foundSPEntity) {
            %> <h3>error:</h3>
		No SAML2 Trusted Partner SP Service Registered Here
       	    <%
	    return;
        } else {
            partnerEntityID = spEntity;
        }
	myTitle = "Hosted Identity Provider: ";
	if (myMetaAlias == null) {
	    myTitle += "<span class=\"error\">"+hostedIDP+" not found</span>";
	} else {
            myTitle += hostedIDP;
	}
        partnerTitle = "Remote SP: " + partnerEntityID;
    } else if (foundSPMetaAlias) {
	SPSSOConfigElement spConfig = mm.getSPSSOConfig(realm, hostedSP);
	if (spConfig == null) {
            %><span class="error">Could not locate the Service Provider</span><br>Make sure the values <%=realm%> and <%=hostedSP%> are correct.<p><a href="<%=thisUrl%>>Back to validation page...</a><%
                return;
        }
        myMetaAlias = spConfig.getMetaAlias();
        iAmSp = true;
	if (!foundIDPEntity) {
            %> <h3>error:</h3>
		Missing Identity Provider URL
       	    <%
	    return;
        } else {
            partnerEntityID = remoteIDP;
        }
        myTitle =  "Hosted SP: " + hostedSP;
        partnerTitle = "Remote IDP: " + partnerEntityID;
    }

    if(!iAmIdp && !iAmSp) {
        %> <h3>error:</h3>
            This server is not enabled as an IDP or SP	
       	<%
        return;
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
            userLabel = userLabel.substring(0,1)
                    + ((userLabel.length() > 0)
                    ? userLabel.substring(1, userLabel.length())
                    : "");
        }
    } catch (SSOException e) {
        //response.sendError(response.SC_INTERNAL_SERVER_ERROR);
    }


%>


<p>&nbsp;</p>                                                                                
    <h3><%= myTitle %></h3>
    <h3><%= partnerTitle %></h3>
    <p>
    <h4>User <%= userLoggedIn ? userLabel + " is logged in." : "is logged out."%></h4>
    <hr/>
    <table cellpadding="2" cellspacing="2" border="0" width="100%">
    <tr>
    <td valign="top" align="left">
    The following tests can be performed on the 
    <% if(iAmIdp) { %>  
	Identity Provider:
    <% } else { %>  
    	Service Provider:
    <% } %>
    </td>
    </tr>
    <tr>
    <td valign="top" align="left">  </td>
    </tr>
    <tr>
    <!-- Login/Logout prompt -->
    <td valign="top" align="left">
      <ul>
        <li>
        <% if(!userLoggedIn) { %>   <!-- user not logged in -->
                    <% if(iAmIdp) { %>      <!-- not logged in, i am idp -->
                            <a href="<%= appBase %>idpssoinit?metaAlias=<%= myMetaAlias %>&spEntityID=<%= partnerEntityID %>&<%= SAML2Constants.BINDING %>=<%= ssoProfile %>&RelayState=<%= URLEncoder.encode(relayState) %>">
                                Single Sign-On to <%= partnerTitle%></a>
                    <% } else { %>          <!-- not logged in, i am sp -->
                            <a href="<%= appBase %>spssoinit?metaAlias=<%= myMetaAlias %>&idpEntityID=<%= partnerEntityID %>&<%= SAML2Constants.BINDING %>=<%= ssoProfile %>&RelayState=<%= URLEncoder.encode(relayState) %>">
                            Single Sign-On through <%=  partnerTitle%></a>
                    <% } %>
       <%  } else { %>             <!-- user logged in -->
            
                    <% if(iAmIdp) { %>      <!-- logged in, i am idp -->
                            <a href="<%= appBase %>IDPSloInit?<%= SAML2Constants.BINDING %>=<%= sloProfile %>&RelayState=<%= URLEncoder.encode(relayState) %>">
                               Single Logout</a>
                    <% } else { %>          <!-- logged in, i am sp -->
                            <a href="<%= appBase %>SPSloInit?idpEntityID=<%= partnerEntityID %>&<%= SAML2Constants.BINDING %>=<%= sloProfile %>&RelayState=<%= URLEncoder.encode(relayState) %>">
                               Single Logout</a>
                    <% } %>
                <%  } %>
            </li>


        </td>
      </tr>
    </table>

</ul>
</body>
</html>
