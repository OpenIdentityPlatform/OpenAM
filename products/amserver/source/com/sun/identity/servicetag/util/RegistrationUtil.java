/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.identity.servicetag.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.common.Constants;

public class RegistrationUtil {

    private static final String REGISTRATION = "registration";
    private static final String LIB = "lib";
    private static final String SERVICE_TAG_REGISTRY_BASE = "servicetag-registry";
    private static final String SERVICE_TAG_REGISTRY_NAME = SERVICE_TAG_REGISTRY_BASE + ".xml";
    private static final String SERVICE_TAG_REGISTRY_LINK_NAME = SERVICE_TAG_REGISTRY_BASE + ".lnk";

    /**
     * @return home for registration
     */
    public static File getRegistrationHome() {
//        String installRoot = System.getProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY);
        String installRoot = SystemProperties.get(SystemProperties.CONFIG_PATH);
        String serviceUri =
            SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        installRoot = installRoot + serviceUri;
        return getRegistrationHome(installRoot);
    }

    /**
     * @return home for registration relative to an installRoot
     */
    public static File getRegistrationHome(String installRoot) {
        File lib = new File(installRoot, LIB);
        File registration = new File(lib, REGISTRATION);
        return registration;
    }

    /**
     * @return Service tag file. Please note that it is possible that the
     *     file may not actually exist
     */
    public static File getServiceTagRegistry() {
        File serviceTagRegistry = new File(getRegistrationHome(), SERVICE_TAG_REGISTRY_NAME);
        return getServiceTagRegistry(serviceTagRegistry);
    }

    /**
     * @return Service tag file relative to an installRoot. Please note that it is possible that the file may not actually exist
     */
    public static File getServiceTagRegistry(String installRoot) {
        File serviceTagRegistry = new File(getRegistrationHome(installRoot), SERVICE_TAG_REGISTRY_NAME);        
        return getServiceTagRegistry(serviceTagRegistry);
    }
    
    private static File getServiceTagRegistry(File serviceTagRegistry) {
        if (!serviceTagRegistry.exists()) {
            // It is possible that we are embedded inside other product check for a link to registration file
            File serviceTagLink = new File(getRegistrationHome(), SERVICE_TAG_REGISTRY_LINK_NAME);
            if (serviceTagLink.exists()) {
                try {
                    BufferedReader in = new BufferedReader(new FileReader(serviceTagLink));
		    // The first line in the link file is expected to contain fully qualified path to actual service tag repository
                    String indirectedServiceTagRegistryName = in.readLine();
                    File indirectedServiceTagRegisitryFile = new File(indirectedServiceTagRegistryName);
                    if(indirectedServiceTagRegisitryFile.exists()) {
                        // Return indirectedServiceTagRegisitryFile as the serviceTagRegistry only if it exists
                        serviceTagRegistry = indirectedServiceTagRegisitryFile;
                    }
                    in.close();
                } catch (IOException e) {
                    // I/O error occurred. There is not much we can do to recover. Assumer that service tags are not present
		    // TODO: Check with Kedar, if a logger can be used here to log a debug message
                }
            } else {
                // the link also does not exist. Fall through and return serviceTagRegistry as the
            }
        }
        return serviceTagRegistry;
    }
}
