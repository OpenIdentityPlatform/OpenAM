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
 * $Id: SsoServerSessSvcImpl.java,v 1.3 2009/11/02 20:10:45 hvijay Exp $
 *
 */

package com.sun.identity.monitoring;

import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.agent.SnmpMib;
import javax.management.MBeanServer;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.management.snmp.SnmpStatusException;

/**
 * This class extends the "SsoServerSessSvc" class.
 */
public class SsoServerSessSvcImpl extends SsoServerSessSvc {
    private static Debug debug = null;
    private static String myMibName;

    /**
     * Constructor
     */
    public SsoServerSessSvcImpl (SnmpMib myMib) {
        super(myMib);
        myMibName = myMib.getMibName();
        init(myMib, null);
    }

    public SsoServerSessSvcImpl (SnmpMib myMib, MBeanServer server) {
        super(myMib, server);
        myMibName = myMib.getMibName();
        init(myMib, server);
    }

    private void init (SnmpMib myMib, MBeanServer server) {
        if (debug == null) {
            debug = Debug.getInstance("amMonitoring");
        }
        SessionAveSessSize = new Integer(0);
        SessionNotifListnrCount = new Long(0);
        SessionNotifCount = new Long(0);
        SessionValidationsCount = new Long(0);
        SessionCreatedCount = new Long(0);
        SessionActiveCount = new Long(0);
        SessionSFOBroker = "N/A";
    }

    /*
     * increment the active session counter
     */
    public void incSessionActiveCount() {
        if (!Agent.isRunning()) {
            return;
        }

        if (debug.messageEnabled()) {
            debug.message("SsoServerSessSvcImpl.incSessionActiveCount");
        }

        long li = SessionActiveCount.longValue();
        li++;
        SessionActiveCount = Long.valueOf(li);
    }

    /*
     * decrement the active session counter
     */
    public void decSessionActiveCount() {
        if (!Agent.isRunning()) {
            return;
        }

        if (debug.messageEnabled()) {
            debug.message("SsoServerSessSvcImpl.decSessionActiveCount");
        }

        long li = SessionActiveCount.longValue();
        li--;
        if (li < 0) {
            SessionActiveCount = new Long(0);
        } else {
            SessionActiveCount = Long.valueOf(li);
        }
    }

    /*
     *  increment the created sessions counter
     *  does there need to be a destroyed sessions counter
     *  method to decrement the created sessions count?
     *  or is this counter just count sessions created?
     */
    public void incCreatedSessionCount() {
        if (!Agent.isRunning()) {
            return;
        }

        long li = SessionCreatedCount.longValue();
        li++;
        SessionCreatedCount = Long.valueOf(li);
    }

    /**
     * Getter for the "SessionNotifCount" variable.
     */
    public Long getSessionNotifCount() throws SnmpStatusException {
        return new Long(SessionService.getNotificationQueueSize());
    }

}
