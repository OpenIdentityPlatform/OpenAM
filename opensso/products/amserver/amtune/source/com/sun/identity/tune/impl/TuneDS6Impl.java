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
 * $Id: TuneDS6Impl.java,v 1.4 2008/08/29 10:25:40 kanduls Exp $
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.tune.impl;

import com.sun.identity.tune.base.AMTuneDSBase;
import com.sun.identity.tune.common.FileHandler;
import com.sun.identity.tune.common.AMTuneException;
import com.sun.identity.tune.config.AMTuneConfigInfo;
import com.sun.identity.tune.util.AMTuneUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;

/**
 * <code>TuneDS6Impl<\code> extends the <code>AMTuneDSBase<\code> and tunes
 * the Directory server 6.0
 *
 */
public class TuneDS6Impl extends AMTuneDSBase {
    private String dsAdmPath;
    private String dbBackupDir;
    
    public TuneDS6Impl() {
    }
    
    /**
     * Initializes the configuration information.
     *
     * @param confInfo Configuration information used for computing the tuning
     *   parameters for Directory server.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public void initialize(AMTuneConfigInfo configInfo)
    throws AMTuneException {
        super.initialize(configInfo);
        if (AMTuneUtil.isWindows()) {
            dsAdmPath = dsConfInfo.getDSToolsBinDir() + FILE_SEP + "dsadm.exe ";
        } else {
            dsAdmPath = dsConfInfo.getDSToolsBinDir() + FILE_SEP + "dsadm ";
        }
        validateDSADMPath();
        dbBackupDir = DB_BACKUP_DIR_PREFIX + "-" + AMTuneUtil.getRandomStr();
        checkDSRealVersion();
    }
    
    /**
     * Validates Directory server tools path
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void validateDSADMPath() 
    throws AMTuneException {
        File dsadm = new File(dsAdmPath.trim());
            if (!dsadm.exists()) {
                AMTuneUtil.printErrorMsg(DS_TOOLS_DIR);
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-ds-tool-not-found"));
            }
        }
    
    /**
     * Executes dsadm command to fine real version of the DS.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void checkDSRealVersion()
    throws AMTuneException {
        String verCmd = dsAdmPath + " --version";
        StringBuffer rBuff = new StringBuffer();
        int extVal = AMTuneUtil.executeCommand(verCmd, rBuff);
        if (extVal == -1) {
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-ds-version-fail-msg"));
        } else {
            if (rBuff.indexOf(DS62_VERSION) != -1) {
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-ds-unsupported-msg"));
            }
        }
    }
    
    /**
     * This method performs the sequence of operations for tuning
     * Directory server 6.0.
     *
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public void startTuning()
    throws AMTuneException {
        try {
            pLogger.log(Level.FINE, "startTuning","Start tuning.");
            mWriter.writeln(CHAPTER_SEP);
            mWriter.writelnLocaleMsg("pt-fam-ds6-tuning");
            mWriter.writeln(CHAPTER_SEP);
            mWriter.writelnLocaleMsg("pt-init");
            mWriter.writeln(LINE_SEP);
            computeTuneValues();
            mWriter.writeln(PARA_SEP);
            modifyLDAP();
            if ( !AMTuneUtil.isWindows()) {
                //For Windows changing dse is not recommended.
                tuneUsingDSE();
            }
            tuneFuture();
            mWriter.writelnLocaleMsg("pt-ds-um-mutliple-msg");
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "startTuning", "Error Tuning DSEE6.0");
            mWriter.writeln(" ");
            mWriter.writelnLocaleMsg("pt-error-tuning-msg");
            pLogger.logException("startTuning", ex);
        } finally {
            try {
                releaseCon();
            } catch (Exception ex) {
            //ignore
            }
            deletePasswordFile();
        }
    }

    /**
     * This method modify the DB home location in dse.ldif to point to
     * new location.
     *
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected void tuneUsingDSE()
    throws AMTuneException {
        try {
            super.tuneUsingDSE();
            pLogger.log(Level.FINE, "tuneUsingDSE", "");
            if (configInfo.isReviewMode()) {
                return;
            }
            pLogger.log(Level.FINE, "tuneUsingDSE", "Modify dse.ldif");
            if (curDBHomeLocation.equals(newDBHomeLocation)) {
                pLogger.log(Level.INFO, "tuneUsingDSE",
                        "Current DB Location is " +
                        "same as recommended value.");
                return;
            }
            stopDS();
            backUpDS();
            FileHandler dseH = new FileHandler(dseLdifPath);
            int reqLineNo = dseH.lineContains(NSSLAPD_DB_HOME_DIRECTORY + ":");
            dseH.replaceLine(reqLineNo, NSSLAPD_DB_HOME_DIRECTORY + ": " +
                    newDBHomeLocation);
            dseH.close();
            startDS();
        } catch (FileNotFoundException fex) {
            throw new AMTuneException(fex.getMessage());
        } catch (IOException ioe) {
            throw new AMTuneException(ioe.getMessage());
        }
    }

    /**
     * This method computes the recommended values for tuning LDAP and apply
     * the modifications to the LDAP.
     *
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void modifyLDAP()
    throws AMTuneException{
        pLogger.log(Level.FINE, "modifyLDAP", "Modify LDAP attributes.");
        boolean applyRec = false;
        boolean remAci = false;
        ldapTuningRecommendations();
        if(configInfo.isReviewMode()) {
            return;
        }
        stopDS();
        backUpDS();
        startDS();
        applyRec = applyRecommendations();
        if (applyRec || remAci) {
            stopDS();
            startDS();
        }
    }

    /**
     * Takes the backup of the DS 6.0 by invoking dsadm backup.
     *
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected void  backUpDS()
    throws AMTuneException {
        try {
            String dbBackUpSuccessFile = instanceDir +
                    FILE_SEP + "bak" + FILE_SEP + dbBackupDir +
                    FILE_SEP + "SUCCESS.dontdelete";
            File successFile = new File(dbBackUpSuccessFile);
            if (successFile.isFile()) {
                pLogger.log(Level.INFO, "backUpDS", "Backup exists");
                return;
            }
            File bakDir = new File(dsConfInfo.getDsInstanceDir() +
                    FILE_SEP + "bak");
            if (!bakDir.isDirectory()) {
                bakDir.mkdir();
            }
            StringBuffer resultBuffer = new StringBuffer();
            String db2BakCmd = dsAdmPath + "backup " + instanceDir + " " +
                    bakDir.getAbsolutePath() + FILE_SEP + dbBackupDir;
            pLogger.log(Level.FINE, "backUpDS", "Backing up DS instance." +
                    instanceDir);
            int retVal = AMTuneUtil.executeCommand(db2BakCmd, resultBuffer);
            if (retVal == -1) {
                mWriter.writelnLocaleMsg("pt-cannot-backup-db");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-ds-db-backup-failed"));
            }
            pLogger.log(Level.FINE, "backUpDS", "Backing up Done...");
            try {
                File dseLdif = new File(dseLdifPath);
                pLogger.log(Level.FINE, "backUpDS", "Backing " + dseLdifPath);
                File bakDseFile = new File(instanceDir +
                        FILE_SEP + "bak" + FILE_SEP + dbBackupDir +
                        FILE_SEP + "dse.ldif");
                AMTuneUtil.CopyFile(dseLdif, bakDseFile);
                pLogger.log(Level.FINE, "backUpDS", "Backing Done..");
            } catch (Exception ex) {
                pLogger.log(Level.SEVERE, "backupDS", ex.getMessage());
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-ds-conf-file-backup"));
            }
            successFile.createNewFile();
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "backUpDS",
                    "Error backing up DS " + ex.getMessage());
            throw new AMTuneException(ex.getMessage());
        }
    }

    /**
     * Stops the Directory server using dsamd stop
     *
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected void stopDS()
    throws AMTuneException {
        pLogger.log(Level.FINE, "stopDS", "Stopping DS6.");
        StringBuffer resultBuffer = new StringBuffer();
        String stopCmd = dsAdmPath + "stop " + instanceDir;
        int retVal = AMTuneUtil.executeCommand(stopCmd, resultBuffer);
        if (retVal == -1){
            throw new AMTuneException(resultBuffer.toString());
        }
        pLogger.log(Level.FINE, "stopDS", "DS6 Successfully stopped.");
    }

    /**
     * Starts the Directory server using dsadm start
     *
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected void startDS()
    throws AMTuneException {
        pLogger.log(Level.FINE, "startDS", "Starting DS6.");
        StringBuffer resultBuffer = new StringBuffer();
        String startCmd = dsAdmPath + "start " + instanceDir;
        int retVal = AMTuneUtil.executeCommand(startCmd, resultBuffer);
        if (retVal == -1){
            throw new AMTuneException(resultBuffer.toString());
        }
        pLogger.log(Level.FINE, "startDS", "DS6 Successfully started.");
    }
}
