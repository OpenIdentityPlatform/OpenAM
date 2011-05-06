/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IdServicesFactory.java,v 1.4 2008/06/27 20:56:23 arviranga Exp $
 *
 */

package com.sun.identity.idm;

import java.security.ProviderException;

import com.iplanet.am.sdk.AMSDKBundle;
import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.SystemProperties;

/**
 * A Factory which provides access to the Directory Services. This Class
 * provides API's to return the appropriate implementation classes for
 * configured packages (viz., ldap or remote).
 */
public class IdServicesFactory {

    private static IdServices idServices;

    private static Debug debug = Debug.getInstance("amIdm");

    private static boolean isInitialized = false;

    private final static String CONFIGURED_SDK_PACKAGE_PROPERTY = 
        "com.iplanet.am.sdk.package";

    private final static String SERVER_PACKAGE = 
        "com.iplanet.am.sdk.ldap";

    private final static String REMOTE_PACKAGE = 
        "com.iplanet.am.sdk.remote";

    private final static String SERVER_IDM_PACKAGE = 
        "com.sun.identity.idm.server";

    private final static String REMOTE_IDM_PACKAGE = 
        "com.sun.identity.idm.remote";

    private final static String ID_SERVICES_PROVIDER_CLASS = 
        "IdServicesProviderImpl";

    private final static String REMOTE_SERVICES_PROVIDER_CLASS = 
        "IdRemoteServicesProviderImpl";

    private final static String PACKAGE_SEPARATOR = ".";

    private static void initialize() {
        String configuredSDK = SystemProperties.get(
                CONFIGURED_SDK_PACKAGE_PROPERTY);

        boolean isCriticalErrorIfClassNotFound = true;
        if ((configuredSDK == null) || (configuredSDK.equals(SERVER_PACKAGE))) {
            // Use the IdRepo server package if nothing has been configured or
            // if Package is configured
            try {
                isCriticalErrorIfClassNotFound = false;
                instantiateImpls(SERVER_IDM_PACKAGE,
                        ID_SERVICES_PROVIDER_CLASS,
                        isCriticalErrorIfClassNotFound);
            } catch (ProviderException pe) {
                // Probably remote mode without the property being configured.
                // So try initializing the REMOTE packages. Use try Remote SDK
                // Package
                if (debug.messageEnabled()) {
                    debug.message("IdServicesFactory.static{} - Initializing "
                            + "the server packages failed. Hence trying the "
                            + "remote client sdk pacakage");
                }
                isCriticalErrorIfClassNotFound = true;
                instantiateImpls(REMOTE_IDM_PACKAGE,
                        REMOTE_SERVICES_PROVIDER_CLASS,
                        isCriticalErrorIfClassNotFound);
            }

        } else if (configuredSDK.equals(REMOTE_PACKAGE)) {
            // Use the Remote idRepo Package
            instantiateImpls(REMOTE_IDM_PACKAGE,
                    REMOTE_SERVICES_PROVIDER_CLASS,
                    isCriticalErrorIfClassNotFound);
        } else { // Mostly a mis-configuration. Fall back to default.
            // The configured package name does not match
            instantiateImpls(SERVER_IDM_PACKAGE, ID_SERVICES_PROVIDER_CLASS,
                    isCriticalErrorIfClassNotFound);
        }

        isInitialized = true;
    }

    private static void instantiateImpls(String packageName, String className,
            boolean isCriticalErrorIfClassNotFound) {

        String providerClass = packageName + PACKAGE_SEPARATOR + className;

        try {

            IdServicesProvider idServicesProvider = (IdServicesProvider) Class
                    .forName(providerClass).newInstance();
            idServices = idServicesProvider.getProvider();

        } catch (InstantiationException e) {
            debug.error("IdServicesFactory.instantiateImpls()- "
                    + "Initializing Impls from package: " + packageName
                    + " FAILED!", e);
            throw new ProviderException(AMSDKBundle.getString("300"));
        } catch (IllegalAccessException e) {
            debug.error("IdServicesFactory.instantiateImpls()- "
                    + "Initializing Impls from package: " + packageName
                    + " FAILED!", e);
            throw new ProviderException(AMSDKBundle.getString("300"));
        } catch (ClassNotFoundException e) {
            String message = "IdServicesFactory.instantiateImpls()- "
                    + "Initializing Impls from package: " + packageName
                    + " FAILED!";
            if (isCriticalErrorIfClassNotFound) {
                debug.error(message, e);
            } else {
                debug.warning(message, e);
            }
            throw new ProviderException(AMSDKBundle.getString("300"));
        }

        if (debug.messageEnabled()) {
            debug.message("IdServicesFactory.instantiateImpls() - "
                    + "Successfully initialized Impls Using Impl Package: "
                    + packageName + " for accessing Directory Services");
        }
    }

    private static boolean isInitialized() {
        return isInitialized;
    }

    public static IdServices getDataStoreServices() {
        if (!isInitialized()) {
            initialize();
        }
        return idServices;
    }

}
