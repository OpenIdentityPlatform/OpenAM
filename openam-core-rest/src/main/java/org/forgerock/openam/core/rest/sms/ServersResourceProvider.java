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
package org.forgerock.openam.core.rest.sms;

import static com.sun.identity.common.configuration.ServerConfiguration.*;
import static java.util.Collections.emptySet;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openam.utils.StringUtils.*;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.annotations.Action;
import org.forgerock.json.resource.annotations.Create;
import org.forgerock.json.resource.annotations.Delete;
import org.forgerock.json.resource.annotations.Query;
import org.forgerock.json.resource.annotations.Read;
import org.forgerock.json.resource.annotations.RequestHandler;
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
@RequestHandler
public final class ServersResourceProvider {

    private final Debug debug;

    @Inject
    public ServersResourceProvider(@Named("frRest") Debug debug) {
        this.debug = debug;
    }

    @Create
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
                return new ConflictException("Server with ID already exists: " + id).asPromise();
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
        } catch (SSOException | SMSException | ConfigurationException e) {
            debug.error("Could not create server", e);
            return new InternalServerErrorException("Could not create server").asPromise();
        } catch (NotFoundException e) {
            debug.error("Could not read server", e);
            return new InternalServerErrorException("Could not read server just created").asPromise();
        } catch (UnknownPropertyNameException e) {
           return new BadRequestException(e.getMessage()).asPromise();
        }
    }

    @Delete
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

    @Query
    public Promise<QueryResponse, ResourceException> query(Context context, QueryRequest request,
            QueryResourceHandler handler) {

        if (!"true".equals(request.getQueryFilter().toString())) {
            return new BadRequestException("Query only supports 'true' filter").asPromise();
        }
        try {
            SSOToken token = getSsoToken(context);
            Set<String> serverUrls = getServers(token);

            for (String serverUrl : serverUrls) {
                handler.handleResource(getServer(token, serverUrl));
            }

            return newResultPromise(newQueryResponse());
        } catch (SSOException | SMSException | ConfigurationException e) {
            debug.error("Could not read servers", e);
            return new InternalServerErrorException("Could not read servers").asPromise();
        } catch (NotFoundException e) {
            debug.error("Could not read servers", e);
            return new InternalServerErrorException("Could not read server we've just got name for").asPromise();
        }
    }

    @Read
    public Promise<ResourceResponse, ResourceException> read(Context context, String id) {
        try {
            SSOToken token = getSsoToken(context);
            return newResultPromise(getServer(token, getServerUrl(token, id)));
        } catch (SMSException e) {
            debug.error("Error reading server from SMS: {}", id, e);
            return new InternalServerErrorException("Error reading SMS", e).asPromise();
        } catch (SSOException e) {
            return new PermanentException(401, "Invalid ssoToken", e).asPromise();
        } catch (ConfigurationException e) {
            debug.error("Could not read server {}", id, e);
            return new InternalServerErrorException("Error reading configuration for server: " + id).asPromise();
        } catch (NotFoundException e) {
            return new NotFoundException("Cannot find server ID: " + id).asPromise();
        }
    }

    @Action
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
        } catch (SSOException | SMSException | ConfigurationException e) {
            return new InternalServerErrorException("Failed to clone server", e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    private ResourceResponse getServer(SSOToken token, String serverUrl) throws SMSException, SSOException,
            ConfigurationException, NotFoundException {

        if (!isServerInstanceExist(token, serverUrl)) {
            throw new NotFoundException();
        }
        String serverId = getServerID(token, serverUrl);
        JsonValue site = json(object(
                field("_id", serverId),
                field("url", serverUrl)
        ));
        return newResourceResponse(serverId, String.valueOf(site.getObject().hashCode()), site);
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
