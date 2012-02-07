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
 * $Id: SsoServerEntitlementSvcImpl.java,v 1.1 2009/10/20 23:55:28 bigfatrat Exp $
 *
 */
/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.monitoring;

import com.sun.identity.entitlement.opensso.DataStore;
import com.sun.identity.entitlement.opensso.OpenSSOIndexStore;
import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.SnmpStatusException;
import com.sun.management.snmp.agent.SnmpMib;
import javax.management.MBeanServer;

/**
 * This class extends the "SsoServerEntitlementSvc" class.
 */
public class SsoServerEntitlementSvcImpl extends SsoServerEntitlementSvc {
    private static Debug debug = null;

    /**
     * Constructor
     */
    public SsoServerEntitlementSvcImpl(SnmpMib myMib) {
        super(myMib);
        init(myMib, null);
    }

    public SsoServerEntitlementSvcImpl(SnmpMib myMib, MBeanServer server) {
        super(myMib);
        init(myMib, server);
    }

    private void init(SnmpMib myMib, MBeanServer server) {
        if (debug == null) {
            debug = Debug.getInstance("amMonitoring");
        }
    }

    public Integer getNumReferrals() throws SnmpStatusException {
        String classMethod = "SsoServerEntitlementSvcImpl.getNumReferrals: ";

        int i = DataStore.getNumberOfReferrals();
        NumReferrals = Integer.valueOf(i);
        if (debug.messageEnabled()) {
            StringBuilder sb = new StringBuilder(classMethod);
            sb.append(i);
            debug.message(sb.toString());
        }
        return NumReferrals;
    }

    public Integer getNumPolicies() throws SnmpStatusException {
        String classMethod = "SsoServerEntitlementSvcImpl.getNumPolicies: ";

        int i = DataStore.getNumberOfPolicies();
        NumPolicies = Integer.valueOf(i);
        if (debug.messageEnabled()) {
            StringBuilder sb = new StringBuilder(classMethod);
            sb.append(i);
            debug.message(sb.toString());
        }
        return NumPolicies;
    }

    public Integer getNumCachedPolicies() throws SnmpStatusException {
        String classMethod =
            "SsoServerEntitlementSvcImpl.getNumCachedPolicies: ";

        int i = OpenSSOIndexStore.getNumCachedPolicies();
        NumCachedPolicies = Integer.valueOf(i);
        if (debug.messageEnabled()) {
            StringBuilder sb = new StringBuilder(classMethod);
            sb.append(i);
            debug.message(sb.toString());
        }
        return NumCachedPolicies;
    }

    public Integer getNumCachedReferrals() throws SnmpStatusException {
        String classMethod =
            "SsoServerEntitlementSvcImpl.getNumCachedReferrals: ";

        int i = OpenSSOIndexStore.getNumCachedReferrals();
        NumCachedReferrals = Integer.valueOf(i);
        if (debug.messageEnabled()) {
            StringBuilder sb = new StringBuilder(classMethod);
            sb.append(i);
            debug.message(sb.toString());
        }
        return NumCachedReferrals;
    }
}
