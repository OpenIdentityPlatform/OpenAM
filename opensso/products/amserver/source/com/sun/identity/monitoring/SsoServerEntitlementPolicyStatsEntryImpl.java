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
 * $Id: SsoServerEntitlementPolicyStatsEntryImpl.java,v 1.1 2009/10/20 23:55:27 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.monitoring;

import com.sun.identity.entitlement.opensso.OpenSSOIndexStore;
import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.SnmpStatusException;
import com.sun.management.snmp.agent.SnmpMib;
import javax.management.MBeanServer;
import javax.management.ObjectName;




/**
 * This class extends the "SsoServerEntitlementPolicyStatsEntry" class.
 */
public class SsoServerEntitlementPolicyStatsEntryImpl extends
    SsoServerEntitlementPolicyStatsEntry
{
    private static Debug debug = null;
    private static String myMibName;

    /**
     * Constructor
     */
    public SsoServerEntitlementPolicyStatsEntryImpl(SnmpMib myMib) {
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
        createSsoServerEntitlementPolicyStatsEntryObjectName (
            MBeanServer server)
    {
        String classModule = "SsoServerEntitlementPolicyStatsEntryImpl." +
            "createSsoServerEntitlementPolicyStatsEntryObjectName: ";
        String prfx = "ssoServerEntitlementPolicyStatsEntry.";

        if (debug.messageEnabled()) {
            debug.message(classModule +
                "\n    SsoServerRealmIndex = " + SsoServerRealmIndex +
                "\n    StatsName = EntitlementPolicy");
        }

        EntitlementPolicyStatsRealmName =
            Agent.getEscRealmNameFromIndex(SsoServerRealmIndex);
        String objname = myMibName +
            "/ssoServerEntitlementPolicyStatsTable:" +
            prfx + "EntitlementPolicyStatsRealmName=" +
            EntitlementPolicyStatsRealmName +
            "," + prfx + "StatsEntryName=EntitlementPolicy";

        try {
            if (server == null) {
                return null;
            } else {
                // is the object name sufficiently unique?
                return new ObjectName(objname);
            }
        } catch (Exception ex) {
            debug.error(classModule + objname, ex);
            return null;
        }
    }

    /**
     * Getter for the "EntitlementPolicyCaches" variable.
     */
    public Integer getEntitlementPolicyCaches() throws SnmpStatusException {
        String classMethod = "SsoServerEntitlementPolicyStatsEntryImpl." +
            "getEntitlementPolicyCaches: ";

        int i = OpenSSOIndexStore.getNumCachedPolicies(
                    EntitlementPolicyStatsRealmName);
        EntitlementPolicyCaches = Integer.valueOf(i);

        if (debug.messageEnabled()) {
            StringBuilder sb = new StringBuilder(classMethod);
            sb.append("for realm ").append(EntitlementPolicyStatsRealmName);
            sb.append(" = ").append(i);
            debug.message(sb.toString());
        }
        return EntitlementPolicyCaches;
    }


    /**
     * Getter for the "EntitlementReferralCaches" variable.
     */
    public Integer getEntitlementReferralCaches() throws SnmpStatusException {
        String classMethod = "SsoServerEntitlementPolicyStatsEntryImpl." +
            "getEntitlementReferralCaches: ";
        int i = OpenSSOIndexStore.getNumCachedReferrals(
                    EntitlementPolicyStatsRealmName);
        EntitlementReferralCaches = Integer.valueOf(i);

        if (debug.messageEnabled()) {
            StringBuilder sb = new StringBuilder(classMethod);
            sb.append("for realm ").append(EntitlementPolicyStatsRealmName);
            sb.append(" = ").append(i);
            debug.message(sb.toString());
        }
        return EntitlementReferralCaches;
    }
}
