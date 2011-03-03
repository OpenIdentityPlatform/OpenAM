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
 * $Id: TuneFAM8Impl.java,v 1.11 2009/12/09 00:40:35 ykwon Exp $
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.tune.impl;

import com.sun.identity.tune.base.AMTuneFAMBase;
import com.sun.identity.tune.common.FileHandler;
import com.sun.identity.tune.common.AMTuneException;
import com.sun.identity.tune.config.AMTuneConfigInfo;
import com.sun.identity.tune.util.AMTuneUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;

/**
 * <code>TuneFAM8Impl<\code> Tunes OpenSSO Server.
 * 
 */
public class TuneFAM8Impl extends AMTuneFAMBase {
    
    /**
     * Initializes the configuration information.
     * 
     * @param confInfo Configuration information used for computing the tuning 
     *   parameters for OpenSSO.
     * 
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public void initialize(AMTuneConfigInfo configInfo)
    throws AMTuneException {
        super.initialize(configInfo);
    }
    
    /**
     * This method performs the sequence of operations for tuning 
     * OpenSSO server.
     * 
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public void startTuning() 
    throws AMTuneException {
        try {
            int noDataStores = 0;
            pLogger.log(Level.FINE, "startTuning",
                "Start tuning OpenSSO.");
            mWriter.writeln(CHAPTER_SEP);
            mWriter.writelnLocaleMsg("pt-fam-tuning-msg");
            mWriter.writeln(CHAPTER_SEP);
            mWriter.writelnLocaleMsg("pt-init");
            mWriter.writeln(LINE_SEP);
            if (!isFAMServerUp()) {
                return;
            }
            tuneFAMServerConfig();
            tuneServerConfig();
            tuneLDAPConnPool();
            tuneLoggingBufferSize();
            List realmList = configInfo.getRealms();
            Iterator realmItr = realmList.iterator();
            Map dataStoreMap = new HashMap();
            while (realmItr.hasNext()) {
                String curRealm = realmItr.next().toString();
                if (curRealm != null & curRealm.trim().length() > 0) {
                    List dataStoreList = getDataStoreList(curRealm);
                    noDataStores += dataStoreList.size();
                    dataStoreMap.put(curRealm, dataStoreList);
                }
            }
            int sCacheSize = configInfo.getSessionCacheSize() * 1024 * 1024;
            if (noDataStores > 0 && !dataStoreMap.isEmpty()) {
                int maxCacheSizeForEachDataStore = sCacheSize / noDataStores;
                realmItr = realmList.iterator();
                while (realmItr.hasNext()) {
                    String curRealm = realmItr.next().toString();
                    if (curRealm != null & curRealm.trim().length() > 0) {
                        List dataStoreList = (ArrayList) dataStoreMap.get(
                                curRealm);
                        Iterator dtItr = dataStoreList.iterator();
                        while (dtItr.hasNext()) {
                            tuneRealmDataStoreConfig(dtItr.next().toString(),
                                    curRealm, maxCacheSizeForEachDataStore);
                        }
                    }
                }
            }
            mWriter.writeln(" ");
            mWriter.writeln(PARA_SEP);
            mWriter.writelnLocaleMsg("pt-done");
        } catch (Exception ex) {
            pLogger.logException("startTuning", ex);
            mWriter.writeln(" ");
            mWriter.writelnLocaleMsg("pt-error-tuning-msg");
            mWriter.writeln(ex.getMessage());
            mWriter.writelnLocaleMsg("pt-manual-msg");
        } finally {
            deletePasswordFile();
        }
    }
    
    /**
     * This method provides the recommendation for tuning 
     * OpenSSO server and apply those recommendations if run in
     * "CHANGE" mode.
     * 
     */
    protected void tuneFAMServerConfig() 
    throws AMTuneException {
        List newAttrList = new ArrayList();
        Map curCfgMap = getFAMServerConfig();
        String ATTR1 = SDK_CACHE_MAXSIZE + "=" + DEFAULT_SDK_CACHE_MAX_SIZE;
        String ATTR2 = NOTIFICATION_THREADPOOL_SIZE + "=" +
                configInfo.getNumNotificationThreads();
        String ATTR3 = NOTIFICATION_THREADPOOL_THRESHOLD + "=" + 
                configInfo.getNumNotificationQueue();
        String ATTR4 = MAX_SESSIONS + "=" + configInfo.getNumSessions();
        //String ATTR5 = HTTP_SESSION_ENABLED + "=false";
        String ATTR5 = SESSION_PURGE_DELAY + "=0";
        String ATTR6 = INVALID_SESSION_MAX_TIME + "=3";
        String curVal = null;
        mWriter.writelnLocaleMsg("pt-fam-rec-parm-tune-msg");
        mWriter.writeln(" ");
        mWriter.writelnLocaleMsg("pt-param-tuning");
        mWriter.writeln(" ");
        mWriter.writeln("1.    " + SDK_CACHE_MAXSIZE);
        mWriter.writeLocaleMsg("pt-cur-val");
        if (curCfgMap.get(SDK_CACHE_MAXSIZE) != null) {
            curVal = curCfgMap.get(SDK_CACHE_MAXSIZE).toString();
              if (ATTR1.indexOf(curVal) == -1 ) {
                newAttrList.add(ATTR1);
            }
        } else {
            curVal = NO_VAL_SET;
            newAttrList.add(ATTR1);
        }
        mWriter.writeln(SDK_CACHE_MAXSIZE + "=" + curVal);
        mWriter.writeLocaleMsg("pt-rec-val");
        mWriter.writeln(ATTR1);
        mWriter.writeln(" ");
        
        mWriter.writeln("2.    " + NOTIFICATION_THREADPOOL_SIZE);
        mWriter.writeLocaleMsg("pt-cur-val");
        if (curCfgMap.get(NOTIFICATION_THREADPOOL_SIZE) != null) {
            curVal = curCfgMap.get(NOTIFICATION_THREADPOOL_SIZE).toString();
            if (ATTR2.indexOf(curVal) == -1) {
                newAttrList.add(ATTR2);
            }
        } else {
            curVal = NO_VAL_SET;
            newAttrList.add(ATTR2);
        }
        mWriter.writeln(NOTIFICATION_THREADPOOL_SIZE + "=" + curVal);
        mWriter.writeLocaleMsg("pt-rec-val");
        mWriter.writeln(ATTR2);
        mWriter.writeln(" ");
        
        mWriter.writeln("3.    " + NOTIFICATION_THREADPOOL_THRESHOLD);
        mWriter.writeLocaleMsg("pt-cur-val");
        if (curCfgMap.get(NOTIFICATION_THREADPOOL_THRESHOLD) != null) {
            curVal = 
                    curCfgMap.get(NOTIFICATION_THREADPOOL_THRESHOLD).toString();
            if (ATTR3.indexOf(curVal) == -1) {
                newAttrList.add(ATTR3);
            }
        } else {
            curVal = NO_VAL_SET;
            newAttrList.add(ATTR3);
        }
        mWriter.writeln(NOTIFICATION_THREADPOOL_THRESHOLD + "=" + curVal);
        mWriter.writeLocaleMsg("pt-rec-val");
        mWriter.writeln(ATTR3);
        mWriter.writeln(" ");
        
        mWriter.writeln("4.    " + MAX_SESSIONS);
        mWriter.writeLocaleMsg("pt-cur-val");
        if (curCfgMap.get(MAX_SESSIONS) != null ) {
            curVal = curCfgMap.get(MAX_SESSIONS).toString();
            if (ATTR4.indexOf(curVal) == -1) {
                newAttrList.add(ATTR4);
            }
        } else {
            curVal = NO_VAL_SET;
            newAttrList.add(ATTR4);
        }
        mWriter.writeln(MAX_SESSIONS + "=" + curVal);
        mWriter.writeLocaleMsg("pt-rec-val");
        mWriter.writeln(ATTR4);
        mWriter.writeln(" ");
       
     /**
      * HTTP_SESSION_ENABLED parameter is not used in OpenSSO: so
      * it is meaningless to change its value.
      *
      */
        //mWriter.writeln("5.    " + HTTP_SESSION_ENABLED);
        //mWriter.writeLocaleMsg("pt-cur-val");
        //if (curCfgMap.get(HTTP_SESSION_ENABLED) != null) {
        //    curVal = curCfgMap.get(HTTP_SESSION_ENABLED).toString();
        //    if (ATTR5.indexOf(curVal) == -1) {
        //        newAttrList.add(ATTR5);
        //    }
        //} else {
        //    curVal = NO_VAL_SET;
        //    newAttrList.add(ATTR5);
        //}
        //mWriter.writeln(HTTP_SESSION_ENABLED + "=" + curVal);
        //mWriter.writeLocaleMsg("pt-rec-val");
        //mWriter.writeln(ATTR5);
        //mWriter.writeln(" ");
        
        mWriter.writeln("5.    " + SESSION_PURGE_DELAY);
        mWriter.writeLocaleMsg("pt-cur-val");
        if (curCfgMap.get(SESSION_PURGE_DELAY) != null ) {
            curVal = curCfgMap.get(SESSION_PURGE_DELAY).toString();
            if (Integer.parseInt(curVal) != 0) {
                newAttrList.add(ATTR5);
            }
        } else {
            curVal = NO_VAL_SET;
            newAttrList.add(ATTR5);
        }
        mWriter.writeln(SESSION_PURGE_DELAY + "=" +curVal);
        mWriter.writeLocaleMsg("pt-rec-val");
        mWriter.writeln(ATTR5);
        mWriter.writeln(" ");
        
        mWriter.writeln("6.    " + INVALID_SESSION_MAX_TIME);
        mWriter.writeLocaleMsg("pt-cur-val");
        if (curCfgMap.get(INVALID_SESSION_MAX_TIME) != null) {
            curVal = curCfgMap.get(INVALID_SESSION_MAX_TIME).toString();
            if (Integer.parseInt(curVal) != 3) {
                newAttrList.add(ATTR6);
            }
        } else {
            curVal = NO_VAL_SET;
            newAttrList.add(ATTR6);
        }
        mWriter.writeln(INVALID_SESSION_MAX_TIME + "=" + curVal);
        mWriter.writeLocaleMsg("pt-rec-val");
        mWriter.writeln(ATTR6);
        mWriter.writeln(" ");
        if (configInfo.isReviewMode()) {
            return;
        }
        mWriter.writelnLocaleMsg("pt-fam-tuning-server-config");
        if (newAttrList != null && newAttrList.size() > 0) {
            updateFAMServiceCfg(newAttrList);
        } else {
            mWriter.writelnLocaleMsg("pt-fam-cur-rec-val-same");
        }
        mWriter.writeln(" ");
    }
    
    /**
     * This method provides the recommendations for tuning
     * serverconfig LDAP connection pool.
     */
    protected void tuneServerConfig() 
    throws AMTuneException {
        String tuneFile = AMTuneUtil.TMP_DIR + "serverconfig.xml";
        try {
            String newMinPool = "8";
            String newMaxPool = "32";
            StringBuilder getCmd = new StringBuilder(famCmdPath);
            getCmd.append(GET_SVRCFG_XML_SUB_CMD);
            getCmd.append(famadmCommonParamsNoServer);
            getCmd.append(" ");
            getCmd.append(SERVER_NAME_OPT);
            getCmd.append(" ");
            getCmd.append(configInfo.getFAMServerUrl());
            getCmd.append(" ");
            getCmd.append(OUTFILE_OPT);
            getCmd.append(" ");
            getCmd.append(tuneFile);
            StringBuffer rBuff = new StringBuffer();
            int extVal = AMTuneUtil.executeCommand(getCmd.toString(), 
                    configInfo.getFamAdminPassword(), 
                    famPassFilePath,
                    rBuff);
            if (extVal == -1) {
                pLogger.log(Level.SEVERE, "tuneServerConfig", "Couldn't get " +
                        "serverconfig.xml file");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-get-server-cfgfile"));
            }
            FileHandler fh = new FileHandler(tuneFile);
            String reqLine = fh.getLine(SMS_ELEMENT);
            StringTokenizer strT = new StringTokenizer(reqLine, " ");
            String curMinConnPool = "";
            String curMaxConnPool = "";
            while (strT.hasMoreTokens()) {
                String token = strT.nextToken();
                if (token.indexOf(MIN_CONN_POOL) != -1) {
                    curMinConnPool = token.replace(MIN_CONN_POOL + "=", 
                            "").replace("\"", "").replace(">", "").trim();
                }
                if (token.indexOf(MAX_CONN_POOL) != -1) {
                    curMaxConnPool = token.replace(MAX_CONN_POOL + "=",
                            "").replace("\"", "").replace(">", "").trim();
                }
            }
            mWriter.writeln(LINE_SEP);
            mWriter.writelnLocaleMsg("pt-fam-tuning-ldap-con-pool");
            mWriter.writeln(" ");
            mWriter.writelnLocaleMsg("pt-param-tuning");
            mWriter.writeln(" ");
            mWriter.writelnLocaleMsg("pt-minconpool-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(MIN_CONN_POOL + "=" + curMinConnPool);
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(MIN_CONN_POOL + "=" + newMinPool);
            mWriter.writeln(" ");
            mWriter.writelnLocaleMsg("pt-maxconpool-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(MAX_CONN_POOL + "=" + curMaxConnPool);
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(MAX_CONN_POOL + "=" + newMaxPool);
            mWriter.writeln(" ");
            if (configInfo.isReviewMode() || 
                    (curMinConnPool.equals(newMinPool) && 
                    curMaxConnPool.equals(newMaxPool))) {
                return;
            }
            AMTuneUtil.backupConfigFile(tuneFile, OPENSSO_SERVER_BACKUP_DIR);
            int lineNo = fh.getLineNum(SMS_ELEMENT);
            reqLine = fh.getLine(SMS_ELEMENT);
            reqLine = reqLine.replace(MIN_CONN_POOL + "=\"" + curMinConnPool + 
                    "\"", MIN_CONN_POOL + "=\"" + newMinPool + "\"");
            reqLine = reqLine.replace(MAX_CONN_POOL + "=\"" + curMaxConnPool + 
                    "\"", MAX_CONN_POOL + "=\"" + newMaxPool + "\"");
            fh.replaceLine(lineNo, reqLine);
            //Replace the default server value as well.
            lineNo = fh.getLineNum(DEFAULT_SERVER_ELEMENT);
            reqLine = fh.getLine(DEFAULT_SERVER_ELEMENT);
            String curDefaultMinConPoolVal = "";
            String curDefaultMaxConPoolVal = "";
            strT = new StringTokenizer(reqLine, " ");
            while (strT.hasMoreTokens()) {
                String token = strT.nextToken();
                if (token.indexOf(MIN_CONN_POOL) != -1) {
                    curDefaultMinConPoolVal = token.replace(MIN_CONN_POOL + "=", 
                            "").replace("\"", "").replace(">", "").trim();
                }
                if (token.indexOf(MAX_CONN_POOL) != -1) {
                    curDefaultMaxConPoolVal = token.replace(MAX_CONN_POOL + "=",
                            "").replace("\"", "").replace(">", "").trim();
                }
            }
            reqLine = reqLine.replace(MIN_CONN_POOL + "=\"" + 
                    curDefaultMinConPoolVal + 
                    "\"", MIN_CONN_POOL + "=\"" + newMinPool + "\"");
            reqLine = reqLine.replace(MAX_CONN_POOL + "=\"" + 
                    curDefaultMaxConPoolVal + 
                    "\"", MAX_CONN_POOL + "=\"" + newMaxPool + "\"");
            fh.replaceLine(lineNo, reqLine);
            fh.close();
            rBuff.setLength(0);
            StringBuilder setCmd = new StringBuilder(famCmdPath);
            setCmd.append(SET_SVRCFG_XML_SUB_CMD);
            setCmd.append(famadmCommonParamsNoServer);
            setCmd.append(" ");
            setCmd.append(SERVER_NAME_OPT);
            setCmd.append(" ");
            setCmd.append(configInfo.getFAMServerUrl());
            setCmd.append(" ");
            setCmd.append(XML_FILE_OPT);
            setCmd.append(" ");
            setCmd.append(tuneFile);
            extVal = AMTuneUtil.executeCommand(setCmd.toString(), 
                    configInfo.getFamAdminPassword(), 
                    famPassFilePath,
                    rBuff);
            if (extVal == -1) {
                pLogger.log(Level.SEVERE, "tuneServerConfig", "Couldn't set " +
                        "serverconfig.xml file");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-set-server-cfgfile"));
            }
            
        } catch (AMTuneException amex) {
            throw amex;
        } catch (Exception ex) {
            pLogger.logException("tuneServerConfig", ex);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-opensso-srv-config"));
        } finally {
            AMTuneUtil.deleteFile(tuneFile);
        }
    }
    
    /**
     * This method provides the recommendations for LDAP Connection Pool in 
     * Global iPlanetAMAuthService...
     */
    protected void tuneLDAPConnPool() 
    throws AMTuneException {
        try {
            String minMaxRatio = "8:32";
            StringBuilder getCmd = new StringBuilder(famCmdPath);
            getCmd.append(GET_ATTR_DEFS_SUB_CMD);
            getCmd.append(famadmCommonParamsNoServer);
            getCmd.append(" ");
            getCmd.append(SERVICE_NAME_OPT);
            getCmd.append(" ");
            getCmd.append(AUTH_SVC);
            getCmd.append(" "); 
            getCmd.append(SCHEMA_TYPE_OPT);
            getCmd.append(" "); 
            getCmd.append(GLOBAL_SCHEMA);
            getCmd.append(" ");
            getCmd.append(ATTR_NAMES_OPT);
            getCmd.append(" ");
            getCmd.append(LDAP_CONNECTION_POOL_SIZE);
            StringBuffer rBuff = new StringBuffer();
            int extVal = AMTuneUtil.executeCommand(getCmd.toString(), 
                    configInfo.getFamAdminPassword(), 
                    famPassFilePath,
                    rBuff);
            if (extVal == -1) {
                pLogger.log(Level.SEVERE, "tuneLDAPConnPool", "Error getting " +
                        "ldapconnpool size.");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-get-ldapconnpool-size"));
            }
            StringTokenizer str = new StringTokenizer(rBuff.toString(), "\n");
            String curLdapConVal = "";
            while (str.hasMoreTokens()) {
                String reqLine = str.nextToken();
                if (reqLine.indexOf(LDAP_CONNECTION_POOL_SIZE) != -1) {
                    curLdapConVal = AMTuneUtil.getLastToken(reqLine, "=");
                }
            }
            mWriter.writeln(LINE_SEP);
            mWriter.writelnLocaleMsg("pt-fam-tuning-global-auth-svc");
            mWriter.writeln(" ");
            mWriter.writeln("Service              : iPlanetAMAuthService");
            mWriter.writeln("SchemaType           : global");
            mWriter.writelnLocaleMsg("pt-param-tuning");
            mWriter.writeln(" ");
            mWriter.writeln("1.   " + LDAP_CONNECTION_POOL_SIZE);
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writelnLocaleMsg(LDAP_CONNECTION_POOL_SIZE + "=" +
                    curLdapConVal);
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(LDAP_CONNECTION_POOL_SIZE + "=" + minMaxRatio);
            mWriter.writeln(" ");
            if (configInfo.isReviewMode() || 
                    minMaxRatio.equals(curLdapConVal)) {
                return;
            }
            StringBuilder setCmd = new StringBuilder(famCmdPath);
            setCmd.append(SET_ATTR_DEFS_SUB_CMD);
            setCmd.append(famadmCommonParamsNoServer);
            setCmd.append(" ");
            setCmd.append(SERVICE_NAME_OPT);
            setCmd.append(" ");
            setCmd.append(AUTH_SVC);
            setCmd.append(" "); 
            setCmd.append(SCHEMA_TYPE_OPT);
            setCmd.append(" "); 
            setCmd.append(GLOBAL_SCHEMA);
            setCmd.append(" ");
            setCmd.append(ATTR_VALUES_OPT);
            if (!AMTuneUtil.isWindows()) {
                setCmd.append(" ");
                setCmd.append(LDAP_CONNECTION_POOL_SIZE);
                setCmd.append("=");
                setCmd.append(minMaxRatio);
            } else {
                setCmd.append(" \"");
                setCmd.append(LDAP_CONNECTION_POOL_SIZE);
                setCmd.append("=");
                setCmd.append(minMaxRatio);
                setCmd.append("\"");
            }
            rBuff.setLength(0);
            extVal = AMTuneUtil.executeCommand(setCmd.toString(), 
                    configInfo.getFamAdminPassword(), 
                    famPassFilePath,
                    rBuff);
            if (extVal == -1) {
                pLogger.log(Level.SEVERE, "tuneLDAPConnPool", "Error setting " +
                        "ldapconnpool size.");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-set-ldapconnpool-size"));
            }
        } catch(AMTuneException aex) {
            throw aex;
        } catch (Exception ex) {
            pLogger.logException("tuneLDAPConnPool", ex);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-set-ldapconnpool-size"));
        }
    }
    
    protected void tuneRealmDataStoreConfig(String dataStoreType, String realm,
            int newCacheSize)
    throws AMTuneException {
        String dataFile = AMTuneUtil.TMP_DIR + "attrvals.txt";
        try {
            String poolMin = "8";
            String poolMax = "32";
            mWriter.writeln(LINE_SEP);
            StringBuilder dataStoreInfoCmd = new StringBuilder(famCmdPath);
            dataStoreInfoCmd.append(SHOW_DATASTORE_SUB_CMD);
            dataStoreInfoCmd.append(" ");
            dataStoreInfoCmd.append(REALM_OPT);
            dataStoreInfoCmd.append(" ");
            dataStoreInfoCmd.append("REALM_NAME");
            dataStoreInfoCmd.append(" -m \"");
            dataStoreInfoCmd.append(dataStoreType);
            dataStoreInfoCmd.append("\"");
            dataStoreInfoCmd.append(famadmCommonParamsNoServer);
            
            StringBuilder dataStoreUpdateCmd = new StringBuilder(famCmdPath);
            dataStoreUpdateCmd.append(UPDATE_DATASTORE_SUB_CMD);
            dataStoreUpdateCmd.append(" ");
            dataStoreUpdateCmd.append(REALM_OPT);
            dataStoreUpdateCmd.append(" ");
            dataStoreUpdateCmd.append("REALM_NAME");
            dataStoreUpdateCmd.append(" -m \"");
            dataStoreUpdateCmd.append(dataStoreType);
            dataStoreUpdateCmd.append("\"");
            dataStoreUpdateCmd.append(famadmCommonParamsNoServer);
            mWriter.writeLocaleMsg("pt-tuning");
            mWriter.write("DataStore \"" + dataStoreType + "\" ");
            mWriter.writelnLocaleMsg("pt-fam-realm-ldapconn-tuning");
            mWriter.writelnLocaleMsg("pt-param-tuning");
            mWriter.writeln(" ");
            mWriter.writeln("Realm Name : " + realm + ":");
            mWriter.writelnLocaleMsg("pt-min-max-ldap-conn-pool");
            mWriter.writelnLocaleMsg("pt-cur-val");
            StringBuffer rBuf = new StringBuffer();
            int extVal = 0;
            if (!AMTuneUtil.isWindows()) {
                //write the command to file and then execute the file
                //workaround as ssoadm is not working directly from
                //runtime in *unix if any option contains space character.
                // -m "Sun DS with AM Schema"
                extVal = AMTuneUtil.executeScriptCmd(
                        dataStoreInfoCmd.toString().replace(
                        "REALM_NAME", realm), configInfo.getFamAdminPassword(), 
                        famPassFilePath,
                        rBuf);
            } else {
                extVal = AMTuneUtil.executeCommand(
                        dataStoreInfoCmd.toString().replace(
                        "REALM_NAME", realm), configInfo.getFamAdminPassword(), 
                        famPassFilePath,
                        rBuf);
            }
            if (extVal == -1) {
                pLogger.log(Level.SEVERE, "tuneRealmDataStoreConfig",
                        "Error finding realm ldap config info ");
            }
            String dsFile = AMTuneUtil.TMP_DIR + realm.replace("/", "rdelim") +
                    "datastore.txt";
            AMTuneUtil.writeResultBufferToTempFile(rBuf, dsFile);
            FileHandler fh = new FileHandler(dsFile);
            String curMax = AMTuneUtil.getLastToken(
                    fh.getLine(LDAP_CONN_POOL_MAX), PARAM_VAL_DELIM);
            String curMin = AMTuneUtil.getLastToken(
                    fh.getLine(LDAP_CONN_POOL_MIN), PARAM_VAL_DELIM);
            mWriter.writeln(LDAP_CONN_POOL_MIN + "=" + curMin);
            mWriter.writeln(LDAP_CONN_POOL_MAX + "=" + curMax);
            mWriter.writelnLocaleMsg("pt-rec-val");
            mWriter.writeln(LDAP_CONN_POOL_MIN + "=" + poolMin);
            mWriter.writeln(LDAP_CONN_POOL_MAX + "=" + poolMax);
            mWriter.writeln(" ");
            /*Uncomment after fixing the root cause of the issue 3014
            int curCacheSize = Integer.parseInt(AMTuneUtil.getLastToken(
                    fh.getLine(LDAP_CONFIG_CACHE_SIZE), PARAM_VAL_DELIM));
            mWriter.writelnLocaleMsg("pt-max-cache-size-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(LDAP_CONFIG_CACHE_SIZE + "=" + curCacheSize);
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(LDAP_CONFIG_CACHE_SIZE + "=" + newCacheSize);
            if (configInfo.isReviewMode() || curCacheSize == newCacheSize) {
                return;
            }*/
            if (configInfo.isReviewMode()) {
                return ;
            }
            StringBuffer attrVals = new StringBuffer();
            if (!curMin.equals(poolMin)) {
                attrVals.append(LDAP_CONN_POOL_MIN);
                attrVals.append(PARAM_VAL_DELIM);
                attrVals.append(poolMin);
                attrVals.append("\n");
            }
            if (!curMax.equals(poolMax)) {
                attrVals.append(LDAP_CONN_POOL_MAX);
                attrVals.append(PARAM_VAL_DELIM);
                attrVals.append(poolMax);
                attrVals.append("\n");
            }
            /* Uncomment after fixing the root cause of the issue 3014
            if (newCacheSize > curCacheSize) {
                attrVals.append(LDAP_CONFIG_CACHE_SIZE);
                attrVals.append(PARAM_VAL_DELIM);
                attrVals.append(newCacheSize);
            }*/
            if (attrVals.toString().length() == 0) {
                pLogger.log(Level.INFO, "tuneRealmDataStoreConfig",
                        "LDAP config values are same as recommended values.");
                return;
            }
            AMTuneUtil.writeResultBufferToTempFile(attrVals, dataFile);
            dataStoreUpdateCmd.append(" ");
            dataStoreUpdateCmd.append(DATAFILE_OPT);
            dataStoreUpdateCmd.append(" ");
            dataStoreUpdateCmd.append(dataFile);
            AMTuneUtil.backupConfigFile(dsFile, OPENSSO_SERVER_BACKUP_DIR);
            rBuf.setLength(0);
            if (!AMTuneUtil.isWindows()) {
                extVal = AMTuneUtil.executeScriptCmd(
                        dataStoreUpdateCmd.toString().replace(
                        "REALM_NAME", realm), configInfo.getFamAdminPassword(), 
                        famPassFilePath,
                        rBuf);
            } else {
                extVal = AMTuneUtil.executeCommand(
                        dataStoreUpdateCmd.toString().replace(
                        "REALM_NAME", realm), configInfo.getFamAdminPassword(), 
                        famPassFilePath,
                        rBuf);
            }
            if (extVal == -1) {
                    pLogger.log(Level.SEVERE, "tuneRealmDataStoreConfig",
                        "Error setting realm ldap config info.");
            }
            File dsF = new File(dsFile);
            if (dsF.isFile()) {
                dsF.delete();
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "tuneRealmDataStoreConfig",
                    "Error tuning data store values. ");
            throw new AMTuneException(ex.getMessage());
        } finally {
            File delFile = new File(dataFile);
            if (delFile.isFile()) {
                delFile.delete();
            }
        }
    }
    
    /**
     * This method sets the value of logging buffer size in 
     * iPlanetAMLoggingService 
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected void tuneLoggingBufferSize()
    throws AMTuneException {
        pLogger.log(Level.FINEST, "tuneLoggingBufferSize", "Tuning logging" +
                "Buffer size.");
        try {
            String recBufferSizeVal = "250";
            mWriter.writeln(LINE_SEP);
            mWriter.writelnLocaleMsg("pt-fam-tuning-logging-buffer-size");
            mWriter.writeln(" ");
            mWriter.writeln("Service              : " +  LOGGING_SVC);
            mWriter.writeln("SchemaType           : global");
            mWriter.writelnLocaleMsg("pt-param-tuning");
            mWriter.writeln(" ");
            StringBuffer getAttrDefs = new StringBuffer(famCmdPath);
            getAttrDefs.append(GET_ATTR_DEFS_SUB_CMD);
            getAttrDefs.append(" ");
            getAttrDefs.append(SERVICE_NAME_OPT);
            getAttrDefs.append(" ");
            getAttrDefs.append(LOGGING_SVC);
            getAttrDefs.append(" ");
            getAttrDefs.append(SCHEMA_TYPE_OPT);
            getAttrDefs.append(" ");
            getAttrDefs.append(GLOBAL_SCHEMA);
            getAttrDefs.append(" ");
            getAttrDefs.append(ATTR_NAMES_OPT);
            getAttrDefs.append(" ");
            getAttrDefs.append(LOGGING_BUFFER_SIZE);
            getAttrDefs.append(famadmCommonParamsNoServer);
            StringBuffer rBuff = new StringBuffer();
            int extVal = AMTuneUtil.executeCommand(getAttrDefs.toString(),
                    configInfo.getFamAdminPassword(), 
                    famPassFilePath,
                    rBuff);
            if (extVal == -1) {
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-get-logging-buffer-size"));
            }
            StringTokenizer str = new StringTokenizer(rBuff.toString(), "\n");
            String curLdapScopeVal = "";
            while (str.hasMoreTokens()) {
                String reqLine = str.nextToken();
                if (reqLine.indexOf(LOGGING_BUFFER_SIZE) != -1) {
                    curLdapScopeVal = AMTuneUtil.getLastToken(reqLine,
                            PARAM_VAL_DELIM);
                }
            }
            mWriter.writeln("1.   " + LOGGING_BUFFER_SIZE);
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(LOGGING_BUFFER_SIZE + "=" + curLdapScopeVal);
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(LOGGING_BUFFER_SIZE + "=" + recBufferSizeVal);
            mWriter.writeln(" ");
            if (configInfo.isReviewMode() || 
                    curLdapScopeVal.equals(recBufferSizeVal)) {
                return;
            }
            StringBuffer setCmd = new StringBuffer(famCmdPath);
            setCmd.append(SET_ATTR_DEFS_SUB_CMD);
            setCmd.append(famadmCommonParamsNoServer);
            setCmd.append(" ");
            setCmd.append(SERVICE_NAME_OPT);
            setCmd.append(" ");
            setCmd.append(LOGGING_SVC);
            setCmd.append(" ");
            setCmd.append(SCHEMA_TYPE_OPT);
            setCmd.append(" ");
            setCmd.append(GLOBAL_SCHEMA);
            setCmd.append(" ");
            setCmd.append(ATTR_VALUES_OPT);
            if (!AMTuneUtil.isWindows()) {
                setCmd.append(" ");
                setCmd.append(LOGGING_BUFFER_SIZE);
                setCmd.append("=");
                setCmd.append(recBufferSizeVal);
            } else {
                setCmd.append(" \"");
                setCmd.append(LOGGING_BUFFER_SIZE);
                setCmd.append("=");
                setCmd.append(recBufferSizeVal);
                setCmd.append("\"");
            }
            rBuff.setLength(0);
            extVal = AMTuneUtil.executeCommand(setCmd.toString(), 
                    configInfo.getFamAdminPassword(), 
                    famPassFilePath,
                    rBuff);
            if (extVal == -1) {
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-get-logging-buffer-size"));
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "tuneLoggingBufferSize", 
                    "Error tuning logging buffer size. ");
            throw new AMTuneException(ex.getMessage());
        }
    }
}
