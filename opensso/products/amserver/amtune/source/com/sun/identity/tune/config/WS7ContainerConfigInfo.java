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
 * $Id: WS7ContainerConfigInfo.java,v 1.6 2009/12/09 00:37:35 ykwon Exp $
 */

package com.sun.identity.tune.config;

import com.sun.identity.tune.base.WebContainerConfigInfoBase;
import com.sun.identity.tune.common.FileHandler;
import com.sun.identity.tune.common.AMTuneException;
import com.sun.identity.tune.util.AMTuneUtil;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.logging.Level;

/**
 * The <code>WS7ContainerConfigInfo<\code> extends WebContainerConfigInfoBase
 * and contains WEbserver 7 configuration information.
 *
 */
public class WS7ContainerConfigInfo extends WebContainerConfigInfoBase {

    private String wsAdminDir;
    private String wsAdminCmd;
    private String wsAdminHost;
    private String wsAdminUser;
    private String wsAdminPort;
    private boolean isAdminPortSecure;
    private String wsAdminConfig;
    private String wsAdminHttpListener;
    private String adminPassFile;
    private String wsadmCommonParamsNoConfig;
    private String wsAdminCommonParams;
    private Map cfgMap;
    private String tempFile;
    private String wsAdminPass;
    private String passWordStr;

    /**
     * Constructs the object
     *
     * @param confRbl
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public WS7ContainerConfigInfo(ResourceBundle confRbl, String passFilePath)
    throws AMTuneException {
        try {
            setWebContainer(WS7_CONTAINER);
            adminPassFile = AMTuneUtil.TMP_DIR + "wsadminpass";
            tempFile = AMTuneUtil.TMP_DIR + "cmdoutput";
            setContainerInstanceDir(confRbl.getString(CONTAINER_INSTANCE_DIR));
            setWSAdminDir(confRbl.getString(WSADMIN_DIR));
            setWSAdminCmd();
            setWSAdminUser(confRbl.getString(WSADMIN_USER));
            FileHandler pHdl = new FileHandler(passFilePath);
            String reqLine = pHdl.getLine(WADM_PASSWORD);
            if (reqLine == null ||
                    (reqLine != null && 
                    reqLine.trim().length() < WADM_PASSWORD.length() + 1)) {
                mWriter.writelnLocaleMsg("pt-cannot-proceed");
                mWriter.writelnLocaleMsg("pt-ws-password-not-found-msg");
                throw new AMTuneException(AMTuneUtil.getResourceBundle().
                        getString("pt-ws-password-not-found"));
            } else {
                setWsAdminPass(AMTuneUtil.getLastToken(reqLine, "="));
                passWordStr = WS7ADMIN_PASSWORD_SYNTAX + getWsAdminPass();
            }
            setWSAdminHost(confRbl.getString(WSADMIN_HOST));
            setWSAdminPort(confRbl.getString(WSADMIN_PORT));
            setWSAdminSecure(confRbl.getString(WSADMIN_SECURE));
            setWSAdminConfig(confRbl.getString(WSADMIN_CONFIG));
            setWSAdminHTTPListener(confRbl.getString(WSADMIN_HTTPLISTENER));
            wsadmCommonParamsNoConfig = " --user=" + getWSAdminUser() +
                    " --password-file=" + adminPassFile + " --host=" +
                    getWSAdminHost() + " --port=" + getWSAdminPort() + 
                    " --ssl=" + isAdminPortSecure;
            wsAdminCommonParams = wsadmCommonParamsNoConfig + " --config=" +
                    getWSAdminConfig();
            validateWSConfig();
            validateWSHttpListener();
            checkWebContainer64BitEnabled();
            fillCfgMap();
        } catch (Exception ex) {
           pLogger.log(Level.SEVERE, "WS7ContainerConfigInfo",
                   "Failed to set webserver configuration information. ");
           throw new AMTuneException(ex.getMessage());
        } finally {
            AMTuneUtil.deleteFile(tempFile);
            deletePasswordFile();
        }
    }

    protected void deletePasswordFile() {
        AMTuneUtil.deleteFile(getWSAdminPassFilePath());
    }

    private void setWsAdminPass(String wsAdminPass)
    throws AMTuneException {
        if (wsAdminPass != null && wsAdminPass.trim().length() > 0) {
            this.wsAdminPass = wsAdminPass.trim();
        } else {
            mWriter.writelnLocaleMsg("pt-ws-password-not-found-msg");
                throw new AMTuneException(AMTuneUtil.getResourceBundle().
                        getString("pt-ws-password-null"));
        }
    }
    
    public String getWsAdminPass() {
        return wsAdminPass;
    }
    
    public String getWSAdminPassFilePath() {
        return adminPassFile;
    }

    private void setWSAdminDir(String wsAdminDir)
    throws AMTuneException {
        File wsDir = new File(wsAdminDir);
        if (wsAdminDir != null && wsDir.isDirectory()) {
            this.wsAdminDir = wsAdminDir.trim();
        } else {
            mWriter.writeLocaleMsg("pt-not-configured");
            AMTuneUtil.printErrorMsg(WSADMIN_DIR);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-invalid-wadm-tool-dir"));
        }
    }

    public String getWSAdminDir() {
        return wsAdminDir;
    }

    private void setWSAdminCmd()
    throws AMTuneException {
        if (AMTuneUtil.isWindows()) {
            wsAdminCmd = wsAdminDir + FILE_SEP + "wadm.bat ";
        } else {
            wsAdminCmd = wsAdminDir + FILE_SEP + "wadm ";
        }
        File cmdFile = new File(wsAdminCmd.trim());
        if (cmdFile != null && !cmdFile.isFile()) {
            mWriter.write(wsAdminCmd);
            mWriter.writeLocaleMsg("pt-tool-not-found");
            mWriter.writeLocaleMsg("pt-cannot-proceed");
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-tool-not-found"));
        }
    }

    public String getWSAdminCmd() {
        return wsAdminCmd;
    }

    private void setWSAdminHost(String wsAdminHost) 
    throws AMTuneException {
        if (wsAdminHost != null && wsAdminHost.trim().length() > 0) {
            this.wsAdminHost = wsAdminHost.trim();
        } else {
            mWriter.writeLocaleMsg("pt-not-configured");
            AMTuneUtil.printErrorMsg(WSADMIN_HOST);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-ws-host-name"));
        }
    }

    public String getWSAdminHost() {
        return wsAdminHost;
    }

    private void setWSAdminSecure(String wsAdminSecure) 
    throws AMTuneException {
        if (wsAdminSecure != null && wsAdminSecure.trim().length() > 0) {
            if (wsAdminSecure.equals("--ssl=true")) {
                isAdminPortSecure = true;
            } else if (wsAdminSecure.equals("--ssl=false")) {
                isAdminPortSecure = false;
            } else {
                AMTuneUtil.printErrorMsg(WSADMIN_SECURE);
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-invalid-ws-secure-prop"));
            }
        } else {
            mWriter.writeLocaleMsg("pt-not-configured");
            AMTuneUtil.printErrorMsg(WSADMIN_SECURE);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-ws-admin-secure"));
        }
    }

    public boolean isAdminPortSecure() {
        return isAdminPortSecure;
    }

    private void setWSAdminConfig(String wsAdminConfig) 
    throws AMTuneException {
        if (wsAdminConfig != null && wsAdminConfig.trim().length() > 0) {
            this.wsAdminConfig = wsAdminConfig.trim();
        } else {
            mWriter.writeLocaleMsg("pt-not-configured");
            AMTuneUtil.printErrorMsg(WSADMIN_CONFIG);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-ws-config-name"));
        }
    }

    public String getWSAdminConfig() {
        return wsAdminConfig;
    }

    private void setWSAdminHTTPListener(String wsAdminHTTPListener)
    throws AMTuneException {
        if (wsAdminHTTPListener != null &&
                wsAdminHTTPListener.trim().length() > 0) {
            this.wsAdminHttpListener = wsAdminHTTPListener.trim();
        } else {
            mWriter.writeLocaleMsg("pt-not-configured");
            AMTuneUtil.printErrorMsg(WSADMIN_HTTPLISTENER);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-error-null-ws-http-listener"));
        }
    }

    public String getWSAdminHttpListener() {
        return wsAdminHttpListener;
    }

    /**
     * Validates Web server Configuration
     *
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void validateWSConfig()
    throws AMTuneException {
        String resultCmd = getWSAdminCmd() + "list-configs" +
                wsadmCommonParamsNoConfig;
        StringBuffer resultBuffer = new StringBuffer();
        int retVal = AMTuneUtil.executeCommand(resultCmd, passWordStr, 
                adminPassFile,
                resultBuffer);
        if (resultBuffer.indexOf("Unable to connect to admin-server") != -1) {
            mWriter.writelnLocaleMsg("pt-web-not-running-msg");
            throw new AMTuneException(AMTuneUtil.getResourceBundle().getString(
                    "pt-error-unable-to-connect-to-asadmin-srv"));
        } else if (resultBuffer.toString().indexOf(
                "Invalid user or password") != -1) {
            mWriter.writelnLocaleMsg("pt-error-ws-check-user-password-msg");
            throw new AMTuneException(AMTuneUtil.getResourceBundle().getString(
                    "pt-error-ws-invalid-user-password"));
        } else if (resultBuffer.toString().indexOf("Unknown host :") != -1) {
            AMTuneUtil.printErrorMsg(WSADMIN_HOST);
            throw new AMTuneException(AMTuneUtil.getResourceBundle().getString(
                    "pt-error-unknow-host"));
        } else if(resultBuffer.toString().indexOf("Invalid value for ssl") != 
                -1) {
            AMTuneUtil.printErrorMsg(WSADMIN_SECURE);
            throw new AMTuneException(AMTuneUtil.getResourceBundle().getString(
                    "pt-error-invalid-ws-ssl"));
        } else if(resultBuffer.toString().indexOf(
                "The server requires an SSL connection") != -1) {
            mWriter.writelnLocaleMsg("pt-error-ws-port-ssl-msg");
            AMTuneUtil.printErrorMsg(WSADMIN_SECURE);
            throw new AMTuneException(AMTuneUtil.getResourceBundle().getString(
                    "pt-error-invalid-ws-ssl"));
        }
        boolean valid = false;
        if (retVal == 0) {
            StringTokenizer str = new StringTokenizer(resultBuffer.toString(),
                    "\n");
            while (str.hasMoreTokens()) {
                String token = str.nextToken();
                if (token.indexOf(getWSAdminConfig()) != -1) {
                    StringTokenizer str2 = new StringTokenizer(token, " ");
                    while (str2.hasMoreTokens()) {
                        if (str2.nextToken().trim().equalsIgnoreCase(
                                getWSAdminConfig())) {
                            valid = true;
                        }
                    }
                }
            }
            if (!valid) {
                mWriter.writeLocaleMsg("pt-web-cur-wadm-settings");
                mWriter.writeln(getWSAdminConfig());
                mWriter.writeLocaleMsg("pt-web-cur-configs");
                mWriter.writeln(resultBuffer.toString());
                mWriter.writeLocaleMsg("pt-conf-parm-cust-msg");
                mWriter.writeln(WSADMIN_CONFIG);
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-invalid-ws-config-name"));
            } else {
                pLogger.log(Level.INFO, "validateWSConfig", "Validated WS " +
                        "config " + resultBuffer.toString());
            }
        } else {
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-web-wadm-conf-error"));
        }
    }

    /**
     * Validates HTTP listener
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void validateWSHttpListener()
    throws AMTuneException {
        String resultCmd = getWSAdminCmd() + "list-http-listeners" +
                wsAdminCommonParams;
        StringBuffer resultBuffer = new StringBuffer();
        int retVal = AMTuneUtil.executeCommand(resultCmd, passWordStr, 
                adminPassFile,
                resultBuffer);
        boolean valid = false;
        if (retVal == 0) {
            StringTokenizer str = new StringTokenizer(resultBuffer.toString(),
                    "\n");
            while (str.hasMoreTokens()) {
                String token = str.nextToken();
                if (token.indexOf(getWSAdminHttpListener()) != -1) {
                    StringTokenizer str2 = new StringTokenizer(token, " ");
                    while (str2.hasMoreTokens()) {
                        if (str2.nextToken().trim()
                                .equalsIgnoreCase(getWSAdminHttpListener())) {
                        valid = true;
                        }
                    }
                }
            }
            if (!valid) {
                mWriter.writeLocaleMsg("pt-web-cur-http-listener-msg");
                mWriter.writeln(getWSAdminHttpListener());
                mWriter.writeLocaleMsg("pt-web-cur-listeners");
                mWriter.writeln(resultBuffer.toString());
                mWriter.writeLocaleMsg("pt-conf-parm-cust-msg");
                mWriter.writeln(WSADMIN_HTTPLISTENER);
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-invalid-ws-http-listener"));
            } else {
                pLogger.log(Level.INFO, "validateWSHttpListener",
                        "Validated WS httplistener " + 
                        getWSAdminHttpListener());
            }
        } else {
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-web-wadm-httplistener-error"));
        }
    }

    /**
     * Checks if Web server is using 64 bit JVM
     */
    private void checkWebContainer64BitEnabled() 
    throws AMTuneException {
        mWriter.writelnLocaleMsg("pt-web-check-jvmbits");
        String jvmcmd = getWSAdminCmd() + "get-config-prop" +
                wsAdminCommonParams + " platform";
        StringBuffer resultBuffer = new StringBuffer();
        int retVal = AMTuneUtil.executeCommand(jvmcmd, passWordStr, 
                adminPassFile,
                resultBuffer);
        if (retVal == 0) {
            if (resultBuffer.toString().indexOf("64") == -1) {
                setJVM64BitEnabled(false);
            } else {
                setJVM64BitEnabled(true);
            }
        } else {
            pLogger.log(Level.SEVERE, "checkWebContainer64BitEnabled",
                    "Error checking jvm bits so using 32 bit. ");
            setJVM64BitEnabled(false);
        }
    }

    /**
     * Calculates the Web server7 tuning parameters
     *
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void fillCfgMap()
    throws AMTuneException {
        try {
            pLogger.log(Level.INFO, "filCfgMap", "Getting server " +
                    "configuration information.");
            String propCmd = getWSAdminCmd() + "get-thread-pool-prop" +
                    wsAdminCommonParams;
            String httpPropCmd = getWSAdminCmd() + "get-http-listener-prop" +
                    wsAdminCommonParams + " --http-listener=" +
                    getWSAdminHttpListener();
            String statsPropCmd = getWSAdminCmd() + "get-stats-prop" +
                    wsAdminCommonParams;
            String listJvmOptions = getWSAdminCmd() + "list-jvm-options" +
                    wsAdminCommonParams;
            StringBuffer resultBuffer = new StringBuffer();
            String reqLine = "";
            cfgMap = new HashMap();
            int retVal = AMTuneUtil.executeCommand(propCmd, passWordStr, 
                    adminPassFile,
                    resultBuffer);
            if (retVal == 0) {
                AMTuneUtil.writeResultBufferToTempFile(resultBuffer,
                        tempFile);
                FileHandler cfgF = new FileHandler(tempFile);
                reqLine = cfgF.getLine(MIN_THREADS);
                cfgMap.put(MIN_THREADS,
                        AMTuneUtil.getLastToken(reqLine, PARAM_VAL_DELIM));
                reqLine = cfgF.getLine(MAX_THREADS);
                cfgMap.put(MAX_THREADS,
                        AMTuneUtil.getLastToken(reqLine, PARAM_VAL_DELIM));
                reqLine = cfgF.getLine(QUEUE_SIZE);
                cfgMap.put(QUEUE_SIZE,
                        AMTuneUtil.getLastToken(reqLine, PARAM_VAL_DELIM));
                reqLine = cfgF.getLine(STACK_SIZE);
                cfgMap.put(STACK_SIZE,
                        AMTuneUtil.getLastToken(reqLine, PARAM_VAL_DELIM));
                cfgF.close();
            } else {
                pLogger.log(Level.SEVERE, "fillCfgMap",
                        "Error getting get-thread-pool-prop configuration " +
                        "information. ");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-getting-thread-pool-prop"));
            }
            resultBuffer.setLength(0);
            retVal = AMTuneUtil.executeCommand(httpPropCmd, passWordStr, 
                    adminPassFile,
                    resultBuffer);
            if (retVal == 0) {
                AMTuneUtil.writeResultBufferToTempFile(resultBuffer,
                        tempFile);
                FileHandler cfgF = new FileHandler(tempFile);
                reqLine = cfgF.getLine(ACCEPTOR_THREADS);
                cfgMap.put(ACCEPTOR_THREADS,
                        AMTuneUtil.getLastToken(reqLine, PARAM_VAL_DELIM));
                cfgF.close();
            } else {
                pLogger.log(Level.SEVERE, "fillCfgMap",
                        "Error getting get-http-listener-prop configuration " +
                        "information. ");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-getting-http-list-prop"));
            }
            resultBuffer.setLength(0);
            retVal = AMTuneUtil.executeCommand(statsPropCmd, passWordStr, 
                    adminPassFile,
                    resultBuffer);
            if (retVal == 0) {
                AMTuneUtil.writeResultBufferToTempFile(resultBuffer,
                        tempFile);
                FileHandler cfgF = new FileHandler(tempFile);
                reqLine = cfgF.getLine(ENABLED);
                cfgMap.put(ENABLED, AMTuneUtil.getLastToken(reqLine, 
                        PARAM_VAL_DELIM));
                cfgF.close();
            } else {
                pLogger.log(Level.SEVERE, "fillCfgMap",
                        "Error getting get-stats-prop configuration " +
                        "information. ");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-getting-stats-prop"));
            }
            resultBuffer.setLength(0);
            retVal = AMTuneUtil.executeCommand(listJvmOptions, passWordStr,
                    adminPassFile,
                    resultBuffer);
            if (retVal == 0) {
                AMTuneUtil.writeResultBufferToTempFile(resultBuffer,
                        tempFile);
                FileHandler cfgF = new FileHandler(tempFile);
                reqLine = cfgF.getLine(MIN_HEAP_FLAG);
                StringTokenizer st = new StringTokenizer(reqLine, " ");
                st.hasMoreElements();
                cfgMap.put(MIN_HEAP_FLAG, st.nextToken());
                st.hasMoreTokens();
                cfgMap.put(MAX_HEAP_FLAG, st.nextToken());
                reqLine = checkJVMOpt(cfgF.getLine(GC_LOG_FLAG));
                cfgMap.put(GC_LOG_FLAG, reqLine);
                reqLine = checkJVMOpt(cfgF.getLine(SERVER_FLAG));
                cfgMap.put(SERVER_FLAG, reqLine);
                reqLine = checkJVMOpt(cfgF.getLine(STACK_SIZE_FLAG));
                cfgMap.put(STACK_SIZE_FLAG, reqLine);
                reqLine = checkJVMOpt(cfgF.getLine(NEW_SIZE_FLAG));
                cfgMap.put(NEW_SIZE_FLAG, reqLine);
                reqLine = checkJVMOpt(cfgF.getLine(MAX_NEW_SIZE_FLAG));
                cfgMap.put(MAX_NEW_SIZE_FLAG, reqLine);
                reqLine = checkJVMOpt(
                        cfgF.getLine(DISABLE_EXPLICIT_GC_FLAG.replace("-XX:+", 
                        "")));
                cfgMap.put(DISABLE_EXPLICIT_GC_FLAG, reqLine);
                reqLine = checkJVMOpt(
                        cfgF.getLine(PARALLEL_GC_FLAG.replace("-XX:+", "")));
                cfgMap.put(PARALLEL_GC_FLAG, reqLine);
                reqLine = checkJVMOpt(
                        cfgF.getLine(HISTOGRAM_FLAG.replace("-XX:+", "")));
                cfgMap.put(HISTOGRAM_FLAG, reqLine);
                reqLine = checkJVMOpt(
                        cfgF.getLine(GC_TIME_STAMP_FLAG.replace("-XX:+", "")));
                cfgMap.put(GC_TIME_STAMP_FLAG, reqLine);
                reqLine = checkJVMOpt(
                        cfgF.getLine(MARK_SWEEP_GC_FLAG.replace("-XX:+", "")));
                cfgMap.put(MARK_SWEEP_GC_FLAG, reqLine);
                reqLine = checkJVMOpt(
                        cfgF.getLine(HEAPDUMP_OOM_FLAG.replace("-XX:+", "")));
                cfgMap.put(HEAPDUMP_OOM_FLAG, reqLine);         
                reqLine = checkJVMOpt(
                        cfgF.getLine(PRINT_CONC_LOCKS_FLAG.replace("-XX:+", "")));
                cfgMap.put(PRINT_CONC_LOCKS_FLAG, reqLine);
                reqLine = checkJVMOpt(
                        cfgF.getLine(ESCAPE_ANALYSIS_FLAG.replace("-XX:+", "")));
                cfgMap.put(ESCAPE_ANALYSIS_FLAG, reqLine);
                reqLine = checkJVMOpt(
                        cfgF.getLine(COMPRESSED_OOPS_FLAG.replace("-XX:+", "")));
                cfgMap.put(COMPRESSED_OOPS_FLAG, reqLine); 
                if (AMTuneUtil.isNiagara()) {
                    reqLine = checkJVMOpt(cfgF.getLine(PARALLEL_GC_THREADS));
                    cfgMap.put(PARALLEL_GC_THREADS, 
                            AMTuneUtil.getLastToken(reqLine, PARAM_VAL_DELIM));
                }
                cfgF.close();
            } else {
                pLogger.log(Level.SEVERE, "fillCfgMap",
                        "Error getting list-jvm-options configuration " +
                        "information. ");
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-getting-jvm-options"));
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "fillCfgMap", "Error getting " +
                    "server config information.");
            throw new AMTuneException(ex.getMessage());
        }
    }

    /**
     * Return wsadm command parameters without config option
     * @return wsadm command parameters without config option
     */
    public String getWSAdmCommonParamsNoConfig() {
        return wsadmCommonParamsNoConfig;
    }

    /**
     * Return wsadm command parameters with config option
     * @return wsadm command parameters with config option
     */
    public String getWSAdminCommonParams() {
        return wsAdminCommonParams;
    }

    /**
     * Return Web Server 7 configuration map
     * @return Web Server 7 configuration map
     */
    public Map getServerCfgMap() {
        return cfgMap;
    }
    
    /**
     * Set WebServer Admin server port
     * 
     * @param wsAdminPort Admin server port number.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setWSAdminPort(String wsAdminPort) 
    throws AMTuneException {
        if (wsAdminPort != null && wsAdminPort.trim().length() > 0) {
            try {
                Integer.parseInt(wsAdminPort.trim());
            } catch (NumberFormatException ne) {
                AMTuneUtil.printErrorMsg(WSADMIN_PORT);
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-error-invalid-port-no"));
            }
            this.wsAdminPort = wsAdminPort.trim();
        } else {
             mWriter.writeLocaleMsg("pt-not-configured");
             AMTuneUtil.printErrorMsg(WSADMIN_PORT);
             throw new AMTuneException(AMTuneUtil.getResourceBundle()
                     .getString("pt-error-null-ws-admin-port"));
        }
    }
    
    /**
     * Return Admin server port number.
     *  
     * @return Admin Server port number.
     */
    public String getWSAdminPort() {
        return wsAdminPort;
    }
    
    /**
     * Set WebServer Administrator user name
     * 
     * @param wsAdminUser Administrator user name.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    private void setWSAdminUser(String wsAdminUser) 
    throws AMTuneException {
        if (wsAdminUser != null && wsAdminUser.trim().length() > 0) {
            this.wsAdminUser = wsAdminUser.trim();
        } else {
             mWriter.writeLocaleMsg("pt-not-configured");
             AMTuneUtil.printErrorMsg(WSADMIN_USER);
             throw new AMTuneException(AMTuneUtil.getResourceBundle()
                     .getString("pt-error-null-ws-admin-user"));
        }
    }
    
    /**
     * Return Administrator User Name.
     *  
     * @return Administrator User name.
     */
    public String getWSAdminUser() {
        return wsAdminUser;
    }
    
    private String checkJVMOpt(String val) {
        if ((val == null) || (val != null && val.trim().length() <= 0)) {
            return NO_VAL_SET;
        } else {
            return val.trim();
        }
    }
}
