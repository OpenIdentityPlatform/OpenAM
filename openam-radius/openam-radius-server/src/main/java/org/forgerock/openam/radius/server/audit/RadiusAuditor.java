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
package org.forgerock.openam.radius.server.audit;

import org.forgerock.openam.radius.server.events.AuthRequestAcceptedEvent;
import org.forgerock.openam.radius.server.events.AuthRequestChallengedEvent;
import org.forgerock.openam.radius.server.events.AuthRequestReceivedEvent;
import org.forgerock.openam.radius.server.events.AuthRequestRejectedEvent;

/**
 * Interface through which audit logs may be made.
 */
public interface RadiusAuditor {

    /**
     * Creates an Access Request entry to the Common Audit Framework.
     *
     * @param authRequestReceivedEvent a <code>RadiusEvent</code> containing details of the access request
     */
    void recordAuthRequestReceivedEvent(AuthRequestReceivedEvent authRequestReceivedEvent);

    /**
     * Creates and Access Request entry to the Common Audit Framework with successful response info.
     *
     * @param authRequestAcceptedEvent an event containing details of the access request and response.
     */
    void recordAuthRequestAcceptedEvent(AuthRequestAcceptedEvent authRequestAcceptedEvent);

    /**
     * Creates and Access Request entry to the Common Audit Framework with rejected response info.
     *
     * @param authRequestRejectedEvent an event containing details of the access request and response.
     */
    void recordAuthRequestRejectedEvent(AuthRequestRejectedEvent authRequestRejectedEvent);

    /**
     * Creates and Access Request entry to the Common Audit Framework with rejected response info.
     *
     * @param authRequestChallengedEvent an event containing details of the access request and challenge response.
     */
    void recordAuthRequestChallengedEvent(AuthRequestChallengedEvent authRequestChallengedEvent);

}
