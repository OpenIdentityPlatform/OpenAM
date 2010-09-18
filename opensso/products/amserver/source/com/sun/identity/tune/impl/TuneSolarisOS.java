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
 * $Id: TuneSolarisOS.java,v 1.6 2009/12/09 00:41:04 ykwon Exp $
 */

package com.sun.identity.tune.impl;

import com.sun.identity.tune.common.FileHandler;
import com.sun.identity.tune.common.MessageWriter;
import com.sun.identity.tune.common.AMTuneException;
import com.sun.identity.tune.common.AMTuneLogger;
import com.sun.identity.tune.config.AMTuneConfigInfo;
import com.sun.identity.tune.intr.TuneOS;
import com.sun.identity.tune.util.AMTuneUtil;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 *  This class tunes Solaris operating system Kernel and TCP parameters.
 */
public class TuneSolarisOS extends TuneOS {
    private AMTuneConfigInfo confInfo;
    private AMTuneLogger pLogger;
    private MessageWriter mWriter;
    private String osVersion;
    private String nddCmd;
    private static String TCP_DIV = "/dev/tcp ";
    
    /**
     * Initializes the configuration information.
     * @param configInfo Configuration information
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public void initialize(AMTuneConfigInfo configInfo) 
    throws AMTuneException {
        this.confInfo = configInfo;
        pLogger = AMTuneLogger.getLoggerInst();
        mWriter = MessageWriter.getInstance();
        osVersion = System.getProperty("os.version");
        nddCmd = FILE_SEP + "usr" + FILE_SEP + "sbin" + FILE_SEP + "ndd ";
    }
    
    /**
     * This method invokes other helper methods for performing tuning.
     * 
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public void startTuning() 
    throws AMTuneException {
        try {
            boolean globalZone = true;
            if (osVersion.equals("5.10")) {
                String cmd = "/usr/sbin/zoneadm list";
                StringBuffer rBuf = new StringBuffer();
                int extVal = AMTuneUtil.executeCommand(cmd, rBuf);
                if (extVal == -1) {
                    mWriter.writelnLocaleMsg("pt-sol-error-tuning");
                    throw new AMTuneException(AMTuneUtil.getResourceBundle()
                            .getString("pt-error-zonadm"));
                } else if (rBuf.toString().toLowerCase().indexOf("global") !=
                        -1) {
                    globalZone = true;
                } else {
                    mWriter.writeln(CHAPTER_SEP);
                    mWriter.writelnLocaleMsg("pt-non-global-zone-msg");
                    mWriter.writeln(CHAPTER_SEP);
                    globalZone = false;
                }
            }
            if (globalZone) {
                mWriter.writelnLocaleMsg("pt-sol-tuning-msg");
                mWriter.writeln(LINE_SEP);
                tuneKernel();
                tuneTCP();
                if (!confInfo.isReviewMode()) {
                    mWriter.writelnLocaleMsg("pt-lnx-reboot-msg");
                }
                mWriter.writeln(PARA_SEP);
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "startTuning",
                    "Error tuning Solaris operating system.");
            mWriter.writeln(" ");
            mWriter.writeLocaleMsg("pt-error-tuning-msg");
            mWriter.writelnLocaleMsg("pt-os-tuning-error-msg");
            mWriter.writelnLocaleMsg("pt-manual-msg");
            pLogger.logException("startTuning", ex);
        }
    }
    
    /**
     * This method modify the system file with recommended file descriptor 
     * values
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected void tuneKernel() 
    throws AMTuneException {
        FileHandler fh = null;
        try {
            String tuneFile = FILE_SEP + "etc" + FILE_SEP + "system";
            fh = new FileHandler (tuneFile);
           
            mWriter.writelnLocaleMsg("pt-sol-kernel-tuning");
            mWriter.writeln(" ");
            mWriter.writeLocaleMsg("pt-file");
            mWriter.writeln(tuneFile);
            mWriter.writelnLocaleMsg("pt-param-tuning");
            mWriter.writeln(" ");
            mWriter.writelnLocaleMsg("pt-sol-fd-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write(RLIM_FD_MAX + "=");
            String reqLine = fh.getLine(RLIM_FD_MAX);
            if (reqLine != null && reqLine.trim().length() > 0) {
                mWriter.writeln(AMTuneUtil.getLastToken(reqLine, "="));
            } else {
                mWriter.writeln(" ");
            }
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.write(RLIM_FD_MAX + "=");
            mWriter.writelnLocaleMsg(AMTUNE_NUM_FILE_DESCRIPTORS + " ");
            
            mWriter.writelnLocaleMsg("pt-sol-fd-cur-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write(RLIM_FD_CUR + "=");
            reqLine = fh.getLine(RLIM_FD_CUR);
            if (reqLine != null && reqLine.trim().length() > 0) {
                mWriter.writeln(AMTuneUtil.getLastToken(reqLine, "="));
            } else {
                mWriter.writeln(" ");
            }
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.write(RLIM_FD_CUR + "=");
            mWriter.writelnLocaleMsg(AMTUNE_NUM_FILE_DESCRIPTORS + " ");
            
            if (osVersion.equals("5.9")) {
                mWriter.writelnLocaleMsg("pt-sol-tcp-hash-size-msg");
                mWriter.writeln(AMTUNE_NUM_TCP_CONN_SIZE + " ");
                mWriter.writelnLocaleMsg("pt-rec-val");
                mWriter.write(TCP_CON_HASH_SIZE + "=");
                reqLine = fh.getLine(TCP_CON_HASH_SIZE);
                if (reqLine != null && reqLine.trim().length() > 0) {
                    mWriter.writeln(AMTuneUtil.getLastToken(reqLine, "="));
                } else {
                    mWriter.writeln(" ");
                }
                mWriter.writelnLocaleMsg("pt-rec-val");
                mWriter.writeln(TCP_CON_HASH_SIZE + "=");
                mWriter.writeln(AMTUNE_NUM_TCP_CONN_SIZE + " ");
            }
            mWriter.writeln(" ");
            if (confInfo.isReviewMode()) {
                return;
            }
            /**check_file_for_write $tune_file
            if [ $? = 100 ]; then
                return
            fi**/
            AMTuneUtil.backupConfigFile(tuneFile);
            String[] delStrs = { "*" + START_FAM_MSG, TCP_CON_HASH_SIZE,
                RLIM_FD_CUR, RLIM_FD_MAX, "*" + END_FAM_MSG};
            fh.removeMatchingLines(delStrs);
            fh.appendLine("*" + START_FAM_MSG);
            fh.appendLine("set " + RLIM_FD_MAX + "=" + 
                    AMTUNE_NUM_FILE_DESCRIPTORS);
            fh.appendLine("set " + RLIM_FD_CUR + "=" + 
                    AMTUNE_NUM_FILE_DESCRIPTORS);
             if (osVersion.equals("5.9")) {
                 fh.appendLine("set " + TCP_CON_HASH_SIZE + "=" +
                         AMTUNE_NUM_TCP_CONN_SIZE);
             }
            fh.appendLine("*" + END_FAM_MSG);
            fh.close();
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "tuneKernal", 
                    "Error tuning Solaris system file");
            throw new AMTuneException(ex.getMessage());
        } 
    }
    
    /**
     * This method tunes TCP parameters using ndd command.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected void tuneTCP() 
    throws AMTuneException {
        try {
            Map tcpCurCFGMap = getCurTCPVals();
            String tuneFile = "/etc/rc2.d/S71ndd_tcp";
            mWriter.writeln(LINE_SEP);
            mWriter.writelnLocaleMsg("pt-tcp-tuning");
            mWriter.writeln(" ");
            mWriter.writeLocaleMsg("pt-file");
            mWriter.writeln(tuneFile);
            mWriter.writelnLocaleMsg("pt-param-tuning");
            mWriter.writeln(" ");
            mWriter.writelnLocaleMsg("pt-tcp-wait-2-flush-interval");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write(TCP_DIV + SOL_TCP_FIN_WAIT_2_FLUSH_INTERVAL);
            mWriter.writeln(" " + tcpCurCFGMap.get(
                    SOL_TCP_FIN_WAIT_2_FLUSH_INTERVAL));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.write(TCP_DIV + SOL_TCP_FIN_WAIT_2_FLUSH_INTERVAL);
            mWriter.writeln(" " + FLUSH_INTERVAL_VAL);
            mWriter.writeln(" ");
            
            mWriter.writelnLocaleMsg("pt-tcp-conn-req-maz-q");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write(TCP_DIV + SOL_TCP_CONN_REQ_MAX_Q);
            mWriter.writeln(" " + tcpCurCFGMap.get(SOL_TCP_CONN_REQ_MAX_Q));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.write(TCP_DIV + SOL_TCP_CONN_REQ_MAX_Q);
            mWriter.writeln(" " + AMTUNE_NUM_TCP_CONN_SIZE);
            mWriter.writeln(" ");
            
            mWriter.writelnLocaleMsg("pt-tcp-conn-req-max-q0");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write(TCP_DIV + SOL_TCP_CONN_REQ_MAX_Q0);
            mWriter.writeln(" " + tcpCurCFGMap.get(SOL_TCP_CONN_REQ_MAX_Q0));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.write(TCP_DIV + SOL_TCP_CONN_REQ_MAX_Q0);
            mWriter.writeln(" " + AMTUNE_NUM_TCP_CONN_SIZE);
            mWriter.writeln(" ");
            
            mWriter.writelnLocaleMsg("pt-tcp-keepalive-interval");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write(TCP_DIV + SOL_TCP_KEEPALIVE_INTERVAL);
            mWriter.writeln(" " + tcpCurCFGMap.get(SOL_TCP_KEEPALIVE_INTERVAL));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.write(TCP_DIV + SOL_TCP_KEEPALIVE_INTERVAL);
            mWriter.writeln(" " + KEEP_ALIVE_INTERVAL_VAL);
            mWriter.writeln(" ");
            
            mWriter.writelnLocaleMsg("pt-tcp-smallest-anon-port");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write(TCP_DIV + SOL_TCP_SMALLEST_ANON_PORT);
            mWriter.writeln(" " + tcpCurCFGMap.get(SOL_TCP_SMALLEST_ANON_PORT));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.write(TCP_DIV + SOL_TCP_SMALLEST_ANON_PORT);
            mWriter.writeln(" " + ANON_PORT_VAL);
            mWriter.writeln(" ");
            
            mWriter.writelnLocaleMsg("pt-tcp-slow-start-initial");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write(TCP_DIV + SOL_TCP_SLOW_START_INTITIAL);
            mWriter.writeln(" " + 
                    tcpCurCFGMap.get(SOL_TCP_SLOW_START_INTITIAL));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.write(TCP_DIV + SOL_TCP_SLOW_START_INTITIAL);
            mWriter.writeln(" " + SLOW_START_INITIAL_VAL);
            mWriter.writeln(" ");
            
            mWriter.writelnLocaleMsg("pt-tcp-xmit-hiwat");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write(TCP_DIV + SOL_TCP_XMIT_HIWAT);
            mWriter.writeln(" " + tcpCurCFGMap.get(SOL_TCP_XMIT_HIWAT));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.write(TCP_DIV + SOL_TCP_XMIT_HIWAT);
            mWriter.writeln(" " + XMIT_RECV_HIWAT_VAL);
            mWriter.writeln(" ");
            
            mWriter.writelnLocaleMsg("pt-tcp-recv-hiwat");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write(TCP_DIV + SOL_TCP_RECV_HIWAT);
            mWriter.writeln(" " + tcpCurCFGMap.get(SOL_TCP_RECV_HIWAT));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.write(TCP_DIV + SOL_TCP_RECV_HIWAT);
            mWriter.writeln(" " + XMIT_RECV_HIWAT_VAL);
            mWriter.writeln(" ");
            
            mWriter.writelnLocaleMsg("pt-tcp-ip-abort-cinterval");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write(TCP_DIV + SOL_TCP_IP_ABORT_CINTERVAL);
            mWriter.writeln(" " + tcpCurCFGMap.get(SOL_TCP_IP_ABORT_CINTERVAL));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.write(TCP_DIV + SOL_TCP_IP_ABORT_CINTERVAL);
            mWriter.writeln(" " + ABORT_CINTERVAL_VAL);
            mWriter.writeln(" ");
            
            mWriter.writelnLocaleMsg("pt-tcp-deferred-ack-interval");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write(TCP_DIV + SOL_TCP_DEFERRED_ACK_INTERVAL);
            mWriter.writeln(" " + 
                    tcpCurCFGMap.get(SOL_TCP_DEFERRED_ACK_INTERVAL));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.write(TCP_DIV + SOL_TCP_DEFERRED_ACK_INTERVAL);
            mWriter.writeln(" " + ACK_INTERVAL_VAL);
            mWriter.writeln(" ");
            
            mWriter.writelnLocaleMsg("pt-tcp-strong-iss");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write(TCP_DIV + SOL_TCP_STRONG_ISS);
            mWriter.writeln(" " + tcpCurCFGMap.get(SOL_TCP_STRONG_ISS));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.write(TCP_DIV + SOL_TCP_STRONG_ISS);
            mWriter.writeln(" " + STRONG_ISS_VAL);
            mWriter.writeln(" ");

            mWriter.writelnLocaleMsg("pt-tcp-max-buf");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write(TCP_DIV + SOL_TCP_MAX_BUF);
            mWriter.writeln(" " + tcpCurCFGMap.get(SOL_TCP_MAX_BUF));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.write(TCP_DIV + SOL_TCP_MAX_BUF);
            mWriter.writeln(" " + MAX_BUF_CWND_VAL);
            mWriter.writeln(" ");

            mWriter.writelnLocaleMsg("pt-tcp-cwnd-max");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write(TCP_DIV + SOL_TCP_CWND_MAX);
            mWriter.writeln(" " + tcpCurCFGMap.get(SOL_TCP_CWND_MAX));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.write(TCP_DIV + SOL_TCP_CWND_MAX);
            mWriter.writeln(" " + MAX_BUF_CWND_VAL);
            mWriter.writeln(" ");

            if (confInfo.isReviewMode()) {
                return;
            }
            if (new File(tuneFile).exists()) {
                AMTuneUtil.backupConfigFile(tuneFile);
                FileHandler solConfFile = new FileHandler(tuneFile);
                String[] delStrs = { "#" + START_FAM_MSG, 
                    SOL_TCP_TIME_WAIT_INTERVAL, 
                    SOL_TCP_FIN_WAIT_2_FLUSH_INTERVAL,
                    SOL_TCP_CONN_REQ_MAX_Q,
                    SOL_TCP_CONN_REQ_MAX_Q0,
                    SOL_TCP_IP_ABORT_INTERVAL,
                    SOL_TCP_KEEPALIVE_INTERVAL,
                    SOL_TCP_REXMIT_INTERVAL_MAX,
                    SOL_TCP_REXMIT_INTERVAL_MIN,
                    SOL_TCP_REXMIT_INTERVAL_INITIAL,
                    SOL_TCP_SMALLEST_ANON_PORT,
                    SOL_TCP_SLOW_START_INTITIAL,
                    SOL_TCP_XMIT_HIWAT,
                    SOL_TCP_RECV_HIWAT,
                    SOL_TCP_IP_ABORT_CINTERVAL,
                    SOL_TCP_DEFERRED_ACK_INTERVAL,
                    SOL_TCP_STRONG_ISS,
                    SOL_TCP_MAX_BUF,
                    SOL_TCP_CWND_MAX,
                    "#" + END_FAM_MSG
                };
                solConfFile.removeMatchingLines(delStrs);
                solConfFile.close();
            } else {
                //create the file
                new File(tuneFile).createNewFile();
            }
            FileHandler fh = new FileHandler(tuneFile);
            fh.appendLine("#" + START_FAM_MSG);
            String setCmd = nddCmd + "-set " + TCP_DIV;
            fh.appendLine(setCmd + SOL_TCP_FIN_WAIT_2_FLUSH_INTERVAL +
                " " + FLUSH_INTERVAL_VAL);
            fh.appendLine(setCmd + SOL_TCP_CONN_REQ_MAX_Q + " " +
                    AMTUNE_NUM_TCP_CONN_SIZE);
            fh.appendLine(setCmd + SOL_TCP_CONN_REQ_MAX_Q0 + " " +
                    AMTUNE_NUM_TCP_CONN_SIZE);
            fh.appendLine(setCmd + SOL_TCP_KEEPALIVE_INTERVAL + " " +
                    KEEP_ALIVE_INTERVAL_VAL);
            fh.appendLine(setCmd + SOL_TCP_SMALLEST_ANON_PORT + " " +
                    ANON_PORT_VAL);
            fh.appendLine(setCmd + SOL_TCP_SLOW_START_INTITIAL + " " +
                    SLOW_START_INITIAL_VAL);
            fh.appendLine(setCmd + SOL_TCP_XMIT_HIWAT + " " +
                    XMIT_RECV_HIWAT_VAL);
            fh.appendLine(setCmd + SOL_TCP_RECV_HIWAT + " " +
                    XMIT_RECV_HIWAT_VAL);
            fh.appendLine(setCmd + SOL_TCP_IP_ABORT_CINTERVAL + " " +
                    ABORT_CINTERVAL_VAL);
            fh.appendLine(setCmd + SOL_TCP_DEFERRED_ACK_INTERVAL + " " +
                    ACK_INTERVAL_VAL);
            fh.appendLine(setCmd + SOL_TCP_STRONG_ISS + " " + STRONG_ISS_VAL);
            fh.appendLine(setCmd + SOL_TCP_MAX_BUF + " " + MAX_BUF_CWND_VAL);
            fh.appendLine(setCmd + SOL_TCP_CWND_MAX + " " + MAX_BUF_CWND_VAL);
            fh.appendLine("#" + END_FAM_MSG);
            fh.close();
            //source the tcp tune file so that the settings are visible 
            // immediately . $tune_file
            //wrapper file to source system file
            File wrap = new File(AMTuneUtil.TMP_DIR + "wrapper");
            BufferedWriter br = new BufferedWriter(new FileWriter(wrap));
            br.write("#!/bin/sh\n");
            br.write(". " + tuneFile.trim());
            br.close();
            //Chmod for both the files.
            StringBuffer rBuff = new StringBuffer();
            AMTuneUtil.changeFilePerm(tuneFile, "700");
            AMTuneUtil.changeFilePerm(wrap.getAbsolutePath(), "700");
            int extVal = AMTuneUtil.executeCommand("/bin/sh " + 
                    wrap.getAbsolutePath(), rBuff);
            if (extVal == -1) {
                mWriter.writeln("Error sourcing file please check " +
                        "debug log file");
                pLogger.log(Level.SEVERE, "tuneTCP", 
                        "Error sourcing tune file " + tuneFile);
            } else {
                pLogger.log(Level.INFO, "tuneTCP", 
                        "Sourcing tune file success.");
            }
            wrap.delete();
        } catch (Exception ex) {
            throw new AMTuneException(ex.getMessage());
        }
    }
    
    /**
     * Returns the map containing current TCP parameter values.
     * @return map containing current TCP parameter values.
     */
    private Map getCurTCPVals() {
        Map curCfg = new HashMap();
        try {
            String getCmd = nddCmd + "-get " + TCP_DIV;
            String[] reqParams = {
                SOL_TCP_FIN_WAIT_2_FLUSH_INTERVAL,
                SOL_TCP_CONN_REQ_MAX_Q,
                SOL_TCP_CONN_REQ_MAX_Q0,
                SOL_TCP_KEEPALIVE_INTERVAL,
                SOL_TCP_SMALLEST_ANON_PORT,
                SOL_TCP_SLOW_START_INTITIAL,
                SOL_TCP_XMIT_HIWAT,
                SOL_TCP_RECV_HIWAT,
                SOL_TCP_IP_ABORT_CINTERVAL,
                SOL_TCP_DEFERRED_ACK_INTERVAL,
                SOL_TCP_STRONG_ISS,
                SOL_TCP_MAX_BUF,
                SOL_TCP_CWND_MAX
            };
            StringBuffer rBuf = new StringBuffer();
            for (int i = 0; i < reqParams.length; i++) {
                rBuf.setLength(0);
                int extVal = AMTuneUtil.executeCommand(getCmd + reqParams[i],
                        rBuf);
                if (extVal == -1) {
                    pLogger.log(Level.WARNING, "getCurTCPVals",
                            "Couldn't get " + reqParams[i]);
                    curCfg.put(reqParams[i], "");
                } else {
                    if (rBuf != null && rBuf.toString().trim().length() > 0) {
                        pLogger.log(Level.FINEST, "getCurTCPVals", 
                                "Cur TCP value of " + reqParams[i] + ": " +
                                rBuf.toString());
                        curCfg.put(reqParams[i], rBuf.toString().trim());
                    } else {
                        curCfg.put(reqParams[i], "");
                    }
                }
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "getCurTCPVals", 
                    "Error getting tcp values " + ex.getMessage());
        }
        return curCfg;
    }
}
