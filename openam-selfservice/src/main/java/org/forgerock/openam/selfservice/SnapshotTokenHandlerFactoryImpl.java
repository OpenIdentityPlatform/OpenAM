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
package org.forgerock.openam.selfservice;

import org.forgerock.selfservice.core.snapshot.SnapshotTokenConfig;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandler;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import java.util.Map;

/**
 * Factory for providing snapshot token handlers.
 *
 * @since 13.0.0
 */
final class SnapshotTokenHandlerFactoryImpl implements SnapshotTokenHandlerFactory {

    private final Map<String, SnapshotTokenHandler> tokenHandlers;

    @Inject
    SnapshotTokenHandlerFactoryImpl(Map<String, SnapshotTokenHandler> tokenHandlers) {
        this.tokenHandlers = tokenHandlers;
    }

    @Override
    public SnapshotTokenHandler get(SnapshotTokenConfig tokenType) {
        Reject.ifFalse(tokenHandlers.containsKey(tokenType), "Unknown snapshot token type");
        return tokenHandlers.get(tokenType);
    }

}
