/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Main.java,v 1.11 2010/01/14 23:38:25 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */
package com.sun.identity.tools.bundles;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.setup.Bootstrap;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.ResourceBundle;

public class Main implements SetupConstants{
    public static void main(String[] args) {
        ResourceBundle bundle = ResourceBundle.getBundle(System.getProperty(
            SETUP_PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE));

        if ((System.getProperty(PRINT_HELP) != null) &&
            System.getProperty(PRINT_HELP).equals(YES)){
            SetupUtils.printUsage(bundle);
            System.exit(0);
        }

        if (System.getProperty(CHECK_VERSION) != null) {
            if (System.getProperty(CHECK_VERSION).equals(YES)) {
                System.exit(VersionCheck.isValid());
            }
        }

        boolean loadConfig = (System.getProperty(CONFIG_LOAD) != null) &&
            System.getProperty(CONFIG_LOAD).equals(YES);
        String currentOS = SetupUtils.determineOS();
        Properties configProp = null;
        String debugPath = null;
        String logPath = null;
        
        if (loadConfig) {
            String configPath = System.getProperty(AMCONFIG_PATH);
            debugPath = System.getProperty(DEBUG_PATH);
            logPath = System.getProperty(LOG_PATH);
            String currentDir = System.getProperty("user.dir");

            try {
                if ((configPath == null) || (configPath.length() == 0)) {
                    configPath = SetupUtils.getUserInput(bundle.getString(
                        currentOS + QUESTION), System.getProperty("user.home")
                            + File.separator + "openam");
                    if (!(new File(configPath).isAbsolute())) {
                        System.out.println(bundle.getString(
                            "message.error.dir.absolute"));
                        System.exit(1);
                    }

                    if ((debugPath == null) || (debugPath.length() == 0)) {
                        debugPath = SetupUtils.getUserInput(bundle.getString(
                            currentOS + ".debug.dir"), currentDir
                                + File.separator + "debug");
                    }
                    if (!(new File(debugPath).isAbsolute())) {
                        System.out.println(bundle.getString(
                            "message.error.dir.absolute"));
                        System.exit(1);
                    }
                        if (!isWriteable(debugPath)) {
                        System.out.println(bundle.getString(
                            "message.error.debug.dir.not.writable"));
                        System.exit(1);                        
                    }

                    if ((logPath == null) || (logPath.length() == 0)) {
                        logPath = SetupUtils.getUserInput(bundle.getString(
                            currentOS + ".log.dir"), currentDir
                                + File.separator + "log");
                    }
                    if (!(new File(logPath).isAbsolute())) {
                        System.out.println(bundle.getString(
                            "message.error.dir.absolute"));
                            System.exit(1);
                    }
                    if (!isWriteable(logPath)) {
                        System.out.println(bundle.getString(
                            "message.error.log.dir.not.writable"));
                        System.exit(1);                        
                    }
                } else {
                    String toolsHome = new File(".").getCanonicalPath();
                    toolsHome = toolsHome.replaceAll("\\\\", "/");

                    if ((debugPath == null) || (debugPath.length() == 0)) {
                        debugPath = toolsHome + "/debug";
                    }
                    if ((logPath == null) || (logPath.length() == 0)) {
                        logPath = toolsHome + "/log";
                    }
                }

                configProp = Bootstrap.load(configPath, false);
                if (configProp == null) {
                    System.out.println(bundle.getString("message.error.dir"));
                    System.exit(1);
                }

                File path = new File(debugPath);
                boolean created = path.exists() || path.mkdirs();
                if (!created) {
                    System.out.println(bundle.getString(
                        "message.error.debug.dir.not.writable"));
                    System.exit(1);                        
                }

                path = new File(logPath);
                created = path.exists() || path.mkdirs();
                if (!created) {
                    System.out.println(bundle.getString(
                        "message.error.log.dir.not.writable"));
                    System.exit(1);                        
                }

                if (!configPath.endsWith(FILE_SEPARATOR)) {
                    configPath = configPath + FILE_SEPARATOR;
                }

                configProp.setProperty(USER_INPUT,
                    configPath.substring(0, configPath.length() - 1));
                configProp.setProperty("LogDir", logPath);
                configProp.setProperty("DebugDir",debugPath);
                configProp.setProperty(CURRENT_PLATFORM, currentOS);
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                System.exit(1);
            }
        } else {
            configProp = new Properties();
        }

        SetupUtils.evaluateBundleValues(bundle, configProp);
        try {
            SetupUtils.copyAndFilterScripts(bundle, configProp);
            if (loadConfig) {
                Object[] p = {debugPath};
                System.out.println(MessageFormat.format(
                    bundle.getString("message.info.debug.dir"), p));
                p[0] = logPath;
                System.out.println(MessageFormat.format(
                    bundle.getString("message.info.log.dir"), p));

                System.out.println(bundle.getString(
                    "message.info.version.tools") + " " +
                    bundle.getString(TOOLS_VERSION));
                System.out.println(
                    bundle.getString("message.info.version.am") +
                    " " + SystemProperties.get("com.iplanet.am.version"));
            }
        } catch (IOException ex) {
            System.out.println(bundle.getString("message.error.copy"));
            System.exit(1);
        }
        System.exit(0);
    }
   
    private static boolean isWriteable(String file) {
        boolean exist = false;
        boolean writable = false;

        while ((file != null) && !exist) {
            File f = new File(file);
            exist = f.exists();
            if (!exist) {
                file = f.getParent();
            }
        }

        if (file != null) {
            File f = new File(file);
            writable = f.canWrite();
        }
        return writable;
    }   
}

