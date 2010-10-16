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
 * $Id: ToolsConfiguration.java,v 1.5 2008/08/04 19:29:26 huacui Exp $
 *
 */

package com.sun.identity.install.tools.admin;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import com.sun.identity.install.tools.launch.IAdminTool;
import com.sun.identity.install.tools.util.Debug;

/**
 * Class that provides means to obtain the bootstrap configuration information
 * needed for Install Tools classes. This class also serves as a point of Entry
 * for the AdminToolLauncher to obtain the AdminTool implementation class.  
 */
public class ToolsConfiguration {

    public static void initialize() {
        // No implementation required
    }

    private static synchronized void initializeConfiguration() {
        if (!isInitialized()) {
            URL resUrl = ClassLoader.getSystemResource(TOOLSCONFIG_FILE_NAME);
            if (resUrl == null) {
                ClassLoader cl = Thread.currentThread()
                    .getContextClassLoader();

                if (cl != null) {
                    resUrl = cl.getResource(TOOLSCONFIG_FILE_NAME);
                }
            }

            if (resUrl == null) {
                throw new RuntimeException("Failed to get configuration file:"
                        + TOOLSCONFIG_FILE_NAME);
            }

            InputStream instream = null;
            try {
                instream = new BufferedInputStream(new FileInputStream(resUrl
                        .getPath()));
                getProperties().load(instream);
                //Debug.log("ToolsConfiguration: loaded successfully");
            } catch (Exception ex) {
                Debug.log("Failed to load configuration", ex);
                throw new RuntimeException("Failed to load configuration: "
                        + ex.getMessage());
            } finally {
                if (instream != null) {
                    try {
                        instream.close();
                    } catch (Exception ex1) {
                        // No handling required
                    }
                }
            }

            // Set the product short name
            setProductShortName(
                    verifyAndGetProperty(STR_PRODUCT_SHORT_NAME_KEY));

            // Mark as initialized.
            markInitialized();
        }
    }

    public static BaseToolsResolver getToolsResolver() {
        if (resolver == null) {
            // Set the Tools Resolver Impl class
            ClassLoader loader = Thread.currentThread()
                .getContextClassLoader();
            String resolverClassName = verifyAndGetProperty(
                    STR_CONFIG_TOOLS_RESOLVER_KEY);
            try {
                Class resolver = loader.loadClass(resolverClassName);
                setToolsResolver((BaseToolsResolver) resolver.newInstance());
            } catch (Exception e) {
                Debug.log("ToolsConfiguration: Failed to load & instantiate "
                        + "class: " + resolverClassName, e);
                throw new RuntimeException("Failed to load & instantiate "
                        + "class: " + resolverClassName + ": " + 
                        e.getMessage());
            }
        }
        return resolver;
    }

    public static IAdminTool getAdminTool() {
        if (adminTool == null) {
            // Set the adminTool Impl class            
            String adminToolClass = verifyAndGetProperty(
                    STR_ADMIN_TOOL_CLASS_KEY);
            setAdminTool((IAdminTool) getInstantiatedObject(adminToolClass));
        }
        return adminTool;
    }

    private static Object getInstantiatedObject(String className) {
        Object implObject = null;
        try {
            implObject = Class.forName(className).newInstance();
        } catch (Exception ex) {
            Debug.log("ToolsConfiguration: Failed to load & instantiate "
                    + "class: " + className, ex);
            throw new RuntimeException("Failed to load & instantiate "
                    + "class: " + className + ": " + ex.getMessage());
        }
        return implObject;
    }

    private static String verifyAndGetProperty(String key) {

        String value = getProperties().getProperty(key);
        if ((value == null) || (value.length() == 0)) {
            String errorMessage = "No value specified for" + "property: " + key
                    + " in: " + TOOLSCONFIG_FILE_NAME;
            Debug.log("ERROR: ToolsConfiguration - " + errorMessage);
            throw new RuntimeException(errorMessage);
        }

        return value;
    }

    private static Properties getProperties() {
        return properties;
    }

    private static boolean isInitialized() {
        return initialized;
    }

    private static void markInitialized() {
        if (!isInitialized()) {
            initialized = true;
        }
    }

    private static void setToolsResolver(BaseToolsResolver resolv) {
        resolver = resolv;
    }

    public static String getProductShortName() {
        return productShortName;
    }

    private static void setProductShortName(String productName) {
        productShortName = productName;
    }

    private static void setAdminTool(IAdminTool adminToolImpl) {
        adminTool = adminToolImpl;
    }

    private static boolean initialized;

    private static Properties properties = new Properties();

    private static BaseToolsResolver resolver = null;

    private static String productShortName = null;

    private static IAdminTool adminTool = null;

    private static final String TOOLSCONFIG_FILE_NAME = 
        "OpenSSOInstallerConfig.properties";

    private static final String STR_CONFIG_TOOLS_RESOLVER_KEY = 
        "com.sun.identity.install.tools.resolver";

    private static final String STR_PRODUCT_SHORT_NAME_KEY = 
        "com.sun.identity.install.tools.product.shortname";

    private static final String STR_ADMIN_TOOL_CLASS_KEY = 
        "com.sun.identity.install.tools.adminTool.class";

    static {
        initializeConfiguration();
    }
}
