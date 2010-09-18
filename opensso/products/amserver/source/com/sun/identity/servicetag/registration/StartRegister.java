/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: StartRegister.java,v 1.2 2008/06/25 05:43:58 qcheng Exp $
 */

package com.sun.identity.servicetag.registration;

import com.sun.identity.servicetag.util.RegistrationUtil;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.shared.debug.Debug;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;

/**
 * 
 */
public class StartRegister {

    public StartRegister() {
    }

    public static void servicetagTransfer() {
        File registrationHome = RegistrationUtil.getRegistrationHome();
        if (registrationHome == null) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.servicetagTransfer: " +
                "Can't find registration home.");
        }

        /*
         *  Put all the jars in this directory into the classpath
         *  for the registrationclassloader
         */
        final File [] files = registrationHome.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.endsWith(".jar") || name.endsWith(".zip"));
            }
        });

        /*
         *  start registration daemon only if the directory exists and
         *  it contains some jars
         */
        if (files != null && files.length != 0 ) {
            try {
                URL[] registrationJars = new URL[files.length];
                for (int i = 0; i < files.length; i++) {
                    registrationJars[i] = files[i].toURI().toURL();
                }
                URLClassLoader classLoaderRegistration =
                    new URLClassLoader(registrationJars);
                Class registrationDaemonClass = null;
                registrationDaemonClass =
                    Class.forName(
                        "com.sun.identity.servicetag.registration.RegisterFAMDaemon",
                        true, classLoaderRegistration);
                Class [] fclass = {File.class};
                Method m =
                    registrationDaemonClass.getMethod("start", fclass);
                final File stfile = RegistrationUtil.getServiceTagRegistry();
                Date startDate, stopDate;

                /*
                 * if the registration daemon doesn't complete the
                 * servicetag transfer to the local repository within
                 * 5 seconds, there was some problem.  don't hold up
                 * completion of configuration process because of it.
                 */
                synchronized (stfile) {
                    Object [] params = {stfile};
                    m.invoke((Object)null, params);
                    startDate = new Date();
                    stfile.wait(5000);
                    stopDate = new Date();
                }
                long startMS = startDate.getTime();
                long stopMS = stopDate.getTime();
                long diffMS = stopMS - startMS;
                if (Debug.getInstance(
                    SetupConstants.DEBUG_NAME).messageEnabled())
                {
                    Debug.getInstance(SetupConstants.DEBUG_NAME).message(
                        "AMSetupServlet.servicetagTransfer: " +
                        "registration daemon finished in " + diffMS + " ms.");
                }
            } catch (Exception e) {
                Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                    "AMSetupServlet.servicetagTransfer: " +
                    "Exception starting registration daemon", e);
            }
        }
    }
}

