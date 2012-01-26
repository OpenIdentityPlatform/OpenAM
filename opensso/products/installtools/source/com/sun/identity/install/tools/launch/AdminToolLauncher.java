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
 * $Id: AdminToolLauncher.java,v 1.5 2009/04/07 17:18:37 leiming Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */
package com.sun.identity.install.tools.launch;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AdminToolLauncher {

    private static void launchAdminTool(List args) throws Exception {
        String fullName = AdminToolLauncher.class.getName();
        int index = fullName.lastIndexOf('.');
        String shortClassName = fullName;
        if (index != -1) {
            shortClassName = fullName.substring(index + 1);
        }
        String classFileName = shortClassName + ".class";

        URL url = AdminToolLauncher.class.getResource(classFileName);
        if (url == null) {
            throw new Exception("Failed to locate resource: " + classFileName);
        }
        String urlString = url.toString();
        // This urlString is URL-encoded, decode it.
        urlString = URLDecoder.decode(urlString, "UTF-8");
        
        String jarFileName = null;
        String jarFilePath = null;
        if (urlString.startsWith(STR_JAR_FILE_URL_PREFIX)) {
            int entryIndex = urlString.indexOf('!');
            if (entryIndex != -1) {
                jarFilePath = urlString.substring(STR_JAR_FILE_URL_PREFIX
                        .length(), entryIndex);
                int jarNameIndex = jarFilePath.lastIndexOf('/');
                if (jarNameIndex != -1) {
                    jarFileName = jarFilePath.substring(jarNameIndex + 1);
                } else {
                    jarFileName = jarFilePath;
                }
            } else {
                throw new Exception("Failed to locate jar entry in: "
                        + urlString);
            }
        }

        if (jarFileName == null) {
            throw new Exception("Failed to locate launcher jar: " + urlString);
        }

        setLauncherJarFileName(jarFileName);

        ArrayList pathURLs = new ArrayList();

        String relativePath = STR_LIB_DIR_PREFIX + jarFileName;
        String productHome = jarFilePath.substring(0, jarFilePath.length()
                - relativePath.length());
        debug("product home=" + productHome);

        // Detect some commonly used special characters, which are not
        // allowed in Agent deployment directory.
        if (productHome.matches(".*[% #+].*")) {
            throw new IOException(
                "Agent deployment directory may have special " +
                "character(% #+), rename it to a new directory name.");
        }
        // Product home must exist
        setProductHomeDir(getRequiredDirectory(productHome, false));

        // Product bin dir must exist
        setProductBinDir(getRequiredDirectory(productHome + STR_BIN_DIR_PREFIX,
                false));

        // Config dir must exist
        setProductConfigDir(getRequiredDirectory(productHome
                + STR_CONFIG_DIR_PREFIX, false));

        // Data dir: create if does not exist
        setProductDataDir(getRequiredDirectory(productHome
                + STR_DATA_DIR_PREFIX, true));

        // Etc dir: create if does not exist
        setProductEtcDir(getRequiredDirectory(productHome + STR_ETC_DIR_PREFIX,
                true));

        // Set JCE dir
        if (isDirectoryExisting(productHome + STR_JCE_DIR_PREFIX)) {
            setProductJCEDir(getRequiredDirectory(
                productHome + STR_JCE_DIR_PREFIX, false));
        }

        // Set JSSE dir
        if (isDirectoryExisting(productHome + STR_JSSE_DIR_PREFIX)) {
            setProductJSSEDir(getRequiredDirectory(
                productHome + STR_JSSE_DIR_PREFIX, false));
        }

        // Lib dir must exist
        setProductLibDir(getRequiredDirectory(productHome + STR_LIB_DIR_PREFIX,
                false));

        // Locale dir must exist
        setProductLocaleDir(getRequiredDirectory(productHome
                + STR_LOCALE_DIR_PREFIX, false));
        // pathURLs.add(getProductLocaleDir().toURL());

        // Logs dir: create if does not exist
        setProductLogsDir(getRequiredDirectory(productHome
                + STR_LOGS_DIR_PREFIX, true));

        initJavaVersion();

        ArrayList excludedFileList = new ArrayList();
        excludedFileList.add(getLauncherJarFileName());
        excludedFileList.add(STR_JDK_LOGGING_JAR_NAME);

        ArrayList pathElements = new ArrayList();
        addFilePaths(getProductLibDir(), pathElements, pathURLs,
                excludedFileList);
        pathElements.add(getProductLocaleDir().getAbsolutePath());
        pathURLs.add(getProductLocaleDir().toURL());
        pathElements.add(getProductConfigDir().getAbsolutePath());
        pathURLs.add(getProductConfigDir().toURL());

        if (!isJDK14OrAbove()) {
            addFilePaths(getProductJCEDir(), pathElements, pathURLs);
            addFilePaths(getProductJSSEDir(), pathElements, pathURLs);
            System.setProperty(IAdminTool.PROP_REGISTER_JCE_PROVIDER, "true");
            System.setProperty(IAdminTool.PROP_REGISTER_JSSE_PROVIDER, "true");
        }
        setClassPathElements(pathElements);

        initializeClassPath(pathElements);

        debug("Path URLS: " + pathURLs);
        URL[] pathEntries = new URL[pathURLs.size()];
        System
                .arraycopy(pathURLs.toArray(), 0, pathEntries, 0, pathURLs
                        .size());

        URLClassLoader loader = new URLClassLoader(pathEntries);
        Thread.currentThread().setContextClassLoader(loader);
        debug("Context thread loader has been set.");

        Class toolsConfiguration = null;
        try {
            toolsConfiguration = loader
                    .loadClass(STR_TOOLS_CONFIGURATION_CLASSNAME);
        } catch (Exception ex) {
            System.out.println(
                    "Error: the Exception might be caused by " +
                    "special character in Agent deployment directory.");
            throw ex;
        }
        // Passing null for parameterTypes because there are no arguments and
        // it is a static method - hence no instance of class.
        Class[] parameterTypes = {};
        Method toolsMethod = toolsConfiguration.getDeclaredMethod(
                STR_TOOLS_CONFIGURATION_METHODNAME, parameterTypes);

        // Since the call is for static method, the first parameter to the
        // invoke method is null.
        Object[] methodArguments = {};
        IAdminTool adminTool = (IAdminTool) toolsMethod.invoke(null,
                methodArguments);
        adminTool.run(args);
    }

    private static File getRequiredDirectory(String path, boolean create)
            throws Exception {
        File dir = new File(path);
        if (dir.exists()) {
            if (!dir.isDirectory()) {
                throw new Exception("Invalid directory: " + dir);
            } else {
                debug("Directory: " + dir + " exists and is valid directory");
            }
        } else {
            if (create) {
                if (!dir.mkdirs()) {
                    throw new Exception("Unable to create directory: " + dir);
                } else {
                    debug("Directory: " + dir + " created successfully.");
                }
            } else {
                throw new Exception("Invalid directory: " + dir);
            }
        }

        return dir;
    }

    private static boolean isDirectoryExisting(String path) 
        throws SecurityException {
        boolean isDirExisting = false;
        if (path != null) {
            File dir = new File(path);
            isDirExisting = dir.isDirectory();
        }
        return isDirExisting;
    }  

    private static void initializeClassPath(ArrayList classPathElements) {
        String currentClassPath = System.getProperty(STR_JAVA_CLASSPATH);
        debug("Current classpath: " + currentClassPath);

        if (currentClassPath.endsWith(File.pathSeparator)) {
            currentClassPath = currentClassPath.substring(0, currentClassPath
                    .length() - 1);
        }
        StringBuffer buff = new StringBuffer();
        buff.append(currentClassPath);
        Iterator it = classPathElements.iterator();
        while (it.hasNext()) {
            buff.append(File.pathSeparatorChar);
            buff.append(it.next());
        }

        System.setProperty(STR_JAVA_CLASSPATH, buff.toString());
        debug("Final Classpath: " + buff.toString());
    }

    private static void addFilePaths(File dir, ArrayList list, ArrayList urls)
            throws Exception {
        addFilePaths(dir, list, urls, new ArrayList());
    }

    private static void addFilePaths(File dir, ArrayList list, ArrayList urls,
            ArrayList excluded) throws Exception {
        File[] files = dir.listFiles();
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                File nextFile = files[i];
                String name = nextFile.getName();
                if (!excluded.contains(name)) {
                    list.add(nextFile.getAbsolutePath());
                    urls.add(nextFile.toURL());
                }
            }
        }
    }

    private static void initJavaVersion() throws Exception {
        String version = System.getProperty(STR_JAVA_VERSION);
        int firstSeparatorIndex = version.indexOf('.');
        String majorVersion = version.substring(0, firstSeparatorIndex);
        int secondSeparatorIndex = version
                .indexOf('.', firstSeparatorIndex + 1);
        String minorVersion = version.substring(firstSeparatorIndex + 1,
                secondSeparatorIndex);

        int majorVersionInt = Integer.parseInt(majorVersion);
        int minorVersionInt = Integer.parseInt(minorVersion);

        int code = majorVersionInt * 10 + minorVersionInt;

        if (code < 13) {
            throw new Exception("Unsupported JDK version in use: " + version);
        } else if (code == 13) {
            setIsJDK14OrAbove(false);
        } else if (code > 13) {
            setIsJDK14OrAbove(true);
        }
    }

    public static void main(String[] args) {
        try {
            boolean debugEnabled = false;
            ArrayList filteredArgs = new ArrayList();
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals(IAdminTool.STR_DEBUG_OPTION)) {
                    debugEnabled = true;
                } else {
                    filteredArgs.add(args[i]);
                }
            }
            setDebugEnabledFlag(debugEnabled);
            launchAdminTool(filteredArgs);
        } catch (Exception ex) {
            System.err.println("Failed to launch AdminTool");
            ex.printStackTrace(System.err);
        }
    }

    private static void setLauncherJarFileName(String jarName) {
        jarFileName = jarName;
        debug("Launcher Jar File: " + jarFileName);
    }

    private static String getLauncherJarFileName() {
        return jarFileName;
    }

    private static void setLocationProperty(String propertyName, File location)
    {
        String path = location.getAbsolutePath().replace('\\', '/');
        setSystemProperty(propertyName, path);
    }

    private static void setSystemProperty(String propertyName, String value) {
        System.setProperty(propertyName, value);
        debug("Added system property: " + propertyName + "=" + value);
    }

    private static void setProductLibDir(File libDir) {
        productLibDir = libDir;
        setLocationProperty(IAdminTool.PROP_LIB_DIR, productLibDir);
        debug("Product lib dir: " + productLibDir);
    }

    private static File getProductLibDir() {
        return productLibDir;
    }

    private static void setProductHomeDir(File homeDir) {
        productHomeDir = homeDir;
        setLocationProperty(IAdminTool.PROP_PRODUCT_HOME, productHomeDir);
        debug("Product home dir: " + productHomeDir);
    }

    private static void setProductBinDir(File binDir) {
        productBinDir = binDir;
        setLocationProperty(IAdminTool.PROP_BIN_DIR, productBinDir);
    }

    private static void setProductEtcDir(File etcDir) {
        productEtcDir = etcDir;
        setLocationProperty(IAdminTool.PROP_ETC_DIR, productEtcDir);
    }

    private static File getProductEtcDir() {
        return productEtcDir;
    }

    private static File getProductBinDir() {
        return productBinDir;
    }

    private static File getProductHomeDir() {
        return productHomeDir;
    }

    private static void setDebugEnabledFlag(boolean debugEnbled) {
        debugEnabled = debugEnbled;
        setSystemProperty(IAdminTool.PROP_DEBUG_ENABLED, String
                .valueOf(debugEnabled));
    }

    private static boolean isDebugEnabled() {
        return debugEnabled;
    }

    private static void debug(String message) {
        if (isDebugEnabled()) {
            System.out.println("AdminToolLauncher: " + message);
        }
    }

    private static File getProductLocaleDir() {
        return productLocaleDir;
    }

    private static void setProductLocaleDir(File localeDir) {
        productLocaleDir = localeDir;
        setLocationProperty(IAdminTool.PROP_LOCALE_DIR, productLocaleDir);
        debug("Product locale dir: " + productLocaleDir);
    }

    private static ArrayList getClassPathElements() {
        return classPathElements;
    }

    private static void setClassPathElements(ArrayList pathElements) {
        classPathElements = pathElements;
        debug("Product classpath elements: " + pathElements);
    }

    private static boolean isJDK14OrAbove() {
        return isJDK14orAbove;
    }

    private static void setIsJDK14OrAbove(boolean flag) {
        isJDK14orAbove = flag;
        debug("Is JDK 14 or Above: " + flag);
    }

    private static File getProductJCEDir() {
        return productJCEDir;
    }

    private static void setProductJCEDir(File JCEDir) {
        productJCEDir = JCEDir;
        setLocationProperty(IAdminTool.PROP_JCE_DIR, productJCEDir);
        debug("Product JCE Dir: " + productJCEDir);
    }

    private static File getProductJSSEDir() {
        return productJSSEDir;
    }

    private static void setProductJSSEDir(File JSSEDir) {
        productJSSEDir = JSSEDir;
        setLocationProperty(IAdminTool.PROP_JSSE_DIR, productJSSEDir);
        debug("Product JSSE Dir: " + productJSSEDir);
    }

    private static File getProductConfigDir() {
        return productConfigDir;
    }

    private static void setProductConfigDir(File configDir) {
        productConfigDir = configDir;
        setLocationProperty(IAdminTool.PROP_CONFIG_DIR, productConfigDir);
        debug("Product Config Dir: " + productConfigDir);
    }

    private static File getProductLogsDir() {
        return productLogsDir;
    }

    private static void setProductLogsDir(File logsDir) {
        productLogsDir = logsDir;
        setLocationProperty(IAdminTool.PROP_LOGS_DIR, productLogsDir);
        debug("Product Logs Dir: " + productLogsDir);
    }

    private static File getProductDataDir() {
        return productDataDir;
    }

    private static void setProductDataDir(File dataDir) {
        productDataDir = dataDir;
        setLocationProperty(IAdminTool.PROP_DATA_DIR, productDataDir);
        debug("Product Data Dir: " + productDataDir);
    }

    private static String jarFileName;

    private static File productHomeDir;

    private static File productBinDir;

    private static File productLibDir;

    private static File productConfigDir;

    private static File productLocaleDir;

    private static File productJCEDir;

    private static File productJSSEDir;

    private static File productLogsDir;

    private static File productDataDir;

    private static File productEtcDir;

    private static boolean debugEnabled;

    private static ArrayList classPathElements;

    private static boolean isJDK14orAbove;

    private static final String STR_JAR_FILE_URL_PREFIX = "jar:file:";

    private static final String STR_LIB_DIR_PREFIX = "/lib/";

    private static final String STR_BIN_DIR_PREFIX = "/bin/";

    private static final String STR_CONFIG_DIR_PREFIX = "/config/";

    private static final String STR_LOCALE_DIR_PREFIX = "/locale/";

    private static final String STR_LOGS_DIR_PREFIX = "/installer-logs/";

    private static final String STR_DATA_DIR_PREFIX = "/data/";

    private static final String STR_ETC_DIR_PREFIX = "/etc/";

    private static final String STR_JDK_LOGGING_JAR_NAME = "jdk_logging.jar";

    private static final String STR_JAVA_VERSION = "java.version";

    private static final String STR_JCE_DIR_PREFIX = "/jce/";

    private static final String STR_JSSE_DIR_PREFIX = "/jsse/";

    private static final String STR_JAVA_CLASSPATH = "java.class.path";

    private static final String STR_TOOLS_CONFIGURATION_CLASSNAME = 
        "com.sun.identity.install.tools.admin.ToolsConfiguration";

    private static final String STR_TOOLS_CONFIGURATION_METHODNAME = 
        "getAdminTool";

}
