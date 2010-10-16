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
 * $Id: DSConfigInfo.java,v 1.5 2008/08/29 10:15:27 kanduls Exp $
 */

package com.sun.identity.tune.config;

import com.sun.identity.tune.common.MessageWriter;
import com.sun.identity.tune.common.AMTuneException;
import com.sun.identity.tune.common.AMTuneLogger;
import com.sun.identity.tune.common.FileHandler;
import com.sun.identity.tune.constants.DSConstants;
import com.sun.identity.tune.util.AMTuneUtil;
import java.io.File;
import java.util.ResourceBundle;
import java.util.logging.Level;

public class DSConfigInfo implements DSConstants {
    private String dsDirMgrPassword;
    private String dsInstanceDir;
    private String dsHost;
    private String dsPort;
    private String rootSuffix;
    private String dirMgrUid;
    private String dsVersion;
    private String dsToolsDir;
    private AMTuneLogger pLogger;
    private MessageWriter mWriter;
    private boolean isRemoteDS;
    public DSConfigInfo(ResourceBundle confBundle, String passFilePath) 
    throws AMTuneException {
        try {
            pLogger = AMTuneLogger.getLoggerInst();
            mWriter = MessageWriter.getInstance();
            setDsHost(confBundle.getString(DS_HOST));
            checkIsDSHostRemote();
            setDsPort(confBundle.getString(DS_PORT));
            setRootSuffix(confBundle.getString(ROOT_SUFFIX));
            setDsInstanceDir(confBundle.getString(DS_INSTANCE_DIR));
            if (!isRemoteDS) {
                if (passFilePath == null || 
                        (passFilePath != null && 
                        !new File(passFilePath).exists())) {
                    mWriter.writelnLocaleMsg("pt-password-file-keys-msg");
                    throw new AMTuneException(AMTuneUtil.getResourceBundle()
                            .getString("pt-error-password-file-not-found"));
                }
                FileHandler pHdl = new FileHandler(passFilePath);
                String reqLine = pHdl.getLine(DIRMGR_PASSWORD);
                if (reqLine == null ||
                        (reqLine != null &&
                        reqLine.trim().length() < DIRMGR_PASSWORD.length() + 
                        1)) {
                    mWriter.writelnLocaleMsg("pt-cannot-proceed");
                    mWriter.writelnLocaleMsg("pt-ds-password-not-found-msg");
                    throw new AMTuneException(AMTuneUtil.getResourceBundle().
                            getString("pt-ds-password-not-found"));
                } else {
                    setDsDirMgrPassword(AMTuneUtil.getLastToken(reqLine, "="));
                }
            }
            setDsVersion(confBundle.getString(DS_VERSION));
            if (getDsVersion().indexOf(DS6_VERSION) != -1) {
                setDSToolsBinDir(confBundle.getString(DS_TOOLS_DIR));
            }
            setDirMgrUid(confBundle.getString(DIRMGR_BIND_DN));
        } catch (Exception ex) {
            throw new AMTuneException(ex.getMessage());
        }
    }

    private void checkIsDSHostRemote() {
        pLogger.log(Level.FINEST, "checkIsDSHostRemote", "DS host is " +
                getDsHost());
        pLogger.log(Level.FINEST, "checkIsDSHostRemote", "Local host is " +
                AMTuneUtil.getHostName());
        if (AMTuneUtil.getHostName().toLowerCase().indexOf(
                getDsHost().substring(0, getDsHost().indexOf('.')).
                toLowerCase()) != -1) {
            isRemoteDS = false;
        } else {
            isRemoteDS = true;
        }
    }
    /**
     * Set Directory Server administrator password.
     * @param dsDirMgrPassword
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setDsDirMgrPassword(String dsDirMgrPassword) 
    throws AMTuneException {
        if (dsDirMgrPassword != null && dsDirMgrPassword.trim().length() > 0) {
            this.dsDirMgrPassword = dsDirMgrPassword.trim();
        } else {
            mWriter.writelnLocaleMsg("pt-ds-password-not-found-msg");
            throw new AMTuneException(AMTuneUtil.getResourceBundle().
                    getString("pt-ds-password-null"));
        }
    }
    
    public String getDsDirMgrPassword() {
        return dsDirMgrPassword;
    }
    
    /**
     * Set directory Server instance Directory.
     * @param dsInstanceDir
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setDsInstanceDir(String dsInstanceDir) 
    throws AMTuneException {
        if (dsInstanceDir != null && dsInstanceDir.trim().length() > 0){
            File dirTest = new File(dsInstanceDir);
            if (!dirTest.isDirectory() && !isRemoteDS) {
                mWriter.writelnLocaleMsg("pt-not-valid-dir");
                AMTuneUtil.printErrorMsg(DS_INSTANCE_DIR);
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-not-valid-dir"));
                
            } else {
                this.dsInstanceDir = dsInstanceDir.trim();
            }
        } else if (!isRemoteDS) {
            mWriter.writeLocaleMsg("pt-inval-val-msg");
            AMTuneUtil.printErrorMsg(DS_INSTANCE_DIR);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-not-valid-dir"));
        }
    }
    
    public String getDsInstanceDir() {
        return dsInstanceDir;
    }
    
    /**
     * Set Directory Server Host name.
     * @param dsHost
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setDsHost(String dsHost) 
    throws AMTuneException {
        if (dsHost != null && dsHost.trim().length() > 0) {
            if (dsHost.indexOf(".") <= 0) {
                AMTuneUtil.printErrorMsg(DS_HOST);
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-fqdn-ds-host-name"));
            }
            this.dsHost = dsHost.trim();
        } else {
            mWriter.writeLocaleMsg("pt-inval-val-msg");
            AMTuneUtil.printErrorMsg(DS_HOST);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-ds-host"));
        }
    }
    
    public String getDsHost() {
        return dsHost;
    }
    
    /**
     * Set Directory server port.
     * @param dsPort
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setDsPort(String dsPort) 
    throws AMTuneException {
        if (dsPort != null && dsPort.trim().length() > 0) {
            try {
                Integer.parseInt(dsPort.trim());
            } catch (NumberFormatException ne) {
                AMTuneUtil.printErrorMsg(DS_PORT);
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-invalid-port-no"));
            }
            this.dsPort = dsPort.trim();
        } else {
            mWriter.writeLocaleMsg("pt-inval-val-msg");
            AMTuneUtil.printErrorMsg(DS_PORT);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-ds-port"));
        }
    }
    
    public String getDsPort() {
        return dsPort;
    }
    
    /**
     * Set Root suffix.
     * @param rootSuffix
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setRootSuffix(String rootSuffix) 
    throws AMTuneException {
        if (rootSuffix != null && rootSuffix.trim().length() > 0) {
            this.rootSuffix = rootSuffix.trim();
        } else {
            mWriter.writeLocaleMsg("pt-inval-val-msg");
            AMTuneUtil.printErrorMsg(ROOT_SUFFIX);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-ds-root-suffix"));
        }
    }
    
    public String getRootSuffix() {
        return rootSuffix;
    }
    
    /**
     * Set Directory server Manager UID.
     * @param dirMgrUid
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setDirMgrUid(String dirMgrUid) 
    throws AMTuneException {
        if (dirMgrUid != null && dirMgrUid.trim().length() > 0) {
            this.dirMgrUid = dirMgrUid.trim();
        } else {
            mWriter.writeLocaleMsg("pt-inval-val-msg");
            AMTuneUtil.printErrorMsg(DIRMGR_BIND_DN);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-ds-bind-dn"));
        }
    }
    public String getDirMgrUid() {
        return dirMgrUid;
    }
    
    /**
     * Set Directory server version.
     * @param dsVersion
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setDsVersion(String dsVersion) 
    throws AMTuneException { 
        if (dsVersion != null && dsVersion.trim().length() > 0) {
            this.dsVersion = dsVersion.trim();
        } else {
            mWriter.writeLocaleMsg("pt-inval-val-msg");
            AMTuneUtil.printErrorMsg(DS_VERSION);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-ds-version"));
        }
    }
    
    public String getDsVersion() {
        return dsVersion;
    }
    
    /**
     * Set DSEE 6.X bin directory.
     * @param dsToolsBinDir
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setDSToolsBinDir(String dsToolsDir) 
    throws AMTuneException {
        if (dsToolsDir != null && dsToolsDir.trim().length() > 0) {
            File dir = new File(dsToolsDir);
            if (!dir.isDirectory() && !isRemoteDS) {
                mWriter.writelnLocaleMsg("pt-not-valid-dir");
                AMTuneUtil.printErrorMsg(DS_TOOLS_DIR);
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-not-valid-dir"));
            } else {
            	this.dsToolsDir = dsToolsDir.trim();
            }
        } else if (!isRemoteDS) {
            mWriter.writeLocaleMsg("pt-inval-val-msg");
            AMTuneUtil.printErrorMsg(DS_TOOLS_DIR);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-ds-tools-dir"));
        }
    }
    
    public String getDSToolsBinDir() {
        return dsToolsDir;
    }
    
    public boolean isRemoteDS() {
        return isRemoteDS;
    }
}
