/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: OSChecker.java,v 1.2 2008/06/25 05:51:29 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util;

/**
 * 
 * This class represents the Operating System that the installer is running on.
 * 
 */
public class OSChecker {

    /**
     * Sets the osName,osMajorVersion,osMinorVersion variables
     */
    private static void getOS() {

        osName = System.getProperty(OS_NAME);

        try {
            String this_version = System.getProperty(OS_VERSION);
            int idx = this_version.indexOf(DOT);

            if (idx != -1) {
                osMajorVersion = Integer
                        .valueOf(this_version.substring(0, idx)).intValue();
                osMinorVersion = Integer.valueOf(
                        this_version.substring(idx + 1)).intValue();
            } else {
                osMajorVersion = Integer.valueOf(this_version).intValue();
            }
        } catch (NumberFormatException ex) {
            Debug.log("OSChecker.getOS() threw exception : ", ex);
        }
    }

    /**
     * Sets the osArchitecture variable
     */
    private static void getArch() {
        osArchitecture = System.getProperty(OS_ARCH);
    }

    /**
     * check if operating system matches with a given operating system name 
     * <br>
     * 
     * @param name
     *            name of operating system
     * 
     * @return true if operating system matches with a given operating system
     *         name
     */
    public static boolean match(String name) {

        boolean result = (osName.equalsIgnoreCase(name) == true);

        if (result) {
            return result;
        } else {
            // For windows
            String winPrefix = name.substring(0, 3);
            if (osName.toLowerCase().indexOf(winPrefix.toLowerCase()) != -1) {
                return true;
            }
        }

        return false;

        // return (osName.equalsIgnoreCase(name) == true);
    }

    /**
     * check if version current operating system matches with a given version
     * <br>
     * <br>
     * 
     * @param name
     *            name of operating system
     * 
     * @param majorVersion
     *            major version
     * 
     * @param minorVersion
     *            minor version
     * 
     * @return true if version current operating system matches with a given
     *         version
     */
    public static boolean match(String name, int majorVersion, 
            int minorVersion) {

        boolean result = (osName.equalsIgnoreCase(name) == true)
                && (osMajorVersion == majorVersion)
                && (osMinorVersion == minorVersion);

        return result;
    }

    /**
     * Method matchApprox
     * 
     * 
     * @param name
     * 
     * @return
     * 
     */
    public static boolean matchApprox(String name) {
        return (osName.regionMatches(true, 0, name, 0, name.length()));
    }

    /**
     * check if version current operating system is greater than a given 
     * version
     * <br>
     * 
     * @param name
     *            name of operating system
     * 
     * @param majorVersion
     *            major version
     * 
     * @param minorVersion
     *            minor version
     * 
     * @return true if version current operating system is greater than a given
     *         version
     */
    public static boolean atleast(String name, int majorVersion,
            int minorVersion) {

        boolean result = (osName.equalsIgnoreCase(name) == true)
                && (osMajorVersion >= majorVersion)
                && (osMinorVersion >= minorVersion);

        return result;
    }

    /**
     * Check if current operating system architecture matches with a given
     * architecture
     * 
     * @param name
     * 
     * @return
     * 
     * @see
     */
    public static boolean matchArch(String name) {
        return osArchitecture.equalsIgnoreCase(name);
    }

    /**
     * Method isSolaris
     * 
     * 
     * @return
     * 
     */
    public static boolean isSolaris() {
        return matchApprox(SUNOS);
    }

    /**
     * Method isWindows
     * 
     * 
     * @return
     * 
     */
    static public boolean isWindows() {
        return matchApprox(WINDOWS);
    }

    /**
     * Method isUnix
     * 
     * 
     * @return
     * 
     */
    static public boolean isUnix() {
        return (isSolaris() || isHPUX() || isLinux() || isAIX());
    }

    static public boolean isAIX() {
        return matchApprox(AIX);
    }

    static public boolean isLinux() {
        return matchApprox(LINUX);
    }

    /**
     * Method isHPUX
     * 
     * 
     * @return
     * 
     */
    static public boolean isHPUX() {
        return (matchApprox(HPUX));
    }

    /** Operating System name */
    private static String osName = null;

    /** Operating System Major Version Number */
    private static int osMajorVersion = 0;

    /** Operating System Minor Version Number */
    private static int osMinorVersion = 0;

    /** Operating System Architecture */
    private static String osArchitecture = null;

    /*
     * OS related constants
     */
    public static String OS_NAME = "os.name";

    public static String OS_VERSION = "os.version";

    public static String DOT = ".";

    public static String OS_ARCH = "os.arch";

    public static String SUNOS = "SunOS";

    public static String WINDOWS = "Windows";

    public static String AIX = "AIX";

    public static String LINUX = "Linux";

    public static String HPUX = "HP-UX";

    static {
        getOS();
        getArch();
    }
}
