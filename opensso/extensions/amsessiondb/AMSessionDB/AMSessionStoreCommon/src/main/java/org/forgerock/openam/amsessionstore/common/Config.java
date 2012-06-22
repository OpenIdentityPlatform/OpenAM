/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.amsessionstore.common;

/**
 *
 * @author steve
 */
public class Config {
    private final static String OS_NAME = "os.name";
    private final static String OS_VERSION = "os.version";
    private final static String OS_ARCH = "os.arch";
    private final static String JAVA_VERSION = "java.version";
    private final static String JAVA_HOME = "java.home";
    private final static String JAVA_RUNTIME_NAME = "java.runtime.name";
    private final static String JAVA_VM_NAME = "java.vm.name";
    private final static String JAVA_VM_VERSION = "java.vm.version";
    private final static String JVM_DATA_MODEL = "sun.arch.data.model";
    private final static String LOCALE = "user.language";
    private final static String CLASSPATH = "java.class.path";
    private final static String JVM_SPEC_VENDOR = "java.vm.specification.vendor";
    
    private static String osName;
    private static String osVersion;
    private static String osArch;
    private static String javaVersion;
    private static String javaHome;
    private static String javaRuntimeName;
    private static String javaVMName;
    private static String javaVMVersion;
    private static String jvmDataModel;
    private static String locale;
    private static String classpath;
    private static String jvmSpecVendor;
    private static String dbImpl;
    
    private static Config instance = null;
    
    static {
        initialize();
    }
    
    private static void initialize() {
        osName = System.getProperty(OS_NAME);
        osVersion = System.getProperty(OS_VERSION);
        osArch = System.getProperty(OS_ARCH);
        javaVersion = System.getProperty(JAVA_VERSION);
        javaHome = System.getProperty(JAVA_HOME);
        javaRuntimeName = System.getProperty(JAVA_RUNTIME_NAME);
        javaVMName = System.getProperty(JAVA_VM_NAME);
        javaVMVersion = System.getProperty(JAVA_VM_VERSION);
        jvmDataModel = System.getProperty(JVM_DATA_MODEL);
        locale = System.getProperty(LOCALE);
        classpath = System.getProperty(CLASSPATH);
        jvmSpecVendor = System.getProperty(JVM_SPEC_VENDOR);
        dbImpl = SystemProperties.get(Constants.PROPERTIES_FILE, "");
    }
    
    public Config() {
        // do nothing
    }
    
    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        
        return instance;
    }
    
    public String getOsName() {
        return osName;
    }
    
    public String getOsVersion() {
        return osVersion;
    }
    
    public String getOsArch() {
        return osArch;
    }
    
    public String getJavaVersion() {
        return javaVersion;
    }
    
    public String getJavaHome() {
        return javaHome;
    }
    
    public String getJavaRuntimeName() {
        return javaRuntimeName;
    }
    
    public String getJavaVMName() {
        return javaVMName;
    }
    
    public String getJavaVMVersion() {
        return javaVMVersion;
    }
    
    public String getJvmDataModel() {
        return jvmDataModel;
    }
            
    public String getLocale() {
        return locale;
    }
    
    public String getClasspath() {
        return classpath;
    }
    
    public String getJvmSpecVendor() {
        return jvmSpecVendor;
    }
    
    public String getDbImpl() {
        return dbImpl;
    }
}
