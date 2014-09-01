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
 * $Id: AMSetupDSConfig.java,v 1.20 2009/11/20 23:52:55 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.setup;

import com.iplanet.am.util.SSLSocketFactoryManager;
import com.sun.identity.common.LDAPUtils;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSSchema;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPSearchResults;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.util.RDN;

/**
 * This class does Directory Server related tasks for 
 * OpenSSO deployed as single web-application. 
 */
public class AMSetupDSConfig {
    private String dsManager;
    private String suffix;
    private String dsHostName;
    private String dsPort;
    private String dsAdminPwd;
    private static LDAPConnection ld = null;
    private String basedir; 
    private String deployuri; 
    private static AMSetupDSConfig dsConfigInstance = null;
    private java.util.Locale locale = null;

    /**
     * Constructs a new instance.
     */
    private AMSetupDSConfig() {
        Map map = ServicesDefaultValues.getDefaultValues();
        dsManager = (String)map.get(SetupConstants.CONFIG_VAR_DS_MGR_DN);
        suffix = (String)map.get(SetupConstants.CONFIG_VAR_ROOT_SUFFIX);
        dsHostName = (String)map.get(
            SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_HOST);
        dsPort = (String)map.get(
            SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_PORT);
        dsAdminPwd = (String)map.get(SetupConstants.CONFIG_VAR_DS_MGR_PWD);
        basedir = (String)map.get(SetupConstants.CONFIG_VAR_BASE_DIR);
        deployuri = (String)map.get(SetupConstants.CONFIG_VAR_SERVER_URI);
    }

    /**
     * Returns a single instance if not already created.
     *
     * @return AMSetupDSConfig instance. 
     */
    public static AMSetupDSConfig getInstance() {
        synchronized (AMSetupDSConfig.class) {
            if (dsConfigInstance == null) {
                dsConfigInstance = new AMSetupDSConfig();
            }
       }
       return dsConfigInstance;
    }

    public void setLocale (java.util.Locale locale) {
        this.locale = locale;
    }

   
    boolean isDServerUp(boolean ssl) {
        LDAPConnection ldc = getLDAPConnection(ssl);
        return ((ldc != null) && ldc.isConnected()) ? true : false;
    }
    /**
     * Validates if directory server is running and can be
     * connected at the specified host and port.
     *
     * @return <code>true</code> if directory server is running.
     */
    boolean isDServerUp() {
        return isDServerUp(false);
    }

    /**
     * Validates the directory server port and returns as an int value.
     *
     * @return port of directory server. 
     * @throws NumberFormatException if port specified is incorrect.
     */
    private int getPort() {
        try {
            return Integer.parseInt(dsPort);
        } catch (NumberFormatException e) {
            throw new ConfiguratorException("configurator.invalidport",
                null, locale);
        }
    }

    /**
     * Set the values required for Service Schema files.
     */
    public void setDSValues() {
        Map map = ServicesDefaultValues.getDefaultValues();
        if ((suffix != null) && (suffix.length() > 0)) {
            suffix = suffix.trim();
            String normalizedDN = LDAPDN.normalize(suffix); 
            String canonicalizedDN = canonicalize(normalizedDN);
            String escapedDN = SMSSchema.escapeSpecialCharacters(normalizedDN);
            String peopleNMDN = "People_" + canonicalizedDN;
            map.put("People_" + SetupConstants.NORMALIZED_ROOT_SUFFIX, 
                replaceDNDelimiter(peopleNMDN, "_"));
            map.put(SetupConstants.SM_ROOT_SUFFIX_HAT, 
                replaceDNDelimiter(escapedDN, "^"));
            map.put(SetupConstants.NORMALIZED_RS, escapedDN); 
            map.put(SetupConstants.NORMALIZED_ORG_BASE, escapedDN); 
            map.put(SetupConstants.ORG_ROOT_SUFFIX, suffix); 
            String rdn = getRDNfromDN(normalizedDN);
            map.put(SetupConstants.RS_RDN, LDAPDN.escapeRDN(rdn)); 
            map.put(SetupConstants.DEFAULT_ORG, canonicalizedDN); 
            map.put(SetupConstants.ORG_BASE, canonicalizedDN);
            map.put(SetupConstants.SM_CONFIG_ROOT_SUFFIX, suffix);
            map.put(SetupConstants.SM_CONFIG_BASEDN, canonicalizedDN);
            map.put(SetupConstants.SM_ROOT_SUFFIX_HAT, 
                replaceDNDelimiter(escapedDN, "^"));
           // Get naming rdn
           String nstr = getRDNfromDN((String) canonicalizedDN);
           map.put(SetupConstants.SM_CONFIG_BASEDN_RDNV, nstr);
        }
    }

    /**
     * Returns the relative DN from the suffix.
     *
     * @param nSuffix Normalized suffix.
     * @return the last component of the suffix. 
     */
    private String getRDNfromDN(String nSuffix) {
        String [] doms = LDAPDN.explodeDN(nSuffix, true);
        return doms[0];
    }

    /**
     * Returns cannonicalized suffix. 
     *
     * @param nSuffix Normalized suffix.
     * @return the cannonicalized suffix. 
     */
    private String canonicalize(String nSuffix) {
        StringBuffer buff = new StringBuffer(1024);
        DN dn = new DN(nSuffix);
        List rdns = dn.getRDNs();
        for (Iterator iter = rdns.iterator(); iter.hasNext();) {
            RDN rdn = (RDN) iter.next();
            buff.append(LDAPDN.escapeRDN(rdn.toString()));
            if (iter.hasNext()) {
                buff.append(",");
            }
        }
        return buff.toString();
    }

    /**
     * Returns suffix with specified delimiter. 
     *
     * @param nSuffix Normalized suffix.
     * @param replaceWith the replacing delimiter to use.
     * @return the suffix with delimiter replaced with the string 
     *         specified as replaceWith. 
     */
    private String replaceDNDelimiter(String nSuffix, String replaceWith) {
        return nSuffix.replaceAll("," ,replaceWith).trim();
    }

    /**
     * Check if Directory Server has the suffix. 
     *
     * @param ssl <code>true</code> if directory server is running on LDAPS.
     * @return <code>true</code> if specified suffix exists. 
     */
    public boolean connectDSwithDN(boolean ssl) {
        String filter = "cn=" + "\"" + suffix + "\"";
        String[] attrs = { "" };
        LDAPSearchResults results = null;
        boolean isValidSuffix = true;
        try {
            results = getLDAPConnection(ssl).search(suffix, 
                LDAPConnection.SCOPE_BASE, filter, attrs, false);
        } catch (LDAPException e) {
            isValidSuffix = false;
            disconnectDServer();
        }
        return isValidSuffix;
    }

    /**
     * Check if DS is loaded with OpenSSO entries
     *
     * @param ssl <code>true</code> of directory server is running on LDAPS.
     * @return <code>true</code> if Service Schema is loaded into
     *         Directory Server.
     */
    String isDITLoaded(boolean ssl) {
        String baseDN = "ou=services," + suffix;
        String filter = "(|(ou=DAI) (ou=sunIdentityRepositoryService))";
        String[] attrs = { "dn" };
        LDAPSearchResults results = null;
        String isLoaded = "false";
        try {
            results = getLDAPConnection(ssl).search(baseDN, 
                LDAPConnection.SCOPE_SUB, filter, attrs, false);
            if (results.getCount() > 0) {
                isLoaded = "true";
            }
        } catch (LDAPException e) {
             if (Debug.getInstance(
                 SetupConstants.DEBUG_NAME).messageEnabled()
             ) {
                 Debug.getInstance(SetupConstants.DEBUG_NAME).message(
                     "AMSetupDSConfig.isDITLoaded: " +
                     "LDAP Operation return code: " +
                     e.getLDAPResultCode());
            }
        }
        return isLoaded;
    }

    /**
     * Loads the schema files into the directory Server.
     *
     * @param schemaFiles Array of schema files to load.
     * @throws ConfiguratorException if unable to load schema.
     */
    public void loadSchemaFiles(List schemaFiles)
        throws ConfiguratorException {
        try {
            for (Iterator i = schemaFiles.iterator(); i.hasNext(); ) {
                String file = (String)i.next();
                int idx = file.lastIndexOf("/");
                String schemaFile = (idx != -1) ? file.substring(idx+1) : file;
                Object[] params = {schemaFile};
                SetupProgress.reportStart("emb.loadingschema", params);
                LDAPUtils.createSchemaFromLDIF(basedir + "/" + schemaFile, ld);
                SetupProgress.reportEnd("emb.success", null);
            }
        } catch (IOException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                 "AMSetupDSConfig.loadSchemaFiles:failed", e);
            SetupProgress.reportEnd("emb.failed", null);
            InstallLog.getInstance().write(
                 "AMSetupDSConfig.loadSchemaFiles:failed", e);
            throw new ConfiguratorException("configurator.ldiferror",
                null, locale);
        } catch (LDAPException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                 "AMSetupDSConfig.loadSchemaFiles:failed", e);
            SetupProgress.reportEnd("emb.failed", null);
            InstallLog.getInstance().write(
                 "AMSetupDSConfig.loadSchemaFiles:failed", e);
            throw new ConfiguratorException(e.getMessage());
        }
    }
  
    /**
     * Helper method to disconnect from Directory Server. 
     */
    private void disconnectDServer() {
        if ((ld != null) && ld.isConnected()) {
            try {
                ld.disconnect();
                ld = null;
                dsConfigInstance = null;
            } catch (LDAPException e) {
                if (Debug.getInstance(
                    SetupConstants.DEBUG_NAME).messageEnabled()
                ) {
                    Debug.getInstance(SetupConstants.DEBUG_NAME).message(
                        "AMSetupDSConfig.disconnectDServer: " +
                        "LDAP Operation return code: " +
                        e.getLDAPResultCode());
                }
            }
        }
    } 
    
    /**
     * Helper method to return Ldap connection 
     *
     * @param ssl <code>true</code> if directory server is running SSL.
     * @return Ldap connection 
     */
    private synchronized LDAPConnection getLDAPConnection(boolean ssl) {
        if (ld == null) {
            try {
                ShutdownManager shutdownMan = ShutdownManager.getInstance();
                if (shutdownMan.acquireValidLock()) {
                    try {
                        ld = (ssl) ? new LDAPConnection(
                            SSLSocketFactoryManager.getSSLSocketFactory()) :
                            new LDAPConnection();
                        ld.setConnectTimeout(300);
                        ld.connect(3, dsHostName, getPort(), dsManager,
                            dsAdminPwd);
                        shutdownMan.addShutdownListener(new
                            ShutdownListener() {

                            public void shutdown() {
                                disconnectDServer();
                            }

                        });
                    } finally {
                        shutdownMan.releaseLockAndNotify();
                    }
                }
            } catch (LDAPException e) {
                disconnectDServer();
                dsConfigInstance = null;
                ld = null;
            } catch (Exception e) {
                dsConfigInstance = null;
                ld = null;
            }
        }
        return ld;
    }
}
