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

import static org.forgerock.json.fluent.JsonValue.*;
import static org.forgerock.openam.rest.sms.SmsJsonSchema.*;
import static org.forgerock.openam.utils.CollectionUtils.*;

import java.util.ResourceBundle;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.guava.common.collect.Sets;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.resource.SSOTokenContext;

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
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        switch (request.getAction()) {
            case SmsResourceProvider.TEMPLATE:
                handler.handleResult(json(object()));
                return;
            case SmsResourceProvider.SCHEMA:
                ResourceBundle i18n = ResourceBundle.getBundle("amConsole");
                handler.handleResult(json(object(field(TYPE, OBJECT_TYPE), field(PROPERTIES, object(
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
                )))));
                return;
            default:
                handler.handleError(new BadRequestException("Action not supported: " + request.getAction()));
        }
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        JsonValue content = request.getContent();
        String id = request.getNewResourceId();
        try {
            id = validWriteOperation(content, id);
        } catch (BadRequestException e) {
            handler.handleError(e);
            return;
        }

        String url = content.get(PRIMARY_URL).asString();
        try {
            SSOToken token = getSsoToken(context);
            if (SiteConfiguration.isSiteExist(token, id)) {
                handler.handleError(new ConflictException("Site with id already exists: " + id));
            }
            SiteConfiguration.createSite(token, id, url, content.get(SECONDARY_URLS).asSet());
            debug.message("Site created: {}", id);
            handler.handleResult(getSite(token, id));
        } catch (SMSException | SSOException | ConfigurationException e) {
            debug.error("Could not create site", e);
            handler.handleError(new InternalServerErrorException("Could not create site"));
        } catch (NotFoundException e) {
            handler.handleError(new InternalServerErrorException("Could not read site just created"));
        }

    }

    private SSOToken getSsoToken(ServerContext context) throws SSOException {
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
    public void deleteInstance(ServerContext context, String id, DeleteRequest request, ResultHandler<Resource> handler) {
        Resource site;
        SSOToken token;
        try {
            token = getSsoToken(context);
            site = getSite(token, id);
        } catch (SMSException | SSOException | ConfigurationException e) {
            debug.error("Could not read site {}", id, e);
            handler.handleError(new InternalServerErrorException("Could not read site"));
            return;
        } catch (NotFoundException e) {
            handler.handleError(e);
            return;
        }
        try {
            if (!site.getRevision().equals(request.getRevision())) {
                handler.handleError(new PreconditionFailedException("Revision did not match"));
            } else if (!SiteConfiguration.listServers(token, id).isEmpty()) {
                handler.handleError(new PreconditionFailedException("Site still has servers attached to it"));
            } else if (!SiteConfiguration.deleteSite(token, id)) {
                handler.handleError(new InternalServerErrorException("Could not delete site: " + id));
            } else {
                handler.handleResult(site);
            }
        } catch (SSOException | SMSException | ConfigurationException e) {
            debug.error("Could not delete site {}", id, e);
            handler.handleError(new InternalServerErrorException("Could not delete site"));
        }
    }

    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        if (!"true".equals(request.getQueryFilter().toString())) {
            handler.handleError(new BadRequestException("Query only supports 'true' filter"));
            return;
        }
        try {
            SSOToken token = getSsoToken(context);
            Set<String> siteNames = SiteConfiguration.getSites(token);

            for (String siteName : siteNames) {
                handler.handleResource(getSite(token, siteName));
            }

            handler.handleResult(new QueryResult());
        } catch (SSOException | SMSException | ConfigurationException e) {
            debug.error("Could not read sites", e);
            handler.handleError(new InternalServerErrorException("Could not read sites"));
        } catch (NotFoundException e) {
            debug.error("Could not read site", e);
            handler.handleError(new InternalServerErrorException("Could not read site we've just got name for"));
        }
    }

    protected Resource getSite(SSOToken token, String siteName) throws SMSException, SSOException,
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
        return new Resource(siteName, String.valueOf(site.getObject().hashCode()), site);
    }

    @Override
    public void readInstance(ServerContext context, String id, ReadRequest request, ResultHandler<Resource> handler) {
        try {
            SSOToken token = getSsoToken(context);
            Resource site = getSite(token, id);
            handler.handleResult(site);
        } catch (SMSException | SSOException | ConfigurationException e) {
            debug.error("Could not read site {}", id, e);
            handler.handleError(new InternalServerErrorException("Could not read site"));
        } catch (NotFoundException e) {
            handler.handleError(e);
        }
    }

    @Override
    public void updateInstance(ServerContext context, String id, UpdateRequest request, ResultHandler<Resource> handler) {
        JsonValue content = request.getContent();
        try {
            validWriteOperation(content, id);
        } catch (BadRequestException e) {
            handler.handleError(e);
            return;
        }

        Resource site;
        SSOToken token;
        try {
            token = getSsoToken(context);
            site = getSite(token, id);
        } catch (SMSException | SSOException | ConfigurationException e) {
            debug.error("Could not read site {}", id, e);
            handler.handleError(new InternalServerErrorException("Could not read site"));
            return;
        } catch (NotFoundException e) {
            handler.handleError(e);
            return;
        }
        try {
            if (!site.getRevision().equals(request.getRevision())) {
                handler.handleError(new PreconditionFailedException("Revision did not match"));
                return;
            }
            SiteConfiguration.setSitePrimaryURL(token, id, content.get("url").asString());
            SiteConfiguration.setSiteSecondaryURLs(token, id, content.get("secondaryURLs").asSet());
            handler.handleResult(getSite(token, id));
        } catch (SSOException | SMSException | ConfigurationException e) {
            debug.error("Could not update site {}", id, e);
            handler.handleError(new InternalServerErrorException("Could not update site"));
        } catch (NotFoundException e) {
            handler.handleError(new InternalServerErrorException("Could not read site after just updating it", e));
        }
    }

    @Override
    public void patchInstance(ServerContext context, String id, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    @Override
    public void actionInstance(ServerContext context, String id, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException("Action not supported on instance"));
    }

}
