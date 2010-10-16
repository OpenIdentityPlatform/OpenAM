/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: AMTuneDSBase.java,v 1.8 2009/02/27 00:56:16 kanduls Exp $
 */

package com.sun.identity.tune.base;

import com.sun.identity.tune.common.MessageWriter;
import com.sun.identity.tune.common.AMTuneException;
import com.sun.identity.tune.common.AMTuneLogger;
import com.sun.identity.tune.config.DSConfigInfo;
import com.sun.identity.tune.config.AMTuneConfigInfo;
import com.sun.identity.tune.constants.AMTuneConstants;
import com.sun.identity.tune.intr.TuneDS;
import com.sun.identity.tune.util.AMTuneUtil;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import com.sun.identity.shared.ldap.LDAPAttribute;
import com.sun.identity.shared.ldap.LDAPAttributeSet;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPEntry;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPModification;
import com.sun.identity.shared.ldap.LDAPSearchResults;
import com.sun.identity.shared.ldap.LDAPv3;

/**
 * <code>AMTuneDSBase<\code> extends TuneDS and defines the abstract functions
 *
 */
public abstract class AMTuneDSBase extends TuneDS {
    protected AMTuneConfigInfo configInfo;
    protected int curNumberOfWorkerThreads;
    protected int newNumberOfWorkerThreads;
    protected String curAccessLogStatus = null;
    protected String instanceDir = null ;
    protected String dsVersion = null;
    protected String dbDirectory = null;
    protected String dbDN = null;
    protected String dbSuffix = null;
    protected String dbEntryCacheSize = null;
    protected long curDBCacheSize;
    protected long newDBCacheSize;
    protected String dbLocation = null;
    protected String curDBHomeLocation = null;
    protected String newDBHomeLocation = null;
    protected MessageWriter mWriter;
    protected AMTuneLogger pLogger;
    protected String dseLdifPath;
    protected DSConfigInfo dsConfInfo;
    private long memAvail;
    private List dbDirs;
    private boolean init = false;
    private LDAPConnection ldapCon;
    private long memNeeded;
    
    
    protected AMTuneDSBase() {
    }
    /**
     * This method initializes the Performance tuning configuration information.
     *
     * @param configInfo Instance of AMTuneConfigInfo class
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public void initialize(AMTuneConfigInfo configInfo)
    throws AMTuneException {
        try {
            this.configInfo = configInfo;
            dsConfInfo = configInfo.getDSConfigInfo();
            if (!init) {
                pLogger = AMTuneLogger.getLoggerInst();
                mWriter = MessageWriter.getInstance();
                initializeLdapCon();
            }
            validateRootSuffix();
            instanceDir = dsConfInfo.getDsInstanceDir();
            dseLdifPath = instanceDir + FILE_SEP + "config" +
                    FILE_SEP + "dse.ldif";
            validateInstanceDir();
            curNumberOfWorkerThreads =
                    Integer.parseInt(getNumberOfWorkerThreads());
            curAccessLogStatus = getAccessLogStatus();
            curDBCacheSize = Long.parseLong(getDBCacheSize());
            dbSuffix = getBackEnd();
            curDBHomeLocation = getDBHomeLocation();
            dbDirectory = getDBDirectory();
            writePasswordToFile();
        } catch (Exception ex) {
            if (pLogger != null) {
                pLogger.logException("Error initialising " +
                        "Directory Server Base.", ex);
            }
            throw new AMTuneException(ex.getMessage());
        }
    }
    
    protected void validateInstanceDir() 
    throws AMTuneException {
        File ldifFile = new File(dseLdifPath.trim());
        if (!ldifFile.exists()) {
            AMTuneUtil.printErrorMsg(DS_INSTANCE_DIR);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-invalid-ds-instance-dir"));
        }
    }
    
    protected void validateRootSuffix() 
    throws AMTuneException {
        try {
            pLogger.log(Level.FINE, "validateRootSuffix", 
                    "Validating Root Suffix " + dsConfInfo.getRootSuffix());
            searchLDAPAttrVal(dsConfInfo.getRootSuffix(), 
                    dsConfInfo.getRootSuffix(), null);
        } catch(AMTuneException aex) {
            pLogger.log(Level.SEVERE, "validateRootSuffix",
                    "Root suffix validation failed: " + aex.getMessage());
            AMTuneUtil.printErrorMsg(ROOT_SUFFIX);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-invalid-root-suffix"));
        }
    }

    /**
     * Initializes LDAP connection.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void initializeLdapCon()
    throws AMTuneException {
        try {
            if (!init) {
                pLogger.log(Level.FINE, "initializeLdapCon", 
                        "Initializing LDAP Connection.");
                ldapCon = new LDAPConnection();
                ldapCon.connect(AMTuneConstants.LDAP_VERSION,
                        dsConfInfo.getDsHost(),
                        Integer.parseInt(dsConfInfo.getDsPort()),
                        dsConfInfo.getDirMgrUid(),
                        dsConfInfo.getDsDirMgrPassword());
                init = true;
            }
        } catch (LDAPException ex) {
            mWriter.writelnLocaleMsg("pt-cannot-proceed");
            pLogger.logException("initializeLdapCon", ex);
            init = false;
            int errorCode = ex.getLDAPResultCode();
            if ( errorCode == LDAPException.INSUFFICIENT_ACCESS_RIGHTS ) {
                mWriter.writelnLocaleMsg("pt-error-check-ds-dn-and-password");
                throw new AMTuneException(
                        "pt-error-ldap-insufficient-access-rights");
            } else if (errorCode == LDAPException.UNAVAILABLE || 
                     errorCode == LDAPException.SERVER_DOWN || 
                     errorCode == LDAPException.CONNECT_ERROR) {
                mWriter.writelnLocaleMsg("pt-error-check-ds-params-msg");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-cannot-connect-to-ds"));
            } else if (errorCode == LDAPException.INVALID_CREDENTIALS) {
                mWriter.writelnLocaleMsg("pt-error-check-ds-dn-and-password");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-invalid-ds-vals"));
            } else if (errorCode == LDAPException.INVALID_DN_SYNTAX) {
                AMTuneUtil.printErrorMsg(DIRMGR_BIND_DN);
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-invalid-bind-dn"));
            } else {
                 throw new AMTuneException(ex.getMessage());
            }
        }
    }

    /**
     * Searches attribute value in the LDAP uses SCOPE_SUB.
     *
     * @param base Search base
     * @param filter Search Filter.
     * @param attr Attribute to be searched
     * @return Returns the value of the attribute.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected String searchLDAPAttrVal(String base, String filter, String attr)
    throws AMTuneException {
        return searchLDAPAttrVal(base, LDAPv3.SCOPE_SUB, filter, attr);
    }

    /**
     * Searches attribute value in the LDAP
     *
     * @param base Search base
     * @param scope Search Scope
     * @param filter Search filter
     * @param attr Attribute to be searched.
     * @return Returns the value of the attribute.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected String searchLDAPAttrVal(String base,int scope, String filter,
            String attr) 
    throws AMTuneException {
        try {
            pLogger.log(Level.FINE, "searchLDAPAttrVal", "Searching for " +
                    " attribute: " + attr);
            String ldapAttr[] = {attr};
            LDAPSearchResults myResults = ldapCon.search(base,
                    scope, filter, ldapAttr, false);
            while (myResults.hasMoreElements()) {
                LDAPEntry myEntry = myResults.next();
                if (attr != null && attr.equals("dn")) {
                    return myEntry.getDN().replace("dn:", "").trim();
                }
                LDAPAttributeSet entryAttrs = myEntry.getAttributeSet();
                Enumeration attrsInSet = entryAttrs.getAttributes();
                while (attrsInSet.hasMoreElements()) {
                    LDAPAttribute nextAttr =
                            (LDAPAttribute) attrsInSet.nextElement();
                    Enumeration valsInAttr = nextAttr.getStringValues();
                    while (valsInAttr.hasMoreElements()) {
                        String attrVal = (String) valsInAttr.nextElement();
                        return attrVal;
                    }
                }
            }
        } catch (LDAPException lex) {
            pLogger.log(Level.SEVERE, "searchLDAPAttrVal",
                    "Error getting ldap attribute value " + attr +
                    " from base " + base);
            pLogger.logException("searchLDAPAttrVal", lex);
            throw new AMTuneException(lex.getMessage());
        }
        return null;
    }

    /**
     * Returns number of worker threads.
     * @return Returns number of worker threads.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private String getNumberOfWorkerThreads()
    throws AMTuneException {
        pLogger.log(Level.FINE, "getNumberOfWorkerThreads", "Getting number " +
                "of worker threads.");
        String val = searchLDAPAttrVal(CONFIG_DN, LDAPv3.SCOPE_BASE,
                OBJ_CLASS_FILTER, NSSLAPD_THREADNO);
        if (val == null || (val != null && val.length() <= 0)) {
            pLogger.log(Level.SEVERE, "getNumberOfWorkerThreads",
                    "Number of worker thread is null");
            mWriter.writeLocaleMsg("pt-failed-to-obtain-conf");
            mWriter.writelnLocaleMsg("pt-no-worker-threads-msg");
            mWriter.writelnLocaleMsg("pt-inval-config");
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-worker-threads"));
        }
        pLogger.log(Level.FINEST, "getNumberOfWorkerThreads",
                "Returning value " + val);
        return val;
    }

    /**
     * Returns access log status.
     * @return Returns access log status.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private String getAccessLogStatus()
    throws AMTuneException {
        pLogger.log(Level.FINE, "getAccessLogStatus", "Getting access log " +
                "status.");
        String val= searchLDAPAttrVal(CONFIG_DN, LDAPv3.SCOPE_BASE,
                OBJ_CLASS_FILTER, NSSLAPD_ACCESSLOG_LOGGING_ENABLED);
        if (val == null || (val != null && val.length() <= 0)) {
            mWriter.writeLocaleMsg("pt-failed-to-obtain-conf");
            mWriter.writelnLocaleMsg("pt-access-log-status");
            mWriter.writelnLocaleMsg("pt-inval-config");
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-access-log-status"));
        }
        pLogger.log(Level.FINE, "getAccessLogStatus",
                "Returning value. " + val);
        return val;
    }

    /**
     * Returns DB directory path.
     * @return Returns DB directory path.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private String getDBDirectory()
    throws AMTuneException {
        pLogger.log(Level.FINE, "getDBDirectory", "Getting DB Directory.");
        String val = searchLDAPAttrVal(CONFIG_DN,
                "(" + NSSLAPD_SUFFIX + "=" + dsConfInfo.getRootSuffix() + ")",
                NSSLAPD_DIRECTORY);
        if (val == null || (val != null && val.length() <= 0)) {
            mWriter.writeLocaleMsg("pt-failed-to-obtain-conf");
            mWriter.writelnLocaleMsg("pt-db-directory");
            mWriter.writelnLocaleMsg("pt-inval-config");
            throw new AMTuneException(AMTuneUtil.getResourceBundle().
                    getString("pt-error-invalid-db-directory"));
        }
        pLogger.log(Level.FINEST, "getDBDirectory", "Returning value " + val);
        return val;
    }

    /**
     * Returns DB DN
     * @return Returns DB DN
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected String getDBDN()
    throws AMTuneException {
        pLogger.log(Level.FINE, "getDBDN", "Getting DB DN.");
        String val = searchLDAPAttrVal(CONFIG_DN,
                "(" + NSSLAPD_SUFFIX + "=" + dsConfInfo.getRootSuffix() + ")",
                "dn");
        if (val == null || (val != null && val.length() <= 0)) {
            mWriter.writeLocaleMsg("pt-failed-to-obtain-conf");
            mWriter.writelnLocaleMsg("pt-db-dn-msg");
            mWriter.writelnLocaleMsg("pt-inval-config");
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-db-dn"));
        }
        pLogger.log(Level.FINEST, "getDBDN", "Returning DB DN. " + val);
        return val;
    }

    /**
     * Returns DB DN by backend.
     * @param backEnd Name of the back end.
     * @return Returns DB DN by backend.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected String getDBDNbyBackend(String backEnd)
    throws AMTuneException {
        pLogger.log(Level.FINE, "getDBDNbyBackend", "Get DBDN by Back end " +
                backEnd);
        String val = searchLDAPAttrVal(CONFIG_DN,
                "(&(" + NSSLAPD_SUFFIX + "=*" + dsConfInfo.getRootSuffix() +
                ")(cn=" + backEnd + "))", "dn");
        if (val == null || (val != null && val.length() <= 0)) {
            mWriter.writeLocaleMsg("pt-failed-to-obtain-conf");
            mWriter.writeln("DB DN value for " + backEnd);
            mWriter.writelnLocaleMsg("pt-inval-config");
            String msg = AMTuneUtil.getResourceBundle().getString(
                    "pt-error-null-db-db-msg");
            Object[] param = {backEnd};
            throw new AMTuneException(MessageFormat.format(
                    msg, param));
        }
        pLogger.log(Level.FINEST, "getDBDNbyBackend", "Returning value " +
                val);
        return val;
    }

    /**
     * Returns the back end.
     * @return Returns the back end.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private String getBackEnd()
    throws AMTuneException {
        pLogger.log(Level.FINE, "getBackEnd", "Get Back end name.");
        String val = searchLDAPAttrVal(MAPPING_CONF_DN,
                "(&(|(cn=" + dsConfInfo.getRootSuffix() + ")(cn=\"" +
                dsConfInfo.getRootSuffix() + "\")(" + NSSLAPD_PARENT_SUFFIX +
                "=" + dsConfInfo.getRootSuffix() + "))(" + NSSLAPD_BACKEND +
                "=*))", NSSLAPD_BACKEND);
        if (val == null || (val != null && val.length() <= 0)) {
            mWriter.writeLocaleMsg("pt-failed-to-obtain-conf");
            mWriter.writelnLocaleMsg("pt-backend-db");
            mWriter.writelnLocaleMsg("pt-inval-config");
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-back-end"));
        } else {
            val = val.replaceFirst("NetscapeRoot", "");
        }
        pLogger.log(Level.FINEST, "getBackEnd", "Returning value " + val);
        return val;
    }

    /**
     * Returns DB Entry cache size by back end
     * @param backEnd Back end name.
     * @return Entry cache size.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected String getDBEntryCacheSizebyBackend(String backEnd)
    throws AMTuneException {
        pLogger.log(Level.FINE, "getDBEntryCacheSizebyBackend",
                "Get DB entry cache size by back end " + backEnd);
        String val= searchLDAPAttrVal(CONFIG_DN,
                "(&(" + NSSLAPD_SUFFIX + "=*" + dsConfInfo.getRootSuffix() +
                ")(cn=" + backEnd + "))", NSSLAPD_CACHEMEMSIZE);
        if (val == null || (val != null && val.length() <= 0)) {
            pLogger.log(Level.SEVERE, "getDBEntryCacheSizebyBackend",
                    "Null DB Entry cache size for back end " + backEnd);
            mWriter.writeLocaleMsg("pt-failed-to-obtain-conf");
            mWriter.writeLocaleMsg("pt-entry-size-msg");
            mWriter.writeln(backEnd);
            mWriter.writelnLocaleMsg("pt-inval-config");
            String msg = AMTuneUtil.getResourceBundle().getString(
                    "pt-error-null-db-entry-cache-size");
            Object[] param = {backEnd};
            throw new AMTuneException(MessageFormat.format(
                    msg, param));
        }
        pLogger.log(Level.FINEST, "getDBEntryCacheSizebyBackend",
                "Returning value " + val);
        return val;
    }

    /**
     * Gets the Suffix for the back end.
     * @param backEnd
     * @return suffix for the backend.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private String getSuffixbyBackend(String backEnd)
    throws AMTuneException {
        pLogger.log(Level.FINE, "getSuffixbyBackend", "Get Suffix by back end" +
                backEnd);
        String val = searchLDAPAttrVal(CONFIG_DN,
                "(cn=" + backEnd + ")", NSSLAPD_SUFFIX);
        if (val == null || (val != null && val.length() <= 0)) {
            mWriter.writeLocaleMsg("pt-failed-to-obtain-conf");
            mWriter.writeLocaleMsg("pt-back-end-suffix-msg");
            mWriter.writeln(backEnd);
            mWriter.writelnLocaleMsg("pt-inval-config");
            String msg = AMTuneUtil.getResourceBundle().getString(
                    "pt-error-null-suffix-back-end");
            Object[] param = {backEnd};
            throw new AMTuneException(MessageFormat.format(
                    msg, param));
        }
        pLogger.log(Level.FINEST, "getSuffixbyBackend", "Returning value " +
                val);
        return val;
    }

    /**
     * Gets DB cache size
     * @return DB cache size.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private String getDBCacheSize()
    throws AMTuneException {
        pLogger.log(Level.FINE, "getDBCacheSize", "Get DB cache size.");
        String val = searchLDAPAttrVal(DB_PLUGIN_CONF_DN, LDAPv3.SCOPE_BASE,
                OBJ_CLASS_FILTER, NSSLAPD_DBCACHESIZE);
        if (val == null || (val != null && val.length() <= 0)) {
            mWriter.writeLocaleMsg("pt-failed-to-obtain-conf");
            mWriter.writelnLocaleMsg("pt-db-size-msg");
            mWriter.writelnLocaleMsg("pt-inval-config");
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-db-cache-size"));
        }
        pLogger.log(Level.FINEST, "getDBCacheSize", "Returning value " + val);
        return val;
    }

    /**
     * Returns DB home Location.
     * @return Returns DB home Location.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private String getDBHomeLocation()
    throws AMTuneException {
        pLogger.log(Level.FINE, "getDBHomeLocation", "Get DB Home location");
        String val = searchLDAPAttrVal(DB_PLUGIN_CONF_DN, LDAPv3.SCOPE_BASE,
                OBJ_CLASS_FILTER, NSSLAPD_DB_HOME_DIRECTORY);
        if (val == null || (val != null && val.length() <= 0)) {
            mWriter.writeLocaleMsg("pt-failed-to-obtain-conf");
            mWriter.writelnLocaleMsg("pt-db-home-location");
            mWriter.writelnLocaleMsg("pt-inval-config");
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-db-home-location"));
        }
        pLogger.log(Level.FINEST, "getDBHomeLocation", "Getting DB Home" +
                " location.");
        return val;
    }

    /**
     * Returns DB Name.
     * @return Name of DB.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected String getDBName()
    throws AMTuneException {
        pLogger.log(Level.FINE, "getDBName", "Get DB Name.");
        String val = searchLDAPAttrVal(CONFIG_DN, LDAPv3.SCOPE_SUB,
                "(&(|(cn=" + dsConfInfo.getRootSuffix() + ")(cn=\"" +
                dsConfInfo.getRootSuffix() + "\"))(" + NSSLAPD_BACKEND + "=*))",
                NSSLAPD_BACKEND);
        if (val == null || (val != null && val.length() <= 0)) {
            mWriter.writeLocaleMsg("pt-failed-to-obtain-conf");
            mWriter.writelnLocaleMsg("pt-db-name-msg");
            mWriter.writelnLocaleMsg("pt-inval-config");
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-db-name"));
        }
        pLogger.log(Level.FINEST, "getDBName", "Returing value. " + val );
        return val;
    }

    /**
     * Releases the LDAP connection
     */
    protected void releaseCon() {
        try {
            if (ldapCon != null && init) {
                ldapCon.disconnect();
            }
        } catch (LDAPException lex) {
            //Ignore
        }
    }

    /**
     * This method recommends the new DB home location to be used.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected void tuneUsingDSE()
    throws AMTuneException {
        try {
            pLogger.log(Level.FINE, "tuneUsingDSE", "Tune DSE ldif.");
            mWriter.writeln(AMTuneConstants.LINE_SEP);
            mWriter.writeLocaleMsg("pt-tuning");
            mWriter.writeln(dseLdifPath);
            mWriter.writeln(" ");
            mWriter.writeLocaleMsg("pt-file");
            mWriter.writeln(dseLdifPath);
            mWriter.writelnLocaleMsg("pt-param-tuning");
            mWriter.writeln(" ");
            mWriter.writelnLocaleMsg("pt-db-home-dir");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write(NSSLAPD_DB_HOME_DIRECTORY + ": ");
            mWriter.writeln(curDBHomeLocation);
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.write(NSSLAPD_DB_HOME_DIRECTORY + ": ");
            mWriter.writeln(newDBHomeLocation);
            mWriter.writeln(" ");
            mWriter.writeln(" ");
        } catch (Exception ex) {
            throw new AMTuneException(ex.getMessage());
        }
    }

    /**
     * This method recommends tuning parameters of LDAP.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected void ldapTuningRecommendations()
    throws AMTuneException {
        try {
            mWriter.writelnLocaleMsg("pt-param-tuning");
            mWriter.writeln(" ");
            mWriter.writeLocaleMsg("pt-root-suffix-msg");
            mWriter.writeln(dsConfInfo.getRootSuffix());
            mWriter.writelnLocaleMsg("pt-db-db-suffix-msg");
            StringTokenizer st = new StringTokenizer(dbSuffix, " ");
            while (st.hasMoreTokens()) {
                mWriter.writeln("                                          : " +
                        getDBDNbyBackend (st.nextToken()));
            }
            mWriter.writeLocaleMsg("pt-db-dir-suffix-msg");
            mWriter.writeln(dbDirectory);
            mWriter.writeln(" ");
            mWriter.writelnLocaleMsg("pt-ds-worker-threads-msg");
            mWriter.writeLocaleMsg("pt-dn-msg");
            mWriter.writeln(CONFIG_DN);
            mWriter.writeLocaleMsg("pt-attribute-msg");
            mWriter.writeln(NSSLAPD_THREADNO);
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(Integer.toString(curNumberOfWorkerThreads));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(Integer.toString(newNumberOfWorkerThreads));
            mWriter.writeln(" ");
            mWriter.writelnLocaleMsg("pt-access-log-msg");
            mWriter.writeLocaleMsg("pt-dn-msg");
            mWriter.writeln(CONFIG_DN);
            mWriter.writeLocaleMsg("pt-attribute-msg");
            mWriter.writeln(NSSLAPD_ACCESSLOG_LOGGING_ENABLED);
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(curAccessLogStatus);
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln("on");
            mWriter.writeln(" ");
            mWriter.writelnLocaleMsg("pt-db-cache-size-msg");
            mWriter.writeLocaleMsg("pt-dn-msg");
            mWriter.writeln(DB_PLUGIN_CONF_DN);
            mWriter.writeLocaleMsg("pt-attribute-msg");
            mWriter.writeln(NSSLAPD_DBCACHESIZE);
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(Long.toString(curDBCacheSize));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(Long.toString(newDBCacheSize));
            mWriter.writeln(" ");
            mWriter.writelnLocaleMsg("pt-db-entry-size-msg");
            mWriter.writeln(" ");
            st = new StringTokenizer(dbSuffix, " ");
            while (st.hasMoreTokens()) {
                String curToken = st.nextToken();
                String curDbEntryCacheSize =
                        getDBEntryCacheSizebyBackend(curToken);
                if (curDbEntryCacheSize == null ||
                        (curDbEntryCacheSize != null &&
                        (curDbEntryCacheSize.trim().length() == 0))) {
                    mWriter.writeLocaleMsg("pt-fail-ds-conf");
                    mWriter.writeln("DB Entry Cache Size " + curToken);
                    mWriter.writelnLocaleMsg("pt-inval-config");
                    throw new AMTuneException(AMTuneUtil.getResourceBundle()
                            .getString("pt-error-invalid-db-entry-cache-size"));
                }
                long newDBEntryCacheSize =
                        suggestDBEntryCacheSizebyBackend(curToken);
                if (newDBEntryCacheSize == 0) {
                    mWriter.writeLocaleMsg("pt-cannot-compute-rec-val");
                    mWriter.writeln("DB Cache Size for " + curToken);
                    mWriter.writelnLocaleMsg("pt-cannot-proceed");
                    throw new AMTuneException(AMTuneUtil.getResourceBundle()
                            .getString(
                            "pt-error-invalid-new-db-entry-cache-size"));
                }
                mWriter.writeLocaleMsg("pt-suffix-msg");
                mWriter.writeln(getSuffixbyBackend(curToken));
                mWriter.writeLocaleMsg("pt-dn-msg");
                mWriter.writeln(getDBDNbyBackend(curToken));
                mWriter.writeLocaleMsg("pt-attribute-msg");
                mWriter.writeln(NSSLAPD_CACHEMEMSIZE);
                mWriter.writeLocaleMsg("pt-cur-val");
                mWriter.writeln(curDbEntryCacheSize);
                mWriter.writeLocaleMsg("pt-rec-val");
                mWriter.writeln(Long.toString(newDBEntryCacheSize));
                mWriter.writeln(" ");
                mWriter.writeln(" ");
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "ldapTuningRecomendations",
                    "Error computing LDAP Recommendaionts");
            throw new AMTuneException(ex.getMessage());
        }
    }

    /**
     * Adds new entry to LDAP.
     * @param newEntry New entry to be added.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected void addLDAPEntry(LDAPEntry newEntry)
    throws AMTuneException {
        try {
            pLogger.log(Level.FINEST, "addLDAPEntry", "Adding entry " +
                    newEntry.toString());
            ldapCon.add(newEntry);
        } catch (Exception ex) {
            throw new AMTuneException(ex.getMessage());
        }
    }

    /**
     * Apply recommendations to LDAP.
     * @return true if success.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected boolean applyRecommendations()
    throws AMTuneException {
        boolean status = false;
        try {
            pLogger.log(Level.FINE, "applyRecommendations",
                    "Applying recommendations. ");
            if (curNumberOfWorkerThreads < newNumberOfWorkerThreads) {
                mWriter.writeLocaleMsg("pt-modify");
                mWriter.writelnLocaleMsg("pt-no-worker-threads-msg");
                LDAPAttribute threadNoAttr =
                        new LDAPAttribute(NSSLAPD_THREADNO,
                        LDAP_WORKER_THREADS);
                LDAPModification threadNo =
                        new LDAPModification(LDAPModification.REPLACE,
                        threadNoAttr);
                pLogger.log(Level.FINEST, "applyRecommendations",
                        "Modifying worker threads. " +
                        threadNoAttr.toString());
                ldapCon.modify(CONFIG_DN, threadNo);
                status = true;
            } else {
                mWriter.writeLocaleMsg("pt-enough");
                mWriter.writelnLocaleMsg("pt-ds-worker-msg");
            }
            if (!curAccessLogStatus.equals("on")) {
                mWriter.writelnLocaleMsg("pt-modify-access-log-status");
                LDAPAttribute accessLogAttr =
                        new LDAPAttribute(NSSLAPD_ACCESSLOG_LOGGING_ENABLED,
                        "on");
                LDAPModification accessLogModif =
                        new LDAPModification(LDAPModification.REPLACE,
                        accessLogAttr);
                pLogger.log(Level.FINEST, "applyRecommendations",
                        "Modifying access log status. " +
                        accessLogAttr.toString());
                ldapCon.modify(CONFIG_DN, accessLogModif);
                status = true;
            } else {
                mWriter.writelnLocaleMsg("pt-access-log-on");
            }
            if (newDBCacheSize > curDBCacheSize) {
                mWriter.writeLocaleMsg("pt-modify");
                mWriter.writelnLocaleMsg("pt-db-size-msg");
                LDAPAttribute cacheSizeAttr =
                        new LDAPAttribute(NSSLAPD_DBCACHESIZE,
                        Long.toString(newDBCacheSize));
                LDAPModification cachesizeModif =
                        new LDAPModification(LDAPModification.REPLACE,
                        cacheSizeAttr);
                pLogger.log(Level.FINEST, "applyRecommendations",
                        "Modifying db cache size. " +
                        cacheSizeAttr.toString());
                ldapCon.modify(DB_PLUGIN_CONF_DN, cachesizeModif);
                status = true;
            } else {
                mWriter.writeLocaleMsg("pt-db-size-msg");
                mWriter.writelnLocaleMsg("pt-already-enough");
            }
            StringTokenizer st = new StringTokenizer(dbSuffix, " ");
            while (st.hasMoreTokens()) {
                String curToken = st.nextToken();
                long curDBEntryCacheSize =
                        Long.parseLong(getDBEntryCacheSizebyBackend(curToken));
                long newDBEntryCacheSize =
                        suggestDBEntryCacheSizebyBackend(curToken);
                if (newDBEntryCacheSize > curDBEntryCacheSize) {
                    mWriter.writeLocaleMsg("pt-modify");
                    mWriter.writeln("DB Entry Cache Size for " + curToken);
                    String dn = getDBDNbyBackend(curToken);
                    LDAPAttribute cacheMemSizeAttr =
                            new LDAPAttribute(NSSLAPD_CACHEMEMSIZE,
                            Long.toString(newDBEntryCacheSize));
                    LDAPModification cacheMemSizeMod =
                            new LDAPModification(LDAPModification.REPLACE,
                            cacheMemSizeAttr);
                    pLogger.log(Level.FINEST, "applyRecommendations",
                            "Modifying db cache size. " +
                            cacheMemSizeAttr.toString());
                    ldapCon.modify(dn, cacheMemSizeMod);
                    status = true;
                } else {
                    mWriter.write("DB Entry Cache Size ");
                    mWriter.writeLocaleMsg("pt-already-enough");
                    mWriter.writeln(" for " + curToken);
                }
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "applyRecommendations", ex.getMessage());
            throw new AMTuneException(ex.getMessage());
        }
        return status;
    }

    /**
     * Utility function to merge tow lists.
     * @param list1
     * @param list2
     * @return merged list
     */
    private List mergeLists(List list1, List list2) {
        Iterator itr2 = list2.iterator();
        while (itr2.hasNext()) {
            String attr = (String)itr2.next();
            boolean found = false;
            Iterator itr1 = list1.iterator();
            while (itr1.hasNext()) {
                String curAttr = itr1.next().toString();
                if (curAttr.equalsIgnoreCase(attr)) {
                    found =true;
                }
            }
            if (!found) {
               list1.add(attr);
            }
        }
        return list1;
    }

    /**
     * Recommends the parameters that can be tuned later.
     */
    protected void tuneFuture() {
        pLogger.log(Level.FINE, "tuneFuture", "Tuning future.");
        mWriter.writeln(LINE_SEP);
        mWriter.writeln(" ");
        mWriter.writelnLocaleMsg("pt-future-tuning-msg1");
        mWriter.writelnLocaleMsg("pt-future-tuning-msg2");
        mWriter.writelnLocaleMsg("pt-future-tuning-msg3");
        mWriter.write("1. ");
        mWriter.writeLocaleMsg("pt-future-tuning-msg4");
        mWriter.writeln("dn: db_dn");
        mWriter.write("    ");
        mWriter.writeLocaleMsg("pt-a");
        mWriter.writeln(NSSLAPD_SIZELIMIT);
        mWriter.write("    ");
        mWriter.writeLocaleMsg("pt-b");
        mWriter.writeln(NSSLAPD_TIMELIMIT);
        mWriter.write("    ");
        mWriter.writeLocaleMsg("pt-c");
        mWriter.writeln(NSSLAPD_LOOKTHROUGHLIMIT);
        mWriter.write("    ");
        mWriter.writeLocaleMsg("pt-d");
        mWriter.writeln(NSSLAPD_REQUIRED_INDEX);
        mWriter.writeln(" ");
        mWriter.write("2. ");
        mWriter.writeLocaleMsg("pt-future-tuning-msg4");
        mWriter.writeln("dn: " + DB_PLUGIN_CONF_DN);
        mWriter.write("    ");
        mWriter.writeLocaleMsg("pt-a");
        mWriter.writeln(NSSLAPD_DB_TRANSACTION_BATCH_VAL);
        mWriter.write("    ");
        mWriter.writeLocaleMsg("pt-b");
        mWriter.writeln(NSSLAPD_DB_LOGBUF_SIZE);
        mWriter.writeln(" ");
        mWriter.write("3. ");
        mWriter.writeLocaleMsg("pt-future-tuning-msg4");
        mWriter.writeln("dn: "+ REF_INTIGRITY_DN);
        mWriter.write("    ");
        mWriter.writeLocaleMsg("pt-a");
        mWriter.writeln(NSSLAPD_PLUGINARG);
        mWriter.writeln(" ");
        mWriter.writelnLocaleMsg("pt-split-comp-msg");
        mWriter.writelnLocaleMsg("pt-database-trans-logs-msg");
        mWriter.writelnLocaleMsg("pt-isolate-msg");
        mWriter.writeln(" ");
        mWriter.writelnLocaleMsg("pt-delegated-admin-msg1");
        mWriter.writelnLocaleMsg("pt-delegated-admin-msg2");
        mWriter.writeln(" ");
        mWriter.writeln(PARA_SEP);
    }

    /**
     * This method computes the DS tuning Parameters.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected void computeTuneValues()
    throws AMTuneException {
        try {
            pLogger.log(Level.FINE, "computeTuneValues",
                    "Compute Tuning values for DS.");
            memAvail = Long.parseLong(AMTuneUtil.getSystemMemory());
            memAvail = memAvail * 1024 * 1024;
            mWriter.writelnLocaleMsg("pt-calc-ds-mem");
            dbDirs = availableDBDirs();
            if (dbDirs.size() == 0) {
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-loc-db-dir"));
            }
            newDBCacheSize = calculateDBCacheSize();
            if (newDBCacheSize == 0) {
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-compute-db-size"));
            }
            memNeeded = newDBCacheSize + calculateTotalNewEntryDBCacheSize();
            if (memNeeded == 0) {
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-unable-mem-req"));
            }
            mWriter.writeLocaleMsg("pt-avail-mem-size");
            mWriter.writeln(Long.toString(memAvail));
            mWriter.writeLocaleMsg("pt-mem-need-ds-cache");
            mWriter.writeln(Long.toString(memNeeded));
            if (memNeeded <= memAvail) {
                mWriter.writelnLocaleMsg("pt-enough-mem");
            } else {
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-no-enough-mem"));
            }
            String dbDirName = new File(instanceDir).getName();
            newDBHomeLocation = AMTuneUtil.TMP_DIR + dbDirName;
            File newDBHome = new File(newDBHomeLocation);
            boolean createStat = true;
            if (!newDBHome.isDirectory()) {
                createStat = newDBHome.mkdir();
            }
            if (!createStat) {
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-cannot-new-db-home"));
            }
            newNumberOfWorkerThreads = Integer.parseInt(LDAP_WORKER_THREADS);
            if (newNumberOfWorkerThreads == 0) {
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-cannot-new-ds-threads"));
            }
        } catch (NumberFormatException ex) {
            throw new AMTuneException(ex.getMessage());
        }
    }

    /**
     * Computes the new DB Entry Cache Size by Backend.
     * @param backEnd Name of the DB
     * @return Size of the directory DB directory size.
     */
    private long suggestDBEntryCacheSizebyBackend(String backEnd) {
        String dbDir = instanceDir + FILE_SEP + "db" + FILE_SEP +
                backEnd;
        long dbSize = (long) (AMTuneUtil.getDirSize(dbDir) * 1.2) / 1;
        return dbSize;
    }

    /**
     * Computes total new entry DB cache size
     * @return New size.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private long calculateTotalNewEntryDBCacheSize()
    throws AMTuneException {
        pLogger.log(Level.FINE, "calculateTotalNewEntryDBCacheSize",
                "Caliculating total new entry db cache size.");
        StringTokenizer suffixTokens = new StringTokenizer(dbSuffix, " ");
        long totalVal = 0;
        while (suffixTokens.hasMoreTokens()) {
            String curSuffix = suffixTokens.nextToken();
            long sugVal =
                    suggestDBEntryCacheSizebyBackend(curSuffix);
            if (sugVal == 0) {
                mWriter.writeLocaleMsg("pt-cannot-proceed");
                String msg = AMTuneUtil.getResourceBundle()
                        .getString("pt-cannot-compute-rec-db-size");
                Object[] param = {curSuffix + " "};
                throw new AMTuneException(MessageFormat.format(msg, param));
            }
            totalVal = totalVal + sugVal;
        }
        pLogger.log(Level.FINE, "calculateTotalNewEntryDBCacheSize",
                "Returning total size ." + totalVal);
        return totalVal;
    }

    /**
     * Returns the DB cache size.
     * @return
     */
    private long calculateDBCacheSize() {
        pLogger.log(Level.FINE, "calculateDBCacheSize",
                "Caliculate DB cache size.");
        List l = dbDirs;
        long size =0;
        for (int i = 0;i<l.size();i++) {
            size += AMTuneUtil.getDirSize((String)l.get(i));
        }
        //Not multiplying with 1024 becuase file dir size is given in bytes.
        size = ((long)(size * 1.2) / 1);
        pLogger.log(Level.FINE, "calculateDBCacheSize", "Returning size " +
                size);
        return size;
    }

    /**
     * Returns available DB directories.
     * @return List of the DB directories.
     */
    private List availableDBDirs() {
        pLogger.log(Level.FINE, "availableDBDirs", "Getting DB dirs. ");
        File iDir = new File( instanceDir + FILE_SEP + "db");
        String[] list = iDir.list();
        ArrayList dirList = new ArrayList();
        int arrLen = 0;
        for (int i=0; i<list.length; i++) {
            File curFile = new File(iDir.getAbsolutePath()+
                    FILE_SEP + list[i]);
            if (curFile.isDirectory()) {
                dirList.add(arrLen++, curFile.getAbsolutePath());
            }
        }
        pLogger.log(Level.FINEST, "availableDBDirs", "Available DB dirs are " +
                dirList.toString());
        return dirList;
    }
    
    
    protected void writePasswordToFile() 
    throws AMTuneException {
        //nothing to do
    }
    
    protected void deletePasswordFile() {
        //nothing to do
    }
}
