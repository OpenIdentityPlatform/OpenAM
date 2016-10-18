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
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"
], function ($, _, Messages, EventManager, Constants) {

    /**
     * @exports org/forgerock/openam/ui/admin/utils/ModelUtils
     */
    var obj = {};

    function hasError (response) {
        return _.has(response, "responseJSON.message") && _.isString(response.responseJSON.message);
    }

    obj.errorHandler = function (response) {
        if (_.get(response, "status") === 401) {
            EventManager.sendEvent(Constants.EVENT_UNAUTHORIZED, { error: response.error() });
        } else if (hasError(response)) {
            Messages.addMessage({ type: Messages.TYPE_DANGER, escape: true, response });
        } else {
            Messages.addMessage({ type: Messages.TYPE_DANGER, message: $.t("config.messages.CommonMessages.unknown") });
        }
    };

    return obj;
});
