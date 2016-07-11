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
package org.forgerock.openam.core.rest;

import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.core.rest.IdentityRestUtils.*;
import static org.forgerock.openam.rest.RestUtils.isAdmin;
import static org.forgerock.openam.utils.CollectionUtils.isNotEmpty;
import static org.forgerock.util.promise.Promises.newResultPromise;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idsvcs.AccessDenied;
import com.sun.identity.idsvcs.Attribute;
import com.sun.identity.idsvcs.GeneralFailure;
import com.sun.identity.idsvcs.IdentityDetails;
import com.sun.identity.idsvcs.ObjectNotFound;
import com.sun.identity.idsvcs.TokenExpired;
import com.sun.identity.idsvcs.opensso.IdentityServicesImpl;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.forgerockrest.utils.MailServerLoader;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.services.RestSecurityProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.sm.config.ConsoleConfigHandler;
import org.forgerock.openam.utils.CrestQuery;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A simple {@code Map} based collection resource provider.
 */
public final class IdentityResourceV3 implements CollectionResourceProvider {

    private final String objectType;
    private final IdentityServicesImpl identityServices;
    private final IdentityResourceV2 identityResourceV2;
    private final Set<String> patchableAttributes;

    private static Debug logger = Debug.getInstance("frRest");

    /**
     * Constructs a new identity resource.
     *
     * @param objectType
     *         the object type (whether user, group or agent)
     * @param mailServerLoader
     *         the mail service provider
     * @param identityServices
     *         the identity service
     * @param coreWrapper
     *         core utility API
     * @param restSecurityProvider
     *         self service config provider
     * @param baseURLProviderFactory
     *         URL provider factory
     * @param patchableAttributes
     *         set of acceptable patchable attributes
     */
    public IdentityResourceV3(String objectType, MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, BaseURLProviderFactory baseURLProviderFactory, Set<String> patchableAttributes,
            Set<UiRolePredicate> uiRolePredicates) {
        this.identityResourceV2 = new IdentityResourceV2(objectType, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider, configHandler, baseURLProviderFactory, uiRolePredicates);
        this.objectType = objectType;
        this.identityServices = identityServices;
        this.patchableAttributes = patchableAttributes;
    }

    /**
     * Version 3 of this endpoint cannot remove these actions as this version is invoked by default.
     * Instead we log messages to remind the user that these actions will disappear from here, although they
     * will be retained by Version 2.
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {

        final String action = request.getAction();
        if ("register".equalsIgnoreCase(action)) {
            logAsDeprecated(action);
        } else if ("confirm".equalsIgnoreCase(action)) {
            logAsDeprecated(action);
        } else if ("anonymousCreate".equalsIgnoreCase(action)) {
            logAsDeprecated(action);
        } else if ("forgotPassword".equalsIgnoreCase(action)) {
            logAsDeprecated(action);
        } else if ("forgotPasswordReset".equalsIgnoreCase(action)) {
            logAsDeprecated(action);
        }
        return identityResourceV2.actionCollection(context, request);
    }

    private void logAsDeprecated(String action) {
        logger.warning("The action '" + action + "' in the 'users' endpoint is deprecated in version 3.0");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(final Context context,
            final String resourceId, final ActionRequest request) {

        return identityResourceV2.actionInstance(context, resourceId, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(final Context context,
            final CreateRequest request) {

        return identityResourceV2.createInstance(context, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(final Context context,
                                                                       final String resourceId,
                                                                       final DeleteRequest request) {
        return identityResourceV2.deleteInstance(context, resourceId, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(final Context context,
                                                                     final String resourceId,
                                                                     final ReadRequest request) {
        return identityResourceV2.readInstance(context, resourceId, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(final Context context,
                                                                       final String resourceId,
                                                                       final UpdateRequest request) {
        return identityResourceV2.updateInstance(context, resourceId, request);
    }

    /*******************************************************************************************************************
     * {@inheritDoc}
     */
    public Promise<QueryResponse, ResourceException> queryCollection(final Context context,
            final QueryRequest request, final QueryResourceHandler handler) {

        RealmContext realmContext = context.asContext(RealmContext.class);
        final String realm = realmContext.getResolvedRealm();

        try {
            SSOToken admin = context.asContext(SSOTokenContext.class).getCallerSSOToken();
            IdentityServicesImpl identityServices = getIdentityServices();
            List<IdentityDetails> userDetails = null;

            // If the user specified _queryFilter, then (convert and) use that, otherwise look for _queryID
            // and if that isn't there either, pretend the user gave a _queryID of "*"
            //
            QueryFilter<JsonPointer> queryFilter = request.getQueryFilter();
            if (queryFilter != null) {
                CrestQuery crestQuery = new CrestQuery(queryFilter);
                userDetails = identityServices.searchIdentityDetails(crestQuery,
                        getIdentityServicesAttributes(realm, objectType),
                        admin);
            } else {
                String queryId = request.getQueryId();
                if (queryId == null || queryId.isEmpty()) {
                    queryId = "*";
                }
                CrestQuery crestQuery = new CrestQuery(queryId);
                userDetails = identityServices.searchIdentityDetails(crestQuery,
                        getIdentityServicesAttributes(realm, objectType),
                        admin);
            }

            String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);
            logger.message("IdentityResourceV3.queryCollection :: QUERY performed on realm "
                    + realm
                    + " by "
                    + principalName);

            for (IdentityDetails userDetail : userDetails) {
                ResourceResponse resource;
                resource = newResourceResponse(userDetail.getName(),
                        "0",
                        identityResourceV2.addRoleInformation(context,
                                userDetail.getName(),
                                identityDetailsToJsonValue(userDetail)));
                handler.handleResource(resource);
            }

        } catch (ResourceException resourceException) {
            logger.warning("IdentityResourceV3.queryCollection caught ResourceException", resourceException);
            return resourceException.asPromise();
        } catch (Exception exception) {
            logger.error("IdentityResourceV3.queryCollection caught exception", exception);
            return new InternalServerErrorException(exception.getMessage(), exception).asPromise();
        }

        return newResultPromise(newQueryResponse());
    }

    /**
     * Patch the user's password and only the password.  No other value may be patched.  The old value of the
     * password does not have to be known.  Admin only.  The only patch operation supported is "replace", i.e. not
     * "add" or "move", etc.
     *
     * @param context The context
     * @param resourceId The username we're patching
     * @param request The patch request
     */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(final Context context,
                                                                      final String resourceId,
                                                                      final PatchRequest request) {

        if (!objectType.equals(IdentityRestUtils.USER_TYPE)) {
            return new BadRequestException("Cannot patch object type " + objectType).asPromise();
        }

        RealmContext realmContext = context.asContext(RealmContext.class);
        final String realm = realmContext.getResolvedRealm();

        try {
            if (!isAdmin(context)) {
                return new ForbiddenException("Only admin can patch user values").asPromise();
            }

            SSOToken ssoToken = getSSOToken(RestUtils.getToken().getTokenID().toString());
            IdentityServicesImpl identityServices = getIdentityServices();

            IdentityDetails identityDetails = identityServices.read(resourceId,
                    getIdentityServicesAttributes(realm, objectType),
                    ssoToken);

            Attribute[] existingAttributes = identityDetails.getAttributes();
            Map<String, Set<String>> existingAttributeMap = attributesToMap(existingAttributes);
            Map<String, Set<String>> newAttributeMap = new HashMap<>();

            if (existingAttributeMap.containsKey(IdentityRestUtils.UNIVERSAL_ID)) {
                Set<String> values = existingAttributeMap.get(IdentityRestUtils.UNIVERSAL_ID);
                if (isNotEmpty(values) && !isUserActive(values.iterator().next())) {
                    return new ForbiddenException("User "
                            + resourceId
                            + " is not active: Request is forbidden").asPromise();
                }
            }

            boolean updateNeeded = false;
            for (PatchOperation patchOperation : request.getPatchOperations()) {
                switch (patchOperation.getOperation()) {
                    case PatchOperation.OPERATION_REPLACE: {
                        String name = getFieldName(patchOperation.getField());

                        if (!patchableAttributes.contains(name)) {
                            return new BadRequestException("For the object type "
                                    + IdentityRestUtils.USER_TYPE
                                    + ", field \""
                                    + name
                                    + "\" cannot be altered by PATCH").asPromise();
                        }

                        JsonValue value = patchOperation.getValue();
                        newAttributeMap.put(name, identityAttributeJsonToSet(value));
                        updateNeeded = true;
                        break;
                    }
                    default:
                        return new BadRequestException("PATCH of "
                                + IdentityRestUtils.USER_TYPE
                                + " does not support operation "
                                + patchOperation.getOperation()).asPromise();
                }
            }

            if (updateNeeded) {
                identityDetails.setAttributes(mapToAttributes(newAttributeMap));
                identityServices.update(identityDetails, ssoToken);

                // re-read the altered identity details from the repo.
                identityDetails = identityServices.read(resourceId,
                        getIdentityServicesAttributes(realm, objectType),
                        ssoToken);
            }
            return newResultPromise(newResourceResponse("result", "1",
                    identityDetailsToJsonValue(identityDetails)));

        } catch (final ObjectNotFound notFound) {
            logger.error("IdentityResourceV3.patchInstance cannot find resource " + resourceId, notFound);
            return new NotFoundException("Resource cannot be found.", notFound).asPromise();
        } catch (final TokenExpired tokenExpired) {
            logger.error("IdentityResourceV3.patchInstance, token expired", tokenExpired);
            return new PermanentException(401, "Unauthorized", null).asPromise();
        } catch (final AccessDenied accessDenied) {
            logger.error("IdentityResourceV3.patchInstance, access denied", accessDenied);
            return new ForbiddenException(accessDenied.getMessage(), accessDenied).asPromise();
        } catch (final GeneralFailure generalFailure) {
            logger.error("IdentityResourceV3.patchInstance, general failure " + generalFailure.getMessage());
            return new BadRequestException(generalFailure.getMessage(), generalFailure).asPromise();
        } catch (ForbiddenException fex) {
            logger.warning("IdentityResourceV3.patchInstance, insufficient privileges.", fex);
            return fex.asPromise();
        } catch (NotFoundException notFound) {
            logger.warning("IdentityResourceV3.patchInstance " + resourceId + " not found", notFound);
            return new NotFoundException("Resource " + resourceId + " cannot be found.", notFound).asPromise();
        } catch (ResourceException resourceException) {
            logger.warning("IdentityResourceV3.patchInstance caught ResourceException", resourceException);
            return resourceException.asPromise();
        } catch (Exception exception) {
            logger.error("IdentityResourceV3.patchInstance caught exception", exception);
            return new InternalServerErrorException(exception.getMessage(), exception).asPromise();
        }
    }

    /**
     * Convert attributes into a map.
     * @param attributes The attributes to convert.
     * @return The map.
     */
    private Map<String, Set<String>> attributesToMap(Attribute[] attributes) {
        Map<String, Set<String>> result = new HashMap<>();

        for (Attribute attribute : attributes) {
            result.put(attribute.getName(), new HashSet<>(Arrays.asList(attribute.getValues())));
        }
        return result;
    }

    /**
     * Convert a map back into an array of attributes.
     * @param map The map to convert.
     * @return The, possibly empty, array of attributes.
     */
    private Attribute[] mapToAttributes(Map<String, Set<String>> map) {
        Attribute[] result = new Attribute[map.size()];
        int index = 0;
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            result[index] = new Attribute();
            result[index].setName(entry.getKey());
            result[index].setValues(entry.getValue().toArray(new String[0]));
            index++;
        }
        return result;
    }

    /**
     * Get the field name of a JsonPointer.
     * @param field The field.
     * @return The field name.
     */
    private String getFieldName(JsonPointer field) {
        String result = field.toString();
        if (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }

    /**
     * @return the identity services implementation.
     */
    private IdentityServicesImpl getIdentityServices() {
        return identityServices;
    }
}
