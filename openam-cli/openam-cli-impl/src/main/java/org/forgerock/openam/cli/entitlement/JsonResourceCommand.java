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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.RequestContext;
import org.forgerock.http.Client;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Abstract class for commands wanting to interact with JSON resources via REST.
 *
 * @since 14.0.0
 */
abstract class JsonResourceCommand extends AuthenticatedCommand {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Client client;

    /**
     * Constructs a new json resource command.
     *
     * @param client
     *         the http client
     */
    protected JsonResourceCommand(Client client) {
        this.client = client;
    }

    @Override
    public final void handleRequest(RequestContext request) throws CLIException {
        super.handleRequest(request);
        ldapLogin();
        handleResourceRequest(request);
    }

    /**
     * Handles the passed request.
     *
     * @param request
     *         the command request
     *
     * @throws CLIException
     *         should some error occur
     */
    protected abstract void handleResourceRequest(RequestContext request) throws CLIException;

    /**
     * Sends the given request to the passed endpoint.
     *
     * @param request
     *         http request
     * @param endpoint
     *         endpoint
     *
     * @return http response
     *
     * @throws CLIException
     *         should some error occur
     */
    protected final Response sendRequest(Request request, String endpoint) throws CLIException {
        String serverUrl = getStringOptionValue(IArgument.SERVER_NAME);
        String realm = getStringOptionValue(IArgument.REALM_NAME);

        if (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }

        if (endpoint.startsWith("/")) {
            endpoint = endpoint.substring(1);
        }

        StringBuilder restUrl = new StringBuilder(serverUrl).append("/json/").append(endpoint);
        restUrl.append(restUrl.indexOf("?") == -1 ? '?' : '&').append("realm=").append(realm);

        try {
            request.setUri(restUrl.toString());
        } catch (URISyntaxException urisE) {
            throw new CLIException(urisE, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        String ssoToken = getAdminSSOToken().getTokenID().toString();
        request.getHeaders().put("iplanetDirectoryPro", ssoToken);
        request.getHeaders().put("Content-Type", "application/json");
        Promise<Response, NeverThrowsException> promise = client.send(request);
        return promise.getOrThrowUninterruptibly();
    }

    /**
     * Reads in a json file.
     *
     * @param jsonFile
     *         the path to the json file
     *
     * @return the json representation
     *
     * @throws CLIException
     *         should some error occur
     */
    protected final JsonValue readJsonFile(String jsonFile) throws CLIException {
        try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {

            StringBuilder contents = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                contents.append(line).append('\n');
            }

            return new JsonValue(MAPPER.readValue(contents.toString(), Map.class));
        } catch (IOException ioE) {
            throw new CLIException(ioE, ExitCodes.IO_EXCEPTION);
        }
    }

    /**
     * Writes out json to file.
     *
     * @param jsonValue
     *         the json to be written out
     * @param jsonFile
     *         the path to the json file
     *
     * @throws CLIException
     *         should some error occur
     */
    protected final void writeJsonFile(JsonValue jsonValue, String jsonFile) throws CLIException {
        try (FileWriter fileWriter = new FileWriter(jsonFile)) {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(fileWriter, jsonValue.getObject());
        } catch (IOException ioE) {
            throw new CLIException(ioE, ExitCodes.IO_EXCEPTION);
        }
    }

    /**
     * Given an object, converts it to a well formatted json representation.
     *
     * @param object
     *         object
     *
     * @return json representation
     *
     * @throws CLIException
     *         should some error occur during the transformation to json
     */
    protected final String objectToJsonString(Object object) throws CLIException {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException jpE) {
            throw new CLIException(jpE, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

}
