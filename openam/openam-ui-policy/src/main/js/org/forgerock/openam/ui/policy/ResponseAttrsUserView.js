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
 * @author Julian Kigwana
 */

/*global window, define, $, _, document, console, Handlebars*/

define("org/forgerock/openam/ui/policy/ResponseAttrsUserView", [
    "org/forgerock/commons/ui/common/main/AbstractView" ,
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"
], function (AbstractView, eventManager, constants) {
    var ResponseAttrsUserView = AbstractView.extend({
        element: "#userAttrs",
        template: "templates/policy/ResponseAttrsUser.html",
        noBaseTemplate: true,
        attrType: "User",

        render: function (args, callback) {

            var self = this,
                attr;

            this.data.selectedUserAttributes = args[0];
            this.data.allUserAttributes = [];

            _.each(args[1], function(propertyName){
                attr = {};
                attr.propertyName = propertyName;
                attr.selected = (_.find(self.data.selectedUserAttributes, function (obj) { return obj.propertyName === propertyName; }));
                self.data.allUserAttributes.push(attr);
            });

            this.parentRender(function () {

                self.initSelectize();
    
                if (callback) {
                    callback();
                }
            });
        },


        getAttrs: function () {
            var data = [],
                attr,
                i,
                length,
                self = this;

            _.each(this.data.selectedUserAttributes, function (value) {
                attr = {};
                attr.type = self.attrType;
                attr.propertyName = value.propertyName || value;
                data.push(attr);
            });

            data = _.sortBy(data, 'propertyName');

            return data;
        },

        initSelectize: function() {

            var self = this,
                title = '';

            this.$el.find('.selectize').each(function(){

                $(this).selectize({
                    plugins: ['restore_on_backspace'],
                    delimiter: ',',
                    persist: false,
                    create: false,
                    hideSelected: true,
                    onChange: function(value){
                        self.data.selectedUserAttributes = value;
                    }
                });

            });
        }
    });

    return new ResponseAttrsUserView();
});
