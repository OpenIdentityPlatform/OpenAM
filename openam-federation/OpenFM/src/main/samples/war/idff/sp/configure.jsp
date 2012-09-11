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

   $Id: configure.jsp,v 1.13 2008/08/21 18:03:33 qcheng Exp $

--%>
<%--
   Portions Copyrighted 2012 ForgeRock Inc
   Portions Copyrighted 2012 Open Source Solution Technology Corporation
--%>

<html>
<head>
<title>Configure Service Provider</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<link rel="stylesheet" type="text/css" href="../../../com_sun_web_ui/css/css_ns6up.css" />

<%
    String errorMsg = null;
%>

<%@ include file="../header.jsp"%>
<%@ include file="../../cli.jsp"%>
<%@ page
    import="com.sun.identity.federation.meta.IDFFMetaException,
        com.sun.identity.federation.meta.IDFFMetaManager,
        com.sun.identity.federation.meta.IDFFMetaUtils,
        com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement,
        com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement,
        com.sun.identity.cot.CircleOfTrustDescriptor,
        com.sun.identity.cot.CircleOfTrustManager,
        com.sun.identity.cot.COTConstants,
        java.util.HashSet,
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
    String hostedSPEntityID = null;
    String remoteIDPEntityID = null;
    String SAMPLE_COT_NAME = "sampleidffcot";
    String REALM = "/";
    if ((localAuthUrl == null) && (errorMsg == null)) {
        String proto = request.getParameter("proto");
        String host = request.getParameter("host");
        String port = request.getParameter("port");
        String deploymenturi = request.getParameter("deploymenturi");
        
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

                try {
                    // [START] Make a call to CLI to get the meta data template
                    String entityName = baseURL;
                    String[] args = {"create-metadata-templ", 
                        "--spec", "idff",
                        "--entityid", entityName,
                        "--serviceprovider", "/sp"};
                    CLIRequest req = new CLIRequest(null, args, ssoToken);
                    cmdManager.addToRequestQueue(req);
                    cmdManager.serviceRequestQueue();
                    String result = outputWriter.getMessages();
                    // [END] Make a call to CLI to get the meta data template


                    // [START] Parse the output of CLI to get metadata XML
                    String endEntityDescriptorTag = "</EntityDescriptor>";
                    int metaStartIdx = result.indexOf("<EntityDescriptor");
                    int metaEndIdx = result.indexOf(endEntityDescriptorTag,
                        metaStartIdx);
                    String metaXML = result.substring(metaStartIdx, metaEndIdx +
                        endEntityDescriptorTag.length() +1);
                    // handle LB case
                    if (!realBaseURL.equals(baseURL)) {
                        metaXML = metaXML.replaceAll(realBaseURL, baseURL);
                    }
                    // [END] Parse the output of CLI to get metadata XML

                    
                    // [START] Parse the output of CLI to get extended data XML
                    String endEntityConfigTag = "</EntityConfig>";
                    int extendStartIdx = result.indexOf("<EntityConfig ");
                    int extendEndIdx = result.indexOf(endEntityConfigTag, 
                        extendStartIdx);
                    String extendedXML = result.substring(extendStartIdx,
                        extendEndIdx + endEntityConfigTag.length() + 1);
                    // handle LB case
                    if (!realBaseURL.equals(baseURL)) {
                        extendedXML = 
                            extendedXML.replaceAll(realBaseURL, baseURL);
                    }
                    // [END] Parse the output of CLI to get extended data XML
                   
                    // [START] modify extended config to set providerHomePageURL
                    int exStartIdx = extendedXML.indexOf(
                        "<Attribute name=\"providerHomePageURL\">");
                    int exValueIdx = extendedXML.indexOf("<Value>",
                        exStartIdx);
                    extendedXML = extendedXML.substring(0, exValueIdx + 7) +
                        baseURL + "/samples/idff/sp/index.jsp" +
                        extendedXML.substring(exValueIdx + 7);
                    // [END] modify extended config to set providerHomePageURL

                    // [START] Import these XMLs
                    IDFFMetaManager metaManager = new IDFFMetaManager(ssoToken);
                    EntityDescriptorElement descriptor =
                        (EntityDescriptorElement)
                            IDFFMetaUtils.convertStringToJAXB(metaXML);
                    hostedSPEntityID = entityName;
                    metaManager.createEntityDescriptor(REALM, descriptor);

                    EntityConfigElement extendConfigElm = (EntityConfigElement)
                        IDFFMetaUtils.convertStringToJAXB(extendedXML);
                    metaManager.createEntityConfig(REALM, extendConfigElm);
                    // [END] Import these XMLs
                    
                    // [START] Make a call to CLI to get IDP meta data template
                    String[] args2 = {"create-metadata-templ", 
                        "--spec", "idff",
                        "--entityid", realBaseURL,
                        "--identityprovider", "/idp"};
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
                    metaXML = result.substring(metaStartIdx, 
                        metaEndIdx + endEntityDescriptorTag.length() +1);
                    // [END] Parse the output of CLI to get metadata XML

                    // [START] Swap protocol, host, port and deployment URI
                    //         to form IDP metadata XML and import it
                    String idpMetaXML = metaXML.replaceAll(realBaseURL,
                        proto + "://" + host + ":" + port + deploymenturi);
                    EntityDescriptorElement idpDescriptor =
                        (EntityDescriptorElement)
                            IDFFMetaUtils.convertStringToJAXB(idpMetaXML);
                    remoteIDPEntityID = idpDescriptor.getProviderID();
                    metaManager.createEntityDescriptor(REALM, idpDescriptor);
                    // [END] Swap protocol, host, port and deployment URI
                    //       to form IDP metadata XML and import it

                    
                    // [START] Create Circle of Trust
                    Set providers = new HashSet();
                    providers.add(hostedSPEntityID + COTConstants.DELIMITER +
                        COTConstants.IDFF);
                    providers.add(remoteIDPEntityID + COTConstants.DELIMITER +
                        COTConstants.IDFF);
                    CircleOfTrustManager cotManager = new 
                        CircleOfTrustManager();
                    cotManager.createCircleOfTrust(REALM,
                        new CircleOfTrustDescriptor(SAMPLE_COT_NAME, REALM,
                            COTConstants.ACTIVE, "", null, null, 
                            null, null, providers));
                    // [END] Create Circle of Trust
                    
                    configured = true;
                } catch (Exception clie) {
                    errorMsg = clie.getMessage();
                    clie.printStackTrace();
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
    if (!configured) {
%>

<h3>Configuring this instance as Service Provider</h3>

<form action="configure.jsp" method="GET">
    This sample will create and load metadata for a hosted Service Provider and a remote Identity Provider, it will also setup circle of trust for the two providers.
    <p>&nbsp;</p>    
    Please provide the remote Identity Provider (must also be an Open Federation instance) information:
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
Hosted Service Provider <%= hostedSPEntityID %> is created.
<p>&nbsp;</p>
Remote Identity Provider <%= remoteIDPEntityID %> is created.
<p>&nbsp;</p>
Circle of Trust <%= SAMPLE_COT_NAME %> is created.
<p>&nbsp;</p>
<p>&nbsp;</p>
Service Provider is configured. Click <a href="../index.html">here</a> to return
to main page.
<%
}
%>

</td></tr>
</table>

</body>
</html>
