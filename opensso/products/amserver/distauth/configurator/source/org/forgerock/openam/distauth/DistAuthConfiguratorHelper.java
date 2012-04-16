/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.distauth;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import com.sun.identity.distauth.setup.SetupDistAuthWAR;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * This class is responsible for ensuring that the base OpenAM SDK subsystem
 * has been initialised.
 *
 * @author Steve Ferris steve.ferris@forgerock.com
 */
public class DistAuthConfiguratorHelper {
    /**
     * This method is called to determine if the distauth webapp has been configured
     *
     * @param servletCtx The Servlet Context
     * @return true if configured, false otherwise
     * @throws ServletException
     */
    public static boolean initialiseDistAuth(ServletContext servletCtx)
    throws ServletException {
        String configFile = System.getProperty("user.home") + File.separator +
                                Constants.CONFIG_VAR_DISTAUTH_BOOTSTRAP_BASE_DIR +
                                File.separator +
                                SetupDistAuthWAR.getNormalizedRealPath(servletCtx) +
                                Constants.CONFIG_VAR_DISTAUTH_BOOTSTRAP_FILENAME;
        File file = new File(configFile);

        if (file.exists()) {
            return setAMDistAuthConfigProperties(configFile);
        } else {
            return false;
        }
    }

    /**
     * Sets properties from AMDistAuthConfig.properties
     * @param configFile path to the AMDistAuthConfig.properties file
     * @return true if configured, false otherwise
     * @throws ServletException when error occurs
     */
    private static boolean setAMDistAuthConfigProperties(String configFile)
    throws ServletException {
        FileInputStream fileStr = null;

        try {
            fileStr = new FileInputStream(configFile);
            if (fileStr != null) {
                Properties props = new Properties();
                props.load(fileStr);
                SystemProperties.initializeProperties(props);
                return true;
            } else {
                throw new ServletException("Unable to open: " + configFile);
            }
        } catch (FileNotFoundException fexp) {
            fexp.printStackTrace();
            throw new ServletException(fexp.getMessage());
        } catch (IOException ioexp) {
            ioexp.printStackTrace();
            throw new ServletException(ioexp.getMessage());
        } finally {
            if (fileStr != null) {
                try {
                    fileStr.close();
                } catch (IOException ioe) {
                }
            }
        }
    }
}
