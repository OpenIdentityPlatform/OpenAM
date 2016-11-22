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

import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.*;
import static org.forgerock.openam.utils.Time.*;
import static org.forgerock.util.promise.Promises.*;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Create;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.Update;
import org.forgerock.api.enums.QueryType;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.filter.CTSQueryFilterVisitor;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;

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
                mvccSupported = false),
        pathParam = @Parameter(
                name = "tokenId",
                type = "string",
                description = CORE_TOKEN_RESOURCE + PATH_PARAM + DESCRIPTION))
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
            store.create(token);

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
            store.delete(tokenId);

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
            store.update(newToken);

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

    @Query(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = CORE_TOKEN_RESOURCE + "error.unexpected.server.error." + DESCRIPTION)},
            description = CORE_TOKEN_RESOURCE + QUERY_DESCRIPTION),
            type = QueryType.FILTER
    )
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
                                                                     QueryResourceHandler handler) {

        QueryFilter<JsonPointer> crestQueryFilter = request.getQueryFilter();
        Reject.ifNull(crestQueryFilter, "Query Filter must be specified in the request");
        QueryFilter<CoreTokenField> queryFilter;
        try {
            queryFilter = crestQueryFilter.accept(new CTSQueryFilterVisitor(), null);
        } catch (IllegalArgumentException e) {
            return new BadRequestException(e.getMessage()).asPromise();
        }

        TokenFilter tokenFilter = new TokenFilterBuilder().withQuery(queryFilter).build();

        try {
            Collection<Token> tokens = store.query(tokenFilter);

            for (Token token : tokens) {
                String json = serialisation.serialise(token);
                ResourceResponse resource = newResourceResponse(
                        token.getTokenId(),
                        String.valueOf(currentTimeMillis()),
                        JsonValueBuilder.toJsonValue(json));
                handler.handleResource(resource);
            }
        } catch (CoreTokenException e) {
            error(e, "QUERY: Error querying CTS with filter {0}", tokenFilter);
            return generateException(e).asPromise();
        }

        return newResultPromise(newQueryResponse());
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
