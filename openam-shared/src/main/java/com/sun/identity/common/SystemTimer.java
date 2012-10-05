/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SystemTimer.java,v 1.4 2008/08/08 00:40:59 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.common;

import com.sun.identity.shared.debug.Debug;

/**
 * SystemTimer is a TimerPool which has only 2 Timer shared in the system.
 */

public class SystemTimer {
    

    protected static TimerPool instance;
    
    /**
     * Create and return the system timer.
     */
    
    public static synchronized TimerPool getTimer() {
        if (instance == null) {
            ShutdownManager shutdownMan = ShutdownManager.getInstance();
            if (shutdownMan.acquireValidLock()) {
                try {
                    // Don't load the Debug object in static block as it can
                    // cause issues when doing a container restart.
                    instance = new TimerPool("SystemTimer", 1, false, Debug.getInstance("SystemTimer"));
                    shutdownMan.addShutdownListener(new ShutdownListener() {
                        public void shutdown() {
                            instance.shutdown();
                            instance = null;
                        }
                    });
                } finally {
                    shutdownMan.releaseLockAndNotify();
                }
            }
        }
        return instance;
    }
}
