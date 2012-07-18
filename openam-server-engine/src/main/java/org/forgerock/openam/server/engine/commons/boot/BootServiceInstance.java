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
package org.forgerock.openam.server.engine.commons.boot;


/**
 * Boot Spring Container Service Instance
 *
 * @author Jeff.A.Schenk@gmail.com
 */
public class BootServiceInstance {

    public static void main(String[] args) throws Exception {
        // **********************************************
        // Boot Container
        //AbstractApplicationContext applicationContext =
        //        SetUpSpringContainerBootStrap.init();
        //    new ServiceInstanceShutdownHook(null);
        // **************************************************
        // Access our Container Shell Service if Available.
        //Shell interactiveShell;

        /**
        try {
            if (applicationContext == null) {
                System.out.println("** Warning: Initial Application Context was Null!");
            } else if (applicationContext.getBean("interactiveShell") == null) {
                System.out.println("** No Interactive Shell Service Available!");
            } else {
                interactiveShell = (Shell) applicationContext.getBean("interactiveShell");
                if (interactiveShell == null) {
                    System.out.println("** No Interactive Shell Service Available!");
                } else {
                    Thread.sleep(1000*10); // Wait 10 Seconds then provide a prompt.
                    // Start the Interactive Shell, if Enabled.
                    interactiveShell.commandInteractiveShell();
                }
            }
        } catch (NoSuchBeanDefinitionException noSuchBeanDefinitionException) {
            System.out.println("** No Interactive Shell Bean has been defined for Use!");
        }


         **/

        // ************************************************
        // Main will not end until Spring Container is
        // shutdown and all child Threads.
    }

}
