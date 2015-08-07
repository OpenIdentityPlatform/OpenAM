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

/*global define, require */
define("org/forgerock/openam/ui/uma/views/resource/LabelTreeNavigationView", [
    "underscore",
    "jquery",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/common/components/TreeNavigation",
    "org/forgerock/openam/ui/uma/delegates/UMADelegate"
], function(_, $, Router, TreeNavigation, UMADelegate) {
    var LabelTreeNavigationView = TreeNavigation.extend({
        template: "templates/uma/views/resource/LabelTreeNavigationTemplate.html",
        partials: [ "templates/uma/views/resource/_NestedList.html" ],
        findActiveNavItem: function (fragment) {
            var anchor = this.$el.find(".sidenav ol > li > a[href='#" + fragment + "']"),
                parentOls;

            if (anchor.length) {
                this.$el.find(".sidenav ol").removeClass("in");

                parentOls = anchor.parentsUntil( this.$el.find(".sidenav"), "ol.collapse" );
                parentOls.addClass("in").parent().children("span[data-toggle]").attr("aria-expanded", "true");
                anchor.parent().addClass("active");

                if(anchor.attr("aria-expanded") === "false"){
                    anchor.attr("aria-expanded", "true");
                }
            }
        },
        navigateToPage: function (event) {
            this.$el.find(".sidenav li").removeClass("active");
            $(event.currentTarget).addClass("active");
            this.nextRenderPage = true;
        },
        render: function(args, callback) {
            var self = this;

            UMADelegate.labels.all().done(function (data) {
                if(!_.any(data.result, function(label) {
                    return label.name.toLowerCase() === "starred";
                })) {
                    UMADelegate.labels.create("starred", "STAR");
                }

                self.data.labels = {
                    starred: _.filter(data.result, function(label) { return label.type.toLowerCase() === "starred"; }),
                    system: _.filter(data.result, function(label) { return label.type.toLowerCase() === "system"; }),
                    user: _.filter(data.result, function(label) { return label.type.toLowerCase() === "user"; })
                };
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

                TreeNavigation.prototype.render.call(self, args, callback);
            });
        }
    });

    return new LabelTreeNavigationView();
});
