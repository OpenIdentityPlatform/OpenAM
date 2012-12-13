/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ProviderUtil.java,v 1.4 2008/08/06 17:28:12 exu Exp $
 *
 */

package com.sun.identity.liberty.ws.util;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;

/**
 * The class <code>ProviderUtil</code> manages <code>ProviderManager</code>.
 */
public class ProviderUtil {
    private static ProviderManager providerManager;
    private static final String PROVIDER_MANAGER_CLASS_PROP =
        "com.sun.identity.liberty.ws.util.providerManagerClass";
    static Debug debug = Debug.getInstance("libIDWSF");

    static {
        try {
            String providerManagerClass = SystemPropertiesManager.get(
                PROVIDER_MANAGER_CLASS_PROP,
                "com.sun.identity.liberty.ws.util.IDFFProviderManager");
            providerManager = (ProviderManager)Class.forName(
                providerManagerClass, true,
                Thread.currentThread().getContextClassLoader()).newInstance();
        } catch (Exception ex) {
            debug.error("ProviderUtil.static:", ex);
        }
    }

    /**
     * Gets <code>ProviderManager</code> implementation. The implementation
     * class name is specified with property 
     * "com.sun.identity.liberty.ws.uti.providerManagerClass"
     * in FederationConfig.
     * @return <code>ProviderManager</code.
     */
    public static ProviderManager getProviderManager() {
        return providerManager;
    }

}

