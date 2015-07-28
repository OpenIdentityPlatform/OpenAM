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
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.rest.devices.services.OathService;
import org.forgerock.openam.rest.devices.services.OathServiceFactory;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.rest.resource.SSOTokenContext;
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

    private final OathServiceFactory oathServiceFactory;
    private final Debug debug;

    @Inject
    public OathDevicesResource(OathDevicesDao dao, ContextHelper helper,
                               @Named("frRest") Debug debug, OathServiceFactory oathServiceFactory) {
        super(dao, helper);
        this.debug = debug;
        this.oathServiceFactory = oathServiceFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {

        try {
            final AMIdentity identity = getIdentity(context);
            final OathService realmOathService = oathServiceFactory.create(getRealm(context));

            switch (request.getAction()) {
                case SKIP:

                    try {
                        final boolean setValue = request.getContent().get(VALUE).asBoolean();

                        realmOathService.setUserSkipOath(identity, setValue);
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
                default:
                    handler.handleError(ResourceException.getException(ResourceException.NOT_SUPPORTED));
            }

        } catch (SMSException e) {
            debug.error("OathDevicesResource :: Action - Unable to communicate with the SMS.", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
        } catch (SSOException | IdRepoException e) {
            debug.error("OathDevicesResource :: Action - Unable to retrieve identity data from request context", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
        }
    }

    @VisibleForTesting
    protected AMIdentity getIdentity(ServerContext context) throws SSOException, IdRepoException {
        final SSOTokenContext ssoContext = context.asContext(SSOTokenContext.class);
        return new AMIdentity(ssoContext.getCallerSSOToken());
    }
}