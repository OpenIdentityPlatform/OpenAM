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

package org.forgerock.openam.blacklist;

import com.sun.identity.shared.debug.Debug;

/**
 * A {@link Blacklist} implementation that ignores all elements. This is used when blacklisting is disabled as
 * a "Null Object Pattern".
 *
 * @param <T> The blacklist type.
 * @since 13.0.0
 */
public class NoOpBlacklist<T extends Blacklistable> implements Blacklist<T> {

    private static final Debug DEBUG = Debug.getInstance("blacklist");

    @Override
    public void blacklist(T entry) throws BlacklistException {
        // Ignore
    }

    @Override
    public boolean isBlacklisted(T entry) throws BlacklistException {
        return false;
    }

    @Override
    public void subscribe(Listener listener) {
        DEBUG.message("NoOpBlacklist: Ignoring entry blacklist listener {} - blacklisting disabled", listener);
    }
}
