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

/*global, define*/
define('org/forgerock/openam/ui/admin/views/realms/RealmView', [
    'jquery',
    'org/forgerock/commons/ui/common/main/AbstractView',
    'require',
    'org/forgerock/commons/ui/common/main/Router'
], function ($, AbstractView, require, Router) {
    var RealmView = AbstractView.extend({
        template: 'templates/admin/views/realms/RealmTemplate.html',
        events: {
            'click .sidenav a[href]:not([data-toggle])': 'navigateToPage'
        },
        navigateToPage: function (event) {
            this.$el.find('li').removeClass('active');
            $(event.currentTarget).parent().addClass('active');

            this.nextRenderPage = true;
        },
        setElement: function (element) {
            AbstractView.prototype.setElement.call(this, element);

            if (this.route && this.nextRenderPage) {
                var module = require(this.route.page);
                if (module) {
                    this.nextRenderPage = false;
                    this.renderPage(module);
                } else {
                    throw 'Unable to render realm page for module ' + this.route.page;
                }
            }
        },
        render: function (args, callback) {
            var self = this;

            this.data.realmName = args[0];

            this.parentRender(function () {
                this.$el.find('li').removeClass('active');

                var activeLink = this.$el.find('li a[href="#' + Router.getURIFragment() + '"]'), parent;
                if (activeLink) {
                    parent = activeLink.parent();
                    parent.addClass('active');

                    // Expand any collapsed element direct above. Only works one level up
                    if (parent.parent().hasClass('collapse')) {
                        parent.parent().collapse('show');
                    }
                }

                self.renderPage(require(this.route.page), args, callback);
            });
        },
        renderPage: function (Module, args, callback) {
            var page = new Module();

            page.element = '#realmsPageContent';
            page.render(args, callback);
            this.delegateEvents();
        }
    });

    return new RealmView();
});
