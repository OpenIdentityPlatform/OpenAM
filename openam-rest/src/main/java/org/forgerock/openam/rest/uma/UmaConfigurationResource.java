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

package org.forgerock.openam.rest.uma;

import static org.forgerock.json.JsonValue.*;

import javax.inject.Inject;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.uma.UmaSettings;
import org.forgerock.openam.uma.UmaSettingsFactory;

public class UmaConfigurationResource implements SingletonResourceProvider {

    private final Debug logger = Debug.getInstance("UmaProvider");
    private final UmaSettingsFactory settingsFactory;

    @Inject
    public UmaConfigurationResource(UmaSettingsFactory settingsFactory) {
        this.settingsFactory = settingsFactory;
    }

    @Override
    public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException());
    }

    @Override
    public void patchInstance(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    @Override
    public void readInstance(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {

        String realm = context.asContext(RealmContext.class).getResolvedRealm();
        UmaSettings settings = settingsFactory.create(realm);

        try {
            JsonValue config = json(object(
                    field("version", settings.getVersion()),
                    field("resharingMode", settings.getResharingMode())));

            handler.handleResult(new Resource("UmaConfiguration", Integer.toString(config.hashCode()), config));
        } catch (ServerException e) {
            logger.error("Failed to get UMA Configuration", e);
            handler.handleError(new InternalServerErrorException("Failed to get UMA Configuration", e));
        }
    }

    @Override
    public void updateInstance(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }
}
