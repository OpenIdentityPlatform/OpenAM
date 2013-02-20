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
 * $Id: LoggingThread.java,v 1.2 2009/07/24 20:02:23 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.log.handlers;

import com.iplanet.am.util.ThreadPool;
import com.iplanet.am.util.ThreadPoolException;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.common.ShutdownPriority;
import com.sun.identity.shared.debug.Debug;

public class LoggingThread {

    private static LoggingThread instance = null;
    private ThreadPool thread;

    protected LoggingThread() {
        ShutdownManager shutdownMan = ShutdownManager.getInstance();
        if (shutdownMan.acquireValidLock()) {
            try {
                // Don't load the Debug object in static block as it can
                // cause issues when doing a container restart.
                thread = new ThreadPool("LoggingThread", 1, 0, false, Debug.getInstance("amLogging"));
                shutdownMan.addShutdownListener(
                    new ShutdownListener() {
                        public void shutdown() {
                            thread.shutdown();
                            instance = null;
                        }
                    }, ShutdownPriority.LOWEST
                );
            } finally {
                shutdownMan.releaseLockAndNotify();
            }
        }
    }

    public synchronized static LoggingThread getInstance() {
        if (instance == null) {
            instance = new LoggingThread();
        }
        return instance;
    }

    public void run(Runnable task) throws ThreadPoolException {
        thread.run(task);
    }

}
