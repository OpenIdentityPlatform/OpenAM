/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014-2015 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global _ define*/
define('org/forgerock/openam/ui/common/util/RealmHelper', [
    'org/forgerock/commons/ui/common/main/Configuration',
    'org/forgerock/commons/ui/common/main/Router'
], function(Configuration, Router) {
    /**
     * @exports org/forgerock/openam/ui/common/util/RealmHelper
     */
    var obj = {};

    /**
     * Decorates a URI with an override realm
     * <p>
     * Appends a realm override to the query string if an override exists
     * @param {String} uri A URI to decorate
     * @returns {String} Decorated URI
     */
    obj.decorateURLWithOverrideRealm = function(uri) {
        var overrideRealm = obj.getOverrideRealm(),
            prepend;

        if(overrideRealm) {
            prepend = uri.indexOf('?') === -1 ? '?' : '&';
            uri = uri + prepend + 'realm=' + overrideRealm;
        }

        return uri;
    };

    /**
     * Decorates a URI with realm information
     * <p>
     * Delegates to #decorateURIWithSubRealm & #decorateURLWithOverrideRealm
     * @param {String} uri A URI to decorate
     * @returns {String} Decorated URI
     */
    obj.decorateURIWithRealm = function(uri) {
        uri = obj.decorateURIWithSubRealm(uri);
        uri = obj.decorateURLWithOverrideRealm(uri);

        return uri;
    };

    /**
     * Decorates a URI with a sub realm
     * <p>
     * Replaces any occurance of '__subrealm__/' in the URI with the sub realm
     * @param {String} uri A URI to decorate
     * @returns {String} Decorated URI
     */
    obj.decorateURIWithSubRealm = function(uri) {
        if(Configuration.globalData && Configuration.globalData.auth && typeof Configuration.globalData.auth.subRealm !== 'string') {
            console.warn('Unable to decorate URI, Configuration.globalData.auth.subRealm not yet set');
        }

        var persistedSubRealm = (Configuration.globalData && Configuration.globalData.auth) ? Configuration.globalData.auth.subRealm : '',
            subRealm = persistedSubRealm ? persistedSubRealm + '/' : '';

        uri = uri.replace('__subrealm__/', subRealm);

        return uri;
    };

    /**
     * Determines the current override realm from the URI query string and hash fragment query string
     * @returns Override realm AS IS (no slash modification) (e.g. <code>/</code> or <code>/realm1</code>)
     */
    obj.getOverrideRealm = function() {
        var uri = Router.convertQueryParametersToJSON(Router.getURIQueryString()).realm, // Realm from URI query string
            fragment = Router.convertQueryParametersToJSON(Router.getURIFragmentQueryString()).realm; // Realm from Fragment query string

        return uri ? uri : fragment;
    };

    /**
     * Determines the current sub realm from the URI hash fragment
     * @returns Sub realm WITHOUT any leading or trailing slash (e.g. <code>realm1/realm2</code>)
     */
    obj.getSubRealm = function() {
        var page,
            subRealm,
            subRealmSplit;

        subRealmSplit = Router.getURIFragment().split('/');
        page = subRealmSplit.shift().split('&')[0];

        if(page && _.include(['login', 'forgotPassword'], page)) {
            subRealm = subRealmSplit.join('/').split('&')[0];
        } else if(Configuration.globalData.auth.subRealm) {
            subRealm = Configuration.globalData.auth.subRealm;
        } else {
            console.warn('Unable to determine realm outside of sub realm aware view (login)');
            subRealm = '';
        }

        return subRealm;
    };

    return obj;
});
