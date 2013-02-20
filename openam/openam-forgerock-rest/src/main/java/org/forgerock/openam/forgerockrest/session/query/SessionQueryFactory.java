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

import org.forgerock.openam.forgerockrest.session.query.impl.RemoteSessionQuery;

/**
 * SessionQueryFactory provides a means of generating SessionQueryTypes based on the server id that is provided.
 *
 * @author robert.wapshott@forgerock.com
 */
public class SessionQueryFactory {
    /**
     * Implementation is currently hard-coded to return the RemoteSessionQuery.
     *
     * @param serverId Non null server id.
     * @return A non null SessionQueryType based on the id.
     */
    public SessionQueryType getSessionQueryType(String serverId) {
        return new RemoteSessionQuery(serverId);
    }
}
