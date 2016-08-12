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
 * Copyright 2013-2016 ForgeRock AS.
 */
package org.forgerock.openam.core.rest.cts;

import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.CORE_TOKEN_RESOURCE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.CREATE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DELETE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.READ_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.UPDATE_DESCRIPTION;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Create;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.Update;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import com.sun.identity.shared.debug.Debug;

/**
 * CoreTokenResource is responsible for exposing the functions of the CoreTokenService via a REST
 * interface to external callers.
 *
 * The objects passed via this interface will either be serialised JSON values of Tokens, or
 * Token ID values. This ensures that the REST interface has the same feel as the API.
 *
 */
@CollectionProvider(
        details = @Handler(
                title = CORE_TOKEN_RESOURCE + TITLE,
                description = CORE_TOKEN_RESOURCE + DESCRIPTION,
                resourceSchema = @Schema(fromType = Token.class),
                mvccSupported = false,
                parameters = {
                        @Parameter(
                                name = "tokenId",
                                type = "string",
                                description = CORE_TOKEN_RESOURCE + "pathParam." + DESCRIPTION)}))
public class CoreTokenResource {
    public static final String DEBUG_HEADER = "CoreTokenResource :: ";
    // Injected
    private final JSONSerialisation serialisation;
    private final CTSPersistentStore store;
    private final Debug debug;

    /**
     * On a return response, we need to describe the Token ID field in a consistent manner.
     * This way, the name of the value is independent of the LDAP attribute.
     */
    private static final String TOKEN_ID = "token.id";

    /**
     * Create a default instance of the interface with required dependencies.
     *
     * @param serialisation Required for deserialisation.
     * @param store Required to perform operations against the persistent store.
     */
    public CoreTokenResource(JSONSerialisation serialisation, CTSPersistentStore store, Debug debug) {
        this.serialisation = serialisation;
        this.store = store;
        this.debug = debug;
    }

    /**
     * Create a token in the store.
     *
     * This call expects a JSON serialised Token instance to be passed in via the CreateRequest.
     *
     * @param serverContext Required context.
     * @param createRequest Contains the serialised JSON value of the Token.
     */
    @Create(operationDescription = @Operation(
        errors = {
            @ApiError(
                code = 400,
                description = CORE_TOKEN_RESOURCE + "error.unexpected.bad.request." + DESCRIPTION),
            @ApiError(
                code = 500,
                description = CORE_TOKEN_RESOURCE + "error.unexpected.server.error." + DESCRIPTION)},
        description = CORE_TOKEN_RESOURCE + CREATE_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> createInstance(Context serverContext,
            CreateRequest createRequest) {
        String principal = PrincipalRestUtils.getPrincipalNameFromServerContext(serverContext);

        String json = createRequest.getContent().toString();
        Token token = serialisation.deserialise(json, Token.class);
        try {
            store.createAsync(token);

            Map<String, String> result = new HashMap<String, String>();
            result.put(TOKEN_ID, token.getTokenId());

            ResourceResponse resource = newResourceResponse(
                    token.getTokenId(),
                    String.valueOf(currentTimeMillis()),
                    new JsonValue(result));

            debug("CREATE by {0}: Stored token with ID: {1}", principal, token.getTokenId());
            return newResultPromise(resource);
        } catch (IllegalArgumentException e) {
            return new BadRequestException(e.getMessage()).asPromise();
        } catch (CoreTokenException e) {
            error(e, "CREATE by {0}: Error creating token resource with ID: {1}", principal, token.getTokenId());
            return generateException(e).asPromise();
        }
    }

    /**
     * Delete the instance referred to by its Resource path.
     *
     * @param serverContext Required context.
     * @param tokenId The TokenID of the token to delete.
     * @param deleteRequest Not used.
     */
    @Delete(operationDescription = @Operation(
        errors = {
                @ApiError(
                        code = 500,
                        description = CORE_TOKEN_RESOURCE + "error.unexpected.server.error." + DESCRIPTION)},
        description = CORE_TOKEN_RESOURCE + DELETE_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context serverContext, String tokenId,
            DeleteRequest deleteRequest) {

        String principal = PrincipalRestUtils.getPrincipalNameFromServerContext(serverContext);

        try {
            store.deleteAsync(tokenId);

            Map<String, String> result = new HashMap<String, String>();
            result.put(TOKEN_ID, tokenId);

            ResourceResponse resource = newResourceResponse(
                    tokenId,
                    String.valueOf(currentTimeMillis()),
                    new JsonValue(result));

            debug("DELETE by {0}: Deleted token resource with ID: {1}", principal, tokenId);
            return newResultPromise(resource);
        } catch (CoreTokenException e) {
            error(e, "DELETE by {0}: Error deleting token resource with ID: {1}", principal, tokenId);
            return generateException(e).asPromise();
        }
    }

    /**
     * Read the token based on its Token ID.
     *
     * If successful, the Token will be returned to the caller in serialised JSON format.
     *
     * @param serverContext Required context.
     * @param tokenId The TokenID of the Token to read.
     * @param readRequest Not used.
     */
    @Read(operationDescription = @Operation(
        errors = {
                @ApiError(
                        code = 500,
                        description = CORE_TOKEN_RESOURCE + "error.unexpected.server.error." + DESCRIPTION)},
            description = CORE_TOKEN_RESOURCE + READ_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> readInstance(Context serverContext, String tokenId,
            ReadRequest readRequest) {

        String principal = PrincipalRestUtils.getPrincipalNameFromServerContext(serverContext);

        try {
            Token token = store.read(tokenId);
            if (token == null) {
                error("READ by {0}: No token resource to read with ID: {1}", principal, tokenId);
                return generateNotFoundException(tokenId).asPromise();
            }

            String json = serialisation.serialise(token);
            ResourceResponse response = newResourceResponse(
                    tokenId,
                    String.valueOf(currentTimeMillis()),
                    JsonValueBuilder.toJsonValue(json));

            debug("READ by {0}: Read token resource with ID: {1}", principal, tokenId);
            return newResultPromise(response);
        } catch (CoreTokenException e) {
            error(e, "READ by {0}: Error reading token resource with ID: {1}", principal, tokenId);
            return generateException(e).asPromise();
        }
    }

    /**
     * Update a Token based on the Resource path.
     *
     * @param serverContext Required context.
     * @param tokenId The tokenId to update. This must be the same TokenId as the serialised Token.
     * @param updateRequest Contains the JSON serialised Token to update.
     */
    @Update(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = CORE_TOKEN_RESOURCE + "error.unexpected.server.error." + DESCRIPTION)},
            description = CORE_TOKEN_RESOURCE + UPDATE_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> updateInstance(Context serverContext, String tokenId,
            UpdateRequest updateRequest) {
        String principal = PrincipalRestUtils.getPrincipalNameFromServerContext(serverContext);

        String value = updateRequest.getContent().toString();
        Token newToken = serialisation.deserialise(value, Token.class);

        try {
            store.updateAsync(newToken);

            ResourceResponse resource = newResourceResponse(
                    newToken.getTokenId(),
                    String.valueOf(currentTimeMillis()),
                    new JsonValue("Token Updated"));

            debug("UPDATE by {0}: Updated token resource with ID: {1}", principal, tokenId);
            return newResultPromise(resource);
        } catch (CoreTokenException e) {
            error(e, "UPDATE by {0}: Error updating token resource with ID: {1}", principal, tokenId);
            return generateException(e).asPromise();
        }
    }

    /**
     * Hand generic errors which are non-recoverable.
     *
     * @param e Error to be handled.
     * @return Non null ResourceException.
     */
    private ResourceException generateException(CoreTokenException e) {
        return new InternalServerErrorException(e.getMessage(), e);
    }

    /**
     * Handle the non recoverable error of the Token not found.
     * @param tokenId The TokenId that could not be found.
     * @return Non null ResourceException.
     */
    private ResourceException generateNotFoundException(String tokenId) {
        return new NotFoundException("Token " + tokenId + " not found");
    }

    private void debug(String format, Object... args) {
        if (debug.messageEnabled()) {
            debug.message(DEBUG_HEADER + MessageFormat.format(format, args));
        }
    }

    private void error(Exception e, String format, Object... args) {
        if (debug.errorEnabled()) {
            debug.error(DEBUG_HEADER + MessageFormat.format(format, args), e);
        }
    }

    private void error(String format, Object... args) {
        error(null, format, args);
    }
}
