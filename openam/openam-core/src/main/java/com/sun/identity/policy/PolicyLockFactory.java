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
package com.sun.identity.policy;

import org.forgerock.openam.shared.concurrency.LockFactory;

/**
 * Maintains a singleton instance to the shared lock factory.
 * <br />
 * This factory is intended to be used when concurrent policy changes are not atomic. The retrieved lock
 * can be used to protect the non-atomic logic, blocking other threads until all operations are completed.
 */
public enum PolicyLockFactory {

    INSTANCE;

    private final LockFactory<String> factory;

    private PolicyLockFactory() {
        factory = new LockFactory<String>();
    }

    /**
     * @return the singleton lock factory instance.
     */
    public LockFactory<String> getFactory() {
        return factory;
    }

}
