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
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openam.cli.entitlement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.forgerock.http.Client;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.util.Uris;
import org.forgerock.json.JsonValue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.log.Level;

/**
 * Given a json file containing the policy model export, attempts to create or update policy model resources.
 *
 * @since 14.0.0
 */
public final class PolicyImport extends JsonResourceCommand {

    private enum ResourceEvent {

        UPDATE_SUCCESS, UPDATE_FAILURE, CREATE_SUCCESS, CREATE_FAILURE, READ_FAILURE;

        private static final EnumSet<ResourceEvent> DETAILED_EVENTS =
                EnumSet.of(UPDATE_FAILURE, CREATE_FAILURE, READ_FAILURE);

        static boolean shouldReportDetails(ResourceEvent resourceEvent) {
            return DETAILED_EVENTS.contains(resourceEvent);
        }

    }

    private static final String RESOURCES_REFERENCE = "resources";
    private static final String VERSION_REFERENCE = "version";

    private final Map<ModelDescriptor, Map<ResourceEvent, EventRecord>> eventLogs;

    /**
     * Constructs a new json resource command.
     *
     * @param client
     *         the http client
     */
    @Inject
    public PolicyImport(Client client) {
        super(client);
        eventLogs = new HashMap<>();
    }

    @Override
    public void handleResourceRequest(RequestContext request) throws CLIException {
        try {
            String jsonFile = getStringOptionValue(IArgument.JSON_FILE);
            JsonValue importPayload = readJsonFile(jsonFile);

            for (ModelDescriptor modelDescriptor : ModelDescriptor.getPrecedentOrder()) {
                importResources(importPayload, modelDescriptor);
            }
        } finally {
            printReport();
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

        String encodedId = Uris.urlEncodePathElement(id);
        String readEndpoint = modelDescriptor.getEndpointIdentifier() + "/" + encodedId;

        try (Response response = sendRequest(readRequest, readEndpoint)) {
            if (response.getStatus() == Status.OK) {
                updateResource(id, version, resource, modelDescriptor);
            } else if (response.getStatus() == Status.NOT_FOUND) {
                createResource(id, version, resource, modelDescriptor);
            } else {
                logResourceEvent(id, response, modelDescriptor, ResourceEvent.READ_FAILURE);
                String httpCode = String.valueOf(response.getStatus().getCode());
                auditEvent(LogWriter.LOG_ERROR, "RESOURCE_READ_FAILED", id, modelDescriptor.name(), httpCode);
            }
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

        String encodedId = Uris.urlEncodePathElement(id);
        String updateEndpoint = modelDescriptor.getEndpointIdentifier() + "/" + encodedId;

        try (Response response = sendRequest(request, updateEndpoint)) {
            if (response.getStatus().isSuccessful()) {
                logResourceEvent(id, response, modelDescriptor, ResourceEvent.UPDATE_SUCCESS);
                auditEvent(LogWriter.LOG_ACCESS, "RESOURCE_UPDATE_SUCCESS", id, modelDescriptor.name());
            } else {
                logResourceEvent(id, response, modelDescriptor, ResourceEvent.UPDATE_FAILURE);
                String httpCode = String.valueOf(response.getStatus().getCode());
                auditEvent(LogWriter.LOG_ERROR, "RESOURCE_UPDATE_FAILED", id, modelDescriptor.name(), httpCode);
            }
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

        try (Response response = sendRequest(request, createEndpoint)) {
            if (response.getStatus().isSuccessful()) {
                logResourceEvent(id, response, modelDescriptor, ResourceEvent.CREATE_SUCCESS);
                auditEvent(LogWriter.LOG_ACCESS, "RESOURCE_CREATE_SUCCESS", id, modelDescriptor.name());
            } else {
                logResourceEvent(id, response, modelDescriptor, ResourceEvent.CREATE_FAILURE);
                String httpCode = String.valueOf(response.getStatus().getCode());
                auditEvent(LogWriter.LOG_ERROR, "RESOURCE_CREATE_FAILED", id, modelDescriptor.name(), httpCode);
            }
        }
    }

    private void logResourceEvent(String id, Response response,
            ModelDescriptor modelDescriptor, ResourceEvent resourceEvent) {

        if (!eventLogs.containsKey(modelDescriptor)) {
            eventLogs.put(modelDescriptor, new HashMap<ResourceEvent, EventRecord>());
        }

        Map<ResourceEvent, EventRecord> eventRecords = eventLogs.get(modelDescriptor);

        if (!eventRecords.containsKey(resourceEvent)) {
            if (ResourceEvent.shouldReportDetails(resourceEvent)) {
                eventRecords.put(resourceEvent, new DetailedEventRecord());
            } else {
                eventRecords.put(resourceEvent, new AccumulativeCountEventRecord());
            }
        }

        Map<String, Object> eventDetails = new HashMap<>();
        eventDetails.put("id", id);

        if (response.getStatus() != null) {
            eventDetails.put("httpCode", response.getStatus().getCode());
        }

        if (response.getCause() != null) {
            eventDetails.put("errorMessage", response.getCause().getMessage());
        }

        if (response.getEntity() != null) {
            try {
                eventDetails.put("responseBody", response.getEntity().getJson());
            } catch (IOException ioE) {
                // Non-critical, do nothing.
            }
        }

        eventRecords.get(resourceEvent).log(eventDetails);
    }

    private void printReport() throws CLIException {
        getOutputWriter().printMessage(objectToJsonString(eventLogs));
    }

    private void auditEvent(int eventType, String messageId, String... args) throws CLIException {
        writeLog(eventType, Level.INFO, messageId, args);
    }

    private interface EventRecord {

        void log(Map<String, Object> eventDetails);

    }

    private static class AccumulativeCountEventRecord implements EventRecord {

        @JsonProperty("count")
        private int accumulativeCount;

        @Override
        public void log(Map<String, Object> eventDetails) {
            accumulativeCount++;
        }

    }

    private static final class DetailedEventRecord extends AccumulativeCountEventRecord {

        @JsonProperty("details")
        private final List<Map<String, Object>> details;

        DetailedEventRecord() {
            details = new ArrayList<>();
        }

        @Override
        public void log(Map<String, Object> eventDetails) {
            super.log(eventDetails);
            details.add(eventDetails);
        }

    }

}
