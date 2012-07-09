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
 * $Id: LogFactory.java,v 1.2 2010/01/07 00:19:12 veiming Exp $
 */

package com.sun.identity.entitlement.util;

import com.sun.identity.entitlement.PrivilegeManager;

/**
 * Provides Log Provider handler.
 */
public class LogFactory {
    private static LogFactory instance = new LogFactory();

    private ILogProvider impl;

    private LogFactory() {
        try {
            //RFE: load different log provider.
            Class clazz = Class.forName(
                "com.sun.identity.entitlement.opensso.LogProvider");
            impl = (ILogProvider)clazz.newInstance();
        } catch (InstantiationException e) {
            PrivilegeManager.debug.error("LogFactory.<init>", e);
        } catch (IllegalAccessException e) {
            PrivilegeManager.debug.error("LogFactory.<init>", e);
        } catch (ClassNotFoundException e) {
            PrivilegeManager.debug.error("LogFactory.<init>", e);
        }
    }

    /**
     * Returns an instance of the factory.
     *
     * @return an instance of the factory.
     */
    public static LogFactory getInstance() {
        return instance;
    }

    public ILogProvider getLogProvider() {
        return impl;
    }
}
