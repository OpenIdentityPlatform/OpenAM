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

package org.forgerock.openam.rest;

import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.http.Context;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.ResourcePath;
import org.forgerock.http.protocol.Form;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * Filter which will consume the realm part of the request URI and inject a
 * {@link RealmContext} into the {@link Context} hierarchy.
 *
 * @since 13.0.0
 */
public class RealmContextFilter implements Filter, org.forgerock.json.resource.Filter {

    private final CoreWrapper coreWrapper;
    private final RestRealmValidator realmValidator;

    @Inject
    public RealmContextFilter(CoreWrapper coreWrapper, RestRealmValidator realmValidator) {
        this.coreWrapper = coreWrapper;
        this.realmValidator = realmValidator;
    }

    @Override
    public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
        try {
            return next.handle(evaluate(context, request), request);
        } catch (ResourceException e) {
            return newResultPromise(new Response(Status.BAD_REQUEST).setEntity(e.toJsonValue().getObject()));
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest request,
            RequestHandler next) {
        try {
            return next.handleAction(evaluate(context, request), request);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest request,
            RequestHandler next) {
        try {
            return next.handleCreate(evaluate(context, request), request);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest request,
            RequestHandler next) {
        try {
            return next.handleDelete(evaluate(context, request), request);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest request,
            RequestHandler next) {
        try {
            return next.handlePatch(evaluate(context, request), request);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest request,
            QueryResourceHandler handler, RequestHandler next) {
        try {
            return next.handleQuery(evaluate(context, request), request, handler);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest request,
            RequestHandler next) {
        try {
            return next.handleRead(evaluate(context, request), request);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest request,
            RequestHandler next) {
        try {
            return next.handleUpdate(evaluate(context, request), request);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    private Context evaluate(Context context, Request request) throws ResourceException {
        String hostname = request.getUri().getHost();
        ResourcePath requestUri = getRemainingRequestUri(context, request);
        List<String> realmOverrideParameter = new Form().fromRequestQuery(request).get("realm");
        return evaluate(context, hostname, requestUri, realmOverrideParameter);
    }

    private Context evaluate(Context context, org.forgerock.json.resource.Request request) throws ResourceException {
        String hostname = URI.create(context.asContext(HttpContext.class).getPath()).getHost();
        ResourcePath requestUri = request.getResourcePathObject();
        List<String> realmOverrideParameter = context.asContext(HttpContext.class).getParameter("realm");
        return evaluate(context, hostname, requestUri, realmOverrideParameter);
    }

    private Context evaluate(Context context, String hostname, ResourcePath requestUri,
            List<String> overrideRealmParameter) throws ResourceException {

        SSOToken adminToken = coreWrapper.getAdminToken();

        String dnsAliasRealm = cleanRealm(getRealmFromServerName(adminToken, hostname));
        StringBuilder matchedUriBuilder = new StringBuilder();
        String currentRealm = dnsAliasRealm;
        int consumedElementsCount = 0;
        for (String element : requestUri) {
            try {
                String subrealm = cleanRealm(element);
                currentRealm = resolveRealm(adminToken, currentRealm, subrealm);
                matchedUriBuilder.append(subrealm);
                consumedElementsCount++;
            } catch (InternalServerErrorException ignored) {
                break;
            }
        }

        String overrideRealm = null;
        if (overrideRealmParameter != null && !overrideRealmParameter.isEmpty()) {
            overrideRealm = resolveRealm(adminToken, "/", cleanRealm(overrideRealmParameter.get(0)));
        }

        ResourcePath remainingUri = requestUri.subSequence(consumedElementsCount, requestUri.size());
        String matchedUri = matchedUriBuilder.length() > 1 ? matchedUriBuilder.substring(1) : matchedUriBuilder.toString();

        RealmContext realmContext = new RealmContext(new UriRouterContext(context, matchedUri, remainingUri.toString(), Collections.<String, String>emptyMap()));
        realmContext.setDnsAlias(hostname, dnsAliasRealm);
        realmContext.setSubRealm(matchedUri, cleanRealm(currentRealm.substring(dnsAliasRealm.length())));
        realmContext.setOverrideRealm(overrideRealm);
        return realmContext;
    }

    private String cleanRealm(String realm) {
        if (!realm.startsWith("/")) {
            realm = "/" + realm;
        }
        if (realm.length() > 1 && realm.endsWith("/")) {
            realm = realm.substring(0, realm.length() - 1);
        }
        return realm;
    }

    private String resolveRealm(SSOToken adminToken, String parentRealm, String subrealm) throws ResourceException {
        String realm = parentRealm.equals("/") ? subrealm : parentRealm + subrealm;
        if (!realmValidator.isRealm(realm)) {
            // Ignoring parentRealm here as a realm alias is only applicable if it is from the root realm
            return getRealmFromServerName(adminToken, subrealm.substring(1));
        }
        return realm;
    }

    private String getRealmFromServerName(SSOToken adminToken, String hostname) throws ResourceException {
        try {
            String orgDN = coreWrapper.getOrganization(adminToken, hostname);
            String realm = coreWrapper.convertOrgNameToRealmName(orgDN);
            if (realm == null) {
                throw new BadRequestException("Invalid realm, " + hostname);
            }
            return realm;
        } catch (IdRepoException | SSOException e) {
            throw new InternalServerErrorException(e);
        }
    }

    private ResourcePath getRemainingRequestUri(Context context, Request request) {
        ResourcePath path = request.getUri().getResourcePath();
        if (context.containsContext(UriRouterContext.class)) {
            ResourcePath matchedUri = ResourcePath.valueOf(context.asContext(UriRouterContext.class).getBaseUri());
            path = path.tail(matchedUri.size());
        }
        return path;
    }
}
