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

define("org/forgerock/openam/ui/admin/views/realms/authentication/chains/PostProcessView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView"
], function($, _, AbstractView) {

    var PostProcessView = AbstractView.extend({
        template: "templates/admin/views/realms/authentication/chains/PostProcessTemplate.html",
        events: {
            'click .delete-btn': 'remove',
            'click #addBtn': 'add',
            'change #newProcessClass': 'change',
            'keyup  #newProcessClass': 'change'
        },
        element: "#postProcessView",

        add: function(e){
            var newProcessClass = this.$el.find('#newProcessClass').val();
            //TODO - check for duplicates
            this.data.chainData.loginPostProcessClass.push(newProcessClass);
            this.render(this.data.chainData);
        },

        remove: function(e){
            var index = $(e.currentTarget).closest('tr').index();
            this.data.chainData.loginPostProcessClass[index] = "";
            this.render(this.data.chainData);
        },

        change: function(e){
            this.$el.find('#addBtn').prop('disabled', (e.currentTarget.value.length === 0));
        },

        render: function (chainData) {
            this.data.chainData = chainData;
            this.parentRender();
        }
    });

    return new PostProcessView();

});
