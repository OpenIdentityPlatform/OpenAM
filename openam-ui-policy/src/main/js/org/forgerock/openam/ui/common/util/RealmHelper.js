/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS.
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
    "org/forgerock/commons/ui/common/main/Router"
], function(Router) {
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
     * Determines the current override realm from the URI query string
     * @returns Override realm AS IS (no slash modification) (e.g. <code>/</code> or <code>/realm1</code>)
     */
    obj.getOverrideRealm = function() {
        return Router.convertQueryParametersToJSON(Router.getURIQueryString()).realm;
    };

    return obj;
});
