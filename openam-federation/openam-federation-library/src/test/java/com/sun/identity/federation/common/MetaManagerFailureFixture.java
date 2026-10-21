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
 * Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.federation.common;

import java.lang.reflect.Field;

/**
 * Reads and sets the time of the last failed attempt to build the ID-FF meta manager, which
 * holds {@code FSUtils.getIDFFMetaManager} off the next attempt for a while. A test that leaves
 * no meta manager in place records a failure there, and puts the time back afterwards so that
 * the next test does not start inside that interval. Process-wide, so callers hold
 * {@link FederationStaticsLock}.
 */
public final class MetaManagerFailureFixture {

    private MetaManagerFailureFixture() {
    }

    /** The time of the last failed attempt, <code>0</code> when none is remembered. */
    public static long failedAt() throws Exception {
        return failedAtField().getLong(null);
    }

    public static void setFailedAt(long failedAt) throws Exception {
        failedAtField().setLong(null, failedAt);
    }

    private static Field failedAtField() throws Exception {
        Field field = FSUtils.class.getDeclaredField("metaManagerFailedAt");
        field.setAccessible(true);
        return field;
    }
}
