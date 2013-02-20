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
 * $Id: EntitlementThreadPool.java,v 1.1 2009/08/19 05:40:32 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */

package com.sun.identity.entitlement;

import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.entitlement.interfaces.IThreadPool;

/**
 * Thread Pool
 */
public class EntitlementThreadPool implements IThreadPool {
    private ThreadPool thrdPool;

    public EntitlementThreadPool(int size) {

        thrdPool = new ThreadPool("entitlementThreadPool", size);
        ShutdownManager shutdownMan = ShutdownManager.getInstance();
        if (shutdownMan.acquireValidLock()) {
            try {
                shutdownMan.addShutdownListener(new ShutdownListener() {
                    public void shutdown() {
                        thrdPool.shutdown();
                        thrdPool = null;
                    }
                });
            } finally {
                shutdownMan.releaseLockAndNotify();
            }
        }
    }


    public void submit(Runnable task) {

        if (thrdPool != null) {
            try {
                thrdPool.run(task);
            } catch (ThreadPoolException e) {
                PrivilegeManager.debug.error("EntitlementThreadPool.submit", e);
            }
        }
    }
}
