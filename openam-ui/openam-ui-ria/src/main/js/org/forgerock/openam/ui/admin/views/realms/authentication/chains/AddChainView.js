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
 * Copyright 2016 ForgeRock AS.
 */
define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/admin/services/realm/AuthenticationService"
], function ($, _, AbstractView, Router, Messages, AuthenticationService) {
    function validateChainProps () {
        var name = this.$el.find("[data-chain-name]").val().trim(),
            nameExists,
            isValid;

        nameExists = _.findWhere(this.data.chainsData, { _id:name });
        if (nameExists) {
            Messages.addMessage({
                type: Messages.TYPE_DANGER,
                message: $.t("console.authentication.chains.duplicateChain")
            });
        }
        isValid = name && !nameExists;
        this.$el.find("[data-save]").attr("disabled", !isValid);
    }

    return AbstractView.extend({
        template: "templates/admin/views/realms/authentication/chains/AddChainTemplate.html",
        events: {
            "keyup [data-chain-name]" : "onValidateChainProps",
            "change [data-chain-name]": "onValidateChainProps",
            "click [data-save]"       : "save"
        },
        render (args, callback) {
            var self = this,
                chainsData = [];
            this.data.realmPath = args[0];

            AuthenticationService.authentication.chains.all(this.data.realmPath).then(function (data) {
                _.each(data.values.result, function (obj) {
                    chainsData.push(obj);
                });
                self.data.chainsData = chainsData;

                self.parentRender(function () {
                    if (callback) {
                        callback();
                    }
                });
            });
        },
        save () {
            var self = this,
                name = this.$el.find("[data-chain-name]").val().trim();

            AuthenticationService.authentication.chains.create(
                self.data.realmPath,
                { _id: name }
            ).then(function () {
                Router.routeTo(Router.configuration.routes.realmsAuthenticationChainEdit, {
                    args: _.map([self.data.realmPath, name], encodeURIComponent),
                    trigger: true
                });
            }, function (response) {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response
                });
            });
        },
        onValidateChainProps () {
            validateChainProps.call(this);
        }
    });
});
