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

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.coretoken.interfaces.AMTokenSAML2Repository;

/**
 *
 * <code>SAML2RepositoryFactory</code> represents the saml2 repository,
 * default repository is <code>CTSPersistentSAML2Store</code>.
 *
 */
public class SAML2RepositoryFactory {

    private static final String DEFAULT_REPOSITORY_CLASS =
        "com.sun.identity.saml2.common.CTSPersistentSAML2Store";
        
    private static final String REPOSITORY_CLASS_PROPERTY =
        "com.sun.identity.saml2.plugins.SAML2RepositoryImpl";

    private static final String REPOSITORY_CLASS = 
        SystemConfigurationUtil.getProperty(
        REPOSITORY_CLASS_PROPERTY, DEFAULT_REPOSITORY_CLASS);

    // TODO -- Pull in 10.1.0.
    @Deprecated
    private static final String DEPRECATED_JMQ_REPOSITORY_CLASS =
            "com.sun.identity.saml2.plugins.DefaultJMQSAML2Repository";

    /**
     * Cached Instance Reference to our SAML2 Token Repository
     */
    private static volatile AMTokenSAML2Repository saml2Repository = null;

    static {
        try {
            saml2Repository = (AMTokenSAML2Repository) Class.forName(
                REPOSITORY_CLASS).newInstance();
        } catch (Exception e) {
            SAML2Utils.debug.error("Failed to instantiate " +
                "AMTokenSAML2Repository", e);
            saml2Repository = null; 
        }
    } 
    /**
     * @return the instance of AMTokenSAML2Repository
     * @throws SAML2Exception when failed to instantiate AMTokenSAML2Repository
     */
    public static AMTokenSAML2Repository getInstance()
        throws SAML2Exception {
        if (saml2Repository == null) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("nullSAML2Repository"));
        }
        return saml2Repository;
    }
}
