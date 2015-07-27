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

/*global define require*/
define("org/forgerock/openam/ui/uma/views/resource/NavResources", [
    "underscore",
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/uma/delegates/UMADelegate"
], function(_, $, AbstractView, Router, UMADelegate) {
    var NavResources = AbstractView.extend({
        template: "templates/uma/views/resource/NavResources.html",
        partials : ["templates/uma/views/resource/_NestedList.html"],
        events: {
            "click .sidenav a[href]:not([data-toggle])": "navigateToPage"
        },
        findActiveNavItem: function (fragment) {
            var anchor = this.$el.find(".sidenav ol > li > a[href='#" + fragment + "']"),
                parentOls, parentAnchors, fragmentSections;
            if (anchor.length) {

                this.$el.find(".sidenav ol").removeClass("in");

                parentOls = anchor.parentsUntil( this.$el.find(".sidenav"), "ol.collapse" );
                parentOls.addClass("in").parent().children("span[data-toggle]").attr("aria-expanded","true");
                anchor.parent().addClass("active");

                if(anchor.attr("aria-expanded") === "false"){
                    anchor.attr("aria-expanded","true");
                }

            } else {
                fragmentSections = fragment.split("/");
                this.findActiveNavItem(fragmentSections.slice(0, -1).join("/"));
            }
        },
        navigateToPage: function (e) {
            this.$el.find(".sidenav li").removeClass("active");
            $(e.currentTarget).addClass("active");
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

        renderPage: function (Module, args, callback) {
            var page = new Module();
            page.element = "#sidePageContent";
            page.render(args, callback);
            this.delegateEvents();
        },

        render: function(args, callback) {
            var self = this;
            this.args = args;

            UMADelegate.labels.all().done(function (data) {

                self.data.labels = data;
                self.data.nestedLabels = [];

                function addToParent(collection, label) {

                    if (label.name.indexOf("/") === -1) {
                        label.title = label.name;
                        label.children = [];
                        label.viewId = _.uniqueId("viewId_");
                        collection.push(label);
                    } else {
                        var shift = label.name.split("/"),
                            parentName = shift.shift(),
                            parent;
                        label.name = shift.join("/");
                        parent = _.findWhere(collection, { title: parentName });
                        if (!parent) {
                            parent = { title: parentName, children: [], viewId: _.uniqueId("viewId_")};
                            collection.push(parent);
                        }
                        addToParent(parent.children, label);
                    }
                }

                _.each(data.user, function(label){
                    addToParent(self.data.nestedLabels, label);
                });

                self.parentRender(function(){
                    self.$el.find(".sidenav li").removeClass("active");
                    self.findActiveNavItem(Router.getURIFragment());
                    self.renderPage(require(self.route.page), args, callback);
                });
            });
        }
    });

    return new NavResources();
});
