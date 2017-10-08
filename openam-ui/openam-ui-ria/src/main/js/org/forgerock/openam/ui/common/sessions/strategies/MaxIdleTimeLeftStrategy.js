/**
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

 /**
  * Maximum Idle Time vs Session Time Left Strategy.
  * <p/>
  * Supports both stateful and stateless sessions.
  * <p/>
  * This strategy utilises the fact that all calls to the server's <code>session</code> end-point will respond with
  * <code>401</code> if the current session has expired, while the response payload will help to decide when to
  * next perform a validation if the session is still valid.
  * <p/>
  * Subsequent calls to <code>getTimeLeft</code> are used to check if the idle timeout has been reached. The number of
  * seconds until the idle timeout is used to determine the next best time to check the session.
  *
  * @module org/forgerock/openam/ui/common/sessions/strategies/MaxIdleTimeLeftStrategy
  */
define([
    "org/forgerock/openam/ui/user/services/SessionService"
], (SessionService) => {

    return function (token) {
        return SessionService.getTimeLeft(token);
    };
});
