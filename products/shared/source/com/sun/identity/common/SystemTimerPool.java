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
 * $Id: SystemTimerPool.java,v 1.5 2008/09/05 00:51:02 ww203982 Exp $
 *
 */

package com.sun.identity.common;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * SystemTimerPool is a TimerPool which shared in the system.
 */

public class SystemTimerPool {
    
    protected static TimerPool instance;
    protected static Debug debug;
    public static final int DEFAULT_POOL_SIZE = 3;
    private static int poolSize;

    static {
        debug = Debug.getInstance("SystemTimerPool");
        poolSize = DEFAULT_POOL_SIZE;
        String size = SystemPropertiesManager.get(
            Constants.SYSTEM_TIMERPOOL_SIZE);
        if (size != null) {
            try {
                poolSize = Integer.parseInt(size);
            } catch (NumberFormatException ex) {
                debug.error("SystemTimerPool.<init>: incorrect pool size "
                    + size + " defaulting to " + DEFAULT_POOL_SIZE);
            }
        }
    }
    
    /**
     * Create and return the system timer pool.
     */
    
    public static synchronized TimerPool getTimerPool() {
        if (instance == null) {
            ShutdownManager shutdownMan = ShutdownManager.getInstance();
            if (shutdownMan.acquireValidLock()) {
                try {
                    instance = new TimerPool("SystemTimerPool", 
                        poolSize, false, debug);
                    shutdownMan.addShutdownListener(new ShutdownListener() {
                        public void shutdown() {
                            instance.shutdown();
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
