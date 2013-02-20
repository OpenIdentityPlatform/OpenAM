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
 * $Id: IBMWebSpherePoliciesValidator.java,v 1.1 2008/11/22 02:41:23 ak138937 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.diagnostic.plugin.services.webcontainer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Enumeration;

import com.sun.identity.diagnostic.base.core.common.DTException;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;

import java.util.Properties;

public class IBMWebSpherePoliciesValidator implements IWebContainerValidate {
    
    private IToolOutput toolOutWriter;
    
    /** Creates a new instance of SunAppSvrPoliciesValidator */
    public IBMWebSpherePoliciesValidator() {
    }
    
    public void validate(
        Properties containerProp, 
        String profilePath,
        String serverPath
    ) throws DTException {
        toolOutWriter = WebContainerService.getToolWriter();
        if (profilePath != null) {
            File profileDir = new File(profilePath);                                                                              
            if (profileDir.exists() && profileDir.isDirectory()) {
                toolOutWriter.printStatusMsg(true,
                    "webcontainer-profile-dir-check"); 
                String profileProp_dir = containerProp.getProperty(
                    WebContainerConstant.POLICIES_DIR);                                                
                File profilePropDir = new File(profileDir, profileProp_dir);
                if (profilePropDir.exists() && profilePropDir.isDirectory()) {
                    toolOutWriter.printStatusMsg(true,
                        "webcontainer-properties-dir-check"); 
                    String policiesFileName = containerProp.getProperty(
                        WebContainerConstant.POLICIES_FILE);                    
                    File policiesPatternFile = new File(
                        containerProp.getProperty(
                        WebContainerConstant.POLICIES_PATTERN_FILE));
                    FileInputStream fin = null;
                    Properties policiesPatternProp = new Properties();
                    if (policiesPatternFile.exists() &&
                        policiesPatternFile.isFile()) {
                        try {
                            fin = new FileInputStream(policiesPatternFile);
                            policiesPatternProp.load(fin);
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
                    try {
                        lnRdr = new LineNumberReader(new FileReader(
                            new File(profilePropDir, policiesFileName)));                        
                        String line = null;
                        StringBuffer buffer = null;
                        boolean policiesStarted = false;
                        while ((line = lnRdr.readLine()) != null) {
                            if (!policiesStarted) {
                                String[] validStart = line.split(
                                    WebContainerConstant.
                                    POLICY_GRANT_START_PATTERN);
                                if ((validStart.length > 1) ||
                                    (!validStart[0].equals(line))) {                                   
                                    buffer = new StringBuffer();
                                    buffer.append(line).append("\n");
                                    policiesStarted = true;
                                }                                                                                   
                            } else {                                
                                buffer.append(line).append("\n");
                                String[] validEnd = line.split(
                                    WebContainerConstant.
                                    POLICY_GRANT_END_PATTERN);                                
                                if ((validEnd.length > 1) ||
                                    (!validEnd[0].equals(line))) {
                                    policiesStarted = false;
                                    String policies = buffer.toString();                                    
                                    String[] validPolicies = policies.split(
                                        WebContainerConstant.
                                        POLICY_GRANT_START_PATTERN + "[{]");
                                    if ((validPolicies.length > 1) ||
                                        (!validPolicies[0].equals(policies))) {                                                                        
                                        for (Enumeration e =
                                            policiesPatternProp.propertyNames();
                                            e.hasMoreElements();) {
                                            String name = (String)
                                                e.nextElement();
                                            if (name.startsWith(
                                                WebContainerConstant.
                                                POLICIES_PATTERN)) {
                                                String value =
                                                    policiesPatternProp.
                                                    getProperty(name);                                                  
                                                String[] answers =
                                                    policies.split(value);
                                                if ((answers.length > 1) ||
                                                    (!answers[0].equals(
                                                    policies))) {
                                                    policiesPatternProp.remove(
                                                        name);
                                                    int num = Integer.parseInt(
                                                        name.substring(
                                                        WebContainerConstant.
                                                        POLICIES_PATTERN.
                                                        length(),
                                                        name.length()));
                                                    policiesPatternProp.remove(
                                                        WebContainerConstant.
                                                        POLICIES_CLEARTEXT +
                                                        String.valueOf(num));
                                                }
                                            }
                                        }
                                    }                                                                                                            
                                }
                            }                            
                        }
                        boolean passed = true;
                        for (Enumeration e =
                            policiesPatternProp.propertyNames();
                            e.hasMoreElements();) {
                            String name = (String) e.nextElement();
                            if (name.startsWith(
                                WebContainerConstant.POLICIES_CLEARTEXT)) {
                                if (passed) {
                                    toolOutWriter.printError(
                                        "webcontainer-policies-invalid");
                                }
                                toolOutWriter.printError(
                                    policiesPatternProp.getProperty(name));
                                passed = false;
                            }
                        }
                        if (passed) {
                            toolOutWriter.printStatusMsg(true,
                                "webcontainer-policies-check");
                        } else {
                            toolOutWriter.printStatusMsg(false,
                                "webcontainer-policies-check");
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
                    toolOutWriter.printError(
                        "webcontainer-properties-dir-invalid");
                    toolOutWriter.printStatusMsg(false,
                        "webcontainer-properties-dir-check"); 
                }
            } else {
                toolOutWriter.printError("webcontainer-profile-dir-invalid");
                toolOutWriter.printStatusMsg(false,
                    "webcontainer-profile-dir-invalid"); 
            }
        }
    }
    
}
