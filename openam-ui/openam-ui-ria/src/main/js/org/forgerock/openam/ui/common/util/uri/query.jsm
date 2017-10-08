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
 * Copyright 2016 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/common/util/uri/query
 */
import _ from "lodash";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";

/**
 * @description Creates an object of key value pairs from the passed in query string
 * @param {String} paramString A string containing a query string
 * @returns {Object} An Object of key value pairs
 */
export function parseParameters (paramString) {
    const object = _.isEmpty(paramString) ? {} : _.object(_.map(paramString.split("&"), (pair) => {
        const key = pair.substring(0, pair.indexOf("="));
        const value = pair.substring(pair.indexOf("=") + 1);
        return [key, value];
    }));
    return object;
}

/**
 * @description Creates an object of key value pairs from the current url query
 * @returns {Object} An Object of key value pairs from the current url query
 */
export function getCurrentQueryParameters () {
    return this.parseParameters(URIUtils.getCurrentQueryString());
}

/**
 * @description Creates query string from an object of key value pairs
 * @param {Object} paramsObject An object of key value pairs
 * @returns {String} A query string.
 */
export function urlParamsFromObject (paramsObject) {
    if (_.isEmpty(paramsObject)) {
        return "";
    }
    return _.map(paramsObject, (value, key) => `${key}=${value}`).join("&");
}
