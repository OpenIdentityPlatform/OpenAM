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
 * $Id: IdentityServicesFactory.java,v 1.2 2008/06/25 05:43:33 qcheng Exp $
 *
 */

package com.sun.identity.idsvcs;

import java.util.Map;
import java.util.HashMap;

/**
 * Factory object to maintain multiple implementations of
 * Identity Services.
 */
public class IdentityServicesFactory {

    //=======================================================================
    // Constants
    //=======================================================================
    final private static String DEFAULT =
        "com.sun.identity.idsvcs.opensso.IdentityServicesImpl";
    //=======================================================================
    // Fields
    //=======================================================================
    final private Class provider;
    private static IdentityServicesImpl providerImpl;
    private static Map cache = new HashMap();

    //=======================================================================
    // Constructors
    //=======================================================================
    /**
     * Setup the provider that w/ create new instances.
     */
    private IdentityServicesFactory(Class clazz) {
        provider = clazz;
    }

    /**
     * The default security implementation is 'OpenSSO'.
     */
    public static IdentityServicesFactory getInstance() {
        return getInstance(DEFAULT);
    }

    /**
     * Return an instance of a factory that provider.
     */
    public static IdentityServicesFactory getInstance(String provider) {
        final Class TEST = IdentityServicesImpl.class;
        IdentityServicesFactory ret = (IdentityServicesFactory)
            cache.get(provider);
        // attempt to determine if the provider is okay..
        if (ret == null) {
            try {
                boolean found = false;
                Class clazz = Class.forName(provider);
                Class[] infs = clazz.getInterfaces();
                for (int i = 0; i < infs.length; i++) {
                    Class tst = infs[i];
                    if (tst.equals(TEST)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new IllegalArgumentException(
                            "Does not implement IdentityServices!");
                }
                ret = new IdentityServicesFactory(clazz);
                cache.put(provider, ret);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return ret;
    }

    /**
     * Create a new instance of a Security implementation using the default
     * provider.
     * 
     * @return Security
     */
    public IdentityServicesImpl newInstance() {
        if (providerImpl == null) {
            try {
                providerImpl = (IdentityServicesImpl) provider.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return (providerImpl);
    }
}
