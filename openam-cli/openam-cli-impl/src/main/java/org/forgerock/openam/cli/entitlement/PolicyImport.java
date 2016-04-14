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

package org.forgerock.openam.cli.entitlement;

import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.log.Level;
import org.forgerock.http.Client;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;

import javax.inject.Inject;

/**
 * Given a json file containing the policy model export, attempts to create or update policy model resources.
 *
 * @since 14.0.0
 */
public final class PolicyImport extends JsonResourceCommand {

    private static final String RESOURCES_REFERENCE = "resources";
    private static final String VERSION_REFERENCE = "version";

    /**
     * Constructs a new json resource command.
     *
     * @param client
     *         the http client
     */
    @Inject
    public PolicyImport(Client client) {
        super(client);
    }

    @Override
    public void handleResourceRequest(RequestContext request) throws CLIException {
        String jsonFile = getStringOptionValue(IArgument.FILE);
        JsonValue importPayload = readJsonFile(jsonFile);

        for (ModelDescriptor modelDescriptor : ModelDescriptor.getPrecedentOrder()) {
            importResources(importPayload, modelDescriptor);
        }
    }

    private void importResources(JsonValue importPayload, ModelDescriptor modelDescriptor) throws CLIException {
        if (!importPayload.isDefined(modelDescriptor.getEndpointIdentifier())) {
            return;
        }

        JsonValue resourcePayload = importPayload.get(modelDescriptor.getEndpointIdentifier());

        if (!resourcePayload.isDefined(RESOURCES_REFERENCE)) {
            return;
        }

        String version = null;

        if (resourcePayload.isDefined(VERSION_REFERENCE)) {
            version = resourcePayload.get(VERSION_REFERENCE)
                    .asString();
        }

        JsonValue resources = resourcePayload.get(RESOURCES_REFERENCE);

        for (JsonValue resource : resources) {
            updateOrCreateResource(resource, version, modelDescriptor);
        }
    }

    private void updateOrCreateResource(JsonValue resource, String version,
            ModelDescriptor modelDescriptor) throws CLIException {

        String id = resource.get(modelDescriptor.getIdField())
                .required()
                .asString();
        Request readRequest = new Request()
                .setMethod("GET");

        String readEndpoint = modelDescriptor.getEndpointIdentifier() + "/" + id;
        Response response = sendRequest(readRequest, readEndpoint);

        if (response.getStatus() == Status.OK) {
            updateResource(id, version, resource, modelDescriptor);
        } else if (response.getStatus() == Status.NOT_FOUND) {
            createResource(id, version, resource, modelDescriptor);
        } else {
            String httpCode = String.valueOf(response.getStatus().getCode());
            writeLog(LogWriter.LOG_ERROR, Level.LL_INFO,
                    "RESOURCE_READ_FAILED", id, modelDescriptor.name(), httpCode);
        }
    }

    private void updateResource(String id, String version, JsonValue resource,
            ModelDescriptor modelDescriptor) throws CLIException {

        Request request = new Request()
                .setMethod("PUT");
        request.getEntity()
                .setJson(resource.getObject());

        if (version != null) {
            request.getHeaders()
                    .put("Accept-API-Version", "protocol=1.0,resource=" + version);
        }

        String updateEndpoint = modelDescriptor.getEndpointIdentifier() + "/" + id;
        Response response = sendRequest(request, updateEndpoint);

        if (response.getStatus().isSuccessful()) {
            writeLog(LogWriter.LOG_ACCESS, Level.LL_INFO,
                    "RESOURCE_UPDATE_SUCCESS", id, modelDescriptor.name());
        } else {
            String httpCode = String.valueOf(response.getStatus().getCode());
            writeLog(LogWriter.LOG_ERROR, Level.LL_INFO,
                    "RESOURCE_UPDATE_FAILED", id, modelDescriptor.name(), httpCode);
        }
    }

    private void createResource(String id, String version, JsonValue resource,
            ModelDescriptor modelDescriptor) throws CLIException {

        Request request = new Request()
                .setMethod("POST");
        request.getEntity()
                .setJson(resource.getObject());

        if (version != null) {
            request.getHeaders()
                    .put("Accept-API-Version", "protocol=1.0,resource=" + version);
        }

        String createEndpoint = modelDescriptor.getEndpointIdentifier() + "?_action=create";
        Response response = sendRequest(request, createEndpoint);

        if (response.getStatus().isSuccessful()) {
            writeLog(LogWriter.LOG_ACCESS, Level.LL_INFO,
                    "RESOURCE_CREATE_SUCCESS", id, modelDescriptor.name());
        } else {
            String httpCode = String.valueOf(response.getStatus().getCode());
            writeLog(LogWriter.LOG_ERROR, Level.LL_INFO,
                    "RESOURCE_CREATE_FAILED", id, modelDescriptor.name(), httpCode);
        }
    }

}
