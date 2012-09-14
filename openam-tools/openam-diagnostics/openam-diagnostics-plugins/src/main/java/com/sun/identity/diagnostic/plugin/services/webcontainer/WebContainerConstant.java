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
 * $Id: WebContainerConstant.java,v 1.1 2008/11/22 02:41:23 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.webcontainer;

/**
 * This class contains the constants used by the web-container configuration 
 * service.
 */

public class WebContainerConstant {
    
    public static final String POLICIES_FILE = "file.policy";
    
    public static final String POLICIES_DIR = "dir.policy";
    
    public static final String POLICIES_PATTERN_FILE = "PoliciesPatternFile";
    
    public static final String JAVAHOME_FILE = "file.javahome";
    
    public static final String JAVAHOME_DIR = "dir.javahome";
    
    public static final String JAVAHOME_TAG = "tag.javahome";
    
    public static final String JVMOPTIONS_FILE = "file.jvmoptions";
    
    public static final String JVMOPTIONS_DIR = "dir.jvmoptions";
    
    public static final String JVMOPTIONS_PATTERN = "pattern.jvmoptions.";
    
    public static final String JVMOPTIONS_CLEARTEXT = "cleartext.jvmoptions.";
    
    public static final String JVMOPTIONS_PATTERN_FILE =
        "JVMOptionsPatternFile";
    
    public static final String POLICIES_PATTERN = "pattern.policies.";
    
    public static final String POLICIES_CLEARTEXT = "cleartext.policies.";
    
    public static final String FILE_SEPARATOR = "/";
    
    public static final String OS_NAME = System.getProperty("os.name");
    
    public static final String WINDOWS = "windows";
    
    public static final String SOLARIS = "solaris";
    
    public static final String LINUX = "linux";    
    
    public static final String WEBCONTAINER_RESOURCE_BUNDLE = "WebContainer";
    
    public static final int SUPPORTED_JVM_MINOR_VERSION = 5;
    
    public static final String POLICY_GRANT_START_PATTERN = "\\s*grant\\s*";
    
    public static final String POLICY_SINGLELINE_COMMENTS = "//";
    
    public static final String POLICY_GRANT_END_PATTERN = "\\s*[}]\\s*";
   
}
