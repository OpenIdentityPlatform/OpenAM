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
 * $Id: AMTuneConfigInfo.java,v 1.12 2009/12/09 00:35:59 ykwon Exp $
 */

package com.sun.identity.tune.config;

import com.sun.identity.tune.base.WebContainerConfigInfoBase;
import com.sun.identity.tune.common.MessageWriter;
import com.sun.identity.tune.common.AMTuneException;
import com.sun.identity.tune.common.AMTuneLogger;
import com.sun.identity.tune.common.FileHandler;
import com.sun.identity.tune.constants.DSConstants;
import com.sun.identity.tune.constants.FAMConstants;
import com.sun.identity.tune.constants.AMTuneConstants;
import com.sun.identity.tune.constants.WebContainerConstants;
import com.sun.identity.tune.util.AMTuneUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;

/**
 * This class contains tuning parameter and there values.
 */
public class AMTuneConfigInfo implements AMTuneConstants, DSConstants,
FAMConstants, WebContainerConstants {
    
    private String confFileName;
    private AMTuneLogger pLogger;
    private MessageWriter mWriter;
    private boolean isReviewMode;
    private String logType;
    private boolean tuneOS;
    private boolean tuneWebContainer;
    private boolean tuneDS;
    private boolean tuneFAM;
    private boolean isJVM64BitAvailable;
    private String webContainer;
    private String famAdmLocation;
    private String famConfigDir;
    private String famServerUrl;
    private String famAdmUser;
    private String defaultOrgPeopleContainer;
    private int famTunePctMemoryToUse;
    private int famTunePerThreadStackSizeInKB;
    private int famTunePerThreadStackSizeInKB64Bit;
    private boolean famTuneDontTouchSessionParameters;
    private int famTuneSessionMaxSessionTimeInMts;
    private int famTuneSessionMaxIdleTimeInMts;
    private int famTuneSessionMaxCachingTimeInMts;
    private double famTuneMemMaxHeapSizeRatio;
    private double famTuneMemMinHeapSizeRatio;
    private int famTuneMinMemoryToUseInMB;
    private int famTuneMaxMemoryToUseInMB;
    private int famTuneMaxMemoryToUseInMBDefault;
    private String famTuneMemMaxHeapSizeRatioExp;
    private String famAdminPassword;
    private List realms;
    private int acceptorThreads;
    private int numNotificationQueue;
    private int numNotificationThreads;
    private int numSMLdapThreads;
    private int numLdapAuthThreads;
    private int numRQThrottle;
    private int numOfMaxThreadPool;
    private int numCpus;
    private int memAvail;
    private int memToUse;
    private int maxHeapSize;
    private int minHeapSize;
    private int maxNewSize;
    private int maxPermSize;
    private int cacheSize;
    private int sdkCacheSize;
    private int numSDKCacheEntries;
    private int sessionCacheSize;
    private int numSessions;
    private double amTuneMaxNoThreads;
    private double amTuneMaxNoThreads64Bit;
    private int maxThreads;
    private WebContainerConfigInfoBase webConfigInfo = null;
    private ResourceBundle confBundle;
    private DSConfigInfo dsConfigInfo;
    private String passFilePath  = null;
    
    /**
     * Constructs the instance of AMTuneConfigInfo
     * 
     * @param confFileName Configuration File name.  This file will be 
     * searched in the classpath.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public AMTuneConfigInfo(String confFileName, String passFilePath)
    throws AMTuneException {
        this.confFileName = confFileName;
        this.passFilePath = passFilePath;
        pLogger = AMTuneLogger.getLoggerInst();
        mWriter = MessageWriter.getInstance();
        initialize();
    }
    
    /**
     * Initializes the configuration information.
     * 
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void initialize() 
    throws AMTuneException {
        try {
            confBundle = ResourceBundle.getBundle(confFileName);
            setReviewMode(confBundle.getString(AMTUNE_MODE));
            setLogType(confBundle.getString(AMTUNE_LOG_LEVEL));
            setTuneOS(confBundle.getString(AMTUNE_TUNE_OS));
            setTuneDS(confBundle.getString(AMTUNE_TUNE_DS));
            setTuneWebContainer(
                    confBundle.getString(AMTUNE_TUNE_WEB_CONTAINER));
            setTuneFAM(confBundle.getString(AMTUNE_TUNE_IDENTITY));
            if (isTuneFAM() || isTuneWebContainer()) {
                if (passFilePath == null || 
                        (passFilePath != null && 
                        !new File(passFilePath).exists())) {
                    mWriter.writelnLocaleMsg("pt-password-file-keys-msg");
                    throw new AMTuneException(AMTuneUtil.getResourceBundle()
                            .getString("pt-error-password-file-not-found"));
                }
                setWebContainer(confBundle.getString(WEB_CONTAINER));
                if (isTuneWebContainer() && 
                        !AMTuneUtil.isSupportedWebContainer(getWebContainer()))
                {
                    throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-unsupported-wc-tuning"));
                }
                if (getWebContainer().equalsIgnoreCase(WS7_CONTAINER)) {
                    webConfigInfo = new WS7ContainerConfigInfo(confBundle,
                            passFilePath);
                } else if (getWebContainer().equalsIgnoreCase(AS91_CONTAINER)) {
                    webConfigInfo = new AS9ContainerConfigInfo(confBundle,
                            passFilePath);
                }
            }
            if (webConfigInfo != null) {
                isJVM64BitAvailable = webConfigInfo.isJVM64Bit();
            } else {
                isJVM64BitAvailable = false;
            }
            if (isTuneFAM()) {
                FileHandler pHdl = new FileHandler(passFilePath);
                String reqLine = pHdl.getLine(SSOADM_PASSWORD);
                if (reqLine == null ||
                    (reqLine != null && reqLine.trim().length() < 
                    SSOADM_PASSWORD.length() + 1)) {
                    mWriter.writelnLocaleMsg("pt-cannot-proceed");
                    mWriter.writelnLocaleMsg(
                            "pt-opensso-password-not-found-msg");
                    throw new AMTuneException(AMTuneUtil.getResourceBundle().
                        getString("pt-opensso-password-not-found"));
                } else {
                    setFamAdminPassword(AMTuneUtil.getLastToken(reqLine, "="));
                }
                setFAMAdmLocation(confBundle.getString(SSOADM_LOCATION));
                setFAMAdmUser(confBundle.getString(OPENSSOADMIN_USER));
                setFAMServerUrl(confBundle.getString(OPENSSOSERVER_URL));
                setRealms(confBundle.getString(REALM_NAME));
            }
            if (isTuneDS()) {
                dsConfigInfo = new DSConfigInfo(confBundle, passFilePath);
            } 
            if (isTuneWebContainer() || isTuneFAM()) {
                setFAMTuneMinMemoryToUseInMB(
                        confBundle.getString(AMTUNE_MIN_MEMORY_TO_USE_IN_MB));
                setFAMTuneMaxMemoryToUseInMB();
                setFAMTunePerThreadStackSizeInKB(confBundle.getString(
                        AMTUNE_PER_THREAD_STACK_SIZE_IN_KB));
                setFAMTunePerThreadStackSizeInKB64Bit(confBundle.getString(
                        AMTUNE_PER_THREAD_STACK_SIZE_IN_KB_64_BIT));
                setFAMTunePctMemoryToUse(
                        confBundle.getString(AMTUNE_PCT_MEMORY_TO_USE));
                setFAMTuneMemMaxHeapSizeRatio(confBundle.getString(
                        AMTUNE_MEM_MAX_HEAP_SIZE_RATIO));
                setFAMTuneMemMinHeapSizeRatio(confBundle.getString(
                        AMTUNE_MEM_MIN_HEAP_SIZE_RATIO));
                calculateTuneParams();
            }
        } catch (AMTuneException aex) {
            throw aex;
        } catch (Exception ex) {
            pLogger.logException("initialize", ex);
            throw new AMTuneException(ex.getMessage());
        }
    }
    
    /**
     * This method parses realm string and converts to list of realms to be 
     * tuned.
     * @param realmNames
     */
    private void setRealms(String realmNames) 
    throws AMTuneException {
        if (realmNames != null && realmNames.trim().length() > 0) {
            if (realmNames.indexOf("|") != -1) {
            realms = AMTuneUtil.getTokensList(realmNames, "|"); 
            } else {
                realms = new ArrayList();
                realms.add(realmNames);
            }
        } else {
            AMTuneUtil.printErrorMsg(REALM_NAME);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-realm-name"));
        }
    }
    
    /**
     * Returns list of realms to be tuned.
     * @return
     */
    public List getRealms() {
        return realms;
    }
    
    private void setReviewMode(String reviewMode) 
    throws AMTuneException {
        if (reviewMode == null || 
                (reviewMode != null && reviewMode.trim().length() == 0 )) {
            AMTuneUtil.printErrorMsg(AMTUNE_MODE);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-amtune-mode"));
        } else if (reviewMode.trim().equalsIgnoreCase("REVIEW")) {
            isReviewMode = true;
        } else if (reviewMode.trim().equalsIgnoreCase("CHANGE")) {
            isReviewMode = false;
        } else {
            AMTuneUtil.printErrorMsg(AMTUNE_MODE);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-invalid-amtune-mode"));
        }
        pLogger.log(Level.INFO, "setReviewMode", "Review mode is set to : " + 
                isReviewMode);
    }
    
    /**
     * Return ture if only review mode.
     * @return
     */
    public boolean isReviewMode() {
        return isReviewMode;
    }
    
    /**
     * Set configuration information logging Type
     * @param logType
     */
    private void setLogType(String logType) 
    throws AMTuneException {
        this.logType = logType;
        if (logType == null || (logType != null && 
                logType.trim().length() == 0)) {
            AMTuneUtil.printErrorMsg(AMTUNE_LOG_LEVEL);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-config-log-type"));
        } else if (logType.trim().equalsIgnoreCase("NONE")) {
            MessageWriter.setWriteToFile(true);
            MessageWriter.setWriteToTerm(false);
        } else if (logType.trim().equalsIgnoreCase("TERM")) {
            MessageWriter.setWriteToFile(false);
            MessageWriter.setWriteToTerm(true);
        } else if (logType.trim().equalsIgnoreCase("FILE")) {
            MessageWriter.setWriteToFile(true);
            MessageWriter.setWriteToTerm(true);
        } else {
            AMTuneUtil.printErrorMsg(AMTUNE_LOG_LEVEL);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-invalid-log-level"));
        }
    }
    
    public String getLogType() {
        return logType;
    }
    
    private void setTuneOS(String value) 
    throws AMTuneException {
        if (value == null || (value != null && value.trim().length() == 0)) {
            AMTuneUtil.printErrorMsg(AMTUNE_TUNE_OS);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-tune-os"));
        } else if(value.trim().equalsIgnoreCase("true")) {
            tuneOS = true;
        } else {
            tuneOS = false;
        }
    }
    
    public boolean isTuneOS() {
        return tuneOS;
    }
    
    private void setTuneWebContainer(String value) 
    throws AMTuneException {
        if (value == null || (value != null && value.trim().length() == 0)) {
            AMTuneUtil.printErrorMsg(AMTUNE_TUNE_WEB_CONTAINER);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-tune-ws"));
        } else if (value.trim().equalsIgnoreCase("true")) {
            tuneWebContainer = true;
        } else {
            tuneWebContainer = false;
        }
    }
    
    public boolean isTuneWebContainer() {
        return tuneWebContainer;
    }
    
    private void setTuneDS(String value) 
    throws AMTuneException {
        if (value == null || (value != null && value.trim().length() == 0)) {
            AMTuneUtil.printErrorMsg(AMTUNE_TUNE_DS);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-tune-ds"));
        } else if (value.trim().equalsIgnoreCase("true")) {
            tuneDS = true;
        } else {
            tuneDS = false;
        }
    }
    
    public boolean isTuneDS() {
        return tuneDS;
    }
    
    private void setTuneFAM(String value) 
    throws AMTuneException {
        if (value == null || (value != null && value.trim().length() == 0)) {
            AMTuneUtil.printErrorMsg(AMTUNE_TUNE_IDENTITY);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-tune-opensso"));
        }
        if (value.trim().equalsIgnoreCase("true")) {
            tuneFAM = true;
        } else {
            tuneFAM = false;
        }
    }
    
    public boolean isTuneFAM() {
        return tuneFAM;
    }
    
    /**
     * Set Web container type.
     * @param webContainer webContainer type.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setWebContainer(String webContainer) 
    throws AMTuneException {
        if (webContainer != null && webContainer.trim().length() > 0) {
            this.webContainer = webContainer.trim();
        } else {
            AMTuneUtil.printErrorMsg(WEB_CONTAINER);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-wc"));
        }
    }
    
    public String getWebContainer() {
        return webContainer;
    }
    
    /**
     * Set OpenSSO admin tools location.
     * @param famAdmLocation Directory were ssoadm tool is present.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setFAMAdmLocation(String famAdmLocation) 
    throws AMTuneException {
        if (famAdmLocation != null &&
                famAdmLocation.trim().length() > 0) {
            File famDir = new File(famAdmLocation.trim());
            if (famDir.isDirectory()) {
                this.famAdmLocation = famAdmLocation.trim();
            } else {
                mWriter.write(famAdmLocation + " ");
                mWriter.writeLocaleMsg("pt-not-valid-dir");
                AMTuneUtil.printErrorMsg(SSOADM_LOCATION);
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-invalid-opensso-admin-tools"));
            }
        } else {
            mWriter.writelnLocaleMsg("pt-inval-config");
            AMTuneUtil.printErrorMsg(SSOADM_LOCATION);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-invalid-opensso-admin-tools"));
        }
    }
    
    public String getFAMAdmLocation() {
        return famAdmLocation;
    }
    
    /**
     * Set OpenSSO server URL
     * @param famServerUrl OpenSSO server url.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setFAMServerUrl(String famServerUrl) 
    throws AMTuneException {
        if (famServerUrl != null && famServerUrl.trim().length() > 0) {
            this.famServerUrl = famServerUrl.trim();
        } else {
            AMTuneUtil.printErrorMsg(OPENSSOSERVER_URL);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-fam-server-url-not-found"));
        }
    }
    
    public String getFAMServerUrl() {
        return famServerUrl;
    }
    
    /**
     * Set OpenSSO Administrator User.
     * @param famAdmUser Administrator user name.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setFAMAdmUser(String famAdmUser) 
    throws AMTuneException {
        if (famAdmUser != null && famAdmUser.trim().length() > 0) {
            this.famAdmUser = famAdmUser.trim();
        } else {
            AMTuneUtil.printErrorMsg(OPENSSOADMIN_USER);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-fam-admin-user-not-found"));
        }
    }
    
    public String getFAMAdmUser() {
        return famAdmUser;
    }
    
    /**
     * Percentage memory to Use.
     * @param famTunePctMemoryToUse
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public void setFAMTunePctMemoryToUse(String famTunePctMemoryToUse) 
    throws AMTuneException {
        try {
            if (famTunePctMemoryToUse != null && 
                    famTunePctMemoryToUse.trim().length() > 0) {
                this.famTunePctMemoryToUse = 
                        Integer.parseInt(famTunePctMemoryToUse.trim());
            } else {
                this.famTunePctMemoryToUse = 75;
            }
            if (this.famTunePctMemoryToUse > 100 ) {
                pLogger.log(Level.WARNING, "setTunePctMemoryToUse", 
                    AMTUNE_PCT_MEMORY_TO_USE + " value is > 100 so using " +
                    "default value 100.");
                this.famTunePctMemoryToUse = 100;
            } else if (this.famTunePctMemoryToUse < 0) {
                pLogger.log(Level.WARNING, "setTunePctMemoryToUse", 
                    AMTUNE_PCT_MEMORY_TO_USE + " value is < 0 so using " +
                    "default value 0.");
                this.famTunePctMemoryToUse = 0;
            }
        } catch (NumberFormatException ex) {
            mWriter.writeLocaleMsg("pt-inval-val-msg");
            AMTuneUtil.printErrorMsg(AMTUNE_PCT_MEMORY_TO_USE);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-not-valid-int"));
        }
    }
    
    private int getFAMTunePctMemoryToUse() {
        return famTunePctMemoryToUse;
    }
    
    /**
     * Per Thread Stack size in Kilo bytes.
     * @param famTunePerThreadStackSizeInKB
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setFAMTunePerThreadStackSizeInKB(
            String famTunePerThreadStackSizeInKB)
    throws AMTuneException {
        try {
            if (famTunePerThreadStackSizeInKB != null && 
                    famTunePerThreadStackSizeInKB.trim().length() > 0) {
                this.famTunePerThreadStackSizeInKB =
                        Integer.parseInt(famTunePerThreadStackSizeInKB.trim());
            } else {
                this.famTunePerThreadStackSizeInKB = 128;
            }
        } catch (NumberFormatException ex) {
            mWriter.writeLocaleMsg("pt-inval-val-msg");
            AMTuneUtil.printErrorMsg(AMTUNE_PER_THREAD_STACK_SIZE_IN_KB);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-not-valid-int"));
        }
    }

    public int getFAMTunePerThreadStackSizeInKB() {
        return famTunePerThreadStackSizeInKB;
    }
    
    /**
     * Per Thread Stack Size in kilo bytes for 64 JVM.
     * @param famTunePerThreadStackSizeInKB64Bit
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setFAMTunePerThreadStackSizeInKB64Bit(
            String famTunePerThreadStackSizeInKB64Bit)
    throws AMTuneException {
        try {
            if (famTunePerThreadStackSizeInKB64Bit != null &&
                    famTunePerThreadStackSizeInKB64Bit.trim().length() > 0) {
                this.famTunePerThreadStackSizeInKB64Bit =
                        Integer.parseInt(
                        famTunePerThreadStackSizeInKB64Bit.trim());
            } else {
                this.famTunePerThreadStackSizeInKB64Bit = 512;
            }
        } catch (NumberFormatException ex) {
            mWriter.writeLocaleMsg("pt-inval-val-msg");
            AMTuneUtil.printErrorMsg(AMTUNE_PER_THREAD_STACK_SIZE_IN_KB_64_BIT);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-not-valid-int"));
        }
    }

    public int getFAMTunePerThreadStackSizeInKB64Bit() {
        return famTunePerThreadStackSizeInKB64Bit;
    }

    /**
     * Set Maximum heap size ratio.
     * @param famTuneMemMaxHeapSizeRatio
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setFAMTuneMemMaxHeapSizeRatio(
            String famTuneMemMaxHeapSizeRatio) 
    throws AMTuneException {
        try {
            this.famTuneMemMaxHeapSizeRatio = 
                    AMTuneUtil.evaluteDivExp(
                    famTuneMemMaxHeapSizeRatio.trim());
            this.famTuneMemMaxHeapSizeRatioExp = 
                    famTuneMemMaxHeapSizeRatio.trim();
        } catch (NumberFormatException ex) {
            mWriter.writeLocaleMsg("pt-inval-val-msg");
            AMTuneUtil.printErrorMsg(AMTUNE_MEM_MAX_HEAP_SIZE_RATIO);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-not-valid-exp"));
        } catch (NullPointerException ne) {
            mWriter.writeLocaleMsg("pt-inval-val-msg");
            AMTuneUtil.printErrorMsg(AMTUNE_MEM_MAX_HEAP_SIZE_RATIO);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-operands"));
        }
    }
            
    public double getFAMTuneMemMaxHeapSizeRatio() {
        return famTuneMemMaxHeapSizeRatio;
    }
    
    public String getFAMTuneMemMaxHeapSizeRatioExp() {
        return famTuneMemMaxHeapSizeRatioExp;
    }
    
    /**
     * Set Minimum heap size ratio.
     * @param famTuneMemMinHeapSizeRatio
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setFAMTuneMemMinHeapSizeRatio(
            String famTuneMemMinHeapSizeRatio) 
    throws AMTuneException {
        try {
            this.famTuneMemMinHeapSizeRatio = 
                    AMTuneUtil.evaluteDivExp(famTuneMemMinHeapSizeRatio.trim());
        } catch (NumberFormatException ex) {
            mWriter.writeLocaleMsg("pt-inval-val-msg");
            AMTuneUtil.printErrorMsg(AMTUNE_MEM_MIN_HEAP_SIZE_RATIO);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-not-valid-exp"));
        } catch (NullPointerException ne) {
            mWriter.writeLocaleMsg("pt-inval-val-msg");
            AMTuneUtil.printErrorMsg(AMTUNE_MEM_MIN_HEAP_SIZE_RATIO);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-operands"));
        }
    }
    
    public double getFAMTuneMemMinHeapSizeRation() {
        return famTuneMemMinHeapSizeRatio;
    }
    /**
     * Set Minimum memory to use in MB
     * @param famTuneMinMemoryToUseInMB
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setFAMTuneMinMemoryToUseInMB(String famTuneMinMemoryToUseInMB) 
    throws AMTuneException {
        try {
            if (famTuneMinMemoryToUseInMB != null) {
                this.famTuneMinMemoryToUseInMB = 
                        Integer.parseInt(famTuneMinMemoryToUseInMB);
            } else {
                this.famTuneMinMemoryToUseInMB = 512;
            }
        } catch (NumberFormatException exp) {
            mWriter.writeLocaleMsg("pt-inval-val-msg");
            AMTuneUtil.printErrorMsg(AMTUNE_MIN_MEMORY_TO_USE_IN_MB);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-not-valid-int"));
        }
    }
    
    private void setFAMTuneMaxMemoryToUseInMB() 
    throws Exception {
        if (AMTuneUtil.isLinux() || AMTuneUtil.isSunOs() || 
                AMTuneUtil.isAIX()) {
            if (getWebContainer().equalsIgnoreCase(WS7_CONTAINER)) {
                if (AMTuneUtil.isLinux()) {
                    setFAMTuneMaxMemoryToUseInMB(
                            AMTUNE_MAX_MEMORY_TO_USE_IN_MB_X86);
                } else {
                    setFAMTuneMaxMemoryToUseInMB(
                            AMTUNE_MAX_MEMORY_TO_USE_IN_MB_SOLARIS);
                }
            } else {
                setFAMTuneMaxMemoryToUseInMB(
                        AMTUNE_MAX_MEMORY_TO_USE_IN_MB_SOLARIS);
            }
        } else if (AMTuneUtil.isWindows()) {
            setFAMTuneMaxMemoryToUseInMB(
                    AMTUNE_MAX_MEMORY_TO_USE_IN_MB_DEFAULT);
        }
    }
    
    public int getFAMTuneMinMemoryToUseInMB() {
        return famTuneMinMemoryToUseInMB;
    }
    
    /**
     * Set Maximum memory to user in MB
     * @param famTuneMaxMemoryToUseInMB
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setFAMTuneMaxMemoryToUseInMB (
            String famTuneMaxMemoryToUseInMBKey) 
    throws AMTuneException {
        try {
            String val = confBundle.getString(famTuneMaxMemoryToUseInMBKey);
            if (val == null || (val != null && val.trim().length() == 0 )) {
                AMTuneUtil.printErrorMsg(famTuneMaxMemoryToUseInMBKey);
                throw new AMTuneException("Null value for " + 
                        famTuneMaxMemoryToUseInMBKey);
            }
            this.famTuneMaxMemoryToUseInMB = Integer.parseInt(val.trim());
        } catch (NumberFormatException exp) {
            AMTuneUtil.printErrorMsg(famTuneMaxMemoryToUseInMBKey);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-not-valid-int"));
        } catch (MissingResourceException mex) {
            AMTuneUtil.printErrorMsg(famTuneMaxMemoryToUseInMBKey);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-key-not-found"));
        }
    }
    
    public int getFAMTuneMaxMemoryToUseInMB() {
        return famTuneMaxMemoryToUseInMB;
    }
   
    /**
     * Set OpenSSO admin Password.
     * @param famAdminPassword
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setFamAdminPassword(String famAdminPassword) 
    throws AMTuneException {
        if (famAdminPassword != null && famAdminPassword.trim().length() > 0) {
            this.famAdminPassword = famAdminPassword.trim();
        } else {
            mWriter.writelnLocaleMsg("pt-opensso-password-not-found-msg");
            throw new AMTuneException(AMTuneUtil.getResourceBundle().
                    getString("pt-opensso-password-null"));
        }
    }
    
    public String getFamAdminPassword() {
        return famAdminPassword;
    }
        
    /**
     * This method calculates required tuning parameters based on the 
     * system memory available.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void calculateTuneParams() 
    throws AMTuneException {
        try {
            mWriter.writeln(LINE_SEP);
            mWriter.write("OpenSSO tune ");
            mWriter.writelnLocaleMsg("pt-conf-info");
            mWriter.writeln(LINE_SEP);
            if (isReviewMode) {
                mWriter.writelnLocaleMsg("pt-review-msg");
            } else {
                mWriter.writelnLocaleMsg("pt-change-msg");
            }
            mWriter.writeLocaleMsg("pt-os-msg");
            mWriter.writeln(tuneOS + " ");
            mWriter.writeLocaleMsg("pt-fam-msg");
            mWriter.writeln(tuneFAM + " ");
            mWriter.writeLocaleMsg("pt-ds-msg");
            mWriter.writeln(tuneDS + " ");
            mWriter.writeLocaleMsg("pt-web-msg");
            mWriter.writeln(tuneWebContainer + " ");
            if (webContainer.equalsIgnoreCase(WS7_CONTAINER) || 
                    webContainer.equalsIgnoreCase(WS61_CONTAINER) ||
                    webContainer.equalsIgnoreCase(AS91_CONTAINER)) {
                if (isJVM64BitAvailable) {
                    mWriter.writelnLocaleMsg("pt-ws-64-msg");
                } else {
                    mWriter.writelnLocaleMsg("pt-ws-32-msg");
                }
            }
            mWriter.writeln(LINE_SEP);
            mWriter.writelnLocaleMsg("pt-conf-detecting");
            mWriter.writeln(LINE_SEP);
            numCpus = Integer.parseInt(AMTuneUtil.getNumberOfCPUS());
            mWriter.writeLocaleMsg("pt-no-cpu");
            mWriter.writeln(numCpus + " ");
            acceptorThreads = numCpus;
            mWriter.writeLocaleMsg("pt-ws-acceptor-msg");
            mWriter.writeln(acceptorThreads + " ");
            memAvail = Integer.parseInt(AMTuneUtil.getSystemMemory());
            mWriter.writeLocaleMsg("pt-mem-avail-msg");
            mWriter.writeln(memAvail + " ");
            //if (!webContainer.equals(WS7_CONTAINER)) {
            //    setFAMTuneMaxMemoryToUseInMB(Integer.toString(
            //           getFAMTuneMaxMemoryToUseInMBDefault()));
            //}
            memToUse = (int) (memAvail * getFAMTunePctMemoryToUse() / 100);
            if ((memToUse > famTuneMaxMemoryToUseInMB) && 
                    !isJVM64BitAvailable) {
                memToUse = famTuneMaxMemoryToUseInMB;
            }
            mWriter.writeLocaleMsg("pt-mem-to-use-msg");
            mWriter.writeln(memToUse + " ");
            if (memToUse == 0) {
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-unable-mem-req"));
            }

            if (memToUse >= getFAMTuneMinMemoryToUseInMB()) {
                mWriter.writelnLocaleMsg("pt-enough-mem");
            } else {
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-no-enough-mem"));
            }
            mWriter.writeln(LINE_SEP);
            mWriter.writelnLocaleMsg("pt-conf-calc-tune-params");
            mWriter.writeln(LINE_SEP);
            maxHeapSize = 
                    (int)((double) memToUse * getFAMTuneMemMaxHeapSizeRatio());
            mWriter.writeLocaleMsg("pt-max-heap-size-msg");
            mWriter.writeln(maxHeapSize + " ");
            minHeapSize = maxHeapSize;
            mWriter.writeLocaleMsg("pt-min-heap-size-msg");
            mWriter.writeln(minHeapSize + " ");

            maxNewSize = (int) ((double) maxHeapSize * AMTUNE_MEM_MAX_NEW_SIZE);
            mWriter.writeLocaleMsg("pt-max-new-size-msg");
            mWriter.writeln(maxNewSize + " ");

            if (getWebContainer().equalsIgnoreCase(WS61_CONTAINER)) {
                maxPermSize =
                        (int) ((double) maxHeapSize * AMTUNE_MEM_MAX_PERM_SIZE);
                mWriter.writeLocaleMsg("pt-max-perm-size-msg");
                mWriter.writeln(maxPermSize + " ");
            }
            cacheSize = (int) ((double) maxHeapSize * AMTUNE_MEM_CACHES_SIZE);
            //mWriter.writeLocaleMsg("pt-cache-size-msg"); 
            //mWriter.writeln(cacheSize + " ");
     /**
      * sdkCacheSize is not used because it is recommended to be set at 
      * its default value of 10,000, DEFAULT_SDK_CACHE_MAX_SIZE.
      */ 
            //sdkCacheSize =
            //        (int) ((double) cacheSize * AMTUNE_MEM_SDK_CACHE_SIZE);
            //mWriter.writeLocaleMsg("pt-sdk-cache-size-msg");
            //mWriter.writeln(sdkCacheSize + " ");
            //numSDKCacheEntries = (int) ((double) sdkCacheSize * 1024.0 /
            //        AMTUNE_AVG_PER_ENTRY_CACHE_SIZE_IN_KB);
            //mWriter.writeLocaleMsg("pt-no-sdk-cache-ent-msg");
            //mWriter.writeln(numSDKCacheEntries + " ");
            // sessionCacheSize =
            //        (int) ((double) cacheSize * AMTUNE_MEM_SESSION_CACHE_SIZE);
            sessionCacheSize = cacheSize;
            mWriter.writeLocaleMsg("pt-session-cache-size-msg");
            mWriter.writeln(sessionCacheSize + " ");
            numSessions = (int) ((double) sessionCacheSize * 1024.0 /
                    AMTUNE_AVG_PER_SESSION_SIZE_IN_KB);
            mWriter.writeLocaleMsg("pt-no-session-cache-ent-msg");
            mWriter.writeln(numSessions + " ");
            //AMTUNE_MAX_NUM_THREADS="$AMTUNE_MEM_THREADS_SIZE*
            //(1024/$AMTUNE_PER_THREAD_STACK_SIZE_IN_KB)"
            amTuneMaxNoThreads = (AMTUNE_MEM_THREADS_SIZE *
                    (1024.0 / (double) getFAMTunePerThreadStackSizeInKB()));
            amTuneMaxNoThreads64Bit = (AMTUNE_MEM_THREADS_SIZE *
                    (1024.0 / 
                    (double) getFAMTunePerThreadStackSizeInKB64Bit()));
            maxThreads = 0;
            if (isJVM64BitAvailable) {
                maxThreads = 
                        (int)(amTuneMaxNoThreads64Bit * (double) maxHeapSize);
            } else {
                maxThreads = (int)(amTuneMaxNoThreads * (double) maxHeapSize);
            }
            mWriter.writeLocaleMsg("pt-max-java-threads-msg"); 
            mWriter.writeln(maxThreads + " ");
            numRQThrottle =
                    (int) ((double) maxThreads * AMTUNE_WS_RQTHROTTLE_THREADS);
            numOfMaxThreadPool = numRQThrottle;
            if (getWebContainer().equals(WS61_CONTAINER)) {
                mWriter.writeLocaleMsg("pt-rq-thro-msg");
                mWriter.writeln(numRQThrottle + " ");
            } else {
                mWriter.writeLocaleMsg("pt-max-thread-pool-msg");
                mWriter.writeln(numOfMaxThreadPool + " ");
            }

            numLdapAuthThreads =
                    (int) ((double) maxThreads * AMTUNE_IS_AUTH_LDAP_THREADS);
            mWriter.writeLocaleMsg("pt-ldap-auth-threads-msg");
            mWriter.writeln(numLdapAuthThreads + " ");

            numSMLdapThreads =
                    (int) ((double) maxThreads * AMTUNE_IS_SM_LDAP_THREADS);
            mWriter.writeLocaleMsg("pt-sm-ldap-threads-msg");
            mWriter.writeln(numSMLdapThreads + " ");

            numNotificationThreads = numCpus * 3;
            mWriter.writeLocaleMsg("pt-notification-threads-msg");
            mWriter.writeln(numNotificationThreads + " ");
            numNotificationQueue =
                    (int) (AMTUNE_NOTIFICATION_QUEUE_CALC_FACTOR *
                    (double) numSessions);
            numNotificationQueue = (numNotificationQueue / (10 ^ 1)) * (10 ^ 1);
            
            if (numNotificationQueue >= 30000) {
                numNotificationQueue = 30000;}

            mWriter.writeLocaleMsg("pt-notification-queue-size-msg");
            mWriter.writeln(numNotificationQueue + " ");
            mWriter.writeln(PARA_SEP);
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "calculateTuneParams",
                    "Error while caliculating tuning parameters.");
            pLogger.logException("calculateTuneParams", ex);
            throw new AMTuneException(ex.getMessage());
        }
    }
    
    /**
     * Return number of acceptor threads.
     * @return Acceptor threads.
     */
    public int getAcceptorThreads() {
        return acceptorThreads;
    }
    
    /**
     * Return maximum number of threads.
     * @return number of threads.
     */
    public double getAmTuneMaxNoThreads() {
        return amTuneMaxNoThreads;
    }
    
    /**
     * Return max number of threads in 64 bit machine
     * @return number of threads.
     */
    public double getAmTuneMaxNoThreads64Bit() {
        return amTuneMaxNoThreads64Bit;
    }
    
    /**
     * Return cache size.
     * @return cache size.
     */
    public int getCacheSize() {
        return cacheSize;
    }
    
    /**
     * Return Maximum heap size that can be set.
     * @return maximum heap size.
     */
    public int getMaxHeapSize() {
        return maxHeapSize;
    }
    
    /**
     * Return max new size.
     * @return max new size.
     */
    public int getMaxNewSize() {
        return maxNewSize;
    }
    
    /**
     * Return max Perm Size.
     * @return max Perm Size.
     */
    public int getMaxPermSize() {
        return maxPermSize;
    }
    
    /**
     * Return Max Threads.
     * @return Maximum no of threads.
     */
    public int getMaxThreads() {
        return maxThreads;
    }
    
    /**
     * Return available memory.
     * @return available memory.
     */
    public int getMemAvail() {
        return memAvail;
    }
    
    /**
     * Return Memory to be used.
     * @return Memory to be used.
     */
    public int getMemToUse() {
        return memToUse;
    }
    
    /**
     * Return Minimum heap size.
     * @return Heap size.
     */
    public int getMinHeapSize() {
        return minHeapSize;
    }
    
    /**
     * Return number of CPU's in the system.
     * @return number of cpu's
     */
    public int getNumCpus() {
        return numCpus;
    }
    
    /**
     * Return number of LDAP Auth threads to be used.
     * @return number of LDAP auth threads.
     */
    public int getNumLdapAuthThreads() {
        return numLdapAuthThreads;
    }
    
    /**
     * Return Notification queue size.
     * @return Notification queue size.
     */
    public int getNumNotificationQueue() {
        return numNotificationQueue;
    }
    
    /**
     * Return number of Notification threads to be used.
     * @return Number of Notification threads
     */
    public int getNumNotificationThreads() {
        return numNotificationThreads;
    }
    
    /**
     * Return number of MaxThreads Pool
     * @return Number of MaxThreads Pool
     */
    public int getNumOfMaxThreadPool() {
        return numOfMaxThreadPool;
    }
    
    /**
     * Return RQThrottle
     * @return RQThrottle
     */
    public int getNumRQThrottle() {
        return numRQThrottle;
    }
    
    /**
     * Return number of SDK cache entries.
     * @return Number of SDK cache entries.
     */
    public int getNumSDKCacheEntries() {
        return numSDKCacheEntries;
    }
    
    /**
     * Return number of SMLDAP threads to be used.
     * @return Number of SMLDAP thread to be used.
     */
    public int getNumSMLdapThreads() {
        return numSMLdapThreads;
    }
    
    /**
     * Return number of sessions.
     * @return Number of sessions.
     */
    public int getNumSessions() {
        return numSessions;
    }
    
    /**
     * Return SDK cache size.
     * @return SDK cache size.
     */
    public int getSdkCacheSize() {
        return sdkCacheSize;
    }
    
    /**
     * Return Session cache size
     * @return Session cache size
     */
    public int getSessionCacheSize() {
        return sessionCacheSize;
    }
        
    /**
     * Return web container configuration object.
     * @return
     */
    public WebContainerConfigInfoBase getWSConfigInfo() {
        return webConfigInfo;
    }
        
    public DSConfigInfo getDSConfigInfo() {
        return dsConfigInfo;
    }
    
}
