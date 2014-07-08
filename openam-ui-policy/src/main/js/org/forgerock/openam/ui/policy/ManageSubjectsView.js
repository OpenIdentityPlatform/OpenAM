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

], function(AbstractView, EditSubjectView, OperatorRulesView, eventManager, constants, conf, uiUtils ) {

    var ManageSubjectsView = AbstractView.extend({

        template: "templates/policy/ManageSubjectsTemplate.html",
        noBaseTemplate: true,
        element: "#subjectContainer",
        events: {
            'click  a#addSubject:not(.inactive)':  'addSubject',
            'click  a#addOperator:not(.inactive)': 'addOperator',
            'click  a#clear:not(.inactive)':       'onClear',
            "mousedown  #first-rule li.subject:not(.editing)"  : 'setFocus',
            "mousedown  #first-rule li.operator:not(.editing)" : 'setFocus'
        },

        buttons:{},
        data: {},
        pickUpItem: null,
        idCount: 0,

        render: function(data, callback) {

            ///- TODO: to be moved to delegate and hardcoded data to be replaced with REST calls
            this.data.subjects = _.filter(data.result, function(item) { return item.logical === false; });
            _.each(this.data.subjects, function(subject) {
                delete subject.config.properties.type;
                delete subject.config.type;
                _.map(subject.config.properties, function(value, key) {
                    switch(value.type) {
                        case 'string':
                            subject.config.properties[key] = '';
                        break;

                        case 'array':
                            subject.config.properties[key] = [];
                        break;

                        case 'object':
                            subject.config.properties[key] = {};
                        break;
                    }
                    delete value.type;
                });
            });

            this.data.operators = _.filter(data.result, function(item) { return item.logical === true; });
            _.each(this.data.operators, function(operator) {
                delete operator.config.properties.type;
                delete operator.config.type;
                _.map(operator.config.properties, function(value, key) {
                    switch(value.type) {
                        case 'string':
                            operator.config.properties[key] = '';
                        break;

                        case 'array':
                            operator.config.properties[key] = [];
                        break;

                        case 'object':
                            operator.config.properties[key] = {};
                        break;
                    }
                    delete value.type;
                });
            });
            ///-

            this.setElement(this.element);

            this.parentRender(function() {

                this.buttons.clearBtn       = this.$el.find("a#clear");
                this.buttons.addSubject     = this.$el.find("a#addSubject");
                this.buttons.addOperator    = this.$el.find("a#addOperator");
                this.pickUpItem             = this.$el.find('#pickUpItem');

                var operatorRules = new OperatorRulesView();
                    operatorRules.render(this.data, null, '#dropOffArea' );

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
                        pointer = container.rootGroup.pointer,
                        editSubjectView = null;
                    self.adjustment = {
                        left: pointer.left - offset.left + 5,
                        top: pointer.top - offset.top
                    };

                    self.setInactive(self.buttons.clearBtn, true);
                    self.setInactive(self.buttons.addSubject, false);
                    self.setInactive(self.buttons.addOperator, false);

                    item.css({width: item.width()}).addClass("dragged");
                    $("body").addClass("dragging");

                    if (!container.options.drop && item.hasClass('subject')) {
                        editSubjectView = $.extend( false, item, new EditSubjectView() );
                        editSubjectView.createListItem({subjects:self.data.subjects}, '#dropOffArea', item);
                        editSubjectView.on(editSubjectView.EDIT_START, self.editStart, self);
                    }

                },

                onDrag: function (item, position) {
                  item.css({
                    left: position.left - self.adjustment.left,
                    top: position.top - self.adjustment.top
                  });
                },

                onDrop: function  (item, container, _super, event) {

                    var rule = null, clonedItem, newHeight, animeAttrs, data, jsonString;

                    if (container.options.drop) {

                        clonedItem = $('<li/>').css({height: 0, backgroundColor: 'transparent', borderColor: 'transparent'});
                        item.before(clonedItem);
                        newHeight = item.height();
                        animeAttrs = clonedItem.position();
                        animeAttrs.width = clonedItem.outerWidth()-10;
                        item.addClass('dropped');
                        clonedItem.animate({'height': newHeight }, 300, 'linear');
                        item.animate( animeAttrs, 300, function  () {

                            clonedItem.detach();
                            item.removeClass('dropped');

                            if (item.data().logical) {
                                rule = $.extend( false, item, new OperatorRulesView() );
                                rule.rebindElement();
                            }
                            item.focus();
                            _super(item, container);
                            self.logData();
                        });

                    } else {

                        if (item.data().logical === undefined) {

                            rule = new EditSubjectView();
                            rule.render( {subjects:self.data.subjects}, null, self.pickUpItem, self.idCount, item.data() );
                            self.idCount++;
                        }
                        item.focus();
                        _super(item, container);
                        self.logData();
                    }

                },

                isValidTarget: function(item, container) {

                    if (container.items.length > 0 &&
                        container.target.parent().data().logical === true &&
                        container.target.parent().data().config.properties.subject
                    ) {
                        return false;
                    } else {
                        return true;
                    }

                },

                serialize: function ($parent, $children, parentIsContainer) {

                   var result = $.extend({}, $parent.data());

                    if(parentIsContainer) {
                        return $children;
                    }

                    else if ($children[0]) {
                        if (result.config.properties.subjects) {
                            result.config.properties.subjects.items = $children;
                        } else if (result.config.properties.subject) {
                            result.config.properties.subject = $children;
                        }
                    }

                    delete result.subContainers;
                    delete result.sortable;

                    return result;
                }

            });

            self.pickUpItem.sortable({
                group: self.element + ' rule-creation-group',
                drop: false
            });

        },

        editStart: function(item) {

           $('body').addClass('editing');
           var self = this,
               editSubjectView = new EditSubjectView();
           editSubjectView.render( {subjects:self.data.subjects}, null, self.pickUpItem, self.idCount, item.data() );
           editSubjectView.on(editSubjectView.EDIT_STOP,  _.bind( self.editStop, self));
           self.idCount++;

           editSubjectView.$el.addClass('editing');
           item.before(editSubjectView.$el) ;
           self.onClear();

           editSubjectView.$el.find('select#selection').focus();
        },

        editStop: function(item) {
            $('body').removeClass('editing');

            var editSubjectView = $.extend( false, item, new EditSubjectView() );
                editSubjectView.createListItem({subjects:this.data.subjects}, '#dropOffArea', item);
                editSubjectView.on(editSubjectView.EDIT_START, this.editStart, this);

            item.next().remove();
        },

        setInactive: function(button, state) {
            if (state) { button.addClass('inactive'); }
            else { button.removeClass('inactive'); }
        },

        onClear: function(e) {
            if(e) { e.preventDefault();}
            this.pickUpItem.children().remove();
            this.setInactive(this.buttons.clearBtn, true);
            this.setInactive(this.buttons.addSubject, false);
            this.setInactive(this.buttons.addOperator, false);
        },

        addOperator: function(e) {
            e.preventDefault();
            this.pickUpItem.children().remove();
            this.setInactive(this.buttons.clearBtn, false);
            this.setInactive(this.buttons.addSubject, false);
            this.setInactive(this.buttons.addOperator, true);

            var operatorRules = new OperatorRulesView();
                operatorRules.render(this.data, null, this.pickUpItem, this.idCount);
            this.idCount++;
        },

        addSubject: function(e) {
            e.preventDefault();
            this.pickUpItem.children().remove();
            this.setInactive(this.buttons.clearBtn, false);
            this.setInactive(this.buttons.addSubject, true);
            this.setInactive(this.buttons.addOperator, false);

            var editSubjectView = new EditSubjectView();
                editSubjectView.render({subjects:this.data.subjects}, null, this.pickUpItem, this.idCount);
            this.idCount++;
        },

        setFocus: function(e) {
            e.stopPropagation();
            var target = $(e.target).is('select') || $(e.target).is('input') ?  e.target : e.currentTarget;
            $(target).focus();
        },

        logData: function(e) {

            var model = this.$el.find('#first-rule').data(),
                properties = model.config.properties,
                children = this.$el.find('ol#dropbox').sortable("serialize").get();
            if (properties.subjects) {
                properties.subjects.items = children;
            } else if (properties.subject) {
                properties.subject = children;
            }

            console.log(model);
        }

    });

    return new ManageSubjectsView();
});
