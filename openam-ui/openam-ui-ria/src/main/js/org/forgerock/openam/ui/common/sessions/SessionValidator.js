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
  * @module org/forgerock/openam/ui/common/sessions/SessionValidator
  */
define("org/forgerock/openam/ui/common/sessions/SessionValidator", [
    "org/forgerock/openam/ui/common/RouteTo"
], function (RouteTo) {
    var delay, ONE_SECOND_IN_MILLISECONDS = 1000, SESSION_ALMOST_EXPIRED_BACKOFF_SECONDS = 1;

    function stop () {
        clearTimeout(delay);

        delay = null;
    }

    function validate (strategy, token, seconds) {
        delay = setTimeout(function () {
            strategy(token).then(function (seconds) {
                /**
                 * If we're within the window of 0 seconds left on the session but still monumentality valid,
                 * backoff the next schedule by a predetermined number of seconds. Avoids an immediate schedule.
                 */
                var adjustedSeconds = seconds > 0 ? seconds : SESSION_ALMOST_EXPIRED_BACKOFF_SECONDS;

                validate(strategy, token, adjustedSeconds);
            }, function () {
                stop();

                RouteTo.sessionExpired();
            });
        }, seconds * ONE_SECOND_IN_MILLISECONDS);
    }

    return {
        /**
         * Starts the periodic session validation progress using the specified strategy.
         * @param {string}   token SSO token to validate
         * @param {org/forgerock/openam/ui/common/sessions/SessionValidator~Strategy} strategy Strategy to use to
         * perform validation
         */
        start: function (token, strategy) {
            if (delay) { throw new Error("Validator has already been started"); }

            validate(strategy, token, 0);
        },
        /**
         * Stops the periodic session validation progress.
         */
        stop: stop
    };
});

/**
 * Interface that strategies must adhere to
 * @callback Strategy
 * @memberOf module:org/forgerock/openam/ui/common/sessions/SessionValidator
 * @param {string} token SSO token to validate
 * @returns {Promise} when resolved, promise must pass a single argument with the seconds until the next check
 */
