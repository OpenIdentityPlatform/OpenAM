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

package org.forgerock.openam.core.rest.session;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.PatchOperation.OPERATION_REMOVE;
import static org.forgerock.json.resource.PatchOperation.OPERATION_REPLACE;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PARAMETER_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATCH_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.READ_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SESSION_PROPERTIES_RESOURCE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.UPDATE_DESCRIPTION;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Patch;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.SingletonProvider;
import org.forgerock.api.annotations.Update;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.SessionPropertyWhitelist;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;

/**
 * EndPoint for querying the updating the session properties via a Rest interface
 * <p>
 * This endpoint allows GET, PATCH and UPDATE
 *
 * GET expects tokenHash Path Parameter
 * Returns:
 * <code>
 *  {'property1' : 'value1', 'property2' : 'value2'}
 * <code/>
 *
 * PATCH expects tokenId Path Parameter, {'patchOperations' : [{'OPERATION' : 'replace', 'property1' : ,newValue, }]}
 * Returns:
 * <code>
 *  {'property1' : 'newValue', 'property2' : 'value2'}
 * <code/>
 *
 * UPDATE expects tokenId Path Parameter, {'property1' : 'newValue1', 'property2' : 'newValue2'}
 * Returns:
 * <code>
 *  {'property1' : 'newValue1', 'property2' : 'newValue2'}
 * <code/>
 *
 *
 * @since 14.0.0
 */
@SingletonProvider(value = @Handler(
        title = SESSION_PROPERTIES_RESOURCE + TITLE,
        description = SESSION_PROPERTIES_RESOURCE + DESCRIPTION,
        mvccSupported = false,
        resourceSchema = @Schema(schemaResource = "SessionPropertiesResource.schema.json"),
        parameters = @Parameter(
                name = SessionPropertiesResource.TOKEN_HASH_PARAM_NAME,
                type = "string",
                description = SESSION_PROPERTIES_RESOURCE + SessionPropertiesResource.TOKEN_HASH_PARAM_NAME + "." + PARAMETER_DESCRIPTION
        )
    ))
public class SessionPropertiesResource implements SingletonResourceProvider {

    private static final Debug LOGGER = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    /**
     * Path Parameter Name
     */
    public static final String TOKEN_HASH_PARAM_NAME = "tokenHash";
    private static final Set<String> SUPPORTED_OPERATIONS = new HashSet<>(Arrays.asList(new String[]{
                    PatchOperation.OPERATION_REMOVE,
                    PatchOperation.OPERATION_REPLACE}));
    private final SessionPropertyWhitelist sessionPropertyWhitelist;
    private final SessionUtilsWrapper sessionUtilsWrapper;
    private final SessionResourceUtil sessionResourceUtil;
    private final TokenHashToIDMapper hashToIDMapper;

    /**
     * Constructs a new instance of the SessionPropertiesResource
     *   @param sessionPropertyWhitelist An instance of the SessionPropertyWhitelist.
     * @param sessionUtilsWrapper An instance of SessionUtilsWrapper.
     * @param sessionResourceUtil An instance of SessionResourceUtil.
     * @param hashToIDMapper An instance of the TokenHashToIDMapper.
     */
    @Inject
    public SessionPropertiesResource(SessionPropertyWhitelist sessionPropertyWhitelist,
            SessionUtilsWrapper sessionUtilsWrapper, SessionResourceUtil sessionResourceUtil,
            TokenHashToIDMapper hashToIDMapper) {
        this.sessionPropertyWhitelist = sessionPropertyWhitelist;
        this.sessionUtilsWrapper = sessionUtilsWrapper;
        this.sessionResourceUtil = sessionResourceUtil;
        this.hashToIDMapper = hashToIDMapper;
    }

    /**
     * This endpoint does not support action.
     *
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest actionRequest) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * Patch operation to selectively modify the session properties
     *
     * Implementation only supports OPERATION_REMOVE and OPERATION_REPLACE
     *
     * Use OPERATION_REMOVE to set the property to empty
     * Use OPERATION_REPLACE to update the value of a property
     *
     * @param context The context.
     * @param request The Request.
     * @return The response indicating the success or failure of the patch operation
     */
    @Override
    @Patch(operationDescription = @Operation(description = SESSION_PROPERTIES_RESOURCE + PATCH_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest request) {

        List<PatchOperation> operations = request.getPatchOperations();
        String tokenId;
        JsonValue result;
        try {
            ensurePatchPermitted(context, operations);
            tokenId = findTokenIdFromUri(context);
            SSOToken target = getToken(tokenId);
            for (PatchOperation operation : operations) {
                switch (operation.getOperation()) {
                    case OPERATION_REMOVE:
                        target.setProperty(getPatchProperty(operation), "");
                        break;
                    case OPERATION_REPLACE:
                        target.setProperty(getPatchProperty(operation), operation.getValue().asString());
                        break;
                }
            }
            result = getSessionProperties(context, tokenId);
        } catch (BadRequestException | ForbiddenException e) {
            return e.asPromise();
        } catch (SSOException  | IdRepoException e) {
            LOGGER.message("Unable to read session property due to unreadable SSOToken", e);
            return new BadRequestException().asPromise();
        }
        return newResultPromise(newResourceResponse(tokenId, String.valueOf(result.getObject().hashCode()), result));
    }


    /**
     *  This method returns the name value pairs of the session white listed properties if available.
     *
     * @param context The context.
     * @param request The request.
     * @return The name value pairs of the session properties.
     */
    @Override
    @Read(operationDescription = @Operation(description = SESSION_PROPERTIES_RESOURCE + READ_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {
        JsonValue result;
        String tokenId;
        try {
            tokenId = findTokenIdFromUri(context);
            result = getSessionProperties(context, tokenId);
        } catch (SSOException | IdRepoException e) {
            LOGGER.message("Unable to read session property due to unreadable SSOToken", e);
            return new BadRequestException().asPromise();
        }
        return newResultPromise(newResourceResponse(tokenId, String.valueOf(result.getObject().hashCode()), result));
    }

    /**
     *
     * The update modify the entire set of white listed properties,
     * Update wont be permitted if request does not encompass the name value pair of all the white listed properties
     *
     * @param context The context.
     * @param request The Request.
     * @return The response indicating the success or failure of the update operation
     */
    @Override
    @Update(operationDescription = @Operation(description = SESSION_PROPERTIES_RESOURCE + UPDATE_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest request) {
        String tokenId;
        JsonValue result;
        try {
            tokenId = findTokenIdFromUri(context);
            SSOToken target = getToken(tokenId);
            JsonValue content = request.getContent();
            ensureUpdatePermitted(context, content, target);

            for (Map.Entry<String, String> entry : content.asMap(String.class).entrySet()) {
                target.setProperty(entry.getKey(), entry.getValue());
            }
            result = getSessionProperties(context, tokenId);
        } catch (BadRequestException | ForbiddenException e) {
            return e.asPromise();
        } catch (SSOException | IdRepoException e) {
            LOGGER.message("Unable to set session property due to unreadable SSOToken", e);
            return new BadRequestException().asPromise();
        } catch (DelegationException e) {
            LOGGER.message("Unable to read session property due to delegation match internal error", e);
            return new InternalServerErrorException().asPromise();
        }
        return newResultPromise(newResourceResponse(tokenId, String.valueOf(result.getObject().hashCode()), result));
    }

    private String findTokenIdFromUri(Context context) throws SSOException {
        return  hashToIDMapper.map(context, findTokenHashFromUri(context));
    }

    private String findTokenHashFromUri(Context context) {
        if(context == null) {
            return null;
        } else {
            UriRouterContext uriRouterContext;
            try {
                uriRouterContext = context.asContext(UriRouterContext.class);
            } catch (IllegalArgumentException e) {
                // URI context not found, check parent
                return findTokenHashFromUri(context.getParent());
            }
            if(uriRouterContext == null) {
                return findTokenHashFromUri(context.getParent());
            } else if(uriRouterContext.getUriTemplateVariables().get(TOKEN_HASH_PARAM_NAME) == null) {
                return findTokenHashFromUri(context.getParent());
            }
            return uriRouterContext.getUriTemplateVariables().get(TOKEN_HASH_PARAM_NAME);
        }
    }

    private JsonValue getSessionProperties(Context context, String tokenId) throws SSOException, IdRepoException {
        JsonValue result = json(object());
        SSOToken target = getToken(tokenId);
        String realm = getTargetRealm(target);
        for (String property : sessionPropertyWhitelist.getAllListedProperties(realm)) {
            final String value = target.getProperty(property);
            result.add(property, value == null ? "" : value);
        }
        return result;
    }

    private SSOToken getToken(String tokenId) throws SSOException {
        return sessionResourceUtil.getTokenWithoutResettingIdleTime(tokenId);
    }

    private void ensurePatchPermitted(Context context, List<PatchOperation> operations)
            throws ForbiddenException, BadRequestException, SSOException {
        operationsAllowed(operations);
        propertiesModifiable(context, operations);
    }

    private void operationsAllowed(List<PatchOperation> operations) throws BadRequestException {
        for (PatchOperation operation : operations) {
            if (!SUPPORTED_OPERATIONS.contains(operation.getOperation())) {
                LOGGER.warning("Operation {} requested by the user is not allowed.", operation.getOperation());
                throw new BadRequestException();
            }
        }
    }

    private void propertiesModifiable(Context context, List<PatchOperation> operations)
            throws ForbiddenException, SSOException, BadRequestException {
        SSOToken caller = context.asContext(SSOTokenContext.class).getCallerSSOToken();
        for (PatchOperation operation : operations) {
            String property = getPatchProperty(operation);
            try {
                String value = operation.getValue().asString();
                sessionUtilsWrapper.checkPermissionToSetProperty(caller, property, value);
            }  catch (JsonValueException e) {
                LOGGER.warning("Operation {} requested by the user is not allowed.", operation.getOperation());
                throw new BadRequestException();
            } catch (SessionException e) {
                LOGGER.warning("User {} requested patch a property {} which was not whitelisted.",
                        caller.getPrincipal(), property);
                throw new ForbiddenException();
            }
        }
    }

    /**
     * Ensures the update is permitted by checking the request has valid contents.
     * Update is permitted only if the request contains all the white listed properties
     *
     * @param context The context,
     * @param content The request content.
     * @param target  The target session SSOToken
     * @throws SSOException When the SSOToken in invalid
     * @throws BadRequestException When the request has not content
     * @throws DelegationException When is whitelisted check fails
     * @throws ForbiddenException When the content in the request does not match whitelisted properties
     */
    private void ensureUpdatePermitted(Context context, JsonValue content, SSOToken target) throws SSOException,
            BadRequestException, DelegationException, ForbiddenException, IdRepoException {

        SSOToken caller = context.asContext(SSOTokenContext.class).getCallerSSOToken();
        String realm = getTargetRealm(target);

        try {
            if (content == null || content.isNull() || content.asMap(String.class).size() == 0) {
                LOGGER.warning("User {} requested with an empty values.", caller.getPrincipal());
                throw new BadRequestException();
            }
        } catch (JsonValueException e) {
            LOGGER.warning("User {} requested with no property value pairs", caller.getPrincipal());
            throw new BadRequestException();
        }

        Map<String, String> entrySet = content.asMap(String.class);
        if (!sessionPropertyWhitelist.getAllListedProperties(realm).equals(entrySet.keySet())
                || !sessionPropertyWhitelist.isPropertyMapSettable(caller, entrySet)) {
            LOGGER.warning("User {} requested property/ies {} to set on {} which was not whitelisted.",
                    caller.getPrincipal(), target.getPrincipal(), entrySet.toString());
            throw new ForbiddenException();
        }
    }

    private String getTargetRealm(SSOToken ssoToken) throws IdRepoException, SSOException {
        return sessionResourceUtil.convertDNToRealm( sessionResourceUtil.getIdentity(ssoToken).getRealm());
    }

    private String getPatchProperty(PatchOperation operation) {
        String result = operation.getField().toString();
        String prefix = "/";
        if (result.startsWith(prefix)) {
            result = result.substring(prefix.length());
        }
        return result;
    }
}
