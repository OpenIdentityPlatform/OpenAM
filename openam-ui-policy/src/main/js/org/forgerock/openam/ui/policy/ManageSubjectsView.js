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
        "org/forgerock/commons/ui/common/main/AbstractView",
        "org/forgerock/openam/ui/policy/EditSubjectView",
        "org/forgerock/openam/ui/policy/OperatorRulesView",
        "org/forgerock/commons/ui/common/main/EventManager",
        "org/forgerock/commons/ui/common/util/Constants",
        "org/forgerock/commons/ui/common/main/Configuration",
        "org/forgerock/commons/ui/common/util/UIUtils"

], function(AbstractView, editSubjectView, OperatorRulesView, eventManager, constants, conf, uiUtils ) {

    var ManageSubjectsView = AbstractView.extend({

        template: "templates/policy/ManageSubjectsTemplate.html",
        noBaseTemplate: true,
        element: "#subjectContainer",
        events: {
            'click  a#addSubject:not(.inactive)':       'addSubject',
            'click  a#addOperator:not(.inactive)':      'addOperator',
            'click  a#clear:not(.inactive)':            'onClear'
        },

        buttons:{},
        data: {},


        render: function(data, callback) {

            // TODO: to be replaced with data from REST via delegate or local storage
            this.data.operators = _.filter(data.result, function(item){ return item.logical === true; });
            this.setElement(this.element);

            this.parentRender(function() {

                this.buttons.clearBtn       = this.$el.find("a#clear");
                this.buttons.addSubject     = this.$el.find("a#addSubject");
                this.buttons.addOperator    = this.$el.find("a#addOperator");

                editSubjectView.render({subjects:data.subjects}, null, this.element + ' #pickup-item');
                editSubjectView.on(editSubjectView.EDIT_START, _.bind( this.editStart, this));
                editSubjectView.on(editSubjectView.EDIT_STOP,  _.bind( this.editStop));

                var operatorRules = new OperatorRulesView();
                operatorRules.render(this.data, null, this.$el.find('#dropoff-area'), true );

                this.onClear();
                this.initSorting();

                if (callback) {callback();}
            });

        },

        initSorting: function() {

            var self = this,
                adjustment = {};

            this.$el.find("ol#dropbox").sortable({
                group: self.element + ' rule-creation-group',
                exclude:'.item-button-panel, li.editing',
                delay: 100,

                // set item relative to cursor position
                onDragStart: function (item, container, _super) {
                    var offset = item.offset(),
                        pointer = container.rootGroup.pointer;
                    self.adjustment = {
                        left: pointer.left - offset.left + 5,
                        top: pointer.top - offset.top
                    };

                    // if new item, not already in the dropbox
                   if (!container.options.drop) {

                        if (item.hasClass('subject')) {
                            editSubjectView.newListItem(item);
                        }

                    }

                    item.css({
                        width: item.width()
                    }).addClass("dragged");

                    $("body").addClass("dragging");

                    self.setInactive(self.buttons.clearBtn, true);
                    self.setInactive(self.buttons.addSubject, false);
                    self.setInactive(self.buttons.addOperator, false);
                },

                onDrag: function (item, position) {
                  item.css({
                    left: position.left - self.adjustment.left,
                    top: position.top - self.adjustment.top
                  });
                },

                onDrop: function  (item, container, _super, event) {

                    if (container.options.drop) {

                        var clonedItem, newHeight, animeAttrs, data, jsonString, rule = null;
                        clonedItem = $('<li/>').css({height: 0, backgroundColor: 'transparent', borderColor: 'transparent'});
                        item.before(clonedItem);
                        newHeight = item.height();
                        animeAttrs = clonedItem.position();
                        animeAttrs.width = clonedItem.outerWidth()-10;
                        item.addClass('dropped');
                        clonedItem.animate({'height': newHeight }, 400, 'linear');
                        item.animate( animeAttrs, 400, function  () {

                            clonedItem.detach();
                            item.removeClass('dropped');

                            if(item.data().operator){
                                rule = $.extend( false, item, new OperatorRulesView() );
                                rule.rebindElement(self.data, '#dropoff-area');
                            }

                            _super(item, container);
                            self.logData();

                        });

                    } else {
                        // TODO: Populate with data
                        editSubjectView.newEditable();
                        _super(item, container);
                        self.logData();
                    }

                },

                isValidTarget: function(item, container){

                    if (container.items.length > 0 &&
                        container.target.parent().data().operator &&
                        container.target.parent().data().operator.config.properties.condition
                    ) {
                        return false;
                    } else {
                        return true;
                    }

                }

            });

            this.$el.find("ol#pickup-item").sortable({
                group: self.element + ' rule-creation-group',
                drop: false
            });

        },

        editStart:function(e){
           $('body').addClass('editing');
           this.onClear();
        },

        editStop:function(e){
            $('body').removeClass('editing');
        },

        setInactive: function(button, state) {
            if (state) { button.addClass('inactive'); }
            else { button.removeClass('inactive'); }
        },

        onClear: function(e) {
            if(e) { e.preventDefault();}
            // TODO : need to acutally delete the object, not just the DOM object
            this.$el.find('#pickup-item').empty();
            this.setInactive(this.buttons.clearBtn, true);
            this.setInactive(this.buttons.addSubject, false);
            this.setInactive(this.buttons.addOperator, false);
        },


        addOperator: function(e) {
            e.preventDefault();
            // TODO : need to acutally delete the object, not just the DOM object
            this.$el.find('#pickup-item').empty();

            var operatorRules = new OperatorRulesView();
            operatorRules.render(this.data, null, this.$el.find('#pickup-item'));

            this.setInactive(this.buttons.clearBtn, false);
            this.setInactive(this.buttons.addSubject, false);
            this.setInactive(this.buttons.addOperator, true);
        },

        addSubject: function(e) {
            e.preventDefault();
            // TODO : need to acutally delete the object, not just the DOM object
            this.$el.find('#pickup-item').empty();

            editSubjectView.newEditable();
            this.setInactive(this.buttons.clearBtn, false);
            this.setInactive(this.buttons.addSubject, true);
            this.setInactive(this.buttons.addOperator, false);
        },

        logData:function(e){
            // TODO - serialize data. This is just an example
            var model = {};
                model.operator = this.$el.find('#first-rule').data().operator;
                model.children = this.$el.find('ol#dropbox').sortable("serialize").get();
            // console.clear();
            // console.log(JSON.stringify(model, null, 4));
        }

    });

    return new ManageSubjectsView();
});
