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
 * $Id: AMTuneConstants.java,v 1.13 2009/12/09 00:29:34 ykwon Exp $
 */

package com.sun.identity.tune.constants;

/**
 * This interface defines the keys in amtune-env.properties as constants.
 * 
 */
public interface AMTuneConstants {
    static String ENV_FILE_NAME = "amtune-env";
    static String CMD_OPTION1 = "inputfile";
    static String CMD_OPTION2 = "debug";
    static String RB_NAME = "amtune";
    static String WINDOWS_2003 = "Windows 2003";
    static String WINDOWS_VISTA = "Windows Vista";
    static String WINDOWS_2008 = "Windows Server 2008";
    static String AIX_OS = "AIX";
    static String LINUX ="Linux";
    static String SUN_OS = "SUNOS";
    static String HOST_NAME_LINE = "Host Name:";
    static String DOMAIN_NAME_LINE = "Domain:";
    static String PROCESSERS_LINE = "Processor";
    static String MEMORY_LINE = "Total Physical Memory:";
    static String HWPLATFORM = "HWPLATFORM";
    //Niagara box models
    static String NIAGARA_I_T1000 = "T100";
    static String NIAGARA_I_T2000 = "T200";
    static String NIAGARA_II_T5120 = "T5120";
    static String NIAGARA_II_T5220 = "T5220";
    static String NIAGARA_II_PLUS_T5140 = "T5140";
    static String NIAGARA_II_PLUS_T5240 = "T5240";
    static String NIAGARA_II_PLUS_T5440 = "T5440";
    //Niagara or CMT's hardware threads per core and scaling factors
    static int DIV_NUM_CPU_NIAGARA_I = 4;
    static int DIV_NUM_CPU_NIAGARA_II = 8;
    static int MIN_NUM_CPU = 1;

    static String LOG_DIR = "logs";
    static String ROOT_REALM = "/";
    static String FILE_SEP = "/";
    static String PARAM_VAL_DELIM = "=";
    static String AMTUNE_MODE = "AMTUNE_MODE";
    static String AMTUNE_LOG_LEVEL = "AMTUNE_LOG_LEVEL";
    static String AMTUNE_TUNE_OS = "AMTUNE_TUNE_OS";
    static String AMTUNE_TUNE_WEB_CONTAINER = "AMTUNE_TUNE_WEB_CONTAINER";
    static String AMTUNE_TUNE_DS = "AMTUNE_TUNE_DS";
    static String AMTUNE_TUNE_IDENTITY = "AMTUNE_TUNE_OPENSSO";
    static String DEFAULT_SDK_CACHE_MAX_SIZE = "10000";
    
    static int AMTUNE_MIN_PERM_SIZE_WS7 = 400;
    static int AMTUNE_MIN_PERM_SIZE_WS61 = 400;
    static int AMTUNE_MIN_PERM_SIZE_AS8 = 400;
    static int AMTUNE_AVG_PER_ENTRY_CACHE_SIZE_IN_KB = 8;
    static int AMTUNE_AVG_PER_SESSION_SIZE_IN_KB = 6;
    /**
     * Out the memory available for Java part of the OpenSSO
     * process memory, the following is the breakdown of memory needs
     */
    static double AMTUNE_MEM_MAX_NEW_SIZE = (double) 1/8;
    static double AMTUNE_MEM_MAX_PERM_SIZE = (double) 1/12;
    static double AMTUNE_MEM_THREADS_SIZE= (double) 1/16;
    static double AMTUNE_MEM_OPERATIONAL = (double) 19/48;
    static double AMTUNE_MEM_CACHES_SIZE = (double) 1/3;
    
    /**
     * Out of the memory available for OpenSSO Caches, 
     * the breakdown b/w SDK and Session Cache size is as follows:
     * NOTE :  It's not clear how much memory policy and other caches 
     *         use.  These fall into the OPERATIONAL memory category. 
     *         Once we have an estimate on them, will adjust these values 
     *         appropriately. 
     *         OPERATIONAL memory is large enough to handle these unknown 
     *         quantities.
     *         The following breakdowns are not used right now because 
     *         SDK cache size is now recommended to be set at the default
     *         value of 10,000 entries, DEFAULT_SDK_CACHE_MAX_SIZE.  Thus, 
     *         for calculating the recommended number of session entries,
     *         AMTUNE_MEM_SESSION_CACHE_SIZE is equivalent to 
     *         AMTUNE_MEM_CACHES_SIZE.
     */
    //static double AMTUNE_MEM_SDK_CACHE_SIZE = (double) 2/3;
    //static double AMTUNE_MEM_SESSION_CACHE_SIZE = (double) 1/3;
    /**
     * From internal testing, best performance was found when notification 
     * queue size was 30% of the maximum number of sessions (here, numSessions).
     */
    static double AMTUNE_NOTIFICATION_QUEUE_CALC_FACTOR = (double)0.3;
    
    
    /** 
     * WS internal threads used. This is not really factored into any
     * calculation. But, nevertheless, its useful to know how much we estimated. 
     * Typical value for this is about 50. 
     * But, we leave one more fold contingent threads
     */
    static double AMTUNE_NUM_WS_INTERNAL_THREADS = 100;
    //Established Thread Counts 
    static int AMTUNE_NUM_JAVA_INTERNAL_THREADS = 8;
    static int AMTUNE_NUM_JAVA_APPS_DEPLOYED = 6;
    static int AMTUNE_NUM_IS_INTERNAL_THREADS = 3;

    /**
     * After all the known threads are taken into account, we still plan for 
     * about a 3rd more threads in the system.  The tuner program will figure 
     * out how much memory can be used up by threads and reserves 1/3 of them 
     * for unplanned threads
     */ 
    static double AMTUNE_THREADS_UNPLANNED = (double)1/3;
    //Known threads breakdown
    static double AMTUNE_WS_RQTHROTTLE_THREADS = (double)5/12;
    static double AMTUNE_IS_OPERATIONAL_THREADS = (double)5/12;
    static double AMTUNE_IS_AUTH_LDAP_THREADS = (double)1/24;
    static double AMTUNE_IS_SM_LDAP_THREADS = (double)1/24;

    //Some known WS and OpenSSO Defaults
    static int AMTUNE_NUM_WS_RQTHROTTLE_MIN = 10;
    static int AMTUNE_NUM_WS_THREAD_INCREMENT = 10;
    static int AMTUNE_NUM_IS_MIN_AUTH_LDAP_THREADS = 10;
    static int AMTUNE_NUM_WS_THREADS_MAX_VAL = 512;
    static int AMTUNE_NUM_WS_THREADS_MIN_VAL = 256;
    static boolean AMTUNE_STATISTIC_ENABLED = false;

    //Just plain constants
    static int AMTUNE_NUM_FILE_DESCRIPTORS = 65536;
    static int AMTUNE_NUM_TCP_CONN_SIZE = 8192;
    static int AMTUNE_NATIVE_STACK_SIZE_64_BIT = 262144;

    //AMTune Error Status Codes
    static int AMTUNE_INVALID_CMDLINE_PARAMETER = 100;
    static int AMTUNE_INVALID_ENVIRON_SETTING = 200;
    
    static String AMTUNE_LINUX_IPV4_LOCAL_PORT_RANGE = "1204 65000";
    static String AMTUNE_LINUX_CORE_RMEM_MAX = "8388608";
    static String AMTUNE_LINUX_IPV4_TCP_RMEM = "4096 131072 8388608";
    static String AMTUNE_LINUX_IPV4_TCP_WMEM = "4096 131072 8388608";
    static String AMTUNE_LINUX_IPV4_TCP_SACK = "0";
    static String AMTUNE_LINUX_IPV4_TCP_TIMESTAMPS = "0";
    static String AMTUNE_LINUX_IPV4_TCP_WIN_SCALE = "0";
    static String AMTUNE_LINUX_IPV4_TCP_KEEPALIVE_TIME = "60";
    static String AMTUNE_LINUX_IPV4_TCP_KEEPALIVE_INTVL = "75";
    static String AMTUNE_LINUX_IPV4_TCP_FIN_TIMEOUT = "30";
    static String AMTUNE_LINUX_STACK_SIZE_LIMITS = "256";
    //All password contants
    static String SSOADM_PASSWORD = "SSOADM_PASSWORD";
    static String WADM_PASSWORD = "WADM_PASSWORD";
    static String ASADMIN_PASSWORD = "ASADMIN_PASSWORD";
    static String DIRMGR_PASSWORD = "DIRMGR_PASSWORD";
    static String AMTUNE_MIN_MEMORY_TO_USE_IN_MB = 
            "AMTUNE_MIN_MEMORY_TO_USE_IN_MB";
    static String AMTUNE_MAX_MEMORY_TO_USE_IN_MB_DEFAULT =
            "AMTUNE_MAX_MEMORY_TO_USE_IN_MB_DEFAULT";
    static String AMTUNE_MAX_MEMORY_TO_USE_IN_MB_SOLARIS = 
            "AMTUNE_MAX_MEMORY_TO_USE_IN_MB_SOLARIS";
    static String AMTUNE_MAX_MEMORY_TO_USE_IN_MB_X86 = 
            "AMTUNE_MAX_MEMORY_TO_USE_IN_MB_X86";
    static String AMTUNE_PCT_MEMORY_TO_USE = "AMTUNE_PCT_MEMORY_TO_USE";
    static String AMTUNE_MEM_MAX_HEAP_SIZE_RATIO = 
            "AMTUNE_MEM_MAX_HEAP_SIZE_RATIO";
    static String AMTUNE_MEM_MIN_HEAP_SIZE_RATIO = 
            "AMTUNE_MEM_MIN_HEAP_SIZE_RATIO";
    static String AMTUNE_PER_THREAD_STACK_SIZE_IN_KB = 
            "AMTUNE_PER_THREAD_STACK_SIZE_IN_KB";
    static String AMTUNE_PER_THREAD_STACK_SIZE_IN_KB_64_BIT =
            "AMTUNE_PER_THREAD_STACK_SIZE_IN_KB_64_BIT";
    static int LDAP_VERSION = 3;
    static String NO_VAL_SET = "<No value set>";
    static String LINE_SEP = "-----------------------------------------------" + 
            "----------------------";
    static String PARA_SEP = "===============================================" +
            "======================";
    static String CHAPTER_SEP = "############################################" +
            "#########################";
    static String START_FAM_MSG = "Start: OpenSSO Tuning";
    static String END_FAM_MSG = "End: OpenSSO Tuning";
    
}
