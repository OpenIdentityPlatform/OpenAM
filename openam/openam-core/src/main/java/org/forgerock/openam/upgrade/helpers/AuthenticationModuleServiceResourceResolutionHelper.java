/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openam.upgrade.helpers;

import com.sun.identity.shared.debug.Debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 *    Authentication Module Service Resource Resolution Helper
 *
 *    Provides Lookup helper for obtaining correct path for
 *    various resource files located within the classpath in a JAR or
 *    resource directory.
 */
public class AuthenticationModuleServiceResourceResolutionHelper {
    protected static Debug debug = Debug.getInstance("Configuration");

    private static Map<String, String> resourceNeighborClassNames = new HashMap<String, String>();

    private AuthenticationModuleServiceResourceResolutionHelper() {}

    /**
     *  Get Resource Neighbor Class Name
     * @return Map<string,String> of all Resource Neighbor Class Names
     */
    public static Map<String, String> getResourceNeighborClassNames() {
        return resourceNeighborClassNames;
    }

    /**
     * Load up the Defined Maps for Knowledge obtaining the Resource.
     */
    static {

        /** amAuthAD.xml **/
        resourceNeighborClassNames.put("amAuthAD.xml","com.sun.identity.authentication.modules.ad.AD");

        /** amAuthAdaptive.xml **/
        resourceNeighborClassNames.put("amAuthAdaptive.xml","org.forgerock.openam.authentication.modules.adaptive.Adaptive");

        /** amAuthAnonymous.xml **/
        resourceNeighborClassNames.put("amAuthAnonymous.xml","com.sun.identity.authentication.modules.anonymous.Anonymous");

        /** amAuthCert.xml **/
        resourceNeighborClassNames.put("amAuthCert.xml","com.sun.identity.authentication.modules.cert.Cert");

        /** amAuthDataStore.xml **/
        resourceNeighborClassNames.put("amAuthDataStore.xml","com.sun.identity.authentication.modules.datastore.DataStore");

        /** amAuthDevicePrint.xml **/
        resourceNeighborClassNames.put("amAuthDevicePrintModule.xml","org.forgerock.openam.authentication.modules.deviceprint.DevicePrintModule");

        /** amAuthHOTP.xml **/
        resourceNeighborClassNames.put("amAuthHOTP.xml","com.sun.identity.authentication.modules.hotp.HOTP");

        /** amAuthHTTPBasic.xml **/
        resourceNeighborClassNames.put("amAuthHTTPBasic.xml","com.sun.identity.authentication.modules.httpbasic.HTTPBasic");

        /** amAuthJDBC.xml **/
        resourceNeighborClassNames.put("amAuthJDBC.xml","com.sun.identity.authentication.modules.jdbc.JDBC");

        /** amAuthLDAP.xml **/
        resourceNeighborClassNames.put("amAuthLDAP.xml","com.sun.identity.authentication.modules.ldap.LDAP");

        /** amAuthMembership.xml **/
        resourceNeighborClassNames.put("amAuthMembership.xml","com.sun.identity.authentication.modules.membership.Membership");

        /** amAuthMSISDN.xml **/
        resourceNeighborClassNames.put("amAuthMSISDN.xml","com.sun.identity.authentication.modules.msisdn.MSISDN");

        /** amAuthNT.xml **/
        resourceNeighborClassNames.put("amAuthNT.xml","com.sun.identity.authentication.modules.nt.NT");

        /** amAuthOATH.xml **/
        resourceNeighborClassNames.put("amAuthOATH.xml","org.forgerock.openam.authentication.modules.oath.OATH");

        /** amAuthOAuth.xml **/
        resourceNeighborClassNames.put("amAuthOAuth.xml","org.forgerock.openam.authentication.modules.oauth2.OAuth");

        /** amAuthRadius.xml **/
        resourceNeighborClassNames.put("amAuthRadius.xml","com.sun.identity.authentication.modules.radius.RADIUS");

        /** amAuthSecurID.xml **/
        resourceNeighborClassNames.put("amAuthSecurID.xml","com.sun.identity.authentication.modules.securid.SecurID");

        /** amAuthWindowsDesktopSSO.xml **/
        resourceNeighborClassNames.put("amAuthWindowsDesktopSSO.xml","com.sun.identity.authentication.modules.windowsdesktopsso.WindowsDesktopSSO");

    };


    /**
     * Obtain the Neighbor Class for the specified Resource.
     * @param resourceName
     * @return String of Found Neighbor Class.
     */
    public static String getNeighborClassName(String resourceName) {
          if ( (resourceName == null) || (resourceName.length() <= 0) )
            { return null; }
          return resourceNeighborClassNames.get(resourceName);
    }

    /**
     * Get Resource Content
     *
     * @param aClass -- Calling Instantiated Class to obtain Resource.
     * @param resName -- Resource Name to be Obtained.
     * @return String Contents of Resource
     * @throws IOException
     */
    public static String getResourceContent(Class<?> aClass, String resName)
            throws IOException {
        if ( (resName == null) || (resName.length() <= 0) || (aClass == null))
            { return null; }
        BufferedReader rawReader = null;
        String content = null;
        URL resourceURL = null;

        try {
            resourceURL = getResourceURL(aClass, resName);
            if (resourceURL == null)
            {
                debug.error("Unable to obtain Resource Content: "+resName+", Resource Not Available!");
                return null;
            }

            rawReader = new BufferedReader(new InputStreamReader(resourceURL.openStream()));
            StringBuilder buff = new StringBuilder();
            String line = null;

            while ((line = rawReader.readLine()) != null) {
                buff.append(line).append("\n");
            }

            rawReader.close();
            rawReader = null;
            content = buff.toString();

        } catch (RuntimeException rte) {
            debug.error("Resource URL: " +
                    ((resourceURL==null)?"is null":resourceURL.toString()+" is not accessible") + ", Exception Encountered.", rte);
        } finally {
            if (rawReader != null) {
                rawReader.close();
            }
        }

        return content;
    }

    /**
     * Attempt to resolve the Resource Names URL for pulling Content.
     * @param resName Resource Name
     * @return URL of Resource or null if Not Found.
     */
    private static URL getResourceURL(Class<?> aClass, String resName) {
        if ( (resName == null) || (resName.length() <= 0) || (aClass == null))
            { return null; }


        URL resourceURL =
                    aClass.getClassLoader().getResource(resName);
            debug.message("ResourceURL " + ((resourceURL == null) ? "is null and" : resourceURL.toExternalForm()) + " was " +
                    ((resourceURL == null) ? "Not Found" : "Found") + " using " + resName + ".");
        if (resourceURL == null)
        {
            resourceURL =
                    aClass.getClassLoader().getResource("/"+resName);
            debug.message("ResourceURL " + ((resourceURL == null) ? "is null and" : resourceURL.toExternalForm()) + " was "
                    + ((resourceURL == null) ? "Not Found" : "Found") + " using /" + resName + ".");
        }
        // *****************************************
        // After two Attempts we still have not
        // found our Resource, attempt to use our
        // helper class to obtain the resource.
        if (resourceURL == null)
        {
            Class<?> neighborClass = null;
            String neighborClassName = AuthenticationModuleServiceResourceResolutionHelper.getNeighborClassName(resName);
            if (neighborClassName != null)
            {  neighborClass = getNeighborClassForResource(aClass, neighborClassName);  }
            if (neighborClass != null)
            {
                resourceURL =
                        neighborClass.getClassLoader().getResource(neighborClass.getPackage().getName().replace(".","/")+"/"+resName);
            }
            debug.message("ResourceURL "+((resourceURL==null)?"is null and":resourceURL.toExternalForm())+" was "
                    +((resourceURL==null)?"Not Found":"Found")+" using /"+resName+" with a neighbor class: "+neighborClassName+".");
        }
        return resourceURL;
    }

    /**
     * Helper Method of Obtain the specified Class.
     * @param className
     * @return Class<?> of Found Class Instance
     */
    private static Class<?> getNeighborClassForResource(Class<?> aClass, String className) {
        Class<?> clazz = null;
        try {
            clazz =
                    aClass.getClassLoader().loadClass(className);
        } catch(ClassNotFoundException cne) {
            debug.error("Unable to obtain Class: " + className);
        }
        return clazz;

    }



}
