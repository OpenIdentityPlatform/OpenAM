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
        events: {
            "dblclick .panel":     "editItem",
            "click #editItem":     "editItem",
            "show.bs.dropdown" :   "setBtnStates",
            "click .move-btn":     "moveBtnClick",
            "click #deleteBtn":    "deleteBtnClick",
            "click .criteria-btn-container button": "selectCriteria"
        },

        deleteBtnClick: function(e){
            if(e){
                e.preventDefault();
            }
            this.deleteItem(e.currentTarget.dataset.mapId);
        },

        deleteItem: function(mapId) {
            this.parent.data.form.chainData.authChainConfiguration.splice(this.$el.index(), 1);
            this.parent.data.linkViewMap[mapId].remove();
            this.parent.setArrows();
            this.remove();
        },

        editItem: function(e) {
            if (e) {
                e.preventDefault();
            }
            var self = this,
                title = this.data.linkConfig.module ? $.t("console.authentication.editChains.editModule") : $.t("console.authentication.editChains.newModule");

            UIUtils.fillTemplateWithData("templates/admin/views/realms/authentication/chains/EditLinkTemplate.html", self.data, function(template) {
                var options = {
                    message: function (dialog) {
                        return $("<div></div>").append(template);
                    },
                    title: title,
                    type: BootstrapDialog.TYPE_DEFAULT,
                    closable: false,
                    buttons: [{
                        label: $.t("common.form.ok"),
                        cssClass: "btn-primary",
                        id: "saveBtn",
                        action: function(dialog) {
                            var moduleSelectize = dialog.getModalBody().find("#selectModule")[0].selectize,
                                criteriaValue = dialog.getModalBody().find("#selectCriteria")[0].selectize.getValue();

                            self.data.linkConfig.module = moduleSelectize.getValue();
                            self.data.linkConfig.type = _.findWhere(moduleSelectize.options, { _id: self.data.linkConfig.module }).type;
                            self.linkInfoView.parentRender();
                            self.criteriaView.setCriteria(criteriaValue);
                            self.parent.setArrows();
                            dialog.close();
                        }
                    }, {
                        label: $.t("common.form.cancel"),
                        action: function(dialog) {
                            if (self.data.linkConfig.module) {
                                self.parent.setArrows();
                            } else {
                                self.deleteItem(self.data.id);
                            }
                            dialog.close();
                        }
                    }],
                    onshow: function (dialog) {
                        dialog.getButton("saveBtn").disable();
                        dialog.getModalBody().find("#selectModule").selectize({
                            options:self.data.allModules,
                            render: {
                                item: function(item) {
                                    return "<div>" + item._id + " <span class='dropdown-subtitle'>"+ item.type + "</span></div>";
                                },
                                option: function(item) {
                                    return "<div><div>" + item._id + "</div><div class='dropdown-subtitle'>"+ item.type + "</div></div>";
                                }
                            },
                            onChange: function (value) {
                                dialog.options.validateDialog(dialog);
                            }

                        });
                        dialog.getModalBody().find("#selectCriteria").selectize({
                            onChange: function (value) {
                                dialog.options.validateDialog(dialog);
                            }
                        });
                        dialog.options.validateDialog(dialog);
                        dialog.getModalBody().find("#selectModule");
                    },

                    validateDialog: function(dialog){
                        var moduleValue = dialog.getModalBody().find("#selectModule")[0].selectize.getValue(),
                            criteriaValue = dialog.getModalBody().find("#selectCriteria")[0].selectize.getValue();

                        if( moduleValue.length === 0 || criteriaValue.length === 0 ){
                            dialog.getButton("saveBtn").disable();
                        } else {
                            dialog.getButton("saveBtn").enable();
                        }
                    }
                };
                BootstrapDialog.show(options);
            });
        },

        moveBtnClick: function(e) {
            e.preventDefault();

            var direction = parseInt(e.currentTarget.dataset.direction, 10),
                chainlinks = this.$el.parent().children(".chain-link"),
                originalIndex = this.$el.index(),
                targetIndex = originalIndex + direction;

            this.parent.sortChainData(originalIndex, targetIndex);

            // The buttons contain the directions -1 for up and +1 for down.
            if (direction === -1 ) {
                this.$el.insertBefore(chainlinks.eq(targetIndex));
            } else {
                this.$el.insertAfter(chainlinks.eq(targetIndex));
            }
            this.parent.setArrows();
        },

        render: function () {
            var self = this;
            this.data.id = this.cid;

            self.parentRender(function(){

                self.criteriaView = new CriteriaView();
                self.criteriaView.element = "#criteria-" + self.data.id;
                self.criteriaView.render(self.data.linkConfig, self.data.allCriteria, self.data.id);

                self.linkInfoView = new LinkInfoView();
                self.linkInfoView.element = "#link-info-" + self.data.id;
                self.linkInfoView.render(self.data.linkConfig);

                if (!self.data.linkConfig.module && !self.data.linkConfig.criteria) {
                    self.editItem();
                } else {
                    self.setArrows();
                }
            });
        },

        setArrows: function(){
            if (this.data.linkConfig.criteria.toLowerCase() === this.criteriaView.REQUIRED && this.parent.data.firstRequiredIndex === -1) {
                this.parent.data.firstRequiredIndex = this.$el.index();
            }
            if (this.parent.data.firstRequiredIndex > -1 && this.$el.index() > this.parent.data.firstRequiredIndex){
                this.criteriaView.setPassThroughAndFailArrows(true);
            } else {
                this.criteriaView.setPassThroughAndFailArrows(false);
            }
        },
        setBtnStates: function(){
            var numLinks = this.$el.parent().children(".chain-link").length - 1,
                itemIndex = this.$el.index(),
                moveUpBtn = this.$el.find("#moveUpLi"),
                moveDownBtn = this.$el.find("#moveDownLi"),
                deleteBtn = this.$el.find("#deleteBtn").parent();

            moveUpBtn.removeClass("disabled");
            moveDownBtn.removeClass("disabled");
            deleteBtn.removeClass("disabled");

            if (itemIndex === 0){
                moveUpBtn.addClass("disabled");
            }
            if (itemIndex === numLinks){
                moveDownBtn.addClass("disabled");
            }
            if(numLinks < 1){
                deleteBtn.addClass("disabled");
            }
        },
        selectCriteria: function(e){
            var criteria = e.currentTarget.dataset.criteria;
            this.criteriaView.setCriteria(criteria);
            this.parent.setArrows();
        }
    });

    return LinkView;

});
