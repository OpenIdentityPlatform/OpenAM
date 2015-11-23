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

define("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/delegates/SMSGlobalDelegate",
    "org/forgerock/openam/ui/common/components/TreeNavigation"
], function ($, _, Constants, EventManager, Router, SMSGlobalDelegate, TreeNavigation) {
    var RealmTreeNavigationView = TreeNavigation.extend({
        events: {
            "click [data-event]": "sendEvent"
        },
        sendEvent: function (e) {
            e.preventDefault();
            EventManager.sendEvent($(e.currentTarget).data().event, this.data.realmPath);
        },
        template: "templates/admin/views/realms/RealmTreeNavigationTemplate.html",
        realmExists: function (path) {
            return SMSGlobalDelegate.realms.get(path);
        },
        render: function (args, callback) {
            var self = this;

            this.data.realmPath = args[0];
            this.data.realmName = this.data.realmPath === "/" ? $.t("console.common.topLevelRealm")
                : this.data.realmPath;

            this.realmExists(this.data.realmPath).done(function () {
                TreeNavigation.prototype.render.call(self, args, callback);
            }).fail(function (xhr) {
                /**
                 * If a non-existant realm was specified, return to realms list
                 */
                if (xhr.status === 404) {
                    Router.routeTo(Router.configuration.routes.realms, {
                        args: [],
                        trigger: true
                    });
                }
            });
        }
    });

    return new RealmTreeNavigationView();
});
