/*
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
 * Portions copyright 2015-2016 ForgeRock AS.
 */

define("org/forgerock/openam/ui/user/UserModel", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractModel",
    "org/forgerock/openam/ui/common/util/array/arrayify",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/common/util/RealmHelper",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/ServiceInvoker",
    "org/forgerock/openam/ui/common/util/object/flattenValues"
], function ($, _, AbstractModel, arrayify, Configuration, Constants, Messages, RealmHelper, Router,
             ServiceInvoker, flattenValues) {
    var baseUrl = Constants.host + "/" + Constants.context + "/json/__subrealm__/users",
        UserModel = AbstractModel.extend({
            idAttribute: "id",
            defaults: {
                kbaInfo: []
            },
            silentReset: function () {
                var previousAttributes = this.previousAttributes();
                this.set(previousAttributes, { silent: true });
            },
            sync: function (method, model, options) {
                var clearPassword = _.bind(function () {
                        delete this.currentPassword;
                        this.unset("password");
                        return this;
                    }, this),
                    currentPassword,
                    errorCallback = function (response) {
                        Messages.addMessage({
                            type: Messages.TYPE_DANGER,
                            response: response
                        });
                        model.silentReset();
                    };

                if (method === "update" || method === "patch") {
                    if (_.has(this.changed, "password")) {
                        // password changes have to occur via a special rest call
                        return ServiceInvoker.restCall({
                            url: RealmHelper.decorateURIWithRealm(baseUrl + "/" + this.id + "?_action=changePassword"),
                            headers: { "Accept-API-Version": "protocol=1.0,resource=2.0" },
                            type: "POST",
                            suppressEvents: true,
                            error: errorCallback,
                            data: JSON.stringify({
                                username: this.get("id"),
                                currentpassword: this.currentPassword,
                                userpassword: this.get("password")
                            })
                        }).then(clearPassword, clearPassword);
                    } else {
                        // overridden implementation for AM, due to the failures which would result
                        // if unchanged attributes are included along with the request
                        currentPassword = this.currentPassword;
                        delete this.currentPassword;
                        options.error = errorCallback;
                        return ServiceInvoker.restCall(_.extend(
                            {
                                type: "PUT",
                                data: JSON.stringify(
                                    _.chain(this.toJSON())
                                        .pick(["givenName", "sn", "mail", "postalAddress", "telephoneNumber"])
                                        .mapValues(function (val) {
                                            return typeof val === "string" ? val.trim() : val;
                                        })
                                        .mapValues(function (val) {
                                            return val || [];
                                        })
                                        .value()
                                ),
                                suppressEvents: true,
                                url: RealmHelper.decorateURIWithRealm(baseUrl + "/" + this.id),
                                headers: {
                                    "If-Match": this.getMVCCRev(),
                                    "Accept-API-Version": "protocol=1.0,resource=2.0",
                                    "currentpassword": currentPassword
                                }
                            },
                            options
                        ));
                    }
                } else {
                    // The only other supported operation is read
                    return ServiceInvoker.restCall(_.extend(
                        {
                            "url" : RealmHelper.decorateURIWithRealm(baseUrl + "/" + this.id),
                            "headers": { "Accept-API-Version": "protocol=1.0,resource=2.0" },
                            "type": "GET"
                        },
                        options
                    )).then(function (response) {
                        model.clear();
                        if (options.parse) {
                            model.set(model.parse(response, options));
                        } else {
                            model.set(response);
                        }
                        return model.toJSON();
                    });
                }
            },
            parse: function (response) {
                delete response.userPassword;

                /**
                 * flattenValues due to the response having many values wrapped in arrays (makes for a simpler data
                 * structure)
                 */
                var user = flattenValues(response);
                // "kbaInfo" property must stay as an array (it's original value)
                user.kbaInfo = response.kbaInfo;

                /**
                 * Re-apply defaults to attributes that were not present in the response. Duplicate of what Backbone
                 * does when a model is first initialised. Fixes scenaries where a previous server response was an value
                 * but the next was missing the attribute and value entirely, failing to clear the previous now invalid
                 * value from the model
                 */
                user = _.defaults({}, user, _.result(this, "defaults"));

                // When we parse response the first time, amadmin don't have uid
                user.id = user.uid || user.username;
                if (!_.has(user, "roles")) {
                    this.uiroles = [];
                } else if (_.isString(user.roles)) {
                    this.uiroles = user.roles.split(",");
                } else {
                    this.uiroles = user.roles;
                }

                if (_.indexOf(user.roles, "ui-user") === -1) {
                    this.uiroles.push("ui-user");
                }

                return user;
            },
            getProfile: function () {
                return ServiceInvoker.restCall({
                    url: RealmHelper.decorateURIWithRealm(baseUrl + "?_action=idFromSession"),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=2.0" },
                    type: "POST",
                    errorsHandlers: { "serverError": { status: "503" }, "unauthorized": { status: "401" } }
                }).then(
                    _.bind(function (data) {
                        Configuration.globalData.auth.successURL = data.successURL;
                        Configuration.globalData.auth.fullLoginURL = data.fullLoginURL;
                        Configuration.globalData.auth.subRealm = data.realm.slice(1);

                        // keep track of the current realm as a future default value, following logout:
                        Router.configuration.routes.login.defaults[0] = data.realm;
                        this.set("id", data.id);
                        return this.fetch().then(_.bind(function () {
                            return this;
                        }, this));
                    }, this)
                );
            },
            getProtectedAttributes: function () {
                return ["password"].concat(Configuration.globalData.protectedUserAttributes);
            },
            setCurrentPassword: function (currentPassword) {
                this.currentPassword = currentPassword;
            },
            /**
             * Determines whether the user has the specified role(s).
             * @param   {string|array} roles Roles as either a string or array of roles
             * @returns {Boolean}      Whether this model has any of the roles specified
             */
            hasRole: function (roles) {
                return _.spread(_.partial(_.contains, this.uiroles))(arrayify(roles));
            }
        });
    return new UserModel();
});
