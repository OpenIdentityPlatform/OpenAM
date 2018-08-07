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
 * Portions copyright 2014-2016 ForgeRock AS.
 */


define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/EditEnvironmentView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/EditSubjectView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/OperatorRulesView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/LegacyListItemView",

    // jquery dependencies
    "sortable"
], ($, _, AbstractView, EventManager, Constants, UIUtils, EditEnvironmentView, EditSubjectView, OperatorRulesView,
    LegacyListItemView) => {

    return AbstractView.extend({
        template: "templates/admin/views/realms/authorization/policies/conditions/ManageRulesTemplate.html",
        noBaseTemplate: true,
        events: {
            "click  [data-add-condition]:not(.disabled)": "addCondition",
            "keyup  [data-add-condition]:not(.disabled)": "addCondition",
            "click  [data-add-operator]:not(.disabled)": "addOperator",
            "keyup  [data-add-operator]:not(.disabled)": "addOperator",
            "click  [data-delete-operator]": "onDelete",
            "keyup  [data-delete-operator]": "onDelete"
        },
        types: {
            ENVIRONMENT: "environmentType",
            SUBJECT: "subjectType",
            LEGACY: "Policy"
        },

        localEntity: {},
        groupCounter: 0,

        idPrefix: "",
        property: "",
        properties: "",

        initialize () {
            AbstractView.prototype.initialize.call(this);

            // Needed for correct work of animation
            UIUtils.preloadTemplates(
                _.map([
                    "ConditionAttrEnum", "ConditionAttrString", "ConditionAttrBoolean", "ConditionAttrArray",
                    "ConditionAttrObject", "ConditionAttrTime", "ConditionAttrDay", "ConditionAttrDate",
                    "OperatorRulesTemplate", "EditSubjectTemplate", "EditEnvironmentTemplate"
                ], (filename) => {
                    return `templates/admin/views/realms/authorization/policies/conditions/${filename}.html`;
                })
            );
        },

        init (args, events) {
            _.extend(this.events, events);
            _.extend(Constants, this.types);

            this.localEntity = null;
            this.sortingInitialised = false;

            if (this.data.entity[this.property]) {
                this.localEntity = this.data.entity[this.property];
            }
        },

        buildList () {
            var self = this,
                newRule = null,
                operators = _.pluck(this.data.operators, "title"),
                properties = null;

            function buildListItem (data, container) {
                if (_.isArray(data) === false) {
                    data = [data];
                }

                _.each(data, function (item) {
                    if (item && _.contains(operators, item.type)) {

                        newRule = new OperatorRulesView();
                        newRule.render(self.data, container, self.idPrefix + self.idCount, (self.idCount === 0));
                        newRule.setValue(item.type);
                        self.idCount++;

                    } else if (!_.isEmpty(item)) {
                        if (item.type === Constants.LEGACY) {
                            newRule = new LegacyListItemView();
                            newRule.render(item, container, self.idCount);
                        } else {
                            newRule = self.getNewRule();
                            properties = self.getProperties();
                            newRule.render(properties, container, self.idCount, item);
                            newRule.createListItem(properties, newRule.$el);
                        }

                        self.idCount++;
                    }

                    if (item && item[self.properties]) {
                        buildListItem(item[self.properties], newRule.dropbox, item);
                    } else if (item && item[self.property]) {
                        buildListItem(item[self.property], newRule.dropbox, item);
                    }
                });
            }

            /*
             * This view will detect if the preserved rule begins with a logical. If it doesn't, an AND logical will be
             * added to the root to give the users somewhere to drop rules into.
             * However if the root logical is obsolete, for example it is one which expects many children but contains
             * one or less, the root logical will be striped from the json before it is saved.
             */

            if (!this.localEntity || _.contains(operators, this.localEntity.type) === false) {
                properties = _.clone(this.localEntity);
                this.localEntity = { type: "AND" };
                this.localEntity[this.properties] = [properties];
            }

            buildListItem(this.localEntity, this.$el.find("ol#dropOffArea"), null);
            this.delegateEvents();
        },

        initSorting () {
            var self = this;

            this.groupCounter++;

            this.$el.find("ol#dropbox").sortable({
                group: `${self.element}rule-creation-group${self.groupCounter}`,
                exclude: ".item-button-panel, li.editing",
                delay: 100,

                onMousedown ($item, _super, event) {
                    event.stopPropagation();
                    if (!event.target.nodeName.match(/^(input|select|textarea)$/i)) {
                        event.preventDefault();
                        return true;
                    }
                },
                // set item relative to cursor position
                onDragStart (item, container) {
                    var offset = item.offset(),
                        pointer = container.rootGroup.pointer,
                        editRuleView = null;
                    self.adjustment = {
                        left: pointer.left - offset.left + 5,
                        top: pointer.top - offset.top
                    };

                    self.setInactive(self.buttons.addCondition, false);
                    self.setInactive(self.buttons.addOperator, false);

                    item.focus();
                    item.css({ width: item.width() }).addClass("dragged");
                    $("body").addClass("dragging");

                    if (!container.options.drop && item.hasClass("rule")) {
                        editRuleView = $.extend(false, item, self.getNewRule());
                        editRuleView.createListItem(self.getProperties(), item);
                    }
                },

                onDrag (item, position) {
                    item.css({
                        left: position.left - self.adjustment.left,
                        top: position.top - self.adjustment.top
                    });
                },

                onDrop (item, container, _super) {
                    var rule = null, clonedItem, newHeight, animeAttrs;

                    clonedItem = $("<li/>").css({
                        height: 0,
                        backgroundColor: "transparent",
                        borderColor: "transparent"
                    });
                    item.before(clonedItem);
                    newHeight = item.height();
                    animeAttrs = clonedItem.position();
                    animeAttrs.width = clonedItem.outerWidth() - 10;
                    item.addClass("dropped");
                    clonedItem.animate({ "height": newHeight }, 300, "linear");
                    item.animate(animeAttrs, 300, function () {

                        clonedItem.detach();
                        item.removeClass("dropped");

                        if (item.data().logical === true) {
                            rule = $.extend(false, item, new OperatorRulesView());
                            rule.rebindElement();
                        }
                        item.focus();
                        _super(item, container);
                        self.save();
                    });

                    $("body").removeClass("dragging");

                    self.delegateEvents();
                },

                isValidTarget (item, container) {
                    var notValid = (container.items.length > 0 &&
                        container.target.parent().data().itemData &&
                        container.target.parent().data().itemData[self.property]) ||
                        item.hasClass("editing-disabled");

                    return !notValid;
                },

                serialize ($parent, $children, parentIsContainer) {
                    var result = $.extend({}, $parent.data().itemData);

                    if (parentIsContainer) {
                        return $children;
                    } else if ($children[0]) {
                        if (result[self.properties]) {
                            result[self.properties] = $children;
                        } else if (result[self.property]) {
                            result[self.property] = $children[0];
                        }
                    }

                    delete result.subContainers;
                    delete result.nestedSortable;
                    return result;
                }
            });

            this.sortingInitialised = true;
        },

        editStart (item) {
            $("body").addClass("editing");

            var self = this,
                editRuleView = self.getNewRule(),
                properties = self.getProperties(),
                disabledConditions;
            editRuleView.render(properties, item.parent(), self.idCount, item.data().itemData);

            self.idCount++;

            editRuleView.$el.addClass("editing");
            item.before(editRuleView.$el);

            this.setInactive(this.buttons.addCondition, true);
            this.setInactive(this.buttons.addOperator, true);

            disabledConditions = this.$el.find(".rule, .operator").not(".editing");
            disabledConditions.addClass("editing-disabled");
            disabledConditions.find("> select").prop("disabled", true);

            editRuleView.$el.find("select.type-selection:first").focus();
        },

        editStop (item) {
            $("body").removeClass("editing");

            var editRuleView = $.extend(false, item, this.getNewRule()),
                properties = this.getProperties(),
                disabledConditions;

            editRuleView.createListItem(properties, item);

            item.next().remove();
            this.save();

            disabledConditions = this.$el.find(".rule, .operator").not(".editing");
            disabledConditions.removeClass("editing-disabled");
            disabledConditions.find("> select").prop("disabled", false);
        },

        setInactive (button, state) {
            button.toggleClass("disabled", state);
        },

        addOperator (e) {
            e.preventDefault();

            if (e.type === "keyup" && e.keyCode !== 13) {
                return;
            }

            var operatorRules = new OperatorRulesView();
            operatorRules.render(this.data, this.droppableParent, this.idPrefix + this.idCount);

            this.idCount++;
        },

        addCondition (e) {
            e.preventDefault();

            if (e.type === "keyup" && e.keyCode !== 13) {
                return;
            }

            var editRuleView = this.getNewRule(),
                self = this;

            editRuleView.render(this.getProperties(), this.droppableParent, this.idCount, null,
                function onRuleRender () {
                    self.editStart(editRuleView.$el);
                    self.$el.find("ol#dropbox").sortable("refresh");
                });

            this.idCount++;
        },

        onSelect (e) {
            e.stopPropagation();
            this.save();
        },

        onDelete (e) {
            e.stopPropagation();

            if (e.type === "keyup" && e.keyCode !== 13) {
                return;
            }

            var self = this,
                item = $(e.currentTarget).closest("li"),
                disabledConditions;

            item.animate({
                height: 0, paddingTop: 0, paddingBottom: 0,
                marginTop: 0, marginBottom: 0, opacity: 0
            }, function () {
                // if deleted on edit step
                if ($("body").hasClass("editing")) {
                    $("body").removeClass("editing");
                    item.next().remove();

                    disabledConditions = self.$el.find(".rule, .operator").not(".editing");
                    disabledConditions.removeClass("editing-disabled");
                    disabledConditions.find("> select").prop("disabled", false);
                }

                item.remove();
                self.save();
            });
        },

        toggleEditing (e) {
            if (e.type === "keyup" && e.keyCode !== 13) {
                return;
            }

            var item = $(e.currentTarget).closest("li");

            if (item.hasClass("editing-disabled")) {
                return;
            }

            if (item.hasClass("editing")) {
                item.removeClass("editing");
                this.editStop(item);
            } else {
                this.editStart(item);
            }
        },

        setFocus (e) {
            e.stopPropagation();
            var target = $(e.target).is("select") || $(e.target).is("input") ? e.target : e.currentTarget;
            $(target).focus();
        },

        getNewRule () {
            return this.conditionType === Constants.ENVIRONMENT ? new EditEnvironmentView() : new EditSubjectView();
        },

        getProperties () {
            var properties = {};
            properties[this.properties] = this.data[this.properties];
            return properties;
        },

        save () {
            if (this.sortingInitialised !== true) {
                return;
            }

            var rules = this.$el.find("ol#dropbox").sortable("serialize").get(),
                operatorData = this.$el.find(`#operator${this.idPrefix}0`).data().itemData;

            // Removing any obsolete root logicals.
            if (operatorData[this.properties]) {
                if (rules.length <= 1) {
                    this.data.entity[this.property] = _.isEmpty(rules[0]) ? null : rules[0];
                } else {
                    operatorData[this.properties] = rules;
                    this.data.entity[this.property] = operatorData;
                }
            } else if (operatorData[this.property]) {
                if (rules[0] && !_.isEmpty(rules[0])) {
                    this.data.entity[this.property] = operatorData;
                    this.data.entity[this.property][this.property] = rules[0];
                } else {
                    this.data.entity[this.property] = null;
                }

            } else if (operatorData[this.property] === null) {
                this.data.entity[this.property] = null;
            } else {
                console.error("This should never be triggered", this.property, operatorData);
            }

            console.log(`\n${this.property}:`, JSON.stringify(this.data.entity[this.property], null, 2));

            this.identifyDroppableLogical();
        },

        /**
         * Searches for the most outer possible dropabble logical container that will be used as a drop target. If such
         * container was not found, disables "Add" buttons and displays corresponding message.
         */
        identifyDroppableLogical () {
            var rootLogical = this.$el.find(`#operator${this.idPrefix}0`),
                nestedItems,
                nestedLogicals,
                nestedLogicalsLength,
                nestedRules,
                logical,
                canHaveMultiple,
                notIsEmpty,
                i = 0;

            this.droppableParent = null;

            if (rootLogical.hasClass("not")) {
                nestedItems = rootLogical.find("li");
                nestedLogicals = nestedItems.filter(".operator").get();
                nestedLogicalsLength = nestedLogicals.length;
                nestedRules = nestedItems.filter(".rule").get();

                if (nestedLogicalsLength) {
                    // loop through nested logicals and return first one which is either an "AND"/"OR"
                    // or "NOT" without children
                    for (; i < nestedLogicalsLength; i++) {
                        logical = $(nestedLogicals[i]);

                        // "AND" or "OR" can have multiple children
                        canHaveMultiple = logical.hasClass("and") || logical.hasClass("or");
                        // "NOT" can have just one child
                        notIsEmpty = logical.hasClass("not") &&
                            logical.children(".dropbox").children("li").length === 0;

                        if (canHaveMultiple || notIsEmpty) {
                            this.droppableParent = logical.children(".dropbox");
                            break;
                        }
                    }
                } else if (nestedRules.length === 0) {
                    // "NOT" is empty
                    this.droppableParent = rootLogical.children(".dropbox");
                }
            } else {
                // root logical is either "AND" or "OR"
                this.droppableParent = rootLogical.children(".dropbox");
            }

            if (this.droppableParent) {
                this.setInactive(this.buttons.addCondition, false);
                this.setInactive(this.buttons.addOperator, false);
                this.$el.find(".one-child-only:first").hide();
            } else {
                this.setInactive(this.buttons.addCondition, true);
                this.setInactive(this.buttons.addOperator, true);
                this.$el.find(".one-child-only:first").show();
            }
        }
    });
});
