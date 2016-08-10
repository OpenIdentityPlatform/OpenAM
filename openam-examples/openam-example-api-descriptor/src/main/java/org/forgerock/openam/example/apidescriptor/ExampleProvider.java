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
package org.forgerock.openam.example.apidescriptor;

import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.*;

import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Create;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.Update;
import org.forgerock.api.enums.QueryType;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

@CollectionProvider(details = @Handler(
        title = EXAMPLE_PROVIDER + TITLE,
        description = EXAMPLE_PROVIDER + DESCRIPTION,
        mvccSupported = false,
        resourceSchema = @Schema(schemaResource = "ExampleProvider.resource.schema.json")))
public class ExampleProvider {

    @Create(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 400,
                            description = EXAMPLE_PROVIDER + CREATE + ERROR_400_DESCRIPTION
                    )},
            description = EXAMPLE_PROVIDER + DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> create(Context context, CreateRequest request) {
        return null;
    }

    @Read(operationDescription = @Operation(description = EXAMPLE_PROVIDER + DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> read(Context context, String id, ReadRequest request) {
        return null;
    }

    @Update(operationDescription = @Operation(description = EXAMPLE_PROVIDER + DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> update(Context context, String id, UpdateRequest request) {
        return null;
    }

    @Delete(operationDescription = @Operation(description = EXAMPLE_PROVIDER + DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> delete(Context context, String id, DeleteRequest request) {
        return null;
    }

    @Query(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = EXAMPLE_PROVIDER + QUERY + ERROR_500_DESCRIPTION
                    )},
            description = EXAMPLE_PROVIDER + DESCRIPTION),
            type = QueryType.FILTER,
            queryableFields = "*")
    public Promise<QueryResponse, ResourceException> query(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        return null;
    }
}
