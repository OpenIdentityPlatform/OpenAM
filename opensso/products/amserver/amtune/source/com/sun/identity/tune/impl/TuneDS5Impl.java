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
 * $Id: TuneDS5Impl.java,v 1.5 2008/08/29 10:25:39 kanduls Exp $
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
 * <code>TuneDS5Impl<\code> extends the <code>AMTuneDSBase<\code> and tunes
 * the Directory server 5.2
 *
 */
public class TuneDS5Impl extends AMTuneDSBase {
    private String db2BakPath;
    private String stopCmdPath;
    private String startCmdPath;
    private String dbBackUpDir;

    /**
     * Constructs the instance of this class
     */
    public TuneDS5Impl() {
    }

    /**
     * Initializes the configuration information.
     *
     * @param confInfo Configuration information used for computing the tuning
     *   parameters for Directory server.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public void initialize(AMTuneConfigInfo confInfo)
    throws AMTuneException {
        super.initialize(confInfo);
        if (AMTuneUtil.isWindows()) {
            db2BakPath = instanceDir + FILE_SEP + "db2bak.bat ";
            stopCmdPath = instanceDir + FILE_SEP + "stop-slapd.bat";
            startCmdPath = instanceDir + FILE_SEP + "start-slapd.bat";
        } else {
            db2BakPath = instanceDir + FILE_SEP + "db2bak ";
            stopCmdPath = instanceDir + FILE_SEP + "stop-slapd";
            startCmdPath = instanceDir + FILE_SEP + "start-slapd";
        }
        validateAdminToolPath();
        dbBackUpDir = DB_BACKUP_DIR_PREFIX + "-" + AMTuneUtil.getRandomStr();
    }
    
    protected void validateAdminToolPath() 
    throws AMTuneException {
        File db2bakFile = new File(db2BakPath.trim());
        if (!db2bakFile.exists()) {
            mWriter.writelnLocaleMsg("pt-error-ds-tool-not-found");
            AMTuneUtil.printErrorMsg(DS_INSTANCE_DIR);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-invalid-ds-instance-dir"));
        }
    }

    /**
     * This method performs the sequence of operations for tuning
     * Directory server 5.2.
     *
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public void startTuning()
    throws AMTuneException {
        try {
            pLogger.log(Level.FINE, "startTuning","Start tuning.");
            mWriter.writeln(CHAPTER_SEP);
            mWriter.writelnLocaleMsg("pt-fam-ds-tuning");
            mWriter.writeln(CHAPTER_SEP);
            mWriter.writelnLocaleMsg("pt-init");
            mWriter.writeln(LINE_SEP);
            computeTuneValues();
            mWriter.writeln(PARA_SEP);
            modifyLDAP();
            if ( !AMTuneUtil.isWindows()) {
                //For Windows changing dse with this script is not recommended.
                tuneUsingDSE();
            }
            tuneFuture();
            mWriter.writelnLocaleMsg("pt-ds-um-mutliple-msg");
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "startTuning", "Error tuning DS5.2");
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
     * This method modifys the DB home location in dse.ldif to point to
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
     * This method computes the recommended values for tuning LDAP and applys
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
     * Takes the backup of the DS 5.2 by invoking db2bak.bat
     *
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected void  backUpDS()
    throws AMTuneException {
        try {
            String dbBackUpSuccessFile = instanceDir + FILE_SEP + "bak" +
                    FILE_SEP + dbBackUpDir + FILE_SEP +
                    "SUCCESS.dontdelete";
            File successFile = new File(dbBackUpSuccessFile);
            if (successFile.isFile()) {
                pLogger.log(Level.INFO, "backUpDS", "Backup exists");
                return;
            }
            File bakDir = new File(instanceDir + FILE_SEP + "bak");
            if (!bakDir.isDirectory()) {
                bakDir.mkdir();
            }
            StringBuffer resultBuffer = new StringBuffer();
            String db2BakCmd = db2BakPath +  bakDir.getAbsolutePath() +
                    FILE_SEP + dbBackUpDir;
            pLogger.log(Level.FINE, "backUpDS", "Backing up DS.");
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
                File bakDseFile = new File(instanceDir + FILE_SEP +
                        "bak" + FILE_SEP + dbBackUpDir + FILE_SEP +
                        "dse.ldif");
                AMTuneUtil.CopyFile(dseLdif, bakDseFile);
                pLogger.log(Level.FINE, "backUpDS", "Backing Done..");
            } catch (Exception ex) {
                pLogger.log(Level.SEVERE, "backupDS", ex.getMessage());
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-ds-conf-file-backup"));
            }
            successFile.createNewFile();
        } catch (Exception ex) {
            throw new AMTuneException(ex.getMessage());
        }
    }

    /**
     * Stops the Directory Server 5.2 using stop-slapd.bat
     *
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected void stopDS()
    throws AMTuneException {
        pLogger.log(Level.FINE, "stopDS", "Stopping DS.");
        StringBuffer resultBuffer = new StringBuffer();
        int retVal = AMTuneUtil.executeCommand(stopCmdPath, resultBuffer);
        if (retVal == -1){
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-ds-stop"));
        }
        pLogger.log(Level.FINE, "stopDS", "DS Successfully stopped.");
    }

    /**
     * Stops the Directory Server 5.2 using start-slapd.bat
     *
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected void startDS()
    throws AMTuneException {
        pLogger.log(Level.FINE, "startDS", "Starting DS.");
        StringBuffer resultBuffer = new StringBuffer();
        int retVal = AMTuneUtil.executeCommand(startCmdPath, resultBuffer);
        if (retVal == -1){
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-ds-start"));
        }
        pLogger.log(Level.FINE, "startDS", "DS Successfully started.");
    }
}
