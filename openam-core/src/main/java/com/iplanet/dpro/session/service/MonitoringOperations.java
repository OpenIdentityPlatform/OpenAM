/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SessionService.java,v 1.37 2010/02/03 03:52:54 bina Exp $
 *
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */

package com.iplanet.dpro.session.service;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.monitoring.Agent;
import com.sun.identity.monitoring.MonitoringUtil;
import com.sun.identity.monitoring.SsoServerSessSvcImpl;

import javax.inject.Singleton;

/**
 * Responsible for keeping a tally of the number of active sessions.
 *
 * Session monitoring logic extracted from SessionService class
 * as part of first-pass refactoring to improve SessionService adherence to SRP.
 *
 * @since 13.0.0
 */
/*
 * Further refactoring is warranted.
 */
@Singleton
public class MonitoringOperations {

    private int numberOfActiveSessions = 0;

    /**
     * Decrements number of active sessions
     */
    public synchronized void decrementActiveSessions() {
        // Fix for OPENAM-486: this is a sanity-check for sessioncount, so it
        // can't go below zero any more in case of erroneous behavior..
        if (numberOfActiveSessions > 0) {
            numberOfActiveSessions--;
            if (SystemProperties.isServerMode() && MonitoringUtil.isRunning()) {
                SsoServerSessSvcImpl sessImpl = Agent.getSessSvcMBean();
                sessImpl.decSessionActiveCount();
            }
        }
    }

    /**
     * Increments number of active sessions
     */
    public synchronized void incrementActiveSessions() {
        numberOfActiveSessions++;
        if (SystemProperties.isServerMode() && MonitoringUtil.isRunning()) {
            SsoServerSessSvcImpl sessImpl = Agent.getSessSvcMBean();
            sessImpl.incSessionActiveCount();
        }
    }

    /**
     * Returns number of active sessions
     */
    public synchronized int getActiveSessions() {
        return numberOfActiveSessions;
    }

}
