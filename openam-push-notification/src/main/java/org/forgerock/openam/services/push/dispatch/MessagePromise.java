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

import java.util.HashSet;
import java.util.Set;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.PromiseImpl;

/**
 * A MessagePromise is an encapsulation of a promise and a set of predicates that must be (successfully) applied
 * to the contents of any supposed response to this message from an external source.
 */
public class MessagePromise {

    private final PromiseImpl<JsonValue, Exception> promise;
    private final Set<Predicate> predicates = new HashSet<>();

    /**
     * Generate a new MessagePromise with the supplied promise and predicates.
     *
     * @param promise The promise, likely retrieved from the MessageDispatcher or similar.
     * @param predicates Predicates to be applied to the contents of the response to this message.
     */
    public MessagePromise(PromiseImpl<JsonValue, Exception> promise, Set<Predicate> predicates) {
        Reject.ifNull(predicates);
        Reject.ifNull(promise);
        this.predicates.addAll(predicates);
        this.promise = promise;
    }

    /**
     * Informs whether there are any predicates set on this MessagePromise.
     *
     * @return true if there are predicates, false otherwise.
     */
    public boolean hasPredicates() {
        return !CollectionUtils.isEmpty(predicates);
    }

    /**
     * Retrieve the set of predicates which should be applied to the contents of any supposed response to
     * this message.
     *
     * @return a set of predicates.
     */
    public Set<Predicate> getPredicates() {
        return predicates;
    }

    /**
     * Retrieve the internally stored promise implementation.
     *
     * @return The stored promise implementation.
     */
    public PromiseImpl<JsonValue, Exception> getPromise() {
        return promise;
    }
}
