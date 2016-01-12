/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: EmbeddedOpenSSO.java,v 1.1 2009/05/02 21:56:42 kevinserwin Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.setup;

import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import com.sun.identity.authentication.UI.LoginLogoutMapping;

/**
 * Provides interfaces to manage an embedded OpenAM instance.
 * Methods to start, configure and shutdown are provided, and additionally
 * a method to determine if the OpenAM instance is configured.
 */
public class EmbeddedOpenSSO {
    private String baseDir;
    private ServletContext servletct;
    private String uri;
    private Map configData;

    /**
     * Instance for standalone i.e., non-web based OpenAM instance.
     * Should not be used within a web application.
     *
     * @param baseDir directory to install OpenDJ (if required)
     * and directory to create debugs, logs and templates
     * @param configData configuration data such as server url, admin password,
     */
    public EmbeddedOpenSSO(String baseDir, Map configData) {
        this.baseDir = baseDir;
        uri = "/eopensso";
        this.configData = configData;
    }

    /**
     * Instance of OpenAM for web based applications. The <class>config</class>
     * parameter is used to initialize the servlets within OpenAM
     *
     * @param config servlet configuration
     * @param baseDir directory to install OpenDS (if required)
     * and directory to create debugs, logs and templates
     * @param configData configuration data such as server url, admin password,
     */
     public EmbeddedOpenSSO(ServletContext servletct, String baseDir,
        Map configData) {
        this.baseDir = baseDir;
        this.servletct = servletct;
        if (servletct != null) {
            uri = servletct.getContextPath();
        } else {
            uri = "/eopensso";
        }
        this.configData = configData;
    }

    /**
     * Determines if the instance of OpenAM is configured.
     *
     * @return true if OpenAM instance is configured; false otherwise
     */
    public boolean isConfigured() {
        return AMSetupServlet.isConfigured(baseDir);
    }

    /**
     * Configures the instance of OpenAM as provided by the
     * configuration data. In the future we should be able to return
     * an OutputStream that provides the current operation being performed.
     * 
     * @return configuration directory
     */
    public String configure() {
        if (isConfigured()) {
            return baseDir;
        }
        configData.put("DEPLOYMENT_URI", uri);
        configData.put("BASE_DIR", baseDir);
        return AMSetupServlet.configure(servletct, configData);
    }

    /**
     * Starts the instance of OpenAM
     */
    public void startup() {
        try {
            (new LoginLogoutMapping()).init(servletct);
        } catch (Exception ex) {
        }
    }

    /**
     * Shuts down the instance of OpenAM
     */
    public void shutdown() {
        // Shutdown the threads
    }
}
