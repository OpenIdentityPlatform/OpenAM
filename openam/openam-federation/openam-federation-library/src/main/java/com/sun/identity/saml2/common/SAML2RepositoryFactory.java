/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: JMQSAML2Repository.java,v 1.3 2008/08/01 22:15:00 hengming Exp $
 *
 * Portions Copyrighted 2012-2013 ForgeRock AS
 *
 */


package com.sun.identity.saml2.common;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.coretoken.interfaces.AMTokenSAML2Repository;

/**
 * <code>SAML2RepositoryFactory</code> represents the saml2 repository,
 * default repository is <code>SAML2CTSPersistentStore</code>.
 */
public class SAML2RepositoryFactory {

    private static final String DEFAULT_REPOSITORY_CLASS =
            "com.sun.identity.saml2.common.SAML2CTSPersistentStore";

    private static final String REPOSITORY_CLASS_PROPERTY =
            "com.sun.identity.saml2.plugins.SAML2RepositoryImpl";

    private static final String CTS_SAML2_REPOSITORY_CLASS_NAME =
            SystemConfigurationUtil.getProperty(
                    REPOSITORY_CLASS_PROPERTY, DEFAULT_REPOSITORY_CLASS);

    /**
     * Cached Instance Reference to our SAML2 Token Repository
     */
    private static volatile AMTokenSAML2Repository saml2Repository = null;

    /**
     * Prevent Instantiation and only use as a functional static class.
     */
    private SAML2RepositoryFactory() {
    }

    /**
     * @return the instance of AMTokenSAML2Repository
     * @throws SAML2Exception when failed to instantiate AMTokenSAML2Repository
     */
    public static AMTokenSAML2Repository getInstance()
            throws SAML2Exception {
        if (saml2Repository == null) {
            if (CTS_SAML2_REPOSITORY_CLASS_NAME.equals(DEFAULT_REPOSITORY_CLASS)) {
                saml2Repository = SAML2CTSPersistentStore.getInstance();
            } else {
                // Here we have an the default or customer / client implementation, for SAML2 Persistence.
                // So simply Instantiate the specified Implementation.
                try {
                    saml2Repository = (AMTokenSAML2Repository) Class.forName(
                            CTS_SAML2_REPOSITORY_CLASS_NAME).newInstance();
                } catch (Exception e) {
                    SAML2Utils.debug.error("Exception occurred attempting to instantiate " +
                            "AMTokenSAML2Repository Implementation!", e);
                    saml2Repository = null;
                }
            }
            // Determine if we were able to obtain the Instance for our AMTokenSAML2Repository.
            if (saml2Repository == null)
            {
                SAML2Utils.debug.error("Failed to instantiate " +
                        "AMTokenSAML2Repository Implementation using "+CTS_SAML2_REPOSITORY_CLASS_NAME+", very bad!");
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("nullSAML2Repository"));
            }
        } // End of Check for null saml2Repository.
        // Return Instance.
        return saml2Repository;
    }
}
