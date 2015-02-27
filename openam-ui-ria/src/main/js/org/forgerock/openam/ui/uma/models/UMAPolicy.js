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

/*global define*/
define("org/forgerock/openam/ui/uma/models/UMAPolicy", [
    "backbone",
    "org/forgerock/openam/ui/uma/util/URLHelper",
    "org/forgerock/openam/ui/uma/models/UMAPolicyPermissionCollection"
], function(Backbone, URLHelper, UMAPolicyPermissionCollection) {
    return Backbone.Model.extend({
        idAttribute: "policyId",
        initialize: function() {
            this.attributes.permissions = new UMAPolicyPermissionCollection();
        },
        parse: function(response, options) {
            if(response.permissions) {
                this.get("permissions").reset(response.permissions);
            }

            this.isSaved = true;
        },
        sync: function(method, model, options) {
            options.beforeSend = function(xhr) {
                xhr.setRequestHeader("Accept-API-Version", "protocol=1.0,resource=1.0");
            };

            if(method.toLowerCase() === 'update' && !model.isSaved) {
                options = options || {};
                options.headers = {};
                options.headers["If-None-Match"] = "*";
            }

            return Backbone.Model.prototype.sync.call(this, method, model, options);
        },
        urlRoot: function() {
            // FIXME: Has to be wrapped in a clojure as __username__ can't resolve early on. Race condition
            return URLHelper.substitute("__api__/users/__username__/uma/policies");
        }
    });
});
