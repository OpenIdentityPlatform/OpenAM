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
    "backbone",
    "org/forgerock/commons/ui/common/util/Base64",
    "org/forgerock/openam/ui/common/util/URLHelper",
    "org/forgerock/openam/ui/admin/utils/ModelUtils"
], function (Backbone, Base64, URLHelper, ModelUtils) {
    return Backbone.Model.extend({
        idAttribute: "_id",
        urlRoot: URLHelper.substitute("__api__/scripts"),
        defaults () {
            return {
                _id: null,
                name: "",
                script: "",
                language: "",
                context: ""
            };
        },

        validate (attrs) {
            if (attrs.name.trim() === "") {
                return "scriptErrorNoName";
            }

            if (attrs.language === "") {
                return "scriptErrorNoLanguage";
            }
        },

        parse (resp) {
            if (resp && resp.script) {
                resp.script = Base64.decodeUTF8(resp.script);
            }
            return resp;
        },

        sync (method, model, options) {
            options = options || {};
            options.beforeSend = function (xhr) {
                xhr.setRequestHeader("Accept-API-Version", "protocol=1.0,resource=1.0");
            };
            options.error = ModelUtils.errorHandler;

            method = method.toLowerCase();
            if (method === "create" || model.id === null) {
                options.url = `${this.urlRoot()}/?_action=create`;
            }

            if (method === "create" || method === "update") {
                model.set("script", Base64.encodeUTF8(model.get("script")));
            }

            return Backbone.Model.prototype.sync.call(this, method, model, options);
        }
    });
});
