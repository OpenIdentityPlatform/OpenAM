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
    "backbone",
    "org/forgerock/commons/ui/common/util/UIUtils"
], ($, Backbone, UIUtils) =>
    Backbone.View.extend({

        initialize (options) {
            this.options = options;
            this.options.activeView = this.options.activeView || 0;
        },

        getElementA () {
            return "#viewAContainer";
        },

        getElementB () {
            return "#viewBContainer";
        },

        getActiveView () {
            const index = this.$el.find(".tab-pane.active").index();
            return index > 0 ? index : 0;
        },

        render (callback) {
            UIUtils.fillTemplateWithData(
                "templates/admin/views/common/ToggleCardListTemplate.html",
                this.options.button,
                (html) => {
                    this.$el.html(html);
                    this.$el.find(".tab-pane").eq(this.options.activeView).addClass("active");
                    this.$el.find(".tab-toggles").eq(this.options.activeView).addClass("active");
                    callback(this);
                }
            );
        }
    }, {
        DEFAULT_VIEW: 0
    })
);
