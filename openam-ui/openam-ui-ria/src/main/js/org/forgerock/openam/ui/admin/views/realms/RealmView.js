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

/*global define, require*/
define("org/forgerock/openam/ui/admin/views/realms/RealmView", [
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/delegates/SMSGlobalDelegate"
], function ($, AbstractView, Router, SMSGlobalDelegate) {
    var RealmView = AbstractView.extend({
        template: "templates/admin/views/realms/RealmTemplate.html",
        events: {
            "click .sidenav a[href]:not([data-toggle])": "navigateToPage"
        },
        findActiveNavItem: function (fragment) {
            var element = this.$el.find(".sidenav ol > li > a[href^='#" + fragment + "']"),
                parent, fragmentSections;
            if (element.length) {
                parent = element.parent();

                this.$el.find('li').removeClass('active');
                element.parentsUntil( this.$el.find('.sidenav'), 'li' ).addClass('active');

                // Expand any collapsed element direct above. Only works one level up
                if (parent.parent().hasClass("collapse")) {
                    parent.parent().addClass("in");
                }
            } else {
                fragmentSections = fragment.split("/");
                this.findActiveNavItem(fragmentSections.slice(0, -1).join("/"));
            }
        },
        navigateToPage: function (event) {
            this.$el.find('li').removeClass('active');
            $(event.currentTarget).parentsUntil( this.$el.find('.sidenav'), 'li' ).addClass('active');

            this.nextRenderPage = true;
        },
        setElement: function (element) {
            AbstractView.prototype.setElement.call(this, element);

            if (this.route && this.nextRenderPage) {
                var module = require(this.route.page);
                if (module) {
                    this.nextRenderPage = false;
                    this.renderPage(module, this.args);
                } else {
                    throw "Unable to render realm page for module " + this.route.page;
                }
            }
        },
        realmExists: function (path) {
            return SMSGlobalDelegate.realms.get(path);
        },
        render: function (args, callback) {
            var self = this;

            this.args = args;
            this.data.realmPath = args[0];
            this.data.realmName = this.data.realmPath === "/" ? $.t('console.common.topLevelRealm') : this.data.realmPath;

            this.realmExists(args[0])
            .done(function () {
                self.parentRender(function () {
                    self.$el.find("li").removeClass("active");
                    self.findActiveNavItem(Router.getURIFragment());
                    self.renderPage(require(self.route.page), args, callback);
                });
            })
            .fail(function () {
                Router.routeTo(Router.configuration.routes.realms, {
                    args: [],
                    trigger: true
                });
            });
        },
        renderPage: function (Module, args, callback) {
            var page = new Module();

            page.element = '#sidePageContent';
            page.render(args, callback);
            this.delegateEvents();
        }
    });

    return new RealmView();
});
