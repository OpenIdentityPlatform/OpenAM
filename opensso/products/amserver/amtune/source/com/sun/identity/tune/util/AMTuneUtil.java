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
 * $Id: AMTuneUtil.java,v 1.14 2009/05/04 23:34:10 ykwon Exp $
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.tune.util;

import com.sun.identity.tune.common.FileHandler;
import com.sun.identity.tune.common.MessageWriter;
import com.sun.identity.tune.common.OutputReaderThread;
import com.sun.identity.tune.common.AMTuneException;
import com.sun.identity.tune.common.AMTuneFileFilter;
import com.sun.identity.tune.common.AMTuneLogger;
import com.sun.identity.tune.config.AMTuneConfigInfo;
import com.sun.identity.tune.config.WS7ContainerConfigInfo;
import com.sun.identity.tune.constants.DSConstants;
import com.sun.identity.tune.constants.AMTuneConstants;
import com.sun.identity.tune.constants.WebContainerConstants;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This class contains all the utility functions for Tuning.
 */

  public class AMTuneUtil implements AMTuneConstants {
    private static AMTuneLogger pLogger;
    private static MessageWriter mWriter;
    private static boolean isWindows2003;
    private static boolean isWinVista;
    private static boolean isWindows2008;
    private static boolean isSunOs;
    private static boolean isLinux;
    private static boolean isAix;
    private static String tempFile;
    private static Map sysInfoMap;
    private static boolean utilInit = true;
    private static String osArch;
    private static boolean isNiagara_I = false;
    private static boolean isNiagara_II = false;
    private static boolean isNiagara_II_Plus = false;
    private static String date;
    private static ResourceBundle rb = null;
    public static String TMP_DIR;
    
    static {
          pLogger = AMTuneLogger.getLoggerInst();
    }
    /**
     * Initializes utils
     *
     * @return Returns <code>true<\code> isf initialization is successfull.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public static boolean initializeUtil()
    throws AMTuneException {
        date = new SimpleDateFormat(
                      "MM/dd/yyyy hh:mm:ss:SSS a zzz").format(new Date());
        mWriter = MessageWriter.getInstance();
        checkSystemEnv();
        setTmpDir();
        tempFile = AMTuneUtil.TMP_DIR + "perftune-temp.txt";
        sysInfoMap = new HashMap();
        try {
            if (isWindows()) {
                getWinSystemInfo();
                //remove the temp file
                File temp = new File(tempFile);
                if (temp.isFile()) {
                    temp.delete();
                }
            } else if (isSunOs()) {
                checkRootUser();
                getSunOSSystemInfo();
            } else if (isLinux()) {
                checkRootUser();
                getLinuxSystemInfo();
            } else if (isAix) {
                checkRootUser();
                getAIXSystemInfo();
            } else {
                utilInit = false;
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-unsupported-os"));
            }
            pLogger.log(Level.FINEST, "initializeUtil", 
                    "System configuration : " + sysInfoMap.toString());
        } catch (Exception ex) {
            pLogger.logException("perftuneutilinit", ex);
            utilInit = false;
            throw new AMTuneException(ex.getMessage());
        }
        return utilInit;
    }
    
    /**
     * Checks local box environment 
     */
    private static void checkSystemEnv() 
    throws AMTuneException {
        mWriter.writelnLocaleMsg("pt-checking-system-env");
        osArch = System.getProperty("os.arch");
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        pLogger.log(Level.INFO, "checkSystemEnv", "OS name :" + osName);
        pLogger.log(Level.INFO, "checkSystemEnv", "OS Version :" + osVersion);
        pLogger.log(Level.INFO, "checkSystemEnv", "OS Arch :" + osArch);
        if (osName.equalsIgnoreCase(SUN_OS)){
            isSunOs = true;
            if (osArch.contains("sparc") || osArch.contains("86")) {
                int solVersion = Integer.parseInt(osVersion.replace("5.", ""));
                if (solVersion > 8) {
                    return;
                } else {
                    throw new AMTuneException(AMTuneUtil.getResourceBundle()
                            .getString("pt-unsupported-sol"));
                }
            } else {
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-unsupported-arch"));
            }
        } else if (osName.equalsIgnoreCase(WINDOWS_2003)) {
            isWindows2003 = true;
        } else if (osName.equalsIgnoreCase(WINDOWS_VISTA)) {
            isWinVista = true;
        } else if (osName.equalsIgnoreCase(WINDOWS_2008)) {
            isWindows2008 = true;
        } else if (osName.equalsIgnoreCase(LINUX)) {
            isLinux = true;
        } else if (osName.equalsIgnoreCase(AIX_OS)) {
            isAix = true;
        } else {
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-unsupported-os"));
        }
    }
    
    /**
     * Executed /usr/sbin/prtconf command and finds AIX system information.
     */
    private static void getAIXSystemInfo()
    throws AMTuneException {
        getCommonNXSystemInfo();
        String nCmd = "/usr/sbin/prtconf";
        String memSizeStr = "Memory Size: ";
        String noProcessorStr = "Number Of Processors:";
        StringBuffer rBuf = new StringBuffer();
        int extVal = executeCommand(nCmd, rBuf);
        if (extVal == -1) {
            mWriter.writelnLocaleMsg("pt-cannot-proceed");
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-unable-avmem"));
        } else {
            try {
                StringTokenizer st = new StringTokenizer(rBuf.toString(),
                        "\n");
                while (st.hasMoreTokens()) {
                    String reqLine = st.nextToken();
                    if (reqLine.indexOf(memSizeStr) == 0) {
                        String size = reqLine.replace(memSizeStr, "").
                                replace(" MB", "");
                        pLogger.log(Level.FINEST, "getAIXSystemInfo",
                                "RAM size is " + size);
                        sysInfoMap.put(MEMORY_LINE, size);
                    } else if (reqLine.indexOf(noProcessorStr) != -1) {
                        String noProcessors = reqLine.replace(noProcessorStr,
                                "");
                        if (noProcessors != null &&
                                noProcessors.trim().length() > 0) {
                            pLogger.log(Level.FINEST, "getAIXSystemInfo",
                                    "Number of Processors " + noProcessors);
                            sysInfoMap.put(PROCESSERS_LINE,
                                    noProcessors.trim());
                        }
                    }
                }
            } catch (Exception ex) {
                pLogger.log(Level.SEVERE, "getAIXSystemInfo",
                        "Error finding AIX system info : ");
                throw new AMTuneException(ex.getMessage());
            }
        }
    }
    
    /**
     * This method finds host name, domain, cpus and memory size by executing
     * native commands.
     */
    private static void getSunOSSystemInfo() 
    throws AMTuneException {
        try {
            getCommonNXSystemInfo();
            String nCmd = "/bin/uname -i";
            StringBuffer rBuf = new StringBuffer();
            int extVal = executeCommand(nCmd, rBuf);
            if (extVal == -1) {
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-unable-hw-pt"));
            } 
            String hwPlatform = rBuf.toString();
            if (hwPlatform != null && hwPlatform.trim().length() > 0) {
                sysInfoMap.put(HWPLATFORM, hwPlatform.trim());
            } else {
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-unable-hw-pt"));
            }
            nCmd = "/usr/sbin/psrinfo";
            rBuf.setLength(0);
            extVal = executeCommand(nCmd, rBuf);
            if (extVal == -1) {
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-unable-no-cpu"));
            }
            int noCpus = getWordCount(rBuf.toString(), "on-line");
            if (noCpus == 0) {
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-unable-no-cpu"));
            } else {
            	setNiagaraBoxType(hwPlatform);
                 if (isNiagara_I()) {
                     pLogger.log(Level.INFO, "getSunOSSystemInfo",
                            "Tuning T1 box");
                     if (noCpus >= DIV_NUM_CPU_NIAGARA_I) {
                        noCpus = noCpus / DIV_NUM_CPU_NIAGARA_I;
                     } else {
                        noCpus = MIN_NUM_CPU;
                     }
                 } else if (isNiagara_II() || isNiagara_II_Plus()) {
                     pLogger.log(Level.INFO, "getSunOSSystemInfo",
                             "Tuning T2 or T2 Plus box");
                     if (noCpus >= DIV_NUM_CPU_NIAGARA_II) {
                        noCpus = noCpus / DIV_NUM_CPU_NIAGARA_II;
                     } else {
                        noCpus = MIN_NUM_CPU;
                     }
                 }
                sysInfoMap.put(PROCESSERS_LINE, Integer.toString(noCpus));
            }
            rBuf.setLength(0);
            nCmd = "/usr/sbin/prtconf";
            extVal = executeCommand(nCmd, rBuf);
            if (extVal == -1) {
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-unable-avmem"));
            } else {
                StringTokenizer st = new StringTokenizer(rBuf.toString(), "\n");
                while (st.hasMoreTokens()) {
                    String reqLine = st.nextToken();
                    if (reqLine.indexOf("Memory size: ") >= 0) {
                        String size = reqLine.replace("Memory size: ", "").
                                replace(" Megabytes", "");
                        sysInfoMap.put(MEMORY_LINE, size);        
                    }
                }
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "getSunOSSystemInfo", "Error finding " +
                    "SUNOS system information ");
            throw new AMTuneException(ex.getMessage());
        }
    }
    
    /**
     * Finds hostname and domain name in *unix OS Machines.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private static void getCommonNXSystemInfo() 
    throws AMTuneException { 
        try {
            String nCmd = "/bin/domainname";
            StringBuffer rBuf = new StringBuffer();
            int extVal = executeCommand(nCmd, rBuf);
            if (extVal == -1) {
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-unable-domainname"));
            }
            String domainName = rBuf.toString();
            if ((domainName != null) && (domainName.length() > 1)) {
                sysInfoMap.put(DOMAIN_NAME_LINE, domainName.trim());
            } else {
                mWriter.writeLocaleMsg("pt-unable-domainname");
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-null-domain-name"));
            }
            nCmd = "/bin/hostname";
            rBuf.setLength(0);
            extVal = executeCommand(nCmd, rBuf);
            if (extVal == -1) {
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-unable-hostname"));
            }
            String hostName = rBuf.toString();
            if ((hostName != null) && (hostName.trim().length() > 1)) {
                if (hostName.indexOf(".") != -1) {
                    hostName = hostName.substring(0, hostName.indexOf("."));
                }
                sysInfoMap.put(HOST_NAME_LINE, hostName.trim() + "." +
                        domainName.trim());
            } else {
                mWriter.writeLocaleMsg("pt-unable-hostname");
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-null-host-name"));
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "getCommonNXSystemInfo", 
                    "Error finding hostname or domain name.");
            throw new AMTuneException(ex.getMessage());
        }
    }
    
    private static void getLinuxSystemInfo() 
    throws AMTuneException {
        try {
            getCommonNXSystemInfo();
            FileHandler fh = new FileHandler("/proc/meminfo");
            String memSize = fh.getLine("MemTotal:").replace("MemTotal:", "");
            memSize = memSize.replace("kB", "");
            memSize = memSize.trim();
            if (memSize != null ) {
                int size = Integer.parseInt(memSize) / 1024;
                sysInfoMap.put(MEMORY_LINE, Integer.toString(size)); 
            } else {
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-unable-avmem"));
            }
            FileHandler fh2 = new FileHandler("/proc/cpuinfo");
            String[] lines = fh2.getMattchingLines("processor", false);
            if (lines.length >= 0) {
                sysInfoMap.put(PROCESSERS_LINE, Integer.toString(lines.length));
            } else {
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-unable-no-cpu"));
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "getLinuxSystemInfo", "Error finding" +
                    " LINUX System information");
            throw new AMTuneException(ex.getMessage());
        }
    }
    /**
     * Checks if the user is root or not.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private static void checkRootUser() 
    throws AMTuneException {
        String userName = System.getProperty("user.name");
        mWriter.writelnLocaleMsg("pt-check-user");
        if (userName.indexOf("root") < 0) {
            mWriter.writelnLocaleMsg("pt-cannot-proceed");
            mWriter.writelnLocaleMsg("pt-should-be-root-user");
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-root-user-error"));
        }
    }
    /**
     * This method uses "systeminfo.exe" command for getting the system
     * information and constructs configuration map.
     *
     * @throws java.lang.Exception
     */

    private static void getWinSystemInfo()
    throws AMTuneException {
        try {
            String hostNameCmd = "cmd /C systeminfo > " + tempFile;
            StringBuffer rBuf = new StringBuffer();
            int extVal = executeCommand(hostNameCmd, rBuf);
            if (extVal == -1) {
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-finding-sys-info"));
            }

            FileHandler fh = new FileHandler(tempFile);
            String domainName;
            String reqLine = fh.getLine(DOMAIN_NAME_LINE);
            if ((reqLine != null) && (reqLine.length() > 1)) {
                int startIdx = reqLine.lastIndexOf(":");
                domainName = reqLine.substring(startIdx + 1).trim();
            } else {
                mWriter.writeLocaleMsg("pt-unable-domainname");
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-null-domain-name"));
            }
            String hostName;
            reqLine = fh.getLine(HOST_NAME_LINE);
            if ((reqLine != null) && (reqLine.length() > 1)) {
                int startIdx = reqLine.lastIndexOf(":");
                hostName = reqLine.substring(startIdx + 1).trim();
                domainName = domainName.replace(hostName + ".", "");
                sysInfoMap.put(HOST_NAME_LINE, hostName + "." + 
                        domainName.trim());
                sysInfoMap.put(DOMAIN_NAME_LINE, domainName);
            } else {
                mWriter.writeLocaleMsg("pt-unable-hostname");
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-null-host-name"));
            }
            String numProcessors;
            reqLine = fh.getLine(PROCESSERS_LINE);
            if ((reqLine != null) && (reqLine.length() > 1)) {
                int startIdx = reqLine.lastIndexOf(":");
                numProcessors = reqLine.substring(startIdx + 1).trim();
                numProcessors = numProcessors.substring(0, 2).trim();
                sysInfoMap.put(PROCESSERS_LINE, numProcessors);
            } else {
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-unable-no-cpu"));
            }
            String memSize;
            reqLine = fh.getLine(MEMORY_LINE);
            if ((reqLine != null) && (reqLine.length() > 1)) {
                int startIdx = reqLine.lastIndexOf(":");
                memSize = reqLine.substring(startIdx + 1).trim();
                StringTokenizer st = new StringTokenizer(memSize, " ");
                st.hasMoreTokens();
                memSize = st.nextToken();
                memSize = memSize.replace(",", "");
                sysInfoMap.put(MEMORY_LINE, memSize);
            } else {
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-unable-avmem"));
            }
            if (hostName != null && domainName != null) {
                domainName = domainName.replace(hostName.toLowerCase() + ".", 
                        "");
                sysInfoMap.put(HOST_NAME_LINE, hostName + "." + domainName);
            }
            fh.close();
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "getWinSystemInfo", "Error finding " +
                    "system information ");
            throw new AMTuneException(ex.getMessage());
        }
    }

     /**
     * Returns FQDN
     *
     * @return Returns FQDN
     */
    public static String getHostName() {
            return (String)sysInfoMap.get(HOST_NAME_LINE);
        }

    /**
     * Returns Domain Name
     *
     * @return Returns Domain Name
     */
    public static String getDomainName() {
        return (String)sysInfoMap.get(DOMAIN_NAME_LINE);
    }

    /**
     * Returns Number of CPU's in the System
     *
     * @return Returns Number of CPU's in the System
     */
    public static String getNumberOfCPUS() {
        return (String)sysInfoMap.get(PROCESSERS_LINE);
    }

    /**
     * Returns current RAM size in KB
     *
     * @return Returns current RAM size in KB
     */
    public static String getSystemMemory() {
        return (String)sysInfoMap.get(MEMORY_LINE);
    }
    
    /**
     * Return Sun OS platform.
     */
    public static String getOSPlatform() {
        return osArch;
    }
    
    /**
     * Return Hardware Platform
     */
    public static String getHardWarePlatform() {
        return (String)sysInfoMap.get(HWPLATFORM);
    }
    /**
     * Executes the command and appends the result in the result buffer
     *
     * @param command Command string to be executed
     * @param resultBuffer Buffer containing the output of the command
     * @return exitValue Positive Integer value for success and -1 if any error.
     */
    public static int executeCommand(String command,
            StringBuffer resultBuffer) {
        pLogger.log(Level.FINEST, "executeCommand", "Executing command : " +
                    command);
        try {
            Process execProcess = null;
            execProcess = Runtime.getRuntime().exec(command);
            if (resultBuffer != null) {
                resultBuffer.setLength(0);
            }
            OutputReaderThread outReaderThread =
                    new OutputReaderThread(execProcess.getInputStream());
            OutputReaderThread errorReaderThread =
                    new OutputReaderThread(execProcess.getErrorStream());
            outReaderThread.start();
            errorReaderThread.start();
            execProcess.waitFor();
            int exitValue = execProcess.exitValue();
            outReaderThread.join(3000);
            errorReaderThread.join(3000);
            execProcess.destroy();
            outReaderThread.interrupt();
            errorReaderThread.interrupt();
            boolean errorOccured = false;
            if (resultBuffer != null) {
                StringBuffer outBuffer = outReaderThread.getBuffer();
                StringBuffer errorBuffer = errorReaderThread.getBuffer();
                if (outBuffer != null && outBuffer.length() != 0) {
                    pLogger.log(Level.FINEST, "executeCommand", 
                            "Out buffer content : " + outBuffer.toString());
                    resultBuffer.append(outBuffer.toString());
                }
                if (errorBuffer!=null && errorBuffer.length() != 0) {
                    pLogger.log(Level.FINEST, "executeCommand", 
                            "Error buffer content : " + errorBuffer.toString());
                    resultBuffer.append(errorBuffer.toString());
                    errorOccured = true;
                }
            }
            if (exitValue != 0 && errorOccured){
                //In some cases original error code may be used by calling func.
                resultBuffer.append("\n Exit value:").append(exitValue);
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-cmd-error"));
            } 
            pLogger.log(Level.INFO, "executeCommand", "Command exit value " +
                    exitValue);
            return(exitValue);
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "executeCommand", "Executing command " +
                    command + " failed.");
            pLogger.logException("executeCommand", ex);
            pLogger.log(Level.SEVERE, "executeCommand", "Error is : " + 
                    resultBuffer.toString());
            resultBuffer.insert(0, ex.getMessage());
            return (-1);
        }
    }
    
    public static int executeScriptCmd(String command, 
            StringBuffer resultBuffer)
    throws AMTuneException {
        int extVal = -1;
        String tempF = AMTuneUtil.TMP_DIR + "amtunecmdhelper.sh";
        try {
            if (!AMTuneUtil.isWindows()) {
                pLogger.log(Level.FINE, "executeScriptCmd",
                        "Command in the file :" + command);
                //write the command to file and then execute the file
                //workaround as ssodm is not working directly from
                //runtime in *unix if any option contains space character.
                // -m "Sun DS with AM Schema"
                File tempSh = new File(tempF);
                BufferedWriter br =
                        new BufferedWriter(new FileWriter(tempSh));
                br.write(command);
                br.close();
                AMTuneUtil.changeFilePerm(tempF, "700");
                extVal = executeCommand(tempF, resultBuffer);
                tempSh.delete();
            }
        } catch (Exception ex) {
            throw new AMTuneException (ex.getMessage());
        } finally {
            File tempSh = new File(tempF);
            if (tempSh.isFile()) {
                tempSh.delete();
            }
        }
        return extVal;
      }

      /**
     *  Evaluates the expression in the form a/b
     *
     * @param divExp Expression to be evaluated
     * @return value of the Division.
     * @throws java.lang.NumberFormatException
     * @throws java.lang.NullPointerException
     */
    public static double evaluteDivExp(String divExp)
    throws NumberFormatException, NullPointerException {
        StringTokenizer st = new StringTokenizer(divExp, "/");
        st.hasMoreTokens();
        double operand1 = Double.parseDouble(st.nextToken().trim());
        st.hasMoreTokens();
        double operand2 = Double.parseDouble(st.nextToken().trim());
        return (double)operand1 / operand2;
    }

    /**
     *  Returns he directory size in KB
     *
     * @param directory Absolute path of the directory.
     * @return size Size of the directory in KB.
     */
    public static long getDirSize(String directory) {
        long size = 0;
        File instanceDir = new File(directory);
        String[] list = instanceDir.list();
        for (int i=0; i<list.length; i++) {
            File tFile = new File(directory + FILE_SEP + list[i]);
            if (tFile.isDirectory()) {
                size += getDirSize(directory + FILE_SEP + list[i]);
            } else {
                size += tFile.length();
            }
        }
        return size;
    }

    /**
     *  Replaces token in the buffer with the given value.
     *
     * @param buf Buffer in which token need to be replaced.
     * @param key Token that need to be replaced.
     * @param value Value.
     */
    public static void replaceToken(StringBuffer buf,
            String key,
            String value) {
        if (key == null || value == null || buf == null) {
            return;
        }
        int loc = 0, keyLen = key.length(), valLen = value.length();
        while ((loc = buf.toString().indexOf(key, loc)) != -1) {
            buf.replace(loc, loc + keyLen, value);
            loc = loc + valLen;
        }
    }

    /**
     * Returns random string.
     *
     * @return Random String.
     */
    public static String getRandomStr() {
	String DATE_FORMAT = "yyyy-MM-dd-HH-mm-ss";
	DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        String rand = df.format(new Date(System.currentTimeMillis()));
        return rand;
    }

    /**
     * Copies source file to destination file
     *
     * @param source File to be copied
     * @param dest Destination File name
     * @throws java.lang.Exception if any errors occurs.
     */
    static public void CopyFile(File source, File dest)
    throws Exception {
        if (source == null || dest == null) {
            throw new IllegalArgumentException();
        }
        pLogger.log(Level.FINEST, "CopyFile", "Copying file from " +
                source.toString() + " to " + dest.toString());
        FileInputStream fis = new FileInputStream(source);
        FileOutputStream fos = new FileOutputStream(dest);
        byte[] buf = new byte[1024];
        int i = 0;
        while ((i = fis.read(buf)) != -1) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
    }

    /**
     * Returns <code>true</code> if OS is Windows 2003.
     *
     * @return <code>true</code> if OS is Windows 2003.
     */
    public static boolean isWindows() {
        if (isWinVista || isWindows2003 || isWindows2008) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Returns <code>true</code> if SUNOS.
     *
     * @return <code>true</code> if SUNOS.
     */
    public static boolean isSunOs() {
        return isSunOs;
    }
    
    /**
     * Return <code>true</code> if Linux.
     * 
     * @return <code>true</code> if Linux
     */
    public static boolean isLinux() {
        return isLinux;
    }
    
    /**
     * Return <code>true</code> if AIX.
     * 
     * @return <code>true</code> if AIX.
     */
    public static boolean isAIX() {
        return isAix;
    }
    /**
     * Return true if under laying hardware is Niagara box.
     * 
     */
    public static boolean isNiagara() {
        return isNiagara_I || isNiagara_II || isNiagara_II_Plus;
    }
    
    /**
     * Return true for T1000 and T2000.
     */
    
    public static boolean isNiagara_I() {
        return isNiagara_I;
    }
    
    /**
     * Return true for T5120 and T5220.
     */
    public static boolean isNiagara_II() {
        return isNiagara_II;
    }
    
    /**
     * Return true for T5140,T5240 and T5440.
     */
    public static boolean isNiagara_II_Plus() {
        return isNiagara_II_Plus;
    }
    
    private static void setNiagaraBoxType(String hwPlatform) {
        pLogger.log(Level.FINEST, "setNiagaraBoxType", "Finding box type");
        if (hwPlatform != null) {
            if (hwPlatform.indexOf(NIAGARA_I_T1000) != -1 ||
                    hwPlatform.indexOf(NIAGARA_I_T2000) != -1) {
                isNiagara_I = true;
                pLogger.log(Level.FINEST, "setNiagaraBoxType", "T1 ");
            } else if (hwPlatform.indexOf(NIAGARA_II_T5120) != -1 ||
                    hwPlatform.indexOf(NIAGARA_II_T5220) != -1) {
                isNiagara_II = true;
                pLogger.log(Level.FINEST, "setNiagaraBoxType", "T2");
            } else if (hwPlatform.indexOf(NIAGARA_II_PLUS_T5140) != -1 ||
                    hwPlatform.indexOf(NIAGARA_II_PLUS_T5240) != -1 ||
                    hwPlatform.indexOf(NIAGARA_II_PLUS_T5440) != -1) {
                isNiagara_II_Plus = true;
                pLogger.log(Level.FINEST, "setNiagaraBoxType", "T2 Plus");
            }
        }
    }
    /**
     *  Returns last token in the string.
     *
     * @param stream String from which last token is required.
     * @param delim Delimiter to be used for creating the tokens.
     * @return val Last Token.
     */
    public static String getLastToken(String stream, String delim) {
        String val = " ";
        if (stream != null && stream.trim().length() > 0) {
            StringTokenizer st = new StringTokenizer(stream, delim);
            while (st.hasMoreTokens()) {
                val = st.nextToken().trim();
            }
        }
        return val;
    }

    /**
     * This method copies the configuration file to directory specified by
     * second parameter under current directory.  
     * If the backup directory is not present it creates new directory
     * under current directory.
     *
     * @param confFile Configuration file name.
     * @param backupDir Directory name where config File need to be copied.
     *      This should be directory name only path should not be given.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public static void backupConfigFile(String confFile, String backupDir)
    throws AMTuneException {
        try {
            File confF = new File(confFile);
            if (!confF.isFile()) {
                mWriter.writelnLocaleMsg("pt-conf-file-missing");
                throw new AMTuneException("Config file " + confFile +
                        " is missing.");
            }
            File bkDir = new File (getCurDir() + ".." +FILE_SEP +".." + 
                    FILE_SEP + backupDir);
            if (!bkDir.isDirectory()) {
                bkDir.mkdirs();
            }
            String baseFileName = confF.getName();
            String bkFileName = bkDir + FILE_SEP + baseFileName +
                    "-orig-" + getRandomStr();
            mWriter.writeLocaleMsg("pt-bk-file");
            mWriter.writeln(" " + confFile + " to " + bkFileName);
            CopyFile(new File(confFile), new File(bkFileName));
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "backupConfigFile",
                    "Couldn't backup file.");
            throw new AMTuneException(ex.getMessage());
        }
    }
    
    public static void backupConfigFile(String confFile)
    throws AMTuneException {
        try {
            File confF = new File(confFile);
            if (!confF.exists()) {
                mWriter.writelnLocaleMsg("pt-conf-file-missing");
                throw new AMTuneException("Config file " + confFile +
                        " is missing.");
            }
            String bkDir = confF.getParent();
            String baseFileName = confF.getName();
            String bkFileName = bkDir + FILE_SEP + baseFileName +
                    "-orig-" + getRandomStr();
            mWriter.writeLocaleMsg("pt-bk-file");
            mWriter.writeln(" " + confFile + " to " + bkFileName);
            CopyFile(new File(confFile), new File(bkFileName));
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "backupConfigFile",
                    "Couldn't backup file.");
            throw new AMTuneException(ex.getMessage());
        }
    }
    
   /**
    * Returns current directory name
    *
    * @return flePath Absolute path of the current Directory.
    */

    public static String getCurDir() {
        File tempF = new File("tempdbk");
        String filePath = tempF.getAbsolutePath();
        filePath = filePath.replace("tempdbk", "");
        return filePath;
    }

    /**
     * This method is used to write buffer content into file.
     *
     * @param buf Content to be written into file
     * @param tFile temporary file name.
     * @throws com.sun.identity.tune.common.AMTuneException if any error
     * occurs while writing the content into file
     */

    public static void writeResultBufferToTempFile(StringBuffer buf,
            String tFile)
    throws AMTuneException {
        try {
            FileWriter fw = new FileWriter(new File(tFile));
            fw.write(buf.toString());
            fw.flush();
            fw.close();

        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "writeResultBufferToTempFile",
                    "Couldn't write buffer to file.");
            throw new AMTuneException(ex.getMessage());
        }
    }
    
    public static String getASJVMOption(List curOptList, String reqFlag, 
            boolean lastToken) {
        Iterator optItr = curOptList.iterator();
        while (optItr.hasNext()) {
            String curFlag = (String) optItr.next();
            if (curFlag.indexOf(reqFlag) != -1) {
                if (lastToken) {
                    return getLastToken(curFlag, PARAM_VAL_DELIM);
                } else {
                    return curFlag;
                }
            }
        }
        return NO_VAL_SET;
    }
    
    /**
     * Count number of times a word has repeated
     */
    public static int getWordCount(String str, String reqWord) {
        StringTokenizer st = new StringTokenizer(str, "\n");
        pLogger.log(Level.FINEST, "getWordCount", "Source string :" + str);
        pLogger.log(Level.FINEST, "getWordCount", "Word to find :" + reqWord);
        int count = 0;
        while (st.hasMoreTokens()) {
            String reqStr = st.nextToken();
            if (reqStr.contains(reqWord)) {
                ++count;
            }
        }
        return count;
    }
    
    /**
     * Returns matched pattern lines.
     */
      public static String[] getMatchedLines(String[] lines, String pattern) {
          List matList = new ArrayList();
          Pattern p = Pattern.compile(pattern);
          int size = lines.length;
          int i = 0;
          for (i = 0; i < size; i++) {
              Matcher m = p.matcher(lines[i]);
              if (m.find()) {
                  matList.add(lines[i]);
              }
          }
          String[] arr = new String[matList.size()];
          for (i = 0; i < matList.size(); i++) {
              arr[i] = matList.get(i).toString();
          }
          return arr;
      }
      
      /**
       * Return matching line.
       */
      public static String getMatchedLine(List lines, String pattern) {
          Iterator itr = lines.iterator();
          while (itr.hasNext()) {
              String curLine = itr.next().toString();
              if (curLine.indexOf(pattern) != -1) {
                  return curLine;
              }
          }
          return null;
      }
      /**
       * Return current date
       */
      public static String getTodayDateStr() {
          return date;
      }
      
      /**
     * Print error message.
     * @param propertyName
     */
    public static void printErrorMsg(String propertyName) {
        mWriter.writelnLocaleMsg("pt-cannot-proceed");
        mWriter.writeLocaleMsg("pt-conf-parm-cust-msg");
        mWriter.writeln(propertyName);
    }
    
    /**
     * Return tokens in a string
     * 
     */
    public static List getTokensList(String line, String delim) {
        StringTokenizer str = new StringTokenizer(line, delim);
        List tokens = new ArrayList();
        while (str.hasMoreTokens()) {
            tokens.add(str.nextToken());
        }
        return tokens;
    }
    
    /**
     * This method restarts the Web Server 7 using wadm tool.
     */
    public static void reStartWS7Serv(WS7ContainerConfigInfo wsConfigInfo) {
        try {
            pLogger.log(Level.INFO, "reStartServ", "Deploying configuration.");
            StringBuffer restartCmd = 
                    new StringBuffer(wsConfigInfo.getWSAdminCmd());
            restartCmd.append(WebContainerConstants.WADM_RESTART_SUB_CMD);
            restartCmd.append(wsConfigInfo.getWSAdminCommonParams());
            String passwordStr = 
                    WebContainerConstants.WS7ADMIN_PASSWORD_SYNTAX + 
                    wsConfigInfo.getWsAdminPass();
            StringBuffer resultBuffer = new StringBuffer();
            int retVal = executeCommand(restartCmd.toString(), passwordStr,
                    wsConfigInfo.getWSAdminPassFilePath(),
                    resultBuffer);
            if (retVal == -1) {
                mWriter.writelnLocaleMsg("pt-error-ws-deployment-failed");
                pLogger.log(Level.SEVERE, "reStartServ",
                        "Error executing command " + restartCmd);
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "reStartServ",
                    "Restart failed. " + ex.getMessage());
        }
    }
    /**
     * Return true if tuning is supported for web container
     * @param webContainer Short form of the web container
     * @return
     */
    public static boolean isSupportedWebContainer(String webContainer) {
        if (webContainer.equalsIgnoreCase(
                WebContainerConstants.WS7_CONTAINER) ||
                webContainer.equalsIgnoreCase(
                WebContainerConstants.AS91_CONTAINER)) {
            return true;
        } else {
            return false;
        }
            
    }
    
    /**
     * Return true if the DS version is supported for storing User information.
     * 
     */
    public static boolean isSupportedUMDSVersion(String dsVersion) {
        if ((dsVersion.indexOf(DSConstants.DS6_VERSION) != -1 ||
                dsVersion.indexOf(DSConstants.DS5_VERSION)!= -1) && 
                !dsVersion.equalsIgnoreCase(DSConstants.DS62_VERSION)) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Set temp Directory based on the platform
     */
    private static void setTmpDir() {
        if (isSunOs() || isLinux() || isAIX()) {
            TMP_DIR = "/tmp" + FILE_SEP;
        } else {
            TMP_DIR = System.getProperty("java.io.tmpdir");
        }
    }
    
    /**
     * This method returns all the files in a given directory and its 
     * sub directories.  
     * @param directory name of the directory.
     * @return file names in the form of List.
     */
    public static List getFileList(String directory) 
    throws AMTuneException {
        List allFiles = new ArrayList();
        File instanceDir = new File(directory);
        if (instanceDir != null && !instanceDir.isDirectory()) {
            pLogger.log(Level.SEVERE, "getFileList", "Directory not present :" +
                    directory);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-not-valid-dir"));
        }
        String[] list = instanceDir.list();
        for (int i = 0; i < list.length; i++) {
            File tFile = new File(directory + FILE_SEP + list[i]);
            if (tFile.isDirectory()) {
                List curList = getFileList(tFile.toString());
                Iterator itr = curList.iterator();
                while (itr.hasNext()) {
                    allFiles.add(itr.next().toString());
                }
            }
            allFiles.add(tFile.toString());
        }
        pLogger.log(Level.FINEST, "getFileList", "Returning files " + 
                allFiles.toString());
        return allFiles;
    }
    
    /**
     * Creates zip file from a given directory
     * @param directory
     * @param zipName
     * @return
     */
    public static String createZipFile(String directory, String zipName) 
    throws AMTuneException {
        ZipOutputStream out = null;
        FileInputStream in = null;
        File zipFile = new File(zipName + ".zip");
        if (zipFile.isFile()) {
            pLogger.log(Level.FINEST, "createZipFile", 
                    "Deleting existing zip file.");
            zipFile.delete();
        }
        try {
            List entries = getFileList(directory);
            byte[] buffer = new byte[4096]; // Create a buffer for copying
            int bytesRead;
            out = new ZipOutputStream(new FileOutputStream(zipFile));
            Iterator itr = entries.iterator();
            while(itr.hasNext()) {
                File f = new File(itr.next().toString());
                pLogger.log(Level.FINEST, "createZipFile", "Zipping file " +
                        f.toString());
                if (f.isDirectory())
                    continue;//Ignore directory
                in = new FileInputStream(f);
                ZipEntry entry = new ZipEntry(f.getPath());
                out.putNextEntry(entry);
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                in.close();
            }
           return zipFile.getAbsolutePath();
        } catch(Exception ex) {
            pLogger.log(Level.SEVERE, "createZipFile", 
                    "Exception while creating zip file " + ex.getMessage());
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-zip-file"));
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch(Exception ex) {
                    //ignore
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ex) {
                    //ignore
                }
            }
        }
    }
    
    /**
     * Deletes files in a given directory.
     * @param directory
     */
    public static void deleteDirectory(String directory) {
        try {
            List delFiles = getFileList(directory);
            Iterator dlItr = delFiles.iterator();
            while (dlItr.hasNext()) {
                File delFile = new File(dlItr.next().toString());
                delFile.delete();
            }
            File delD = new File(directory);
            delD.delete();
        } catch (Exception ex) {
            pLogger.log(Level.WARNING, "deleteDirectory", "Error occured " +
                    "while deleting the files :" + ex.getMessage());
        }
    }
    
    /**
     * Creates zip file for tuning remote directory.
     * @param configInfo
     */
    public static void createRemoteDSTuningZipFile(AMTuneConfigInfo configInfo){
        String baseDir = "remotedstune" + FILE_SEP;
        try {
            pLogger.log(Level.INFO, "createRemoteDSTuningZipFile", 
                    "Creating amtune.zip file.");
            mWriter.writelnLocaleMsg("pt-ds-create-tar");
            String curDir = AMTuneUtil.getCurDir();
            String zBinDir = baseDir + "bin";
            String zLibDir =  baseDir + "lib";
            String zLocaleDir = baseDir + "resources";
            String zLdifDir = baseDir + "ldif";
            String zBinWinDir = zBinDir + FILE_SEP + "windows";
            String zBinUnxDir = zBinDir + FILE_SEP + "unix";
            String reqDirs[] = { zBinDir, zLibDir, zLocaleDir, zLdifDir, 
                zBinWinDir, zBinUnxDir };
            //create layout
            for (int i = 0; i < reqDirs.length; i ++) {
                File dir = new File (reqDirs[i]);
                if (!dir.isDirectory()) {
                    pLogger.log(Level.FINEST, "createRemoteDSTuningZipFile",
                            "Creating directory " + dir.toString());
                    dir.mkdirs();
                }
            }
            String filesToCopy[][] = { 
                {"../../../lib/amtune.jar", zLibDir + FILE_SEP + "amtune.jar"},
                {"../../../lib/opensso-sharedlib.jar", zLibDir + FILE_SEP +
                         "opensso-sharedlib.jar"},
                {"../../../template/unix/bin/amtune/amtune.template",
                         zBinUnxDir + FILE_SEP + "amtune"},
                {"../../../template/windows/bin/amtune/amtune.bat.template",
                         zBinWinDir + FILE_SEP + "amtune.bat"},
                {"../../../template/unix/bin/amtune/" +
                         "amtune-env.properties.template",
                         zBinUnxDir + FILE_SEP + "amtune-env.properties"},
                {"../../../template/windows/bin/amtune/" +
                         "amtune-env.properties.template",
                         zBinWinDir + FILE_SEP + "amtune-env.properties"},
                {"../../../template/unix/bin/amtune/" +
                         "amtune-samplepasswordfile.template",
                         zBinUnxDir + FILE_SEP + "amtune-samplepasswordfile"},
                {"../../../template/windows/bin/amtune/" +
                         "amtune-samplepasswordfile.template",
                         zBinWinDir + FILE_SEP + "amtune-samplepasswordfile"},
            };
            for (int i = 0; i < filesToCopy.length; i++) {
                File source = new File(filesToCopy[i][0]);
                File dest = new File(filesToCopy[i][1]);
                //If datastore is "generic ldapv3" then index.ldif and
                //fam_sds_index.ldif files will not be present.
                if (source.exists()) {
                    AMTuneUtil.CopyFile(source, dest);
                } else {
                    pLogger.log(Level.INFO, "createRemoteDSTuningZipFile",
                            "File " + source + " not found.");
                }
            }
            
            File localeFiles = new File (curDir + "../../../resources");
            File lFiles[] = localeFiles.listFiles(
                    new AMTuneFileFilter("amtune"));
            for (int i = 0; i < lFiles.length; i++) {
                String fileName = lFiles[i].getName();
                AMTuneUtil.CopyFile(lFiles[i], new File(zLocaleDir + FILE_SEP +
                        fileName));
            }
            updateDSParamsInEnvFile(configInfo, 
                    zBinWinDir + FILE_SEP + "amtune-env.properties");
            updateDSParamsInEnvFile(configInfo, 
                    zBinUnxDir + FILE_SEP + "amtune-env.properties");
            String zipPath = AMTuneUtil.createZipFile(baseDir, "amtune");
            mWriter.writeLocaleMsg("pt-ds-tar-file-location");
            mWriter.writeln(" " + zipPath);
            mWriter.writelnLocaleMsg("pt-ds-steps");
            mWriter.writelnLocaleMsg("pt-ds-copy-tar-file");
            mWriter.writelnLocaleMsg("pt-ds-untar-file");
            mWriter.writelnLocaleMsg("pt-ds-set-values");
            mWriter.writelnLocaleMsg("pt-ds-set-env-values");
            mWriter.writelnLocaleMsg("pt-ds-execute-review-mode");
            mWriter.writelnLocaleMsg("pt-ds-review");
            mWriter.writelnLocaleMsg("pt-ds-change-mode");
            mWriter.writeln(PARA_SEP);
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "createRemoteDSTuningZipFile",
                    "Error creating amtune.zip file." + ex.getMessage());
        } finally {
            deleteDirectory(baseDir);
        }
    }
    
    /**
     * This method replace required properties in amtune-env.properties file
     * @param configInfo
     * @param fileName
     */
    private static void updateDSParamsInEnvFile(AMTuneConfigInfo configInfo,
            String fileName) {
        FileHandler fh = null;
        try {
            fh = new FileHandler(fileName);
            
            String propName = DSConstants.DS_HOST + "=";
            int reqIdx = fh.getLineNum(propName);
            String reqLine = fh.getLine(reqIdx);
            reqLine = reqLine.replace(propName, propName + 
                    configInfo.getDSConfigInfo().getDsHost());
            fh.replaceLine(reqIdx, reqLine);
            
            propName = DSConstants.DS_PORT + "=";
            reqIdx = fh.getLineNum(propName);
            reqLine = fh.getLine(reqIdx);
            reqLine = reqLine.replace(propName, propName + 
                    configInfo.getDSConfigInfo().getDsPort());
            fh.replaceLine(reqIdx, reqLine);
            
            propName = DSConstants.ROOT_SUFFIX + "=";
            reqIdx = fh.getLineNum(propName);
            reqLine = fh.getLine(reqIdx);
            reqLine = reqLine.replace(propName, propName + 
                    configInfo.getDSConfigInfo().getRootSuffix());
            fh.replaceLine(reqIdx, reqLine);
            
            propName = DSConstants.DS_VERSION + "=";
            reqIdx = fh.getLineNum(propName);
            reqLine = fh.getLine(reqIdx);
            reqLine = reqLine.replace(propName, propName + 
                    configInfo.getDSConfigInfo().getDsVersion());
            fh.replaceLine(reqIdx, reqLine);
            
            propName = DSConstants.DIRMGR_BIND_DN + "=";
            reqIdx = fh.getLineNum(propName);
            reqLine = fh.getLine(reqIdx);
            reqLine = reqLine.replace(propName, propName + 
                    configInfo.getDSConfigInfo().getDirMgrUid());
            fh.replaceLine(reqIdx, reqLine);
            
            propName = DSConstants.DS_TOOLS_DIR + "=";
            reqIdx = fh.getLineNum(propName);
            reqLine = fh.getLine(reqIdx);
            String toolsBinDir = configInfo.getDSConfigInfo()
                    .getDSToolsBinDir();
            if (toolsBinDir != null && toolsBinDir.trim().length() > 0 ) {
                reqLine = reqLine.replace(propName, propName + toolsBinDir);
                fh.replaceLine(reqIdx, reqLine);
            }
            
            propName = DSConstants.DS_INSTANCE_DIR + "=";
            reqIdx = fh.getLineNum(propName);
            reqLine = fh.getLine(reqIdx);
            String instDir = configInfo.getDSConfigInfo()
                    .getDsInstanceDir();
            if (instDir != null && instDir.trim().length() > 0 ) {
                reqLine = reqLine.replace(propName, propName + instDir);
                fh.replaceLine(reqIdx, reqLine);
            }
            
            propName = AMTuneConstants.AMTUNE_MODE + "=";
            reqIdx = fh.getLineNum(propName);
            reqLine = fh.getLine(reqIdx);
            reqLine = reqLine.replace(propName, propName + "REVIEW");
            fh.replaceLine(reqIdx, reqLine);
            
            propName = DSConstants.AMTUNE_TUNE_DS + "=";
            reqIdx = fh.getLineNum(propName);
            reqLine = fh.getLine(reqIdx);
            reqLine = reqLine.replace(propName, propName +
                    configInfo.isTuneDS());
            fh.replaceLine(reqIdx, reqLine);
            
            propName = DSConstants.AMTUNE_TUNE_OS + "=";
            reqIdx = fh.getLineNum(propName);
            reqLine = fh.getLine(reqIdx);
            reqLine = reqLine.replace(propName, propName + "false");
            fh.replaceLine(reqIdx, reqLine);
            
            propName = DSConstants.AMTUNE_TUNE_WEB_CONTAINER + "=";
            reqIdx = fh.getLineNum(propName);
            reqLine = fh.getLine(reqIdx);
            reqLine = reqLine.replace(propName, propName + "false");
            fh.replaceLine(reqIdx, reqLine);
            
            propName = DSConstants.AMTUNE_TUNE_IDENTITY + "=";
            reqIdx = fh.getLineNum(propName);
            reqLine = fh.getLine(reqIdx);
            reqLine = reqLine.replace(propName, propName + "false");
            fh.replaceLine(reqIdx, reqLine);
            
            propName = DSConstants.AMTUNE_LOG_LEVEL + "=";
            reqIdx = fh.getLineNum(propName);
            reqLine = fh.getLine(reqIdx);
            reqLine = reqLine.replace(propName, propName + "FILE");
            fh.replaceLine(reqIdx, reqLine);
            
        } catch (Exception ex) {
            pLogger.log(Level.WARNING, "replaceDSParamsInEnvFile",
                    "Error replacing DS params: " + ex.getMessage());
        } finally {
            if (fh != null) {
                try {
                    fh.close();
                } catch (Exception ex) {
                    //ignore
                }
            }
        }
    }
    /**
     * Changes the file permission 
     * @param fileName Absolute path of the file
     * @param perm permissions
     */
    public static void changeFilePerm(String fileName, String perm) {
        if (!isWindows()) {
            StringBuffer rBuff = new StringBuffer();
            int extVal = executeCommand("chmod " + perm + " " + fileName,
                    rBuff);
        } 
    }
    
    /**
     * Checks if the password file have readonly permissions by owner only.
     * @param fileName
     * @throws com.sun.identity.tune.common.AMTuneException if the file doesn't
     * have readonly by owner only. 
     */
    public static void validatePwdFilePermissions(String fileName)
    throws AMTuneException {
        if (System.getProperty("path.separator").equals(":")) {
            StringBuilder lsCmd = new StringBuilder("/bin/ls");
            lsCmd.append(" -l ");
            lsCmd.append(fileName);
            StringBuffer rBuf = new StringBuffer();
            executeCommand(lsCmd.toString(), rBuf);
            String s = rBuf.toString();
            if (s != null) {
                int idx = s.indexOf(" ");
                if (idx != -1) {
                    String permission = s.substring(0, idx);
                    if (!permission.equals("-r--------")) {
                        String msg = getResourceBundle().getString(
                                "pt-error-password-file-not-readonly");
                        Object[] param = {fileName};
                        throw new AMTuneException(MessageFormat.format(
                                msg, param));
                    }
                }
            }
        }
    }
    
    /**
     * Returns amtune resource bundle
     */
    public static ResourceBundle getResourceBundle() 
    throws AMTuneException {
        try {
            if (rb == null) {
               rb = ResourceBundle.getBundle(AMTuneConstants.RB_NAME);
            }
        } catch(MissingResourceException me) {
            throw new AMTuneException("Couldn't find resource file: " +
                    AMTuneConstants.RB_NAME);
        }
        return rb;
    }
    
    /**
     * Writes password to file
     * @param password Password string to be written into file
     * @param passFilePath Absolute path of the file.
     * @throws java.io.IOException
     */
      public static void writePasswordToFile(String password, 
              String passFilePath)
      throws IOException {
          pLogger.log(Level.FINE, "writePasswordToFile", "Creating " +
                  "password file.");
          File passFile = new File(passFilePath);
          BufferedWriter pOut =
                  new BufferedWriter(new FileWriter(passFile));
          pOut.write(password);
          pOut.flush();
          pOut.close();
          AMTuneUtil.changeFilePerm(passFilePath, "400");
      }
    
    /**
     * Deletes the file
     * @param filePath Absolute path of the file
     */
    public static void deleteFile(String filePath) {
        File f = new File(filePath);
        if (f.isFile()) {
            f.delete();
        }
    }
    
    /**
     * Wrapper function to execute cmd which requires password.  This method 
     * writes password to file and deletes the file after the 
     * execution is completed.
     * @param cmd Command to execute
     * @param password Password for executing command 
     * @param passFilePath Absolute path for the password file.
     * @param resultBuffer Command execution result.
     * @return command exit value.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public static int executeCommand(String cmd, String password, 
            String passFilePath, StringBuffer resultBuffer) 
    throws AMTuneException {
        int extVal = -1;
        try {
            writePasswordToFile(password, passFilePath);
            extVal = executeCommand(cmd, resultBuffer);
        } catch (IOException ioe) {
            pLogger.log(Level.SEVERE, "executeScriptCmd", 
                    "Error creating password file " + ioe.getMessage());
        } finally {
            deleteFile(passFilePath);
        }
        return extVal;
    }
    
    /**
     * Wrapper function to execute commands.  This method writes password 
     * to file and deletes the file after the execution is completed.
     * @param cmd Command to execute
     * @param password Password for executing command 
     * @param passFilePath Absolute path for the password file.
     * @param resultBuffer Command execution result.
     * @return command exit value.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public static int executeScriptCmd(String cmd, String password, 
            String passFilePath, StringBuffer resultBuffer) 
    throws AMTuneException {
        int extVal = -1;
        try {
            writePasswordToFile(password, passFilePath);
            extVal = executeScriptCmd(cmd, resultBuffer);
        } catch (IOException ioe) {
            pLogger.log(Level.SEVERE, "executeScriptCmd", 
                    "Error writing password to file " + ioe.getMessage());
        } finally {
            deleteFile(passFilePath);
        }
        return extVal;
    }
}
