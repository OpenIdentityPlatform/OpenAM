/*
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
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 * Portions Copyrighted 2022-2026 3A Systems LLC.
 */
package com.sun.identity.setup;

import static org.forgerock.opendj.ldap.LDAPConnectionFactory.*;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSSchema;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.openam.ldap.LDAPSchemaModificationException;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.ldap.LdifUtils;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.SSLContextBuilder;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.AddRequest;
import org.forgerock.opendj.ldap.requests.SimpleBindRequest;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.forgerock.util.Options;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;
import org.forgerock.util.time.Duration;

/**
 * This class does Directory Server related tasks for 
 * OpenAM deployed as single web application.
 */
public class AMSetupDSConfig {
    private String dsManager;
    private String suffix;
    private String dsHostName;
    private String dsPort;
    private String dsAdminPwd;
    private static ConnectionFactory ld;
    private String basedir; 
    private String deployuri; 
    private static AMSetupDSConfig dsConfigInstance;
    private Locale locale;

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
        Connection ldc = getLDAPConnection(ssl);
        return ldc != null && ldc.isValid();
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
            String normalizedDN = DN.valueOf(suffix).toString();
            String escapedDN = SMSSchema.escapeSpecialCharacters(normalizedDN);
            String peopleNMDN = "People_" + normalizedDN;
            map.put("People_" + SetupConstants.NORMALIZED_ROOT_SUFFIX, 
                replaceDNDelimiter(peopleNMDN, "_"));
            map.put(SetupConstants.SM_ROOT_SUFFIX_HAT, 
                replaceDNDelimiter(escapedDN, "^"));
            map.put(SetupConstants.SM_ROOT_SUFFIX_HAT_SLASH,
                    replaceDNDelimiter(escapedDN, "^")
                            .replace("=", "\\="));
            map.put(SetupConstants.NORMALIZED_RS, escapedDN); 
            map.put(SetupConstants.NORMALIZED_ORG_BASE, escapedDN); 
            map.put(SetupConstants.ORG_ROOT_SUFFIX, suffix); 
            String rdn = getRDNfromDN(normalizedDN);
            map.put(SetupConstants.RS_RDN, rdn);
            map.put(SetupConstants.DEFAULT_ORG, normalizedDN);
            map.put(SetupConstants.ORG_BASE, normalizedDN);
            map.put(SetupConstants.SM_CONFIG_ROOT_SUFFIX, suffix);
            map.put(SetupConstants.SM_CONFIG_BASEDN, normalizedDN);
            map.put(SetupConstants.SM_ROOT_SUFFIX_HAT, 
                replaceDNDelimiter(escapedDN, "^"));
           // Get naming rdn
           String nstr = getRDNfromDN(normalizedDN);
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
        return LDAPUtils.rdnValueFromDn(nSuffix);
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
        try (Connection conn = getLDAPConnection(ssl)) {
            ConnectionEntryReader results = conn.search(LDAPRequests.newSearchRequest(suffix, SearchScope.BASE_OBJECT,
                    Filter.objectClassPresent().toString()));
            return results.hasNext();
        } catch (LdapException e) {
            disconnectDServer();
            return false;
        }
    }

    /**
     * Ensures that the base entry (root suffix) exists in an external directory
     * server, creating it when it is missing.
     *
     * <p>For an embedded OpenDJ the suffix is created from
     * <code>openam_suffix.ldif</code>. When OpenAM is installed against an
     * external directory server the suffix is assumed to already exist (e.g.
     * created by the OpenDJ docker image with <code>--addBaseEntry</code>).
     * This method makes OpenAM create the base entry itself so the installation
     * succeeds even when the suffix has not been pre-created.</p>
     *
     * <p>The method is idempotent: if the suffix already exists nothing is
     * done. The object class of the created entry is derived from the naming
     * attribute of the suffix (<code>dc</code>, <code>o</code> or
     * <code>ou</code>).</p>
     *
     * @param ssl <code>true</code> if the directory server is running on LDAPS.
     * @throws ConfiguratorException if the base entry cannot be created.
     */
    public void createBaseEntry(boolean ssl) throws ConfiguratorException {
        if ((suffix == null) || (suffix.trim().length() == 0)) {
            return;
        }
        // Suffix already present - nothing to do.
        if (connectDSwithDN(ssl)) {
            return;
        }

        DN suffixDN = DN.valueOf(suffix);
        String namingAttr = suffixDN.rdn().getFirstAVA().getAttributeType().getNameOrOID();
        String rdnValue = LDAPUtils.rdnValueFromDn(suffixDN);

        SetupProgress.reportStart("emb.creatingfamsuffix", null);
        Connection conn = getLDAPConnection(ssl);
        if (conn == null) {
            SetupProgress.reportEnd("emb.creatingfamsuffix.failure", new Object[] { suffix });
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                    "AMSetupDSConfig.createBaseEntry: unable to connect to directory server");
            throw new ConfiguratorException("configurator.dsconnnectfailure", null, locale);
        }
        try {
            AddRequest addRequest = LDAPRequests.newAddRequest(suffixDN)
                    .addAttribute("objectClass", "top");

            if ("dc".equalsIgnoreCase(namingAttr)) {
                addRequest.addAttribute("objectClass", "domain")
                        .addAttribute("dc", rdnValue);
            } else if ("o".equalsIgnoreCase(namingAttr)) {
                addRequest.addAttribute("objectClass", "organization")
                        .addAttribute("o", rdnValue);
            } else if ("ou".equalsIgnoreCase(namingAttr)) {
                addRequest.addAttribute("objectClass", "organizationalUnit")
                        .addAttribute("ou", rdnValue);
            } else {
                // Fallback for any other naming attribute.
                addRequest.addAttribute("objectClass", "extensibleObject")
                        .addAttribute(namingAttr, rdnValue);
            }

            conn.add(addRequest);
            SetupProgress.reportEnd("emb.creatingfamsuffix.success", null);
        } catch (LdapException e) {
            // Created concurrently or already present - acceptable.
            if (e.getResult() != null && ResultCode.ENTRY_ALREADY_EXISTS.equals(e.getResult().getResultCode())) {
                SetupProgress.reportEnd("emb.creatingfamsuffix.success", null);
                return;
            }
            Object[] error = { suffix };
            SetupProgress.reportEnd("emb.creatingfamsuffix.failure", error);
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                    "AMSetupDSConfig.createBaseEntry: failed to create base entry " + suffix, e);
            InstallLog.getInstance().write(
                    "AMSetupDSConfig.createBaseEntry: failed to create base entry " + suffix, e);
            throw new ConfiguratorException("configurator.ldiferror", null, locale);
        } finally {
            conn.close();
        }
    }

    /**
     * Check if DS is loaded with OpenAM entries
     *
     * @param ssl <code>true</code> of directory server is running on LDAPS.
     * @return <code>true</code> if Service Schema is loaded into
     *         Directory Server.
     */
    String isDITLoaded(boolean ssl) {
        String baseDN = "ou=services," + suffix;
        String filter = "(|(ou=DAI)(ou=sunIdentityRepositoryService))";
        try (Connection conn = getLDAPConnection(ssl)){
            ConnectionEntryReader results = conn.search(LDAPRequests.newSearchRequest(baseDN, SearchScope.WHOLE_SUBTREE,
                    filter, "dn"));
            return Boolean.toString(results.hasNext());
        } catch (LdapException e) {
             if (Debug.getInstance(SetupConstants.DEBUG_NAME).messageEnabled()) {
                 Debug.getInstance(SetupConstants.DEBUG_NAME).message(
                     "AMSetupDSConfig.isDITLoaded: LDAP Operation return code: " +
                             e.getResult().getResultCode());
            }
            return "false";
        }
    }

    /**
     * Loads the schema files into the directory Server.
     *
     * @param schemaFiles Array of schema files to load.
     * @throws ConfiguratorException if unable to load schema.
     */
    public void loadSchemaFiles(List schemaFiles)
        throws ConfiguratorException {

        for (Iterator i = schemaFiles.iterator(); i.hasNext(); ) {
            String file = (String) i.next();
            int idx = file.lastIndexOf("/");
            String schemaFile = (idx != -1) ? file.substring(idx + 1) : file;
            Object[] params = {schemaFile};
            SetupProgress.reportStart("emb.loadingschema", params);
            try {
                LdifUtils.createSchemaFromLDIF(basedir + "/" + schemaFile, ld.getConnection());
                SetupProgress.reportEnd("emb.success", null);
            } catch (IOException e) {
                Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                        "AMSetupDSConfig.loadSchemaFiles:failed", e);
                SetupProgress.reportEnd("emb.failed", null);
                InstallLog.getInstance().write(
                        "AMSetupDSConfig.loadSchemaFiles:failed", e);
                if(!(e instanceof LDAPSchemaModificationException)) {
                    throw new ConfiguratorException("configurator.ldiferror",
                            null, locale);
                }
            }
        }
    }
  
    /**
     * Helper method to disconnect from Directory Server. 
     */
    private void disconnectDServer() {
        if (ld != null) {
            ld.close();
            ld = null;
            dsConfigInstance = null;
        }
    } 
    
    /**
     * Helper method to return Ldap connection 
     *
     * @param ssl <code>true</code> if directory server is running SSL.
     * @return Ldap connection 
     */
    private synchronized Connection getLDAPConnection(boolean ssl) {
        try {
            if (ld == null) {
                ShutdownManager shutdownMan = com.sun.identity.common.ShutdownManager.getInstance();

                // All connections will use authentication
                SimpleBindRequest request = LDAPRequests.newSimpleBindRequest(dsManager, dsAdminPwd.toCharArray());
                // REQUEST_TIMEOUT bounds individual LDAP operations, but on its own it does NOT bound
                // the initial TCP connect / bind (nor a connect triggered while chasing a referral).
                // Without a CONNECT_TIMEOUT those can block indefinitely when the directory's listening
                // socket accepts the connection but never completes the handshake - which is exactly how
                // setup hangs for ~20 minutes on some hosts when createBaseEntry() probes an external
                // directory that has no base entry yet. Cap connection establishment so setup either
                // proceeds or fails fast with a visible error instead of stalling the whole install.
                Options options = Options.defaultOptions()
                        .set(CONNECT_TIMEOUT, new Duration((long)10, TimeUnit.SECONDS))
                        .set(REQUEST_TIMEOUT, new Duration((long)3, TimeUnit.SECONDS))
                        .set(AUTHN_BIND_REQUEST, request);

                if (ssl) {
                    String defaultProtocolVersion = SystemProperties.get(Constants.LDAP_SERVER_TLS_VERSION, "TLS");
                    options = options.set(SSL_CONTEXT,
                            new SSLContextBuilder().setProtocol(defaultProtocolVersion).getSSLContext());
                }

                ld = new LDAPConnectionFactory(dsHostName, getPort(), options);

                shutdownMan.addShutdownListener(new
                    ShutdownListener() {

                    public void shutdown() {
                        disconnectDServer();
                    }

                });
            }

            return ld.getConnection();
        } catch (LdapException e) {
            disconnectDServer();
            dsConfigInstance = null;
            ld = null;
        } catch (Exception e) {
            dsConfigInstance = null;
            ld = null;
        }
        return null;
    }
}
