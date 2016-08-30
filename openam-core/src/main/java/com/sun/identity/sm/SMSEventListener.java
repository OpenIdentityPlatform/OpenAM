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

package com.sun.identity.sm;

/**
 * Defines a listener that should be notified when an SMS event occurs to an object. The notifications are triggered
 * by the SMSEventListenerManager.
 */
public interface SMSEventListener {
    /**
     * Called when an SMS event occurs to the requested object.
     * @param dn The DN of the object that the event has occurred for.
     * @param event The event that has occurred.
     */
    void notifySMSEvent(String dn, int event);
}
