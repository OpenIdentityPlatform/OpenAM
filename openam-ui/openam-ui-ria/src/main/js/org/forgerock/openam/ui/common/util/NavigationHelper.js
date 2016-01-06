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

            // Remove any previously added dynamic navigation links.
            // The reason why this is required is because we override the values in the AppConfiguration when we
            // add new links at runtime via the common Navigation module. This stops us from being able to reset the
            // navigation's configuration upon log out or session end. Which in turn means that the next user to log in
            // will get the altered configuration.
            // FIXME: The correct fix would be to change the way the Navigation works so that the original configuration
            // remains intact and we just call Navigation.reset() when a users session ends or a new one begins.
            if (_.has(Navigation.configuration, "links.admin.urls.realms.urls")) {
                Navigation.configuration.links.admin.urls.realms.urls = _.reject(
                    Navigation.configuration.links.admin.urls.realms.urls, "dynamicLink", true);
            }

            SMSGlobalDelegate.realms.all().done(function (data) {
                _(data.result).filter("active").sortBy("path").take(maxRealms).forEach(function (realm) {
                    name = realm.name === "/" ? $.t("console.common.topLevelRealm") : realm.name;
                    Navigation.addLink({
                        "url": "#" + Router.getLink(Router.configuration.routes.realmDefault,
                            [encodeURIComponent(realm.path)]),
                        "name": name,
                        "cssClass": "dropdown-sub",
                        "dynamicLink": true
                    }, "admin", "realms");
                }).run();

                Navigation.addLink({
                    "url": "#" + Router.getLink(Router.configuration.routes.realms),
                    "name": $.t("config.AppConfiguration.Navigation.links.realms.viewAll"),
                    "cssClass": "dropdown-sub",
                    "dynamicLink": true
                }, "admin", "realms");

                Navigation.reload();
            });
        }
    };
});
