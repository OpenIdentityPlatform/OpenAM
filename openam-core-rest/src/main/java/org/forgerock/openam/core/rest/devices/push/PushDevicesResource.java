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

package org.forgerock.openam.core.rest.devices.push;

import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.util.promise.Promises.*;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import javax.inject.Inject;
import javax.inject.Named;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.rest.devices.TwoFADevicesResource;
import org.forgerock.openam.core.rest.devices.UserDevicesResource;
import org.forgerock.openam.core.rest.devices.services.push.AuthenticatorPushServiceFactory;
import org.forgerock.openam.core.rest.devices.services.push.AuthenticatorPushService;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * A user devices resource for Push authentication devices.
 *
 * @since 13.5.0
 * @see UserDevicesResource
 */
public class PushDevicesResource extends TwoFADevicesResource<PushDevicesDao> {

    private final AuthenticatorPushServiceFactory pushServiceFactory;
    private final Debug debug;

    /**
     * Construct a new PushDevicesResource endpoint.
     *
     * @param dao The data access object for PushDevices.
     * @param helper The ContextHelper used to determine the URL's subject.
     * @param debug A debug instance for logging.
     * @param pushServiceFactory The Push Service Factory used to get the push service for this realm.
     */
    @Inject
    public PushDevicesResource(PushDevicesDao dao, ContextHelper helper,
                               @Named("frRest") Debug debug, AuthenticatorPushServiceFactory pushServiceFactory) {
        super(dao, helper);
        this.debug = debug;
        this.pushServiceFactory = pushServiceFactory;
    }

    /**
     * Only supports the "reset" action.
     *
     * The "reset" action will remove all Push devices from the user's profile attributes.
     *
     * A valid response will take the form:
     *
     * { "result" : true }
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {

        try {
            final AMIdentity identity = getUserIdFromUri(context); //could be admin
            final AuthenticatorPushService realmPushService = pushServiceFactory.create(getRealm(context));

            switch (request.getAction()) {
                case RESET: //deletes their profile attribute
                    try {
                        realmPushService.removeAllUserDevices(identity);
                        return newResultPromise(newActionResponse(JsonValueBuilder.jsonValue().put(RESULT, true)
                                .build()));

                    } catch (SSOException | IdRepoException e) {
                        debug.error("PushDevicesResource :: Action - Unable to reset identity attributes", e);
                        return new InternalServerErrorException().asPromise();
                    }
                default:
                    return new NotSupportedException().asPromise();
            }

        } catch (SMSException e) {
            debug.error("PushDevicesResource :: Action - Unable to communicate with the SMS.", e);
            return new InternalServerErrorException().asPromise();
        } catch (SSOException | InternalServerErrorException e) {
            debug.error("PushDevicesResource :: Action - Unable to retrieve identity data from request context", e);
            return new InternalServerErrorException().asPromise();
        }
    }
}