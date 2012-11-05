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
package com.iplanet.dpro.session.service;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.coretoken.interfaces.AMSessionRepository;
import com.sun.identity.sm.ldap.CTSPersistentStore;
import com.sun.identity.sm.mq.JMQSessionRepository;

/**
 * <code>SessionRepositoryFactory</code> provides a default
 * factory for obtaining our Core Token services BackEnd Repository.
 */

class SessionRepositoryFactory {

    /**
     * Global Definitions.
     */
    private static final String DEFAULT_CTS_REPOSITORY_CLASS_NAME =
            CTSPersistentStore.class.getName();

    private static final String CTS_REPOSITORY_CLASS_NAME = SystemProperties.get(
            AMSessionRepository.CTS_REPOSITORY_CLASS_PROPERTY, DEFAULT_CTS_REPOSITORY_CLASS_NAME);

    /**
     * Singleton instance of AM Session Repository or CTS.
     */
    private static AMSessionRepository sessionRepository = null;

    /**
     * Private, do not allow instantiation.
     */
    private SessionRepositoryFactory() {
    }

    /**
     * Common Get Instance method to obtain access to
     * Service Methods.
     *
     * @return AMSessionRepository Singleton Instance.
     * @throws Exception
     */
    protected static AMSessionRepository getInstance()
            throws Exception {
        if (sessionRepository == null) {
            if (CTS_REPOSITORY_CLASS_NAME.equals(CTSPersistentStore.class.getName())) {
                sessionRepository = CTSPersistentStore.getInstance();
            } else if (CTS_REPOSITORY_CLASS_NAME.equals(JMQSessionRepository.class.getName())) {
                sessionRepository = JMQSessionRepository.getInstance();
            } else {
                throw new IllegalAccessException("Unable to instantiate the CTS Persistent Store as Implementation Class:["+
                        CTS_REPOSITORY_CLASS_NAME+"], is unknown to OpenAM!");
            }
        }
        return sessionRepository;
    }

}
