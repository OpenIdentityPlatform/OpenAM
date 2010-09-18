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
 * $Id: AMTune.java,v 1.7 2008/08/29 09:56:10 kanduls Exp $
 */

package com.sun.identity.tune;

import com.sun.identity.tune.common.AMTuneException;
import com.sun.identity.tune.common.MessageWriter;
import com.sun.identity.tune.common.AMTuneLogger;
import com.sun.identity.tune.config.AMTuneConfigInfo;
import com.sun.identity.tune.constants.AMTuneConstants;
import com.sun.identity.tune.constants.DSConstants;
import com.sun.identity.tune.constants.WebContainerConstants;
import com.sun.identity.tune.impl.TuneAS9Container;
import com.sun.identity.tune.impl.TuneDS5Impl;
import com.sun.identity.tune.impl.TuneDS6Impl;
import com.sun.identity.tune.impl.TuneFAM8Impl;
import com.sun.identity.tune.impl.TuneLinuxOS;
import com.sun.identity.tune.impl.TuneSolarisOS;
import com.sun.identity.tune.impl.TuneWS7Container;
import com.sun.identity.tune.intr.Tuning;
import com.sun.identity.tune.util.AMTuneUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * This is a main class which invokes DS, OpenSSO and Web container 
 * tuners based on the options set in the amtune-env.properties.
 */
public class AMTune {
        static AMTuneLogger pLogger = null;
        static MessageWriter mWriter = null;
    public static void main(String args[]) {
        AMTuneConfigInfo confInfo = null;
        try {
            String confFilePath = AMTuneConstants.ENV_FILE_NAME;
            String passFilePath = null;
            pLogger = AMTuneLogger.getLoggerInst();
            mWriter = MessageWriter.getInstance();
            AMTuneLogger.setLogLevel("FINEST");
            if (args.length == 1) {
                File passFile = new File(args[0]);
                if (passFile.exists()) {
                    AMTuneUtil.validatePwdFilePermissions(args[0]);
                    passFilePath = args[0];
                } else if (args[0].indexOf(AMTuneConstants.CMD_OPTION2) != -1) {
                    AMTuneLogger.setLogLevel(
                            AMTuneUtil.getLastToken(args[0], "="));
                } else if(args[0].indexOf("--help") != -1 || 
                        args[0].indexOf("-?") != -1) {
                    mWriter.writelnLocaleMsg("pt-usage");
                    System.exit(0);
                } else {
                    mWriter.writelnLocaleMsg("pt-usage");
                    System.exit(0);
                }
            }
            if (args.length == 2 && 
                    args[1].indexOf(AMTuneConstants.CMD_OPTION2) != -1) {
                    AMTuneLogger.setLogLevel(
                            AMTuneUtil.getLastToken(args[1], "="));
            }
            mWriter.writeln(AMTuneConstants.PARA_SEP);
            mWriter.writeln("Error log file : " + pLogger.getLogFilePath());
            mWriter.writeln("Configuration information file : " + 
                    mWriter.getConfigurationFilePath());
            mWriter.writeln(AMTuneConstants.PARA_SEP);
             //init utils
            AMTuneUtil.initializeUtil();
            confInfo = new AMTuneConfigInfo(confFilePath, passFilePath);
            List tunerList = getTuners(confInfo);
            Iterator itr = tunerList.iterator();
            while (itr.hasNext()) {
                Tuning compTuner = (Tuning)itr.next();
                compTuner.initialize(confInfo);
                compTuner.startTuning();
            }
        } catch (Exception ex) {
           
            if (pLogger != null) {
                pLogger.log(Level.SEVERE, "main", ex.getMessage());
                pLogger.logException("main", ex);
            } else {
                ex.printStackTrace();
            }
            if (mWriter != null) {
                mWriter.writeln(" ");
                mWriter.writeLocaleMsg("pt-error-tuning-msg");
                mWriter.writeln(ex.getMessage());
            } else {
                System.out.println("Error occured while tuning: " +
                        ex.getMessage());
            }
        } finally {
            if (pLogger != null) {
                pLogger.close();
            }
            if (mWriter != null) {
                mWriter.close();
            }
        }
    }

    /**
     * Factory method which creates component tuners for tuning.
     * @param confInfo
     * @return List of tuner objects.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private static List getTuners(AMTuneConfigInfo confInfo)
    throws AMTuneException {
        List tunerList = new ArrayList();
        if (confInfo.isTuneOS()) {
            if (AMTuneUtil.isSunOs()) {
                tunerList.add(new TuneSolarisOS());
            } else if (AMTuneUtil.isLinux()) {
                tunerList.add(new TuneLinuxOS());
            }  else {
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-unsupported-os-tuning"));
            }
        }
        if (confInfo.isTuneDS()) {
            if (confInfo.getDSConfigInfo().isRemoteDS()) {
                AMTuneUtil.createRemoteDSTuningZipFile(confInfo);
            } else {
                String dsVersion = confInfo.getDSConfigInfo().getDsVersion();
                if (AMTuneUtil.isSupportedUMDSVersion(dsVersion)) {
                    if (dsVersion.indexOf(DSConstants.DS5_VERSION) != -1) {
                        tunerList.add(new TuneDS5Impl());
                    } else if (dsVersion.indexOf(
                            DSConstants.DS6_VERSION) != -1) {
                        tunerList.add(new TuneDS6Impl());
                    }
                } else {
                    throw new AMTuneException(AMTuneUtil.getResourceBundle()
                            .getString("pt-ds-unsupported-msg"));
                }
            }
        }
        if (confInfo.isTuneWebContainer()) {
            if (confInfo.getWebContainer().equalsIgnoreCase(
                    WebContainerConstants.WS7_CONTAINER)) {
                tunerList.add(new TuneWS7Container());
            } else if (confInfo.getWebContainer().equalsIgnoreCase(
                    WebContainerConstants.AS91_CONTAINER)) {
                tunerList.add(new TuneAS9Container());
            } else {
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-unsupported-wc-tuning"));
            }
        }
        if (confInfo.isTuneFAM()) {
            tunerList.add(new TuneFAM8Impl());
        }
        return tunerList;
    }
}
