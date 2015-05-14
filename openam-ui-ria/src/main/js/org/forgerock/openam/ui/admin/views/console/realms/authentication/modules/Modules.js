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
 * Copyright 2015 ForgeRock AS.
 */

/*global define, $, _*/

define("org/forgerock/openam/ui/admin/views/console/realms/authentication/modules/Modules", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants"
], function(AbstractView, Configuration, EventManager, Router, Constants) {

    var Module = AbstractView.extend({
        template: "templates/admin/views/console/realms/authentication/ModuleTemplate.html",
        baseTemplate: "templates/common/DefaultBaseTemplate.html",
        events: {},

        render: function(args, callback) {
            this.data.realm = Configuration.globalData.auth.subRealm || "Top level Realm";
            this.data.name = args[0];
            this.data.consolePath = Constants.CONSOLE_PATH;
            this.parentRender();
        }

    });

    return new Module();
});
