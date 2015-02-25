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

package org.forgerock.openam.oauth2.legacy;

import org.forgerock.oauth2.core.AccessToken;

import java.util.Map;

/**
 * Adapter between new {@link AccessToken} and legacy {@link CoreToken}.
 *
 * @since 12.0.0
 */
@Deprecated
public class LegacyAccessTokenAdapter extends CoreToken {

    private final AccessToken token;

    public LegacyAccessTokenAdapter(AccessToken token) {
        super(token.getTokenId(), token);
        this.token = token;
    }

    @Override
    public Map<String, Object> convertToMap() {
        return token.toMap();
    }

    @Override
    public Map<String, Object> getTokenInfo() {
        return token.getTokenInfo();
    }
}
