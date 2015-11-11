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

define("org/forgerock/openam/ui/common/util/NavigationHelper", [
    "lodash",
    "jquery",
    "org/forgerock/commons/ui/common/components/Navigation",
    "org/forgerock/openam/ui/admin/delegates/SMSGlobalDelegate",
    "org/forgerock/commons/ui/common/main/Router"
], function (_, $, Navigation, SMSGlobalDelegate, Router) {
    return {
        populateRealmsDropdown: function () {
            var maxRealms = 4,
                name;

            SMSGlobalDelegate.realms.all().done(function (data) {
                _(data.result).filter("active").sortBy("path").take(maxRealms).forEach(function (realm) {
                    name = realm.name === "/" ? $.t("console.common.topLevelRealm") : realm.name;
                    Navigation.addLink({
                        "url": "#" + Router.getLink(Router.configuration.routes.realmDefault,
                            [encodeURIComponent(realm.path)]),
                        "name": name,
                        "cssClass": "dropdown-sub"
                    }, "admin", "realms");
                }).run();

                Navigation.addLink({
                    "url": "#" + Router.getLink(Router.configuration.routes.realms),
                    "name": $.t("config.AppConfiguration.Navigation.links.realms.viewAll"),
                    "cssClass": "dropdown-sub"
                }, "admin", "realms");

                Navigation.reload();
            });
        }
    };
});
