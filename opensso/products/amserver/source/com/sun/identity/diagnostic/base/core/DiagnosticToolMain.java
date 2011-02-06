/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DiagnosticToolMain.java,v 1.1 2008/11/22 02:19:53 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.diagnostic.base.core.common.DTException;
import com.sun.identity.diagnostic.base.core.common.ToolConstants;
import com.sun.identity.shared.debug.Debug;


/**
 * This is entry point for the tool. It sets up
 * logging and other resources and delegates the control
 * to <code>ToolManager</code> that starts the
 * application.
 *
 */

public class DiagnosticToolMain {
    
    public static String rbName;
    
    static {
        rbName = ToolConstants.RESOURCE_BUNDLE_NAME;
        init();
    }
    
    /**
     * This is entry point to start the tool.
     */
    public static void main(String[] args) {
        try {
            processArguments(args);
            ToolLogWriter.log(rbName, Level.INFO, "start-app-msg", null);
            DiagnosticToolController.setupServices();
            ToolLogWriter.log(rbName, Level.INFO, "service-started-success", 
                null);
            System.out.println("\n\nPress <Enter> to stop the Diagnostic Tool");
            waitForEnterPressed();
            
            ToolLogWriter.log(rbName, Level.INFO, "stop-tool-msg", null);
            System.out.println("\nEnd ...");
        } catch (DTException dte) {
            Debug.getInstance(ToolConstants.DEBUG_NAME).error(
                "DiagnosticToolMain.main: " + dte.getMessage());
        } catch (Exception e) {
            Debug.getInstance(ToolConstants.DEBUG_NAME).error(
                "DiagnosticToolMain.main: " + e.getMessage());
        }
    }
    
    private static void waitForEnterPressed() {
        try {
            System.in.read();
        } catch (IOException e) {
            Debug.getInstance(ToolConstants.DEBUG_NAME).error(
                "DiagnosticToolMain.waitForEnterPressed: " + e.getMessage());
        }
    }
    
    private static void processArguments(String[] argv) {
        int len = argv.length;
        if (len > 0) {
            if (argv[0].toLowerCase().equals("--console")) {
                SystemProperties.initializeProperties(
                    ToolConstants.TOOL_RUN_MODE, "CLI");
            }
        }
    }
    
    private static void init() {
        try {
            //read the configuration file for env params
            if (!loadDTConfigProperties(
                new File(".").getCanonicalPath()+ "/config/" +
                ToolConstants.TOOL_PROPERTIES)) {
                System.out.println(ResourceBundle.getBundle(rbName).getString(
                    "cannot-find-cfg-file"));
                Debug.getInstance(ToolConstants.DEBUG_NAME).error(
                    "DiagnosticToolMain.init: " +
                    ResourceBundle.getBundle(rbName).getString(
                    "cannot-find-cfg-file"));
                System.out.println(ResourceBundle.getBundle(rbName).getString(
                    "stop-tool-msg"));
                System.exit(1);
            } else {
                ToolLogWriter.init();
            }
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            Debug.getInstance(ToolConstants.DEBUG_NAME).error(
                "DiagnosticToolMain.init: " + ioe.getMessage());
            System.out.println(ResourceBundle.getBundle(rbName).getString(
                "error-reading-cfg-file"));
            System.out.println(ResourceBundle.getBundle(rbName).getString(
                "stop-tool-msg"));
            System.exit(1);
        }
    }
    
    private static boolean loadDTConfigProperties(
        String fileLocation
    ) throws IOException {
        boolean loaded = false;
        File test = new File(fileLocation);
        if (test.exists()) {
            FileInputStream fin = null;
            try {
                fin = new FileInputStream(fileLocation);
                if (fin != null) {
                    Properties props = new Properties();
                    props.load(fin);
                    SystemProperties.initializeProperties(props);
                    loaded = true;
                }
            } finally {
                if (fin != null) {
                    try {
                        fin.close();
                    } catch (IOException e) {
                        //ignore
                    }
                }
            }
        }
        return loaded;
    }
}
