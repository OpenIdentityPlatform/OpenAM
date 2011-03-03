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
 * $Id: AMTuneFAMBase.java,v 1.9 2009/04/02 06:19:35 kanduls Exp $
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.tune.base;

import com.sun.identity.tune.common.MessageWriter;
import com.sun.identity.tune.common.AMTuneException;
import com.sun.identity.tune.common.AMTuneLogger;
import com.sun.identity.tune.config.AMTuneConfigInfo;
import com.sun.identity.tune.intr.TuneFAM;
import com.sun.identity.tune.util.AMTuneUtil;
import java.io.File;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;

/**
 * Base class for tuning OpenSSO.
 *
 */
public abstract class AMTuneFAMBase extends TuneFAM {
    protected String famPassFilePath;
    protected String famCmdPath;
    protected AMTuneConfigInfo configInfo;
    protected AMTuneLogger pLogger;
    protected MessageWriter mWriter;
    protected String famadmCommonParamsNoServer;

    /**
     * This method initializes the Performance tuning configuration information.
     *
     * @param configInfo Instance of AMTuneConfigInfo class
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public void initialize(AMTuneConfigInfo configInfo)
    throws AMTuneException {
        this.configInfo = configInfo;
        famPassFilePath = AMTuneUtil.TMP_DIR + "ssoadmpassfile";
        pLogger = AMTuneLogger.getLoggerInst();
        mWriter = MessageWriter.getInstance();
        setFAMAdmCmd();
        famadmCommonParamsNoServer = " --adminid " +
                configInfo.getFAMAdmUser() + " --password-file " +
                famPassFilePath;
    }
    
    /**
     * Set ssoadm cmd based on platform.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setFAMAdmCmd() 
    throws AMTuneException {
        if (AMTuneUtil.isWindows()) {
            famCmdPath = configInfo.getFAMAdmLocation() + FILE_SEP +
                    "ssoadm.bat ";
        } else {
            famCmdPath = configInfo.getFAMAdmLocation() + FILE_SEP +
                    "ssoadm ";
        }
        File famAdmF = new File(famCmdPath.trim());
        if (!famAdmF.exists()) {
            AMTuneUtil.printErrorMsg(SSOADM_LOCATION);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-fam-tool-not-found"));
        }
    }

    /**
     * Deletes password file.
     */
    protected void deletePasswordFile() {
        AMTuneUtil.deleteFile(famPassFilePath);
    }
    
    protected void writePasswordToFile() {
        //Nothing need to be done
    }
    
    /**
     * Updates Service config
     * @param args List of properties to be updated.
     */
    protected void updateFAMServiceCfg(List attrs) {
        StringBuffer resultBuffer = new StringBuffer();
        try {
            StringBuilder updateCmd = new StringBuilder(famCmdPath);
            updateCmd.append(UPDATE_SERVER_SUB_CMD);
            updateCmd.append(famadmCommonParamsNoServer);
            updateCmd.append(" ");
            updateCmd.append(SERVER_NAME_OPT);
            updateCmd.append(" ");
            updateCmd.append(configInfo.getFAMServerUrl());
            updateCmd.append(" ");
            updateCmd.append(ATTR_VALUES_OPT);
            if (!AMTuneUtil.isWindows()) {
                updateCmd.append(" ");
            } else {
                updateCmd.append(" \"");
            }
            int retVal;
            Iterator itr = attrs.iterator();
            while(itr.hasNext()) {
                String args = itr.next().toString();
                String cmd = updateCmd.toString() + args;
                if (AMTuneUtil.isWindows()) {
                    cmd = cmd + "\"";
                }
                pLogger.log(Level.FINEST, "updateServiceCfg", 
                        "Executing cmd " + cmd);
                try {
                    if (!AMTuneUtil.isWindows()) {
                        retVal = AMTuneUtil.executeScriptCmd(cmd,
                                configInfo.getFamAdminPassword(),
                                famPassFilePath,
                                resultBuffer);

                    } else {
                        retVal = AMTuneUtil.executeCommand(cmd,
                                configInfo.getFamAdminPassword(),
                                famPassFilePath,
                                resultBuffer);
                    }
                } catch (AMTuneException ex) {
                    retVal = -1;
                }
                if (retVal == -1) {
                    throw new AMTuneException(AMTuneUtil.getResourceBundle()
                            .getString("pt-error-updating-opensso-config"));
                }
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "updateServiceCfg", ex.getMessage());
        }
    }
    
    /**
     * Returns the list of data stores for the realmName
     * @param realmName Name of the realm.
     * @return DataStore names in the form of List.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected List getDataStoreList(String realmName) 
    throws AMTuneException {
        List dataStoreList = new ArrayList();
        try {
            pLogger.log(Level.INFO, "getDataStoreList",
                    "Getting datastore list. ");
            StringBuilder dataStoreListCmd =
                    new StringBuilder(famCmdPath);
            dataStoreListCmd.append(LIST_DATA_STORES_SUB_CMD);
            dataStoreListCmd.append(" -e ");
            dataStoreListCmd.append(realmName);
            dataStoreListCmd.append(famadmCommonParamsNoServer);
            StringBuffer rBuff = new StringBuffer();
            int extVal = AMTuneUtil.executeCommand(dataStoreListCmd.toString(),
                    configInfo.getFamAdminPassword(), 
                    famPassFilePath,
                    rBuff);
            if (extVal == -1) {
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-datastore-list"));
            }
            if (rBuff.indexOf("There were no datastores") != -1) {
                pLogger.log(Level.SEVERE, "getDataStoreList",
                        "No datastore for the realm:" + realmName);
            } else {
                String reqStr = rBuff.toString().trim().replace("Datastore:",
                        "");
                StringTokenizer str = new StringTokenizer(reqStr, "\n");
                while (str.hasMoreTokens()) {
                    String dsName = str.nextToken();
                    if (dsName != null && dsName.trim().length() > 0) {
                        dataStoreList.add(dsName.trim());
                    }
                }
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "getDataStoreList",
                    "Error getting data store list. ");
            throw new AMTuneException(ex.getMessage());
        }
        pLogger.log(Level.FINEST, "getDataStoreList",
                    "Returning datastore list. " + dataStoreList.toString());
        return dataStoreList;
    }
    
    /**
     * This method queries the OpenSSO server and returns the
     * server configuration in the form of Map.
     */
    protected Map getFAMServerConfig() 
    throws AMTuneException {
        Map famCfgInfo = new HashMap();
        StringBuilder listSerCfgCmd = new StringBuilder(famCmdPath);
        listSerCfgCmd.append(LIST_SERVER_CFG_SUB_CMD);
        listSerCfgCmd.append(" ");
        listSerCfgCmd.append(SERVER_NAME_OPT);
        listSerCfgCmd.append(" ");
        listSerCfgCmd.append(configInfo.getFAMServerUrl());
        listSerCfgCmd.append(famadmCommonParamsNoServer);
        listSerCfgCmd.append(" -w");
        StringBuffer rBuff = new StringBuffer();
        int extVal = AMTuneUtil.executeCommand(listSerCfgCmd.toString(),
                configInfo.getFamAdminPassword(),
                famPassFilePath,
                rBuff);
        if (rBuff.toString().indexOf("Login failed") != -1) {
            mWriter.writelnLocaleMsg(
                    "pt-error-opensso-check-user-password-msg");
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-login-failed"));
        }
        if (extVal != -1) {
            StringTokenizer str =
                    new StringTokenizer(rBuff.toString(), "\n");
            while (str.hasMoreTokens()) {
                String line = str.nextToken();
                if (line != null && line.length() > 0) {
                    StringTokenizer lStr = new StringTokenizer(line, 
                            PARAM_VAL_DELIM);
                    lStr.hasMoreTokens();
                    String key = lStr.nextToken();
                    if (lStr.hasMoreTokens()) {
                        String val = lStr.nextToken();
                        famCfgInfo.put(key, val);
                    } else {
                        famCfgInfo.put(key, "");
                    }
                }
            }
        } else {
            pLogger.log(Level.WARNING, "getServerConfig",
                    "Error while getting server configuration.");
        }
        pLogger.log(Level.FINEST, "getServerConfig",
            "Returning OpenSSO configuration Map " + famCfgInfo.toString());
        return famCfgInfo;
    }

    protected boolean isFAMServerUp() {
        boolean isUp = false;
        try {
            URL u = new URL(configInfo.getFAMServerUrl());
            int responseCode = 0;
            pLogger.log(Level.INFO, "isServerUp", 
                    "Connecting to OpenSSO URL : " + u.toString());
            URLConnection  famConn = u.openConnection();
            if (u.getProtocol().equalsIgnoreCase("http")){
                HttpURLConnection testConnect = (HttpURLConnection)famConn;
                testConnect.connect();
                responseCode = testConnect.getResponseCode();
            } else if (u.getProtocol().equalsIgnoreCase("https")) {
                HttpsURLConnection testConnect = (HttpsURLConnection)famConn;
                testConnect.connect();
                responseCode = testConnect.getResponseCode();
            }
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND ||
                    responseCode == HttpsURLConnection.HTTP_NOT_FOUND) {
                mWriter.writelnLocaleMsg("pt-error-tuning-msg");
                mWriter.writeLocaleMsg("pt-fam-server-unreachable-error-msg");
                return isUp;
            }
            isUp = true;
        } catch (UnknownHostException uhx) {
            pLogger.logException("isFAMServerUp", uhx);
            mWriter.writelnLocaleMsg("pt-error-tuning-msg");
            mWriter.writeLocaleMsg("pt-fam-server-unreachable-error-msg");
        } catch (ConnectException cone) {
            pLogger.logException("isFAMServerUp", cone);
            mWriter.writelnLocaleMsg("pt-error-tuning-msg");
            mWriter.writeLocaleMsg("pt-fam-server-down-msg");
        } catch (SSLException ssle) {
            pLogger.logException("isFAMServerUp", ssle);
            mWriter.writelnLocaleMsg("pt-error-tuning-msg");
            mWriter.writeLocaleMsg("pt-fam-server-ssl-error-msg");
        } catch (Exception ex) {
            pLogger.logException("isFAMServerUp", ex);
            mWriter.writelnLocaleMsg("pt-error-tuning-msg");
            mWriter.writeLocaleMsg("pt-fam-server-down-msg");
        }
        return isUp;
    }
}
