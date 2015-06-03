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
 * Copyright 2015 ForgeRock AS.
 */

/*global define*/

define("config/messages/ScriptsMessages", [], function () {
    return {
        "scriptCreated": {
            msg: "config.messages.ScriptsMessages.scriptCreated",
            type: "info"
        },
        "scriptUpdated": {
            msg: "config.messages.ScriptsMessages.scriptUpdated",
            type: "info"
        },
        "validationNoScript": {
            msg: "config.messages.ScriptsMessages.validation.noScript",
            type: "info"
        },
        "scriptErrorNoName": {
            msg: "config.messages.ScriptsMessages.error.noName",
            type: "error"
        },
        "scriptErrorNoScript": {
            msg: "config.messages.ScriptsMessages.error.noScript",
            type: "error"
        }
    };
});