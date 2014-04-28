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

package org.forgerock.common;

import org.forgerock.oauth2.ResourceOwnerImpl;

import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @since 12.0.0
 */
@Singleton
public class UserStore {

    private final Map<String, ResourceOwnerImpl> resourceOwners = new ConcurrentHashMap<String, ResourceOwnerImpl>();

    {
        resourceOwners.put("demo", new ResourceOwnerImpl("demo", "cangetin"));
    }

    public ResourceOwnerImpl get(String userId) {
        return resourceOwners.get(userId);
    }
}
