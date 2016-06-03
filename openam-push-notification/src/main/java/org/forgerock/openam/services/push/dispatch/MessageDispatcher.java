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
package org.forgerock.openam.services.push.dispatch;

import com.google.inject.name.Named;
import com.sun.identity.shared.debug.Debug;
import java.util.Set;
import javax.inject.Inject;
import org.forgerock.guava.common.cache.Cache;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.PromiseImpl;

/**
 * The MessageDispatcher acts to forward messages between disparate locations in OpenAM via
 * a unique identifier and an aysnc interface. The MessageDispatcher should initially be
 * told to expect to receive a message with a given id - this will return an incomplete promise which
 * may or may not later be updated by this dispatcher; and store it in a cache this promise under a key of
 * its messageId.
 *
 * Later, the MessageDispatcher may be asked to handle a message with a messageId and its JsonValue contents.
 *
 * If this messageId is expected, then the promise associated with that messageId will be asked whether it has
 * any predicates:
 *
 *  -   If it does not, the promise will complete, and be removed from the cache.
 *  -   If it does, then all the predicates will be run - one at a time, and if any of them return boolean false
 *          then the MessagePromise will NOT be completed, and the MessageDispatcher will continue to await
 *          a valid message in response to that messageId.
 */
public class MessageDispatcher {

    private final Cache<String, MessagePromise> cache;
    private final Debug debug;

    /**
     * A message dispatcher which holds a Cache (a timeout-based Map) which contains the
     * promises which have yet to be returned to their instantiators.
     *
     * @param dispatch A cache to store messages which will shortly be dispatched.
     * @param debug for writing debug messages.
     */
    @Inject
    public MessageDispatcher(Cache<String, MessagePromise> dispatch, @Named("frPush") Debug debug) {
        this.cache = dispatch;
        this.debug = debug;
    }

    /**
     * Handles the message passed to the dispatcher with the provided messageId.
     *
     * @param messageId The messageId of the promise to complete. May not be null.
     * @param content The contents to complete the awaiting promise with. May not be null.
     * @throws NotFoundException If the provided was messageId was null.
     * @throws PredicateNotMetException If any expected-successful predicate fails.
     */
    public void handle(String messageId, JsonValue content) throws NotFoundException, PredicateNotMetException {
        Reject.ifNull(content);
        Reject.ifNull(messageId);
        MessagePromise messagePromise = cache.getIfPresent(messageId);
        if (messagePromise != null) {
            for (Predicate p : messagePromise.getPredicates()) {
                if (!p.perform(content)) {
                    throw new PredicateNotMetException("Predicate was not matched. Message invalid.");
                }
            }

            messagePromise.getPromise().tryHandleResult(content);
            cache.invalidate(messageId);
        } else {
            debug.warning("Cache was asked to handle {} but never expected it.", messageId);
            throw new NotFoundException("This cache was not primed for this messageId.");
        }
    }

    /**
     * Tells the message dispatcher to expect a message to be handled with the given messageId. This returns
     * an incomplete promise.
     *
     * @param messageId The messageId to inform this cache to prepare to handle. May not be null or empty.
     * @param predicates The predicates that must be run against the content of any response to this messageId.
     * @return A promise which will later be completed by this MessageDispatcher handling a JsonValue for the messageId.
     */
    public MessagePromise expect(String messageId, Set<Predicate> predicates) {
        Reject.ifTrue(StringUtils.isBlank(messageId));
        Reject.ifNull(predicates);
        MessagePromise mp = new MessagePromise(PromiseImpl.<JsonValue, Exception>create(), predicates);
        cache.put(messageId, mp);
        return mp;
    }

    /**
     * Forgets any promise returned by this cache for the provided messageId. Removes the promise from
     * the cache. Returns true if all of this occurred, false if the provided messageId was not found.
     *
     * @param messageId The messageId to cancel.
     * @return True if the message was forgotten by the cache for this call, false if the message did not exist in
     * the cache.
     */
    public boolean forget(String messageId) {
        MessagePromise messagePromise = cache.getIfPresent(messageId);
        if (messagePromise != null) {
            cache.invalidate(messageId);
            return true;
        }
        return false;
    }
}
