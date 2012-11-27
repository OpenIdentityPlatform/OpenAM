/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock US Inc. All Rights Reserved
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
package com.sun.identity.saml2.common;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.coretoken.interfaces.AMTokenRepository;
import com.sun.identity.coretoken.interfaces.AMTokenSAML2Repository;
import com.sun.identity.sm.ldap.CTSPersistentStore;
import com.sun.identity.sm.mq.JMQSessionRepository;

/**
 * This class is sort of duplicated to keep package level
 * access restrictions still in play.
 *
 * So we can not access the AMTokenRepositoryFactory directory since
 * it is a package level protected class.
 *
 * We duplicate the necessary factory pattern here to allow
 * our CTSPersistentSAML2Store class to access the BackEnd
 * implementation of the CTS Repository.
 *
 * And we only have Access to the Implementation for the
 * AMTokenSAML2Repository Interface within the CTSPersistentStore class.
 *
 * @author jeff.schenk@forgerock.com
 */
class CTSPersistentSAML2StoreFactory {

    /**
     * Global Definitions.
     */
    private static final String DEFAULT_CTS_REPOSITORY_CLASS_NAME =
            CTSPersistentStore.class.getName();

    private static final String CTS_REPOSITORY_CLASS_NAME = SystemProperties.get(
            AMTokenRepository.CTS_REPOSITORY_CLASS_PROPERTY, DEFAULT_CTS_REPOSITORY_CLASS_NAME);

    /**
     * Singleton instance of AM Session Repository or CTS.
     */
    private static volatile AMTokenSAML2Repository amTokenSAML2Repository = null;

    /**
     * Singleton, do not allow Instantiation.
     */
    private CTSPersistentSAML2StoreFactory() {
    }

    /**
     * Common Get Instance method to obtain access to
     * Service Methods.
     *
     * @return AMTokenSAML2Repository Singleton Instance.
     * @throws Exception
     */
    protected static AMTokenSAML2Repository getInstance()
            throws Exception {
        if (amTokenSAML2Repository == null) {
            if (CTS_REPOSITORY_CLASS_NAME.equals(CTSPersistentStore.class.getName())) {
                amTokenSAML2Repository = (AMTokenSAML2Repository) CTSPersistentStore.getInstance();
            } else if (CTS_REPOSITORY_CLASS_NAME.equals(JMQSessionRepository.class.getName())) {
                amTokenSAML2Repository =  JMQSessionRepository.getInstance();
            } else {
                throw new IllegalAccessException("Unable to instantiate the SAML2 CTS Persistent Store as Implementation Class:["+
                        CTS_REPOSITORY_CLASS_NAME+"], is unknown to OpenAM!");
            }
        }
        return amTokenSAML2Repository;
    }

}

