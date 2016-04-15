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
package org.forgerock.openam.services.push.gcm;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openam.services.push.PushMessage.*;
import static org.forgerock.util.promise.Promises.*;

import com.sun.identity.shared.debug.Debug;
import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.annotations.Action;
import org.forgerock.json.resource.annotations.RequestHandler;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.services.push.dispatch.MessageDispatcher;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;

/**
 * An endpoint, created and attached to the Router at the point of a new {@link GcmHttpDelegate} being
 * configured to accept inbound messages from a remote device over GCM.
 *
 * {@see GcmHttpDelegate}.
 * {@see PushNotificationService}.
 */
@RequestHandler
public class GcmMessageResource {

    private static final JsonPointer MESSAGE_ID_POINTER = new JsonPointer("data/" + MESSAGE_ID);

    private final MessageDispatcher messageDispatcher;
    private final Debug debug;

    /**
     * Generate a new GcmMessageResource using the provided MessageDispatcher.
     * @param messageDispatcher Used to deliver messages recieved at this endpoint to their appropriate locations
     *                          within OpenAM.
     * @param debug For writing out debug messages.
     */
    @Inject
    public GcmMessageResource(MessageDispatcher messageDispatcher, @Named("frPush") Debug debug) {
        this.messageDispatcher = messageDispatcher;
        this.debug = debug;
    }

    /**
     * Forwards the provided message - so long as it has a messageId in the appropriate place - to the
     * messageDispatcher which is responsible for informing the appropriate receiver in OpenAM of the
     * data received in the message.
     *
     * @param context Context of this request used to retrieve the current realm location.
     * @param actionRequest The request triggering the dispatch.
     * @return An empty HTTP 200 if everything was okay, or an HTTP 400 if the request was malformed.
     */
    @Action
    public Promise<ActionResponse, ResourceException> send(Context context, ActionRequest actionRequest) {
        Reject.ifFalse(context.containsContext(RealmContext.class));

        final JsonValue actionContent = actionRequest.getContent();

        String realm = context.asContext(RealmContext.class).getResolvedRealm();
        String messageId = actionContent.get(MESSAGE_ID_POINTER).asString();

        if (messageId == null) {
            debug.warning("Received message in realm {} with invalid messageId.", realm);
            return RestUtils.generateBadRequestException();
        }

        try {
            messageDispatcher.handle(messageId, actionRequest.getContent());
        } catch (NotFoundException e) {
            debug.warning("Unable to deliver message with messageId {} in realm {}.", messageId, realm, e);
            return RestUtils.generateBadRequestException(); //drop down to CTS [?]
        }

        return newResultPromise(newActionResponse(json(object())));
    }
}
