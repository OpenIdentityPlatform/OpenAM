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
 */

package org.forgerock.openam.core.rest.sms;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.json.resource.http.HttpUtils.PROTOCOL_VERSION_1;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.ARRAY_TYPE;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.ITEMS;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.OBJECT_TYPE;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.PROPERTIES;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.READONLY;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.STRING_TYPE;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.TITLE;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.TYPE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ACTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.CREATE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.CREATE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DELETE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DELETE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_400_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_401_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_404_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_500_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATH_PARAM;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.QUERY;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.QUERY_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.READ;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.READ_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.UPDATE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.UPDATE_DESCRIPTION;
import static org.forgerock.openam.rest.RestUtils.crestProtocolVersion;
import static org.forgerock.openam.rest.RestUtils.isContractConformantUserProvidedIdCreate;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.forgerock.openam.utils.CollectionUtils.newList;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SITES_RESOURCE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;

import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.Actions;
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
import org.forgerock.api.enums.CountPolicy;
import org.forgerock.api.enums.PagingMode;
import org.forgerock.api.enums.QueryType;
import com.google.common.collect.Sets;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants;
import org.forgerock.openam.rest.RestConstants;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;

/**
 * A CREST collection resource provider that presents the global sites config in a coherent way.
 */
@CollectionProvider(
        details = @Handler(
                title = SITES_RESOURCE + ApiDescriptorConstants.TITLE,
                description = SITES_RESOURCE + DESCRIPTION,
                resourceSchema = @Schema(schemaResource = "SitesResourceProvider.schema.json"),
                mvccSupported = false),
        pathParam = @Parameter(
                name = "sitesId",
                type = "string",
                description = SITES_RESOURCE + PATH_PARAM + DESCRIPTION))
public class SitesResourceProvider {

    private static final String SITE_NAME = "_id";
    private static final String SITE_ID = "id";
    private static final String PRIMARY_URL = "url";
    private static final String SERVERS = "servers";
    private static final String SECONDARY_URLS = "secondaryURLs";
    private static final String NAME_LABEL = "serverconfig.site.attribute.label.name";
    private static final String PRIMARY_URL_LABEL = "serverconfig.site.attribute.label.primary.url";
    private static final String SERVERS_LABEL = "serverconfig.site.attribute.label.site.servers";
    private static final String SECONDARY_URLS_LABEL = "serverconfig.site.attribute.label.failover.urls";
    private static final String SERVER_ID = "id";
    private static final String SERVER_URL = "url";
    private final Debug debug;

    @Inject
    public SitesResourceProvider(@Named("frRest") Debug debug) {
        this.debug = debug;
    }

    @Action(operationDescription = @Operation(
            description = SITES_RESOURCE + ACTION + "template." + DESCRIPTION),
            response = @Schema(schemaResource = "SitesResourceProvider.action.template.response.schema.json"))
    public Promise<ActionResponse, ResourceException> template(Context context, ActionRequest request) {
        return newResultPromise(newActionResponse(json(object(
                field(PRIMARY_URL, ""),
                field(SERVERS, array()),
                field(SECONDARY_URLS, array())))));
    }

    @Action(operationDescription = @Operation(
            description = SITES_RESOURCE + ACTION + "schema." + DESCRIPTION),
            response = @Schema(schemaResource = "SitesResourceProvider.action.schema.response.schema.json"))
    public Promise<ActionResponse, ResourceException> schema(Context context, ActionRequest request) {
        ResourceBundle i18n = ResourceBundle.getBundle("amConsole");
        return newResultPromise(newActionResponse(json(object(field(TYPE, OBJECT_TYPE), field(PROPERTIES, object(
                field(SITE_ID, object(
                        field(TYPE, STRING_TYPE),
                        field(READONLY, true)
                        )),
                field(SITE_NAME, object(
                        field(TYPE, STRING_TYPE),
                        field(TITLE, i18n.getString(NAME_LABEL))
                        )),
                field(PRIMARY_URL, object(
                        field(TYPE, STRING_TYPE),
                        field(TITLE, i18n.getString(PRIMARY_URL_LABEL))
                        )),
                field(SERVERS, object(
                        field(TYPE, ARRAY_TYPE),
                        field(TITLE, i18n.getString(SERVERS_LABEL)),
                        field(ITEMS, object(field(TYPE, OBJECT_TYPE), field(PROPERTIES, object(
                                field(SERVER_ID, object(field(TYPE, STRING_TYPE))),
                                field(SERVER_URL, object(field(TYPE, STRING_TYPE)))
                                )))),
                        field(READONLY, true)
                        )),
                field(SECONDARY_URLS, object(
                        field(TYPE, ARRAY_TYPE),
                        field(TITLE, i18n.getString(SECONDARY_URLS_LABEL)),
                        field(ITEMS, object(field(TYPE, STRING_TYPE)))
                        ))
                ))))));
    }

    @Create(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = SITES_RESOURCE + CREATE + ERROR_500_DESCRIPTION)},
            description = SITES_RESOURCE + CREATE_DESCRIPTION
    ))
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        JsonValue content = request.getContent();
        String id = request.getNewResourceId();
        try {
            id = validWriteOperation(content, id);
        } catch (BadRequestException e) {
            return e.asPromise();
        }

        String url = content.get(PRIMARY_URL).asString();
        try {
            SSOToken token = getSsoToken(context);
            if (SiteConfiguration.isSiteExist(token, id)) {
                throw new ConflictException("Site with id already exists: " + id);
            }
            SiteConfiguration.createSite(token, id, url, content.get(SECONDARY_URLS).asCollection());
            debug.message("Site created: {}", id);
            return newResultPromise(getSite(token, id));
        } catch (SMSException | SSOException | ConfigurationException e) {
            debug.error("Could not create site", e);
            return new InternalServerErrorException("Could not create site").asPromise();
        } catch (ConflictException e) {
            if (isContractConformantUserProvidedIdCreate(context, request)) {
                return new PreconditionFailedException(e.getMessage()).asPromise();
            }
            return e.asPromise();
        } catch (NotFoundException e) {
            return new InternalServerErrorException("Could not read site just created").asPromise();
        }
    }

    private SSOToken getSsoToken(Context context) throws SSOException {
        return context.asContext(SSOTokenContext.class).getCallerSSOToken();
    }

    private String validWriteOperation(JsonValue content, String id) throws BadRequestException {
        if (!SiteConfiguration.validateUrl(content.get("url").asString())) {
            throw new BadRequestException("Invalid URL");
        }
        if (!Sets.intersection(content.keys(), asSet("servers", "id")).isEmpty()) {
            throw new BadRequestException("Only url, secondaryURLs and _id are valid in write");
        }
        if (id == null) {
            return content.get("_id").asString();
        } else if (content.isDefined("_id") && !id.equals(content.get("_id").asString())) {
            throw new BadRequestException("IDs do not match");
        }

        return id;
    }

    @Delete(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = SITES_RESOURCE + DELETE + ERROR_500_DESCRIPTION)},
            description = SITES_RESOURCE + DELETE_DESCRIPTION
    ))
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String id,
            DeleteRequest request) {
        ResourceResponse site;
        SSOToken token;
        try {
            token = getSsoToken(context);
            site = getSite(token, id);
        } catch (SMSException | SSOException | ConfigurationException e) {
            debug.error("Could not read site {}", id, e);
            return new InternalServerErrorException("Could not read site").asPromise();
        } catch (NotFoundException e) {
            return e.asPromise();
        }
        try {
            if (!site.getRevision().equals(request.getRevision())) {
                return new PreconditionFailedException("Revision did not match").asPromise();
            } else if (!SiteConfiguration.listServers(token, id).isEmpty()) {
                return new PreconditionFailedException("Site still has servers attached to it").asPromise();
            } else if (!SiteConfiguration.deleteSite(token, id)) {
                return new InternalServerErrorException("Could not delete site: " + id).asPromise();
            } else {
                return newResultPromise(site);
            }
        } catch (SSOException | SMSException | ConfigurationException e) {
            debug.error("Could not delete site {}", id, e);
            return new InternalServerErrorException("Could not delete site").asPromise();
        }
    }

    @Query(operationDescription =
    @Operation(
            description = SITES_RESOURCE + QUERY_DESCRIPTION,
            errors = {
                    @ApiError(
                            code = 400,
                            description = SITES_RESOURCE + QUERY + ERROR_400_DESCRIPTION),
                    @ApiError(
                            code = 500,
                            description = SITES_RESOURCE + QUERY + ERROR_500_DESCRIPTION)}),
            type = QueryType.FILTER,
            countPolicies = {CountPolicy.NONE},
            pagingModes = {PagingMode.COOKIE, PagingMode.OFFSET},
            queryableFields = {"_id", "url"})
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        if (!"true".equals(request.getQueryFilter().toString())) {
            return new BadRequestException("Query only supports 'true' filter").asPromise();
        }
        try {
            SSOToken token = getSsoToken(context);
            Set<String> siteNames = SiteConfiguration.getSites(token);

            for (String siteName : siteNames) {
                handler.handleResource(getSite(token, siteName));
            }

            return newResultPromise(newQueryResponse());
        } catch (SSOException | SMSException | ConfigurationException e) {
            debug.error("Could not read sites", e);
            return new InternalServerErrorException("Could not read sites").asPromise();
        } catch (NotFoundException e) {
            debug.error("Could not read site", e);
            return new InternalServerErrorException("Could not read site we've just got name for").asPromise();
        }
    }

    protected ResourceResponse getSite(SSOToken token, String siteName) throws SMSException, SSOException,
            ConfigurationException, NotFoundException {
        if (!SiteConfiguration.isSiteExist(token, siteName)) {
            throw new NotFoundException();
        }
        JsonValue site = json(object(
                field("_id", siteName),
                field("id", SiteConfiguration.getSiteID(token, siteName)),
                field("url", SiteConfiguration.getSitePrimaryURL(token, siteName)),
                field("secondaryURLs", newList(SiteConfiguration.getSiteSecondaryURLs(token, siteName))),
                field("servers", array())
        ));
        JsonValue servers = site.get("servers");
        for (String server : SiteConfiguration.listServers(token, siteName)) {
            servers.add(object(
                    field("id", ServerConfiguration.getServerID(token, server)),
                    field("url", server)
            ));
        }
        return newResourceResponse(siteName, String.valueOf(site.getObject().hashCode()), site);
    }

    @Read(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 401,
                            description = SITES_RESOURCE + READ + ERROR_401_DESCRIPTION),
                    @ApiError(
                            code = 404,
                            description = SITES_RESOURCE + READ + ERROR_404_DESCRIPTION),
                    @ApiError(
                            code = 500,
                            description = SITES_RESOURCE + READ + ERROR_500_DESCRIPTION)},
            description = SITES_RESOURCE + READ_DESCRIPTION
    ))
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String id,
            ReadRequest request) {
        try {
            SSOToken token = getSsoToken(context);
            ResourceResponse site = getSite(token, id);
            return newResultPromise(site);
        } catch (SMSException e) {
            debug.error("Error reading SMS", id, e);
            return new InternalServerErrorException("Error reading SMS", e).asPromise();
        } catch (SSOException e) {
            return new PermanentException(401, "Invalid ssoToken", e).asPromise();
        } catch (ConfigurationException e) {
            debug.error("Could not read site {}", id, e);
            return new InternalServerErrorException("Error reading configuration for site: " + id).asPromise();
        } catch (NotFoundException e) {
            return new NotFoundException("Cannot find site: " + id).asPromise();
        }
    }

    @Update(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = SITES_RESOURCE + UPDATE + ERROR_500_DESCRIPTION)},
            description = SITES_RESOURCE + UPDATE_DESCRIPTION
    ))
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String id,
            UpdateRequest request) {
        JsonValue content = request.getContent();
        try {
            validWriteOperation(content, id);
        } catch (BadRequestException e) {
            return e.asPromise();
        }

        ResourceResponse site;
        SSOToken token;
        try {
            token = getSsoToken(context);
            site = getSite(token, id);
        } catch (SMSException | SSOException | ConfigurationException e) {
            debug.error("Could not read site {}", id, e);
            return new InternalServerErrorException("Could not read site").asPromise();
        } catch (NotFoundException e) {
            return e.asPromise();
        }
        try {
            if (!site.getRevision().equals(request.getRevision())) {
                return new PreconditionFailedException("Revision did not match").asPromise();
            }
            SiteConfiguration.setSitePrimaryURL(token, id, content.get("url").asString());
            SiteConfiguration.setSiteSecondaryURLs(token, id, content.get("secondaryURLs").asCollection());
            return newResultPromise(getSite(token, id));
        } catch (SSOException | SMSException | ConfigurationException e) {
            debug.error("Could not update site {}", id, e);
            return new InternalServerErrorException("Could not update site").asPromise();
        } catch (NotFoundException e) {
            return new InternalServerErrorException("Could not read site after just updating it", e).asPromise();
        }
    }
}
