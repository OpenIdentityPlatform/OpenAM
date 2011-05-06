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
 * $Id: TuneLinuxOS.java,v 1.5 2009/08/21 02:04:46 ykwon Exp $
 */

package com.sun.identity.tune.impl;

import com.sun.identity.tune.common.FileHandler;
import com.sun.identity.tune.common.MessageWriter;
import com.sun.identity.tune.common.AMTuneException;
import com.sun.identity.tune.common.AMTuneLogger;
import com.sun.identity.tune.config.AMTuneConfigInfo;
import com.sun.identity.tune.intr.TuneOS;
import com.sun.identity.tune.util.AMTuneUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;


public class TuneLinuxOS extends TuneOS {
    
    private AMTuneConfigInfo confInfo;
    private AMTuneLogger pLogger;
    private MessageWriter mWriter;
    
    public void initialize(AMTuneConfigInfo configInfo) 
    throws AMTuneException {
        this.confInfo = configInfo;
        pLogger = AMTuneLogger.getLoggerInst();
        mWriter = MessageWriter.getInstance();
    }
    
    public void startTuning() 
    throws AMTuneException {
        try {
            mWriter.writelnLocaleMsg("pt-lnx-tuning");
            tuneKernel();
            tuneRcLocal();
            tuneSecurityLimits();
            if (!confInfo.isReviewMode()) {
                mWriter.writelnLocaleMsg("pt-lnx-reboot-msg");
            }
            mWriter.writeln(PARA_SEP);
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "startTuning", 
                    "Error tuning linux system.");
            mWriter.writeln(" ");
            mWriter.writeLocaleMsg("pt-error-tuning-msg");
            mWriter.writelnLocaleMsg("pt-os-tuning-error-msg");
            mWriter.writelnLocaleMsg("pt-manual-msg");
            pLogger.logException("startTuning", ex);
        }
    }
    
    protected void tuneKernel()
    throws AMTuneException {
        FileHandler sysOutHdlr = null;
        File outFile = null;
        FileHandler tuneFileFh = null;
        try {
            String tuneFile = "/etc/sysctl.conf";
            List recVals = new ArrayList();
            String recVal;
            tuneFileFh = new FileHandler(tuneFile);
            mWriter.writeln(LINE_SEP);
            mWriter.writelnLocaleMsg("pt-lnx-kernel-tuning-msg");
            mWriter.writeln(" ");
            mWriter.writeLocaleMsg("pt-file");
            mWriter.writeln(tuneFile);
            mWriter.writelnLocaleMsg("pt-param-tuning");
            mWriter.writeln(" ");
            mWriter.writelnLocaleMsg("pt-lnx-file-max");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write(LNX_FILE_MAX + " = ");
            String reqLine = tuneFileFh.getLine(LNX_FILE_MAX);
            mWriter.writeln(AMTuneUtil.getLastToken(reqLine, "="));
            mWriter.writeLocaleMsg("pt-rec-val");
            recVal = LNX_FILE_MAX + " = " + AMTUNE_NUM_FILE_DESCRIPTORS;
            mWriter.writeln(recVal);
            recVals.add(recVal);
            mWriter.writeln(" ");
            
            // Execute sysctl cmd and get all the values into resultbuffer
            String sysCtlCmd = "/sbin/sysctl ";
            StringBuffer rBuff = new StringBuffer();
            int extVal = AMTuneUtil.executeCommand(sysCtlCmd + "-a", rBuff);
            if (extVal != -1) {
                String sysOutTempFile = AMTuneUtil.TMP_DIR + "sysctlout";
                AMTuneUtil.writeResultBufferToTempFile(rBuff, sysOutTempFile);
                sysOutHdlr = new FileHandler(sysOutTempFile);
                outFile = new File(sysOutTempFile);
                mWriter.writelnLocaleMsg("pt-lnx-ip-local-port-range");
                mWriter.writeLocaleMsg("pt-cur-val");
                reqLine = sysOutHdlr.getLine(LNX_IPV4_LOCAL_PORT_RANGE);
                mWriter.writeln(LNX_IPV4_LOCAL_PORT_RANGE + " = " +  
                        AMTuneUtil.getLastToken(reqLine, "=" ));
                mWriter.writeLocaleMsg("pt-rec-val");
                recVal = LNX_IPV4_LOCAL_PORT_RANGE + " = " + 
                        AMTUNE_LINUX_IPV4_LOCAL_PORT_RANGE;
                mWriter.writeln(recVal);
                recVals.add(recVal);
                mWriter.writeln(" ");
                
                mWriter.writelnLocaleMsg("pt-lnx-core-rmem-max-msg");
                mWriter.writeLocaleMsg("pt-cur-val");
                reqLine = sysOutHdlr.getLine(LNX_CORE_RMEM_MAX);
                mWriter.writeln(LNX_CORE_RMEM_MAX + " = " +
                        AMTuneUtil.getLastToken(reqLine, "="));
                mWriter.writeLocaleMsg("pt-rec-val");
                recVal = LNX_CORE_RMEM_MAX + " = " + AMTUNE_LINUX_CORE_RMEM_MAX;
                mWriter.writeln(recVal);
                recVals.add(recVal);
                mWriter.writeln(" ");
                
                mWriter.writelnLocaleMsg("pt-lnx-tcp-rmem-msg");
                mWriter.writeLocaleMsg("pt-cur-val");
                reqLine = sysOutHdlr.getLine(LNX_IPV4_TCP_RMEM);
                mWriter.writeln(LNX_IPV4_TCP_RMEM + " = " +
                        AMTuneUtil.getLastToken(reqLine, "="));
                mWriter.writeLocaleMsg("pt-rec-val");
                recVal = LNX_IPV4_TCP_RMEM + " = " + AMTUNE_LINUX_IPV4_TCP_RMEM;
                mWriter.writeln(recVal);
                recVals.add(recVal);
                mWriter.writeln(" ");
                
                mWriter.writelnLocaleMsg("pt-lnx-tcp-wmem-msg");
                mWriter.writeLocaleMsg("pt-cur-val");
                reqLine = sysOutHdlr.getLine(LNX_IPV4_TCP_WMEM);
                mWriter.writeln(LNX_IPV4_TCP_WMEM + " = " + 
                        AMTuneUtil.getLastToken(reqLine, "="));
                mWriter.writeLocaleMsg("pt-rec-val");
                recVal = LNX_IPV4_TCP_WMEM + " = " + AMTUNE_LINUX_IPV4_TCP_WMEM;
                mWriter.writeln(recVal);
                recVals.add(recVal);
                mWriter.writeln(" ");
                
                mWriter.writelnLocaleMsg("pt-lnx-tcp-sack-msg");
                mWriter.writeLocaleMsg("pt-cur-val");
                reqLine = sysOutHdlr.getLine(LNX_IPV4_TCP_SACK);
                mWriter.writeln(LNX_IPV4_TCP_SACK + " = " +
                        AMTuneUtil.getLastToken(reqLine, "="));
                mWriter.writeLocaleMsg("pt-rec-val");
                recVal = LNX_IPV4_TCP_SACK + " = " + AMTUNE_LINUX_IPV4_TCP_SACK;
                mWriter.writeln(recVal);
                recVals.add(recVal);
                mWriter.writeln(" ");
                
                mWriter.writelnLocaleMsg("pt-lnx-tcp-timestamps-msg");
                mWriter.writeLocaleMsg("pt-cur-val");
                reqLine = sysOutHdlr.getLine(LNX_IPV4_TCP_TIMESTAMPS);
                mWriter.writeln(LNX_IPV4_TCP_TIMESTAMPS + " = " + 
                        AMTuneUtil.getLastToken(reqLine, "="));
                mWriter.writeLocaleMsg("pt-rec-val");
                recVal = LNX_IPV4_TCP_TIMESTAMPS + " = " + 
                        AMTUNE_LINUX_IPV4_TCP_TIMESTAMPS;
                mWriter.writeln(recVal);
                recVals.add(recVal);
                mWriter.writeln(" ");
                
                mWriter.writelnLocaleMsg("pt-lnx-tcp-window-scaling-msg");
                mWriter.writeLocaleMsg("pt-cur-val");
                reqLine = sysOutHdlr.getLine(LNX_IPV4_TCP_WINDOW_SCALING);
                mWriter.writeln(LNX_IPV4_TCP_WINDOW_SCALING + " = " +
                        AMTuneUtil.getLastToken(reqLine, "="));
                mWriter.writeLocaleMsg("pt-rec-val");
                recVal = LNX_IPV4_TCP_WINDOW_SCALING + " = " +
                        AMTUNE_LINUX_IPV4_TCP_WIN_SCALE;
                mWriter.writeln(recVal);
                recVals.add(recVal);
                mWriter.writeln(" ");
                
                mWriter.writelnLocaleMsg("pt-lnx-tcp-keepalive-time-msg");
                mWriter.writeLocaleMsg("pt-cur-val");
                reqLine = sysOutHdlr.getLine(LNX_IPV4_TCP_KEEPALIVE_TIME);
                mWriter.writeln(LNX_IPV4_TCP_KEEPALIVE_TIME + " = " +
                        AMTuneUtil.getLastToken(reqLine, "="));
                mWriter.writeLocaleMsg("pt-rec-val");
                recVal = LNX_IPV4_TCP_KEEPALIVE_TIME + " = " +
                        AMTUNE_LINUX_IPV4_TCP_KEEPALIVE_TIME;
                mWriter.writeln(recVal);
                recVals.add(recVal);
                mWriter.writeln(" ");
                
                mWriter.writelnLocaleMsg("pt-lnx-tcp-keepalive-intvl-msg");
                mWriter.writeLocaleMsg("pt-cur-val");
                reqLine = sysOutHdlr.getLine(LNX_IPV4_TCP_KEEPALIVE_INTVL);
                mWriter.writeln(LNX_IPV4_TCP_KEEPALIVE_INTVL + " = " +
                        AMTuneUtil.getLastToken(reqLine, "="));
                mWriter.writeLocaleMsg("pt-rec-val");
                recVal = LNX_IPV4_TCP_KEEPALIVE_INTVL + " = " +
                        AMTUNE_LINUX_IPV4_TCP_KEEPALIVE_INTVL;
                mWriter.writeln(recVal);
                recVals.add(recVal);
                mWriter.writeln(" ");
                
                mWriter.writelnLocaleMsg("pt-lnx-tcp-fin-timeout-msg");
                mWriter.writeLocaleMsg("pt-cur-val");
                reqLine = sysOutHdlr.getLine(LNX_IPV4_TCP_FIN_TIMEOUT);
                mWriter.writeln(LNX_IPV4_TCP_FIN_TIMEOUT + " = " +
                        AMTuneUtil.getLastToken(reqLine, "="));
                mWriter.writeLocaleMsg("pt-rec-val");
                recVal = LNX_IPV4_TCP_FIN_TIMEOUT + " = " +
                        AMTUNE_LINUX_IPV4_TCP_FIN_TIMEOUT;
                mWriter.writeln(recVal);
                recVals.add(recVal);
                mWriter.writeln(" ");
                
            } else {
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-sysctl-cmd"));
            } 
            if (confInfo.isReviewMode()) {
                return;
            }
            String[] delLines = { START_FAM_MSG, LNX_FILE_MAX,
                LNX_IPV4_LOCAL_PORT_RANGE, LNX_CORE_RMEM_MAX, 
                LNX_CORE_RMEM_DEFAULT, LNX_IPV4_TCP_RMEM, LNX_IPV4_TCP_WMEM,
                LNX_IPV4_TCP_SACK, LNX_IPV4_TCP_TIMESTAMPS,
                LNX_IPV4_TCP_WINDOW_SCALING, LNX_IPV4_TCP_KEEPALIVE_TIME,
                LNX_IPV4_TCP_KEEPALIVE_INTVL, LNX_IPV4_TCP_FIN_TIMEOUT,
                END_FAM_MSG};
            AMTuneUtil.backupConfigFile(tuneFile);
            pLogger.log(Level.FINEST, "tuneKernel", 
                    "Removing existing configuration values from " + tuneFile);
            tuneFileFh.removeMatchingLines(delLines);
            tuneFileFh.appendLine("# " + START_FAM_MSG + " " + 
                     AMTuneUtil.getTodayDateStr());
             Iterator itr = recVals.iterator();
             while(itr.hasNext()) {
                 tuneFileFh.appendLine((String)itr.next());
             }
             tuneFileFh.appendLine("# " + END_FAM_MSG + " " + 
                     AMTuneUtil.getTodayDateStr());
             tuneFileFh.close();
             mWriter.writeLocaleMsg("pt-lnx-load-vals-msg");
             mWriter.writeln(" " + tuneFile);
             rBuff.setLength(0);
             extVal = AMTuneUtil.executeCommand(sysCtlCmd + " -p", rBuff);
             if (extVal != -1) {
                 pLogger.log(Level.INFO, "tuneKernel", 
                         "Linux kernel tuning successful.");
             } else {
                 mWriter.writelnLocaleMsg("pt-lnx-kernel-tuning-error-msg");
             }
        } catch(AMTuneException amex) {
            throw amex;
        } catch (Exception ex) {
            pLogger.logException("tuneKernel", ex);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-lnx-kernel-tuning-error-msg"));
        } finally {
            try {
                if (sysOutHdlr != null){
                    sysOutHdlr.close();
                }
                if (outFile != null) {
                    outFile.delete();
                }
                if (tuneFileFh != null) {
                    tuneFileFh.close();
                }
            } catch(Exception ex) {
                pLogger.log(Level.WARNING, "tuneKernel", ex.getMessage());
            }
        }
    }
    
    protected void tuneTCP() 
    throws AMTuneException {
        //nothing to do.       
    }
    
    private void tuneRcLocal() 
    throws AMTuneException {
        try {
            String tuneFile = "/etc/rc.local";
            String recVal ="";
            List recVals = new ArrayList();
            FileHandler fh = new FileHandler(tuneFile);
            mWriter.writeln(LINE_SEP);
            mWriter.writelnLocaleMsg("pt-lnx-boot-script-tuning-msg");
            mWriter.writeln(" ");
            mWriter.writeLocaleMsg("pt-file");
            mWriter.writeln(tuneFile);
            mWriter.writelnLocaleMsg("pt-param-tuning");
            mWriter.writeln(" ");
            mWriter.writeln("1.   " + LNX_TCP_FIN_TIMEOUT_NAME);
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(fh.getLine(LNX_TCP_FIN_TIMEOUT_NAME));
            mWriter.writeLocaleMsg("pt-rec-val");
            recVal = "echo " + AMTUNE_LINUX_IPV4_TCP_FIN_TIMEOUT + 
                    " > " + LNX_TCP_FIN_TIMEOUT_NAME;
            mWriter.writeln(recVal);
            recVals.add(recVal);
            mWriter.writeln(" ");
            
            mWriter.writeln("2.   " + LNX_TCP_KEEPALIVE_TIME_NAME);
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(fh.getLine(LNX_TCP_KEEPALIVE_TIME_NAME));
            mWriter.writeLocaleMsg("pt-rec-val");
            recVal = "echo " + AMTUNE_LINUX_IPV4_TCP_KEEPALIVE_TIME + 
                    " > " + LNX_TCP_KEEPALIVE_TIME_NAME;
            mWriter.writeln(recVal);
            recVals.add(recVal);
            mWriter.writeln(" ");
            
            mWriter.writeln("3.   " + LNX_TCP_KEEPALIVE_INTVL_NAME);
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(fh.getLine(LNX_TCP_KEEPALIVE_INTVL_NAME));
            mWriter.writeLocaleMsg("pt-rec-val");
            recVal = "echo " + AMTUNE_LINUX_IPV4_TCP_KEEPALIVE_INTVL + 
                    " > " + LNX_TCP_KEEPALIVE_INTVL_NAME;
            mWriter.writeln(recVal);
            recVals.add(recVal);
            mWriter.writeln(" ");
            
            mWriter.writeln("4.   " + LNX_TCP_WINDOW_SCALING_NAME);
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(fh.getLine(LNX_TCP_WINDOW_SCALING_NAME));
            mWriter.writeLocaleMsg("pt-rec-val");
            recVal = "echo " + AMTUNE_LINUX_IPV4_TCP_WIN_SCALE + 
                    " > " + LNX_TCP_WINDOW_SCALING_NAME;
            mWriter.writeln(recVal);
            recVals.add(recVal);
            mWriter.writeln(" ");

            mWriter.writeLocaleMsg("pt-lnx-loading-kernel-msg");
            mWriter.writeln(LNX_LOAD_SYSCTL_CMD);
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(fh.getLine(LNX_LOAD_SYSCTL_CMD));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(LNX_LOAD_SYSCTL_CMD);
            recVals.add(LNX_LOAD_SYSCTL_CMD);
            mWriter.writeln(" ");
            
            if (confInfo.isReviewMode()) {
                return;
            }
            AMTuneUtil.backupConfigFile(tuneFile);
            pLogger.log(Level.FINEST, "tuneRcLocal", 
                    "Removing existing configuration from " + tuneFile);
            String delLines[] = {
                START_FAM_MSG, LNX_TCP_FIN_TIMEOUT_NAME, 
                LNX_TCP_KEEPALIVE_TIME_NAME, LNX_TCP_KEEPALIVE_INTVL_NAME,
                LNX_TCP_WINDOW_SCALING_NAME,
                LNX_LOAD_SYSCTL_CMD, END_FAM_MSG
            };
            fh.removeMatchingLines(delLines);
            pLogger.log(Level.FINEST, "tuneRcLocal", 
                    "Adding recomendations to the " + tuneFile);
            fh.appendLine("# " + START_FAM_MSG + " " + 
                    AMTuneUtil.getTodayDateStr());
            Iterator itr = recVals.iterator();
            while (itr.hasNext()) {
                fh.appendLine((String)itr.next());
            }
            fh.appendLine("# " + END_FAM_MSG + " " + 
                    AMTuneUtil.getTodayDateStr());
            fh.close();
        } catch (Exception ex) {
            pLogger.logException("tuneRcLocal", ex);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-lnx-boot-file-error"));
        }
    }
    
    protected void tuneSecurityLimits() 
    throws AMTuneException {
        try {
            String tuneFile = "/etc/security/limits.conf";
            FileHandler tuneFileFh = new FileHandler(tuneFile);
            mWriter.writeln(LINE_SEP);
            mWriter.writelnLocaleMsg("pt-lnx-open-file-msg");
            mWriter.writeln(" ");
            mWriter.writeLocaleMsg("pt-file");
            mWriter.writeln(tuneFile);
            mWriter.writelnLocaleMsg("pt-param-tuning");
            mWriter.writeln(" ");
            String curNoFileSoftDomain = "";
            String newNoFileSoftString = "";
            String[] matLines = tuneFileFh.getMattchingLines("^#", true);
            String[] sofTlines = AMTuneUtil.getMatchedLines(matLines, "soft");
            String[] reqLns = AMTuneUtil.getMatchedLines(sofTlines, "nofile");
            if (reqLns.length > 0) {
                curNoFileSoftDomain = 
                        reqLns[0].substring(0, reqLns[0].indexOf(" ")); 
                newNoFileSoftString = curNoFileSoftDomain + 
                        "               soft    nofile          " +
                        AMTUNE_NUM_FILE_DESCRIPTORS;
            } else {
                newNoFileSoftString = "*               soft    nofile        " +
                        "  " + AMTUNE_NUM_FILE_DESCRIPTORS;
            }
            String[] hardLines = AMTuneUtil.getMatchedLines(matLines, "hard");
            String[] noFLines = 
                    AMTuneUtil.getMatchedLines(hardLines, "nofile");
            String curNoFileHardDomain = "";
            String newNoFileHardString = "";
            if (noFLines.length > 0) {
                curNoFileHardDomain = 
                        noFLines[0].substring(0,noFLines[0].indexOf(" "));
                newNoFileHardString = curNoFileHardDomain.trim() + 
                        "               hard    nofile          " +
                        AMTUNE_NUM_FILE_DESCRIPTORS;
            } else {
                newNoFileHardString = "*               hard    nofile        " +
                        "  " + AMTUNE_NUM_FILE_DESCRIPTORS;
            }
            mWriter.writelnLocaleMsg("pt-lnx-soft-file-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            if (reqLns.length > 0) {
                mWriter.writeln(reqLns[0]);
            } else {
                mWriter.writeln(" ");
            }
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(newNoFileSoftString);
            mWriter.writeln(" ");
            
            mWriter.writelnLocaleMsg("pt-lnx-hard-file-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            if (noFLines.length > 0) {
                mWriter.writeln(noFLines[0]);
            } else {
                mWriter.writeln(" ");
            }
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(newNoFileHardString);
            mWriter.writeln(" ");
            if (confInfo.isReviewMode()) {
                return;
            }
            AMTuneUtil.backupConfigFile(tuneFile);
            pLogger.log(Level.FINEST, "tuneSecurityLimits", 
                    "Deleting configuration from " + tuneFile);
            String[] delLines = { START_FAM_MSG, END_FAM_MSG };
            tuneFileFh.removeMatchingLines(delLines);
            if (reqLns.length > 0) {
                tuneFileFh.removeMatchingLines(reqLns);
            }
            if (noFLines.length > 0) {
                tuneFileFh.removeMatchingLines(noFLines);
            }
            pLogger.log(Level.FINEST, "tuneSecurityLimits", 
                    "Modifying configuration file " + tuneFile);
            tuneFileFh.appendLine("# " + START_FAM_MSG + " " +
                    AMTuneUtil.getTodayDateStr());
            tuneFileFh.appendLine(newNoFileSoftString);
            tuneFileFh.appendLine(newNoFileHardString);
            tuneFileFh.appendLine("# " + END_FAM_MSG + " " +
                    AMTuneUtil.getTodayDateStr());
            tuneFileFh.close();
        } catch (Exception ex) {
            pLogger.logException("tuneSecurityLimits", ex);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-lnx-limits-conf-error"));
        }
    }
}
