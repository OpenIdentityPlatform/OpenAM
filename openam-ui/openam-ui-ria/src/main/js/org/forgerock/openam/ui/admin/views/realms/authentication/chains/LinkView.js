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

define("org/forgerock/openam/ui/admin/views/realms/authentication/chains/LinkView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "bootstrap-dialog",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/admin/views/realms/authentication/chains/CriteriaView",
    "org/forgerock/openam/ui/admin/views/realms/authentication/chains/LinkInfoView"
], function($, _, AbstractView, BootstrapDialog, UIUtils, CriteriaView, LinkInfoView) {

    var LinkView = AbstractView.extend({
        template: "templates/admin/views/realms/authentication/chains/LinkTemplate.html",
        mode: "append",
        //data: {},
        events: {
            'dblclick .panel':     'editItem',
            'click #editItem':     'editItem',
            'click #deleteItem':   'deleteItem',
            'click #moveUpBtn':    'moveUp',
            'click #moveDownBtn':  'moveDown',
            'show.bs.dropdown' :   'setArrrowBtnStates'
        },
        // Constants used for moving item up or down.
        MOVE_UP: "moveUp",
        MOVE_DOWN: "moveDown",

        deleteItem: function(e) {
            if(e){e.preventDefault();}
            var self = this;
            _.remove(self.data.formData.chainData.authChainConfiguration, {
                id: self.data.id
            });
            this.remove();
        },

        editItem: function(e) {
            if (e) {
                e.preventDefault();
            }
            var self = this,
                linkConfig = _.find( this.data.formData.chainData.authChainConfiguration, function(config){
                    return config.id === self.data.id;
                }),
                title = linkConfig.module ? $.t('console.authentication.editChains.editModule') : $.t('console.authentication.editChains.newModule'); // TODO i18n

            // FIXME: Refactor this code as a separate view. Enable adding and deletion of options.
            UIUtils.fillTemplateWithData("templates/admin/views/realms/authentication/chains/EditLinkTemplate.html", { linkConfig:linkConfig, formData:this.data.formData }, function(template) {
                var options = {

                    message: function (dialog) {
                        return $('<div></div>').append(template);
                    },

                    title: title,
                    type: BootstrapDialog.TYPE_DEFAULT,
                    closable: false,
                    buttons: [{
                        label: "Ok",
                        cssClass: "btn-primary",
                        id: "saveBtn",
                        action: function(dialog) {
                            var moduleSelectize = dialog.getModalBody().find('#selectModule')[0].selectize,
                                criteriaValue = dialog.getModalBody().find('#selectCriteria')[0].selectize.getValue();

                            linkConfig.module = moduleSelectize.getValue();
                            linkConfig.type = _.findWhere(moduleSelectize.options, { _id: linkConfig.module }).type;

                            self.linkInfoView.parentRender();
                            self.criteriaView.setCriteria(criteriaValue);

                            dialog.close();
                        }
                    }, {
                        label: "Cancel",
                        action: function(dialog) {
                            if (!linkConfig.module) {
                                self.deleteItem();
                            }
                            dialog.close();
                        }
                    }],

                    onshow: function (dialog) {

                        dialog.getButton('saveBtn').disable();
                        dialog.getModalBody().find('#selectModule').selectize({

                            options:self.data.formData.allModules,
                            render: {
                                item: function(item) {
                                    return '<div>' + item._id + ' <span class="dropdown-subtitle">'+ item.type + '</span></div>';
                                },
                                option: function(item) {
                                    return '<div><div>' + item._id + '</div><div class="dropdown-subtitle">'+ item.type + '</div></div>';
                                }
                            },
                            onChange: function (value) {
                                dialog.options.validateDialog(dialog);
                            }

                        });

                        dialog.getModalBody().find('#selectCriteria').selectize({
                            onChange: function (value) {
                                dialog.options.validateDialog(dialog);
                            }
                        });

                        dialog.options.validateDialog(dialog);
                        dialog.getModalBody().find('#selectModule');

                    },

                    validateDialog: function(dialog){
                        var moduleValue = dialog.getModalBody().find('#selectModule')[0].selectize.getValue(),
                            criteriaValue = dialog.getModalBody().find('#selectCriteria')[0].selectize.getValue();

                        if( moduleValue.length === 0 || criteriaValue.length === 0 ){
                            dialog.getButton('saveBtn').disable();
                        } else {
                            dialog.getButton('saveBtn').enable();
                        }
                    }
                };

                BootstrapDialog.show(options);
            });
        },

        render: function (data, index, li) {

            var self = this,
                linkConfig = {};

            self.el = li;
            self.element = li;

            //self.data = {};
            self.data.formData = data;
            self.data.id = _.uniqueId('id-');

            linkConfig = self.data.formData.chainData.authChainConfiguration[index];
            linkConfig.id = self.data.id;

            self.parentRender(function(){

                if (!linkConfig.module) {
                    self.editItem();
                }

                self.criteriaView = new CriteriaView();
                self.criteriaView.element = "#criteria-" + self.data.id;
                self.criteriaView.render(linkConfig, self.data.formData.allCriteria);

                self.linkInfoView = new LinkInfoView();
                self.linkInfoView.element = "#link-info-" + self.data.id;
                self.linkInfoView.render(linkConfig);

            });
        },

        moveUp: function(e) {
            e.preventDefault();
            this.move(this.MOVE_UP);
        },

        moveDown: function(e) {
            e.preventDefault();
            this.move(this.MOVE_DOWN);
        },

        move: function(direction) {

            var chainlinks = this.$el.parent().children('.chain-link'),
                itemIndex = this.$el.index();

            if (direction === this.MOVE_UP) {
                this.$el.insertBefore(chainlinks.eq(itemIndex-2));

            } else if (direction === this.MOVE_DOWN) {
                this.$el.insertAfter(chainlinks.eq(itemIndex));
            }
        },

        setArrrowBtnStates: function(){

            var numLinks = this.$el.parent().children('.chain-link').length,
                itemIndex = this.$el.index(),
                moveUpBtn = this.$el.find('#moveUpLi'),
                moveDownBtn = this.$el.find('#moveDownLi');

            moveUpBtn.removeClass('disabled');
            moveDownBtn.removeClass('disabled');

            if (itemIndex === 1){
                moveUpBtn.addClass('disabled');
            }
            if (itemIndex === numLinks){
                moveDownBtn.addClass('disabled');
            }
        }

    });

    return LinkView;

});
