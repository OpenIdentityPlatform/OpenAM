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
 * $Id: SsoServerAuthModulesEntryImpl.java,v 1.3 2009/10/21 00:02:10 bigfatrat Exp $
 *
 */

package com.sun.identity.monitoring;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.agent.SnmpMib;

/**
 * This class extends the "SsoServerAuthModulesEntry" class.
 */
public class SsoServerAuthModulesEntryImpl extends SsoServerAuthModulesEntry {
    private static Debug debug = null;
    private static String myMibName;

    /**
     * Constructor
     */
    public SsoServerAuthModulesEntryImpl (SnmpMib myMib) {
        super(myMib);
        myMibName = myMib.getMibName();
        init();
    }

    private void init() {
        if (debug == null) {
            debug = Debug.getInstance("amMonitoring");
        }
    }

    public ObjectName
        createSsoServerAuthModulesEntryObjectName (MBeanServer server)
    {
        String classModule = "SsoServerAuthModulesEntryImpl." +
            "createSsoServerAuthModulesEntryObjectName: ";
        String prfx = "ssoServerAuthModulesEntry.";
        String realmName =
            Agent.getEscRealmNameFromIndex(SsoServerRealmIndex);

        if (debug.messageEnabled()) {
            debug.message(classModule +
                "\n    SsoServerRealmIndex = " +
                SsoServerRealmIndex +
                "\n    AuthModuleIndex = " +
                AuthModuleIndex +
                "\n    AuthModuleName = " +
                AuthModuleName +
                "\n    AuthModuleType = " +
                AuthModuleType +
                "\n    AuthModuleName = " +
                AuthModuleName);
        }

        String objname = myMibName +
            "/ssoServerAuthModulesTable:" +
            prfx + "ssoServerRealmName=" + realmName + "," +
            prfx + "authModuleName=" + AuthModuleName;

        try {
            if (server == null) {
                return null;
            } else {
                // is the object name sufficiently unique?
                return
                    new ObjectName(objname);
            }
        } catch (Exception ex) {
            debug.error(classModule + objname, ex);
            return null;
        }
    }

    /*
     * increment the module's authentication failure count
     */
    public void incModuleFailureCount() {
        long li = AuthModuleFailureCount.longValue();
        li++;
        AuthModuleFailureCount = Long.valueOf(li);
    }

    /*
     * increment the module's authentication success count
     */
    public void incModuleSuccessCount() {
        long li = AuthModuleSuccessCount.longValue();
        li++;
        AuthModuleSuccessCount = Long.valueOf(li);
    }
}
