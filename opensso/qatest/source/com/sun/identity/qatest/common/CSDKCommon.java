/* The contents of this file are subject to the terms
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
 * $Id:
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.qatest.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class contains helper methods related to CSDK module
 */
public class CSDKCommon extends TestCommon {

    private static String osName = System.getProperty("os.name").toLowerCase();
    private static String osArch = System.getProperty("os.arch").toLowerCase();
    private static String cpuList = System.getProperty("sun.cpu.isalist").
            toLowerCase();
    private String libraryPath;
    private String directoryPath;
    private String tmpLibraryPath = "CSDKGlobalData.lb_library_path.";
    private String tmpDiretoryPath = "CSDKGlobalData.directory.";
    private String tmpCryptUtilPath = "CSDKGlobalData.cryptutil.";
    private String platform;
    private String cryptUtilPath;
    boolean isWindows = false;

    /**
     * Creates a new instance of CSDKCOmmon
     */
    public CSDKCommon() {
        super("CSDKCommon");
    }

    /**
     * Gets the ld_library_path , location of binaries depending on
     * the platform
     * @return Map
     * @throws Exception
     */
    public Map getLibraryPath() throws Exception {
        entering("getLibraryPath", null);
        Map lpMap = new HashMap();
        Map csdkMap = getMapFromResourceBundle("csdk" + fileseparator +
                "CSDKGlobalData", null, null);
        if (osName.contains("sunos")) {
            ProcessBuilder pb = new ProcessBuilder("isainfo", "-kv");
            try {
                Process p = pb.start();
                BufferedReader stdInput = new BufferedReader(
                        new InputStreamReader(p.getInputStream()));
                if (stdInput.readLine().contains("64")) {
                    if (osArch.contains("sparc")) {
                        platform = "sparc_64";                        
                    } else {
                        platform = "x86_64";
                    }
                } else {
                    if (osArch.contains("sparc")) {
                        platform = "sparc_32";
                    } else {
                        platform = "x86_32";
                    }
                }
            } catch (Exception e) {
                log(Level.SEVERE, "getLibraryPath", e.getMessage());
            }
        } else if (osName.contains("linux")) {
            ProcessBuilder pbArch = new ProcessBuilder("uname", "-an");
            try {
                Process p = pbArch.start();
                BufferedReader stdInput = new BufferedReader(
                        new InputStreamReader(p.getInputStream()));
                if (stdInput.readLine().contains("64")) {
                    platform = "linux_64";
                } else {
                    platform = "linux_32";
                }
            } catch (Exception e) {
                log(Level.SEVERE, "getLibraryPath", e.getMessage());
            }
        } else if (osName.contains("windows")) {
            if (cpuList.contains("64")) {
                platform = "windows_64";
                isWindows = true;
            } else {
                platform = "windows_32";
                isWindows = true;
            }
        }
        libraryPath = (String) csdkMap.get(tmpLibraryPath + platform);
        directoryPath = (String) csdkMap.get(tmpDiretoryPath + platform);
        cryptUtilPath = (String) csdkMap.get(tmpCryptUtilPath + platform);
        lpMap.put("libraryPath", libraryPath);
        lpMap.put("directoryPath", directoryPath);
        lpMap.put("platform", platform);
        lpMap.put("cryptUtilPath", cryptUtilPath);
        lpMap.put("isWindows", isWindows);
        exiting("getLibraryPath");
        return lpMap;
    }

    /**
     * Returns the path of Bootstrap file
     * @return String
     * @throws Exception
     */
    public String getBootStrapFilePath() throws Exception {
        return getBaseDir() + fileseparator +
                serverName + fileseparator + "built" + fileseparator +
                "classes" + fileseparator + "csdk" + fileseparator +
                "CSDKBootstrap.properties";
    }

    /**
     * Returns the path of Configuration file
     * @return String
     * @throws Exception
     */
    public String getConfigurationFilePath() throws Exception {
        return getBaseDir() + fileseparator +
                serverName + fileseparator + "built" + fileseparator +
                "classes" + fileseparator + "csdk" + fileseparator +
                "CSDKConfiguration.properties";
    }
}
