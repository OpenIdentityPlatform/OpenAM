/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS Inc. All Rights Reserved
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
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted [2010] [ForgeRock AS]
 *
 */

package org.forgerock.openam.server.engine.tomcat;

import com.sun.identity.common.GeneralTaskRunnable;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.forgerock.openam.server.engine.commons.shutdown.ServiceInstanceShutdownLogger;

import java.io.File;


/**
 * Embedded Tomcat Service Component to provide HTTP/HTTPS Server Listeners
 *
 * @author jeff.schenk@forgerock.com
 */
public class TomcatServiceComponent extends GeneralTaskRunnable {
    /**
     * Single Instance
     */
    private static volatile TomcatServiceComponent instance;

    /**
     * Tomcat Service Thread
     */
    private static final String OPENAM_SERVER_ENGINE =
            "OPENAM_SERVER_ENGINE";
    private static Thread tomcatServiceThread;

    /**
     * Our Tomcat Instance Object.
     */
    private BetterTomcat tomcat;

    /**
     * Global Properties and Environment Injected
     */
    private String appBase;

    private Integer appPort;

    private String contextPath;

    private String baseDirectory;

    private String appHostName;

    /**
     * Default Constructor
     */
    private TomcatServiceComponent() {
    }

    /**
     * Initialize the Embedded Service Component.
     */
    private static synchronized void initialize() {

        if (instance == null) {
            instance = new TomcatServiceComponent();
            tomcatServiceThread = new Thread(instance);
            //logger.info("Starting Background Thread for Embedded Tomcat Service Facility");
            tomcatServiceThread.setName(OPENAM_SERVER_ENGINE);
            tomcatServiceThread.start();
            //logger.info("Completed Background Embedded Tomcat Service Facility Thread Initialization.");


        }

    }

    /**
     * Destroy Service
     * Invoked during Termination of the Spring Container.
     */
    public synchronized void destroy() {
        //logger.info("Stopping Background Thread for Embedded Tomcat Service Facility");

        endServiceInstance();

        //logger.info("Completed Background Embedded Tomcat Service Facility Thread shutdown.");
    }




        public synchronized void run() {
            if (tomcat == null) {
                try {
                    // Configure Our Embedded Tomcat Instance.
                    tomcat = new BetterTomcat();
                    tomcat.addConnector(BetterTomcat.Protocol.HTTP_11_NIO, this.appHostName, this.appPort.intValue());

                    // *****************************************************************
                    // For HTTPS, Specify a KeyStore.
                    //Connector https = tomcat.addConnector(BetterTomcat.Protocol.HTTPS_11_NIO, 8443);
                    //https.setProperty("keystoreFile", KEYSTORE_PATH);
                    //https.setProperty("keystorePass", "changeit");
                    // *****************************************************************

                    // Set Base Directory
                    tomcat.setBaseDir(new File(baseDirectory).getAbsolutePath());

                    // Establish a WEB Context
                    if (contextPath.equalsIgnoreCase("ROOT")) {
                        this.contextPath = "";
                    }
                    Context tcApplicationContext = tomcat.addWebapp(contextPath, new File(appBase).getAbsolutePath());
                    tcApplicationContext.setParentClassLoader(Thread.currentThread().getContextClassLoader());
                    // Start.
                    tomcat.startAndWait();

                } catch (BetterTomcatException bte) {
                    //logger.error("Servlet Exception Initializing the Tomcat ContextPath with AppBase:[" + appBase + "]", bte);
                } catch (Exception ce) {
                    //logger.error("Exception during thread running for Embedded Tomcat Execution " + ce.getMessage(), ce);
                } finally {
                    ServiceInstanceShutdownLogger.log(this.getClass(), "WARN", "Completed Background Thread for Embedded Tomcat Execution.");
                }
            }
        }

        /**
         * End the Service Instance.
         */
        protected synchronized void endServiceInstance() {
            ServiceInstanceShutdownLogger.log(this.getClass(), "INFO", "Stopping Embedded Tomcat Service Facility.");
            try {
                tomcat.stop();
                //this.tomcat.destroy();
            } catch (LifecycleException lifecycleException) {
                ServiceInstanceShutdownLogger.log(this.getClass(), "ERROR", "Embedded Tomcat Life Cycle Exception:" + lifecycleException, lifecycleException);
            }
        }

        /**
         * Adds an element to this TaskRunnable.
         *
         * @param key Element to be added to this TaskRunnable
         * @return a boolean to indicate whether the add success
         */
        public boolean addElement(Object key) {
            return false;
        }

        /**
         * Removes an element from this TaskRunnable.
         *
         * @param key Element to be removed from this TaskRunnable
         * @return A boolean to indicate whether the remove success
         */
        public boolean removeElement(Object key) {
            return false;
        }

        /**
         * Indicates whether this TaskRunnable is empty.
         *
         * @return A boolean to indicate whether this TaskRunnable is empty
         */
        public boolean isEmpty() {
            return false;
        }

        /**
         * Returns the run period of this TaskRunnable.
         *
         * @return A long value to indicate the run period
         */
        public long getRunPeriod() {
            return 0;
        }

}
