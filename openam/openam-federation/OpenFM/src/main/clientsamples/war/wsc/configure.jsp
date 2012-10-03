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

   $Id: configure.jsp,v 1.11 2009/02/05 00:46:38 mrudulahg Exp $

--%>
<%--
   Portions Copyrighted 2012 ForgeRock Inc
   Portions Copyrighted 2012 Open Source Solution Technology Corporation
--%>

<html>
<head>
<title>Configure Identity Provider</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<link rel="stylesheet" type="text/css" href="../com_sun_web_ui/css/css_ns6up.css" />


<%@ page import="
com.sun.identity.setup.SetupClientWARSamples,
java.io.*,
java.util.Properties"
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
<div class="MstDivTtl"><img name="ProdName" src="../console/images/PrimaryProductName.png" alt="" /></div></td><td class="MstTdLogo" width="1%"><img name="RMRealm.mhCommon.BrandLogo" src="../com_sun_web_ui/images/other/javalogo.gif" alt="Java(TM) Logo" border="0" height="55" width="31" /></td></tr></tbody></table>
<table class="MstTblEnd" border="0" cellpadding="0" cellspacing="0" width="100%"><tbody><tr><td><img name="RMRealm.mhCommon.EndorserLogo" src="../com_sun_web_ui/images/masthead/masthead-sunname.gif" alt="Sun(TM) Microsystems, Inc." align="right" border="0" height="10" width="108" /></td></tr></tbody></table></div><div class="SkpMedGry1"><a name="SkipAnchor2089" id="SkipAnchor2089"></a></div>
<div class="SkpMedGry1"><a href="#SkipAnchor4928"><img src="../com_sun_web_ui/images/other/dot.gif" alt="Jump Over Tab Navigation Area. Current Selection is: Access Control" border="0" height="1" width="1" /></a></div>
                                                                                
<table border="0" cellpadding="10" cellspacing="0" width="100%">
<tr><td>


<%

    String configDir = System.getProperty("user.home") + File.separator +
        SetupClientWARSamples.CLIENT_WAR_CONFIG_TOP_DIR;

    File fedConfig = new File(configDir + File.separator +
        SetupClientWARSamples.getNormalizedRealPath(
        getServletConfig().getServletContext()) +
        "AMConfig.properties");
    if (!fedConfig.exists()) {
%>
<p>&nbsp;</p>
Client SDK is not configured. Please click <a class="named" href="../Configurator.jsp">here</a> to configure it first.
<%
    } else {

        String bootstrapFile = configDir + File.separator +
            SetupClientWARSamples.getNormalizedRealPath(
            getServletConfig().getServletContext())
            + "ClientSampleWSC.properties";
        File bootstrapConfig = new File(bootstrapFile);
        boolean configured = false;
        if (bootstrapConfig.exists()) {
            configured = true;
        }  

        String errorMsg = null;
        String idpProt = request.getParameter("idpProt");
        String idpHost = request.getParameter("idpHost");
        String idpPort = request.getParameter("idpPort");
        String idpDeploymenturi = request.getParameter("idpDeploymenturi");
        String spProviderIDinput = request.getParameter("spProviderIDinput");
        String submit = request.getParameter("submit");
        if (submit != null) {

            Properties fcprops = new Properties();
            fcprops.load(new FileInputStream(fedConfig));
            String spProt = fcprops.getProperty(
                "com.iplanet.am.server.protocol");
            String spHost = fcprops.getProperty("com.iplanet.am.server.host");
            String spPort = fcprops.getProperty("com.iplanet.am.server.port");
            String spDeployUri = fcprops.getProperty(
                "com.iplanet.am.services.deploymentDescriptor");
            String spProviderID;
            if (spProviderIDinput != null) {
                spProviderID = spProviderIDinput;
            } else {
                spProviderID = spProt + "://" + spHost + ":" + spPort +
                    spDeployUri;
            }
            Properties props = new Properties();
            props.setProperty("spProviderID", spProviderID);
            props.setProperty("idpProt", idpProt);
            props.setProperty("idpHost", idpHost);
            props.setProperty("idpPort", idpPort);
            props.setProperty("idpDeploymenturi", idpDeploymenturi);
            FileOutputStream fo = null;
            try {
                fo = new FileOutputStream(bootstrapFile);
                props.store(fo, null);
                configured = true;
            } catch (IOException ioex) {
                errorMsg = "Unable to create WSC sample property " +
                    "file. " + ioex.getMessage();
            } finally {
                if (fo != null) {
                    fo.close();
                }
            }
        }

        if (!configured) {
%>

<h3>Configuring WSC Sample</h3>

<form action="configure.jsp" name="configure" method="GET">
    Please provide the Identity Provider Information.
    <p>&nbsp;</p>    

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
    <td>SP Provider ID:</td>
    <td><input name="spProviderIDinput" type="text" size="6" value="<%= spProviderIDinput == null ? "" : spProviderIDinput %>" /></td>
    </tr>
    <tr>
    <td>IDP Protocol:</td>
    <td><input name="idpProt" type="text" size="6" value="<%= idpProt == null ? "" : idpProt %>" /></td>
    </tr>
    <tr>
    <td>IDP Host:</td>
    <td><input name="idpHost" type="text" size="30" value="<%= idpHost == null ? "" : idpHost %>" /></td>
    </tr>
    <tr>
    <td>IDP Port:</td>
    <td><input name="idpPort" type="text" size="6" value="<%= idpPort == null ? "" : idpPort %>" /></td>
    </tr>
    <tr>
    <td>IDP Deployment URI:</td>
    <td><input name="idpDeploymenturi" type="text" size="15" value="<%= idpDeploymenturi == null ? "" : idpDeploymenturi %>" /></td>
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
WSC Sample is configured. Click <a href="index.html">here</a> to return
<%
        }
    }
%>
</td></tr></table>
</body>
</html>
