/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock US Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information:
 *
 * "Portions copyright [year] [name of copyright owner]".
 *
 */
package com.iplanet.dpro.session.service;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.sm.ldap.CTSPersistentStore;
import com.sun.identity.sm.ldap.api.CoreTokenConstants;

/**
 * <code>CoreTokenServiceFactory</code> provides a default
 * factory for obtaining our Core Token services BackEnd Repository.
 *
 * ** This is a Package Protected class **
 *
 * ** This class is sort of duplicated in:
 * com.sun.identity.saml2.common.AMTokenRepositoryFactoryAccessor
 *
 * @author robert.wapshott@forgerock.com
 * @author jeff.schenk@forgerock.com
 */

public class CoreTokenServiceFactory {

    /**
     * Global Definitions.
     */
    private static final String DEFAULT_CTS_REPOSITORY_CLASS_NAME =
            CTSPersistentStore.class.getName();

    private static final String CTS_REPOSITORY_CLASS_NAME = SystemProperties.get(
            CoreTokenConstants.CTS_REPOSITORY_CLASS_PROPERTY, DEFAULT_CTS_REPOSITORY_CLASS_NAME);

    /**
     * Singleton instance of AM Session Repository aka CTS.
     */
    private static volatile CTSPersistentStore coreTokenService = null;

    /**
     * Prevent Instantiation and only use as a functional static class.
     */
    private CoreTokenServiceFactory() {
    }

    /**
     * Common Get Instance method to obtain access to
     * Service Methods.
     *
     * @return CoreTokenService Singleton Instance.
     * @throws Exception
     */
    public synchronized static CTSPersistentStore getInstance() {
        if (coreTokenService == null) {
            if (CTS_REPOSITORY_CLASS_NAME.equals(CTSPersistentStore.class.getName())) {
                coreTokenService = CTSPersistentStore.getInstance();
            } else {
                throw new RuntimeException("Unable to instantiate the CTS Persistent Store as Implementation Class:["+
                        CTS_REPOSITORY_CLASS_NAME+"], is unknown to OpenAM!");
            }
        }
        // Check if we have gone null during initialization due to offending processing exceptions during instantiation phase.
        if (coreTokenService == null) {
            throw new IllegalAccessError("Unable to instantiate the CTS Persistent Store as Implementation Class:["+
                    CTS_REPOSITORY_CLASS_NAME+"], failed during Initialization.");
        }
        // return the implementation.
        return coreTokenService;
    }

}
