/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global define, _ */

define("org/forgerock/openam/ui/common/util/RealmHelper", [
    "underscore",
    'org/forgerock/commons/ui/common/util/UIUtils'
], function(_, uiUtils) {
    /**
     * @exports org/forgerock/openam/ui/common/util/RealmHelper
     */
    var obj = {};

    /**
     * Cleans a realm string
     * @param {String} realm The realm
     */
    obj.cleanRealm = function(realm) {
        if(typeof realm === "string" && realm.charAt(0) !== "/"){
            realm = "/" + realm;
        }
        if((typeof realm !== "string") || realm === "/"){
            realm = "";
        }
        return realm;
    };

    /**
     * Determines the realm from the current URI
     * <p>
     * As a realm can be specified in more than one section of the URI,
     * this function will determine (if more than one is specified) they
     * are consistent and return that consistent realm.
     * @returns Realm with leading forward slash (e.g. <code>/realmA'</code>) or <code>null</code> if realm was inconsistent
     */
    obj.getRealm = function() {
        var urlQueryStringRealm = (uiUtils.convertQueryParametersToJSON(uiUtils.getURIQueryString()).realm || '').trim(),
            fragmentQueryStringRealm = (uiUtils.convertQueryParametersToJSON(uiUtils.getURIFragmentQueryString()).realm || '').trim(),
            fragmentRealm = (uiUtils.getURIFragment() === 'login/') ?  ((uiUtils.getURIFragment().split('/')[1] || '').split('&')[0] || '').trim() : '',
            realm = '/', // Default to root realm
            realms = _.compact(_.uniq([urlQueryStringRealm, fragmentRealm, fragmentQueryStringRealm]));

        if(realms.length > 1) {
          return null;
        } else if(realms.length === 1) {
          realm = realms[0];
        }

        if(realm[0] !== '/') { realm = '/' + realm; }

        return realm;
    };


    return obj;
});
