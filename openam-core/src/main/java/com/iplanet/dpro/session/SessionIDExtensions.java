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
package com.iplanet.dpro.session;

import java.util.Map;

/**
 * Responsible for modelling the Primary Server, Site and Storage Key of a Session ID.
 */
public interface SessionIDExtensions {

    /**
     * The Primary ID for the SessionID is that Sessions home server.
     * @return Non null String.
     */
    String getPrimaryID();

    /**
     * Servers can be clustered together into Sites which are grouped for Session Failover and
     * crosstalk purposes.
     *
     * @return If the Primary server belongs to a Site, this value will be populated.
     */
    String getSiteID();

    String getStorageKey();

    String get(String key);

    void add(String key, String value);

    Map<String, String> asMap();
}
