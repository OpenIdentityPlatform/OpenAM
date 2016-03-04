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
 * Copyright 2015 ForgeRock AS.
 */

define("org/forgerock/openam/ui/common/components/TreeNavigation", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/ModuleLoader",
    "org/forgerock/commons/ui/common/util/URIUtils"
], function ($, _, AbstractView, ModuleLoader, URIUtils) {
    var TreeNavigation = AbstractView.extend({
        events: {
            "click .sidenav a[href]:not([data-toggle])": "navigateToPage"
        },
        findActiveNavItem: function (fragment) {
            var element = this.$el.find(".sidenav ol > li > a[href^=\"#" + fragment + "\"]"),
                parent, fragmentSections;
            if (element.length) {
                parent = element.parent();

                this.$el.find(".sidenav ol > li").removeClass("active");
                element.parentsUntil(this.$el.find(".sidenav"), "li").addClass("active");

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
            this.$el.find(".sidenav ol > li").removeClass("active");
            $(event.currentTarget).parentsUntil(this.$el.find(".sidenav"), "li").addClass("active");

            this.nextRenderPage = true;
        },
        setElement: function (element) {
            AbstractView.prototype.setElement.call(this, element);

            if (this.route && this.nextRenderPage) {
                ModuleLoader.load(this.route.page).then(
                    _.bind(function (module) {
                        this.renderPage(module, this.args);
                    }, this),
                    _.bind(function () {
                        throw "Unable to render page for module " + this.route.page;
                    }, this)
                );
            }
        },

        render: function (args, callback) {
            var self = this;

            this.args = args;

            self.parentRender(function () {
                self.$el.find(".sidenav li").removeClass("active");
                self.findActiveNavItem(URIUtils.getCurrentFragment());
                if (!self.nextRenderPage) {
                    ModuleLoader.load(self.route.page).then(function (page) {
                        self.renderPage(page, args, callback);
                    });
                }
            });
        },
        renderPage: function (Module, args, callback) {
            var page = new Module();
            this.nextRenderPage = false;
            page.element = "#sidePageContent";
            page.render(args, callback);
            this.delegateEvents();
        }
    });

    return TreeNavigation;
});
