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

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.forgerock.http.Client;
import org.forgerock.http.protocol.Header;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;

import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.log.Level;

/**
 * Exports policy model resources into a JSON file.
 *
 * @since 14.0.0
 */
public final class PolicyExport extends JsonResourceCommand {

    private static final String RESOURCES_REFERENCE = "resources";
    private static final String VERSION_REFERENCE = "version";

    /**
     * Constructs a new json resource command.
     *
     * @param client
     *         the http client
     */
    @Inject
    protected PolicyExport(Client client) {
        super(client);
    }

    @Override
    public void handleResourceRequest(RequestContext request) throws CLIException {
        String realm = getStringOptionValue(IArgument.REALM_NAME);
        String outfile = getStringOptionValue(IArgument.JSON_FILE);

        JsonValue exportPayload = json(object());

        Map<ModelDescriptor, Integer> exportMetrics = new HashMap<>();

        for (ModelDescriptor modelDescriptor : ModelDescriptor.getPrecedentOrder()) {
            int count = exportResourceToPayload(modelDescriptor, exportPayload);
            exportMetrics.put(modelDescriptor, count);
        }

        writeJsonFile(exportPayload, outfile);

        writeLog(LogWriter.LOG_ACCESS, Level.LL_INFO, "POLICY_EXPORT_SUCCESS", realm, outfile);

        getOutputWriter().printlnMessage(objectToJsonString(exportMetrics));

    }

    /*
     * @return number of resources set to the export payload.
     */
    private int exportResourceToPayload(ModelDescriptor modelDescriptor, JsonValue exportPayload) throws CLIException {
        try (Response response = queryForAllResources(modelDescriptor)) {
            JsonValue resources = extractResources(response);

            List<Object> resourceObjects = new ArrayList<>();
            int countResources = 0;

            for (JsonValue resource : resources) {

                if (modelDescriptor.isExcludedResource(resource)) {
                    continue;
                }

                resourceObjects.add(resource.getObject());
                countResources++;
            }

            if (resources.size() == 0) {
                return countResources;
            }

            String version = extractResourceVersion(response);

            Map<String, Object> exportEntry = new HashMap<>();
            exportEntry.put(RESOURCES_REFERENCE, resourceObjects);
            exportEntry.put(VERSION_REFERENCE, version);

            exportPayload.add(modelDescriptor.getEndpointIdentifier(), exportEntry);

            return countResources;
        }
    }

    private Response queryForAllResources(ModelDescriptor endpoint) throws CLIException {
        Request readRequest = new Request().setMethod("GET");
        String queryEndpoint = endpoint.getEndpointIdentifier() + "?_queryFilter=true";

        return sendRequest(readRequest, queryEndpoint);
    }

    private JsonValue extractResources(Response queryResponse) throws CLIException {
        try {
            JsonValue queryJson = json(queryResponse.getEntity().getJson());
            return queryJson.get("result");
        } catch (IOException e) {
            throw new CLIException(e, ExitCodes.IO_EXCEPTION);
        }
    }

    private String extractResourceVersion(Response queryResponse) throws CLIException {
        Header versionHeader = queryResponse.getHeaders().get("Content-API-Version");

        for (String version : versionHeader.getValues()) {
            int index = version.indexOf("resource=");

            if (index == -1) {
                continue;
            }

            return version.substring(index + "resource=".length());
        }

        throw new CLIException("Unable to identify version", ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
    }

}
