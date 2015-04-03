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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.session.blacklist;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.service.SessionConstants;
import com.sun.identity.shared.debug.Debug;

/**
 * A {@link SessionBlacklist} implementation that ignores all elements. This is used when blacklisting is disabled as
 * a "Null Object Pattern".
 *
 * @since 13.0.0
 */
public enum NoOpSessionBlacklist implements SessionBlacklist {
    INSTANCE;

    private static final Debug DEBUG = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    @Override
    public void blacklist(final Session session) throws SessionException {
        // Ignore
    }

    @Override
    public boolean isBlacklisted(final Session session) throws SessionException {
        return false;
    }

    @Override
    public void subscribe(final Listener listener) {
        DEBUG.message("NoOpSessionBlacklist: Ignoring session blacklist listener {} - blacklisting disabled", listener);
    }
}
