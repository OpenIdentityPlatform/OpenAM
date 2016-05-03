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
  * @module org/forgerock/openam/ui/common/util/Promise
  */
define([
    "jquery",
    "lodash"
], ($, _) => ({
    /**
     * Returns a promise that resolves when all of the promises in the iterable argument have resolved, or rejects
     * with the reason of the first passed promise that rejects.
     * @param {Array} promises An array of promises
     * @returns {Promise} A promise that represents all of the specified promises
     * @see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/all
     */
    all (promises) {
        if (_.isArray(promises)) {
            if (promises.length) {
                return $.when(...promises).then(function () {
                    const args = Array.prototype.slice.call(arguments);

                    if (args.length === 1 || promises.length !== 1) {
                        return args;
                    }

                    return [args];
                });
            } else {
                return $.Deferred().resolve([]).promise();
            }
        } else {
            return $.Deferred().reject(new TypeError("Expected an array of promises")).promise();
        }
    }
}));
