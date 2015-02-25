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

package org.forgerock.oauth2;

import org.forgerock.oauth2.core.ResourceOwner;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 12.0.0
 */
public class ResourceOwnerImpl implements ResourceOwner {

    private final String username;
    private final String password;
    private final Map<String, String> attributes = new HashMap<String, String>();

    public ResourceOwnerImpl(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    public String getId() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAttribute(String attribute) {
        return attributes.get(attribute);
    }

    public String getModifiedTimestamp() {
        return "0";
    }
}
