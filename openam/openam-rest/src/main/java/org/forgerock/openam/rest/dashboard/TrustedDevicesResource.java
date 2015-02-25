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

package org.forgerock.openam.rest.dashboard;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
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

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * REST resource for a user's trusted devices.
 *
 * @since 12.0.0
 */
public class TrustedDevicesResource implements CollectionResourceProvider {

    private static final DateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final DateFormat DATE_FORMATTER = SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM,
            DateFormat.SHORT);
    private static final String LAST_SELECTED_DATE_KEY = "lastSelectedDate";

    private final TrustedDevicesDao trustedDevicesDao;

    /**
     * Constructs a new TrustedDevucesResource.
     *
     * @param trustedDevicesDao An instance of the {@code TrustedDevicesDao}.
     */
    @Inject
    public TrustedDevicesResource(TrustedDevicesDao trustedDevicesDao) {
        this.trustedDevicesDao = trustedDevicesDao;
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
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request, ResultHandler<JsonValue> handler) {
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
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request, ResultHandler<Resource> handler) {

        try {
            List<JsonValue> devices = trustedDevicesDao.getDeviceProfiles(context);

            JsonValue toDelete = null;
            for (JsonValue device : devices) {
                if (resourceId.equals(device.get("uuid").asString())) {
                    toDelete = device;
                    break;
                }
            }

            if (toDelete == null) {
                handler.handleError(new NotFoundException("Trusted device, " + resourceId + ", not found."));
                return;
            }

            devices.remove(toDelete);

            trustedDevicesDao.saveDeviceProfiles(context, devices);

            handler.handleResult(new Resource(resourceId, toDelete.hashCode() + "", toDelete));
        } catch (InternalServerErrorException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("Not supported."));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {

        try {
            for (JsonValue profile : trustedDevicesDao.getDeviceProfiles(context)) {
                JsonValue lastSelectedDateJson = profile.get(LAST_SELECTED_DATE_KEY);
                Date lastSelectedDate;
                if (lastSelectedDateJson.isString()) {
                    lastSelectedDate = DATE_PARSER.parse(lastSelectedDateJson.asString());
                } else {
                    lastSelectedDate = new Date(lastSelectedDateJson.asLong());
                }
                profile.put(LAST_SELECTED_DATE_KEY, DATE_FORMATTER.format(lastSelectedDate));
                handler.handleResource(new Resource(profile.get("name").asString(), profile.hashCode() + "", profile));
            }

            handler.handleResult(new QueryResult());
        } catch (ParseException e) {
            handler.handleError(new InternalServerErrorException(e.getMessage()));
        } catch (InternalServerErrorException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("Not supported."));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("Not supported."));
    }
}
