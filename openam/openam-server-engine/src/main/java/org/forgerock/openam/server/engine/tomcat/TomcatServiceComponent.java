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


import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.forgerock.openam.server.engine.commons.shutdown.ServiceInstanceShutdownLogger;

import javax.annotation.PreDestroy;
import java.io.File;


/**
     * Embedded Tomcat as our WEB Interface Layer.
     * @author jeffaschenk@gmail.com
     */
    //@Service("tomcat")
    public class TomcatServiceComponent {
        /**
         * Logging
         */
        //private final static Logger logger = LoggerFactory.getLogger(TomcatServiceComponent.class);


        //@Value("#{systemEnvironmentProperties['tomcat.app.base']}")
        private String appBase;

        //@Value("#{systemEnvironmentProperties['tomcat.app.port']}")
        private Integer appPort;

        //@Value("#{systemEnvironmentProperties['tomcat.app.context.path']}")
        private String contextPath;

        //@Value("#{systemEnvironmentProperties['tomcat.app.base.dir']}")
        private String baseDirectory;

        //@Value("#{systemEnvironmentProperties['tomcat.app.hostname']}")
        private String appHostName;


        /**
         * Initialization Indicator.
         */
        private boolean initialized = false;

        /**
         * Tomcat Service Thread
         */
        private TomcatServiceThread tomcatServiceThread;

        /**
         * Task Executor
         */
       //TaskExecutor taskExecutor;

        /**
         * Default Constructor
         */
        public TomcatServiceComponent() {
        }


        public synchronized void initialize() {

            if (this.initialized) {
                // Instance Already Initialized, ignore the request.
                //logger.error("Tomcat instance already initialized, ignoring request");
            }

            tomcatServiceThread = new TomcatServiceThread(appBase, appPort, appHostName, contextPath, baseDirectory);

            //logger.info("Starting Background Thread for Embedded Tomcat Service Facility");
            //taskExecutor.execute(tomcatServiceThread);
            //logger.info("Completed Background Embedded Tomcat Service Facility Thread Initialization.");
            this.initialized = true;

        }

        /**
         * Destroy Service
         * Invoked during Termination of the Spring Container.
         */
        @PreDestroy
        public synchronized void destroy() {
            //logger.info("Stopping Background Thread for Embedded Tomcat Service Facility");

            this.tomcatServiceThread.endServiceInstance();

            //logger.info("Completed Background Embedded Tomcat Service Facility Thread shutdown.");
        }

        /**
         * Embedded Tomcat as our WEB Interface Layer.
         */
        //@Service("tomcat_service_thread")
        public class TomcatServiceThread extends Thread {


            /**
             * Initialization Indicator.
             */
            private boolean running = false;

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
            private TomcatServiceThread() {
            }


            /**
             * Background Task Constructor with All necessary parameters.
             */
            protected TomcatServiceThread(String appBase, Integer appPort, String appHostName, String contextPath, String baseDirectory) {
                this.appBase = appBase;
                this.appPort = appPort;
                this.appHostName = appHostName;
                this.contextPath = contextPath;
                this.baseDirectory = baseDirectory;
            }

            @Override
            public synchronized void run() {
                if (running) {
                    //logger.warn("Attempted to start another thread when Tomcat Thread is running, check for redundant spring wiring.");
                    return;
                }
                try {
                    this.running = true;
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

            /**
             * End the Service Instance.
             */
            protected synchronized void endServiceInstance() {
                ServiceInstanceShutdownLogger.log(this.getClass(), "INFO", "Stopping Embedded Tomcat Service Facility.");
                try {
                    this.tomcat.stop();
                    //this.tomcat.destroy();
                } catch (LifecycleException lifecycleException) {
                    ServiceInstanceShutdownLogger.log(this.getClass(), "ERROR", "Embedded Tomcat Life Cycle Exception:" + lifecycleException, lifecycleException);
                }
            }

        }

}
