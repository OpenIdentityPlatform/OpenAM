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
    "backbone",
    "org/forgerock/openam/ui/common/util/URLHelper",
    "org/forgerock/openam/ui/admin/utils/ModelUtils"
], function (_, Backbone, URLHelper, ModelUtils) {
    return Backbone.Model.extend({
        idAttribute: "name",
        urlRoot: URLHelper.substitute("__api__/policies"),

        defaults () {
            return {
                name: null,
                description: "",
                resources: [],
                actionValues: {}
            };
        },

        validate (attrs) {
            if (attrs.name.trim() === "") {
                return "errorNoName";
            }

            // entities that are stored in LDAP can't start with '#'. http://www.jguru.com/faq/view.jsp?EID=113588
            if (attrs.name.indexOf("#") === 0) {
                return "errorCantStartWithHash";
            }

            if (attrs.resources.length === 0) {
                return "policyErrorNoResources";
            }
        },

        sync (method, model, options) {
            options = options || {};
            options.beforeSend = function (xhr) {
                xhr.setRequestHeader("Accept-API-Version", "protocol=1.0,resource=2.0");
            };
            options.error = ModelUtils.errorHandler;

            if (model.id === null) {
                method = "create";
                options.url = `${this.urlRoot()}/?_action=create`;
            }

            return Backbone.Model.prototype.sync.call(this, method, model, options);
        }
    });
});
