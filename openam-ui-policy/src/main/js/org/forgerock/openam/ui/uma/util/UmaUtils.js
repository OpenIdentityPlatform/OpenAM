/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

/*global define Backgrid Backbone _ $*/

define("org/forgerock/openam/ui/uma/util/UmaUtils", [
  "org/forgerock/openam/ui/uma/delegates/UmaDelegate"
], function (umaDelegate) {
    /**
     * @exports org/forgerock/openam/ui/uma/util/UmaUtils
     */
    var obj = {};

    obj.getResourceSet = function(uid, curResourceSet){

        var promise = $.Deferred,
            newPromise = $.Deferred;

        if (curResourceSet && curResourceSet.uid === uid) {
             return promise.apply().resolve(curResourceSet);
        } else {
            newPromise = umaDelegate.getResourceSetFromId(uid);
            $.when(newPromise).done(function(resourceSet){
                if (resourceSet) {
                    resourceSet.uid = uid;
                    resourceSet.scopes = obj.processResourceScopes(resourceSet.scopes);
                }
            });
        }

        return newPromise;
    };

    obj.processResourceScopes = function(data){

        var obj, scopes = [], temp;
        _.each(data, function(scope){
            obj = {};
            if (_.isUrl(scope)){
                // TODO: This should be a promise hitting the url not this string manipulation
                temp = scope.split('/');
                obj = {
                    "name" : temp[temp.length-1],
                    "icon_uri" : "http://www.example.com/icons/reading-glasses"
                };
            } else {
                obj.name = scope;
            }
            scopes.push(obj);
        });

        return scopes;
    };

    return obj;
});
