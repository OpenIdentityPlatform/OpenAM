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
 * $Id: ToolContext.java,v 1.1 2008/11/22 02:19:53 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.diagnostic.base.core.common.ToolConstants;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.diagnostic.base.core.service.ToolService;
import com.sun.identity.diagnostic.base.core.service.ToolServiceManager;
import com.sun.identity.shared.debug.Debug;

/**
 * This class represents the context of the tool application.
 */

public class ToolContext implements ToolConstants {
    
    private ToolServiceManager sManager = null;
    private ToolManager tManager = null;
    private String appHome = null;
    private String runMode = null;
    private List<String> serviceLocs = new ArrayList<String>();
    
    /**
     * Creates a new instance of ToolContext.
     *
     * @param tManager Tool manager that manages this context.
     * @param sManager Tool Service Manager that runs in this context.
     *
     */
    public ToolContext(
        ToolManager tManager,
        ToolServiceManager sManager
    ) {
        this.sManager = sManager;
        this.tManager = tManager;
    }
    
    /**
     * This method takes care of loading application environment
     * and configuration. It's called once during application start
     * up.
     */
    public void configure() {
        try {
            appHome = (String) System.getProperty(TOOL_BASE_DIR);
            if (appHome == null || appHome.length() == 0) {
                appHome = new File(".").getCanonicalPath();
            }
            runMode = (String) SystemProperties.get(TOOL_RUN_MODE);
            if (runMode == null || runMode.length() == 0 ||
                runMode.equalsIgnoreCase(GUI_MODE)) {
                runMode = GUI_MODE;
            } else {
                runMode = CLI_MODE;
            }
        } catch (IOException e) {
            Debug.getInstance(DEBUG_NAME).error(
                "ToolContext.configure: " + e.getMessage());
        }
    }
    
    /**
     * Returns the application run mode which is one of GUI or CLI.
     *
     * @return Run mode of the application.
     */
    public String getMode() {
        return runMode;
    }
    
    /**
     * Returns the path to application home.
     *
     * @return Path to Application home.
     */
    public String getApplicationHome() {
        return appHome;
    }
    
    /**
     * Returns the path to service home for a given service id.
     *
     * @param id id of the service for which home dir is to be obtained.
     * @return Path to service home for the given service.
     */
    public URL getServiceHome(String id) {
        return sManager.getRegistry().getServiceHome(id);
    }
    
    /**
     * Returns the categories from all the registered services
     *
     * @return HashMap to registered service categories
     */
    public HashMap<String, HashMap> getServiceCategories() {
        return sManager.getRegistry().getAllServiceCategories();
    }
    
    /**
     * Returns the map of operation and service name for a given
     * category and service name
     *
     * @return HashMap of operation and service class
     */
    public HashMap<String, String> getServiceCategories(
        String category,
        String serviceName
    ) {
        return sManager.getRegistry().getServiceDetails(category, serviceName);
    }
    
    /**
     * Returns the requested service.
     *
     * @param id id of the service that is requested.
     * @return An instance of the requested service.
     */
    public ToolService getService(String id) {
        return sManager.getService(id);
    }
    
    /**
     * Returns the list of all services that are currently active.
     *
     * @return list of active services.
     */
    public List<String> getAllActiveServices() {
        return sManager.getAllActiveServices();
    }
    
    /**
     * Returns true if the specified service is active. False otherwise.
     *
     * @param id id of the service for which the status is requested.
     * @return true if the given service is active, false otherwise.
     */
    public boolean isServiceActive(String id){
        return sManager.isServiceActive(id);
    }
    
    /**
     * Returns the default service repository location i.e. TOOL_HOME/services
     *
     * @return the location of the default service repository.
     */
    public String getDefaultServiceRepository() {
        return getApplicationHome() + File.separatorChar + 
            SERVICES_REPOSITORY_FOLDER;
    }
    
    /**
     * Returns the custom service repository location.
     *
     * @return the location of the default service repository.
     */
    public List<String> getCustomServiceRepositories() {
        return serviceLocs;
    }
    
    /**
     * Returns the output writer for the tool
     *
     * @return the output writer based on the run mode 
     */
    public IToolOutput getOutputWriter() {
        return tManager.getToolOutputManager().getOutputWriter();
    }
}
