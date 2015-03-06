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

/*global _, define*/
define("org/forgerock/openam/ui/uma/models/UMAResourceSetWithPolicy", [
    'backbone',
    'backboneRelational',
    'org/forgerock/openam/ui/uma/models/UMAPolicy',
    'org/forgerock/openam/ui/uma/models/UMAPolicyPermissionScope',
    'org/forgerock/openam/ui/uma/util/URLHelper'
], function(Backbone, BackboneRelational, UMAPolicy, UMAPolicyPermissionScope, URLHelper) {
    return Backbone.RelationalModel.extend({
        idAttribute: "_id",
        parse: function(response, options) {
            // Hardwiring the id across to the UMAPolicy object as the server doesn't provide it
            if(response.policy) {
                response.policy.policyId = response._id;
            }

            response.scopes = _.map(response.scopes, function(scope) {
                return { id: scope };
            });

            return response;
        },
        relations: [{
            type: Backbone.HasOne,
            key: 'policy',
            relatedModel: UMAPolicy
        }, {
            type: Backbone.HasMany,
            key: 'scopes',
            relatedModel: UMAPolicyPermissionScope,
            parse: true
        }],
        urlRoot: URLHelper.substitute("__api__/users/__username__/oauth2/resourcesets")
    });
});
