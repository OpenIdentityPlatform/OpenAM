/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package com.sun.identity.monitoring;

import com.sun.management.snmp.agent.SnmpMib;
import javax.management.MBeanServer;

/**
 *
 * @author Peter Major
 */
public class SsoServerConnPoolSvcImpl extends SsoServerConnPoolSvc {

    /**
     * Constructor
     */
    public SsoServerConnPoolSvcImpl(SnmpMib myMib) {
        super(myMib);
        init(myMib, null);
    }

    public SsoServerConnPoolSvcImpl(SnmpMib myMib, MBeanServer server) {
        super(myMib);
        init(myMib, server);
    }

    private void init(SnmpMib myMib, MBeanServer server) {
    }

    public void updateWaitingTime(long then, long now) {
        ConnRequestWaitTimeCurrent = now - then;
        if (ConnRequestWaitTimeCurrent < ConnRequestWaitTimeLowWaterMark) {
            ConnRequestWaitTimeLowWaterMark = ConnRequestWaitTimeCurrent;
        }
        if (ConnRequestWaitTimeHighWaterMark < ConnRequestWaitTimeCurrent) {
            ConnRequestWaitTimeHighWaterMark = ConnRequestWaitTimeCurrent;
        }
    }

    public void adjustBusyConnections(int diff) {
        NumConnUsedCurrent += diff;
        if (NumConnUsedCurrent < NumConnUsedLowWaterMark) {
            NumConnUsedLowWaterMark = NumConnUsedCurrent;
        }
        if (NumConnUsedHighWaterMark < NumConnUsedCurrent) {
            NumConnUsedHighWaterMark = NumConnUsedCurrent;
        }
    }

    public void incReleasedConns() {
        NumConnReleased++;
    }
    
    public void setUsedConnections(int newVal) {
    	NumConnUsedCurrent = newVal;
    }
}
