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

   $Id: Configurator.jsp,v 1.10 2008/11/11 07:05:08 veiming Exp $

--%>
<%--
   Portions Copyrighted 2012 ForgeRock Inc
   Portions Copyrighted 2012 Open Source Solution Technology Corporation
--%>

<html>
<head>
<title>Configure OpenAM Administration Console WAR</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<link rel="stylesheet" type="text/css" href="com_sun_web_ui/css/css_ns6up.css" />


<%@ page import="
com.iplanet.am.util.SystemProperties,
com.iplanet.services.util.ConfigurableKey,
com.iplanet.services.util.Crypt,
com.sun.identity.security.EncodeAction,
com.sun.identity.setup.SetupClientWARSamples,
java.io.*,
java.security.AccessController,
java.util.Properties,
org.owasp.esapi.ESAPI"
%>

<%
    String configFile = System.getProperty("user.home") +
        File.separator + "AMConfig.properties";
    String configTemplate = "/WEB-INF/classes/AMConfig.properties.template";
    String errorMsg = null;
    boolean configured = false;
    String famProt = null; 
    String famHost = null;
    String famPort = null;
    String famDeploymenturi = null;
    String consoleProt = null; 
    String consoleHost = null;
    String consolePort = null;
    String consoleDeploymenturi = null;
    String encPwd = ""; 
    String debugDir = null;
    String appUser = null;
    String appPassword = null;

    File configF = new File(configFile);
    if (configF.exists()) {
        errorMsg = "The Administration Console WAR has already been configued."
            + "<br>Configuration file : " + configFile + "<br><p><br>" 
            + "Click <a href=\"index.html\">here</a> to go to the console.";
        // reinitialize properties
        Properties props = new Properties();
        props.load(new FileInputStream(configFile));
        SystemProperties.initializeProperties(props);
        configured = true;
    } else {
        famProt = request.getParameter("famProt");
        famHost = request.getParameter("famHost");
        famPort = request.getParameter("famPort");
        famDeploymenturi = request.getParameter("famDeploymenturi");
        consoleProt = request.getParameter("consoleProt");
        consoleHost = request.getParameter("consoleHost");
        consolePort = request.getParameter("consolePort");
        consoleDeploymenturi = request.getParameter("consoleDeploymenturi");
        encPwd = request.getParameter("encPwd");
        if (encPwd == null) {
            encPwd = "";
        }
        debugDir = request.getParameter("debugDir");
        appUser = request.getParameter("appUser");
        appPassword = request.getParameter("appPassword");
        String submit = request.getParameter("submit");
        String servletPath = request.getServletPath();

        if (submit != null) { 
            if ((famProt != null) && !famProt.equals("") && 
                (famHost != null) && !famHost.equals("") && 
                (famPort != null) && !famPort.equals("") && 
                (famDeploymenturi != null) && !famDeploymenturi.equals("") && 
                (consoleProt != null) && !consoleProt.equals("") && 
                (consoleHost != null) && !consoleHost.equals("") && 
                (consolePort != null) && !consolePort.equals("") && 
                (consoleDeploymenturi != null) && 
                !consoleDeploymenturi.equals("") && 
                (debugDir != null) && !debugDir.equals("") &&
                (appUser != null) && !appUser.equals("") &&
                (appPassword != null) && !appPassword.equals("")) {
                if (encPwd.length() != 0) {
                    SystemProperties.initializeProperties("am.encryption.pwd", 
                        encPwd);
                    ((ConfigurableKey) Crypt.getEncryptor()).setPassword(encPwd);
                }
                Properties props = new Properties();
                props.setProperty("SERVER_PROTOCOL", famProt);
                props.setProperty("SERVER_HOST", famHost);
                props.setProperty("SERVER_PORT", famPort);
                props.setProperty("DEPLOY_URI", famDeploymenturi);
                props.setProperty("CONSOLE_PROTOCOL", consoleProt);
                props.setProperty("CONSOLE_HOST", consoleHost);
                props.setProperty("CONSOLE_PORT", consolePort);
                props.setProperty("CONSOLE_DEPLOY_URI", consoleDeploymenturi);
                props.setProperty("CONSOLE_REMOTE", "true");
                props.setProperty("DEBUG_DIR", debugDir);
                props.setProperty("NAMING_URL", famProt + "://" + famHost + ":" 
                    + famPort + famDeploymenturi + "/namingservice");
                props.setProperty("NOTIFICATION_URL", consoleProt + "://" 
                    + consoleHost + ":" + consolePort + consoleDeploymenturi 
                    + "/notificationservice");
                props.setProperty("DEBUG_LEVEL", "error");
                props.setProperty("APPLICATION_USER", appUser);
                props.setProperty("ENCODED_APPLICATION_PASSWORD", (String) 
                  AccessController.doPrivileged(new EncodeAction(appPassword)));
                // set empty application password
                props.setProperty("APPLICATION_PASSWD", ""); 
                props.setProperty("AM_COOKIE_NAME", "iPlanetDirectoryPro");
                props.setProperty("ENCRYPTION_KEY", encPwd);
                props.setProperty("ENCRYPTION_KEY_LOCAL", "");
                props.setProperty("SESSION_PROVIDER_CLASS", 
                    "com.sun.identity.plugin.session.impl.FMSessionProvider");
                props.setProperty("CONFIGURATION_PROVIDER_CLASS", 
                    "com.sun.identity.plugin.configuration.impl.ConfigurationInstanceImpl");
                props.setProperty("DATASTORE_PROVIDER_CLASS", 
                    "com.sun.identity.plugin.datastore.impl.IdRepoDataStoreProvider");
                props.setProperty("CONFIG_DIR",
                    System.getProperty("user.home"));

                try {
                    SetupClientWARSamples configurator = 
                        new SetupClientWARSamples(
                        getServletConfig().getServletContext());
                    configurator.createAMConfigProperties(configFile, 
                        configTemplate, props);
                    configurator.setAMConfigProperties(configFile);
                } catch (IOException ioex) {
                    ioex.printStackTrace();
                    errorMsg = "Unable to create sample AMConfig.properties " +
                       "file: " + ioex.getMessage();
                }
                configured = true;
            } else {
                errorMsg = "Missing one or more required fields."; 
            }
        } else {
            // get local protocol/host/port as default for this console
            if (consoleProt == null) {
                consoleProt = request.getScheme();
            } 
            if (consoleHost == null) {
                consoleHost = request.getServerName();
            }
            if (consolePort == null) {
                consolePort = "" + request.getServerPort();
            }
            if (consoleDeploymenturi == null) {
                String tmp = request.getRequestURI();
                int secondSlash = tmp.indexOf("/", 1);
                if (secondSlash == -1) {
                    consoleDeploymenturi = tmp;
                } else {
                    consoleDeploymenturi = tmp.substring(0, secondSlash);
                }
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

<h3>Configuring OpenAM Administration Console WAR</h3>

<form action="Configurator.jsp" method="POST" 
    name="consoleconfigurator">
    Please provide the OpenAM Server Information. This is the server instance this remote administration console will be managing.
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
    <td>Server Protocol:</td>
    <td><input name="famProt" type="text" size="6" value="<%= famProt == null ? "" : ESAPI.encoder().encodeForHTML(famProt) %>" /></td>
    </tr>
    <tr>
    <td>Server Host:</td>
    <td><input name="famHost" type="text" size="30" value="<%= famHost == null ? "" : ESAPI.encoder().encodeForHTML(famHost) %>" /></td>
    </tr>
    <tr>
    <td>Server Port:</td>
    <td><input name="famPort" type="text" size="6" value="<%= famPort == null ? "" : ESAPI.encoder().encodeForHTML(famPort) %>" /></td>
    </tr>
    <tr>
    <td>Server Deployment URI:</td>
    <td><input name="famDeploymenturi" type="text" size="15" value="<%= famDeploymenturi == null ? "" : ESAPI.encoder().encodeForHTML(famDeploymenturi) %>" /></td>
    </tr>
    <tr>
    <td>Password Encryption Key:</td>
    <td><input name="encPwd" type="password" size="20" value="" /><br>The key must be the same as the Password Encryption Key set in the server instance. The Password Encryption Key of the server instance could be retrieved using ssoadm.jsp export-server option, enter the server instance and get the value of am.encryption.pwd attribute in the exported server configuration XML file.</td>
    </tr>
    <tr>
    <td>Application user name</td>
    <td><input name="appUser" type="text" size="15" value="<%= appUser == null ? "" : ESAPI.encoder().encodeForHTML(appUser) %>" /></td>
    </tr>
    <tr>
    <td>Application user password</td>
    <td><input name="appPassword" type="password" size="15" value="" /></td>
    </tr>
    <tr>
        <td>  </td>
    </tr>
    <tr>
    <td>Administration Console Protocol:</td>
    <td><input name="consoleProt" type="text" size="6" value="<%= consoleProt == null ? "" : ESAPI.encoder().encodeForHTML(consoleProt) %>" /></td>
    </tr>
    <tr>
    <td>Administration Console Host:</td>
    <td><input name="consoleHost" type="text" size="30" value="<%= consoleHost == null ? "" : ESAPI.encoder().encodeForHTML(consoleHost) %>" /></td>
    </tr>
    <tr>
    <td>Administration Console Port:</td>
    <td><input name="consolePort" type="text" size="6" value="<%= consolePort == null ? "" : ESAPI.encoder().encodeForHTML(consolePort) %>" /></td>
    </tr>
    <tr>
    <td>Administration Console Deployment URI:</td>
    <td><input name="consoleDeploymenturi" type="text" size="15" value="<%= consoleDeploymenturi == null ? "" : ESAPI.encoder().encodeForHTML(consoleDeploymenturi) %>" /></td>
    </tr>
    <tr>
    <td>Administration Console Debug directory</td>
    <td><input name="debugDir" type="text" size="15" value="<%= debugDir == null ? "" : ESAPI.encoder().encodeForHTML(debugDir) %>" /></td>
    </tr>
    <tr>
        <td>  </td>
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
The Administration Console only WAR had been successfully configured.<br>
AMConfig.properties created at <%= configFile %><br>
<br>
<p>
Click <a href="index.html">here</a> to go to the administration console. 
<%
    }
}
%>
</td></tr></table>
</body>
</html>
