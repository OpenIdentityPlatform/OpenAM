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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.uma.rest;

import static org.forgerock.json.JsonValueFunctions.enumConstant;
import static org.forgerock.json.resource.ResourceException.*;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.json.resource.http.HttpUtils.PROTOCOL_VERSION_1;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.*;
import static org.forgerock.openam.rest.RestUtils.crestProtocolVersion;
import static org.forgerock.openam.rest.RestUtils.isContractConformantUserProvidedIdCreate;
import static org.forgerock.util.promise.Promises.newResultPromise;

import com.sun.identity.common.LocaleContext;
import com.sun.identity.shared.debug.Debug;
import java.util.Collections;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Create;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.enums.QueryType;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.openam.oauth2.resources.labels.LabelType;
import org.forgerock.openam.oauth2.resources.labels.ResourceSetLabel;
import org.forgerock.openam.oauth2.resources.labels.UmaLabelsStore;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * A collection provider for UMA Labels.
 * @Since 13.0.0
 */
@CollectionProvider(
        details = @Handler(
                mvccSupported = true,
                title = UMA_LABEL_RESOURCE + TITLE,
                description =  UMA_LABEL_RESOURCE + DESCRIPTION,
                resourceSchema = @Schema(fromType = ResourceSetLabel.class),
                parameters = {
                        @Parameter(
                                name = "user",
                                type = "string",
                                description = UMA_LABEL_RESOURCE + "pathparams.user")}),
        pathParam = @Parameter(
                name = "umaLabelId",
                type = "string",
                description = UMA_LABEL_RESOURCE + PATH_PARAM + DESCRIPTION))
public class UmaLabelResource {
    private static final Debug debug = Debug.getInstance("umaLabel");
    private static final String TYPE_LABEL = "type";
    private static final String NAME_LABEL = "name";
    private final UmaLabelsStore labelStore;
    private final ContextHelper contextHelper;
    private final ClientRegistrationStore clientRegistrationStore;
    private final Provider<LocaleContext> localeContextProvider;

    @Inject
    public UmaLabelResource(UmaLabelsStore labelStore, ContextHelper contextHelper,
            ClientRegistrationStore clientRegistrationStore, Provider<LocaleContext> localeContextProvider) {
        this.labelStore = labelStore;
        this.contextHelper = contextHelper;
        this.clientRegistrationStore = clientRegistrationStore;
        this.localeContextProvider = localeContextProvider;
    }

    @Create(operationDescription =
    @Operation(
            description = UMA_LABEL_RESOURCE + CREATE_DESCRIPTION,
            errors = {
                    @ApiError(
                            code = INTERNAL_ERROR,
                            description = UMA_LABEL_RESOURCE + CREATE + ERROR_500_DESCRIPTION),
                    @ApiError(
                            code = CONFLICT,
                            description = UMA_LABEL_RESOURCE + CREATE + ERROR_409_DESCRIPTION)}))
    public Promise<ResourceResponse, ResourceException> createInstance(Context serverContext,
            CreateRequest createRequest) {
        final JsonValue umaLabel = createRequest.getContent();

        try {
            validate(umaLabel);
        } catch (BadRequestException e) {
            return e.asPromise();
        }

        final String realm = getRealm(serverContext);
        final String userName = getUserName(serverContext);
        final String labelName = umaLabel.get(NAME_LABEL).asString();
        final String labelType = umaLabel.get(TYPE_LABEL).asString();
        final ResourceSetLabel label;

        try {
            label = labelStore.create(realm, userName, new ResourceSetLabel(null, labelName, LabelType.valueOf(labelType), Collections.EMPTY_SET));
            return newResultPromise(newResourceResponse(label.getId(), String.valueOf(label.hashCode()), label.asJson()));
        } catch (ConflictException e) {
            if (isContractConformantUserProvidedIdCreate(serverContext, createRequest)) {
                return new PreconditionFailedException(e.getMessage()).asPromise();
            }
            return e.asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    private void validate(JsonValue umaLabel) throws BadRequestException {
        try {
            umaLabel.get(TYPE_LABEL).required();
            umaLabel.get(TYPE_LABEL).as(enumConstant(LabelType.class));
            umaLabel.get(NAME_LABEL).required();
        } catch (JsonValueException e) {
            debug.error("Invalid Json - " + e.getMessage());
            throw new BadRequestException("Invalid Json - " + e.getMessage());
        }
    }

    @Delete(operationDescription = @Operation(
            description = UMA_LABEL_RESOURCE + DELETE_DESCRIPTION,
            errors = {@ApiError(
                    code = BAD_REQUEST,
                    description = UMA_LABEL_RESOURCE + DELETE + ERROR_400_DESCRIPTION)}))
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context serverContext, String labelId,
            DeleteRequest deleteRequest) {
        try {
            ResourceSetLabel resourceSetLabel = labelStore.read(getRealm(serverContext), getUserName(serverContext), labelId);

            if (!isSameRevision(deleteRequest, resourceSetLabel)) {
                throw new BadRequestException("Revision number doesn't match latest revision.");
            }

            labelStore.delete(getRealm(serverContext), getUserName(serverContext), labelId);
            return newResultPromise(newResourceResponse(labelId, null, resourceSetLabel.asJson()));
        } catch (ResourceException e) {
            return new BadRequestException("Error deleting label.").asPromise();
        }
    }

    private boolean isSameRevision(DeleteRequest deleteRequest, ResourceSetLabel resourceSetLabel) {
        final String revision = deleteRequest.getRevision();

        if (revision == null || revision.equals("*")) {
            return true;
        }

        return revision.equals(String.valueOf(resourceSetLabel.hashCode()));
    }

    @Query(operationDescription = @Operation(
            description = UMA_LABEL_RESOURCE + QUERY_DESCRIPTION),
            type = QueryType.FILTER,
            queryableFields = "*")
    public Promise<QueryResponse, ResourceException> queryCollection(Context serverContext,
            QueryRequest queryRequest, QueryResourceHandler queryResultHandler) {
        if (!queryRequest.getQueryFilter().toString().equals("true")) {
            return new BadRequestException("Invalid query").asPromise();
        }

        Set<ResourceSetLabel> labels;
        try {
            labels = labelStore.list(getRealm(serverContext), getUserName(serverContext));
        } catch (ResourceException e) {
            return new BadRequestException("Error retrieving labels.").asPromise();
        }

        LocaleContext localeContext = localeContextProvider.get();
        localeContext.setLocale(serverContext);

        for (ResourceSetLabel label : labels) {
            try {
                label = resolveLabelName(contextHelper.getRealm(serverContext), label, localeContext, serverContext);
            } catch (InternalServerErrorException e) {
                debug.error("Could not resolve Resource Server label name. id: {}, name: {}", label.getId(),
                        label.getName(), e);
            }
            queryResultHandler.handleResource(newResourceResponse(label.getId(),
                    String.valueOf(label.asJson().getObject().hashCode()), label.asJson()));
        }

        return newResultPromise(newQueryResponse());
    }

    private ResourceSetLabel resolveLabelName(String realm, ResourceSetLabel label, LocaleContext localeContext,
                                              Context serverContext)
            throws InternalServerErrorException {
        if (label.getId().endsWith("/" + label.getName())) {
            String resourceServerId = label.getId().substring(0, label.getId().lastIndexOf("/"));
            String resourceServerName = resolveResourceServerName(resourceServerId, realm, localeContext, serverContext);
            if (resourceServerName != null) {
                label.setName(resourceServerName + "/" + label.getName());
            }
        }
        return label;
    }

    private String resolveResourceServerName(String resourceServerId, final String realm, LocaleContext
            localeContext, Context serverContext) throws InternalServerErrorException {
        try {
            ClientRegistration clientRegistration = clientRegistrationStore.get(resourceServerId, realm, serverContext);
            return clientRegistration.getDisplayName(localeContext.getLocale());
        } catch (InvalidClientException | NotFoundException e) {
            throw new InternalServerErrorException("Could not resolve Resource Server label name", e);
        }
    }

    private String getRealm(Context context) {
        return contextHelper.getRealm(context);
    }

    private String getUserName(Context context) {
        return contextHelper.getUserId(context);
    }
}
