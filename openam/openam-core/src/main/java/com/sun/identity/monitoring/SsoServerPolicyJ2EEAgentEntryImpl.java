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
 * $Id: SsoServerPolicyJ2EEAgentEntryImpl.java,v 1.3 2009/10/21 00:03:11 bigfatrat Exp $
 *
 */

package com.sun.identity.monitoring;

import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.agent.SnmpMib;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * This class extends the "SsoServerPolicyJ2EEAgentEntry" class.
 */
public class SsoServerPolicyJ2EEAgentEntryImpl extends
    SsoServerPolicyJ2EEAgentEntry
{
    private static Debug debug = null;
    private static String myMibName;

    /**
     * Constructor
     */
    public SsoServerPolicyJ2EEAgentEntryImpl(SnmpMib myMib) {
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
        createSsoServerPolicyJ2EEAgentEntryObjectName (MBeanServer server)
    {
        String classModule = "SsoServerPolicyJ2EEAgentEntryImpl." +
            "createSsoServerPolicyJ2EEAgentEntryObjectName: ";
        String prfx = "ssoServerPolicyJ2EEAgentEntry.";

        if (debug.messageEnabled()) {
            debug.message(classModule +
                "\n    SsoServerRealmIndex = " + SsoServerRealmIndex +
                "\n    PolicyJ2EEAgentIndex = " + PolicyJ2EEAgentIndex +
                "\n    PolicyJ2EEAgentName = " + PolicyJ2EEAgentName +
                "\n    PolicyJ2EEAgentGroup = " + PolicyJ2EEAgentGroup +
                "\n    PolicyJ2EEAgentAgentURL = " + PolicyJ2EEAgentAgentURL);
        }

        String realm = Agent.getEscRealmNameFromIndex(SsoServerRealmIndex);
        String objname = myMibName +
            "/ssoServerPolicyJ2EEAgentTable:" +
            prfx + "ssoServerRealmName=" + realm + "," +
            prfx + "policyJ2EEAgentName=" + PolicyJ2EEAgentName;

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
}
