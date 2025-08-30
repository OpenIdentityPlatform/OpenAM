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
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.core.rest.sms;

import static com.sun.identity.common.configuration.ServerConfiguration.cloneServerInstance;
import static com.sun.identity.common.configuration.ServerConfiguration.createServerInstance;
import static com.sun.identity.common.configuration.ServerConfiguration.deleteServerInstance;
import static com.sun.identity.common.configuration.ServerConfiguration.getServerConfigXML;
import static com.sun.identity.common.configuration.ServerConfiguration.getServerID;
import static com.sun.identity.common.configuration.ServerConfiguration.getServerSite;
import static com.sun.identity.common.configuration.ServerConfiguration.getServers;
import static com.sun.identity.common.configuration.ServerConfiguration.hasServerOrSiteId;
import static com.sun.identity.common.configuration.ServerConfiguration.isServerInstanceExist;
import static java.util.Collections.emptySet;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.json.resource.http.HttpUtils.PROTOCOL_VERSION_1;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DELETE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DELETE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_400_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_401_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_405_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_500_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATH_PARAM;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.QUERY;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.QUERY_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.READ_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SERVERS_RESOURCE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.openam.rest.RestUtils.crestProtocolVersion;
import static org.forgerock.openam.rest.RestUtils.isContractConformantUserProvidedIdCreate;
import static org.forgerock.openam.utils.StringUtils.isEmpty;
import static org.forgerock.openam.utils.StringUtils.isNotEmpty;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.Set;
import java.util.TreeSet;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.enums.QueryType;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.common.configuration.UnknownPropertyNameException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;

/**
 * Resource provider that deals with server instances.
 *
 * Since 14.0.0
 */
@CollectionProvider(
        details = @Handler(
                title = SERVERS_RESOURCE + TITLE,
                description = SERVERS_RESOURCE + DESCRIPTION,
                resourceSchema = @Schema(schemaResource = "ServersResource.schema.json"),
                mvccSupported = false),
        pathParam = @Parameter(
                name = "id",
                type = "string",
                description = SERVERS_RESOURCE + PATH_PARAM + DESCRIPTION))

public final class ServersResourceProvider {

    private final Debug debug;

    @Inject
    public ServersResourceProvider(@Named("frRest") Debug debug) {
        this.debug = debug;
    }

    @Action(name = "create",
            operationDescription = @Operation(
                    description = SERVERS_RESOURCE + "action.create." + DESCRIPTION
            ),
            request = @Schema(schemaResource = "ServersResource.create.request.schema.json"),
            response = @Schema(schemaResource = "ServersResource.schema.json"))
    public Promise<ResourceResponse, ResourceException> create(Context context, CreateRequest request) {
        JsonValue content = request.getContent();
        String id = request.getNewResourceId();
        try {
            id = validWriteOperation(content, id);
        } catch (BadRequestException e) {
            return e.asPromise();
        }

        String url = content.get("url").asString();
        try {
            SSOToken token = getSsoToken(context);
            if (hasServerOrSiteId(token, id)) {
                throw new ConflictException("Server with ID already exists: " + id);
            }
            if (isServerInstanceExist(token, url)) {
                return new ConflictException("Server with URL already exists: " + url).asPromise();
            }

            String svrConfigXML = getServerConfigXML(token, SystemProperties.getServerInstanceName());
            if (id == null) {
                createServerInstance(token, url, emptySet(), svrConfigXML);
            } else {
                createServerInstance(token, url, id, emptySet(), svrConfigXML);
            }
            return newResultPromise(getServer(token, url));
        } catch (SSOException | SMSException e) {
            debug.error("Could not create server", e);
            return new InternalServerErrorException("Could not create server").asPromise();
        } catch (NotFoundException e) {
            debug.error("Could not read server", e);
            return new InternalServerErrorException("Could not read server just created").asPromise();
        } catch (ConflictException e) {
            if (isContractConformantUserProvidedIdCreate(context, request)) {
                return new PreconditionFailedException(e.getMessage()).asPromise();
            }
            return e.asPromise();
        } catch (UnknownPropertyNameException | ConfigurationException e) {
           return new BadRequestException(e).asPromise();
        }
    }

    @Delete(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 400,
                            description = SERVERS_RESOURCE + DELETE + ERROR_400_DESCRIPTION
                    ),
                    @ApiError(
                            code = 401,
                            description = SERVERS_RESOURCE + DELETE + ERROR_401_DESCRIPTION
                    ),
                    @ApiError(
                            code = 500,
                            description = SERVERS_RESOURCE + ERROR_500_DESCRIPTION
                    )},
            description = SERVERS_RESOURCE + DELETE_DESCRIPTION
    ))
    public Promise<ResourceResponse, ResourceException> delete(Context context, String id) {
        try {
            SSOToken token = getSsoToken(context);
            deleteServerInstance(token, getServerUrl(token, id));
            return newResultPromise(newResourceResponse(id, null, json(object())));
        } catch (SMSException e) {
            debug.error("Error reading server from SMS: {}", id, e);
            return new InternalServerErrorException("Error reading SMS", e).asPromise();
        } catch (SSOException e) {
            return new PermanentException(401, "Invalid ssoToken", e).asPromise();
        } catch (NotFoundException e) {
            return new NotFoundException("Cannot find server ID: " + id).asPromise();
        }
    }

    @Query(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 400,
                            description = SERVERS_RESOURCE + QUERY + ERROR_400_DESCRIPTION
                    ),
                    @ApiError(
                            code = 405,
                            description = SERVERS_RESOURCE + QUERY + ERROR_405_DESCRIPTION
                    ),
                    @ApiError(
                            code = 500,
                            description = SERVERS_RESOURCE + ERROR_500_DESCRIPTION
                    )},
            description = SERVERS_RESOURCE + QUERY_DESCRIPTION),
            type = QueryType.FILTER,
            queryableFields = "*")
    public Promise<QueryResponse, ResourceException> query(Context context, QueryRequest request,
            QueryResourceHandler handler) {

        if (!"true".equals(request.getQueryFilter().toString())) {
            return new BadRequestException("Query only supports 'true' filter").asPromise();
        }
        try {
            SSOToken token = getSsoToken(context);
            Set<String> serverUrls = new TreeSet<>(getServers(token));

            for (String serverUrl : serverUrls) {
                handler.handleResource(getServer(token, serverUrl));
            }

            return newResultPromise(newQueryResponse());
        } catch (SSOException | SMSException e) {
            debug.error("Could not read servers", e);
            return new InternalServerErrorException("Could not read servers").asPromise();
        } catch (NotFoundException e) {
            debug.error("Could not read servers", e);
            return new InternalServerErrorException("Could not read server we've just got name for").asPromise();
        }
    }

    @Read(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = SERVERS_RESOURCE + ERROR_500_DESCRIPTION
                    )
            },
            description = SERVERS_RESOURCE + READ_DESCRIPTION
    ))
    public Promise<ResourceResponse, ResourceException> read(Context context, String id) {
        try {
            SSOToken token = getSsoToken(context);
            return newResultPromise(getServer(token, getServerUrl(token, id)));
        } catch (SMSException e) {
            debug.error("Error reading server from SMS: {}", id, e);
            return new InternalServerErrorException("Error reading SMS", e).asPromise();
        } catch (SSOException e) {
            return new PermanentException(401, "Invalid ssoToken", e).asPromise();
        } catch (NotFoundException e) {
            return new NotFoundException("Cannot find server ID: " + id).asPromise();
        }
    }

    @Action(name = "clone",
            operationDescription = @Operation(
                    description = SERVERS_RESOURCE + "action.clone." + DESCRIPTION
            ),
            request = @Schema(schemaResource = "ServersResource.clone.request.schema.json"),
            response = @Schema(schemaResource = "ServersResource.schema.json"))
    public Promise<ActionResponse, ResourceException> clone(
            String existingServerId, Context context, ActionRequest request) {
        try {
            if (isEmpty(existingServerId)) {
                return new BadRequestException("Existing server Id must be specified").asPromise();
            }

            SSOToken ssoToken = getSsoToken(context);
            String existingServerUrl = getServerUrl(ssoToken, existingServerId);

            JsonValue requestBody = request.getContent();
            String clonedUrl = requestBody.get("clonedUrl").asString();

            if (isEmpty(clonedUrl)) {
                return new BadRequestException("Missing clonedUrl from json body").asPromise();
            }
            if (isServerInstanceExist(ssoToken, clonedUrl)) {
                return new ConflictException("Server URL already exists: " + clonedUrl).asPromise();
            }

            String clonedId = requestBody.get("clonedId").asString();

            if (isNotEmpty(clonedId)) {
                if (hasServerOrSiteId(ssoToken, clonedId)) {
                    return new ConflictException("Server ID already exists: " + clonedId).asPromise();
                }

                cloneServerInstance(ssoToken, existingServerUrl, clonedUrl, clonedId);
            } else {
                cloneServerInstance(ssoToken, existingServerUrl, clonedUrl);
                clonedId = getServerID(ssoToken, clonedUrl);
            }

            JsonValue responseBody = json(
                    object(
                            field("clonedId", clonedId),
                            field("clonedUrl", clonedUrl)));

            return newResultPromise(newActionResponse(responseBody));
        } catch (SSOException | SMSException e) {
            return new InternalServerErrorException("Failed to clone server", e).asPromise();
        } catch (ConfigurationException e) {
            return new BadRequestException(e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    private ResourceResponse getServer(SSOToken token, String serverUrl)
            throws SMSException, SSOException, NotFoundException {

        if (!isServerInstanceExist(token, serverUrl)) {
            throw new NotFoundException();
        }
        String serverId = getServerID(token, serverUrl);
        String siteName = getServerSite(token, serverUrl);
        JsonValue responseBody = json(object(
                field("_id", serverId),
                field("url", serverUrl),
                field("siteName", siteName)
        ));
        return newResourceResponse(serverId, String.valueOf(responseBody.getObject().hashCode()), responseBody);
    }

    private SSOToken getSsoToken(Context context) throws SSOException {
        return context.asContext(SSOTokenContext.class).getCallerSSOToken();
    }

    private String validWriteOperation(JsonValue content, String id) throws BadRequestException {
        if (id == null) {
            id = content.get("_id").asString();
        } else if (content.isDefined("_id") && !id.equals(content.get("_id").asString())) {
            throw new BadRequestException("IDs do not match");
        }
        return id;
    }

    private String getServerUrl(SSOToken token, String serverId) throws NotFoundException, SSOException, SMSException {
        Set<String> serverUrls = getServers(token);
        for (String serverUrl : serverUrls) {
            String id = getServerID(token, serverUrl);
            if (serverId.equals(id)) {
                return serverUrl;
            }
        }
        throw new NotFoundException();
    }
}
