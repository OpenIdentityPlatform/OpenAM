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
 * Copyright 2016 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "backbone",
    "org/forgerock/commons/ui/common/util/UIUtils",

    // jquery dependencies
    "bootstrap-tabdrop"
], ($, _, Backbone, UIUtils) => {
    function has (attribute, tab) {
        if (!tab[attribute]) {
            throw new TypeError(`[TabComponent] Expected all items within 'tabs' to have a '${attribute}' attribute.`);
        }
        if (!_.isString(tab[attribute])) {
            throw new TypeError(`[TabComponent] Expected all items within 'tabs' to have String '${attribute}'s.`);
        }

        return true;
    }

    const TabComponent = Backbone.View.extend({
        template: "templates/common/components/tab/TabComponentTemplate.html",
        bodyTemplate: "templates/common/components/tab/TabComponentBodyTemplate.html",
        events: {
            "show.bs.tab ul.nav.nav-tabs a": "handleTabClick"
        },
        initialize (options) {
            if (!(options.tabs instanceof Array)) {
                throw new TypeError("[TabComponent] \"tabs\" argument is not an Array.");
            }
            if (_.isEmpty(options.tabs)) {
                throw new TypeError("[TabComponent] \"tabs\" argument is an empty Array.");
            }
            _(options.tabs)
                .each(_.partial(has, "id"))
                .each(_.partial(has, "title"))
                .value();

            this.options = options;
        },
        getBody () {
            return this.tabBody;
        },
        getBodyElement () {
            return this.$el.find("[data-tab-panel]");
        },
        getFooter () {
            return this.options.tabFooter;
        },
        getFooterElement () {
            return this.$el.find("[data-tab-footer]");
        },
        getTabId () {
            return this.currentTabId;
        },
        setTabId (id) {
            this.currentTabId = id;
            this.$el.find(`[data-tab-id="${id}"]`).tab("show");
        },
        handleTabClick (event) {
            this.currentTabId = $(event.currentTarget).data("tab-id");

            this.options.tabFooter = this.options.createFooter(this.currentTabId);
            this.tabBody = this.options.createBody(this.currentTabId);

            UIUtils.compileTemplate(this.bodyTemplate, this.options).then((html) => {
                this.$el.find("[data-tab-component-panel]").html(html);

                if (this.tabBody) {
                    this.tabBody.setElement(this.getBodyElement());
                    this.tabBody.render();
                }

                if (this.options.tabFooter) {
                    this.options.tabFooter.setElement(this.getFooterElement());
                    this.options.tabFooter.render();
                }
            });
        },
        render () {
            UIUtils.compileTemplate(this.template, this.options).then((html) => {
                this.$el.html(html);
                this.$el.find(".tab-menu .nav-tabs").tabdrop();
                this.setTabId(this.options.tabs[0].id);
            });
            return this;
        }
    });

    return TabComponent;
});
