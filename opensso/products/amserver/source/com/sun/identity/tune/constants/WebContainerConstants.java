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
 * $Id: WebContainerConstants.java,v 1.7 2009/12/09 00:34:15 ykwon Exp $
 */

package com.sun.identity.tune.constants;

/**
 * Defines the constants for WebContainer.
 * 
 */
public interface WebContainerConstants extends AMTuneConstants {
    static String WEB_CONTAINER = "WEB_CONTAINER";
    static String WS_TYPE = "WS";
    static String AS_TYPE = "AS";
    static String WS7_CONTAINER = "WS7";
    static String WS61_CONTAINER = "WS61";
    static String AS8_CONTAINER = "AS8";
    static String AS91_CONTAINER = "AS91";
    static String WS7ADMIN_PASSWORD_SYNTAX = "wadm_password=";
    static String ASADMIN_PASSWORD_SYNTAX= "AS_ADMIN_PASSWORD=";
    static String CONTAINER_INSTANCE_DIR = "CONTAINER_INSTANCE_DIR";
    static String WSADMIN_DIR = "WSADMIN_DIR";
    static String WSADMIN_USER = "WSADMIN_USER";
    static String WSADMIN_HOST = "WSADMIN_HOST";
    static String WSADMIN_PORT = "WSADMIN_PORT";
    static String WSADMIN_SECURE = "WSADMIN_SECURE";
    static String WSADMIN_CONFIG = "WSADMIN_CONFIG";
    static String WSADMIN_HTTPLISTENER = "WSADMIN_HTTPLISTENER";
    static String MIN_THREADS = "min-threads";
    static String MAX_THREADS = "max-threads";
    static String QUEUE_SIZE = "queue-size";
    static String STACK_SIZE = "stack-size";
    static String ACCEPTOR_THREADS = "acceptor-threads";
    static String ENABLED = "enabled";
    static String MIN_HEAP_FLAG = "-Xms";
    static String MAX_HEAP_FLAG = "-Xmx";
    static String GC_LOG_FLAG = "-Xloggc";
    static String SERVER_FLAG = "-server";
    static String CLIENT_FLAG = "-client";
    static String STACK_SIZE_FLAG = "-Xss";
    static String NEW_SIZE_FLAG = "-XX:NewSize";
    static String MAX_NEW_SIZE_FLAG = "-XX:MaxNewSize";
    static String DISABLE_EXPLICIT_GC_FLAG = "-XX:+DisableExplicitGC";
    static String PARALLEL_GC_FLAG = "-XX:+UseParNewGC";
    static String HISTOGRAM_FLAG = "-XX:+PrintClassHistogram";
    static String GC_TIME_STAMP_FLAG = "-XX:+PrintGCTimeStamps";
    static String MARK_SWEEP_GC_FLAG = "-XX:+UseConcMarkSweepGC";
    static String HEAPDUMP_OOM_FLAG = "-XX:+HeapDumpOnOutOfMemoryError";
    static String PRINT_CONC_LOCKS_FLAG = "-XX:+PrintConcurrentLocks";
    static String ESCAPE_ANALYSIS_FLAG = "-XX:+DoEscapeAnalysis";
    static String COMPRESSED_OOPS_FLAG = "-XX:+UseCompressedOops";
    static String PARALLEL_GC_THREADS = "-XX:ParallelGCThreads";
    static String WADM_RESTART_SUB_CMD = "restart-instance";
    static String WADM_SET_STATS_SUB_CMD = "set-stats-prop";
    static String WADM_SET_HTTP_LISTENER_SUB_CMD = "set-http-listener-prop";
    static String WADM_SET_THREAD_POOL_SUB_CMD = "set-thread-pool-prop";
    static String WADM_DEL_JVM_OPT_SUB_CMD = "delete-jvm-options";
    static String WADM_CREATE_JVM_OPT_SUB_CMD = "create-jvm-options";
    static String WADM_DEPLOY_SUB_CMD = "deploy-config";
    static String WADM_FORCE_OPT = " --force ";
    //Appserver related
    static String ASADMIN_DIR = "ASADMIN_DIR";
    static String ASADMIN_USER = "ASADMIN_USER";
    static String ASADMIN_HOST = "ASADMIN_HOST";
    static String ASADMIN_PORT = "ASADMIN_PORT";
    static String ASADMIN_SECURE = "ASADMIN_SECURE";
    static String ASADMIN_TARGET = "ASADMIN_TARGET";
    static String ASADMIN_HTTPLISTENER = "ASADMIN_HTTPLISTENER";
    static String AMTUNE_WEB_CONTAINER_JAVA_POLICY = 
            "AMTUNE_WEB_CONTAINER_JAVA_POLICY";
    static String JAVA_SECURITY_POLICY = "-Djava.security.policy";
    static String MAX_PENDING_COUNT = "max-pending-count";
    static String REQUEST_PROCESSING_INITIAL_THREAD_COUNT = 
            "initial-thread-count";
    static String REQUEST_PROCESSING_THREAD_COUNT =
            "thread-count";
    static String ACCEPTOR_THREAD_PARAM = ".acceptor-threads";
    static String REQUESTPROC_THREAD_PARAM = 
            "server.http-service.request-processing.thread-count";
    static String REQUESTPROC_INIT_THREAD_PARAM =
            "server.http-service.request-processing.initial-thread-count";
    static String COUNT_THREAD_PARAM = 
            "server.http-service.connection-pool.max-pending-count";
    static String QUEUE_SIZE_PARAM =
            "server.http-service.connection-pool.queue-size-in-bytes";
    static String AS_PARAM_DELIM = ":";
    static String GENERATE_JVM_REPORT_SUB_CMD = "generate-jvm-report";
            
}
