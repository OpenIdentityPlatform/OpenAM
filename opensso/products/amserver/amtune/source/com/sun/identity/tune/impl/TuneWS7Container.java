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
 * $Id: TuneWS7Container.java,v 1.11 2009/12/09 00:39:15 ykwon Exp $
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.tune.impl;

import com.sun.identity.tune.common.MessageWriter;
import com.sun.identity.tune.common.AMTuneException;
import com.sun.identity.tune.common.AMTuneLogger;
import com.sun.identity.tune.config.AMTuneConfigInfo;
import com.sun.identity.tune.config.WS7ContainerConfigInfo;
import com.sun.identity.tune.constants.WebContainerConstants;
import com.sun.identity.tune.intr.TuneWebServer;
import com.sun.identity.tune.util.AMTuneUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * <code>TuneWS7Container<\code> tunes Webserver 7 container.
 *
 */
public class TuneWS7Container extends TuneWebServer implements 
        WebContainerConstants {
    private AMTuneConfigInfo configInfo;
    private WS7ContainerConfigInfo wsConfigInfo;
    private AMTuneLogger pLogger;
    private MessageWriter mWriter;
    private String tuneFile;
    private Map curCfgMap;
    private String passwordStr = null;

    /**
     * Initializes the configuration information.
     *
     * @param confInfo Configuration information used for computing the tuning
     *   parameters for OpenSSO server.
     *
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public void initialize(AMTuneConfigInfo configInfo)
    throws AMTuneException {
        this.configInfo = configInfo;
        wsConfigInfo = (WS7ContainerConfigInfo)configInfo.getWSConfigInfo();
        passwordStr = WS7ADMIN_PASSWORD_SYNTAX + 
                    wsConfigInfo.getWsAdminPass();
        pLogger = AMTuneLogger.getLoggerInst();
        mWriter = MessageWriter.getInstance();
        tuneFile = wsConfigInfo.getContainerInstanceDir() + FILE_SEP +
                "config" + FILE_SEP + "server.xml";
        validateInstanceDir();
        curCfgMap = wsConfigInfo.getServerCfgMap();
    }

    /**
     * This method performs the sequence of operations for tuning
     * Web Server 7.
     *
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public void startTuning()
    throws AMTuneException {
        try {
            mWriter.writeln(LINE_SEP);
            tuneServerConfigFile();
            mWriter.writeln(PARA_SEP);
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "startTuning", "Error tuning WS7");
            mWriter.writeln(" ");
            mWriter.writelnLocaleMsg("pt-error-tuning-msg");
            mWriter.writeLocaleMsg("pt-web-tuning-error-msg");
            mWriter.writelnLocaleMsg("pt-manual-msg");
            pLogger.logException("startTuning", ex);
        } finally {
            try {
                deletePasswordFile();
            } catch(Exception ex) {
                //ignore
            }
        }
    }
    
    /**
     * This method finds the current Web Server configuration information and
     * recommends if any parameters require tuning.  If review mode is set to
     * "CHANGE", it sets the recommendations.
     *
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected void tuneServerConfigFile()
    throws AMTuneException {
        try {
            mWriter.writeln(CHAPTER_SEP);
            mWriter.writelnLocaleMsg("pt-web-server-inst");
            mWriter.writeln(CHAPTER_SEP);
            mWriter.writelnLocaleMsg("pt-init");
            mWriter.writeln(LINE_SEP);
            mWriter.writeln(" ");
            mWriter.writeLocaleMsg("pt-file");
            mWriter.writeln(tuneFile + " " + "(using wadm command line tool)");
            mWriter.writelnLocaleMsg("pt-param-tuning");
            mWriter.writeln(" ");
            mWriter.writelnLocaleMsg("pt-minthreads-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            String curMinThreads = curCfgMap.get(MIN_THREADS).toString();
            mWriter.writeln(MIN_THREADS + "=" + curMinThreads);
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(MIN_THREADS + "=" + curMinThreads);
            mWriter.writeln(" ");
            mWriter.writelnLocaleMsg("pt-maxthreads-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            String curMaxThreads = curCfgMap.get(MAX_THREADS).toString();
            mWriter.writeln(MAX_THREADS + "=" + curMaxThreads);
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(MAX_THREADS + "=" + getMaxThreadPoolVal());
            mWriter.writeln(" ");
            mWriter.writelnLocaleMsg("pt-queuesize-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(QUEUE_SIZE + "=" + curCfgMap.get(QUEUE_SIZE));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(QUEUE_SIZE + "=" + AMTUNE_NUM_TCP_CONN_SIZE);
            mWriter.writeln(" ");
            mWriter.writelnLocaleMsg("pt-nativestacksize-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(STACK_SIZE + "=" + curCfgMap.get(STACK_SIZE));
            mWriter.writeLocaleMsg("pt-rec-val");
            if (wsConfigInfo.isJVM64Bit()) {
                mWriter.writeln(STACK_SIZE + "=" +
                        AMTUNE_NATIVE_STACK_SIZE_64_BIT);
            } else {
                mWriter.writeln("Use current value");
            }
            mWriter.writelnLocaleMsg("pt-acceptorthreads-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(ACCEPTOR_THREADS + "=" +
                    curCfgMap.get(ACCEPTOR_THREADS));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(ACCEPTOR_THREADS + "=" +
                    configInfo.getAcceptorThreads());
            mWriter.writeln(" ");

            mWriter.writelnLocaleMsg("pt-stats-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(ENABLED + "=" + curCfgMap.get(ENABLED));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(ENABLED + "=" + AMTUNE_STATISTIC_ENABLED);
            mWriter.writeln(" ");
            int newMinHeapVal = configInfo.getMaxHeapSize();
            int newMaxHeapVal = configInfo.getMaxHeapSize();
            //workaround as WS u3 is not starting if the heap size is more 
            //than 12 GB
            if (newMinHeapVal > 12288) {
                newMinHeapVal = 12288;
            } 
            String wsAdminNewMinHeap = MIN_HEAP_FLAG + newMinHeapVal + "M";
            String wsAdminNewMaxHeap = MAX_HEAP_FLAG + newMaxHeapVal + "M";
            mWriter.writelnLocaleMsg("pt-maxminheap-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write("Min Heap: " + curCfgMap.get(MIN_HEAP_FLAG));
            mWriter.writeln(" Max Heap: " + curCfgMap.get(MAX_HEAP_FLAG));

            if (wsConfigInfo.isJVM64Bit() && (configInfo.getMemToUse() >
                    configInfo.getFAMTuneMaxMemoryToUseInMB())) {
                displayJVM64bitMessage(wsAdminNewMinHeap, wsAdminNewMaxHeap);
            } else {
                mWriter.writeLocaleMsg("pt-rec-val");
                mWriter.writeln(wsAdminNewMinHeap + " " + wsAdminNewMaxHeap);
            }
            mWriter.writeln(" ");
            String wsAdminNewLoggcOutput = GC_LOG_FLAG + ":" +
                    wsConfigInfo.getContainerInstanceDir() + FILE_SEP +
                    "logs" + FILE_SEP + "gc.log";
            mWriter.writelnLocaleMsg("pt-loggcoutput-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(GC_LOG_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(wsAdminNewLoggcOutput);
            mWriter.writeln(" ");

            String wsAdminNewServerMode = SERVER_FLAG;
            mWriter.writelnLocaleMsg("pt-jvmserver-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(SERVER_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(wsAdminNewServerMode);
            mWriter.writeln(" ");

            String wsAdminNewStackSize = "";
            if (wsConfigInfo.isJVM64Bit()) {
                wsAdminNewStackSize = "-Xss" +
                        configInfo.getFAMTunePerThreadStackSizeInKB64Bit() +
                        "k";
            } else {
                wsAdminNewStackSize = "-Xss" +
                        configInfo.getFAMTunePerThreadStackSizeInKB() + "k";
            }

            mWriter.writelnLocaleMsg("pt-jvmstacksize-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(STACK_SIZE_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(wsAdminNewStackSize);
            mWriter.writeln(" ");

            String wsAdminNewNewSize = NEW_SIZE_FLAG + "=" +
                    configInfo.getMaxNewSize() + "M";
            mWriter.writelnLocaleMsg("pt-newsize-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(NEW_SIZE_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(wsAdminNewNewSize);
            mWriter.writeln(" ");

            String wsAdminNewMaxNewSize = MAX_NEW_SIZE_FLAG + "=" +
                    configInfo.getMaxNewSize() + "M";
            mWriter.writelnLocaleMsg("pt-maxnewsize-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(MAX_NEW_SIZE_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(wsAdminNewMaxNewSize);
            mWriter.writeln(" ");

            String wsAdminNewDisableExplicitGc = DISABLE_EXPLICIT_GC_FLAG;

            mWriter.writelnLocaleMsg("pt-disableexp-gc-mg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(DISABLE_EXPLICIT_GC_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(wsAdminNewDisableExplicitGc);
            mWriter.writeln(" ");

            String wsAdminNewUseParallelGc = PARALLEL_GC_FLAG;

            mWriter.writelnLocaleMsg("pt-newpar-gc-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(PARALLEL_GC_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(wsAdminNewUseParallelGc);
            mWriter.writeln(" ");

            String wsAdminNewPrintClassHistogram = HISTOGRAM_FLAG;

            mWriter.writelnLocaleMsg("pt-prnclasshistogc-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(HISTOGRAM_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(wsAdminNewPrintClassHistogram);
            mWriter.writeln(" ");

            String wsAdminNewPrintGcTimeStamps = GC_TIME_STAMP_FLAG;
            mWriter.writelnLocaleMsg("pt-prngctimestamp-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(GC_TIME_STAMP_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(wsAdminNewPrintGcTimeStamps);
            mWriter.writeln(" ");

            String wsAdminNewUseConMarkSweepGc = MARK_SWEEP_GC_FLAG;
            mWriter.writelnLocaleMsg("pt-concmskgc-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(MARK_SWEEP_GC_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(wsAdminNewUseConMarkSweepGc);
            mWriter.writeln(" ");

            String wsAdminNewHeapDumpOOM = HEAPDUMP_OOM_FLAG;
            mWriter.writelnLocaleMsg("pt-ws-heapdumpoom-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(HEAPDUMP_OOM_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(wsAdminNewHeapDumpOOM);
            mWriter.writeln(" ");

            String wsAdminNewPrintConcLocks = PRINT_CONC_LOCKS_FLAG;
            mWriter.writelnLocaleMsg("pt-ws-printconclocks-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(PRINT_CONC_LOCKS_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(wsAdminNewPrintConcLocks);
            mWriter.writeln(" ");

            String wsAdminNewUseDoEscapeAnalysis = ESCAPE_ANALYSIS_FLAG;
            mWriter.writelnLocaleMsg("pt-ws-doescapeanalysis-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(ESCAPE_ANALYSIS_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(wsAdminNewUseDoEscapeAnalysis);
            mWriter.writeln(" ");

            String wsAdminNewUseCompressedOOPS = COMPRESSED_OOPS_FLAG;
            if (wsConfigInfo.isJVM64Bit()) {
                mWriter.writelnLocaleMsg("pt-ws-compressedoops-msg");
                mWriter.writeLocaleMsg("pt-cur-val");
                mWriter.writeln((String)curCfgMap.get(COMPRESSED_OOPS_FLAG));
                mWriter.writeLocaleMsg("pt-rec-val");
                mWriter.writeln(wsAdminNewUseCompressedOOPS);
                mWriter.writeln(" ");
            }

            String wsAdminCurParellelGCThreads = "";
            if (AMTuneUtil.isNiagara()) {
                mWriter.writelnLocaleMsg("pt-parallel-gc-threads-msg");
                mWriter.writeLocaleMsg("pt-cur-val");
                wsAdminCurParellelGCThreads = PARALLEL_GC_THREADS + "=" + 
                        curCfgMap.get(PARALLEL_GC_THREADS);
                mWriter.writeln(wsAdminCurParellelGCThreads);
                mWriter.writeLocaleMsg("pt-rec-val");
                mWriter.writeLocaleMsg("pt-rec-none");
                mWriter.writeln(" "); 
            }
            if (configInfo.isReviewMode()) {
                return;
            }
            //AMTuneUtil.backupConfigFile(tuneFile, "config-ws7-backup");
            String bakFile = wsConfigInfo.getContainerInstanceDir() + 
                    FILE_SEP + "server.xml-orig-" + AMTuneUtil.getRandomStr();
            AMTuneUtil.CopyFile(new File(tuneFile), new File(bakFile));
            boolean isThreadPoolPropChanged = false;
            //In Windows Vista WS7 is returning "default" value
            if (!curMaxThreads.equals("default")) {
                int curMax = Integer.parseInt(curMaxThreads);
                if (curMax != getMaxThreadPoolVal()) {
                    isThreadPoolPropChanged = setThreadPoolProp();
                }
            } else {
                isThreadPoolPropChanged = setThreadPoolProp();
            }
            boolean isHttpListnerPropChanged = false;
            if (!curCfgMap.get(ACCEPTOR_THREADS).toString().equals("default")) {
                int curAcceptorT = Integer.parseInt(
                        curCfgMap.get(ACCEPTOR_THREADS).toString());
                if (curAcceptorT != configInfo.getAcceptorThreads()) {
                    isHttpListnerPropChanged = setHttpListenerProp();
                }
            } else {
                isHttpListnerPropChanged = setHttpListenerProp();
            }
            boolean statsEnb =
                    Boolean.parseBoolean(curCfgMap.get(ENABLED).toString());
            boolean isStatsChanged = false;
            if (statsEnb != AMTUNE_STATISTIC_ENABLED) {
                isStatsChanged = setStatsProp();
            }
            List curJVMHeapOptList = new ArrayList();
            List newJVMHeapOptList = new ArrayList();
            List curJVMOptList = new ArrayList();
            List newJVMOptList = new ArrayList();
            
            if (!curCfgMap.get(MIN_HEAP_FLAG).toString().equals(
                    wsAdminNewMinHeap)) {
                curJVMHeapOptList.add(curCfgMap.get(MIN_HEAP_FLAG));
                newJVMHeapOptList.add(wsAdminNewMinHeap);
            }
            if (!curCfgMap.get(MAX_HEAP_FLAG).toString().equals(
                    wsAdminNewMaxHeap)) {
                curJVMHeapOptList.add(curCfgMap.get(MAX_HEAP_FLAG));
                newJVMHeapOptList.add(wsAdminNewMaxHeap);
            }
            if (!curCfgMap.get(SERVER_FLAG).toString().equals(
                    wsAdminNewServerMode)) {
                curJVMOptList.add(curCfgMap.get(SERVER_FLAG));
                newJVMOptList.add(wsAdminNewServerMode);
            }
            if (!curCfgMap.get(STACK_SIZE_FLAG).toString().equals(
                    wsAdminNewStackSize)) {
                curJVMOptList.add(curCfgMap.get(STACK_SIZE_FLAG));
                newJVMOptList.add(wsAdminNewStackSize);
            }
            if (!curCfgMap.get(GC_LOG_FLAG).toString().equals(
                    wsAdminNewLoggcOutput)) {
                curJVMOptList.add(curCfgMap.get(GC_LOG_FLAG));
                newJVMOptList.add(wsAdminNewLoggcOutput);
            }
            if (!curCfgMap.get(NEW_SIZE_FLAG).toString().equals(
                    wsAdminNewNewSize)) {
                curJVMOptList.add(curCfgMap.get(NEW_SIZE_FLAG));
                newJVMOptList.add(wsAdminNewNewSize);
            }
            if (!curCfgMap.get(MAX_NEW_SIZE_FLAG).toString().equals(
                    wsAdminNewMaxNewSize)) {
                curJVMOptList.add(curCfgMap.get(MAX_NEW_SIZE_FLAG));
                newJVMOptList.add(wsAdminNewMaxNewSize);
            }
            if (!curCfgMap.get(DISABLE_EXPLICIT_GC_FLAG).toString().equals(
                    wsAdminNewDisableExplicitGc)) {
                curJVMOptList.add(curCfgMap.get(DISABLE_EXPLICIT_GC_FLAG));
                newJVMOptList.add(wsAdminNewDisableExplicitGc);
            }
            if (!curCfgMap.get(PARALLEL_GC_FLAG).toString().equals(
                    wsAdminNewUseParallelGc)) {
                curJVMOptList.add(curCfgMap.get(PARALLEL_GC_FLAG));
                newJVMOptList.add(wsAdminNewUseParallelGc);
            }
            if (!curCfgMap.get(HISTOGRAM_FLAG).toString().equals(
                    wsAdminNewPrintClassHistogram)) {
                curJVMOptList.add(curCfgMap.get(HISTOGRAM_FLAG));
                newJVMOptList.add(wsAdminNewPrintClassHistogram);
            }
            if (!curCfgMap.get(GC_TIME_STAMP_FLAG).toString().equals(
                    wsAdminNewPrintGcTimeStamps)) {
                curJVMOptList.add(curCfgMap.get(GC_TIME_STAMP_FLAG));
                newJVMOptList.add(wsAdminNewPrintGcTimeStamps);
            }
            if (!curCfgMap.get(MARK_SWEEP_GC_FLAG).toString().equals(
                    wsAdminNewUseConMarkSweepGc)) {
                curJVMOptList.add(curCfgMap.get(MARK_SWEEP_GC_FLAG));
                newJVMOptList.add(wsAdminNewUseConMarkSweepGc);
            }

            if (!curCfgMap.get(HEAPDUMP_OOM_FLAG).toString().equals(
                    wsAdminNewHeapDumpOOM)) {
                curJVMOptList.add(curCfgMap.get(HEAPDUMP_OOM_FLAG));
                newJVMOptList.add(wsAdminNewHeapDumpOOM);
            } 

            if (!curCfgMap.get(PRINT_CONC_LOCKS_FLAG).toString().equals(
                    wsAdminNewPrintConcLocks)) {
                curJVMOptList.add(curCfgMap.get(PRINT_CONC_LOCKS_FLAG));
                newJVMOptList.add(wsAdminNewPrintConcLocks);
            }

            if (!curCfgMap.get(ESCAPE_ANALYSIS_FLAG).toString().equals(
                    wsAdminNewUseDoEscapeAnalysis)) {
                curJVMOptList.add(curCfgMap.get(ESCAPE_ANALYSIS_FLAG));
                newJVMOptList.add(wsAdminNewUseDoEscapeAnalysis);
            }

            if (wsConfigInfo.isJVM64Bit() && 
                    !curCfgMap.get(COMPRESSED_OOPS_FLAG).toString().equals(
                    wsAdminNewUseCompressedOOPS)) {
                curJVMOptList.add(curCfgMap.get(COMPRESSED_OOPS_FLAG));
                newJVMOptList.add(wsAdminNewUseCompressedOOPS);
            }

            if (AMTuneUtil.isNiagara()) {
                if (wsAdminCurParellelGCThreads.indexOf(NO_VAL_SET) == -1) {
                    curJVMOptList.add(wsAdminCurParellelGCThreads);
                }
            }
            boolean isHeapOptDel = 
                    deleteJVMOptionUsingWSAdmin(curJVMHeapOptList, true);
            boolean isJvmOptDel =
                    deleteJVMOptionUsingWSAdmin(curJVMOptList, false);
            boolean isInsertHeapOpt =
                    insertJVMOptionUsingWSAdmin(newJVMHeapOptList, true);
            boolean isInsertJVMOpt = 
                    insertJVMOptionUsingWSAdmin(newJVMOptList, false);
            if (isThreadPoolPropChanged || isHttpListnerPropChanged || 
                    isStatsChanged || isHeapOptDel || isJvmOptDel || 
                    isInsertHeapOpt || isInsertJVMOpt) {
                boolean retVal = deployConfig();
                if (retVal) {
                    AMTuneUtil.reStartWS7Serv(wsConfigInfo);
                } else {
                    mWriter.writelnLocaleMsg("pt-error-ws-deployment-failed");
                }
            } else {
                mWriter.writelnLocaleMsg("pt-web-conf-same-rec-msg");
            }
        } catch (AMTuneException amex) {
            pLogger.log(Level.SEVERE, "tuneServerConfigFile",
                    "Error tuning webserver7 configuration.");
            throw amex;  
        } catch (Exception ex) {
            pLogger.logException("tuneServerConfigFile", ex);
            throw new AMTuneException(ex.getMessage());
        }
    }

    /**
     * This method deploys new Web Server 7 configuration using wadm tool.
     */
    private boolean deployConfig() {
        try {
            pLogger.log(Level.INFO, "deployConfig", "Deploying configuration.");
            String deployCmd = wsConfigInfo.getWSAdminCmd() + 
                    WADM_DEPLOY_SUB_CMD + WADM_FORCE_OPT + 
                    wsConfigInfo.getWSAdmCommonParamsNoConfig() + " " +
                    wsConfigInfo.getWSAdminConfig();
            StringBuffer resultBuffer = new StringBuffer();
            int retVal = AMTuneUtil.executeCommand(deployCmd, passwordStr, 
                    wsConfigInfo.getWSAdminPassFilePath(),
                    resultBuffer);
            //wadm some times returns error code 125 even thought the 
            //values gets updated properly, this should not be considered 
            //to be failure case.
            if (retVal == -1 && resultBuffer.toString().indexOf(":125") == -1) {
                pLogger.log(Level.SEVERE, "deployConfig",
                        "Error executing command " + deployCmd);
                return false;
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "deployConfig",
                    "Deploying configuration failed. " + ex.getMessage());
        }
        return true;
    }

    /**
     * This method disables the  for statistics in Web Server
     */
    private boolean setStatsProp() {
        boolean statsChange = false;
        try {
            String statsCmd = wsConfigInfo.getWSAdminCmd() + 
                    WADM_SET_STATS_SUB_CMD + 
                    wsConfigInfo.getWSAdminCommonParams() + " " + ENABLED +
                    "=" + AMTUNE_STATISTIC_ENABLED;
            StringBuffer resultBuffer = new StringBuffer();
            int retVal = AMTuneUtil.executeCommand(statsCmd, passwordStr, 
                    wsConfigInfo.getWSAdminPassFilePath(),
                    resultBuffer);
            if (retVal == -1) {
                mWriter.writelnLocaleMsg("pt-set-param-error-msg");
                mWriter.writelnLocaleMsg("pt-check-dbg-logs-msg");
                pLogger.log(Level.SEVERE, "setStatsProp",
                        "Error executing command " + statsCmd);
            } else {
                statsChange = true;
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "setStatsProp", "Error setting " +
                    "set-thread-pool-prop " + ex.getMessage());
        }
        return statsChange;
    }

    /**
     * This method sets the HTTP Listener.
     */
    private boolean setHttpListenerProp() {
        boolean listenerChange = false;
        try {
            String setListenerCmd = wsConfigInfo.getWSAdminCmd() +
                    WADM_SET_HTTP_LISTENER_SUB_CMD +
                    wsConfigInfo.getWSAdminCommonParams() +
                    " --http-listener=" +
                    wsConfigInfo.getWSAdminHttpListener() +
                    " acceptor-threads=" + configInfo.getAcceptorThreads();
            StringBuffer resultBuffer = new StringBuffer();
            int retVal = AMTuneUtil.executeCommand(setListenerCmd, passwordStr, 
                    wsConfigInfo.getWSAdminPassFilePath(),
                    resultBuffer);
            if (retVal == -1) {
                mWriter.writelnLocaleMsg("pt-set-param-error-msg");
                mWriter.writelnLocaleMsg("pt-check-dbg-logs-msg");
                pLogger.log(Level.SEVERE, "setHttpListenerProp",
                        "Error executing command " + setListenerCmd);
            } else {
                listenerChange = true;
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "setHttpListenerProp", "Error setting " +
                    "set-http-listener-prop " + ex.getMessage());
        }
        return listenerChange;
    }

    /**
     * Sets the thread pool size
     */
    private boolean setThreadPoolProp() {
        boolean threadPoolPropChange = false;
        try {
            pLogger.log(Level.INFO, "setThreadPoolProp",
                    "Setting set-thread-pool-prop using wadm command.");
            String commonParam = MAX_THREADS + "=" + getMaxThreadPoolVal() +
                    " " + QUEUE_SIZE + "=" + AMTUNE_NUM_TCP_CONN_SIZE;
            String parmStr64Bit = commonParam + " " + STACK_SIZE + "=" +
                    AMTUNE_NATIVE_STACK_SIZE_64_BIT;

            String setProp64Cmd = wsConfigInfo.getWSAdminCmd() +
                    WADM_SET_THREAD_POOL_SUB_CMD +
                    wsConfigInfo.getWSAdminCommonParams() +
                    " " + parmStr64Bit;
            String setPropCmd = wsConfigInfo.getWSAdminCmd() +
                    WADM_SET_THREAD_POOL_SUB_CMD +
                    wsConfigInfo.getWSAdminCommonParams() +
                    " " + commonParam;
            StringBuffer resultBuffer = new StringBuffer();
            int retVal = 0;
            if (wsConfigInfo.isJVM64Bit()) {
                retVal = AMTuneUtil.executeCommand(setProp64Cmd, passwordStr,
                        wsConfigInfo.getWSAdminPassFilePath(),
                        resultBuffer);
                if (retVal == -1) {
                    mWriter.writelnLocaleMsg("pt-set-param-error-msg");
                    mWriter.writelnLocaleMsg("pt-check-dbg-logs-msg");
                    pLogger.log(Level.SEVERE, "setThreadPoolProp",
                            "Error executing command " + setProp64Cmd);
                } else {
                    threadPoolPropChange = true;
                }
            } else {
                retVal = AMTuneUtil.executeCommand(setPropCmd, passwordStr,
                        wsConfigInfo.getWSAdminPassFilePath(),
                        resultBuffer);
                if (retVal == -1) {
                    mWriter.writelnLocaleMsg("pt-set-param-error-msg");
                    mWriter.writelnLocaleMsg("pt-check-dbg-logs-msg");
                    pLogger.log(Level.SEVERE, "setThreadPoolProp",
                            "Error executing command " + setPropCmd);
                } else {
                    threadPoolPropChange = true;
                }
            }

        } catch(Exception ex) {
            pLogger.log(Level.SEVERE, "setThreadPoolProp", "Error setting " +
                    "set-thread-pool-prop " + ex.getMessage());
        }
        return threadPoolPropChange;
    }

    /**
     * Displays the heap size value 64 bit JVM.
     *
     * @param calMaxHeapSize
     * @param calMinHeapSize
     */
    private void displayJVM64bitMessage(String calMaxHeapSize,
            String calMinHeapSize) {
        mWriter.writelnLocaleMsg("pt-web-64bit-jvm-rec-msg1");
        mWriter.write("                     : " + configInfo.getMemToUse());
        mWriter.writelnLocaleMsg("pt-web-64bit-jvm-rec-msg2");
        mWriter.write("                     : ");
        mWriter.writeLocaleMsg("pt-web-64bit-jvm-rec-msg3");
        mWriter.writeln(AMTUNE_MEM_MAX_HEAP_SIZE_RATIO + ", in amtune-env");
        mWriter.write("                     : ");
        mWriter.writelnLocaleMsg("pt-web-64bit-jvm-rec-msg4");
        mWriter.write("                     : ");
        mWriter.writelnLocaleMsg("pt-web-64bit-jvm-rec-msg5");
        mWriter.write("                     : ");
        mWriter.writelnLocaleMsg("pt-web-64bit-jvm-rec-msg6");
        mWriter.write("                     : ");
        mWriter.writelnLocaleMsg("pt-web-64bit-jvm-rec-msg7");
        mWriter.writeln(" ");
        mWriter.write("                     : ");
        mWriter.writelnLocaleMsg("pt-web-64bit-jvm-cur-msg1");
        mWriter.write("                     : ");
        mWriter.writeln(AMTUNE_MEM_MAX_HEAP_SIZE_RATIO + "=" +
                configInfo.getFAMTuneMemMaxHeapSizeRatioExp());
        mWriter.write("                     : ");
        mWriter.writelnLocaleMsg("pt-web-64bit-jvm-cur-msg2");
        mWriter.write("                     : ");
        mWriter.writeln("Min Heap: " + calMaxHeapSize + " Max Heap: " +
                calMinHeapSize);

    }

    /**
     * This method Deletes one or more jvm option(s) from the server 
     * configuration.
     *
     * @param jvmOptions list of JVM options to be deleted.
     * @param combined set to true if options need to be enclosed in double 
     * quotes.
     *
     */
    private boolean deleteJVMOptionUsingWSAdmin(List jvmOptions,
            boolean combined) {
        if (jvmOptions.isEmpty()) {
            pLogger.log(Level.WARNING, "deleteJVMOptionUsingWSAdmin",
                    "JVM to be deleted are null");
            return false;
        }
        mWriter.writeln(" ");
        mWriter.writeLocaleMsg("pt-web-del-jvm-options");
        mWriter.writeln(jvmOptions.toString().replace(NO_VAL_SET, ""));
        StringBuilder jvmOpts = new StringBuilder();
        Iterator optItr = jvmOptions.iterator();
        if (combined) {
            jvmOpts.append("\"");
        }
        while (optItr.hasNext()) {
            String val = (String) optItr.next();
            if (val != null && val.trim().length() > 0 && 
                    !val.equals(NO_VAL_SET)) {
                jvmOpts.append(val);
                jvmOpts.append(" ");
            }
        }
        if (jvmOpts.toString().trim().length() < 1) {
            pLogger.log(Level.INFO, "deleteJVMOptionUsingWSAdmin",
                    "Nothing to delete from webserver config.");
            return false;
        }
        String delJVMCmd = wsConfigInfo.getWSAdminCmd() + 
                WADM_DEL_JVM_OPT_SUB_CMD +
                wsConfigInfo.getWSAdminCommonParams() + " -- " +
                jvmOpts.toString();
        delJVMCmd = delJVMCmd.trim();
        if (combined) {
            delJVMCmd = delJVMCmd + "\"";
        }
        StringBuffer resultBuffer = new StringBuffer();
        int retVal = -1;
        try {
            if (!AMTuneUtil.isWindows()) {
                retVal = AMTuneUtil.executeScriptCmd(delJVMCmd, passwordStr,
                        wsConfigInfo.getWSAdminPassFilePath(),
                        resultBuffer);
            } else {
                retVal = AMTuneUtil.executeCommand(delJVMCmd, passwordStr,
                        wsConfigInfo.getWSAdminPassFilePath(),
                        resultBuffer);
            }
        } catch (AMTuneException aex) {
            pLogger.log(Level.WARNING, "deleteJVMOptionUsingWSAdmin",
                    "Deleting jvm opt failed" + aex.getMessage());
            retVal = -1;
        }
        if (retVal == -1) {
            mWriter.writelnLocaleMsg("pt-del-jvm-error-msg");
            return false;
        } else {
            return true;
        }
    }

    /**
     * This method Create one or more jvm option(s)
     *
     * @param jvmOptions List of JVM options to be inserted
     * @param combined Set to true if options need to be enclosed in 
     * double quotes.
     */
     private boolean insertJVMOptionUsingWSAdmin(List jvmOptions,
             boolean combined) {
        if (jvmOptions.isEmpty()) {
            pLogger.log(Level.WARNING, "insertJVMOptionUsingWSAdmin",
                    "JVM options to add are null");
            return false;
        }
        mWriter.writeln(" ");
        mWriter.writelnLocaleMsg("pt-web-add-jvm-options");
        mWriter.writeln(jvmOptions.toString());
        StringBuilder jvmOpts = new StringBuilder();
        Iterator optItr = jvmOptions.iterator();
        if (combined) {
            jvmOpts.append("\"");
        }
        while (optItr.hasNext()) {
            String val = (String) optItr.next();
            if (val != null && val.trim().length() > 0) {
                jvmOpts.append(val);
                jvmOpts.append(" ");
            }
        }
        String addJVMCmd = wsConfigInfo.getWSAdminCmd() + 
                WADM_CREATE_JVM_OPT_SUB_CMD +
                wsConfigInfo.getWSAdminCommonParams() + " -- " +
                jvmOpts.toString();
        addJVMCmd = addJVMCmd.trim();
        if (combined) {
            addJVMCmd = addJVMCmd + "\"";
        }
        StringBuffer resultBuffer = new StringBuffer();
        int retVal = 0; 
        try {
            if (AMTuneUtil.isWindows()) {
                retVal = AMTuneUtil.executeCommand(addJVMCmd, passwordStr,
                        wsConfigInfo.getWSAdminPassFilePath(),
                        resultBuffer);
            } else {
                retVal = AMTuneUtil.executeScriptCmd(addJVMCmd, passwordStr,
                        wsConfigInfo.getWSAdminPassFilePath(),
                        resultBuffer);
            }
        } catch (AMTuneException ex) {
            pLogger.log(Level.WARNING, "insertJVMOptionUsingWSAdmin",
                   "Inserting jvm opts failed " + ex.getMessage());
            retVal = -1;
        }
        
        if (retVal == -1) {
            mWriter.writelnLocaleMsg("pt-create-jvm-opts-error-msg");
            mWriter.writelnLocaleMsg("pt-check-dbg-logs-msg");
            return false;
        } else {
            return true;
        }
    }
     
    protected void deletePasswordFile() {
        AMTuneUtil.deleteFile(wsConfigInfo.getWSAdminPassFilePath());
    }
    
    /**
     * Return maximum number of threads.
     */
    private int getMaxThreadPoolVal() {
        int recMaxThreadPool = configInfo.getNumOfMaxThreadPool();
        if (recMaxThreadPool < AMTUNE_NUM_WS_THREADS_MIN_VAL) {
            recMaxThreadPool = AMTUNE_NUM_WS_THREADS_MIN_VAL;
        }
        if (recMaxThreadPool > AMTUNE_NUM_WS_THREADS_MAX_VAL) {
            recMaxThreadPool = AMTUNE_NUM_WS_THREADS_MAX_VAL;
        }
        return recMaxThreadPool;
    }
    
    /**
     * Validates the instance directory
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void validateInstanceDir() 
    throws AMTuneException {
        File serConfFile = new File(tuneFile);
        if (!serConfFile.exists()) {
            mWriter.writelnLocaleMsg("pt-error-ws-conf-file-not-found");
            AMTuneUtil.printErrorMsg(CONTAINER_INSTANCE_DIR);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-ws-invalid-instance-dir"));
        }
    }
}
