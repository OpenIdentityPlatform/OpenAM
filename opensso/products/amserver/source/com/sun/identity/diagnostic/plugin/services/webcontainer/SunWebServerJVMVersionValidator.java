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
 * $Id: SunWebServerJVMVersionValidator.java,v 1.1 2008/11/22 02:41:23 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.webcontainer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Properties;

import com.sun.identity.diagnostic.base.core.common.DTException;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;


public class SunWebServerJVMVersionValidator implements IWebContainerValidate {
   
    private IToolOutput toolOutWriter;
    
    /** Creates a new instance of SunWebServerJVMVersionValidator */
    public SunWebServerJVMVersionValidator() {
    }
    
    public void validate(
        Properties containerProp, 
        String containerPath,
        String domainPath
    ) throws DTException {
        toolOutWriter = WebContainerService.getToolWriter();
        if (domainPath != null) {
            File domainDir = new File(domainPath);
            if (domainDir.exists() && domainDir.isDirectory()) {
                toolOutWriter.printStatusMsg(true,
                    "webcontainer-domain-dir-check"); 
                String javahome_dir = containerProp.getProperty(
                    WebContainerConstant.JAVAHOME_DIR);
                File javahomeDir = new File(domainDir, javahome_dir);
                if (javahomeDir.exists() && javahomeDir.isDirectory()) {
                    toolOutWriter.printStatusMsg(true,
                        "webcontainer-config-dir-check"); 
                    String javahomeFileName = containerProp.getProperty(
                        WebContainerConstant.JAVAHOME_FILE);                    
                    LineNumberReader lnRdr = null;
                    String javahomePath = null;
                    try {
                        lnRdr = new LineNumberReader(new FileReader(
                            new File(javahomeDir, javahomeFileName)));
                        String buffer = null;
                        String javahomeTag = containerProp.getProperty(
                            WebContainerConstant.JAVAHOME_TAG);
                        while ((buffer = lnRdr.readLine()) != null) {
                            String[] s = buffer.split("\\s*<" + javahomeTag +
                                ">\\s*");
                            if ((s.length > 1) || (!s[0].equals(buffer))) {
                                int equalIndex = buffer.indexOf("<" +
                                    javahomeTag + ">");
                                int endIndex = buffer.indexOf("</" + javahomeTag
                                    + ">", equalIndex + javahomeTag.length() + 
                                    2);
                                if (endIndex < 0) {
                                    endIndex = buffer.length();
                                }
                                javahomePath = buffer.substring(
                                    equalIndex + javahomeTag.length() + 2,
                                    endIndex).trim();
                                break;
                            }
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
                    LineNumberReader processLnRdr = null;
                    try {
                        toolOutWriter = WebContainerService.getToolWriter();
                        if (javahomePath.startsWith("\"")) {
                            javahomePath = javahomePath.substring(1,
                                javahomePath.length());
                        }
                        if (javahomePath.endsWith("\"")) {
                            javahomePath = javahomePath.substring(0,
                                javahomePath.length() - 1);
                        }
                        if (javahomePath.endsWith("/") ||
                            (javahomePath.endsWith("\\"))) {
                            javahomePath = javahomePath.substring(0,
                                javahomePath.length() - 1);
                        }                        
                        Process pro = Runtime.getRuntime().exec(javahomePath +
                            "/bin/java -version");
                        processLnRdr = new LineNumberReader(new
                            InputStreamReader(pro.getErrorStream()));
                        String buffer = null;
                        String jvmVersion = null;
                        while ((buffer = processLnRdr.readLine()) != null) {
                            int startIndex = buffer.indexOf("\"");
                            int endIndex = buffer.indexOf("\"", startIndex + 1);
                            if ((startIndex >= 0) && (endIndex >= 0) &&
                                (endIndex > startIndex)) {
                                jvmVersion = buffer.substring(startIndex + 1,
                                    endIndex);
                                break;
                            }
                        }                        
                        String startWithMinor = jvmVersion.substring(
                            jvmVersion.indexOf(".") + 1, jvmVersion.length());
                        int index = -1;
                        if ((index = startWithMinor.indexOf(".")) >= 0) {
                            startWithMinor = startWithMinor.substring(0,
                                index);
                        }
                        if ((index = startWithMinor.indexOf("_")) >= 0) {
                            startWithMinor = startWithMinor.substring(0,
                                index + 1);
                        }
                        if (Integer.valueOf(startWithMinor) >=
                            WebContainerConstant.SUPPORTED_JVM_MINOR_VERSION) {
                            toolOutWriter.printResult(jvmVersion);
                            toolOutWriter.printStatusMsg(true,
                               "webcontainer-container-version");
                        } else {
                            toolOutWriter.printError(jvmVersion);
                            toolOutWriter.printStatusMsg(false,
                               "webcontainer-container-version");
                        }                        
                    } catch (IOException ex) {
                        throw new DTException(ex.getMessage());
                    } finally {
                        if (processLnRdr != null) {
                            try {
                                processLnRdr.close();
                            } catch (Exception ignored) {}
                        }
                    }
                } else {
                    toolOutWriter.printError("webcontainer-config-dir-invalid");
                    toolOutWriter.printStatusMsg(false,
                        "webcontainer-config-dir-check"); 
                }
            } else {
                toolOutWriter.printError("webcontainer-base-dir-invalid");
                toolOutWriter.printStatusMsg(false,
                    "webcontainer-base-dir-check"); 
            }
        } else {
            toolOutWriter.printError("webcontainer-base-dir-invalid");
                toolOutWriter.printStatusMsg(false,
                    "webcontainer-base-dir-check"); 
        }                                    
    }
}
