/**
 * Copyright 2013 ForgeRock, Inc.
 *
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
 */
package org.forgerock.openam.forgerockrest.session.query;

import com.iplanet.dpro.session.share.SessionInfo;

import java.util.Collection;

/**
 * Defines the ability to query a Server and return Session based information from that server.
 *
 * This interface can easily be expanded to support more queries.
 *
 * @author robert.wapshott@forgerock.com
 */
public interface SessionQueryType {
    /**
     * Query a server and return all the Sessions that are stored on the server.
     *
     * @return Non null but possibly empty collection of Sessions.
     */
    public Collection<SessionInfo> getAllSessions();
}
