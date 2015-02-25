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

   $Id: getServerInfo.jsp,v 1.6 2008/09/04 00:34:01 rajeevangal Exp $

   Portions Copyrighted 2010-2014 ForgeRock AS
--%>

<%@ page
import="com.iplanet.am.util.SystemProperties,
        com.iplanet.services.naming.WebtopNaming,
        com.iplanet.sso.SSOException,
        com.iplanet.sso.SSOToken,
        com.iplanet.sso.SSOTokenManager,
        com.sun.identity.authentication.AuthContext,
        com.sun.identity.setup.AMSetupServlet,
        com.sun.identity.setup.BootstrapData,
        com.sun.identity.setup.EmbeddedOpenDS,
        com.sun.identity.setup.JCECrypt,
        com.sun.identity.setup.SetupConstants,
        java.io.File,
        java.net.URLDecoder,
        java.net.URLEncoder,
        java.util.ArrayList,
        java.util.Map,
        javax.security.auth.callback.Callback,
        javax.security.auth.callback.NameCallback,
        javax.security.auth.callback.PasswordCallback"
%>
<%
   if ("POST".equals(request.getMethod()) != true) {
       response.sendError(405);
       return;
   }
   String username = request.getParameter("IDToken1");
   String password = request.getParameter("IDToken2");

   if ( username == null && password == null) {
       response.sendError(400);
       return;
   }

   if ("amadmin".equals(username) == false) {
       response.sendError(401);
       return;
   }
   
   AuthContext lc = new AuthContext("/");
   lc.login(AuthContext.IndexType.SERVICE, "adminconsoleservice");
    while (lc.hasMoreRequirements()) {
        Callback[] callbacks = lc.getRequirements();
        ArrayList missing = new ArrayList();
        // loop through the requires setting the needs..
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                NameCallback nc = (NameCallback) callbacks[i];
                nc.setName(username);
            } else if (callbacks[i] instanceof PasswordCallback) {
                PasswordCallback pc = (PasswordCallback) callbacks[i];
                pc.setPassword(password.toCharArray());
            } else {
                missing.add(callbacks[i]);
            }
        }
        // there's missing requirements not filled by this
        if (missing.size() > 0) {
            // need add the missing later..
            response.sendError(401);
            return;
        }
        lc.submitRequirements(callbacks);
    }
    // validate the password..
    if (lc.getStatus() != AuthContext.Status.SUCCESS) {
        response.sendError(401);
        return;
    } else {
        try {
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            mgr.destroyToken(lc.getSSOToken());
        } catch (SSOException e) {
            // ignore
        }
    }

    String baseDir = SystemProperties.get(SystemProperties.CONFIG_PATH);
    String encKey = SystemProperties.get("am.encryption.pwd");
    String defAgentPwd = SystemProperties.get("com.iplanet.am.service.secret");
    BootstrapData bootstrapData = new BootstrapData(baseDir);
    boolean isEmbeddedDS = (new File(baseDir + "/opends")).exists();
    // Assumption : opends entry is the 1st
    Map bMap = bootstrapData.getDataAsMap(0);
    String dsbasedn = (String) bMap.get(BootstrapData.DS_BASE_DN);
    String dsport = (String) bMap.get(BootstrapData.DS_PORT);
    String dsprotocol = (String) bMap.get(BootstrapData.DS_PROTOCOL);
    String dsrelport = (String) bMap.get(BootstrapData.DS_REPLICATIONPORT);
    String dshost = (String) bMap.get(BootstrapData.DS_HOST);
    String dsmgr = (String) bMap.get(BootstrapData.DS_MGR);
    String dspwd = (String) bMap.get(BootstrapData.DS_PWD);
    String serverid = WebtopNaming.getLocalServer();

    // if embedded get replication port status. Two cases :
    //   i) No replication port -> generate a new one
    //   ii) replication port available -> retrieve it
    String replPort = null;
    String replPortAvailable = null;
    String adminPort = null;

    if (isEmbeddedDS) {
        replPort = EmbeddedOpenDS.getReplicationPort(dsmgr, JCECrypt.decode(dspwd),
            "localhost", dsport);
        replPortAvailable = "true";
        if (replPort == null) {
            replPortAvailable = "false";
            replPort = ""+ AMSetupServlet.getUnusedPort("localhost", 50889, 1000);
        }

        adminPort = EmbeddedOpenDS.getAdminPort(dsmgr, JCECrypt.decode(dspwd),
                "localhost", dsport);

        if (adminPort == null) {
            adminPort = "4444";
        }
    }
    // We have collected all the data - return a response
    StringBuffer buf = new StringBuffer();
    
    buf.append(BootstrapData.DS_ISEMBEDDED).append("=").append(isEmbeddedDS).
                                        append("&");
    if (dsprotocol != null) {
        buf.append(BootstrapData.DS_PROTOCOL).append("=").
            append(URLEncoder.encode(dsprotocol, "UTF-8")).append("&");
    }
    if (dshost != null) {
        buf.append(BootstrapData.DS_HOST).append("=").
            append(URLEncoder.encode(dshost, "UTF-8")).append("&");
    }
    if (dsport != null) {
        buf.append(BootstrapData.DS_PORT).append("=").append(dsport).
            append("&");
    }
    if (dsbasedn != null) {
        buf.append(BootstrapData.DS_BASE_DN).append("=").
            append(URLEncoder.encode(dsbasedn, "UTF-8")).append("&");
    }
    if (replPort != null) {
        buf.append(BootstrapData.DS_REPLICATIONPORT).append("=").
            append(replPort).append("&");
    }
    if (adminPort != null) {
        buf.append(SetupConstants.DS_EMB_REPL_ADMINPORT2).append("=").
            append(adminPort).append("&");
    }
    if (replPortAvailable != null) {
        buf.append(BootstrapData.DS_REPLICATIONPORT_AVAILABLE).append("=").
            append(replPortAvailable).append("&");
    }
    if (dsmgr != null) {
        buf.append(BootstrapData.DS_MGR).append("=").
            append(URLEncoder.encode(dsmgr, "UTF-8")).append("&");
    }
    if (dspwd != null) {
        buf.append(BootstrapData.DS_PWD).append("=").
            append(URLEncoder.encode(dspwd, "UTF-8")).append("&");
    }
    if (encKey != null) {
        buf.append(BootstrapData.ENCKEY).append("=").
            append(URLEncoder.encode(encKey, "UTF-8"));
    }
    if (defAgentPwd != null) {
        buf.append("&ENCLDAPUSERPASSWD=").append(
            URLEncoder.encode(defAgentPwd, "UTF-8"));
    }
    if (serverid != null) {
        buf.append("&existingserverid=").append(
            URLEncoder.encode(serverid, "UTF-8"));
    }

    out.println(buf.toString());
%>
