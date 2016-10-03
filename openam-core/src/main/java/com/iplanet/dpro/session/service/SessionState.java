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
 * Copyright 2016 ForgeRock AS.
 */
package com.iplanet.dpro.session.service;

/**
 * State of the session.
 * <p>
 * The following diagram illustrates the state transitions for a session:
 * <pre>
 *
 *                     |
 *                     |
 *                     |
 *                     V
 *       ---------- invalid
 *      |              |
 *      |              |creation (authentication OK)
 *      |              |
 *      |max login time|   max idle time
 *      |destroy       V  ---------------&gt;
 *      |            valid              inactive --
 *      |              |  &lt;--------------           |
 *      |              |       reactivate           |
 *      |              |                            |
 *      |              | logout                     | destroy
 *      |              | destroy                    | max session time
 *      |              | max session time           |
 *      |              V                            |
 *       ---------&gt;  destroy  &lt;---------------------
 *
 * </pre>
 */
public enum SessionState {

    /**
     * Marks a session as invalid.
     * <p>
     * Sessions start in an {@code INVALID} state.
     * <p>
     * After successful authentication, sessions can transition to the {@code VALID} state;
     * alternatively, if authentication times out, the authentication session will transition
     * to a {@code DESTROYED} state.
     */
    INVALID,

    /**
     * Marks a session as valid.
     * <p>
     * Sessions can transition to a {@code VALID} state from an {@code INVALID} state.
     * <p>
     * A {@code VALID} session can transition to a {@code DESTROYED} state when:
     * <ul>
     *     <li>The session owner requests logout.</li>
     *     <li>An administrator requests the session be destroyed.</li>
     *     <li>The session times out (and no purge delay is configured).</li>
     * </ul>
     */
    VALID,

    /**
     * Marks a session as inactive.
     * <p>
     * Sessions can transition to an {@code INVALID} state from a {@code VALID}.
     * <p>
     * An {@code INVALID} session can transition to a {@code DESTROYED} state when:
     * <ul>
     *     <li>The purge delay elapses.</li>
     * </ul>
     */
    INACTIVE,

    /**
     * Marks a session as destroyed.
     * <p>
     * Sessions can transition to a {@code DESTROYED} state from any other state.
     */
    DESTROYED

}
