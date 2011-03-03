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
 * $Id: TuneAS9Container.java,v 1.11 2009/12/09 00:38:36 ykwon Exp $
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.tune.impl;

import com.sun.identity.tune.common.FileHandler;
import com.sun.identity.tune.common.MessageWriter;
import com.sun.identity.tune.common.AMTuneException;
import com.sun.identity.tune.common.AMTuneLogger;
import com.sun.identity.tune.config.AS9ContainerConfigInfo;
import com.sun.identity.tune.config.AMTuneConfigInfo;
import com.sun.identity.tune.constants.WebContainerConstants;
import com.sun.identity.tune.intr.TuneAppServer;
import com.sun.identity.tune.util.AMTuneUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class tunes Application server 9.1
 */
public class TuneAS9Container extends TuneAppServer implements 
        WebContainerConstants {
    
    private AMTuneLogger pLogger;
    private MessageWriter mWriter;
    private AMTuneConfigInfo configInfo;
    private AS9ContainerConfigInfo asConfigInfo;
    private Map curCfgMap;
    private String passwordStr;
    
    /**
     * Constructs instance of TuneAS9Container
     * 
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public TuneAS9Container() throws AMTuneException {
        pLogger = AMTuneLogger.getLoggerInst();
        mWriter = MessageWriter.getInstance();
    }
    
    /**
     * Initialize the configuration data.
     * 
     * @param confInfo
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public void initialize(AMTuneConfigInfo confInfo) 
    throws AMTuneException {
        try {
            this.configInfo = confInfo;
            asConfigInfo = (AS9ContainerConfigInfo) confInfo.getWSConfigInfo();
            passwordStr = ASADMIN_PASSWORD_SYNTAX + 
                    asConfigInfo.getAsAdminPass();
            curCfgMap = asConfigInfo.getCurASConfigInfo();
            validateInstanceDir();
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "initialize", 
                    "Error initializing Application server 9.1.");
            throw new AMTuneException(ex.getMessage());
        }
    }
    
    /**
     * Tunes Application server 9.1
     * 
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public void startTuning() 
    throws AMTuneException {
        try {
            mWriter.writeln(CHAPTER_SEP);
            mWriter.writelnLocaleMsg("pt-app-tuning-msg");
            mWriter.writeln(CHAPTER_SEP);
            mWriter.writelnLocaleMsg("pt-init");
            mWriter.writeln(LINE_SEP);
            tuneDomainXML();
            if (AMTuneUtil.isLinux()) {
                tuneSecurityLimits();
            }
            mWriter.writeln(PARA_SEP);
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "startTuning", 
                    "Error Tuning Application server 9.1.");
            mWriter.writeln(" ");
            mWriter.writelnLocaleMsg("pt-error-tuning-msg");
            mWriter.writeLocaleMsg("pt-web-tuning-error-msg");
            mWriter.writelnLocaleMsg("pt-manual-msg");
            pLogger.logException("startTuning", ex);
        } finally {
            try {
                deletePasswordFile();
            } catch (Exception ex) {
                //ignore
            }
        }
    }
    
    /**
     * Tunes domain.xml file
     * 
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected void tuneDomainXML() 
    throws AMTuneException {
        try {
            String tuneFile = asConfigInfo.getContainerInstanceDir() + 
                    FILE_SEP + "config" + FILE_SEP + "domain.xml";
            mWriter.writelnLocaleMsg("pt-app-srv-tuning-inst");
            mWriter.writeln(" ");
            mWriter.writeLocaleMsg("pt-file");
            mWriter.writeln(tuneFile + " (using asadmin command line tool)");
            mWriter.writelnLocaleMsg("pt-param-tuning");
            mWriter.writeln(" ");
            
            mWriter.writelnLocaleMsg("pt-as-acceptor-threads-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(ACCEPTOR_THREADS + "=" +
                    curCfgMap.get(ACCEPTOR_THREAD_PARAM));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(ACCEPTOR_THREADS + "=" +
                    configInfo.getAcceptorThreads());
            mWriter.writeln(" ");

            mWriter.writelnLocaleMsg("pt-as-pending-count-threads-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(MAX_PENDING_COUNT + "=" +
                    curCfgMap.get(COUNT_THREAD_PARAM));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(MAX_PENDING_COUNT + "=" + AMTUNE_NUM_TCP_CONN_SIZE);
            mWriter.writeln(" ");
            
            mWriter.writelnLocaleMsg("pt-as-queue-size-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(QUEUE_SIZE + "=" +
                    curCfgMap.get(QUEUE_SIZE_PARAM));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(QUEUE_SIZE + "=" + AMTUNE_NUM_TCP_CONN_SIZE);
            mWriter.writeln(" ");
            
            int newMinHeapVal = configInfo.getMaxHeapSize();
            int newMaxHeapVal = configInfo.getMaxHeapSize();
            //workaround as AS is not starting if the heap size is more 
            //than 12 GB
            if (newMinHeapVal > 12288) {
                newMinHeapVal = 12288;
            } 
            
            String asAdminNewMinHeap = MIN_HEAP_FLAG + newMinHeapVal + "M";
            String asAdminNewMaxHeap = MAX_HEAP_FLAG + newMaxHeapVal + "M";
            mWriter.writelnLocaleMsg("pt-as-heap-size-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write("Min Heap: " + curCfgMap.get(MIN_HEAP_FLAG));
            mWriter.writeln(" Max Heap: " + curCfgMap.get(MAX_HEAP_FLAG));
            if (asConfigInfo.isJVM64Bit() && (configInfo.getMemToUse() >
                    configInfo.getFAMTuneMaxMemoryToUseInMB())) {
                displayJVM64bitMessage(asAdminNewMinHeap, asAdminNewMaxHeap);
            } else {
                mWriter.writeLocaleMsg("pt-rec-val");
                mWriter.writeln(asAdminNewMinHeap + " " + asAdminNewMaxHeap);
                mWriter.writeln(" ");
            }
            
            String asAdminNewLoggcOutput = GC_LOG_FLAG + ":" +
                    asConfigInfo.getContainerInstanceDir() + FILE_SEP +
                    "logs" + FILE_SEP + "gc.log";
            mWriter.writelnLocaleMsg("pt-as-loggc-output-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(GC_LOG_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewLoggcOutput);
            mWriter.writeln(" ");
            
            String asAdminNewServerMode = SERVER_FLAG;
            mWriter.writelnLocaleMsg("pt-as-server-mode-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            String modeFlag = (String)curCfgMap.get(CLIENT_FLAG);
            if (modeFlag != null && modeFlag.trim().length() > 0 &&
                    modeFlag.indexOf(CLIENT_FLAG) != -1) {
                mWriter.writeln(modeFlag);
            } else {
                mWriter.writeln((String)curCfgMap.get(SERVER_FLAG));
            }
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewServerMode);
            mWriter.writeln(" ");
            
            String asAdminNewStackSize = "";
            if (asConfigInfo.isJVM64Bit()) {
                asAdminNewStackSize = STACK_SIZE_FLAG +
                        configInfo.getFAMTunePerThreadStackSizeInKB64Bit() +
                        "k";
            } else {
                asAdminNewStackSize = STACK_SIZE_FLAG +
                        configInfo.getFAMTunePerThreadStackSizeInKB() + "k";
            }
            mWriter.writelnLocaleMsg("pt-as-stack-size-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(STACK_SIZE_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewStackSize);
            mWriter.writeln(" ");
            
            String asAdminNewNewSize = NEW_SIZE_FLAG + "=" +
                    configInfo.getMaxNewSize() + "M";
            mWriter.writelnLocaleMsg("pt-as-new-size-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(NEW_SIZE_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewNewSize);
            mWriter.writeln(" ");

            String asAdminNewMaxNewSize = MAX_NEW_SIZE_FLAG + "=" +
                    configInfo.getMaxNewSize() + "M";
            mWriter.writelnLocaleMsg("pt-as-max-new-size-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(MAX_NEW_SIZE_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewMaxNewSize);
            mWriter.writeln(" ");

            String asAdminNewDisableExplicitGc = DISABLE_EXPLICIT_GC_FLAG;

            mWriter.writelnLocaleMsg("pt-as-diable-gc-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(DISABLE_EXPLICIT_GC_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewDisableExplicitGc);
            mWriter.writeln(" ");

            String asAdminNewUseParallelGc = PARALLEL_GC_FLAG;

            mWriter.writelnLocaleMsg("pt-as-use-parallel-gc-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(PARALLEL_GC_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewUseParallelGc);
            mWriter.writeln(" ");

            String asAdminNewPrintClassHistogram = HISTOGRAM_FLAG;

            mWriter.writelnLocaleMsg("pt-as-histo-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(HISTOGRAM_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewPrintClassHistogram);
            mWriter.writeln(" ");

            String asAdminNewPrintGcTimeStamps = GC_TIME_STAMP_FLAG;
            mWriter.writelnLocaleMsg("pt-as-gc-time-stamp-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(GC_TIME_STAMP_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewPrintGcTimeStamps);
            mWriter.writeln(" ");

            String asAdminNewUseConMarkSweepGc = MARK_SWEEP_GC_FLAG;
            mWriter.writelnLocaleMsg("pt-as-sweep-mark-gc-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(MARK_SWEEP_GC_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewUseConMarkSweepGc);
            mWriter.writeln(" ");

            String asAdminNewHeapDumpOOM = HEAPDUMP_OOM_FLAG;
            mWriter.writelnLocaleMsg("pt-as-heapdumpoom-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(HEAPDUMP_OOM_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewHeapDumpOOM);
            mWriter.writeln(" ");

            String asAdminNewPrintConcLocks = PRINT_CONC_LOCKS_FLAG;
            mWriter.writelnLocaleMsg("pt-as-printconclocks-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(PRINT_CONC_LOCKS_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewPrintConcLocks);
            mWriter.writeln(" ");

            String asAdminNewUseDoEscapeAnalysis = ESCAPE_ANALYSIS_FLAG;
            mWriter.writelnLocaleMsg("pt-as-doescapeanalysis-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(ESCAPE_ANALYSIS_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewUseDoEscapeAnalysis);
            mWriter.writeln(" ");

            String asAdminNewUseCompressedOOPS = COMPRESSED_OOPS_FLAG;
            if (asConfigInfo.isJVM64Bit()) {
                mWriter.writelnLocaleMsg("pt-as-compressedoops-msg");
                mWriter.writeLocaleMsg("pt-cur-val");
                mWriter.writeln((String)curCfgMap.get(COMPRESSED_OOPS_FLAG));
                mWriter.writeLocaleMsg("pt-rec-val");
                mWriter.writeln(asAdminNewUseCompressedOOPS);
                mWriter.writeln(" ");
            }

            String asAdminNewServerpolicy = "";
            if (asConfigInfo.isTuneWebContainerJavaPolicy()) {
                asAdminNewServerpolicy =
                        "${com.sun.aas.instanceRoot}/config/" +
                        "server.policy.NOTUSED";
                mWriter.writelnLocaleMsg("pt-as-server-sec-policy-check-msg");
                mWriter.writeLocaleMsg("pt-cur-val");
                mWriter.write(JAVA_SECURITY_POLICY + "=");
                mWriter.writeln((String) curCfgMap.get(JAVA_SECURITY_POLICY));
                mWriter.writeLocaleMsg("pt-rec-val");
                mWriter.write(JAVA_SECURITY_POLICY + "=");
                mWriter.writeln(asAdminNewServerpolicy);
                mWriter.writeln(" ");
            }
            if (AMTuneUtil.isNiagara()) {
                mWriter.writelnLocaleMsg("pt-as-parallel-gc-threads-msg");
                mWriter.writeLocaleMsg("pt-cur-val");
                mWriter.writeln(PARALLEL_GC_THREADS + "=" + 
                        curCfgMap.get(PARALLEL_GC_THREADS));
                mWriter.writeLocaleMsg("pt-rec-val");
                mWriter.writelnLocaleMsg("pt-rec-none");
                mWriter.writeln(" ");
            }
            
            mWriter.writelnLocaleMsg("pt-as-rqst-proc-init-threads-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(REQUEST_PROCESSING_INITIAL_THREAD_COUNT + "=" +
                    curCfgMap.get(REQUESTPROC_INIT_THREAD_PARAM));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(REQUEST_PROCESSING_INITIAL_THREAD_COUNT + "=" +
                    AMTUNE_NUM_WS_RQTHROTTLE_MIN);
            mWriter.writeln(" ");

            mWriter.writelnLocaleMsg("pt-as-rqst-proc-thread-count-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(REQUEST_PROCESSING_THREAD_COUNT + "=" +
                    curCfgMap.get(REQUESTPROC_THREAD_PARAM));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(REQUEST_PROCESSING_THREAD_COUNT + "=" +
                    getMaxThreadPoolVal());
            mWriter.writeln(" ");

            if (configInfo.isReviewMode()) {
                return;
            }

            AMTuneUtil.backupConfigFile(tuneFile);
            setASParams();
            List delOptList = new ArrayList();
            List newOptList = new ArrayList();
            if (!curCfgMap.get(MIN_HEAP_FLAG).toString().equals(
                    asAdminNewMinHeap)) {
                delOptList.add(curCfgMap.get(MIN_HEAP_FLAG));
                newOptList.add(asAdminNewMinHeap);
            }
            if (!curCfgMap.get(MAX_HEAP_FLAG).toString().equals(
                    asAdminNewMaxHeap)) {
                delOptList.add(curCfgMap.get(MAX_HEAP_FLAG));
                newOptList.add(asAdminNewMaxHeap);
            }
            if (!curCfgMap.get(GC_LOG_FLAG).toString().equals(
                    asAdminNewLoggcOutput)) {
                delOptList.add(curCfgMap.get(GC_LOG_FLAG));
                newOptList.add(asAdminNewLoggcOutput);
            }
            //delOptList.add(curCfgMap.get(CLIENT_FLAG));
            if (!curCfgMap.get(STACK_SIZE_FLAG).toString().equals(
                    asAdminNewStackSize)) {
                delOptList.add(curCfgMap.get(STACK_SIZE_FLAG));
                newOptList.add(asAdminNewStackSize);
            }
            if (!curCfgMap.get(NEW_SIZE_FLAG).toString().equals(
                    asAdminNewNewSize)) {
                delOptList.add(curCfgMap.get(NEW_SIZE_FLAG));
                newOptList.add(asAdminNewNewSize);
            }
            if (!curCfgMap.get(MAX_NEW_SIZE_FLAG).toString().equals(
                    asAdminNewMaxNewSize)) {
                delOptList.add(curCfgMap.get(MAX_NEW_SIZE_FLAG));
                newOptList.add(asAdminNewMaxNewSize);
            }
            if (!curCfgMap.get(DISABLE_EXPLICIT_GC_FLAG).toString().equals(
                    asAdminNewDisableExplicitGc)) {
                delOptList.add(curCfgMap.get(DISABLE_EXPLICIT_GC_FLAG));
                newOptList.add(asAdminNewDisableExplicitGc);
            }
            if (!curCfgMap.get(PARALLEL_GC_FLAG).toString().equals(
                    asAdminNewUseParallelGc)) {
                delOptList.add(curCfgMap.get(PARALLEL_GC_FLAG));
                newOptList.add(asAdminNewUseParallelGc);
            }
            if (!curCfgMap.get(MARK_SWEEP_GC_FLAG).toString().equals(
                    asAdminNewUseConMarkSweepGc)) {
                delOptList.add(curCfgMap.get(MARK_SWEEP_GC_FLAG));
                newOptList.add(asAdminNewUseConMarkSweepGc);
            }
            if (!curCfgMap.get(HEAPDUMP_OOM_FLAG).toString().equals(
                    asAdminNewHeapDumpOOM)) {
                delOptList.add(curCfgMap.get(HEAPDUMP_OOM_FLAG));
                newOptList.add(asAdminNewHeapDumpOOM);
            }
            if (!curCfgMap.get(PRINT_CONC_LOCKS_FLAG).toString().equals(
                    asAdminNewPrintConcLocks)) {
                delOptList.add(curCfgMap.get(PRINT_CONC_LOCKS_FLAG));
                newOptList.add(asAdminNewPrintConcLocks);
            }
            if (!curCfgMap.get(ESCAPE_ANALYSIS_FLAG).toString().equals(
                    asAdminNewUseDoEscapeAnalysis)) {
                delOptList.add(curCfgMap.get(ESCAPE_ANALYSIS_FLAG));
                newOptList.add(asAdminNewUseDoEscapeAnalysis);
            }
            if (asConfigInfo.isJVM64Bit() &&
                  !curCfgMap.get(COMPRESSED_OOPS_FLAG).toString().equals(
                  asAdminNewUseCompressedOOPS)) {
                delOptList.add(curCfgMap.get(COMPRESSED_OOPS_FLAG));
                newOptList.add(asAdminNewUseCompressedOOPS);
            }
            if (!curCfgMap.get(HISTOGRAM_FLAG).toString().equals(
                    asAdminNewPrintClassHistogram)) {
                delOptList.add(curCfgMap.get(HISTOGRAM_FLAG));
                newOptList.add(asAdminNewPrintClassHistogram);
            }
            if (!curCfgMap.get(GC_TIME_STAMP_FLAG).toString().equals(
                    asAdminNewPrintGcTimeStamps)) {
                delOptList.add(curCfgMap.get(GC_TIME_STAMP_FLAG));
                newOptList.add(asAdminNewPrintGcTimeStamps);
            }
            if (modeFlag != null && modeFlag.trim().length() > 0
                    && modeFlag.indexOf(CLIENT_FLAG) != -1) {
                delOptList.add(modeFlag);
                newOptList.add(asAdminNewServerMode);
            }
            if (asConfigInfo.isTuneWebContainerJavaPolicy()) {
                delOptList.add(JAVA_SECURITY_POLICY + "=" + 
                        curCfgMap.get(JAVA_SECURITY_POLICY));
            }
            if (AMTuneUtil.isNiagara()) {
                String curGCThreadOpt = PARALLEL_GC_THREADS + "=" + 
                        curCfgMap.get(PARALLEL_GC_THREADS);
                if (curGCThreadOpt.indexOf(NO_VAL_SET) == -1) {
                    delOptList.add(curGCThreadOpt);
                }
            }
            if (asConfigInfo.isTuneWebContainerJavaPolicy()) {
                newOptList.add(JAVA_SECURITY_POLICY + "=" + 
                        asAdminNewServerpolicy);
            }
            deleteCurJVMOptions(delOptList);
            insertNewJVMOptions(newOptList);
            if (delOptList.size() > 0 || newOptList.size() > 0) {
                mWriter.writelnLocaleMsg("pt-as-restart-msg");
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "tuneDomainXML", 
                    "Error tuning Application server 9.1 domain xml file.");
            throw new AMTuneException(ex.getMessage());
        }
    }
    /**
     * This method Construct a parameter string to perform an asadmin 
     * set for acceptor-thread, request-processing thread, queue-size 
     * and count-thread parameters
     */
    private void setASParams() {
        try {
            StringBuilder asAdminSetParams = new StringBuilder();
            if (curCfgMap.get(ACCEPTOR_THREAD_PARAM) != null) {
                int curAccVal = Integer.parseInt(
                        curCfgMap.get(ACCEPTOR_THREAD_PARAM).toString());
                if (curAccVal != configInfo.getAcceptorThreads()) {
                    asAdminSetParams.append(
                            asConfigInfo.getAcceptorThreadString());
                    asAdminSetParams.append("=");
                    asAdminSetParams.append(configInfo.getAcceptorThreads());
                    asAdminSetParams.append(" ");
                }
            }
            if (curCfgMap.get(REQUESTPROC_INIT_THREAD_PARAM) != null) {
                int curReqInitVal = Integer.parseInt(
                     curCfgMap.get(REQUESTPROC_INIT_THREAD_PARAM).toString());
                if (curReqInitVal < AMTUNE_NUM_WS_RQTHROTTLE_MIN) {
                    asAdminSetParams.append(REQUESTPROC_INIT_THREAD_PARAM);
                    asAdminSetParams.append("=");
                    asAdminSetParams.append(AMTUNE_NUM_WS_RQTHROTTLE_MIN);
                    asAdminSetParams.append(" ");
                }
            }
            if (curCfgMap.get(REQUESTPROC_THREAD_PARAM) != null) {
                int curReqVal = Integer.parseInt(
                      curCfgMap.get(REQUESTPROC_THREAD_PARAM).toString());
                if (curReqVal < getMaxThreadPoolVal()) {
                    asAdminSetParams.append(REQUESTPROC_THREAD_PARAM);
                    asAdminSetParams.append("=");
                    asAdminSetParams.append(getMaxThreadPoolVal());
                    asAdminSetParams.append(" ");
                }
            }
            if (curCfgMap.get(COUNT_THREAD_PARAM) != null) {
                int curVal = Integer.parseInt(
                        curCfgMap.get(COUNT_THREAD_PARAM).toString());
                if (curVal < AMTUNE_NUM_TCP_CONN_SIZE ) {
                    asAdminSetParams.append(COUNT_THREAD_PARAM);
                    asAdminSetParams.append("=");
                    asAdminSetParams.append(AMTUNE_NUM_TCP_CONN_SIZE);
                    asAdminSetParams.append(" ");
                }
            }
            if (curCfgMap.get(QUEUE_SIZE_PARAM) != null) {
                int curVal = Integer.parseInt(
                        curCfgMap.get(QUEUE_SIZE_PARAM).toString());
                if (curVal < AMTUNE_NUM_TCP_CONN_SIZE) {
                    asAdminSetParams.append(QUEUE_SIZE_PARAM);
                    asAdminSetParams.append("=");
                    asAdminSetParams.append(AMTUNE_NUM_TCP_CONN_SIZE);
                }
            }
            if (asAdminSetParams.length() == 0) {
                pLogger.log(Level.INFO, "asAdminSetParams",
                        "All params are same as recommended values.");
                return;
            }
            StringBuffer resultBuffer = new StringBuffer();
            StringBuilder setCmd =
                    new StringBuilder(asConfigInfo.getASAdminCmd());
            setCmd.append("set ");
            setCmd.append(asConfigInfo.getAsAdminCommonParamsNoTarget());
            setCmd.append(" ");
            setCmd.append(asAdminSetParams.toString());
            int retVal = AMTuneUtil.executeCommand(setCmd.toString(), 
                    passwordStr, 
                    asConfigInfo.getAdminPassfilePath(),
                    resultBuffer);
            if (retVal != 0) {
                mWriter.writelnLocaleMsg("pt-set-param-error-msg");
                mWriter.writelnLocaleMsg("pt-check-dbg-logs-msg");
                pLogger.log(Level.SEVERE, "setASParams", "Error executing " +
                        "asadmin.");
            }
        } catch (Exception ex) {
            mWriter.writelnLocaleMsg("pt-set-param-error-msg");
            mWriter.writelnLocaleMsg("pt-check-dbg-logs-msg");
            pLogger.log(Level.SEVERE, "setASParams",
                    "Application Server Parameters couldn't be set. " +
                    ex.getMessage());
        }
    }
    
    /**
     * Deletes current JVM options using asadmin.
     * @param curJvmOptions List of Application server 9.2 options.
     */
    private void deleteCurJVMOptions(List curJvmOptions) {
        try {
            StringBuilder delOpts = new StringBuilder();
            if (AMTuneUtil.isWindows()) {
                delOpts.append(" \"");
            } else {
                delOpts.append(" :");
            }
            Iterator optItr = curJvmOptions.iterator();
            while (optItr.hasNext()) {
                String val = optItr.next().toString().trim();
                if (val.length() > 0 && 
                        !val.equalsIgnoreCase(NO_VAL_SET)) {
                    delOpts.append(AS_PARAM_DELIM);
                    val = val.replace(AS_PARAM_DELIM, "\\" + AS_PARAM_DELIM);
                    delOpts.append(val);
                }
            }
            if (AMTuneUtil.isWindows()) {
                delOpts.append("\"");
            }
            if(delOpts.toString().trim().length() < 3) {
                pLogger.log(Level.INFO, "deleteCurJVMOptions", 
                        "No JVM options to delete");
                return;
            }
            StringBuilder depOptCmd =
                    new StringBuilder(asConfigInfo.getASAdminCmd());
            depOptCmd.append("delete-jvm-options ");
            depOptCmd.append(asConfigInfo.getAsAdminCommonParams());
            depOptCmd.append(delOpts.toString());
            StringBuffer resultBuffer = new StringBuffer();
            int retVal = AMTuneUtil.executeCommand(depOptCmd.toString(), 
                    passwordStr, 
                    asConfigInfo.getAdminPassfilePath(),
                    resultBuffer);
            if (retVal != 0) {
                mWriter.writelnLocaleMsg("pt-del-jvm-error-msg");
                mWriter.writelnLocaleMsg("pt-check-dbg-logs-msg");
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "deleteCurJVMOptions",
                    "Error deleting JVM options. " + ex.getMessage());
        }
    }
    
    /**
     * Inserts new JVM options using asadmin
     * 
     * @param newJvmOptions
     */
    private void insertNewJVMOptions(List newJVMOpts) {
        try {
            StringBuilder newOpts = new StringBuilder();
            if (AMTuneUtil.isWindows()) {
                newOpts.append(" \"");
            } else {
                newOpts.append(" :");
            }
            Iterator optItr = newJVMOpts.iterator();
            while(optItr.hasNext()) {
                String val = optItr.next().toString().trim();
                if(val.length() > 0) {
                    newOpts.append(AS_PARAM_DELIM);
                    val = val.replace(AS_PARAM_DELIM, "\\" + AS_PARAM_DELIM);
                    newOpts.append(val);
                }
            }
            if (AMTuneUtil.isWindows()) {
                newOpts.append("\"");
            }
            if(newOpts.toString().trim().length() < 3) {
                pLogger.log(Level.INFO, "insertNewJVMOptions", 
                        "No JVM options to insert");
                return;
            }
            StringBuffer resultBuffer = new StringBuffer();
            StringBuilder newOptCmd =
                    new StringBuilder(asConfigInfo.getASAdminCmd());
            newOptCmd.append("create-jvm-options ");
            newOptCmd.append(asConfigInfo.getAsAdminCommonParams());
            newOptCmd.append(newOpts.toString());
            int retVal = AMTuneUtil.executeCommand(newOptCmd.toString(), 
                    passwordStr, 
                    asConfigInfo.getAdminPassfilePath(),
                    resultBuffer);
            if (retVal != 0) {
                mWriter.writelnLocaleMsg("pt-create-jvm-opts-error-msg");
                mWriter.writelnLocaleMsg("pt-check-dbg-logs-msg");
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "insertNewJVMOptions", 
                    "Error setting new JVM options. " + ex.getMessage());
        }
    }
    
    /**
     * 
     */
    protected void tuneSecurityLimits() 
    throws AMTuneException {
        String tuneFile = "/etc/security/limits.conf";
        FileHandler fh = null;
        try {
            fh = new FileHandler(tuneFile);
            mWriter.writeln(LINE_SEP);
            mWriter.writelnLocaleMsg("pt-app-stack-size-tuning");
            mWriter.writeln(" ");
            mWriter.writeLocaleMsg("pt-file");
            mWriter.writeln(tuneFile);
            mWriter.writelnLocaleMsg("pt-param-tuning");
            String[] mLines = fh.getMattchingLines("^#", true);
            mLines = AMTuneUtil.getMatchedLines(mLines, "stack");
            mLines = AMTuneUtil.getMatchedLines(mLines, "hard");
            String newStackSize = "";
            String curStackSizeStr = " ";
            if (mLines.length > 0) {
                String firCol = mLines[0].substring(0, mLines[0].indexOf(" "));
                newStackSize = firCol + "               " + "hard    " +
                        "stack          " + AMTUNE_LINUX_STACK_SIZE_LIMITS;
                curStackSizeStr = mLines[0];
            } else {
                newStackSize = "*               hard    stack          " +
                        AMTUNE_LINUX_STACK_SIZE_LIMITS;
            }
            mWriter.writelnLocaleMsg("pt-stack-size-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(curStackSizeStr);
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(newStackSize);
            if (configInfo.isReviewMode() || 
                    (curStackSizeStr != null && 
                    curStackSizeStr.trim().length() > 0 &&
                    curStackSizeStr.equals(newStackSize))) {
                return;
            }
            String[] delLines = new String[3];
            delLines[0] = "Start: AS9.1 OpenSSO Tuning :";
            delLines[1] = "End: AS9.1 OpenSSO Tuning :";
            if (curStackSizeStr != null && curStackSizeStr.trim().length() >0) {
                delLines[2] = curStackSizeStr;
            }
            fh.removeMatchingLines(delLines);
            fh.appendLine("# " + delLines[0] + AMTuneUtil.getTodayDateStr());
            fh.appendLine(newStackSize);
            fh.appendLine("# " + delLines[1] + AMTuneUtil.getTodayDateStr());
            mWriter.writelnLocaleMsg("pt-lnx-reboot-msg");
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "tuneSecurityLimits",
                    "Error tuning security limits " + ex.getMessage());
            throw new AMTuneException(ex.getMessage());
        } finally {
            try {
                fh.close();
            } catch (Exception ex) {
                //ignore
            }
        }
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
    
    protected void deletePasswordFile() {
        AMTuneUtil.deleteFile(asConfigInfo.getAdminPassfilePath());
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
     * Validates the instance directory.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void validateInstanceDir() 
    throws AMTuneException {
        String appConfFile =asConfigInfo.getContainerInstanceDir() + 
                    FILE_SEP + "config" + FILE_SEP + "domain.xml";
        File confFile = new File(appConfFile);
        if (!confFile.exists()) {
            mWriter.writelnLocaleMsg("pt-error-as-conf-file-not-found");
            AMTuneUtil.printErrorMsg(CONTAINER_INSTANCE_DIR);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-as-invalid-instance-dir"));
        }
    }
}
