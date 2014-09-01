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
 */
package org.forgerock.openam.agents.install.appserver;

import com.sun.identity.agents.install.appserver.IConfigKeys;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.OSChecker;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A simple class which is able to find out the GlassFish version for the given
 * server.
 *
 * @author Peter Major
 */
public class VersionChecker {

    private static String versionString = null;

    /**
     * Checks the version of the GlassFish server
     *
     * @param stateAccess for agent configuration
     * @return returns <code>true</code> if the GlassFish Server version is v3
     * or greater, and returns <code>false</code> if GlassFish Server version is
     * v2. If the version can't be identified, then the server is v2 by default.
     */
    public static boolean isGlassFishv3(IStateAccess stateAccess) {
        if (versionString == null) {
            initVersionString(stateAccess);
            if (versionString == null) {
                Debug.log("ERROR: Unable to determine the version string based on "
                        + "asadmin version, falling back to GlassFishv2 mode");
                //We couldn't find out the GF version, fallback to GFv2 and prior
                return false;
            }
        }

        //if GF version is v3 or 3.0.1 or greater (the . is needed since
        //the build number could start with 3, like (build 33))
        //Examples:
        //Version = GlassFish Server Open Source Edition 3.0.1 (build 22)
        //Version = Sun GlassFish Enterprise Server v2.1.1
        if (versionString.matches(".*(v3\\.| 3\\.).*")) {
            Debug.log("GlassFish v3 server");
            return true;
        } else {
            Debug.log("GlassFish v2 or prior server");
            return false;
        }
    }

    /**
     * Initializes the versionstring value. Basically it runs an
     * 'asadmin version' command and stores the versionstring.
     *
     * @param stateAccess for accessing agent configuration
     */
    private static void initVersionString(IStateAccess stateAccess) {
        String configDir = (String) stateAccess.get(IConfigKeys.STR_KEY_AS_INST_CONFIG_DIR);
        String command;
        InputStream is = null;

        try {
            if (OSChecker.isWindows()) {
                command = "\"" + configDir + "\\..\\..\\..\\bin\\asadmin.bat\" version";
            } else {
                command = configDir + "/../../../bin/asadmin version";
            }

            String line = null;
            Debug.log("Executing process: " + command);
            Process p = Runtime.getRuntime().exec(command);
            is = p.getInputStream();
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

            while ((line = input.readLine()) != null) {
                Debug.log("INFO ------> " + line);
                if (line.startsWith("Version = ")) {
                    Debug.log("Identified Glassfish server version: " + line);
                    versionString = line;
                    break;
                }
            }
        } catch (IOException ioe) {
            Debug.log("Version check: Error - Unable to identify Glassfish server version", ioe);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                }
            }
        }
    }
}
