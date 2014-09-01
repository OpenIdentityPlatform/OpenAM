/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SessionManager.java,v 1.3 2008/08/06 17:28:15 exu Exp $
 *
 */

package com.sun.identity.plugin.session;

import java.util.ResourceBundle;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.common.SystemConfigurationUtil;

/**
 * This class is used by toolkit for access the configured
 * SessionProvider implementation; This class is not to be
 * used by toolkit users.
 */
public final class SessionManager {

    private static final String PROP_SESSION_IMPL_CLASS =
        "com.sun.identity.plugin.session.class";
    private static SessionProvider sp = null;
    private static Debug debug = Debug.getInstance("libPlugins");
    private static ResourceBundle bundle =
        Locale.getInstallResourceBundle("libSessionProvider");
    static {
        String sessionClass =
            SystemConfigurationUtil.getProperty(PROP_SESSION_IMPL_CLASS);
        if (sessionClass == null || sessionClass.length() == 0) {
            debug.error("SessionManager static block: the property "+
                        PROP_SESSION_IMPL_CLASS + " is not set.");
        } else {
            try {
                sp = (SessionProvider)
                    Class.forName(sessionClass).newInstance();
            } catch (IllegalAccessException iae) {
                debug.error("SessionManager static block: "+
                            "Failed creating SessionProvider "+
                            "instance: " + sessionClass, iae);
            } catch (InstantiationException ie) {
                debug.error("SessionManager static block: "+
                            "Failed creating SessionProvider "+
                            "instance: " + sessionClass, ie);                
            } catch (ClassNotFoundException cnfe) {
                debug.error("SessionManager static block: "+
                            "Unable to find class " +
                            sessionClass, cnfe);
            }
        }
    }

    /**
     * Returns the configured <code>SessionProvider</code> instance.
     *
     * @return the configured session provider.
     * @throws SessionException if the session provider is null.
     */
    public static SessionProvider getProvider()
        throws SessionException {
            
        if (sp == null) {
            throw new SessionException(
                bundle.getString("nullSessionProvider"));
        }
        return sp;
    }
}

