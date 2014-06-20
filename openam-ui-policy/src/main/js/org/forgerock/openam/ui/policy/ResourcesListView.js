/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/**
 * @author Eugenia Sergueeva
 */

/*global window, define, $, form2js, _, js2form, document, console */

define("org/forgerock/openam/ui/policy/ResourcesListView", [
    "org/forgerock/commons/ui/common/main/AbstractView"
], function (AbstractView) {
    var ResourcesListView = AbstractView.extend({
        element: "#resourcesList",
        template: "templates/policy/ResourcesListTemplate.html",
        noBaseTemplate: true,
        events: {
            'click .toggle-all-resources': 'toggleAllResources',
            'click #deleteResources': 'deleteResources'
        },

        render: function (args, callback) {
            _.extend(this.data, args);

            if (!this.data.app.resources) {
                this.data.app.resources = [];
            }

            this.parentRender(callback);
        },

        /**
         * Toggles all resources.
         */
        toggleAllResources: function (e) {
            this.$el.find('[data-resource-index]').attr('checked', e.target.checked);
        },

        /**
         * Deletes all selected resources.
         */
        deleteResources: function () {
            var self = this,
                selected = this.$el.find('[data-resource-index]:checked'),
                resources = self.data.app.resources,
                resourcesToDelete = [];

            _.each(selected, function (value, key, list) {
                resourcesToDelete.push(resources[value.getAttribute('data-resource-index')]);
            });

            this.data.app.resources = _.difference(resources, resourcesToDelete);

            this.render(self.data);
        }
    });

    return new ResourcesListView();
});
