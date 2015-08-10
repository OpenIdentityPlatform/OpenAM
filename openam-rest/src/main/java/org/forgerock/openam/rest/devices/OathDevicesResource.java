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

package org.forgerock.openam.rest.devices;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import java.util.Collections;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.rest.devices.services.OathService;
import org.forgerock.openam.rest.devices.services.OathServiceFactory;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.util.annotations.VisibleForTesting;

/**
 * A user devices resource for OATH authentication devices.
 *
 * @since 13.0.0
 * @see UserDevicesResource
 */
public class OathDevicesResource extends TwoFADevicesResource<OathDevicesDao> {

    private final static String SKIP = "skip";
    private final static String CHECK = "check";

    private final static String VALUE = "value";
    private final static String RESULT = "result";
    private final static String RESET = "reset";

    private final OathServiceFactory oathServiceFactory;
    private final ContextHelper contextHelper;
    private final Debug debug;

    @Inject
    public OathDevicesResource(OathDevicesDao dao, ContextHelper helper,
                               @Named("frRest") Debug debug, OathServiceFactory oathServiceFactory,
                               ContextHelper contextHelper) {
        super(dao, helper);
        this.debug = debug;
        this.oathServiceFactory = oathServiceFactory;
        this.contextHelper = contextHelper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {

        try {
            final AMIdentity identity = getUserIdFromUri(context); //could be admin
            final OathService realmOathService = oathServiceFactory.create(getRealm(context));

            switch (request.getAction()) {
                case SKIP:

                    try {
                        final boolean setValue = request.getContent().get(VALUE).asBoolean();

                        realmOathService.setUserSkipOath(identity,
                                setValue ? OathService.SKIPPABLE : OathService.NOT_SKIPPABLE);
                        handler.handleResult(JsonValueBuilder.jsonValue().build());

                    } catch (SSOException | IdRepoException e) {
                        debug.error("OathDevicesResource :: SKIP action - Unable to set value in user store.", e);
                        handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
                    }

                    return;
                case CHECK:
                    try {
                        final Set resultSet = identity.getAttribute(realmOathService.getSkippableAttributeName());
                        boolean result = false;

                        if (CollectionUtils.isNotEmpty(resultSet)) {
                            String tmp = (String) resultSet.iterator().next();
                            int resultInt = Integer.valueOf(tmp);
                            if (resultInt == OathService.SKIPPABLE) {
                                result = true;
                            }
                        }

                        handler.handleResult(JsonValueBuilder.jsonValue().put(RESULT, result).build());

                    } catch (SSOException | IdRepoException e) {
                        debug.error("OathDevicesResource :: CHECK action - Unable to read value from user store.", e);
                        handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
                    }
                    return;
                case RESET: //sets their 'skippable' selection to default (NOT_SET) and deletes their profiles attribute
                    try {

                        realmOathService.setUserSkipOath(identity, OathService.NOT_SET);
                        identity.removeAttributes(
                                Collections.singleton(realmOathService.getConfigStorageAttributeName()));
                        identity.store();

                        handler.handleResult(JsonValueBuilder.jsonValue().put(RESULT, true).build());

                    } catch (SSOException | IdRepoException e) {
                        debug.error("OathDevicesResource :: Action - Unable to reset identity attributes", e);
                        handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
                    }

                    return;
                default:
                    handler.handleError(ResourceException.getException(ResourceException.NOT_SUPPORTED));
            }

        } catch (SMSException e) {
            debug.error("OathDevicesResource :: Action - Unable to communicate with the SMS.", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
        } catch (SSOException | InternalServerErrorException e) {
            debug.error("OathDevicesResource :: Action - Unable to retrieve identity data from request context", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
        }
    }

    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
                               ResultHandler<Resource> handler) {

        try {
            final OathService realmOathService = oathServiceFactory.create(getRealm(context));
            final AMIdentity identity = getUserIdFromUri(context); //could be admin

            super.deleteInstance(context, resourceId, request, handler); //make sure we successfully delete

            realmOathService.setUserSkipOath(identity, OathService.NOT_SET); //then reset the skippable attr
        } catch (InternalServerErrorException | SMSException e) {
            debug.error("OathDevicesResource :: Delete - Unable to communicate with the SMS.", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
        } catch (SSOException | IdRepoException e) {
            debug.error("OathDevicesResource :: Delete - Unable to reset identity attributes", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
        }

    }

    /**
     * Retrieving the user id in this fashion ensures that an admin can utilise these functions.
     */
    @VisibleForTesting
    protected AMIdentity getUserIdFromUri(ServerContext context) throws InternalServerErrorException {
        return IdUtils.getIdentity(contextHelper.getUserId(context), contextHelper.getRealm(context));
    }
}