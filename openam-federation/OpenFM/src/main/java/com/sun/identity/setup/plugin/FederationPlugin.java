/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: FederationPlugin.java,v 1.8 2008/10/30 18:24:04 mallas Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */
                                                                                
package com.sun.identity.setup.plugin;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.saml.common.SAMLSiteID;
import com.sun.identity.setup.ConfiguratorPlugin;
import com.sun.identity.setup.ServicesDefaultValues;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;

/**
 * Does open federation post configuration task.
 */
public class FederationPlugin implements ConfiguratorPlugin {

    private static final String ATTR_KEY_SAML_SITEID = "SAML_SITEID";

    /**
     * Re-initialize configuration file.
     *
     * @param baseDir Base directory of the configuration data store.
     */
    public void reinitConfiguratioFile(String baseDir) {
        // do nothing
    }

    /**
     * Copies <code>is-html.xsl</code> and <code>is-wml.xsl</code> to base
     * directory <code>/xsl</code> directory.
     *
     * @param servletCtx Servlet Context.
     * @param adminSSOToken Super Administrator Single Sign On Token.
     */
    public void doPostConfiguration(
        ServletContext servletCtx,
        SSOToken adminSSOToken
    ) {
        setXSLFiles(servletCtx);
        setAuthModules(adminSSOToken);
        setSAMLSiteID(adminSSOToken);
    }

    private void setSAMLSiteID(SSOToken adminSSOToken) {
        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                "iPlanetAMSAMLService", adminSSOToken);
            ServiceSchema ss = mgr.getSchema(SchemaType.GLOBAL);
            Map values = ss.getAttributeDefaults();
            Set siteIDs = 
                (Set) values.get("iplanet-am-saml-siteid-issuername-list");
            if (siteIDs != null && !siteIDs.isEmpty()) {
                String siteID = (String) siteIDs.iterator().next();
                int idPos = siteID.indexOf("=" + ATTR_KEY_SAML_SITEID + "|");
                if (idPos != -1) {
                    Map defaults = ServicesDefaultValues.getDefaultValues();
                    String protocol = (String)defaults.get(
                        SetupConstants.CONFIG_VAR_SERVER_PROTO);
                    String hostname = (String)defaults.get(
                        SetupConstants.CONFIG_VAR_SERVER_HOST);
                    String port = (String)defaults.get(
                        SetupConstants.CONFIG_VAR_SERVER_PORT);
                    String encoded = SAMLSiteID.generateSourceID(
                        protocol + "://" + hostname + ":" + port); 
                    siteIDs.remove(siteID);
                    siteID = siteID.substring(0, idPos + 1) + encoded
                        + siteID.substring(idPos +
                            ATTR_KEY_SAML_SITEID.length() +1);
                    siteIDs.add(siteID);
                    ss.setAttributeDefaults(values);
                }
            }
        } catch (SSOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (SMSException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void setAuthModules(SSOToken adminSSOToken) {
        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                "iPlanetAMAuthService", adminSSOToken);
            ServiceSchema ss = mgr.getSchema(SchemaType.GLOBAL);
            Map values = ss.getAttributeDefaults();
            Set modules = (Set)values.get("iplanet-am-auth-authenticators");
            modules.add(
               "com.sun.identity.authentication.modules.federation.Federation");
            modules.add(
               "com.sun.identity.authentication.modules.sae.SAE");

            ss.setAttributeDefaults(values);
        } catch (SSOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (SMSException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void setXSLFiles(ServletContext servletCtx) {
        try {
            Map values = ServicesDefaultValues.getDefaultValues();
            String baseDir = (String)values.get(
                SetupConstants.CONFIG_VAR_BASE_DIR);
            String deployURI = (String)values.get(
                SetupConstants.CONFIG_VAR_SERVER_URI);
            String dir = baseDir + "/" + deployURI + "/lib";
            File file = new File(dir);
            file.mkdirs();

            writeToFile(dir + "/is-html.xsl", 
                getWebResource("/WEB-INF/classes/is-html.xsl", servletCtx));
            writeToFile(dir + "/is-wml.xsl", 
                getWebResource("/WEB-INF/classes/is-wml.xsl", servletCtx));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private String getWebResource(String fileName, ServletContext servletCtx)
        throws IOException {
        InputStreamReader fin = new InputStreamReader(
            servletCtx.getResourceAsStream(fileName));
        StringBuffer sbuf = new StringBuffer();

        try {
            char[] cbuf = new char[1024];
            int len;
            while ((len = fin.read(cbuf)) > 0) {
                sbuf.append(cbuf, 0, len);
            }
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (Exception ex) {
                    //No handling requried
                }
            }
        }
        return sbuf.toString();
    }

    private void writeToFile(String fileName, String content)
        throws IOException {
        FileWriter fout = null;
        try {
            fout = new FileWriter(fileName);
            fout.write(content);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (Exception ex) {
                    //No handling requried
                }
            }
        }
    }
}

