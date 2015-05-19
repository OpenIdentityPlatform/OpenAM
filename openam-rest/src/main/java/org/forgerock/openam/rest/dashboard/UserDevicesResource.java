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

package org.forgerock.openam.rest.dashboard;

import java.text.ParseException;
import java.util.List;
import javax.security.auth.Subject;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.entitlements.RealmAwareResource;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;

/**
 * REST resource for a user's trusted devices.
 *
 * @since 13.0.0
 */
public abstract class UserDevicesResource<T extends UserDevicesDao> extends RealmAwareResource {

    static final String USER = "user";
    static final String UUID_KEY = "uuid";

    protected final T userDevicesDao;

    /**
     * Constructs a new UserDevicesResource.
     *
     * @param userDevicesDao An instance of the {@code UserDevicesDao}.
     */
    public UserDevicesResource(T userDevicesDao) {
        this.userDevicesDao = userDevicesDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException("Not supported."));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
                               ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException("Not supported."));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("Not supported."));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
                               ResultHandler<Resource> handler) {

        final Subject subject = getContextSubject(context);
        final String principalName = PrincipalRestUtils.getPrincipalNameFromSubject(subject);

        try {
            List<JsonValue> devices = userDevicesDao.getDeviceProfiles(principalName, getRealm(context));

            JsonValue toDelete = null;
            for (JsonValue device : devices) {
                if (resourceId.equals(device.get(UUID_KEY).asString())) {
                    toDelete = device;
                    break;
                }
            }

            if (toDelete == null) {
                handler.handleError(new NotFoundException("User device, " + resourceId + ", not found."));
                return;
            }

            devices.remove(toDelete);

            userDevicesDao.saveDeviceProfiles(principalName, getRealm(context), devices);

            handler.handleResult(new Resource(resourceId, toDelete.hashCode() + "", toDelete));
        } catch (InternalServerErrorException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
                              ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("Not supported."));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        try {
            final Subject subject = getContextSubject(context);
            final String principalName = PrincipalRestUtils.getPrincipalNameFromSubject(subject);

            for (JsonValue profile : userDevicesDao.getDeviceProfiles(principalName, getRealm(context))) {
                handler.handleResource(convertValue(profile));
            }
            handler.handleResult(new QueryResult());
        } catch (ParseException e) {
            handler.handleError(new InternalServerErrorException(e.getMessage()));
        } catch (InternalServerErrorException e) {
            handler.handleError(e);
        }
    }

    protected abstract Resource convertValue(JsonValue queryResult) throws ParseException;

    /**
     * {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
                             ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("Not supported."));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
                               ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("Not supported."));
    }

}