/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted 2012 ForgeRock Inc"
 */
package com.sun.identity.saml2.common;

import org.forgerock.openam.shared.service.OpenAMService;

/**
 * Provides the CTS Persistent Store Injector to allow access to a package protected
 * class to prevent inadvertent instantiation.
 *
 * @author jeff.schenk@forgerock.com
 */
public class CTSPersistentSAML2StoreInjector implements OpenAMService {

    /**
     * The Instance we will Inject.
     */
    private static final CTSPersistentSAML2Store ctsPersistentSAML2Store = new CTSPersistentSAML2Store();

    /**
     * Default Constructor
     */
    public CTSPersistentSAML2StoreInjector() {
    }

    /**
    * Obtain the associated Instance for this Injector.
    *
    * @return CTSPersistentStore
    */
    @Override public OpenAMService getInstance() {
        return (OpenAMService) ctsPersistentSAML2Store.getInstance();
    }

    /**
     * Provide the Instance ClassName we handle for this Injector.
     * @return String of Instance ClassName.
     */
    @Override public String getInstanceClassName() {
        return CTSPersistentSAML2Store.class.getName();
    }

}
