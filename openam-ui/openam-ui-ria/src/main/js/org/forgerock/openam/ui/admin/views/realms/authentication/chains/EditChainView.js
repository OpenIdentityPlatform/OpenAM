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
 * Copyright 2015 ForgeRock AS.
 */

/*global define*/
define("org/forgerock/openam/ui/admin/views/realms/authentication/chains/EditChainView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/openam/ui/admin/views/realms/authentication/chains/LinkView",
    "org/forgerock/openam/ui/admin/views/realms/authentication/chains/PostProcessView",
    "org/forgerock/openam/ui/admin/delegates/SMSRealmDelegate",

    // jquery dependencies
    "sortable"
], function($, _, AbstractView, FormHelper, LinkView, PostProcessView, SMSRealmDelegate) {
    var EditChainView = AbstractView.extend({
        template: "templates/admin/views/realms/authentication/chains/EditChainTemplate.html",
        events: {
            "click #saveChanges":       "saveChanges",
            "click #addModuleLink":     "addModuleLink"
        },

        addModuleLink: function(e) {
            if (e) {
                e.preventDefault();
            }
            var li = $("<li class='chain-link' />"),
                index = this.data.form.chainData.authChainConfiguration.length,
                linkView = this.createLinkView(li, index);
            linkView.render();
        },

        createLinkView: function(li, index){
            var linkView = new LinkView();

            // A new list item is being dynamically created and added to the current EditChainView as a child View.
            // In order to do this we must create the element here,  parent and pass it to the child so that it has something to render inside of.
            linkView.el = li;
            linkView.element = li;
            linkView.parent = this;
            this.$el.find("ol#sortable").append(li);
            this.data.linkViewMap[linkView.cid] = linkView;

            // The authChainConfiguration is an array of authentication objects with modules, options and criteria.
            // Below we add an empty object to the array if there is not already one at authChainConfiguration[index].
            // This will result in an empty module being created, which will trigger the Edit Link dialog to open
            // upon LinkView render.
            if (this.data.form.chainData.authChainConfiguration.length <= index){
                this.data.form.chainData.authChainConfiguration.push({ module: '', options: '', criteria: '' });
            }

            linkView.data = {
                // Each linkview instance requires allCriteria and allModules to render. These values are never changed
                // Because multiple instances require this same data, I grab it only in this parent view, then pass it
                // on to to all the child linkview instances.
                allModules : this.data.allModules,
                allCriteria : this.data.allCriteria,
                linkConfig : this.data.form.chainData.authChainConfiguration[index],
                typeDescription : ""
            };

            return linkView;
        },

        initSortable: function(){

            var self = this;

            this.$el.find("ol#sortable").nestingSortable({
                exclude:"li:not(.chain-link)",
                delay: 100,
                vertical: true,
                placeholder: "<li class='placeholder'><i class='fa fa-download'></i>"+ $.t("console.authentication.editChains.dropHere") +"</li>",

                onDrag: function (item, position) {
                    item.css({
                        left: position.left - self.adjustment.left,
                        top: position.top - self.adjustment.top
                    });
                },

                onDragStart: function (item, container, _super) {
                    var offset = item.offset(),
                        pointer = container.rootGroup.pointer;
                    self.adjustment = {
                        left: pointer.left - offset.left + 5,
                        top: pointer.top - offset.top
                    };
                    self.originalIndex = item.index();
                    item.addClass("dragged");
                    $("body").addClass("dragging");
                },

                onDrop: function  (item, container, _super, event) {
                    var clonedItem = $("<li/>").css({height: item.height() + 6, backgroundColor: "transparent", borderColor: "transparent"});
                    self.sortChainData( self.originalIndex, item.index());
                    item.before(clonedItem);
                    item.animate( clonedItem.position(), 300, function  () {
                        clonedItem.detach();
                        _super(item, container);
                        self.setArrows();
                    });
                }
            });
        },

        render: function(args, callback) {
            var self = this;

            SMSRealmDelegate.authentication.chains.get(args[0], args[1]).done(function (data) {

                self.data = {
                    // firstRequiredIndex is used in linkView.setArrows() which renders itself differently
                    // if the linkView's index is greater than the first required module's index.
                    firstRequiredIndex : -1,
                    realmPath : args[0],
                    linkViewMap : {},
                    allModules : data.modulesData,
                    allCriteria : [{
                        REQUIRED : $.t("console.authentication.editChains.criteria.0")
                    },{
                        OPTIONAL : $.t("console.authentication.editChains.criteria.1")
                    },{
                        REQUISITE : $.t("console.authentication.editChains.criteria.2")
                    },{
                        SUFFICIENT : $.t("console.authentication.editChains.criteria.3")
                    }],
                    form : {  chainData: data.chainData }
                };

                self.parentRender(function(){
                    var sortable = self.$el.find("ol#sortable");

                    _.each(self.data.form.chainData.authChainConfiguration, function(linkConfig, index){
                        var li = $("<li class='chain-link' />"),
                            linkView = self.createLinkView(li, index);
                        linkView.render();
                    });

                    self.$el.find("[data-toggle='popover']").popover();
                    self.initSortable();
                    PostProcessView.render(self.data.form.chainData);

                    if (self.data.form.chainData.authChainConfiguration.length === 0) {
                        self.addModuleLink();
                    }
                });

            })
            .fail(function () {
                // TODO: Add failure condition
            });
        },

        saveChanges: function(e){
            e.preventDefault();
            var self = this,
                promise = null,
                chainData = this.data.form.chainData;

            chainData.loginSuccessUrl[0] = this.$el.find("#loginSuccessUrl").val();
            chainData.loginFailureUrl[0] = this.$el.find("#loginFailureUrl").val();

            PostProcessView.addClassNameDialog().done(function(){
                promise = SMSRealmDelegate.authentication.chains.update(self.data.realmPath, chainData._id, chainData);
                promise.fail(function(e) {
                    // TODO: Add failure condition
                    console.error(e);
                });
                FormHelper.bindSavePromiseToElement(promise, e.target);
            });
        },

        setArrows: function(){
            var chainlinks = this.$el.find(".chain-link"),
                linkView = null,
                self = this;

            // firstRequiredIndex is used in linkView.setArrows() which renders itself differently
            // if the linkView's index is greater than the first required module's index.
            this.data.firstRequiredIndex = -1;
            _.each(chainlinks, function(chainlink){
                linkView = self.data.linkViewMap[$(chainlink).children().data().id];
                linkView.setArrows();
            });
        },

        sortChainData: function(from, to) {
            this.data.form.chainData.authChainConfiguration.splice(to, 0, this.data.form.chainData.authChainConfiguration.splice(from, 1)[0]);
        }
    });

    return EditChainView;

});
