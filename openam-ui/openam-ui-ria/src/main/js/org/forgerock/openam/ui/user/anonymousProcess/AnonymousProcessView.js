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

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/user/delegates/AnonymousProcessDelegate",
    "org/forgerock/commons/ui/user/anonymousProcess/AnonymousProcessView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/RealmHelper",
    "org/forgerock/commons/ui/common/main/EventManager"
], ($, _, AnonymousProcessDelegate, AnonymousProcessView, Router, Constants, RealmHelper, EventManager) => {

    return AnonymousProcessView.extend({

        render () {
            var params = Router.convertCurrentUrlToJSON().params,
                overrideRealm = RealmHelper.getOverrideRealm(),
                subRealm = RealmHelper.getSubRealm(),
                endpoint = this.endpoint,
                realmPath = "/",
                continueRoute;

            this.events["click #anonymousProcessReturn"] = "returnToLoginPage";

            if (endpoint === Constants.SELF_SERVICE_REGISTER) {
                continueRoute = Router.configuration.routes.continueSelfRegister;
            } else if (endpoint === Constants.SELF_SERVICE_RESET_PASSWORD) {
                continueRoute = Router.configuration.routes.continuePasswordReset;
            }

            if (overrideRealm && overrideRealm !== "/") {
                const slicedOverrideRealm = overrideRealm.substring(0, 1) === "/"
                    ? overrideRealm.slice(1)
                    : overrideRealm;
                endpoint = `${slicedOverrideRealm}/${endpoint}`;
                realmPath = overrideRealm;
            } else if (!overrideRealm && subRealm) {
                endpoint = `${subRealm}/${endpoint}`;
                realmPath = subRealm;
            }

            realmPath = realmPath.substring(0, 1) === "/" ? realmPath : `/${realmPath}`;

            if (!this.delegate || Router.currentRoute !== continueRoute) {
                this.setDelegate(`json/${endpoint}`, params.token);
            }

            if (params.token) {
                this.submitDelegate(params, () => {
                    Router.routeTo(continueRoute, { args: [realmPath], trigger: true });
                });
                return;
            }

            this.setTranslationBase();
            this.parentRender();
        },

        returnToLoginPage (e) {
            e.preventDefault();
            location.href = e.target.href + RealmHelper.getSubRealm();
        },

        restartProcess (e) {
            e.preventDefault();
            delete this.delegate;
            delete this.stateData;
            const subrealm = RealmHelper.getSubRealm();

            EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                route: _.extend({}, Router.currentRoute, { forceUpdate: true }),
                args: [`/${subrealm}`]
            });
        }
    });
});
