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

   $Id: configure.jsp,v 1.8 2009/07/22 00:39:12 sean_brydon Exp $

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

<%@ include file="../header.jsp"%>
<%@ include file="../../cli.jsp"%>
<%@ page
    import="com.sun.identity.saml2.meta.SAML2MetaException,
        com.sun.identity.saml2.meta.SAML2MetaManager,
        com.sun.identity.saml2.meta.SAML2MetaUtils,
        com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement,
        com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement,
        com.sun.identity.cot.CircleOfTrustDescriptor,
        com.sun.identity.cot.CircleOfTrustManager,
        com.sun.identity.cot.COTConstants,
        java.util.HashSet,
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
    String SAMPLE_COT_NAME = "samplesaml2cot";
    if ((localAuthUrl == null) && (errorMsg == null)) {
        String proto = request.getParameter("proto");
        String host = request.getParameter("host");
        String port = request.getParameter("port");
        String deploymenturi = request.getParameter("deploymenturi");
        String remoteEntityID = null;

        if ((proto != null) && (host != null) && (port != null) &&
            (deploymenturi != null)
        ) {
            proto = proto.trim();
            host = host.trim();
            port = port.trim();
            deploymenturi = deploymenturi.trim();
            
            if ((proto.length() > 0) && (host.length() > 0) && 
                (port.length() > 0) && (deploymenturi.length() > 0)
            ) {
                if (deploymenturi.charAt(0) != '/') {
                    deploymenturi = "/" + deploymenturi;
                }
                remoteEntityID = new StringBuffer()
                                     .append(proto).append("://").append(host)
                                     .append(":").append(port)
                                     .append(deploymenturi).toString();

                try {

                    // [START] Make a call to CLI to get the meta data template
                    String entityName = baseURL; 
                    SAML2MetaManager metaManager = new SAML2MetaManager();
                    List idpEntityList = 
                        metaManager.getAllHostedIdentityProviderEntities(
                                    defaultOrg);
                    boolean idpExists =  ((idpEntityList != null && 
                                           !idpEntityList.isEmpty()) && 
                                           idpEntityList.contains(entityName)) ;
                    CLIRequest req = null;
                    int metaStartIdx = 0;
                    int metaEndIdx = 0;
                    String metaXML = null;
                    String endEntityDescriptorTag=null;
                    String result = null;
                    int extendStartIdx = 0;
                    int extendEndIdx = 0;
                    hostedIDPEntityID = entityName;
                    if (!idpExists) {
                    String[] args = {"create-metadata-templ", 
                        "--entityid", entityName,
                        "--identityprovider", "/idp"};
                    req = new CLIRequest(null, args, ssoToken);
                    cmdManager.addToRequestQueue(req);
                    cmdManager.serviceRequestQueue();
                    result = outputWriter.getMessages();
                    // [END] Make a call to CLI to get the meta data template


                    // [START] Parse the output of CLI to get metadata XML
                    endEntityDescriptorTag = "</EntityDescriptor>";
                    metaStartIdx = result.indexOf("<EntityDescriptor");
                    metaEndIdx = result.indexOf(endEntityDescriptorTag,
                        metaStartIdx);
                    metaXML = result.substring(metaStartIdx, metaEndIdx +
                        endEntityDescriptorTag.length() +1);
                    // handle LB case
                    if (!realBaseURL.equals(baseURL)) {
                        metaXML = metaXML.replaceAll(realBaseURL, baseURL);
                    }
                    // [END] Parse the output of CLI to get metadata XML


                    // [START] Parse the output of CLI to get extended data XML
                    String endEntityConfigTag = "</EntityConfig>";
                    extendStartIdx = result.indexOf("<EntityConfig ");
                    extendEndIdx = result.indexOf(endEntityConfigTag, 
                        extendStartIdx);
                    String extendedXML = result.substring(extendStartIdx,
                        extendEndIdx + endEntityConfigTag.length() + 1);
                    // handle LB case
                    if (!realBaseURL.equals(baseURL)) {
                        extendedXML = 
                            extendedXML.replaceAll(realBaseURL, baseURL);
                    }
                    // [END] Parse the output of CLI to get extended data XML


                    // [START] Import these XMLs
                    EntityDescriptorElement descriptor =
                        (EntityDescriptorElement)
                            SAML2MetaUtils.convertStringToJAXB(metaXML);
                    hostedIDPEntityID = entityName;
                    metaManager.createEntityDescriptor(defaultOrg,descriptor);

                    EntityConfigElement extendConfigElm = (EntityConfigElement)
                        SAML2MetaUtils.convertStringToJAXB(extendedXML);
                    metaManager.createEntityConfig(defaultOrg,extendConfigElm);
                    // [END] Import these XMLs
                    }

                    // [START] Make a call to CLI to get SP meta data template
                    List spEntityList = 
                        metaManager.getAllRemoteServiceProviderEntities(
                        defaultOrg);
                    boolean spExists = 
                        ((spEntityList != null && !spEntityList.isEmpty()) 
                         && spEntityList.contains(remoteEntityID));
                    remoteSPEntityID = remoteEntityID;
                    if (!spExists) {
                    String[] args2 = {"create-metadata-templ", 
                        "--entityid", remoteEntityID,
                        "--serviceprovider", "/sp"};
                    outputWriter = new StringOutputWriter();
                    env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, 
                        outputWriter);
                    cmdManager = new CommandManager(env);
                    req = new CLIRequest(null, args2, ssoToken);
                    cmdManager.addToRequestQueue(req);
                    cmdManager.serviceRequestQueue();
                    result = outputWriter.getMessages();
                    // [END] Make a call to CLI to get the meta data template

                    // [START] Parse the output of CLI to get metadata XML
                    metaStartIdx = result.indexOf("<EntityDescriptor");
                    metaEndIdx = result.indexOf(endEntityDescriptorTag,
                        metaStartIdx);
                    metaXML = result.substring(metaStartIdx, metaEndIdx +
                        endEntityDescriptorTag.length() +1);
                    // [END] Parse the output of CLI to get metadata XML

                    // [START] Swap protocol, host, port and deployment URI
                    //         to form IDP metadata XML and import it
                    metaXML = metaXML.replaceAll(remoteSPEntityID,
                        "@remoteSPEntityID@");
                    String spMetaXML = metaXML.replaceAll(realBaseURL,
                        proto + "://" + host + ":" + port + deploymenturi);
                    spMetaXML = spMetaXML.replaceAll("@remoteSPEntityID@",
                        remoteSPEntityID);
                    EntityDescriptorElement spDescriptor =
                        (EntityDescriptorElement)
                            SAML2MetaUtils.convertStringToJAXB(spMetaXML);
                    remoteSPEntityID = spDescriptor.getEntityID();
                    metaManager.createEntityDescriptor(defaultOrg,spDescriptor);
                    // [END] Swap protocol, host, port and deployment URI
                    //       to form IDP metadata XML and import it
                  }

                  // [START] Create Circle of Trust
                  createCircleOfTrust(SAMPLE_COT_NAME,hostedIDPEntityID,
                                      remoteSPEntityID);
                  configured = true;
                } catch (Exception clie) {
                    errorMsg = clie.getMessage();
                }
            } else {
                errorMsg = "Required fields are missing.";
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
    You have not logged in. Click <a href=<%= baseURL+"/UI/Login?goto="+baseURL+"/"+baseURI %>>here</a> to login.

<%
    } else {
        if (!configured) {
%>

<h3>Configuring this instance as Identity Provider</h3>

<form action="configure.jsp" method="GET">
    This sample will create and load metadata for a hosted Identity Provider and a remote Service Provider, it will also setup circle of trust for the two providers.
    <p>&nbsp;</p>    
    Please provide the remote Service Provider (must also be an OpenAM instance) information: <p>

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
%>


    <tr>
    <td>Protocol:</td>
    <td><input name="proto" type="text" size="6" value="http" /></td>
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
    </table>
</form>

<%
} else {
%>
<p>&nbsp;</p>
Hosted Identity Provider <%= hostedIDPEntityID %> is created.
<p>&nbsp;</p>
Remote Service Provider <%= remoteSPEntityID %> is created.
<p>&nbsp;</p>
Circle of Trust <%= SAMPLE_COT_NAME %> is created.
<p>&nbsp;</p>
Identity Provider is configured. Click <a href="../index.html">here</a> to return
<%
}
}
%>
</td></tr></table>
</body>
</html>
