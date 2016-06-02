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
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/ModuleLoader",
    "org/forgerock/commons/ui/common/util/URIUtils",
    "org/forgerock/openam/ui/common/util/es6/normaliseModule"
], ($, _, AbstractView, ModuleLoader, URIUtils, normaliseModule) => {
    const TreeNavigation = AbstractView.extend({
        template: "templates/admin/views/common/navigation/TreeNavigationTemplate.html",
        partials: [
            "partials/breadcrumb/_Breadcrumb.html",
            "templates/admin/views/common/navigation/_TreeNavigationLeaf.html"
        ],
        events: {
            "click .sidenav a[href]:not([data-toggle])": "navigateToPage"
        },
        findActiveNavItem (fragment) {
            const element = this.$el.find(`.sidenav ol > li > a[href^="#${fragment}"]`);
            if (element.length) {
                const parent = element.parent();

                this.$el.find(".sidenav ol > li").removeClass("active");
                element.parentsUntil(this.$el.find(".sidenav"), "li").addClass("active");

                // Expand any collapsed element direct above. Only works one level up
                if (parent.parent().hasClass("collapse")) {
                    parent.parent().addClass("in");
                }
            } else {
                const fragmentSections = fragment.split("/");
                this.findActiveNavItem(fragmentSections.slice(0, -1).join("/"));
            }
        },
        navigateToPage (event) {
            this.$el.find(".sidenav ol > li").removeClass("active");
            $(event.currentTarget).parentsUntil(this.$el.find(".sidenav"), "li").addClass("active");
            this.nextRenderPage = true;
        },
        setElement (element) {
            AbstractView.prototype.setElement.call(this, element);
            if (this.route && this.nextRenderPage) {
                ModuleLoader.load(this.route.page).then(
                    _.bind((module) => {
                        this.renderPage(module, this.args);
                    }, this),
                    _.bind(() => {
                        throw `Unable to render page for module ${this.route.page}`;
                    }, this)
                );
            }
        },

        render (args, callback) {
            this.args = args;
            this.parentRender(() => {
                this.$el.find(".sidenav li").removeClass("active");
                this.findActiveNavItem(URIUtils.getCurrentFragment());
                if (!this.nextRenderPage) {
                    ModuleLoader.load(this.route.page).then((page) => {
                        this.renderPage(page, args, callback);
                    });
                }
            });
        },
        renderPage (Module, args, callback) {
            Module = normaliseModule.default(Module);

            const page = new Module();
            this.nextRenderPage = false;
            page.element = "#sidePageContent";
            page.render(args, callback);
            this.delegateEvents();
        }
    });

    return TreeNavigation;
});
