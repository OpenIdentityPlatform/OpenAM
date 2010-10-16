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
 * $Id: SsoServerPolicySvcImpl.java,v 1.2 2009/10/21 00:03:12 bigfatrat Exp $
 *
 */

package com.sun.identity.monitoring;

import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.agent.SnmpMib;
import javax.management.MBeanServer;

/**
 * This class extends the "SsoServerPolicySvc" class.
 */
public class SsoServerPolicySvcImpl extends SsoServerPolicySvc {
    private static Debug debug = null;
    private static String myMibName;

    /**
     * Constructor
     */
    public SsoServerPolicySvcImpl(SnmpMib myMib) {
        super(myMib);
        myMibName = myMib.getMibName();
        init(myMib, null);
    }

    public SsoServerPolicySvcImpl(SnmpMib myMib, MBeanServer server) {
        super(myMib);
        myMibName = myMib.getMibName();
        init(myMib, server);
    }

    private void init(SnmpMib myMib, MBeanServer server) {
        if (debug == null) {
            debug = Debug.getInstance("amMonitoring");
        }
        PolicyEvalsOut = new Long(0);
        PolicyEvalsIn = new Long(0);
        PolicyStatus = "dormant";
    }

    /*
     *  set the Policy service's status to "operational"
     */
    public void setPolicyStatusOperational() {
        if (!Agent.isRunning()) {
            return;
        }
        PolicyStatus = new String("operational");
    }

    /*
     *  set the Policy service's status to "dormant";
     *  likely won't happen.
     */
    public void setPolicyStatusDormant() {
        if (!Agent.isRunning()) {
            return;
        }
        PolicyStatus = new String("dormant");
    }

    /*
     *  increment the number of evaluation requests received
     */
    public void incPolicyEvalsIn() {
        if (!Agent.isRunning()) {
            return;
        }
        if (PolicyStatus.equals("dormant")) {
            setPolicyStatusOperational();
        }
        long li = PolicyEvalsIn.longValue();
        li++;
        PolicyEvalsIn = Long.valueOf(li);
    }

    /*
     *  increment the number of evaluation requests processed
     */
    public void incPolicyEvalsOut() {
        if (!Agent.isRunning()) {
            return;
        }
        long li = PolicyEvalsOut.longValue();
        li++;
        PolicyEvalsOut = Long.valueOf(li);
    }
}
