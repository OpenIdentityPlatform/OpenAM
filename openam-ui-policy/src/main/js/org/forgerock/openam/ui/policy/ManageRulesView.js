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

define( "org/forgerock/openam/ui/policy/ManageRulesView", [
        "org/forgerock/commons/ui/common/main/AbstractView",
        "org/forgerock/openam/ui/policy/EditEnvironmentView",
        "org/forgerock/openam/ui/policy/EditSubjectView",
        "org/forgerock/openam/ui/policy/OperatorRulesView",
        "org/forgerock/openam/ui/policy/LegacyListItemView",
        "org/forgerock/commons/ui/common/main/EventManager",
        "org/forgerock/commons/ui/common/util/Constants"

], function(AbstractView, EditEnvironmentView, EditSubjectView, OperatorRulesView, LegacyListItemView, EventManager, Constants ) {

    var ManageRulesView = AbstractView.extend({

        template: "templates/policy/ManageRulesTemplate.html",
        noBaseTemplate: true,
        events: {
            'click  a#addCondition:not(.inactive)':   'addCondition',
            'click  a#addOperator:not(.inactive)':    'addOperator',
            'click  a#clear:not(.inactive)':          'onClear',
            'click  .operator > .item-button-panel > .icon-remove' : 'onDelete',
            'keyup  .operator > .item-button-panel > .icon-remove' : 'onDelete'
        },
        types: {
            ENVIRONMENT: 'environmentType',
            SUBJECT: 'subjectType',
            LEGACY: "Policy"
        },

        pickUpItem: null,
        localEntity: {},
        groupCounter: 0,

        idPrefix: '',
        property: '',
        properties: '',

        init: function(args, events) {

            _.extend(this.events, events);
            _.extend(Constants, this.types);


            this.localEntity = null;
            this.sortingInitialised = false;

            if (this.data.entity[this.property]) {
                this.localEntity = this.data.entity[this.property];
            }

        },

        buildList: function() {

            var self = this,
                newRule = null,
                operators = _.pluck( this.data.operators, 'title' ),
                buildListItem = null,
                properties = null;

                buildListItem = function(data, container, parent) {

                    if( _.isArray(data) === false ){
                        data = [data];
                    }

                    _.each(data, function(item) {

                        if ( item && _.contains( operators, item.type )) {

                            newRule = new OperatorRulesView();
                            newRule.render(self.data, null, container, self.idPrefix + self.idCount, (self.idCount === 0) );
                            newRule.setValue(item.type);
                            self.idCount++;

                        } else if ( !_.isEmpty(item) ) {

                            if( item.type === Constants.LEGACY){
                                newRule = new LegacyListItemView();
                                newRule.render( item, null, container, self.idCount);
                            } else {
                                newRule = self.getNewRule();
                                properties = self.getProperties();
                                newRule.render( properties, null, container, self.idCount, item);
                                newRule.createListItem( properties, newRule.$el );
                            }

                            self.idCount++;
                        }

                        if ( item && item[self.properties] ) {
                            buildListItem( item[self.properties], newRule.dropbox, item );
                        } else if (item && item[self.property]) {
                            buildListItem( item[self.property], newRule.dropbox, item );
                        }

                    });
                };

            /* This view will detect if the preserved rule begins with a logical. If it doesn't, an AND logical will be added to the root to give the users somewhere to drop rules into.
             * However if the root logical is obsolete, for example it is one which expects many children but contains one or less, the root logical will be striped from the json before it is saved.
             */

            if ( !this.localEntity || _.contains( operators, this.localEntity.type ) === false ){
                properties = _.clone(this.localEntity);
                this.localEntity = {type:"AND"};
                this.localEntity[this.properties] = [properties];
            }

            buildListItem(this.localEntity, this.$el.find('ol#dropOffArea'), null);
            this.delegateEvents();

        },

        initSorting: function() {

            var self = this,
                adjustment = {};

            this.groupCounter++;

            this.$el.find("ol#dropbox").nestingSortable({
                group: self.element + 'rule-creation-group' + self.groupCounter,
                exclude:'.item-button-panel, li.editing',
                delay: 100,

                // set item relative to cursor position
                onDragStart: function (item, container, _super) {

                    var offset = item.offset(),
                        pointer = container.rootGroup.pointer,
                        editRuleView = null;
                    self.adjustment = {
                        left: pointer.left - offset.left + 5,
                        top: pointer.top - offset.top
                    };

                    self.setInactive(self.buttons.clearBtn, true);
                    self.setInactive(self.buttons.addCondition, false);
                    self.setInactive(self.buttons.addOperator, false);
                    self.showHint(false);

                    item.focus();
                    item.css({width: item.width()}).addClass("dragged");
                    $("body").addClass("dragging");

                    if (!container.options.drop && item.hasClass('rule')) {
                        editRuleView = $.extend( false, item, self.getNewRule() );
                        editRuleView.createListItem( self.getProperties(),  item);
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

                            if (item.data().logical === true) {
                                rule = $.extend( false, item, new OperatorRulesView() );
                                rule.rebindElement();
                            }
                            item.focus();
                            _super(item, container);
                            self.save();
                        });

                    } else {

                        if (item.data().logical === undefined) {

                            rule = self.getNewRule();
                            rule.render( self.getProperties(), null, self.pickUpItem, self.idCount, item.data().itemData );
                            self.idCount++;
                            item.remove();
                            self.setInactive(self.buttons.addCondition, true);
                            self.setInactive(self.buttons.addOperator, false);

                        } else {
                            item.focus();
                            _super(item, container);
                            self.setInactive(self.buttons.addCondition, false);
                            self.setInactive(self.buttons.addOperator, true);
                        }

                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "ruleErrorFullLogical");
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "ruleHelperTryAndOr");
                        self.setInactive(self.buttons.clearBtn, false);
                        self.save();
                    }

                    $("body").removeClass("dragging");

                    self.delegateEvents();

                },

                isValidTarget: function(item, container) {

                    if ( container.items.length > 0 &&
                         container.target.parent().data().itemData &&
                         container.target.parent().data().itemData[self.property]
                    ) {
                        return false;
                    } else {
                        return true;
                    }

                },

                serialize: function ($parent, $children, parentIsContainer) {

                   var result = $.extend({}, $parent.data().itemData);

                    if ( parentIsContainer ) {
                        return $children;
                    }

                    else if  ($children[0] ) {
                        if (result[self.properties]) {
                            result[self.properties] = $children;
                        } else if (result[self.property]) {
                            result[self.property] = $children[0];
                        }
                    }

                    delete result.subContainers;
                    delete result.nestingSortable;
                    return result;
                }

            });

            self.pickUpItem.nestingSortable({
                group: self.element + 'rule-creation-group' + self.groupCounter,
                drop: false
            });

            this.sortingInitialised = true;

        },

        editStart: function(item) {

           $('body').addClass('editing');
           var self = this,
               editRuleView = self.getNewRule(),
               properties = self.getProperties();
               editRuleView.render( properties, null, self.pickUpItem, self.idCount, item.data().itemData );

           self.idCount++;

           editRuleView.$el.addClass('editing');
           item.before(editRuleView.$el) ;
           self.onClear();

           editRuleView.$el.find('select#selection').focus();
        },

        editStop: function(item) {
            $('body').removeClass('editing');

            var editRuleView = $.extend( false, item, this.getNewRule() ),
                properties = this.getProperties();
                editRuleView.createListItem( properties,  item);

            item.next().remove();
            this.save();
        },

        setInactive: function(button, state) {
            if (state) { button.addClass('inactive'); }
            else { button.removeClass('inactive'); }
        },

        showHint:function(state){
            this.$el.find('#condition-management').toggleClass('show-hint', state);
        },

        onClear: function(e) {
            if(e) { e.preventDefault();}
            this.pickUpItem.children().remove();
            this.setInactive(this.buttons.clearBtn, true);
            this.setInactive(this.buttons.addCondition, false);
            this.setInactive(this.buttons.addOperator, false);
            this.showHint(false);
        },

        addOperator: function(e) {
            e.preventDefault();
            this.pickUpItem.children().remove();
            this.setInactive(this.buttons.clearBtn, false);
            this.setInactive(this.buttons.addCondition, false);
            this.setInactive(this.buttons.addOperator, true);

            var operatorRules = new OperatorRulesView();
                operatorRules.render(this.data, null, this.pickUpItem, this.idPrefix + this.idCount);

            this.showHint(true);
            this.idCount++;
        },

        addCondition: function(e) {
            e.preventDefault();
            this.pickUpItem.children().remove();
            this.setInactive(this.buttons.clearBtn, false);
            this.setInactive(this.buttons.addCondition, true);
            this.setInactive(this.buttons.addOperator, false);

            var editRuleView = this.getNewRule();
                editRuleView.render(this.getProperties(), null, this.pickUpItem, this.idCount);

            this.showHint(true);
            this.idCount++;
        },

        onSelect: function(e) {
            e.stopPropagation();
            this.save();
        },

        onDelete: function(e) {
            e.stopPropagation();
            if (e.type === 'keyup' && e.keyCode !== 13) { return;}
            var self = this, item = $(e.currentTarget).closest('li');
            item.animate({height: 0, paddingTop: 0, paddingBottom: 0,marginTop: 0,marginBottom: 0, opacity:0}, function() {
                item.remove();
                self.save();
            });
        },

        toggleEditing: function(e){
            if (e.type === 'keyup' && e.keyCode !== 13) { return;}
            var item = $(e.currentTarget).closest('li');
            if (item.hasClass('editing') ) {
                item.removeClass('editing');
                this.editStop(item);
            } else {
                this.editStart(item);
            }
        },

        setFocus: function(e) {
            e.stopPropagation();
            var target = $(e.target).is('select') || $(e.target).is('input') ?  e.target : e.currentTarget;
            $(target).focus();
        },

        getNewRule: function() {
            return this.conditionType === Constants.ENVIRONMENT ? new EditEnvironmentView() : new EditSubjectView();
        },

        getProperties: function() {
            var properties = {};
                properties[this.properties] = this.data[this.properties];
            return properties;
        },

        save: function(e) {

            if (this.sortingInitialised !== true) {
                return;
            }

            var rules = this.$el.find('ol#dropbox').nestingSortable('serialize').get(),
                operatorData = this.$el.find('#operator' + this.idPrefix + '0').data().itemData;

            // Removing any obsolete root logicals.
            if ( operatorData[this.properties] ) {
                if (rules.length <= 1) {
                    this.data.entity[this.property] = _.isEmpty(rules[0]) ? null : rules[0];
                } else {
                    operatorData[this.properties] = rules;
                    this.data.entity[this.property] = operatorData;
                }
            } else if ( operatorData[this.property] ){
                if (rules[0] && !_.isEmpty(rules[0])) {
                    this.data.entity[this.property] = operatorData;
                    this.data.entity[this.property][this.property] = rules[0];
                } else {
                    this.data.entity[this.property] = null;
                }

            } else if ( operatorData[this.property] === null) {
                this.data.entity[this.property] = null;
            } else {
                console.error("This should never be triggered", this.property, operatorData);
            }

            console.log("\n" + this.property + ":",  JSON.stringify(this.data.entity[this.property], null, 2));
        }

    });

    return ManageRulesView;
});
