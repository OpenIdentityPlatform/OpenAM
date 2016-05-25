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
    "jquery",
    "lodash",
    "backbone",
    "backbone-relational"
], function ($, _, Backbone) {
    return Backbone.RelationalModel.extend({
        parse (response) {
            if (_.isUrl(response.id)) {
                response = this.resolve(response.id);
            } else {
                response.name = response.id;
            }

            return response;
        },
        resolve (url) {
            var resolved = {
                id: url,
                name: url
            };

            // Synchronous!
            $.ajax({
                async: false,
                dataType: "json",
                success (data) {
                    resolved.name = data.name;
                    resolved["icon_uri"] = data.icon_uri;
                },
                url
            });

            return resolved;
        },
        sync (method, model, options) {
            options.beforeSend = function (xhr) {
                xhr.setRequestHeader("Accept-API-Version", "protocol=1.0,resource=1.0");
            };

            return Backbone.Model.prototype.sync.call(this, method, model, options);
        }
    });
});
