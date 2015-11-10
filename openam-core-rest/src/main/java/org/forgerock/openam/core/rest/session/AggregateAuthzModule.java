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
package org.forgerock.openam.core.rest.session;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;

/**
 * This module does not fail on the first failure it encounters. Rather it attempts
 * each of the modules contained within in in turn, if all of them return false it
 * will return false. The order of inserted authz modules is significant as the
 * first module to return success will end the execution chain.
 */
public class AggregateAuthzModule implements CrestAuthorizationModule {

    final List<CrestAuthorizationModule> filters;

    /**
     * Generate a new AggregateAuthzModule with the list of filters supplied.
     *
     * @param filters Filters appropriate to this aggregate module.
     */
    public AggregateAuthzModule(CrestAuthorizationModule... filters) {
        Reject.ifNull(filters);
        Reject.ifTrue(filters.length == 0);

        this.filters = Arrays.asList(filters);
    }

    @Override
    public String getName() {
        return "AggregateAuthzModule";
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeCreate(Context context,
                                                                           CreateRequest createRequest) {
        Promise<AuthorizationResult, ResourceException> response;

        for (CrestAuthorizationModule module : filters) {
            response = module.authorizeCreate(context, createRequest);

            try {
                if (response.get().isAuthorized()) {
                    return response;
                }
            } catch (ExecutionException | InterruptedException e) {
                return new ForbiddenException().asPromise();
            }
        }

        return new ForbiddenException().asPromise();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeRead(Context context, ReadRequest readRequest) {
        Promise<AuthorizationResult, ResourceException> response;

        for (CrestAuthorizationModule module : filters) {
            response = module.authorizeRead(context, readRequest);

            try {
                if (response.get().isAuthorized()) {
                    return response;
                }
            } catch (ExecutionException | InterruptedException e) {
                return new ForbiddenException().asPromise();
            }
        }

        return new ForbiddenException().asPromise();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeUpdate(Context context,
                                                                           UpdateRequest updateRequest) {
        Promise<AuthorizationResult, ResourceException> response;

        for (CrestAuthorizationModule module : filters) {
            response = module.authorizeUpdate(context, updateRequest);

            try {
                if (response.get().isAuthorized()) {
                    return response;
                }
            } catch (ExecutionException | InterruptedException e) {
                return new ForbiddenException().asPromise();
            }
        }

        return new ForbiddenException().asPromise();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeDelete(Context context,
                                                                           DeleteRequest deleteRequest) {
        Promise<AuthorizationResult, ResourceException> response;

        for (CrestAuthorizationModule module : filters) {
            response = module.authorizeDelete(context, deleteRequest);

            try {
                if (response.get().isAuthorized()) {
                    return response;
                }
            } catch (ExecutionException | InterruptedException e) {
                return new ForbiddenException().asPromise();
            }
        }

        return new ForbiddenException().asPromise();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizePatch(Context context,
                                                                          PatchRequest patchRequest) {
        Promise<AuthorizationResult, ResourceException> response;

        for (CrestAuthorizationModule module : filters) {
            response = module.authorizePatch(context, patchRequest);

            try {
                if (response.get().isAuthorized()) {
                    return response;
                }
            } catch (ExecutionException | InterruptedException e) {
                return new ForbiddenException().asPromise();
            }
        }

        return new ForbiddenException().asPromise();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeAction(Context context,
                                                                           ActionRequest actionRequest) {
        Promise<AuthorizationResult, ResourceException> response;

        for (CrestAuthorizationModule module : filters) {
            response = module.authorizeAction(context, actionRequest);

            try {
                if (response.get().isAuthorized()) {
                    return response;
                }
            } catch (ExecutionException | InterruptedException e) {
                return new ForbiddenException().asPromise();
            }
        }

        return new ForbiddenException().asPromise();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeQuery(Context context, QueryRequest queryRequest) {
        Promise<AuthorizationResult, ResourceException> response;

        for (CrestAuthorizationModule module : filters) {
            response = module.authorizeQuery(context, queryRequest);

            try {
                if (response.get().isAuthorized()) {
                    return response;
                }
            } catch (ExecutionException | InterruptedException e) {
                return new ForbiddenException().asPromise();
            }
        }

        return new ForbiddenException().asPromise();
    }
}
