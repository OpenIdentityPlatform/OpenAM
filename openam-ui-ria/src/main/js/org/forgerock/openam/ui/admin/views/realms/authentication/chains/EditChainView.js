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
 * Copyright 2015 ForgeRock AS.
 */

/*global define*/

define("org/forgerock/openam/ui/admin/views/realms/authentication/chains/EditChainView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "bootstrap-dialog",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/delegates/SMSDelegate"
], function($, _, AbstractView, BootstrapDialog, Configuration, EventManager, Router, Constants, SMSDelegate) {

    var EditChainView = AbstractView.extend({
        template: "templates/admin/views/realms/authentication/chains/EditChainTemplate.html",
        events: {

        },

        render: function(args, callback) {

            var self = this;

            SMSDelegate.RealmAuthenticationChains.getChain(args[1]).done(function (data) {

                self.data = data.values;

                self.parentRender(function(){

                    self.$el.find('[data-toggle="popover"]').popover();

                });

            })
            .fail(function () {
                // TODO: Add failure condition
            });

        }

    });

    return EditChainView;

});
