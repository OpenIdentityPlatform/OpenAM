/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * If applicable, addReferral the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: CoreTokenStoreFactory.java,v 1.1 2009/11/19 00:07:40 qcheng Exp $
 */
package com.sun.identity.coretoken;

import com.sun.identity.coretoken.service.CoreTokenConfigService;
import com.sun.identity.coretoken.spi.CoreTokenStore;

/**
 * This <code>OpenSSOCoreTokenStore</code> implements the core token store
 * using SM store.
 */

@SuppressWarnings("static-access")
public class CoreTokenStoreFactory {
    private static CoreTokenStore instance = null;

    static {
            if (CoreTokenConfigService.implClassName != null) {
                try {
                    instance = (CoreTokenStore) Thread.currentThread()
                        .getContextClassLoader().getClass()
                        .forName(CoreTokenConfigService.implClassName)
                        .newInstance();
                } catch (ClassNotFoundException ex) {
                    CoreTokenUtils.debug.error("CoreTokenStoreFactory.ini", ex);
                } catch (InstantiationException ex) {
                    CoreTokenUtils.debug.error("CoreTokenStoreFactory.ini", ex);
                } catch (IllegalAccessException ex) {
                    CoreTokenUtils.debug.error("CoreTokenStoreFactory.ini", ex);
                }
            }
    }

    /**
     * Returns an instance of the <code>CoreTokenStore</code> implementation
     * class. The instance is a singleton.
     * @return an instance of the <code>CoreTokenStore</code> implementation
     * class.
     * @throws CoreTokenException if failed to instantiate the instance.
     */
    public static CoreTokenStore getInstance() throws CoreTokenException {
        if (instance != null) {
            return instance;
        } else {
            throw new CoreTokenException(302, null, 500);
        }
    }
}
