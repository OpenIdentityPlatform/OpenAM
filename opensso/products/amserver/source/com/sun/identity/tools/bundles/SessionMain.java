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
 * $Id: SessionMain.java,v 1.4 2008/06/25 05:44:12 qcheng Exp $
 *
 */

package com.sun.identity.tools.bundles;

import java.io.File;
import java.io.IOException;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

public class SessionMain implements SetupConstants{
    
    public static void main(String[] args) {
        String destPath = null;
        String currentOS = null;
        Properties configProp=new Properties();
        
        ResourceBundle bundle = ResourceBundle.getBundle(System.getProperty(
            SETUP_PROPERTIES_FILE, DEFAULT_SESSION_PROPERTIES_FILE));
        if (System.getProperty(PRINT_HELP) != null) {
            if (System.getProperty(PRINT_HELP).equals(YES)) {
                SetupUtils.printUsage(bundle);
                System.exit(0);
            }
        }
        currentOS = SetupUtils.determineOS();
        destPath = System.getProperty(PATH_DEST);
        try {
            if ((destPath != null) && (destPath.trim().length() > 0)) {
                if ((destPath.indexOf("/") > -1) || 
                    (destPath.indexOf("\\") > -1)) {
                    System.out.println(
                        bundle.getString("message.error.inputformat"));
                    destPath = null;
                }
            }    
            while ((destPath == null) || (destPath.trim().length() == 0)) {
                destPath = SetupUtils.getUserInput(bundle.getString(currentOS
                    + QUESTION));
                if ((destPath != null) && (destPath.trim().length() > 0)) {
                    if ((destPath.indexOf("/") > -1) || 
                        (destPath.indexOf("\\") > -1)) {
                        System.out.println(bundle.getString(
                            "message.error.inputformat"));
                        destPath = null;
                    }
                }    
            }
        } catch (IOException ex) {
            System.out.println(
                bundle.getString("message.error.input"));
            System.out.println(ex.getMessage());
            System.exit(1);
        }        
        configProp.setProperty(USER_INPUT, destPath);
        configProp.setProperty(CURRENT_PLATFORM, currentOS);
        SetupUtils.evaluateBundleValues(bundle, configProp);
        try {
            SetupUtils.copyAndFilterScripts(bundle, configProp);
        } catch (IOException ex) {
            System.out.println(bundle.getString("message.error.copy"));
            System.exit(1);
            //ex.printStackTrace();
        }
        String extDir = null;
        try{
            extDir = bundle.getString(EXT_DIR);
        } catch (MissingResourceException ex) {
            extDir = DEFAULT_EXT_DIR;
        }
        String jmqDir = null;
        try{
            jmqDir = bundle.getString(JMQ_DIR); 
        } catch (MissingResourceException ex) {
            jmqDir = DEFAULT_JMQ_DIR;
        }
        try {
            String jmqFileName = bundle.getString(currentOS + JMQ);
            if (currentOS.equals(WINDOWS)) {
                SetupUtils.unzip(extDir + FILE_SEPARATOR + jmqFileName, jmqDir,
                    true);
                System.out.println(bundle.getString("message.info.jmq.success")
                + " " + (new File(".").getCanonicalPath() + FILE_SEPARATOR +
                jmqDir));
            } else {
                Process proc = Runtime.getRuntime().exec("unzip -o -q "+ extDir
                    + FILE_SEPARATOR + jmqFileName + " -d " + jmqDir);
                try {
                    if (proc.waitFor() != 0) {
                        System.out.println(
                        bundle.getString("message.info.jmq.fail")
                        + " " + (new File(".").getCanonicalPath() + 
                        FILE_SEPARATOR + jmqDir));
                    } else {
                        System.out.println(
                        bundle.getString("message.info.jmq.success")
                        + " " + (new File(".").getCanonicalPath() + 
                        FILE_SEPARATOR + jmqDir));
                    }
                } catch (InterruptedException ex) {
                    System.out.println(bundle.getString("message.info.jmq.fail")
                    + " " + (new File(".").getCanonicalPath() + FILE_SEPARATOR +
                    jmqDir));
                }
            }
        } catch (IOException ex) {
            System.out.println(bundle.getString("message.error.jmq"));
            System.out.println(bundle.getString("message.info.jmq.fail") + " " +
                jmqDir + ".");
            //ex.printStackTrace();
        }
        
        /*
        String bdbDir = null;
        try{
            bdbDir = bundle.getString(BDB_DIR);
        } catch (MissingResourceException ex) {
            bdbDir = DEFAULT_BDB_DIR;
        }
        try {
        	
            String bdbFileName = bundle.getString(currentOS + BDB);
            if (currentOS.equals(WINDOWS)) {
                SetupUtils.unzip(extDir + FILE_SEPARATOR + bundle.getString(
                    currentOS + BDB), bdbDir, true);
                System.out.println(bundle.getString("message.info.bdb.success")
                    + " " + bdbDir + ".");
            } else{
                SetupUtils.ungzip(extDir + FILE_SEPARATOR + bdbFileName,
                    bdbDir);
                File tarFile = new File(bdbDir + FILE_SEPARATOR +
                    bdbFileName.substring(0, bdbFileName
                    .lastIndexOf(GZIP_EXT)));
                Process proc = Runtime.getRuntime().exec("tar -xf " +
                    tarFile.getName(), null, new File(bdbDir));
                try {
                    if (proc.waitFor() != 0) {
                        System.out.println(bundle.getString(
                            "message.info.bdb.fail") + " " + bdbDir + ".");
                    } else {
                        System.out.println(bundle.getString(
                            "message.info.bdb.success") + " " + bdbDir + ".");
                    }
                } catch (InterruptedException ex) {
                    System.out.println(bundle.getString("message.info.bdb.fail")
                        + " " + bdbDir + ".");
                }
                tarFile.delete();
            }
        } catch (IOException ex) {
            System.out.println(bundle.getString("message.error.bdb"));
            System.out.println(bundle.getString("message.info.bdb.fail") + " " +
                bdbDir + ".");
            //ex.printStackTrace();
        }*/
    }

}
