/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */
package com.sun.identity.common;

import javax.inject.Inject;

/**
 * Simplifies the usage of adding a ShutdownListener to the ShutdownManager.
 * This is a non-static class to simplify testing and dependency injection.
 */
public class ShutdownManagerWrapper {
    private final ShutdownManager shutdownManager;

    @Inject
    public ShutdownManagerWrapper(ShutdownManager manager) {
        this.shutdownManager = manager;
    }

    /**
     * @see com.sun.identity.common.ShutdownManager#addShutdownListener(com.sun.identity.common.ShutdownListener)
     */
    public void addShutdownListener(final ShutdownListener listener) {
        perform(listener, true);
    }

    /**
     * @see com.sun.identity.common.ShutdownManager#removeShutdownListener(com.sun.identity.common.ShutdownListener)
     */
    public void removeShutdownListener(ShutdownListener listener) {
        perform(listener, false);
    }

    /**
     * Handles the detail of locking the ShutdownManager and releasing the lock.
     * @param listener Non null listener to perform action on.
     * @param add True indicates add operation, false remove.
     */
    private void perform(ShutdownListener listener, boolean add) {
        if (shutdownManager.acquireValidLock()) {
            try {
                if (add) {
                    shutdownManager.addShutdownListener(listener);
                } else {
                    shutdownManager.removeShutdownListener(listener);
                }
            } finally {
                shutdownManager.releaseLockAndNotify();
            }
        } else {
            throw new IllegalStateException("Failed to acquire lock");
        }
    }
}
