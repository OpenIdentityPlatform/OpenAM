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
import java.util.List;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.saml2.idpdiscovery.Debug;
import org.forgerock.http.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
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

public class RealmContextFilter implements org.forgerock.http.Filter, Filter {

    private final Debug logger = Debug.getInstance("frRest");
    private final RestRealmValidator realmValidator;
    private final CoreWrapper coreWrapper;
    private final ChfRouterContextAdded chf;
    private final CrestRouterContextAdded crest;

    /**
     *
     *
     * @param realmValidator An instance of the RestRealmValidator.
     * @param coreWrapper An instance of the CoreWrapper.
     */
    @Inject
    public RealmContextFilter(RestRealmValidator realmValidator, CoreWrapper coreWrapper) {
        this.realmValidator = realmValidator;
        this.coreWrapper = coreWrapper;
        this.chf = new ChfRouterContextAdded();
        this.crest = new CrestRouterContextAdded();
    }

    @Override
    public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
        try {
            return next.handle(chf.addRouterContext(context, request), request);
        } catch (BadRequestException e) {
            logger.message("Attempting to access invalid realm", e);
            return newResultPromise(new Response(Status.BAD_REQUEST));
        } catch (InternalServerErrorException e) {
            logger.message("Failed to determine server name", e);
            return newResultPromise(new Response(Status.INTERNAL_SERVER_ERROR));
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest request,
            RequestHandler next) {
        try {
            return next.handleAction(crest.addRouterContext(context, request), request);
        } catch (ResourceException e) {
            logger.message("Attempting to access invalid realm", e);
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest request,
            RequestHandler next) {
        try {
            return next.handleCreate(crest.addRouterContext(context, request), request);
        } catch (ResourceException e) {
            logger.message("Attempting to access invalid realm", e);
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest request,
            RequestHandler next) {
        try {
            return next.handleDelete(crest.addRouterContext(context, request), request);
        } catch (ResourceException e) {
            logger.message("Attempting to access invalid realm", e);
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest request,
            RequestHandler next) {
        try {
            return next.handlePatch(crest.addRouterContext(context, request), request);
        } catch (ResourceException e) {
            logger.message("Attempting to access invalid realm", e);
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest request,
            QueryResourceHandler handler, RequestHandler next) {
        try {
            return next.handleQuery(crest.addRouterContext(context, request), request, handler);
        } catch (ResourceException e) {
            logger.message("Attempting to access invalid realm", e);
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest request,
            RequestHandler next) {
        try {
            return next.handleRead(crest.addRouterContext(context, request), request);
        } catch (ResourceException e) {
            logger.message("Attempting to access invalid realm", e);
            return newExceptionPromise(e);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest request,
            RequestHandler next) {
        try {
            return next.handleUpdate(crest.addRouterContext(context, request), request);
        } catch (ResourceException e) {
            logger.message("Attempting to access invalid realm", e);
            return newExceptionPromise(e);
        }
    }

    private abstract class RouterContextAdded<T> {

        Context addRouterContext(Context context, T request) throws BadRequestException, InternalServerErrorException {

            RealmContext realmContext;
            if (context.containsContext(RealmContext.class)) {
                realmContext = context.asContext(RealmContext.class);
            } else {
                realmContext = new RealmContext(context);
            }

            boolean handled = getRealmFromURI(context, realmContext);
            if (!handled) {
                getRealmFromServerName(context, realmContext, request);
            }

            setRealmFromQueryString(context, realmContext, request);
            return realmContext;
        }

        private boolean getRealmFromURI(Context context, RealmContext realmContext) throws BadRequestException {
            if (context.containsContext(UriRouterContext.class)) {
                String subRealm = context.asContext(UriRouterContext.class).getUriTemplateVariables().get("realm");
                subRealm = validateRealm(realmContext.getRebasedRealm(), subRealm);
                if (subRealm != null) {
                    realmContext.addSubRealm(subRealm, subRealm);
                    return true;
                }
            }
            return false;
        }

        private void setRealmFromQueryString(Context context, RealmContext realmContext, T request)
                throws BadRequestException {
            if (realmContext.getOverrideRealm() != null) {
                return;
            }
            List<String> realm = getRealmFromQueryString(context, request);
            if (realm == null || realm.size() != 1) {
                return;
            }
            String validatedRealm = validateRealm("", realm.get(0));
            if (validatedRealm != null) {
                realmContext.setOverrideRealm(validatedRealm);
            } else {
                throw new BadRequestException("Invalid realm, " + realm.get(0));
            }
        }

        abstract List<String> getRealmFromQueryString(Context context, T request);

        private boolean getRealmFromServerName(Context context, RealmContext realmContext, T request)
                throws InternalServerErrorException, BadRequestException {
            String serverName = getServerName(context, request);
            try {
                SSOToken adminToken = coreWrapper.getAdminToken();
                String orgDN = coreWrapper.getOrganization(adminToken, serverName);
                String realmPath = validateRealm(coreWrapper.convertOrgNameToRealmName(orgDN));
                realmContext.addDnsAlias(serverName, realmPath);
                return true;
            } catch (IdRepoException | SSOException e) {
                throw new InternalServerErrorException(e);
            }
        }

        abstract String getServerName(Context context, T request);

        private String validateRealm(String realmPath) throws BadRequestException {
            if (!realmValidator.isRealm(realmPath)) {
                try {
                    SSOToken adminToken = coreWrapper.getAdminToken();
                    //Need to strip off leading '/' from realm otherwise just generates a DN based of the realm value,
                    // which is wrong
                    String realm = realmPath;
                    if (realm.startsWith("/")) {
                        realm = realm.substring(1);
                    }
                    String orgDN = coreWrapper.getOrganization(adminToken, realm);
                    return coreWrapper.convertOrgNameToRealmName(orgDN);
                } catch (IdRepoException | SSOException ignored) {
                    logger.message("Failed to validate realm", ignored);
                }
                throw new BadRequestException("Invalid realm, " + realmPath);
            }
            return realmPath;
        }

        private String validateRealm(String realmPath, String subRealm) throws BadRequestException {
            if (subRealm == null || subRealm.isEmpty()) {
                return null;
            }
            if (realmPath.endsWith("/")) {
                realmPath = realmPath.substring(0, realmPath.length() - 1);
            }
            if (!subRealm.startsWith("/")) {
                subRealm = "/" + subRealm;
            }
            if (subRealm.endsWith("/") && (realmPath + subRealm).length() > 1) {
                subRealm = subRealm.substring(0, subRealm.length() - 1);
            }
            String validatedRealm = validateRealm(realmPath + subRealm);
            if (!realmValidator.isRealm(realmPath + subRealm)) {
                return validatedRealm;
            } else {
                return subRealm;
            }
        }
    }

    private final class ChfRouterContextAdded extends RouterContextAdded<Request> {

        @Override
        List<String> getRealmFromQueryString(Context context, Request request) {
            return request.getForm().get("realm");
        }

        @Override
        String getServerName(Context context, Request request) {
            return request.getUri().getHost();
        }
    }

    private final class CrestRouterContextAdded extends RouterContextAdded<org.forgerock.json.resource.Request> {

        @Override
        List<String> getRealmFromQueryString(Context context, org.forgerock.json.resource.Request request) {
            return context.asContext(HttpContext.class).getParameter("realm");
        }

        @Override
        String getServerName(Context context, org.forgerock.json.resource.Request request) {
            return URI.create(context.asContext(HttpContext.class).getPath()).getHost();
        }
    }
}
