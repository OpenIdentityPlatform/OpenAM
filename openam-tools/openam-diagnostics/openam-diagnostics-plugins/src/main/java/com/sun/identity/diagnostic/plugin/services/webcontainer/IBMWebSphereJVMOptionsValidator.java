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
 * $Id: IBMWebSphereJVMOptionsValidator.java,v 1.1 2008/11/22 02:41:23 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.webcontainer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Enumeration;
import java.util.Properties;

import com.sun.identity.diagnostic.base.core.common.DTException;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;

public class IBMWebSphereJVMOptionsValidator implements IWebContainerValidate {

    private IToolOutput toolOutWriter;
    
    public IBMWebSphereJVMOptionsValidator() {        
    }
    
    public void validate(
        Properties containerProp, 
        String profilePath,
        String serverPath
    ) throws DTException {
        toolOutWriter = WebContainerService.getToolWriter();        
        if (serverPath != null) {
            File serverDir = new File(serverPath);                                                                              
            if (serverDir.exists() && serverDir.isDirectory()) {
                toolOutWriter.printStatusMsg(true,
                    "webcontainer-server-dir-check");                                                                                                            
                String serverFileName = containerProp.getProperty(
                    WebContainerConstant.JVMOPTIONS_FILE);                                        
                File jvmoptionsFile = new File(containerProp.getProperty(
                    WebContainerConstant.JVMOPTIONS_PATTERN_FILE));
                FileInputStream fin = null;
                Properties jvmoptionsProp = new Properties();
                if (jvmoptionsFile.exists() &&
                    jvmoptionsFile.isFile()) {
                    try {
                        fin = new FileInputStream(jvmoptionsFile);
                        jvmoptionsProp.load(fin);
                    } catch (IOException ex) {
                        throw new DTException(ex.getMessage());
                    } finally {
                        if (fin != null) {
                            try {
                                fin.close();
                            } catch (IOException ignored) {}
                        }
                    }                        
                }
                LineNumberReader lnRdr = null;
                String javahomePath = null;
                try {
                    lnRdr = new LineNumberReader(new FileReader(
                        new File(serverDir, serverFileName)));                        
                    String buffer = null;
                    while ((buffer = lnRdr.readLine()) != null) {
                        for (Enumeration e = jvmoptionsProp.propertyNames();
                            e.hasMoreElements();) {
                            String name = (String) e.nextElement();
                            if (name.startsWith(
                                WebContainerConstant.JVMOPTIONS_PATTERN)) {
                                String value = jvmoptionsProp.getProperty(
                                    name);              
                                String[] answers = buffer.split(value);
                                if ((answers.length > 1) ||
                                    (!answers[0].equals(buffer))) {
                                    jvmoptionsProp.remove(name);
                                    int num = Integer.parseInt(
                                        name.substring(WebContainerConstant.
                                        JVMOPTIONS_PATTERN.length(),
                                        name.length()));
                                    jvmoptionsProp.remove(
                                        WebContainerConstant.
                                        JVMOPTIONS_CLEARTEXT +
                                        String.valueOf(num));
                                }
                            }
                        }
                    }
                    boolean passed = true;
                    for (Enumeration e = jvmoptionsProp.propertyNames();
                        e.hasMoreElements();) {
                        String name = (String) e.nextElement();
                        if (name.startsWith(
                            WebContainerConstant.JVMOPTIONS_CLEARTEXT)) {
                            if (passed) {
                                toolOutWriter.printError(
                                    "webcontainer-jvm-options-invalid");
                            }
                            toolOutWriter.printError(
                                jvmoptionsProp.getProperty(name));
                            passed = false;
                        }
                    }
                    if (passed) {
                        toolOutWriter.printStatusMsg(true,
                            "webcontainer-jvm-options-check");
                    } else {
                        toolOutWriter.printStatusMsg(false,
                            "webcontainer-jvm-options-check");
                    }
                } catch (IOException ex) {
                    throw new DTException(ex.getMessage());
                } finally {
                    if (lnRdr != null) {
                        try {
                            lnRdr.close();
                        } catch (IOException ignored) {}
                    }
                }                                    
            } else {
                toolOutWriter.printError("webcontainer-server-dir-invalid");
                toolOutWriter.printStatusMsg(false,
                    "webcontainer-server-dir-check"); 
            }
        } else {
            toolOutWriter.printError("webcontainer-server-dir-invalid");
                toolOutWriter.printStatusMsg(false,
                    "webcontainer-server-dir-check"); 
        }
    }
}
