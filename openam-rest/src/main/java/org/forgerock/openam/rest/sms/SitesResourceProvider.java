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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.rest.sms;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.ResourceException.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openam.rest.sms.SmsJsonSchema.*;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ResourceBundle;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import org.forgerock.guava.common.collect.Sets;
import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.util.promise.Promise;

/**
 * A CREST collection resource provider that presents the global sites config in a coherent way.
 */
public class SitesResourceProvider implements CollectionResourceProvider {

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

    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        switch (request.getAction()) {
            case SmsResourceProvider.TEMPLATE:
                return newResultPromise(newActionResponse(json(object())));
            case SmsResourceProvider.SCHEMA:
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
            default:
                return newExceptionPromise(newBadRequestException("Action not supported: " + request.getAction()));
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        JsonValue content = request.getContent();
        String id = request.getNewResourceId();
        try {
            id = validWriteOperation(content, id);
        } catch (BadRequestException e) {
            return newExceptionPromise(adapt(e));
        }

        String url = content.get(PRIMARY_URL).asString();
        try {
            SSOToken token = getSsoToken(context);
            if (SiteConfiguration.isSiteExist(token, id)) {
                return newExceptionPromise(adapt(new ConflictException("Site with id already exists: " + id)));
            }
            SiteConfiguration.createSite(token, id, url, content.get(SECONDARY_URLS).asSet());
            debug.message("Site created: {}", id);
            return newResultPromise(getSite(token, id));
        } catch (SMSException | SSOException | ConfigurationException e) {
            debug.error("Could not create site", e);
            return newExceptionPromise(newInternalServerErrorException("Could not create site"));
        } catch (NotFoundException e) {
            return newExceptionPromise(newInternalServerErrorException("Could not read site just created"));
        }
    }

    private SSOToken getSsoToken(Context context) throws SSOException {
        return context.asContext(SSOTokenContext.class).getCallerSSOToken();
    }

    private String validWriteOperation(JsonValue content, String id) throws BadRequestException {
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

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String id,
            DeleteRequest request) {
        ResourceResponse site;
        SSOToken token;
        try {
            token = getSsoToken(context);
            site = getSite(token, id);
        } catch (SMSException | SSOException | ConfigurationException e) {
            debug.error("Could not read site {}", id, e);
            return newExceptionPromise(newInternalServerErrorException("Could not read site"));
        } catch (NotFoundException e) {
            return newExceptionPromise(adapt(e));
        }
        try {
            if (!site.getRevision().equals(request.getRevision())) {
                return newExceptionPromise(adapt(new PreconditionFailedException("Revision did not match")));
            } else if (!SiteConfiguration.listServers(token, id).isEmpty()) {
                return newExceptionPromise(adapt(new PreconditionFailedException("Site still has servers attached to it")));
            } else if (!SiteConfiguration.deleteSite(token, id)) {
                return newExceptionPromise(newInternalServerErrorException("Could not delete site: " + id));
            } else {
                return newResultPromise(site);
            }
        } catch (SSOException | SMSException | ConfigurationException e) {
            debug.error("Could not delete site {}", id, e);
            return newExceptionPromise(newInternalServerErrorException("Could not delete site"));
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        if (!"true".equals(request.getQueryFilter().toString())) {
            return newExceptionPromise(newBadRequestException("Query only supports 'true' filter"));
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
            return newExceptionPromise(newInternalServerErrorException("Could not read sites"));
        } catch (NotFoundException e) {
            debug.error("Could not read site", e);
            return newExceptionPromise(newInternalServerErrorException("Could not read site we've just got name for"));
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
                field("secondaryURLs", SiteConfiguration.getSiteSecondaryURLs(token, siteName)),
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

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String id,
            ReadRequest request) {
        try {
            SSOToken token = getSsoToken(context);
            ResourceResponse site = getSite(token, id);
            return newResultPromise(site);
        } catch (SMSException | SSOException | ConfigurationException e) {
            debug.error("Could not read site {}", id, e);
            return newExceptionPromise(newInternalServerErrorException("Could not read site"));
        } catch (NotFoundException e) {
            return newExceptionPromise(adapt(e));
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String id,
            UpdateRequest request) {
        JsonValue content = request.getContent();
        try {
            validWriteOperation(content, id);
        } catch (BadRequestException e) {
            return newExceptionPromise(adapt(e));
        }

        ResourceResponse site;
        SSOToken token;
        try {
            token = getSsoToken(context);
            site = getSite(token, id);
        } catch (SMSException | SSOException | ConfigurationException e) {
            debug.error("Could not read site {}", id, e);
            return newExceptionPromise(newInternalServerErrorException("Could not read site"));
        } catch (NotFoundException e) {
            return newExceptionPromise(adapt(e));
        }
        try {
            if (!site.getRevision().equals(request.getRevision())) {
                return newExceptionPromise(adapt(new PreconditionFailedException("Revision did not match")));
            }
            SiteConfiguration.setSitePrimaryURL(token, id, content.get("url").asString());
            SiteConfiguration.setSiteSecondaryURLs(token, id, content.get("secondaryURLs").asSet());
            return newResultPromise(getSite(token, id));
        } catch (SSOException | SMSException | ConfigurationException e) {
            debug.error("Could not update site {}", id, e);
            return newExceptionPromise(newInternalServerErrorException("Could not update site"));
        } catch (NotFoundException e) {
            return newExceptionPromise(newInternalServerErrorException("Could not read site after just updating it", e));
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String id,
            PatchRequest request) {
        return newExceptionPromise(newNotSupportedException());
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String id,
            ActionRequest request) {
        return newExceptionPromise(newNotSupportedException("Action not supported on instance"));
    }
}
