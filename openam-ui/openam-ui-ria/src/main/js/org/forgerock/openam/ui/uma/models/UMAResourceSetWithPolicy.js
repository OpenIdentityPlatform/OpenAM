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


define("org/forgerock/openam/ui/uma/models/UMAResourceSetWithPolicy", [
    "jquery",
    "underscore",
    "backbone",
    "backbone-relational",
    "org/forgerock/openam/ui/uma/models/UMAPolicy",
    "org/forgerock/openam/ui/uma/models/UMAPolicyPermissionScope",
    "org/forgerock/openam/ui/uma/util/URLHelper"
], function ($, _, Backbone, BackboneRelational, UMAPolicy, UMAPolicyPermissionScope, URLHelper) {
    return Backbone.RelationalModel.extend({
        // Promise version of fetch
        fetch: function () {
            var d = $.Deferred();
            Backbone.RelationalModel.prototype.fetch.call(this, {
                success: function (model) {
                    d.resolve(model);
                },
                error: function (model, response) {
                    d.reject(response);
                }
            });
            return d.promise();
        },
        idAttribute: "_id",
        parse: function (response) {
            // Hardwiring the id across to the UMAPolicy object as the server doesn't provide it
            if (!response.policy) {
                response.policy = {};
                response.policy.permissions = [];
            }
            response.policy.policyId = response._id;

            response.scopes = _.map(response.scopes, function (scope) {
                return { id: scope };
            });

            return response;
        },
        relations: [{
            type: Backbone.HasOne,
            key: "policy",
            relatedModel: UMAPolicy,
            parse: true
        }, {
            type: Backbone.HasMany,
            key: "scopes",
            relatedModel: UMAPolicyPermissionScope,
            includeInJSON: Backbone.Model.prototype.idAttribute,
            parse: true
        }],
        toggleStarred: function (starredLabelId) {
            var isStarred = _.contains(this.get("labels"), starredLabelId);

            if (isStarred) {
                this.set("labels", _.reject(this.get("labels"), function (label) {
                    return label === starredLabelId;
                }));
            } else {
                this.get("labels").push(starredLabelId);
            }
        },
        urlRoot: URLHelper.substitute("__api__/__subrealm__/users/__username__/oauth2/resources/sets")
    });
});
