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
 * @author JKigwana
 */

/*global window, define, $, form2js, _, js2form, document, console */

define( "org/forgerock/openam/ui/policy/ManageSubjectsView", [
        "org/forgerock/openam/ui/policy/ManageRulesView",
        "org/forgerock/commons/ui/common/util/Constants"

], function( ManageRulesView, Constants ) {

    var ManageSubjectsView = ManageRulesView.extend({

        element: "#subjectContainer",
        subEvents: {

            'change .subject-area .operator select' : 'onSelect',
            'mousedown #operatorSub_0 li.rule:not(.editing)' : 'setFocus',
            'mousedown #operatorSub_0 li.operator:not(.editing)' : 'setFocus',

            'click    #operatorSub_0 .rule > .item-button-panel > .icon-remove' :    'onDelete',
            'keyup    #operatorSub_0 .rule > .item-button-panel > .icon-remove' :    'onDelete',
            'click    #operatorSub_0 .rule > .item-button-panel > .icon-pencil' :    'toggleEditing',
            'keyup    #operatorSub_0 .rule > .item-button-panel > .icon-pencil' :    'toggleEditing',
            'click    #operatorSub_0 .rule > .item-button-panel > .icon-checkmark' : 'toggleEditing',
            'keyup    #operatorSub_0 .rule > .item-button-panel > .icon-checkmark' : 'toggleEditing',
            'dblclick #operatorSub_0 li.rule:not(.legacy-condition)' :               'toggleEditing'
        },
        data: {},
        buttons:{},
        idCount: 0,
        typeAND: {"title":"AND","logical":true,"config":{"properties":{"subjects":{"type":"array","items":{"type":"any"}}}}},
        

        render: function(args, callback, element) {

            this.idPrefix = 'Sub_';
            this.property = 'subject';
            this.properties = 'subjects';
            this.data.conditionName = "Subject Condition";
            this.data.entity = args.entity;
            this.data.options = args.options;
            this.data.subjects = [];
            this.data.operators = [];

            var self = this;

            _.each(this.data.options.availableSubjects, function(item) {

                if(item.logical === true){
                    self.data.operators.push(item);
                }else{
                    self.data.subjects.push(item);
                }

                delete item.config.type;
            });

            if( !_.findWhere(this.data.operators, {title:"AND"}) ){
                this.data.operators.push(this.typeAND);
            }

            this.init(args, this.subEvents);
            this.conditionType = Constants.SUBJECT;
            this.setElement(this.element);

            this.idCount = 0;

            this.parentRender(function() {


                this.buttons.clearBtn       = this.$el.find("a#clear");
                this.buttons.addCondition   = this.$el.find("a#addCondition");
                this.buttons.addOperator    = this.$el.find("a#addOperator");
                this.pickUpItem             = this.$el.find('ol#pickUpItem');

                if (self.data.operators.length === 0) {
                    this.buttons.addOperator.hide();
                }

                this.buildList();
                this.onClear();
                this.initSorting();

                if (callback) {callback();}
            });

        }

    });

    return new ManageSubjectsView();
});
