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
 * Portions copyright 2014-2016 ForgeRock AS.
 */

import _ from "lodash";

import { sessionAddInfo } from "store/actions/creators";
import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import store from "store/index";
import moment from "moment";

const obj = new AbstractDelegate(`${Constants.host}/${Constants.context}/json/sessions`);
const getSessionInfo = (token, options) => {
    return obj.serviceCall(_.merge({
        url: `?_action=getSessionInfo&tokenId=${token}`,
        type: "POST",
        data: {},
        headers: {
            "Accept-API-Version": "protocol=1.0,resource=2.0"
        }
    }, options));
};

export const getTimeLeft = (token) => {
    return getSessionInfo(token, { suppressSpinner: true }).then((sessionInfo) => {
        const idleExpiration = moment(sessionInfo.maxIdleExpirationTime).diff(moment(), "seconds");
        const maxExpiration = moment(sessionInfo.maxSessionExpirationTime).diff(moment(), "seconds");
        return _.min([idleExpiration, maxExpiration]);
    });
};

export const updateSessionInfo = (token, options) => {
    return getSessionInfo(token, options).then((response) => {
        store.dispatch(sessionAddInfo({
            realm: response.realm,
            sessionHandle: response.sessionHandle
        }));
        return response;
    });
};

export const isSessionValid = (token) => getSessionInfo(token).then((response) => _.has(response, "username"));

export const logout = (token) => {
    return obj.serviceCall({
        url: `?_action=logout&tokenId=${token}`,
        type: "POST",
        data: {},
        headers: {
            "Accept-API-Version": "protocol=1.0,resource=2.0"
        },
        errorsHandlers: { "Bad Request": { status: 400 } }
    });
};
