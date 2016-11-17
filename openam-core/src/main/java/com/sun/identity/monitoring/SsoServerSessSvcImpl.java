/*
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
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package com.sun.identity.monitoring;

import javax.management.MBeanServer;

import org.forgerock.guice.core.InjectorHolder;

import com.iplanet.dpro.session.service.SessionNotificationSender;
import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.SnmpStatusException;
import com.sun.management.snmp.agent.SnmpMib;

/**
 * This class extends the "SsoServerSessSvc" class.
 */
public class SsoServerSessSvcImpl extends SsoServerSessSvc {
    private static Debug debug = null;

    /**
     * Constructor
     */
    public SsoServerSessSvcImpl (SnmpMib myMib) {
        super(myMib);
        init(myMib, null);
    }

    public SsoServerSessSvcImpl (SnmpMib myMib, MBeanServer server) {
        super(myMib, server);
        init(myMib, server);
    }

    private void init (SnmpMib myMib, MBeanServer server) {
        if (debug == null) {
            debug = Debug.getInstance("amMonitoring");
        }
    }

    /*
     *  increment the created sessions counter
     *  does there need to be a destroyed sessions counter
     *  method to decrement the created sessions count?
     *  or is this counter just count sessions created?
     */
    public void incCreatedSessionCount() {
        long li = SessionCreatedCount.longValue();
        li++;
        SessionCreatedCount = Long.valueOf(li);
    }

    /**
     * Getter for the "SessionNotifCount" variable.
     */
    public Long getSessionNotifCount() throws SnmpStatusException {
        return Long.valueOf(InjectorHolder.getInstance(SessionNotificationSender.class).getNotificationQueueSize());
    }

}
