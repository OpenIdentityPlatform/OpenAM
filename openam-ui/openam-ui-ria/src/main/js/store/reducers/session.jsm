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

import {
    SESSION_ADD_INFO,
    SESSION_REMOVE_INFO
} from "../actions/types";

import _ from "lodash";

const initialState = {
    realm: undefined,
    maxidletime: undefined
};

const session = function (state = initialState, action) {
    switch (action.type) {
        case SESSION_ADD_INFO: {
            const sessionInfo = {};
            const secondsInMinute = 60;

            if (action.info.maxidletime) { sessionInfo.maxidletime = action.info.maxidletime * secondsInMinute; }
            if (action.info.realm) { sessionInfo.realm = action.info.realm.toLowerCase(); }

            return _.merge({}, state, sessionInfo);
        }

        case SESSION_REMOVE_INFO: return {};
        default: return state;
    }
};

export default session;
