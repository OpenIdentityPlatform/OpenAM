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
    "lodash",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/URIUtils",
    "org/forgerock/commons/ui/user/anonymousProcess/AnonymousProcessView",
    "org/forgerock/commons/ui/user/delegates/AnonymousProcessDelegate",
    "org/forgerock/openam/ui/common/services/fetchUrl",
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/uri/query",
    "store/index"
], (_, EventManager, Router, URIUtils, AnonymousProcessView, AnonymousProcessDelegate, fetchUrl, Constants, query,
    store) => {

    function getFragmentParamString () {
        const params = URIUtils.getCurrentFragmentQueryString();
        return _.isEmpty(params) ? "" : `&${params}`;
    }

    function getNextRoute (endpoint) {
        if (endpoint === Constants.SELF_SERVICE_REGISTER) {
            return Router.configuration.routes.continueSelfRegister;
        } else if (endpoint === Constants.SELF_SERVICE_RESET_PASSWORD) {
            return Router.configuration.routes.continuePasswordReset;
        }
        return "";
    }

    function isFromEmailLink (params) {
        return params.token;
    }

    return AnonymousProcessView.extend({

        render () {
            const fragmentParams = query.parseParameters(URIUtils.getCurrentFragmentQueryString());
            const nextRoute = getNextRoute(this.endpoint);
            const endpoint = fetchUrl.default(`/${this.endpoint}`, { realm: store.default.getState().server.realm });

            if (!this.delegate || Router.currentRoute !== nextRoute) {
                this.setDelegate(`json${endpoint}`, fragmentParams.token);
            }

            if (isFromEmailLink(fragmentParams)) {
                this.submitDelegate(fragmentParams, () => {
                    Router.routeTo(nextRoute, { trigger: true });
                });
            } else {
                // TODO: The first undefined argument is the deprecated realm which is defined in the
                // CommonRoutesConfig login route. This needs to be removed as part of AME-11109.
                this.data.args = [undefined, getFragmentParamString()];
                this.setTranslationBase();
                this.parentRender();
            }
        },

        restartProcess (e) {
            e.preventDefault();
            delete this.delegate;
            delete this.stateData;

            EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                args: this.data.args,
                route: _.extend({}, Router.currentRoute, { forceUpdate: true })
            });
        }
    });
});
