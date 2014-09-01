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
 * $Id: AMX509KeyManagerFactory.java,v 1.2 2008/06/25 05:52:58 qcheng Exp $
 *
 */

package com.sun.identity.security.keystore;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.security.SecurityDebug;

/**
 * The <code>WSX509KeyManager</code> class implements JSSE X509KeyManager
 * interface. This implementation is the same as JSSE default implementation
 * exception it will supply user-specified client certificate alias when
 * client authentication is on.
 */
public class AMX509KeyManagerFactory {
    private static final String JKS_ONLY_AMX509KEYMANAGER_CLASS =
                 "com.sun.identity.security.keystore.v_14.AMX509KeyManagerImpl";
    public static Debug debug = SecurityDebug.debug;

    public static String jvmVersionStr = null;
    public static float jvmVersion;
    
    static {
        jvmVersionStr = System.getProperty("java.vm.version");
        if (debug.messageEnabled()) {
            debug.message("AMX509KeyManagerFactory : "+
                          " runtime version = " + jvmVersionStr);
        }
        
        jvmVersion = Float.valueOf(jvmVersionStr.substring(0, 3)).floatValue();
    }
    
    public static AMX509KeyManager createAMX509KeyManager() {
        if (debug.messageEnabled()) {
            debug.message("AMX509KeyManagerFactory.createAMX509KeyManager : ");
        }
        try {
            if (jvmVersion <= 1.4) {
                if (debug.messageEnabled()) {
                    debug.message("returns " + JKS_ONLY_AMX509KEYMANAGER_CLASS);
                }
                return (AMX509KeyManager)Class.
                        forName(JKS_ONLY_AMX509KEYMANAGER_CLASS).newInstance();
            }

            if (debug.messageEnabled()) {
                debug.message("returns AMX509KeyManagerImpl.");
            }
        } catch (Exception ex) {
            if (debug.warningEnabled()) {
                debug.warning("AMX509KeyManagerFactory.createAMX509KeyManager:",
                              ex);
            }
        }

        return new AMX509KeyManagerImpl();
    }

    public static AMX509KeyManager createAMX509KeyManager(        
        String ksType,
        String ksFile,
        String ksProvider,
        AMCallbackHandler cbHandle) {
        try {
            if (jvmVersion <= 1.4) {
                return (AMX509KeyManager)Class.
                        forName(JKS_ONLY_AMX509KEYMANAGER_CLASS).newInstance();
            }

            return new AMX509KeyManagerImpl(ksType, ksFile, ksProvider,
                      cbHandle);
        } catch (Exception ex) {
            if (debug.warningEnabled()) {
                debug.warning("AMX509KeyManagerFactory.createAMX509KeyManager:",
                              ex);
            }
        }

        return new AMX509KeyManagerImpl();
    }

}
