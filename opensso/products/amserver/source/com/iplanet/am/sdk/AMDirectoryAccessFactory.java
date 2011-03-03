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
 * $Id: AMDirectoryAccessFactory.java,v 1.6 2008/08/27 22:05:40 veiming Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.sdk;

import com.iplanet.am.sdk.common.IComplianceServices;
import com.iplanet.am.sdk.common.IDCTreeServices;
import com.iplanet.am.sdk.common.IDirectoryServices;
import com.iplanet.am.sdk.common.IDirectoryServicesProvider;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceManager;
import java.security.AccessController;
import java.security.ProviderException;

/**
 *  A Factory which provides access to the Directory Services. This Class
 *  provides API's to return the appropriate implementation classes for 
 *  configured packages (viz., ldap or remote).
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */
public class AMDirectoryAccessFactory {

    private static IDirectoryServices dsServicesImpl;

    private static IDCTreeServices dcTreeServicesImpl;

    private static IComplianceServices complianceServicesImpl;

    private static Debug debug = AMCommonUtils.debug;

    private static boolean isInitialized = false;

    private final static String CONFIGURED_SDK_PACKAGE_PROPERTY = 
        "com.iplanet.am.sdk.package";

    private final static String LDAP_SDK_PACKAGE = "com.iplanet.am.sdk.ldap";

    private final static String REMOTE_SDK_PACKAGE = 
        "com.iplanet.am.sdk.remote";

    private final static String DS_SERVICES_PROVIDER_CLASS = 
        "DirectoryServicesProviderImpl";

    private final static String REMOTE_SERVICES_PROVIDER_CLASS = 
        "RemoteServicesProviderImpl";

    private final static String PACKAGE_SEPARATOR = ".";

    private static void initialize() {
        // Check if AMSDK is enabled & configured
        if (!ServiceManager.isAMSDKEnabled()) {
           debug.error("AMDirectoryAccessFactory.initialize() " +
                "AM.SDK not configured");
            throw (new RuntimeException("AMSDK NOT configured"));
        }
        
        String configuredSDK = SystemProperties
                .get(CONFIGURED_SDK_PACKAGE_PROPERTY);

        boolean isCriticalErrorIfClassNotFound = true;
        if ((configuredSDK == null) || (configuredSDK.equals(LDAP_SDK_PACKAGE)))
        {
            // Use the LDAP SDK Package if nothing has been configured or if
            // LDAP Package is configured
            try {
                isCriticalErrorIfClassNotFound = false;
                instantiateImpls(LDAP_SDK_PACKAGE, DS_SERVICES_PROVIDER_CLASS,
                        isCriticalErrorIfClassNotFound);
            } catch (ProviderException pe) {
                // Probably remote mode without the property being configured.
                // So try initializing the REMOTE packages. Use try Remote SDK
                // Package
                if (debug.messageEnabled()) {
                    debug.message("AMDirectoryAccessFactory.static{} - "
                            + "Initializing the server packages failed. Hence "
                            + "trying the remote client sdk pacakage");
                }
                isCriticalErrorIfClassNotFound = true;
                instantiateImpls(REMOTE_SDK_PACKAGE,
                        REMOTE_SERVICES_PROVIDER_CLASS,
                        isCriticalErrorIfClassNotFound);
            }
        } else if (configuredSDK.equals(REMOTE_SDK_PACKAGE)) {
            // Use the Remote SDK Package
            instantiateImpls(REMOTE_SDK_PACKAGE,
                    REMOTE_SERVICES_PROVIDER_CLASS,
                    isCriticalErrorIfClassNotFound);
        } else { // Some mis-configuration. The package name was not specified
            // correctly. So try initializing the default LDAP package
            instantiateImpls(LDAP_SDK_PACKAGE, DS_SERVICES_PROVIDER_CLASS,
                    isCriticalErrorIfClassNotFound);
        }
        isInitialized = true;
    }

    private static void instantiateImpls(String packageName, String className,
            boolean isCriticalErrorIfClassNotFound) {

        String providerClass = packageName + PACKAGE_SEPARATOR + className;

        try {

            IDirectoryServicesProvider dsServicesProvider = 
                (IDirectoryServicesProvider) Class.forName(
                        providerClass).newInstance();
            dsServicesImpl = dsServicesProvider.getDirectoryServicesImpl();
            dcTreeServicesImpl = dsServicesProvider.getDCTreeServicesImpl();
            complianceServicesImpl = dsServicesProvider
                    .getComplianceServicesImpl();
            // Add the listener
            initListener();

        } catch (InstantiationException e) {
            debug.error("AMDirectoryAccessFactory.instantiateImpls()- "
                    + "Initializing Impls from package: " + packageName
                    + " FAILED!", e);
            throw new ProviderException(AMSDKBundle.getString("300"));
        } catch (IllegalAccessException e) {
            debug.error("AMDirectoryAccessFactory.instantiateImpls()- "
                    + "Initializing Impls from package: " + packageName
                    + " FAILED!", e);
            throw new ProviderException(AMSDKBundle.getString("300"));
        } catch (ClassNotFoundException e) {
            String message = "AMDirectoryAccessFactory."
                    + "instantiateImpls()- Initializing Impls from "
                    + "package: " + packageName + " FAILED!";
            if (isCriticalErrorIfClassNotFound) {
                debug.error(message, e);
            } else {
                debug.warning(message, e);
            }
            throw new ProviderException(AMSDKBundle.getString("300"));
        } catch (AMEventManagerException ame) {
            debug.error("AMDirectoryAccessFactory.instantiateImpls()- "
                    + "Initializing Impls from package: " + packageName
                    + "FAILED!", ame);
            throw new ProviderException(AMSDKBundle.getString("300"));
        }

        if (debug.messageEnabled()) {
            debug.message("AMDirectoryAccessFactory.instantiateImpls() - "
                    + "Successfully initialized Impls Using Impl Package: "
                    + packageName + " for accessing Directory Services");
        }
    }

    private static void initListener() throws AMEventManagerException {
        SSOToken token = (SSOToken) AccessController
                .doPrivileged(AdminTokenAction.getInstance());
        AMObjectListener amListener = new AMObjectListenerImpl();
        if (debug.messageEnabled()) {
            debug.message("AMDirectoryAccessFactory.initListener() - "
                    + "adding listener: " + amListener.getClass().getName());
        }

        dsServicesImpl.addListener(token, amListener, null);
    }

    private static boolean isInitialized() {
        return isInitialized;
    }

    public static IDirectoryServices getDirectoryServices() {
        if (!isInitialized()) {
            initialize();
        }
        return dsServicesImpl;
    }

    public static IDCTreeServices getDCTreeServices() {
        if (!isInitialized()) {
            initialize();
        }

        return dcTreeServicesImpl;
    }

    public static IComplianceServices getComplianceServices() {
        if (!isInitialized()) {
            initialize();
        }

        return complianceServicesImpl;
    }
}
