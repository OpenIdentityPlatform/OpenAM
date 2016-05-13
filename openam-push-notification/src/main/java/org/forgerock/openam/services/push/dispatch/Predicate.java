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

import org.forgerock.json.JsonValue;

/**
 * An interface for a basic, stand-alone predicate.
 */
public interface Predicate {

    /**
     * Execute the predicate against the given Json content and return the predicate's success/failure.
     *
     * @param content against which the predicate can be performed.
     * @return whether the predicate passed or not.
     */
    boolean perform(JsonValue content);

    /**
     * Returns a jsonified representation of this object to be used when tranmitting across cluster.
     *
     * @return A jsonified representation of this class instance.
     */
    String jsonify();

}
