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
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.uma.UmaSettings;
import org.forgerock.openam.uma.UmaSettingsFactory;
import org.forgerock.util.promise.Promise;

public class UmaConfigurationResource implements SingletonResourceProvider {

    private final Debug logger = Debug.getInstance("UmaProvider");
    private final UmaSettingsFactory settingsFactory;

    @Inject
    public UmaConfigurationResource(UmaSettingsFactory settingsFactory) {
        this.settingsFactory = settingsFactory;
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest request) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest request) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {

        String realm = context.asContext(RealmContext.class).getResolvedRealm();
        UmaSettings settings = settingsFactory.create(realm);

        try {
            JsonValue config = json(object(
                    field("version", settings.getVersion()),
                    field("resharingMode", settings.getResharingMode())));

            return newResultPromise(newResourceResponse("UmaConfiguration", Integer.toString(config.hashCode()), config));
        } catch (ServerException e) {
            logger.error("Failed to get UMA Configuration", e);
            return new InternalServerErrorException("Failed to get UMA Configuration", e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest request) {
        return new NotSupportedException().asPromise();
    }
}
