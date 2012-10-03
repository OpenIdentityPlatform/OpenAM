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

package org.forgerock.openam.server.engine.commons.shutdown;

/**
 * Shutdown Service Instance Hook.
 *
 * @author Jeff.Schenk@forgerock.com
 */
public class ServiceInstanceShutdownHook extends Thread {

    /**
     * Logging
     */
    //private final static org.slf4j.Logger logger = LoggerFactory.getLogger(ServiceInstanceShutdownHook.class);


    /**
     * Default Constructor
     */
    public ServiceInstanceShutdownHook() {
        super();
        //this.applicationContext = applicationContext;
        //logger.info("Establishing Shutdown Hook.");
        this.establishJVMShutdownHook();
        //logger.info("Established Shutdown Hook.");
    }

    /**
     * Establish Our Shutdown Hook for the JVM.
     */
    private void establishJVMShutdownHook() {



        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                this.setName("SHUTDOWN-THREAD");
                ServiceInstanceShutdownLogger.log(this.getClass(), "INFO", "Shutdown Thread caught JVM Interrupt.");
                try {
                    ServiceInstanceShutdownLogger.log(this.getClass(), "INFO", "Ordered Shutdown Commencing.");
                    // Wait for Container to Finish all PostDestroy and Destroy Processing
                    //while ((applicationContext != null) && (
                    //        (applicationContext.isRunning()) || (applicationContext.isActive()))) {
                    //    Thread.sleep(100);
                    //}
                    // Done.
                    ServiceInstanceShutdownLogger.log(this.getClass(), "INFO", "Done.");
                } catch (Exception e) {
                    ServiceInstanceShutdownLogger.log(this.getClass(), "ERROR", "Embedded Tomcat Life Cycle Exception:" + e.getMessage(), e);
                }
            }

        });

    }


}
