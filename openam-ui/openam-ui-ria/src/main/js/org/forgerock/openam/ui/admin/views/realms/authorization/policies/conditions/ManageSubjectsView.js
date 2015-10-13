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
 * Portions copyright 2014-2015 ForgeRock AS.
 */


define("org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ManageSubjectsView", [
    "jquery",
    "underscore",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ManageRulesView",
    "org/forgerock/commons/ui/common/util/Constants"
], function ($, _, ManageRulesView, Constants) {
    var ManageSubjectsView = ManageRulesView.extend({
        element: "#subjectContainer",
        subEvents: {
            "change .subject-area .operator > select": "onSelect",
            "mousedown #operatorSub_0 li.rule:not(.editing)": "setFocus",
            "mousedown #operatorSub_0 li.operator:not(.editing)": "setFocus",

            "click    #operatorSub_0 .rule > .item-button-panel > .fa-times": "onDelete",
            "keyup    #operatorSub_0 .rule > .item-button-panel > .fa-times": "onDelete",
            "click    #operatorSub_0 .rule > .item-button-panel > .fa-pencil": "toggleEditing",
            "keyup    #operatorSub_0 .rule > .item-button-panel > .fa-pencil": "toggleEditing",
            "click    #operatorSub_0 .rule > .item-button-panel > .fa-check": "toggleEditing",
            "keyup    #operatorSub_0 .rule > .item-button-panel > .fa-check": "toggleEditing",
            "dblclick #operatorSub_0 li.rule:not(.legacy-condition)": "toggleEditing"
        },
        data: {},
        buttons: {},
        idCount: 0,
        typeAND: {
            "title": "AND",
            "logical": true,
            "config": {
                "properties": {
                    "subjects": {
                        "type": "array",
                        "items": {
                            "type": "any"
                        }
                    }
                }
            }
        },

        render: function (args, callback) {

            this.idPrefix = "Sub_";
            this.property = "subject";
            this.properties = "subjects";
            this.data.conditionName = $.t("console.authorization.policies.edit.addSubjectCondition");
            this.data.entity = args.entity;
            this.data.options = args.options;
            this.data.subjects = [];
            this.data.operators = [];

            var self = this;

            _.each(this.data.options.availableSubjects, function (item) {

                if (item.logical === true) {
                    self.data.operators.push(item);
                } else {
                    self.data.subjects.push(item);
                }

                delete item.config.type;
            });

            if (!_.find(this.data.operators, { title: "AND" })) {
                this.data.operators.push(this.typeAND);
            }

            this.init(args, this.subEvents);
            this.conditionType = Constants.SUBJECT;
            this.setElement(this.element);

            this.idCount = 0;

            this.parentRender(function () {
                this.buttons.addCondition = this.$el.find("a#addCondition");
                this.buttons.addOperator = this.$el.find("a#addOperator");

                if (self.data.operators.length === 0) {
                    this.buttons.addOperator.hide();
                }

                this.buildList();
                this.initSorting();
                this.identifyDroppableLogical();

                if (callback) {
                    callback();
                }
            });
        }
    });

    return new ManageSubjectsView();
});
