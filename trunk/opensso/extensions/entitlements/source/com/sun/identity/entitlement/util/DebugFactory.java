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
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: DebugFactory.java,v 1.4 2009/05/21 08:17:49 veiming Exp $
 */

package com.sun.identity.entitlement.util;

import com.sun.identity.shared.debug.DebugAdaptor;
import com.sun.identity.shared.debug.IDebug;
import com.sun.identity.shared.debug.IDebugProvider;

/**
 * Providers the debug provider handler.
 */
public final class DebugFactory {
    private static DebugFactory instance = new DebugFactory();

    private IDebugProvider impl;

    private DebugFactory() {
        //TODO load different debug provider.
        impl = DebugAdaptor.getProvider();
    }

    /**
     * Returns the debug factory.
     *
     * @return the debug factory.
     */
    public static DebugFactory getInstance() {
        return instance;
    }

    /**
     * Return debug provider.
     *
     * @return debug provider.
     */
    public IDebugProvider getProvider() {
        return impl;
    }

    /**
     * Return debugger.
     *
     * @param name Name of debugger.
     * @return debugger.
     */
    public static IDebug getDebug(String name) {
        return instance.impl.getInstance(name);
    }
}
